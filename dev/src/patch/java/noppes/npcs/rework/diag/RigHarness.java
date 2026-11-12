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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.anim.Animators;
import noppes.npcs.rework.anim.IAnimator;
import noppes.npcs.rework.rig.RigApi;
import noppes.npcs.rework.rig.client.RigClient;

/**
 * M16 controller-only acceptance of the built-in animation core on a real integrated dev client
 * (RwAnimCore=1, no customnpcentities): NPCs summoned with only NBT RwRig are drawn as their models,
 * action clips reach the client, a data-only {@code hit} event hurts a zombie, the rig and an NPC's own
 * clips survive the NBT round trip a clone makes, and an own clip plays. Two screenshots.
 */
@SideOnly(Side.CLIENT)
public final class RigHarness {
    private final Properties job;
    private final String[] MODELS;
    private final File out;
    private final JsonArray checks = new JsonArray();
    private final JsonArray screenshots = new JsonArray();
    private final JsonObject data = new JsonObject();
    private final List<Integer> ids = new ArrayList<Integer>();
    private volatile String taskError;
    private volatile boolean serverDone;
    private volatile int zombieId = -1;
    private volatile float zombieBefore, zombieAfter = -1;
    private volatile String cloneModel = "", cloneClips = "", ownPlayed = "", ownErrors = "";
    private volatile int trollId = -1;
    private String syncedClip = "";
    private volatile int knightId = -1, bossZombie = -1;
    private volatile boolean knightScript;
    private volatile String knightModel = "", bossClips = "";
    private volatile float bossBefore, bossAfter = -1;
    private String phase = "MENU";
    private long since = System.nanoTime();
    private int delay;
    private boolean finished;
    private int framesAtStart;

    private RigHarness(Properties job) {
        this.job = job;
        out = new File(job.getProperty("out"));
        MODELS = job.getProperty("models", "troll,warg,giant_spider,cave_bear").split(",");
    }

