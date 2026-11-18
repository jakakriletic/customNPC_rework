package noppes.npcs.rework.diag;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityVindicator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.render.NpcBatch;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/** Controller-only M14.3b5. Production path AI walks a patrol, then production combat AI
 * fights a vindicator. Nothing sets NPC positions or animation fields after spawn. Client
 * poses are sampled synchronously at real partialTicks; A/B/A2 use the whole living renderer.
 * World Pre/Post counters separately witness the normal render pass at switch 1. No timing. */
@SideOnly(Side.CLIENT)
public final class NpcBatchMotionHarness {
    private static final String NAME = "M143M";
    // Extra diagnostic coverage, not substitutes for the real render-event partial.
    // Include v4's recorded partial exactly; its other unrecorded inputs are unknown.
    private static final float[] WALK_PARTIALS = {0, 0.099999666f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1};
    private final Properties job;
    private final File out;
    private final JsonArray rows = new JsonArray(), probes = new JsonArray(), checks = new JsonArray(), screenshots = new JsonArray();
    private final Set<String> captured = new HashSet<String>();
    private String phase = "MENU", error = "", pendingShot;
    private long since = System.nanoTime();
    private boolean finished, allOk = true, sampling, controlsDone, diagnosedFailure, routePrepared;
    private volatile boolean setupDone, running, simulationDone;
    private volatile String serverError, serverStage = "warm";
    private volatile int serverTicks;
    private volatile JsonObject serverSummary;
    private JsonObject legacyControl;
    private EntityCustomNpc npc;
    // Server-thread-owned fields only. Client sees immutable summary published at the end.
    private EntityNPCInterface actor;
    private EntityVindicator enemy;
    private int simTicks, pathTicks, serverSwings, serverHurts;
    private double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
    private float actorBefore, enemyBefore, actorAfter, enemyAfter;
    private int sampleTick = -100, walk, swing, hurt, recovery, frames, badFrames, glErrors, posts, offers;
    private long beforeDraws, frameDraws;
    private boolean frameHurt;
    private String firstBad = "";

    private NpcBatchMotionHarness(Properties p) { job = p; out = new File(p.getProperty("out")); }

