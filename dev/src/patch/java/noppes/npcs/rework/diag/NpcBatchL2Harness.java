package noppes.npcs.rework.diag;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.far.FarRender;
import noppes.npcs.rework.far.ImpostorL2;
import noppes.npcs.rework.render.NpcBatch;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

/** M14.3b3 controller-only live L1/L2 transition proof. One integrated dev client walks the
 * camera frame by frame across the L2 threshold and back at RwNpcInstancing 0/1/0 (after a
 * baking warmup at 1). Every frame each in-view NPC must be drawn exactly once: either its
 * model (RenderLivingEvent.Post) or its impostor (FarRender.DRAWN_IDS), never both or neither.
 * v2 (Q508): events alone are not visibility. The screen framebuffer depth inside each NPC's
 * projected box must also lie in the NPC's own depth range every frame (empty-box negative
 * control), with the world framebuffer bound and pass 0 at every offer.
 * Production decisions are observed, never forced. No cost window, no optional mods. */
@SideOnly(Side.CLIENT)
public final class NpcBatchL2Harness {
    static final String PLAIN = "M143L-plain", ARMOR = "M143L-armor";
    private static final int START = 24, HOLD = 20, MAX_WARM = 1200;
    private static final double STEP = 0.1, NEAR = 8, FAR = 36, NPC_Z = 2.5;
    private static final String[] PASSES = {"warm", "A", "B", "A2"};
    private final Properties job;
    private final File out;
    private final JsonArray checks = new JsonArray(), passes = new JsonArray();
    private final List<String> shots = new ArrayList<String>();
    private String phase = "MENU", error = "";
    private int delay;
    private long since = System.nanoTime();
    private boolean finished, allOk = true;
    private volatile boolean serverDone;
    private volatile String serverError;
    private final EntityNPCInterface[] npcs = new EntityNPCInterface[2];
    private int oldFar, oldStart, oldInstancing;

    // Per-pass camera path (frame index -> z) and per-frame observations.
    private int pass = -1, frame = -1, warmFar, waited;
    private boolean waiting;
    private String pendingShot;
    private double[] path;
    private final int[] offered = new int[2], posts = new int[2];
    private final long[] batchBefore = new long[2], batchDelta = new long[2], fallBefore = new long[2], fallDelta = new long[2],
            modelBefore = new long[2], modelDelta = new long[2];
    private final double[] dist = new double[2];
    private final int[] fboAtPre = new int[2], passAtPre = new int[2];
    // Visibility: a pixel counts when its depth lies in the NPC's own projected depth range.
    private static final double MIN_COVER = 0.2, CONTROL_DX = -3;
    private static final int MIN_PIXELS = 12, FB_BINDING = 0x8CA6;
    private final FloatBuffer mv = BufferUtils.createFloatBuffer(16), proj = BufferUtils.createFloatBuffer(16),
            win = BufferUtils.createFloatBuffer(3), depth = BufferUtils.createFloatBuffer(1 << 20);
    private final IntBuffer viewport = BufferUtils.createIntBuffer(16);
    private double controlMax;
    private final JsonArray shotDiag = new JsonArray();
    private long hookBefore, glErrors;
    private List<String[]> routes;
    private final List<List<String[]>> routesByPass = new ArrayList<List<String[]>>();
    private Stats[] stats;

    private NpcBatchL2Harness(Properties p) { job = p; out = new File(p.getProperty("out")); }