    static boolean installIfRequested(String path) throws Exception {
        Properties p = new Properties();
        try (InputStreamReader in = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)) { p.load(in); }
        if (!"1".equals(p.getProperty("rigCore"))) { return false; }
        if (!p.containsKey("out") || !p.containsKey("world")) { throw new IllegalArgumentException("Incomplete rigCore job"); }
        MinecraftForge.EVENT_BUS.register(new RigHarness(p));
        System.out.println("M16 INSTALL world=" + p.getProperty("world"));
        return true;
    }

    private void check(String key, boolean ok, String detail) {
        JsonObject row = new JsonObject();
        row.addProperty("key", key); row.addProperty("ok", ok); row.addProperty("detail", detail);
        checks.add(row);
        System.out.println("M16 CHECK " + key + " ok=" + ok + " " + detail);
        if (!ok) { throw new IllegalStateException(key + ": " + detail); }
    }

    private void enter(String next, int wait) {
        phase = next; since = System.nanoTime(); delay = wait;
        System.out.println("M16 PHASE " + phase);
    }

    private void submit(Minecraft mc, Runnable work) {
        serverDone = false;
        mc.getIntegratedServer().addScheduledTask(() -> {
            try { work.run(); } catch (Exception | LinkageError problem) { problem.printStackTrace(); taskError = problem.toString(); }
            serverDone = true;
        });
    }

    private static List<EntityNPCInterface> rigged(net.minecraft.world.World w) {
        List<EntityNPCInterface> out = new ArrayList<EntityNPCInterface>();
        for (Entity x : w.loadedEntityList) {
            if (x instanceof EntityNPCInterface && ((EntityNPCInterface) x).rwRig.active()) { out.add((EntityNPCInterface) x); }
        }
        out.sort((a, b) -> Double.compare(a.posX, b.posX));
        return out;
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || finished) { return; }
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (taskError != null) { throw new IllegalStateException(taskError); }
            if (System.nanoTime() - since > 120000000000L) { throw new IllegalStateException("Timeout " + phase); }
            if (delay-- > 0) { return; }
            switch (phase) {
                case "MENU":
                    if (!(mc.currentScreen instanceof GuiMainMenu)) { break; }
                    mc.gameSettings.pauseOnLostFocus = false;
                    enter("JOIN", 10);
                    mc.launchIntegratedServer(job.getProperty("world"), job.getProperty("world"),
                            new WorldSettings(HarnessJob.seedOf("customnpc-rework"), GameType.CREATIVE, false, false, WorldType.FLAT).enableCommands());
                    break;
                case "JOIN": {
                    if (mc.world == null || mc.player == null || mc.currentScreen != null) { break; }
                    final String player = mc.player.getName();
                    mc.gameSettings.hideGUI = true; // clean screenshots
                    submit(mc, () -> {
                        MinecraftServer s = mc.getIntegratedServer();
                        for (int cx = -2; cx <= 2; cx++) { for (int cz = -1; cz <= 2; cz++) { s.getWorld(0).getChunkFromChunkCoords(cx, cz); } }
                        List<String> cmds = new ArrayList<String>();
                        for (String c : new String[] {"setworldspawn 0 4 0", "gamemode 1 " + player, "gamerule doMobSpawning false",
                                "gamerule doDaylightCycle false", "time set 6000", "tp " + player + " 0 7 -9 0 12"}) { cmds.add(c); }
                        for (int i = 0; i < MODELS.length; i++) {
                            int x = -((MODELS.length - 1) * 5) / 2 + i * 5;
                            cmds.add("summon customnpcs:customnpc " + x + " 4 10 {Name:\"rig_" + MODELS[i] + "\",MovingState:0,StandingState:1,Orientation:180,"
                                    + "RwRig:{Model:\"" + MODELS[i] + "\"}}");
                        }
                        for (String c : cmds) {
                            if (s.getCommandManager().executeCommand(s, c) < 1) { throw new IllegalStateException("Command failed: " + c); }
                        }
                    });
                    enter("FIND", 60);
                    break;
                }
                case "FIND": {
                    List<EntityNPCInterface> server = rigged(mc.getIntegratedServer().getWorld(0));
                    List<EntityNPCInterface> client = rigged(mc.world);
                    check("server-rigs", server.size() == MODELS.length, "server=" + server.size());
                    check("client-rigs", client.size() == MODELS.length, "client=" + client.size());
                    StringBuilder missing = new StringBuilder();
                    for (EntityNPCInterface npc : client) {
                        if (!RigClient.active(npc)) { missing.append(npc.rwRig.model).append(' '); }
                        ids.add(npc.getEntityId());
                        if (npc.rwRig.model.equals("troll")) { trollId = npc.getEntityId(); }
                    }
                    check("client-meshes", missing.length() == 0, "without mesh: " + missing);
                    framesAtStart = RigClient.frames;
                    enter("SHOT_IDLE", 40);
                    break;
                }
                case "SHOT_IDLE":
                    check("rig-frames", RigClient.frames - framesAtStart > 40 * 4, "frames=" + (RigClient.frames - framesAtStart));
                    shot(mc, "m16-rig-idle.png");
                    submit(mc, () -> {
                        WorldServer w = mc.getIntegratedServer().getWorld(0);
                        EntityNPCInterface troll = (EntityNPCInterface) w.getEntityByID(trollId);
                        EntityZombie z = new EntityZombie(w);
                        z.setLocationAndAngles(troll.posX, troll.posY, troll.posZ - 4, 0, 0);
                        z.setNoAI(true);
                        w.spawnEntity(z);
                        zombieId = z.getEntityId();
                        zombieBefore = z.getHealth();
                        troll.rotationYaw = troll.rotationYawHead = troll.renderYawOffset = 180; // faces -Z, toward the zombie
                        IAnimator a = Animators.of(troll.wrappedNPC);
                        if (!a.isAvailable() || !a.play("smash")) { throw new IllegalStateException("troll smash not played"); }
                        for (EntityNPCInterface n : rigged(w)) {
                            if (n == troll) { continue; }
                            List<String> clips = n.rwRig.library().sortedClipNames();
                            for (String c : clips) {
                                if (!c.equals("idle") && !c.equals("walk") && !c.equals("hurt") && !c.equals("death")) { Animators.of(n.wrappedNPC).play(c); break; }
                            }
                        }
                    });
                    enter("ACTION", 12);
                    break;
                case "ACTION": {
                    Entity t = mc.world.getEntityByID(trollId);
                    String clip = t instanceof EntityNPCInterface ? ((EntityNPCInterface) t).rwRigPlayer.clip() : "?";
                    check("client-clip-synced", clip.equals("smash"), "troll client clip=" + clip);
                    shot(mc, "m16-rig-attack.png");
                    enter("HIT", 50);
                    break;
                }
                case "HIT":
                    submit(mc, () -> {
                        WorldServer w = mc.getIntegratedServer().getWorld(0);
                        Entity z = w.getEntityByID(zombieId);
                        zombieAfter = z instanceof EntityZombie ? ((EntityZombie) z).getHealth() : 0;
                        EntityNPCInterface troll = (EntityNPCInterface) w.getEntityByID(trollId);
                        // Clone round trip: the saved NBT makes a new NPC that wears the same rig and own clips.
                        String own = "{\"animations\":{\"wave\":{\"animation_length\":3.0,\"bones\":{\"armL\":{\"rotation\":{\"0.0\":[0,0,0],\"0.5\":[-150,0,-20],\"3.0\":[0,0,0]}}},"
                                + "\"events\":[{\"t\":0.5,\"type\":\"sound\",\"sound\":\"troll.roar\"}]}}}";
                        String[] problems = RigApi.setClips(troll.wrappedNPC, own);
                        ownErrors = String.join(";", problems);
                        NBTTagCompound tag = new NBTTagCompound();
                        troll.writeToNBTOptional(tag);
                        Entity copy = EntityList.createEntityFromNBT(tag, w);
                        if (copy instanceof EntityNPCInterface) {
                            cloneModel = ((EntityNPCInterface) copy).rwRig.model;
                            cloneClips = String.join(",", ((EntityNPCInterface) copy).rwRig.library().sortedClipNames());
                        }
                        ownPlayed = Animators.of(troll.wrappedNPC).play("wave") ? Animators.of(troll.wrappedNPC).getClip() : "not played";
                    });
                    enter("SYNC", 6);
                    break;
                case "SYNC": {
                    if (!serverDone) { break; }
                    Entity t = mc.world.getEntityByID(trollId);
                    syncedClip = t instanceof EntityNPCInterface ? ((EntityNPCInterface) t).rwRigPlayer.clip() : "?";
                    enter("CHECK", 0);
                    break;
                }
                case "CHECK":
                    check("hit-event-damage", zombieAfter >= 0 && zombieAfter < zombieBefore, "zombie " + zombieBefore + " -> " + zombieAfter);
                    check("own-clips-valid", ownErrors.isEmpty(), "problems=" + ownErrors);
                    check("clone-keeps-rig", cloneModel.equals("troll"), "model=" + cloneModel);
                    check("clone-keeps-own-clips", cloneClips.contains("wave") && cloneClips.contains("smash"), "clips=" + cloneClips);
                    check("own-clip-plays", ownPlayed.equals("wave"), "playing=" + ownPlayed);
                    check("own-clip-synced", syncedClip.equals("wave"), "client clip=" + syncedClip);
                    if (job.getProperty("package", "").isEmpty()) {
                        data.addProperty("frames", RigClient.frames);
                        finish(mc, true, "");
                        break;
                    }
                    submit(mc, () -> {
                        try {
                            File src = new File(job.getProperty("package"));
                            File dir = noppes.npcs.rework.rig.RigModels.folder();
                            dir.mkdirs();
                            Files.copy(src.toPath(), new File(dir, src.getName()).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            MinecraftServer s = mc.getIntegratedServer();
                            if (s.getCommandManager().executeCommand(s, "rwrig apply rig_knight " + src.getName()) < 1) { throw new IllegalStateException("apply failed"); }
                            WorldServer w = s.getWorld(0);
                            EntityNPCInterface knight = null;
                            for (EntityNPCInterface n : rigged(w)) { if (n.getName().equals("Sir Varn")) { knight = n; } }
                            if (knight == null) { throw new IllegalStateException("no Sir Varn after apply"); }
                            knightId = knight.getEntityId();
                            knightScript = knight.script.isEnabled();
                            knightModel = knight.rwRig.model + "@" + knight.rwRig.scale;
                            EntityZombie z = new EntityZombie(w);
                            z.setLocationAndAngles(knight.posX, knight.posY, knight.posZ - 2.5, 0, 0);
                            z.setNoAI(true);
                            w.spawnEntity(z);
                            bossZombie = z.getEntityId();
                            bossBefore = z.getHealth();
                            knight.setAttackTarget(z);
                        } catch (java.io.IOException io) { throw new IllegalStateException(io); }
                    });
                    enter("BOSS", 120);
                    break;
                case "BOSS":
                    if (!serverDone) { break; }
                    submit(mc, () -> {
                        WorldServer w = mc.getIntegratedServer().getWorld(0);
                        Entity z = w.getEntityByID(bossZombie);
                        bossAfter = z instanceof EntityZombie ? ((EntityZombie) z).getHealth() : 0;
                        EntityNPCInterface knight = (EntityNPCInterface) w.getEntityByID(knightId);
                        bossClips = knight == null ? "" : String.join(",", knight.rwRig.library().sortedClipNames());
                    });
                    shot(mc, "m16-knight-boss.png");
                    enter("BOSS_CHECK", 10);
                    break;
                case "BOSS_CHECK":
                    if (!serverDone) { break; }
                    check("package-applied", knightModel.equals("knight@1.25") && knightScript, "model=" + knightModel + " script=" + knightScript);
                    check("package-own-clip", bossClips.contains("taunt") && bossClips.contains("leap_slam"), "clips=" + bossClips);
                    check("js-attack-damage", bossAfter >= 0 && bossAfter < bossBefore, "zombie " + bossBefore + " -> " + bossAfter);
                    data.addProperty("frames", RigClient.frames);
                    finish(mc, true, "");
                    break;
                default:
                    break;
            }
        } catch (Exception | LinkageError problem) {
            problem.printStackTrace();
            finish(mc, false, problem.toString());
        }
    }

    private void shot(Minecraft mc, String name) {
        ScreenShotHelper.saveScreenshot(out.getParentFile(), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        if (!new File(new File(out.getParentFile(), "screenshots"), name).isFile()) { throw new IllegalStateException("Missing screenshot"); }
        screenshots.add(name);
    }

    private void finish(Minecraft mc, boolean ok, String error) {
        finished = true;
        try {
            if (mc.world != null) { mc.world.sendQuittingDisconnectingPacket(); mc.loadWorld(null); }
            mc.displayGuiScreen(new GuiMainMenu());
            JsonObject proof = new JsonObject();
            proof.addProperty("success", ok); proof.addProperty("error", error);
            proof.add("checks", checks); proof.add("screenshots", screenshots); proof.add("data", data);
            try (Writer writer = Files.newBufferedWriter(out.toPath(), StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(proof, writer);
            }
            System.out.println("M16 DONE success=" + ok);
        } catch (Exception problem) { problem.printStackTrace(); }
        mc.shutdown();
    }
}
