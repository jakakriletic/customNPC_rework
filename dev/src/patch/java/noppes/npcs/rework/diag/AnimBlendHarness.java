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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
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
import noppes.npcs.rework.anim.AnimBlend;
import noppes.npcs.rework.anim.Animators;
import noppes.npcs.rework.anim.IAnimator;
import noppes.npcs.rework.anim.PoseBlend;

/** P7b controller-only acceptance of IAnimator.blendTo on a real integrated client with
 * customnpcentities 0.7.0 (RwAnimHooks=1). Two cave bears run the same timeline; bear A switches with
 * play, bear B with blendTo. Primary evidence are the numeric poses the patched AnimatedModel.buildPose
 * returns (AnimBlend probe): the copied previous pose, linear mixing, start/middle/end, replacement
 * during a blend and death. API guards run on the server thread; one off-thread call from the client
 * thread. Script actions only through the public Animators API. */
@SideOnly(Side.CLIENT)
public final class AnimBlendHarness {
    static final class Sample {
        final int token, age, deathTime; final float w; final String state;
        final Map<String, float[]> from, built, out;
        Sample(int token, int age, int deathTime, float w, String state, Map<String, float[]> from, Map<String, float[]> built, Map<String, float[]> out) {
            this.token = token; this.age = age; this.deathTime = deathTime; this.w = w; this.state = state;
            this.from = from; this.built = built; this.out = out;
        }
    }

    private final Properties job;
    private final File out;
    private final JsonArray checks = new JsonArray();
    private final JsonArray screenshots = new JsonArray();
    private final JsonObject data = new JsonObject();
    private final Map<Integer, List<Sample>> samples = new HashMap<>();
    private final Map<String, Integer> tokens = Collections.synchronizedMap(new HashMap<String, Integer>());
    private final Map<String, Boolean> results = Collections.synchronizedMap(new HashMap<String, Boolean>());
    private volatile int idA = -1, idB = -1;
    private volatile String taskError;
    private String phase = "MENU";
    private long since = System.nanoTime();
    private int delay, step;
    private boolean finished;

    private AnimBlendHarness(Properties job) {
        this.job = job;
        out = new File(job.getProperty("out"));
    }