    static boolean installIfRequested(String path) throws Exception {
        Properties p = new Properties();
        try (InputStreamReader in = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)) { p.load(in); }
        if (!"1".equals(p.getProperty("npcBatchL2"))) { return false; }
        if (!p.containsKey("out") || !p.containsKey("world")) { throw new IllegalArgumentException("Incomplete L2 transition job"); }
        MinecraftForge.EVENT_BUS.register(new NpcBatchL2Harness(p));
        return true;
    }

    private void check(String key, boolean ok, String detail) {
        JsonObject c = new JsonObject(); c.addProperty("key", key); c.addProperty("ok", ok); c.addProperty("detail", detail);
        checks.add(c); allOk &= ok;
        System.out.println("M143L CHECK " + key + " ok=" + ok + " " + detail);
    }

    private void enter(String next, int wait) { phase = next; delay = wait; since = System.nanoTime(); }

    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e) {
        if (finished || e.phase != TickEvent.Phase.END) { return; }
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (serverError != null) { throw new IllegalStateException(serverError); }
            if (System.nanoTime() - since > 300000000000L) { throw new IllegalStateException("Timeout " + phase); }
            if (delay-- > 0) { return; }
            switch (phase) {
                case "MENU":
                    if (!(mc.currentScreen instanceof GuiMainMenu)) { break; }
                    mc.gameSettings.pauseOnLostFocus = false;
                    CustomNpcs.RwNpcInstancing = 0;
                    enter("JOIN", 20);
                    mc.launchIntegratedServer(job.getProperty("world"), job.getProperty("world"),
                            new WorldSettings(HarnessJob.seedOf("customnpc-rework"), GameType.CREATIVE, false, false, WorldType.FLAT).enableCommands());
                    break;
                case "JOIN":
                    if (mc.world == null || mc.player == null || mc.currentScreen != null) { break; }
                    serverDone = false;
                    final String player = mc.player.getName();
                    final MinecraftServer server = mc.getIntegratedServer();
                    server.addScheduledTask(() -> {
                        try {
                            for (String c : new String[] {"gamerule doMobSpawning false", "gamerule doDaylightCycle false",
                                    "time set 6000", "tp " + player + " 1.5 4 " + (NPC_Z - NEAR) + " 0 5",
                                    "summon customnpcs:customnpc 0.5 4 2.5 {Name:\"" + PLAIN + "\",MovingState:0,StandingState:1,Orientation:180,StartPosNew:[I;0,4,2]}",
                                    "summon customnpcs:customnpc 2.5 4 2.5 {Name:\"" + ARMOR + "\",MovingState:0,StandingState:1,Orientation:180,StartPosNew:[I;2,4,2]}"}) {
                                if (server.getCommandManager().executeCommand(server, c) < 1) { throw new IllegalStateException(c); }
                            }
                            boolean armored = false;
                            for (Entity x : server.getWorld(0).loadedEntityList) {
                                if (x instanceof EntityNPCInterface && ARMOR.equals(x.getName())) {
                                    ((EntityNPCInterface) x).setItemStackToSlot(EntityEquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                                    ((EntityNPCInterface) x).updateClient = true;
                                    armored = true;
                                }
                            }
                            if (!armored) { throw new IllegalStateException("armor NPC missing on server"); }
                        } catch (Exception problem) { serverError = problem.toString(); }
                        serverDone = true;
                    });
                    enter("FIND", 60);
                    break;
                case "FIND":
                    if (!serverDone) { break; }
                    for (Entity x : mc.world.loadedEntityList) {
                        if (x instanceof EntityNPCInterface && PLAIN.equals(x.getName())) { npcs[0] = (EntityNPCInterface) x; }
                        if (x instanceof EntityNPCInterface && ARMOR.equals(x.getName())) { npcs[1] = (EntityNPCInterface) x; }
                    }
                    if (npcs[0] == null || npcs[1] == null || npcs[1].getItemStackFromSlot(EntityEquipmentSlot.CHEST).isEmpty()) { break; }
                    // Standing NPCs return to StartPosNew; both must stand on the grass where they were summoned.
                    check("live-npcs", npcs[0].getItemStackFromSlot(EntityEquipmentSlot.CHEST).isEmpty()
                            && mc.world.getEntityByID(npcs[0].getEntityId()) == npcs[0] && mc.world.getEntityByID(npcs[1].getEntityId()) == npcs[1]
                            && at(npcs[0], 0.5) && at(npcs[1], 2.5),
                            "plain=" + npcs[0].getEntityId() + " armor=" + npcs[1].getEntityId() + " chest=" + npcs[1].getItemStackFromSlot(EntityEquipmentSlot.CHEST)
                            + String.format(java.util.Locale.ROOT, " plainPos=(%.2f,%.2f,%.2f) armorPos=(%.2f,%.2f,%.2f)",
                                    npcs[0].posX, npcs[0].posY, npcs[0].posZ, npcs[1].posX, npcs[1].posY, npcs[1].posZ));
                    oldFar = CustomNpcs.RwFarView; oldStart = CustomNpcs.RwImpostorStart; oldInstancing = CustomNpcs.RwNpcInstancing;
                    CustomNpcs.RwFarView = 2; CustomNpcs.RwImpostorStart = START;
                    FarRender.trackIds = true;
                    mc.player.capabilities.isFlying = true; mc.player.sendPlayerAbilities();
                    begin(0);
                    enter("RUN", 0);
                    break;
                case "FINISH":
                    finish(mc);
                    break;
                default: break;
            }
        } catch (Exception | LinkageError problem) { problem.printStackTrace(); error = problem.toString(); finish(mc); }
    }

    /** Pass 0 bakes at RwNpcInstancing=1 by standing beyond the threshold; A/B/A2 walk out and back. */
    private void begin(int next) {
        pass = next; frame = -1;
        CustomNpcs.RwNpcInstancing = PASSES[pass].equals("A") || PASSES[pass].equals("A2") ? 0 : 1;
        List<Double> z = new ArrayList<Double>();
        for (int i = 0; i < HOLD; i++) { z.add(NPC_Z - NEAR); }
        int steps = (int) Math.round((FAR - NEAR) / STEP);
        for (int i = 1; i <= steps; i++) { z.add(NPC_Z - NEAR - i * STEP); }
        for (int i = 0; i < HOLD; i++) { z.add(NPC_Z - FAR); }
        if (pass == 0) { warmFar = z.size() - 1; }
        for (int i = steps - 1; i >= 0; i--) { z.add(NPC_Z - NEAR - i * STEP); }
        for (int i = 0; i < HOLD; i++) { z.add(NPC_Z - NEAR); }
        path = new double[z.size()];
        for (int i = 0; i < path.length; i++) { path[i] = z.get(i); }
        routes = new ArrayList<String[]>();
        stats = new Stats[] {new Stats(), new Stats()};
        hookBefore = FarRender.hookFrames; glErrors = 0; waited = 0; controlMax = 0;
    }

    @SubscribeEvent public void renderTick(TickEvent.RenderTickEvent e) {
        if (finished || !phase.equals("RUN")) { return; }
        Minecraft mc = Minecraft.getMinecraft();
        if (e.phase == TickEvent.Phase.END) {
            // Whole finished frame (world, hand and HUD), never inside the world pass.
            if (pendingShot != null) { shot(mc, pendingShot); pendingShot = null; }
            return;
        }
        if (mc.player == null || mc.world == null) { return; }
        for (int i = 0; i < 2; i++) {
            offered[i] = 0; posts[i] = 0; batchDelta[i] = 0; fallDelta[i] = 0; modelDelta[i] = 0; dist[i] = -1;
            fboAtPre[i] = -1; passAtPre[i] = -2;
        }
        // Warmup holds at the far end (unrecorded frames) until the live pipeline baked both looks.
        waiting = pass == 0 && frame == warmFar && !(baked(0) && baked(1)) && waited < MAX_WARM;
        if (waiting) {
            waited++;
            return;
        }
        frame++;
        if (frame >= path.length) { return; }
        double z = path[frame];
        mc.player.setPosition(1.5, 4, z);
        mc.player.prevPosX = mc.player.lastTickPosX = 1.5;
        mc.player.prevPosY = mc.player.lastTickPosY = 4;
        mc.player.prevPosZ = mc.player.lastTickPosZ = z;
        mc.player.motionX = mc.player.motionY = mc.player.motionZ = 0;
        mc.player.rotationYaw = mc.player.prevRotationYaw = 0;
        mc.player.rotationYawHead = mc.player.prevRotationYawHead = 0;
        mc.player.rotationPitch = mc.player.prevRotationPitch = 5;
        mc.player.capabilities.isFlying = true;
        mc.setRenderViewEntity(mc.player);
    }

    private int index(EntityLivingBase e) { return e == npcs[0] ? 0 : e == npcs[1] ? 1 : -1; }

    private static long fallbacks() { long n = 0; for (long f : NpcBatch.fallbacks()) { n += f; } return n; }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void pre(RenderLivingEvent.Pre<EntityLivingBase> e) {
        int i = index(e.getEntity());
        if (finished || !phase.equals("RUN") || i < 0 || frame < 0 || frame >= path.length || waiting) { return; }
        offered[i]++;
        fboAtPre[i] = GL11.glGetInteger(FB_BINDING); passAtPre[i] = MinecraftForgeClient.getRenderPass();
        Entity view = Minecraft.getMinecraft().getRenderViewEntity();
        dist[i] = view == null ? -1 : e.getEntity().getDistanceSq(view);
        batchBefore[i] = NpcBatch.draws; fallBefore[i] = fallbacks(); modelBefore[i] = NpcBatch.fallbacks()[1];
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void post(RenderLivingEvent.Post<EntityLivingBase> e) {
        int i = index(e.getEntity());
        if (finished || !phase.equals("RUN") || i < 0 || frame < 0 || frame >= path.length || waiting) { return; }
        posts[i]++;
        batchDelta[i] += NpcBatch.draws - batchBefore[i];
        fallDelta[i] += fallbacks() - fallBefore[i];
        modelDelta[i] += NpcBatch.fallbacks()[1] - modelBefore[i];
    }

    private static boolean at(Entity e, double x) {
        return Math.abs(e.posX - x) < 0.05 && Math.abs(e.posY - 4) < 0.05 && Math.abs(e.posZ - NPC_Z) < 0.05;
    }

    private boolean baked(int i) { return npcs[i] != null && FarRender.bakedLook(npcs[i]) != null; }

    /** {cover, pixels, area}: share of the box's screen rectangle whose world depth lies in the box's own depth range. */
    private double[] cover(Minecraft mc, AxisAlignedBB b, double cx, double cy, double cz) {
        double x0 = Double.MAX_VALUE, y0 = Double.MAX_VALUE, x1 = -Double.MAX_VALUE, y1 = -Double.MAX_VALUE;
        double z0 = Double.MAX_VALUE, z1 = -Double.MAX_VALUE;
        for (int k = 0; k < 8; k++) {
            win.clear();
            boolean ok = GLU.gluProject((float) (((k & 1) == 0 ? b.minX : b.maxX) - cx), (float) (((k & 2) == 0 ? b.minY : b.maxY) - cy),
                    (float) (((k & 4) == 0 ? b.minZ : b.maxZ) - cz), mv, proj, viewport, win);
            if (!ok || win.get(2) < 0 || win.get(2) > 1) { return new double[] {0, 0, 0}; }
            x0 = Math.min(x0, win.get(0)); x1 = Math.max(x1, win.get(0)); y0 = Math.min(y0, win.get(1)); y1 = Math.max(y1, win.get(1));
            z0 = Math.min(z0, win.get(2)); z1 = Math.max(z1, win.get(2));
        }
        int vx = viewport.get(0), vy = viewport.get(1), vw = viewport.get(2), vh = viewport.get(3);
        int ix0 = Math.max(vx, (int) Math.floor(x0)), iy0 = Math.max(vy, (int) Math.floor(y0));
        int ix1 = Math.min(vx + vw, (int) Math.ceil(x1)), iy1 = Math.min(vy + vh, (int) Math.ceil(y1));
        int w = ix1 - ix0, h = iy1 - iy0;
        if (w <= 0 || h <= 0 || (long) w * h > depth.capacity()) { return new double[] {0, 0, 0}; }
        int previous = GL11.glGetInteger(FB_BINDING);
        if (OpenGlHelper.isFramebufferEnabled()) { OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, mc.getFramebuffer().framebufferObject); }
        depth.clear();
        GL11.glReadPixels(ix0, iy0, w, h, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depth);
        if (OpenGlHelper.isFramebufferEnabled()) { OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, previous); }
        double pad = (z1 - z0) * 0.25 + 1e-7;
        int n = 0;
        for (int p = 0; p < w * h; p++) { float d = depth.get(p); if (d >= z0 - pad && d <= z1 + pad) { n++; } }
        return new double[] {n / (double) (w * h), n, w * h};
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void last(RenderWorldLastEvent e) {
        if (finished || !phase.equals("RUN") || frame < 0 || frame >= path.length) { return; }
        Minecraft mc = Minecraft.getMinecraft();
        try {
            while (GL11.glGetError() != GL11.GL_NO_ERROR) { glErrors++; }
            if (waiting) { return; }
            // Camera of this world pass (entities are drawn relative to the interpolated view).
            mv.clear(); proj.clear(); viewport.clear();
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, mv); GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, proj);
            GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
            Entity view = mc.getRenderViewEntity();
            float pt = e.getPartialTicks();
            double cx = view.lastTickPosX + (view.posX - view.lastTickPosX) * pt;
            double cy = view.lastTickPosY + (view.posY - view.lastTickPosY) * pt;
            double cz = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt;
            int fboNow = GL11.glGetInteger(FB_BINDING), world = OpenGlHelper.isFramebufferEnabled() ? mc.getFramebuffer().framebufferObject : 0;
            String[] row = new String[2];
            double[][] cover = new double[2][];
            for (int i = 0; i < 2; i++) {
                boolean impostor = FarRender.DRAWN_IDS.contains(npcs[i].getEntityId());
                String route = (posts[i] == 1 && !impostor) ? "model" : (posts[i] == 0 && impostor) ? "impostor"
                        : "bad(posts=" + posts[i] + ",impostor=" + impostor + ")";
                row[i] = route;
                cover[i] = cover(mc, npcs[i].getEntityBoundingBox(), cx, cy, cz);
                String where = String.format(java.util.Locale.ROOT,
                        "pass=%s frame=%d %s cover=%.3f px=%d/%d fboPre=%d fboLast=%d world=%d renderPass=%d cam=(%.2f,%.2f,%.2f) yaw=%.1f pitch=%.1f npc=(%.2f,%.2f,%.2f) invisible=%s",
                        PASSES[pass], frame, route, cover[i][0], (long) cover[i][1], (long) cover[i][2], fboAtPre[i], fboNow,
                        world, passAtPre[i], cx, cy, cz, view.rotationYaw, view.rotationPitch,
                        npcs[i].posX, npcs[i].posY, npcs[i].posZ, npcs[i].isInvisible());
                stats[i].add(this, i, route, mc);
                stats[i].visibility(cover[i], fboAtPre[i], passAtPre[i], world, where);
            }
            controlMax = Math.max(controlMax, cover(mc, npcs[0].getEntityBoundingBox().offset(CONTROL_DX, 0, 0), cx, cy, cz)[0]);
            String[] previous = routes.isEmpty() ? null : routes.get(routes.size() - 1);
            routes.add(row);
            if (pass > 0 && previous != null && !previous[0].equals(row[0])) {
                pendingShot = PASSES[pass] + "-" + (stats[0].enters + stats[0].leaves) + "-" + row[0];
                JsonObject d = new JsonObject(); d.addProperty("shot", pendingShot);
                for (int i = 0; i < 2; i++) { d.addProperty(i == 0 ? "plain" : "armor", String.format(java.util.Locale.ROOT, "%.3f", cover[i][0])); }
                d.addProperty("camera", String.format(java.util.Locale.ROOT, "(%.2f,%.2f,%.2f) yaw=%.1f pitch=%.1f", cx, cy, cz, view.rotationYaw, view.rotationPitch));
                shotDiag.add(d);
            }
            if (frame == path.length - 1) { endPass(); }
        } catch (Exception | LinkageError problem) { problem.printStackTrace(); error = problem.toString(); finish(mc); }
    }

    private void shot(Minecraft mc, String label) {
        String name = out.getName().replace(".proof.json", "") + "-" + label + ".png";
        ScreenShotHelper.saveScreenshot(out.getParentFile(), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        shots.add("screenshots/" + name);
    }

    private void endPass() {
        String p = PASSES[pass];
        long hooks = FarRender.hookFrames - hookBefore;
        JsonObject o = new JsonObject(); o.addProperty("pass", p); o.addProperty("instancing", CustomNpcs.RwNpcInstancing);
        o.addProperty("frames", path.length); o.addProperty("waited", waited); o.addProperty("hookFrames", hooks); o.addProperty("glErrors", glErrors);
        for (int i = 0; i < 2; i++) { o.add(i == 0 ? "plain" : "armor", stats[i].json()); }
        passes.add(o);
        check(p + "-hook-gl", hooks == path.length + waited && glErrors == 0,
                "hookFrames=" + hooks + "/" + (path.length + waited) + " waited=" + waited + " glErrors=" + glErrors);
        if (pass == 0) {
            check("warm-baked", baked(0) && baked(1) && stats[0].impostor > 0 && stats[1].impostor > 0,
                    "plain=" + baked(0) + " armor=" + baked(1) + " impostorFrames=" + stats[0].impostor + "/" + stats[1].impostor);
        } else {
            boolean on = CustomNpcs.RwNpcInstancing == 1;
            for (int i = 0; i < 2; i++) {
                Stats s = stats[i]; String k = p + "-" + (i == 0 ? "plain" : "armor");
                check(k + "-exactly-once", s.bad == 0 && s.offeredBad == 0 && s.model + s.impostor == path.length,
                        "model=" + s.model + " impostor=" + s.impostor + " bad=" + s.bad + " offeredBad=" + s.offeredBad + " first=" + s.firstBad);
                check(k + "-threshold", s.expectedBad == 0 && s.unbaked == 0 && s.enters == 1 && s.leaves == 1
                        && s.enterDist > START && s.enterPrev <= START && s.leaveDist <= START - ImpostorL2.HYSTERESIS
                        && s.leavePrev > START - ImpostorL2.HYSTERESIS && s.bandModel > 0 && s.bandImpostor > 0,
                        "expectedBad=" + s.expectedBad + " unbaked=" + s.unbaked + " enters=" + s.enters + " leaves=" + s.leaves
                        + String.format(java.util.Locale.ROOT, " enter=%.3f(prev %.3f) leave=%.3f(prev %.3f)",
                                s.enterDist, s.enterPrev, s.leaveDist, s.leavePrev)
                        + " bandModel=" + s.bandModel + " bandImpostor=" + s.bandImpostor);
                boolean route;
                if (!on) {
                    route = s.batch == 0 && s.fallback == 0;
                } else if (i == 0) {
                    route = s.batch == s.model && s.fallback == 0 && s.batchBad == 0;
                } else {
                    route = s.batch == 0 && s.fallback == s.model && s.modelRefusals == s.model && s.batchBad == 0;
                }
                check(k + "-route", route, "instancing=" + CustomNpcs.RwNpcInstancing + " modelFrames=" + s.model + " batch=" + s.batch
                        + " fallbacks=" + s.fallback + " modelRefusals=" + s.modelRefusals + " batchBad=" + s.batchBad);
                check(k + "-visible", s.invisible == 0 && s.fboBad == 0 && s.passBad == 0,
                        String.format(java.util.Locale.ROOT, "invisible=%d fboBad=%d passBad=%d minCover=%.3f first=%s",
                                s.invisible, s.fboBad, s.passBad, s.minCover, s.firstInvisible.isEmpty() ? s.firstFrame : s.firstInvisible));
            }
            check(p + "-visible-control", controlMax < MIN_COVER, String.format(java.util.Locale.ROOT, "emptyBoxMaxCover=%.3f", controlMax));
        }
        routesByPass.add(routes);
        if (pass + 1 < PASSES.length) {
            begin(pass + 1);
        } else {
            boolean same = routesByPass.size() == 4;
            for (int q = 2; same && q <= 3; q++) {
                same = routesByPass.get(q).size() == routesByPass.get(1).size();
                for (int f = 0; same && f < routesByPass.get(1).size(); f++) {
                    same = java.util.Arrays.equals(routesByPass.get(1).get(f), routesByPass.get(q).get(f));
                }
            }
            check("route-sequence-A-B-A2", same, "frames=" + routesByPass.get(1).size());
            check("screenshots", shots.size() == 6, "count=" + shots.size());
            enter("FINISH", 0);
        }
    }

    private void finish(Minecraft mc) {
        if (finished) { return; }
        finished = true;
        try {
            CustomNpcs.RwNpcInstancing = 0;
            if (pass >= 0) { CustomNpcs.RwFarView = oldFar; CustomNpcs.RwImpostorStart = oldStart; }
            FarRender.trackIds = false;
            JsonObject proof = new JsonObject(); boolean ok = allOk && error.isEmpty();
            proof.addProperty("success", ok); proof.addProperty("error", error);
            proof.addProperty("start", START); proof.addProperty("hysteresis", ImpostorL2.HYSTERESIS);
            proof.addProperty("restoredInstancing", oldInstancing);
            proof.add("checks", checks); proof.add("passes", passes);
            JsonArray s = new JsonArray(); for (String n : shots) { s.add(n); } proof.add("screenshots", s);
            proof.add("shotDiag", shotDiag);
            try (Writer writer = Files.newBufferedWriter(out.toPath(), StandardCharsets.UTF_8)) { new GsonBuilder().setPrettyPrinting().create().toJson(proof, writer); }
            System.out.println("M143L DONE success=" + ok);
        } catch (Exception problem) { problem.printStackTrace(); }
        mc.shutdown();
    }

    /** Per NPC and pass: routes, threshold replica (ImpostorL2 with hysteresis) and batch counters. */
    private static final class Stats {
        int model, impostor, bad, offeredBad, expectedBad, unbaked, enters, leaves, bandModel, bandImpostor, batchBad;
        int invisible, fboBad, passBad;
        long batch, fallback, modelRefusals;
        double enterDist, enterPrev, leaveDist, leavePrev, prevDist = -1, minCover = Double.MAX_VALUE;
        boolean in, prevImpostor;
        String firstBad = "", firstInvisible = "", firstFrame = "";

        void visibility(double[] c, int fbo, int renderPass, int world, String where) {
            if (firstFrame.isEmpty()) { firstFrame = where; }
            minCover = Math.min(minCover, c[0]);
            boolean seen = c[0] >= MIN_COVER && c[1] >= MIN_PIXELS;
            if (!seen) { invisible++; if (firstInvisible.isEmpty()) { firstInvisible = where; } }
            if (fbo != world) { fboBad++; if (firstInvisible.isEmpty()) { firstInvisible = where; } }
            if (renderPass != 0) { passBad++; if (firstInvisible.isEmpty()) { firstInvisible = where; } }
        }

        void add(NpcBatchL2Harness h, int i, String route, Minecraft mc) {
            double d = Math.sqrt(Math.max(0, h.dist[i]));
            if (h.offered[i] != 1) { offeredBad++; if (firstBad.isEmpty()) { firstBad = "frame " + h.frame + " offered=" + h.offered[i]; } }
            if (route.equals("model")) { model++; } else if (route.equals("impostor")) { impostor++; }
            else { bad++; if (firstBad.isEmpty()) { firstBad = "frame " + h.frame + " " + route; } }
            boolean imp = route.equals("impostor");
            in = ImpostorL2.next(in, h.dist[i], START);
            if (!h.baked(i)) { unbaked++; }
            if (imp != in) { expectedBad++; }
            if (h.frame > 0 && imp && !prevImpostor) { enters++; enterDist = d; enterPrev = prevDist; }
            if (h.frame > 0 && !imp && prevImpostor) { leaves++; leaveDist = d; leavePrev = prevDist; }
            if (d > START - ImpostorL2.HYSTERESIS && d <= START) { if (imp) { bandImpostor++; } else { bandModel++; } }
            batch += h.batchDelta[i]; fallback += h.fallDelta[i]; modelRefusals += h.modelDelta[i];
            if (h.batchDelta[i] > 1 || (imp && (h.batchDelta[i] != 0 || h.fallDelta[i] != 0))) { batchBad++; }
            prevImpostor = imp; prevDist = d;
        }

        JsonObject json() {
            JsonObject o = new JsonObject();
            o.addProperty("model", model); o.addProperty("impostor", impostor); o.addProperty("bad", bad);
            o.addProperty("offeredBad", offeredBad); o.addProperty("expectedBad", expectedBad); o.addProperty("unbaked", unbaked);
            o.addProperty("enters", enters); o.addProperty("leaves", leaves); o.addProperty("enterDist", enterDist);
            o.addProperty("enterPrev", enterPrev); o.addProperty("leaveDist", leaveDist); o.addProperty("leavePrev", leavePrev);
            o.addProperty("bandModel", bandModel); o.addProperty("bandImpostor", bandImpostor);
            o.addProperty("batch", batch); o.addProperty("fallbacks", fallback); o.addProperty("modelRefusals", modelRefusals);
            o.addProperty("batchBad", batchBad); o.addProperty("firstBad", firstBad);
            o.addProperty("invisible", invisible); o.addProperty("fboBad", fboBad); o.addProperty("passBad", passBad);
            o.addProperty("minCover", minCover); o.addProperty("firstInvisible", firstInvisible); o.addProperty("firstFrame", firstFrame);
            return o;
        }
    }
}