    static boolean installIfRequested(String path) throws Exception {
        Properties p = new Properties();
        try (InputStreamReader in = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)) { p.load(in); }
        if (!"1".equals(p.getProperty("npcBatchMotion"))) { return false; }
        if (!p.containsKey("world") || !p.containsKey("out")) { throw new IllegalArgumentException("Incomplete motion job"); }
        MinecraftForge.EVENT_BUS.register(new NpcBatchMotionHarness(p)); return true;
    }

    private void check(String key, boolean ok, String detail) {
        JsonObject c = new JsonObject(); c.addProperty("key", key); c.addProperty("ok", ok); c.addProperty("detail", detail);
        checks.add(c); allOk &= ok; System.out.println("M143M CHECK " + key + " ok=" + ok + " " + detail);
    }
    private void enter(String next) { phase = next; since = System.nanoTime(); }

    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e) {
        if (finished || e.phase != TickEvent.Phase.END) { return; }
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (serverError != null) { throw new IllegalStateException(serverError); }
            if (System.nanoTime() - since > 300000000000L) {
                throw new IllegalStateException("Timeout " + phase + " setup=" + setupDone
                    + " npc=" + (npc == null ? "absent" : npc.getName() + " ticks=" + npc.ticksExisted
                        + " health=" + npc.getMaxHealth() + " skin=" + npc.display.getSkinTexture()
                        + " showName=" + npc.display.getShowName() + " updateClient=" + npc.updateClient)
                    + " stage=" + serverStage + " serverTick=" + serverTicks);
            }
            if (phase.equals("MENU") && mc.currentScreen instanceof GuiMainMenu) {
                mc.gameSettings.pauseOnLostFocus = false; CustomNpcs.RwNpcInstancing = 0;
                enter("JOIN");
                mc.launchIntegratedServer(job.getProperty("world"), job.getProperty("world"),
                    new WorldSettings(HarnessJob.seedOf("customnpc-rework"), GameType.CREATIVE, false, false, WorldType.FLAT).enableCommands());
            } else if (phase.equals("JOIN") && mc.world != null && mc.player != null && mc.currentScreen == null) {
                final MinecraftServer server = mc.getIntegratedServer(); final String player = mc.player.getName();
                server.addScheduledTask(() -> {
                    try {
                        // A fresh FLAT world's spawn may be hundreds of blocks away.
                        // Teleport does not synchronously load its destination: summon
                        // rejects unloaded chunks before it even parses the NPC's NBT.
                        // Load the patrol/camera corridor on the server thread first.
                        for (int cx = -1; cx <= 0; cx++) {
                            for (int cz = -1; cz <= 0; cz++) { server.getWorld(0).getChunkFromChunkCoords(cx, cz); }
                        }
                        for (String c : new String[] {"gamerule doMobSpawning false", "gamerule doDaylightCycle false",
                            "difficulty normal", "time set 6000", "tp " + player + " 0.5 4 -8 0 8",
                            "summon customnpcs:customnpc -3.5 4 2.5 {Name:\"" + NAME
                                + "\",MovingState:0,StandingState:1,StartPosNew:[I;-4,4,2]}"}) {
                            if (server.getCommandManager().executeCommand(server, c) < 1) { throw new IllegalStateException(c); }
                        }
                        for (Entity x : server.getWorld(0).loadedEntityList) {
                            if (x instanceof EntityNPCInterface && NAME.equals(x.getName())) { actor = (EntityNPCInterface) x; }
                        }
                        if (actor == null) { throw new IllegalStateException("server NPC missing"); }
                        actor.stats.setMaxHealth(1000); actor.setHealth(1000);
                        actor.display.setShowName(1); actor.display.setSkinTexture("minecraft:textures/entity/steve.png");
                        actor.updateClient = true;
                    } catch (Exception problem) { serverError = problem.toString(); }
                    setupDone = true;
                });
                enter("FIND");
            } else if (phase.equals("FIND") && setupDone) {
                for (Entity x : mc.world.loadedEntityList) {
                    if (x instanceof EntityCustomNpc && NAME.equals(x.getName())) { npc = (EntityCustomNpc) x; }
                }
                if (npc != null && !routePrepared) {
                    // accepts installs its render-boundary listener lazily, even when it
                    // refuses a pass. Register before a subsequent RenderTick.START,
                    // while still at switch 0 and before the AI/observed interval starts.
                    // Otherwise the first switch-1 world pass correctly stays original
                    // because the listener missed that frame's START (v4/v5).
                    RenderLivingBase<?> renderer = (RenderLivingBase<?>) mc.getRenderManager().getEntityRenderObject(npc);
                    NpcBatch.accepts(renderer, renderer.getMainModel(), npc);
                    routePrepared = true;
                    return;
                }
                // readSpawnData -> DataDisplay.setName sets updateClient even on the
                // client, where the server-only updateClient() sender never clears it.
                // Wait for the actual networked setup values, not that dirty flag.
                if (npc == null || npc.ticksExisted < 40 || npc.getMaxHealth() != 1000
                        || npc.display.getShowName() != 1
                        || !"minecraft:textures/entity/steve.png".equals(npc.display.getSkinTexture())) { return; }
                check("live-npc", mc.world.getEntityByID(npc.getEntityId()) == npc && Math.abs(npc.posY - 4) < 0.1,
                    "id=" + npc.getEntityId() + " position=" + npc.getPositionVector());
                if ("1".equals(job.getProperty("depthProbe")) || "1".equals(job.getProperty("depthRegression"))) {
                    // Separate synthetic diagnosis: never counted as live AI evidence.
                    enter("DEPTH_PROBE");
                } else { CustomNpcs.RwNpcInstancing = 1; running = true; enter("RUN"); }
            } else if (phase.equals("RUN") && simulationDone) {
                running = false;
                check("server-navigation", serverSummary.get("walkSpan").getAsDouble() >= 4 && pathTicksSummary() >= 20,
                    serverSummary.toString());
                check("server-combat", serverSummary.get("swings").getAsInt() >= 3 && serverSummary.get("hurts").getAsInt() >= 3
                    && actorAfter < actorBefore && enemyAfter < enemyBefore && actorAfter > 0 && enemyAfter > 0, serverSummary.toString());
                check("samples-walk", walk >= 20, "walk=" + walk);
                check("samples-swing", swing >= 4, "unhurt attack=" + swing);
                check("samples-hurt", hurt >= 4, "hurt=" + hurt);
                check("samples-recovery", recovery >= 10, "recovery=" + recovery);
                boolean images = rows.size() > 0;
                for (com.google.gson.JsonElement r : rows) { images &= r.getAsJsonObject().get("success").getAsBoolean(); }
                for (com.google.gson.JsonElement r : probes) { images &= r.getAsJsonObject().get("success").getAsBoolean(); }
                check("all-images", images, "samples=" + rows.size());
                check("walk-partial-sweep", walk >= 20 && probes.size() == walk * WALK_PARTIALS.length,
                    "walk=" + walk + " probes=" + probes.size() + " partials=" + Arrays.toString(WALK_PARTIALS));
                check("negative-controls", controlsDone, "pose+UV controls executed and rejected");
                check("live-world-route", frames >= 100 && badFrames == 0, "frames=" + frames + " bad=" + badFrames + " first=" + firstBad);
                check("live-world-gl", glErrors == 0, "GL errors=" + glErrors);
                check("screenshots", captured.size() == 4 && screenshots.size() == 4, "FBO+world categories=" + captured);
                finish(mc);
            }
        } catch (Exception | LinkageError problem) { problem.printStackTrace(); error = problem.toString(); finish(mc); }
    }

    private int pathTicksSummary() { return serverSummary.get("pathTicks").getAsInt(); }

    @SubscribeEvent public void serverTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !running || simulationDone || serverError != null) { return; }
        try {
            if (++simTicks == 1) {
                actor.ais.setMovingPath(Arrays.asList(new int[] {-4, 4, 2}, new int[] {4, 4, 2}));
                actor.ais.setMovingPathType(0, false); actor.ais.setMovingType(2); actor.updateClient = true;
            }
            if (simTicks <= 240) {
                minX = Math.min(minX, actor.posX); maxX = Math.max(maxX, actor.posX);
                if (!actor.getNavigator().noPath()) { pathTicks++; }
                serverStage = "walk";
            } else if (simTicks == 241) {
                actor.ais.setMovingType(0); actor.getNavigator().clearPath();
                enemy = new EntityVindicator(actor.world);
                enemy.setLocationAndAngles(actor.posX + 2.5, 4, actor.posZ, 0, 0);
                enemy.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(1000); enemy.setHealth(1000);
                // First half isolates actual unhurt attack swings; the second half enables
                // the opponent AI. Otherwise synchronized hits can hide every swing in hurt.
                enemy.setNoAI(true);
                actor.world.spawnEntity(enemy); actor.setAttackTarget(enemy);
                actorBefore = actor.getHealth(); enemyBefore = enemy.getHealth(); serverStage = "combat";
            } else if (simTicks <= 560) {
                if (simTicks == 401) { enemy.setNoAI(false); enemy.setAttackTarget(actor); }
                if (actor.isSwingInProgress) { serverSwings++; }
                if (actor.hurtTime > 0) { serverHurts++; }
            } else if (simTicks == 561) {
                actorAfter = actor.getHealth(); enemyAfter = enemy.getHealth();
                enemy.setDead(); actor.setAttackTarget(null); actor.setRevengeTarget(null);
                actor.getNavigator().clearPath(); serverStage = "recovery";
            } else if (simTicks == 641) {
                JsonObject s = new JsonObject(); s.addProperty("ticks", simTicks); s.addProperty("walkSpan", maxX - minX);
                s.addProperty("pathTicks", pathTicks); s.addProperty("swings", serverSwings); s.addProperty("hurts", serverHurts);
                s.addProperty("opponentAiEnabledAt", 401);
                s.addProperty("actorBefore", actorBefore); s.addProperty("actorAfter", actorAfter);
                s.addProperty("enemyBefore", enemyBefore); s.addProperty("enemyAfter", enemyAfter);
                serverSummary = s; simulationDone = true;
            }
            serverTicks = simTicks;
        } catch (Exception problem) { serverError = problem.toString(); }
    }

    @SubscribeEvent public void renderTick(TickEvent.RenderTickEvent e) {
        if (finished || !phase.equals("RUN") || npc == null) { return; }
        Minecraft mc = Minecraft.getMinecraft();
        if (e.phase == TickEvent.Phase.END) {
            if (pendingShot != null) {
                String name = out.getName().replace(".proof.json", "") + "-world-" + pendingShot + ".png";
                ScreenShotHelper.saveScreenshot(out.getParentFile(), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
                screenshots.add("screenshots/" + name); pendingShot = null;
            }
            return;
        }
        offers = posts = 0; frameDraws = 0;
        // Only the camera follows the subject. The server still owns all NPC movement.
        mc.player.setPosition(npc.posX, 4, npc.posZ - 10);
        mc.player.prevPosX = mc.player.lastTickPosX = mc.player.posX;
        mc.player.prevPosY = mc.player.lastTickPosY = 4;
        mc.player.prevPosZ = mc.player.lastTickPosZ = mc.player.posZ;
        mc.player.motionX = mc.player.motionY = mc.player.motionZ = 0;
        mc.player.rotationYaw = mc.player.prevRotationYaw = 0;
        mc.player.rotationPitch = mc.player.prevRotationPitch = 8;
        mc.player.capabilities.isFlying = true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST) public void pre(RenderLivingEvent.Pre e) {
        if (!finished && phase.equals("RUN") && !sampling && e.getEntity() == npc) {
            offers++; beforeDraws = NpcBatch.draws; frameHurt = npc.hurtTime > 0 || npc.deathTime > 0;
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public void post(RenderLivingEvent.Post e) {
        if (!finished && phase.equals("RUN") && !sampling && e.getEntity() == npc) { posts++; frameDraws += NpcBatch.draws - beforeDraws; }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST) public void last(RenderWorldLastEvent e) {
        if (!finished && phase.equals("DEPTH_PROBE") && npc != null) {
            Minecraft mc = Minecraft.getMinecraft();
            try { depthProbe(mc); }
            catch (Exception | LinkageError problem) { problem.printStackTrace(); error = problem.toString(); }
            finish(mc); return;
        }
        if (finished || !phase.equals("RUN") || npc == null) { return; }
        Minecraft mc = Minecraft.getMinecraft();
        try {
            frames++;
            if (offers != 1 || posts != 1 || frameDraws != (frameHurt ? 0 : 1)) {
                badFrames++;
                if (firstBad.isEmpty()) { firstBad = "tick=" + npc.ticksExisted + " offers=" + offers + " posts=" + posts + " draws=" + frameDraws + " hurt=" + frameHurt; }
            }
            if (GL11.glGetError() != 0) { glErrors++; }
            if (npc.ticksExisted - sampleTick < 3 || simulationDone) { return; }
            sampleTick = npc.ticksExisted;
            String stage = serverStage;
            boolean walking = stage.equals("walk") && npc.limbSwingAmount > 0.05f;
            boolean hurting = stage.equals("combat") && npc.hurtTime > 0;
            boolean swinging = stage.equals("combat") && npc.getSwingProgress(e.getPartialTicks()) > 0 && npc.hurtTime == 0;
            boolean recovering = stage.equals("recovery") && npc.hurtTime == 0 && serverTicks > 580;
            String category = walking ? "walk" : hurting ? "hurt" : swinging ? "swing" : recovering ? "recovery" : "other";
            boolean image = !category.equals("other") && !captured.contains(category);
            boolean controls = walking && !controlsDone;
            String base = out.getPath().replace(".proof.json", "") + "-" + category;
            JsonObject r = sample(mc, e.getPartialTicks(), image ? base : "", controls);
            r.addProperty("stage", stage); r.addProperty("category", category); r.addProperty("tick", npc.ticksExisted);
            r.addProperty("serverTick", serverTicks); r.addProperty("limb", npc.limbSwing); r.addProperty("amount", npc.limbSwingAmount);
            r.addProperty("swing", npc.getSwingProgress(e.getPartialTicks())); r.addProperty("hurt", npc.hurtTime);
            r.addProperty("x", npc.posX); r.addProperty("y", npc.posY); r.addProperty("z", npc.posZ);
            r.addProperty("partial", e.getPartialTicks()); rows.add(r);
            retainFailure(r);
            if (walking) { walk++; } if (swinging) { swing++; } if (hurting) { hurt++; } if (recovering) { recovery++; }
            if (controls) {
                check("pose-control", r.get("poseControl").getAsBoolean(), "5 degrees on live walk");
                check("uv-control", r.get("uvControl").getAsBoolean(), "one texel on live walk");
                controlsDone = r.get("poseControl").getAsBoolean() && r.get("uvControl").getAsBoolean();
                if (!controlsDone) { throw new IllegalStateException("Insensitive live comparison"); }
            }
            if (image) { captured.add(category); pendingShot = category; }
            if (walking) {
                // The live row above always uses the real event partial. Additionally
                // scan the same untouched networked pose at fixed interpolation points:
                // v5 happened not to visit v4's depth failure. No NPC field is assigned,
                // no client tick runs between these passes, and EVERY probe must pass.
                for (float partial : WALK_PARTIALS) {
                    JsonObject probe = sample(mc, partial, "", false);
                    probe.addProperty("tick", npc.ticksExisted); probe.addProperty("serverTick", serverTicks);
                    probe.addProperty("category", "walk-partial-sweep"); probe.addProperty("partial", partial);
                    probe.addProperty("hurt", npc.hurtTime); probes.add(probe); retainFailure(probe);
                }
            }
        } catch (Exception | LinkageError problem) { problem.printStackTrace(); error = problem.toString(); finish(mc); }
    }

    private void retainFailure(JsonObject row) {
        // Complete all AI phases but never remove, average or waive a failed sample.
        if (!row.get("success").getAsBoolean()) {
            allOk = false;
            if (!diagnosedFailure) { System.out.println("M143M first failed sample " + row); }
            diagnosedFailure = true;
        }
    }

    /** Q520: bounded reconstruction around v4's known limb/amount/partial. Unknown
     * previous amount is searched, not presented as a recording or a live pose. No
     * server simulation runs in this mode; all changed client fields restore before
     * returning to the world. A reproduced image failure remains success=false in
     * its row. Overall success here means diagnostic reproduction only. */
    private void depthProbe(Minecraft mc) throws Exception {
        float[] saved = {npc.limbSwing, npc.limbSwingAmount, npc.prevLimbSwingAmount,
            npc.renderYawOffset, npc.prevRenderYawOffset, npc.rotationYawHead, npc.prevRotationYawHead,
            npc.rotationPitch, npc.prevRotationPitch, npc.swingProgress, npc.prevSwingProgress};
        int ticks = npc.ticksExisted;
        boolean reproduced = false, valid = true;
        boolean regression = "1".equals(job.getProperty("depthRegression"));
        boolean accepted = true;
        try {
            npc.ticksExisted = 250;
            npc.limbSwing = 100.62506f; npc.limbSwingAmount = 0.5396589f;
            npc.renderYawOffset = npc.prevRenderYawOffset = 90;
            npc.rotationYawHead = npc.prevRotationYawHead = 90;
            npc.rotationPitch = npc.prevRotationPitch = npc.swingProgress = npc.prevSwingProgress = 0;
            if (regression) {
                // An explicit old-shader control must still fail on the SAME recorded
                // neighbourhood pose. A chance pass on different live inputs cannot
                // establish that the production transform was corrected.
                npc.prevLimbSwingAmount = 0.5396224f;
                rebuildBatch(4);
                diagnosedFailure = true; // this intentional negative is saved separately
                try {
                    legacyControl = sample(mc, 0.099999666f,
                        out.getPath().replace(".proof.json", "") + "-old-control", false);
                } finally { diagnosedFailure = false; }
                check("old-transform-rejected", legacyControl.get("repeatEqual").getAsBoolean()
                    && legacyControl.get("eventsEqual").getAsBoolean() && legacyControl.get("stateEqual").getAsBoolean()
                    && legacyControl.get("glEqual").getAsBoolean() && legacyControl.get("draws").getAsInt() == 1
                    && legacyControl.get("fallbacks").getAsInt() == 0
                    && legacyControl.getAsJsonObject("pixels").get("depthOver").getAsInt() > 0,
                    legacyControl.toString());
                rebuildBatch(0);
            }
            // Last v4 amounts and v6's recorded neighbouring pose put prevAmount
            // near .5396224. Search its float neighbours first, then a wider interval.
            for (int i = 0; i < 146 && (regression || !reproduced); i++) {
                npc.prevLimbSwingAmount = i < 17
                    ? Float.intBitsToFloat(Float.floatToRawIntBits(0.5396224f) + i - 8)
                    : 0.5394589f + (i - 17) * (0.0002f / 128);
                String images = regression && i == 8 ? out.getPath().replace(".proof.json", "") + "-fixed-control" : "";
                JsonObject r = sample(mc, 0.099999666f, images, false);
                r.addProperty("category", "synthetic-v4-neighbourhood");
                r.addProperty("index", i); rows.add(r);
                boolean rowValid = r.get("repeatEqual").getAsBoolean() && r.get("eventsEqual").getAsBoolean()
                    && r.get("stateEqual").getAsBoolean() && r.get("glEqual").getAsBoolean()
                    && r.get("draws").getAsLong() == 1 && r.get("fallbacks").getAsLong() == 0;
                valid &= rowValid;
                accepted &= r.get("success").getAsBoolean();
                reproduced = rowValid && r.getAsJsonObject("pixels").get("depthOver").getAsLong() > 0;
                if (!r.get("success").getAsBoolean()) { diagnosedFailure = true; }
            }
        } finally {
            npc.limbSwing = saved[0]; npc.limbSwingAmount = saved[1]; npc.prevLimbSwingAmount = saved[2];
            npc.renderYawOffset = saved[3]; npc.prevRenderYawOffset = saved[4];
            npc.rotationYawHead = saved[5]; npc.prevRotationYawHead = saved[6];
            npc.rotationPitch = saved[7]; npc.prevRotationPitch = saved[8];
            npc.swingProgress = saved[9]; npc.prevSwingProgress = saved[10]; npc.ticksExisted = ticks;
            if (regression) { rebuildBatch(0); }
        }
        check(regression ? "regression-inputs-valid" : "diagnostic-inputs-valid",
            valid && rows.size() > 0, "synthetic samples=" + rows.size());
        if (regression) {
            check("depth-regression", accepted && rows.size() == 146, "all 146 images must pass unchanged Q475");
        } else {
            check("depth-failure-reproduced", reproduced, "diagnostic only; failed image is retained, package NOT accepted");
        }
    }

    /** Render-thread program rebuild for the isolated shader negative control only. */
    private void rebuildBatch(int fault) throws Exception {
        NpcBatch.testProgramFault = fault;
        java.lang.reflect.Method reset = NpcBatch.class.getDeclaredMethod("resetResources");
        reset.setAccessible(true); reset.invoke(null);
    }

    private JsonObject sample(Minecraft mc, float partial, String images, boolean controls) throws Exception {
        int p = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM), vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int buffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING), client = GL11.glGetInteger(GL13.GL_CLIENT_ACTIVE_TEXTURE);
        int unit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE), mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        NpcBatchCompatHarness.SavedMirrors mirrors = new NpcBatchCompatHarness.SavedMirrors();
        NpcBatchSceneCheck.SavedState state = new NpcBatchSceneCheck.SavedState();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); GL11.glPushClientAttrib(GL11.GL_ALL_CLIENT_ATTRIB_BITS);
        for (int m : new int[] {GL11.GL_PROJECTION, GL11.GL_MODELVIEW}) { GlStateManager.matrixMode(m); GlStateManager.pushMatrix(); }
        Framebuffer fb = null; sampling = true;
        try {
            if (!OpenGlHelper.isFramebufferEnabled()) { throw new IllegalStateException("FBO required"); }
            fb = new Framebuffer(256, 256, true); fb.setFramebufferColor(0, 0, 0, 0);
            return NpcBatchSceneCheck.live(mc, fb, npc, partial, images, controls,
                out.getPath().replace(".proof.json", "") + "-first-failure", !diagnosedFailure);
        } finally {
            sampling = false;
            GL20.glUseProgram(p); NpcBatchCompatHarness.bindVao(vao); GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            GL13.glClientActiveTexture(client);
            for (int m : new int[] {GL11.GL_MODELVIEW, GL11.GL_PROJECTION}) { GlStateManager.matrixMode(m); GlStateManager.popMatrix(); }
            GlStateManager.matrixMode(mode); state.restore(); mirrors.restore();
            GL11.glPopClientAttrib(); GL11.glPopAttrib(); GL13.glActiveTexture(unit);
            mc.getFramebuffer().bindFramebuffer(true); if (fb != null) { fb.deleteFramebuffer(); }
        }
    }

    private void finish(Minecraft mc) {
        if (finished) { return; } finished = true; running = false; CustomNpcs.RwNpcInstancing = 0;
        try {
            JsonObject p = new JsonObject(); p.addProperty("success", allOk && error.isEmpty()); p.addProperty("error", error);
            p.addProperty("diagnosticOnly", "1".equals(job.getProperty("depthProbe")));
            p.addProperty("depthRegression", "1".equals(job.getProperty("depthRegression")));
            p.add("legacyControl", legacyControl);
            p.add("checks", checks); p.add("rows", rows); p.add("probes", probes);
            p.add("screenshots", screenshots); p.add("server", serverSummary);
            p.addProperty("frames", frames); p.addProperty("badFrames", badFrames); p.addProperty("glErrors", glErrors);
            p.addProperty("firstBadFrame", firstBad);
            try (Writer w = Files.newBufferedWriter(out.toPath(), StandardCharsets.UTF_8)) { new GsonBuilder().setPrettyPrinting().create().toJson(p, w); }
            System.out.println("M143M DONE success=" + (allOk && error.isEmpty()));
        } catch (Exception problem) { problem.printStackTrace(); }
        mc.shutdown();
    }
}