    static boolean installIfRequested(String path) throws Exception {
        Properties p = new Properties();
        try (InputStreamReader in = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)) { p.load(in); }
        if (!"1".equals(p.getProperty("animBlend"))) { return false; }
        if (!p.containsKey("out") || !p.containsKey("world")) { throw new IllegalArgumentException("Incomplete animBlend job"); }
        MinecraftForge.EVENT_BUS.register(new AnimBlendHarness(p));
        System.out.println("P7B INSTALL world=" + p.getProperty("world"));
        return true;
    }

    private void check(String key, boolean ok, String detail) {
        JsonObject row = new JsonObject();
        row.addProperty("key", key); row.addProperty("ok", ok); row.addProperty("detail", detail);
        checks.add(row);
        System.out.println("P7B CHECK " + key + " ok=" + ok + " " + detail);
        if (!ok) { throw new IllegalStateException(key + ": " + detail); }
    }

    private void enter(String next, int wait) {
        phase = next; since = System.nanoTime(); delay = wait;
        System.out.println("P7B PHASE " + phase);
    }

    /** Timeline after the bears are visible: {server action, client ticks to wait after it}. */
    private Object[][] timeline() {
        return new Object[][]{
            {"c1-roar", 20}, {"c1-switch", 3}, {"shot", 40},
            {"c2-roar", 20}, {"c2-swipe", 4}, {"c2-maul", 30},
            {"guards", 5}, {"c3-roar", 20}, {"c3-charge", 5}, {"c3-kill", 40}, {"end", 0}};
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
                case "JOIN":
                    if (mc.world == null || mc.player == null || mc.currentScreen != null) { break; }
                    final String player = mc.player.getName();
                    submit(mc, () -> {
                        MinecraftServer s = mc.getIntegratedServer();
                        // The player joins far from 0,0; the teleport loads its chunks only on a later tick and
                        // summon refuses unloaded positions ("out of the world"), so load the bear chunks first.
                        s.getWorld(0).getChunkFromChunkCoords(-1, 0);
                        s.getWorld(0).getChunkFromChunkCoords(0, 0);
                        for (String c : new String[]{"setworldspawn 0 4 0", "gamemode 1 " + player, "gamerule doMobSpawning false",
                                "gamerule doDaylightCycle false", "time set 6000", "tp " + player + " 0 4 0 0 15",
                                "summon customnpcentities:cave_bear -2 4 7", "summon customnpcentities:cave_bear 2 4 7"}) {
                            if (s.getCommandManager().executeCommand(s, c) < 1) { throw new IllegalStateException("Command failed: " + c); }
                        }
                    });
                    enter("FIND", 40);
                    break;
                case "FIND":
                    submit(mc, () -> {
                        List<EntityNPCInterface> bears = new ArrayList<>();
                        for (Entity x : mc.getIntegratedServer().getWorld(0).loadedEntityList) {
                            if (x instanceof EntityNPCInterface && Animators.of(((EntityNPCInterface) x).wrappedNPC).isAvailable()) { bears.add((EntityNPCInterface) x); }
                        }
                        if (bears.size() != 2) { throw new IllegalStateException("bears=" + bears.size()); }
                        bears.sort((a, b) -> Double.compare(a.posX, b.posX));
                        // Standing in view; locomotion is not an action and does not change the token.
                        for (EntityNPCInterface bear : bears) { bear.wrappedNPC.getAi().setMovingType(0); }
                        idA = bears.get(0).getEntityId(); idB = bears.get(1).getEntityId();
                    });
                    enter("VISIBLE", 20);
                    break;
                case "VISIBLE":
                    if (idA < 0 || idB < 0) { break; }
                    if (samples.isEmpty()) {
                        AnimBlend.setProbe((npc, token, age, partial, w, state, from, built, pose) -> {
                            if (npc.getEntityId() != idA && npc.getEntityId() != idB) { return; }
                            samples.computeIfAbsent(npc.getEntityId(), k -> new ArrayList<>()).add(new Sample(token, age,
                                    ((EntityLivingBase) npc).deathTime, w, state, from, built, pose));
                        });
                        samples.put(idA, new ArrayList<>()); samples.put(idB, new ArrayList<>());
                        check("blend-class-patched", "1".equals(System.getProperty("rwcore.blendpatch")), "rwcore.blendpatch=" + System.getProperty("rwcore.blendpatch"));
                        check("journal-class-patched", "1".equals(System.getProperty("rwcore.animpatch")), "rwcore.animpatch=" + System.getProperty("rwcore.animpatch"));
                        enter("VISIBLE", 40);
                        break;
                    }
                    check("bears-rendered", samples.get(idA).size() > 5 && samples.get(idB).size() > 5,
                            "frames A=" + samples.get(idA).size() + " B=" + samples.get(idB).size());
                    int[] c = AnimBlend.counts();
                    data.addProperty("acceptedBefore", c[0]); data.addProperty("rejectedBefore", c[1]);
                    enter("STEP", 0);
                    break;
                case "STEP":
                    Object[][] t = timeline();
                    if (step >= t.length) { break; }
                    String name = (String) t[step][0];
                    int wait = (Integer) t[step][1];
                    step++;
                    if (name.equals("shot")) { snapshot(mc, "switch"); }
                    else if (name.equals("guards")) { guards(mc); }
                    else if (name.equals("end")) { evaluate(mc); finish(mc, true, ""); return; }
                    else { final String action = name; submit(mc, () -> server(mc, action)); }
                    enter("STEP", wait);
                    break;
                default:
                    break;
            }
        } catch (Exception | LinkageError problem) { problem.printStackTrace(); finish(mc, false, problem.toString()); }
    }

    private EntityNPCInterface bear(Minecraft mc, int id) {
        WorldServer w = mc.getIntegratedServer().getWorld(0);
        return (EntityNPCInterface) w.getEntityByID(id);
    }

    /** Server thread. */
    private void server(Minecraft mc, String action) {
        IAnimator a = Animators.of(bear(mc, idA).wrappedNPC), b = Animators.of(bear(mc, idB).wrappedNPC);
        boolean ok;
        switch (action) {
            case "c1-roar": ok = a.play("rear_roar") && b.play("rear_roar"); break;
            case "c1-switch": ok = a.play("bite") && b.blendTo("bite", 10); break;
            case "c2-roar": ok = b.play("rear_roar"); break;
            case "c2-swipe": ok = b.blendTo("swipe", 12); break;
            case "c2-maul": ok = b.blendTo("maul", 10); break;
            case "c3-roar": ok = b.play("rear_roar"); break;
            case "c3-charge": ok = b.blendTo("charge", 30); break;
            case "c3-kill": bear(mc, idB).setHealth(0F); ok = true; break;
            default: throw new IllegalStateException(action);
        }
        results.put(action, ok);
        tokens.put(action + "-A", tokenOf(bear(mc, idA)));
        tokens.put(action + "-B", tokenOf(bear(mc, idB)));
        System.out.println("P7B SERVER " + action + " ok=" + ok + " clipB=" + b.getClip());
    }

    private static int tokenOf(EntityNPCInterface npc) { return AnimBlend.token(npc); }

    /** Argument, thread and play-equivalence guards: false without change; 0 ticks is a play without a message. */
    private void guards(Minecraft mc) throws Exception {
        final IAnimator offThread = Animators.of(bear(mc, idB).wrappedNPC);
        check("off-thread-false", !offThread.blendTo("bite", 5), "client thread call");
        final int[] before = AnimBlend.counts();
        mc.getIntegratedServer().addScheduledTask(() -> {
            IAnimator b = Animators.of(bear(mc, idB).wrappedNPC);
            String clip = b.getClip();
            int token = tokenOf(bear(mc, idB));
            boolean bad = b.blendTo("bite", -1) || b.blendTo("bite", 201) || b.blendTo("no_such_clip", 5) || b.blendTo(null, 5);
            results.put("bad-args-false", !bad && clip.equals(b.getClip()) && token == tokenOf(bear(mc, idB)));
            results.put("zero-ticks-plays", b.blendTo("hurt", 0) && "hurt".equals(b.getClip()));
            results.put("absent-handle-false", !Animators.of(null).blendTo("bite", 5));
        }).get();
        check("bad-args-false", Boolean.TRUE.equals(results.get("bad-args-false")), "-1, 201, unknown, null: false and clip/token unchanged");
        check("zero-ticks-plays", Boolean.TRUE.equals(results.get("zero-ticks-plays")), "blendTo(hurt,0) == play");
        check("absent-handle-false", Boolean.TRUE.equals(results.get("absent-handle-false")), "inert handle");
        data.addProperty("acceptedBeforeGuards", before[0]);
    }

    private List<Sample> withToken(List<Sample> all, int token) {
        List<Sample> r = new ArrayList<>();
        for (Sample s : all) { if (s.token == token) { r.add(s); } }
        return r;
    }

    private Sample before(List<Sample> all, Sample first) { int i = all.indexOf(first); return i > 0 ? all.get(i - 1) : null; }

    private static double d(Map<String, float[]> a, Map<String, float[]> b) { return PoseBlend.distance(a, b); }

    /** Linear mixing on every blended frame: |out-built| = (1-w)|from-built| for each L1 distance. */
    private String linear(List<Sample> frames) {
        double worst = 0;
        int n = 0;
        for (Sample s : frames) {
            if (s.from == null || s.w < 0) { continue; }
            double full = d(s.from, s.built), err = Math.abs(d(s.out, s.built) - (1 - s.w) * full) / Math.max(1, full);
            worst = Math.max(worst, err); n++;
        }
        return n + ":" + worst;
    }

    private void evaluate(Minecraft mc) {
        for (String action : new String[]{"c1-roar", "c1-switch", "c2-roar", "c2-swipe", "c2-maul", "c3-roar", "c3-charge"}) {
            check("server-" + action, Boolean.TRUE.equals(results.get(action)), "API returned " + results.get(action));
        }
        List<Sample> a = samples.get(idA), b = samples.get(idB);
        // Case 1: A play (cut), B blendTo(bite, 10) from the mid rear_roar pose.
        int tokA = tokens.get("c1-switch-A"), tokB = tokens.get("c1-switch-B");
        List<Sample> fa = withToken(a, tokA), fb = withToken(b, tokB);
        check("c1-frames", fa.size() > 3 && fb.size() > 3, "A=" + fa.size() + " B=" + fb.size());
        Sample a0 = fa.get(0), pa = before(a, a0), b0 = fb.get(0), pb = before(b, b0);
        check("c1-previous-was-roar", pa != null && pb != null && pa.token == tokens.get("c1-roar-A") && pb.token == tokens.get("c1-roar-B"),
                "previous tokens " + (pa == null ? -1 : pa.token) + "/" + (pb == null ? -1 : pb.token));
        check("c1-from-is-last-drawn", "blend".equals(b0.state) && b0.from != null && d(b0.from, pb.out) < 1e-4, "state=" + b0.state);
        double cutA = d(a0.out, pa.out), cutB = d(b0.out, pb.out);
        data.addProperty("c1CutPlay", cutA); data.addProperty("c1CutBlend", cutB);
        data.addProperty("c1FirstW", b0.w); data.addProperty("c1FirstAge", b0.age);
        check("c1-play-cuts", cutA > 20, "play jump " + cutA);
        check("c1-blend-continuous", cutB <= 0.5 * cutA && b0.w <= 0.35, "blend jump " + cutB + " vs play " + cutA + ", first w " + b0.w);
        String lin = linear(fb);
        data.addProperty("c1Linear", lin);
        check("c1-linear", Double.parseDouble(lin.split(":")[1]) < 1e-3 && Integer.parseInt(lin.split(":")[0]) >= 3, "frames:worst " + lin);
        boolean mid = false, done = false, after = true;
        for (Sample s : fb) {
            if (s.w >= 0.3f && s.w <= 0.7f && "blend".equals(s.state)) { mid = true; record("c1Mid", s); }
            if ("done".equals(s.state)) { done = true; record("c1End", s); }
            else if (done) { after &= "none".equals(s.state) && d(s.out, s.built) == 0; }
        }
        record("c1Start", b0);
        check("c1-mid", mid, "a frame with 0.3<=w<=0.7");
        check("c1-end", done && after, "done, then the plain clip");
        // Case 2: blendTo(swipe, 12) replaced after 4 ticks by blendTo(maul, 10).
        List<Sample> sw = withToken(b, tokens.get("c2-swipe-B")), ma = withToken(b, tokens.get("c2-maul-B"));
        check("c2-frames", sw.size() > 2 && ma.size() > 3, "swipe=" + sw.size() + " maul=" + ma.size());
        boolean swipeBlended = true, swipeDone = false;
        for (Sample s : sw) { swipeBlended &= s.w >= 0 && s.w < 1; swipeDone |= "done".equals(s.state); }
        check("c2-swipe-blended-until-replaced", swipeBlended && !swipeDone, "every swipe frame blended, none finished");
        Sample m0 = ma.get(0), pm = before(b, m0);
        check("c2-maul-from-blended-pose", pm != null && pm.token == tokens.get("c2-swipe-B") && "blend".equals(m0.state)
                && d(m0.from, pm.out) < 1e-4, "from = last drawn (blended swipe) pose");
        data.addProperty("c2CutBlend", d(m0.out, pm.out));
        String lin2 = linear(ma) + "|" + linear(sw);
        data.addProperty("c2Linear", lin2);
        check("c2-linear", Double.parseDouble(lin2.split("\\|")[0].split(":")[1]) < 1e-3 && Double.parseDouble(lin2.split("\\|")[1].split(":")[1]) < 1e-3, lin2);
        // Case 3: death during blendTo(charge, 30): death wins, no blending while dying.
        List<Sample> ch = withToken(b, tokens.get("c3-charge-B"));
        boolean chargeBlend = false;
        for (Sample s : ch) { chargeBlend |= "blend".equals(s.state) && s.w < 1; }
        int dying = 0; boolean plain = true;
        for (Sample s : b.subList(b.indexOf(ch.isEmpty() ? b.get(0) : ch.get(0)), b.size())) {
            if (s.deathTime > 0) { dying++; plain &= s.w < 0 && d(s.out, s.built) == 0; }
        }
        check("c3-blend-before-death", chargeBlend, "charge frames " + ch.size());
        check("c3-death-unblended", dying > 3 && plain, "dying frames " + dying);
        int[] counts = AnimBlend.counts();
        int expected = data.get("acceptedBefore").getAsInt() + 4;
        data.addProperty("accepted", counts[0]); data.addProperty("rejected", counts[1]);
        check("messages", counts[0] == expected && counts[1] == data.get("rejectedBefore").getAsInt(),
                "accepted " + counts[0] + " (expected " + expected + "), rejected " + counts[1]);
        data.addProperty("framesA", a.size()); data.addProperty("framesB", b.size());
    }

    private void record(String key, Sample s) {
        JsonObject o = new JsonObject();
        o.addProperty("w", s.w); o.addProperty("age", s.age); o.addProperty("state", s.state);
        o.addProperty("fromToBuilt", s.from == null ? -1 : d(s.from, s.built));
        o.addProperty("outToBuilt", d(s.out, s.built));
        o.addProperty("outToFrom", s.from == null ? -1 : d(s.out, s.from));
        float[] neck = s.out.get("neck");
        o.addProperty("neckRotX", neck == null ? 0 : neck[0]);
        data.add(key, o);
    }

    private void snapshot(Minecraft mc, String label) {
        String name = out.getName().replace(".json", "") + "-" + label + ".png";
        ScreenShotHelper.saveScreenshot(out.getParentFile(), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        if (!new File(new File(out.getParentFile(), "screenshots"), name).isFile()) { throw new IllegalStateException("Missing screenshot"); }
        screenshots.add(name);
    }

    private void submit(Minecraft mc, Runnable work) {
        mc.getIntegratedServer().addScheduledTask(() -> {
            try { work.run(); } catch (Exception | LinkageError problem) { taskError = problem.toString(); }
        });
    }

    private void finish(Minecraft mc, boolean ok, String error) {
        finished = true;
        AnimBlend.setProbe(null);
        try {
            if (mc.world != null) { mc.world.sendQuittingDisconnectingPacket(); mc.loadWorld(null); }
            mc.displayGuiScreen(new GuiMainMenu());
            JsonObject proof = new JsonObject();
            proof.addProperty("success", ok); proof.addProperty("error", error);
            proof.add("checks", checks); proof.add("screenshots", screenshots); proof.add("data", data);
            try (Writer writer = Files.newBufferedWriter(out.toPath(), StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(proof, writer);
            }
            System.out.println("P7B DONE success=" + ok);
        } catch (Exception problem) { problem.printStackTrace(); }
        mc.shutdown();
    }
}
