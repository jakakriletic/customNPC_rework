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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.rework.army.ArmyClient;
import noppes.npcs.rework.army.ArmyClusters;
import noppes.npcs.rework.army.ArmySnapshot;

/** M11.2 controller-only acceptance of the army snapshot on a real client against a dev dedicated server: the real
 * rwarmy channel, a real player's rights and the limits of the server. The player starts as an operator and sets the
 * scene with real commands; the rights are then changed from the server console, which waits for each ARMYSNAP SYNC
 * line. Phase "off": the server has RwArmies=0 and never answers. */
@SideOnly(Side.CLIENT)
public final class ArmySnapshotHarness {
    private static final String OTHER = "0b5a1c2e-1111-4a2b-9c3d-123456789abc";
    private static final int FOOT = 4;
    private static final Pattern COUNTERS = Pattern.compile("RWARMY clusters armies=(\\d+) radius=(\\d+) min=(\\d+) served=(\\d+) limited=(\\d+) rejected=(\\d+)");

    private interface Act { void run() throws Exception; }
    private interface Cond { boolean ok() throws Exception; }
    private static final class Step {
        final String name; final int wait; final Act act; final Cond until;
        Step(String name, int wait, Act act, Cond until) { this.name = name; this.wait = wait; this.act = act; this.until = until; }
    }

    private final Properties job;
    private final File out;
    private final JsonArray checks = new JsonArray();
    private final JsonObject data = new JsonObject();
    private final List<String> chat = Collections.synchronizedList(new ArrayList<String>());
    private final Deque<Step> steps = new ArrayDeque<>();
    private Step step;
    private int delay, mark, sent, pollWait, baseReplies;
    private boolean acted, finished;
    private long since = System.nanoTime();

    private ArmySnapshotHarness(Properties job) {
        this.job = job;
        out = new File(job.getProperty("out"));
    }

    static boolean installIfRequested(String path) throws Exception {
        Properties p = new Properties();
        try (InputStreamReader in = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)) { p.load(in); }
        if (!"1".equals(p.getProperty("armySnapshot"))) { return false; }
        if (!p.containsKey("out") || !p.containsKey("connect") || !p.containsKey("phase")) { throw new IllegalArgumentException("Incomplete armySnapshot job"); }
        ArmySnapshotHarness h = new ArmySnapshotHarness(p);
        if ("off".equals(p.getProperty("phase"))) { h.planOff(); } else { h.planMain(); }
        MinecraftForge.EVENT_BUS.register(h);
        System.out.println("ARMYSNAP INSTALL phase=" + p.getProperty("phase"));
        return true;
    }

    // ------------------------------------------------------------------ scenario
    private void join(Minecraft mc) {
        step("menu", 0, () -> { }, () -> mc.currentScreen instanceof GuiMainMenu);
        step("join", 10, () -> {
            mc.gameSettings.pauseOnLostFocus = false;
            mc.displayGuiScreen(new GuiConnecting(new GuiMainMenu(), mc, "127.0.0.1", Integer.parseInt(job.getProperty("connect"))));
        }, () -> mc.world != null && mc.player != null && mc.currentScreen == null);
    }

    private void planMain() {
        Minecraft mc = Minecraft.getMinecraft();
        join(mc);
        for (String c : new String[]{"gamemode 1", "gamerule doMobSpawning false", "tp @p 600 " + (FOOT + 1) + " 0"}) {
            step("setup " + c, 5, () -> command(mc, c), () -> true);
        }
        step("arrive", 60, () -> { }, () -> Math.abs(mc.player.posX - 600) < 2);

        // Scene, all near x=600: north = 9 together + 2 far away, south = 3 together, hidden = 1, ghost = orphan name.
        for (int x : new int[]{596, 600, 604}) { for (int z : new int[]{10, 14, 18}) { spawn(mc, "ARMY_N", x, z); } }
        spawn(mc, "ARMY_N", 680, 10);
        spawn(mc, "ARMY_N", 684, 10);
        for (int x : new int[]{600, 604, 608}) { spawn(mc, "ARMY_S", x, -60); }
        spawn(mc, "ARMY_H", 560, 14);
        spawn(mc, "ARMY_G", 596, 30);
        for (String a : new String[]{"north", "south", "hidden"}) {
            chatStep(mc, "create " + a, "rwarmy create " + a + " uuid:" + OTHER, "RWARMY created " + a, m -> { });
        }
        chatStep(mc, "create mine", "rwarmy create mine", "RWARMY created mine", m -> { });
        assign(mc, "north", "ARMY_N", 11);
        assign(mc, "south", "ARMY_S", 3);
        assign(mc, "hidden", "ARMY_H", 1);
        chatStep(mc, "orphan on the npc", "entitydata @e[name=ARMY_G,c=1] {}", "RwArmy:\"ghost\"", m -> check("orphan-name-on-npc", true, "ARMY_G has RwArmy ghost, no such army"));
        step("settle", 40, () -> { }, () -> true);

        // Operator: everything visible, exact clusters; an orphan name never appears.
        poll(mc, "op-snapshot", s -> s.entry("north") != null && s.entry("north").loaded == 11);
        step("op-checks", 0, () -> {
            ArmySnapshot s = ArmyClient.latest();
            check("op-all-visible", names(s).equals("hidden,mine,north,south"), names(s));
            check("op-command-everywhere", s.entry("hidden").command && s.entry("north").command && s.entry("mine").command, "operator");
            check("config-defaults", s.radius == ArmyClusters.DEFAULT_RADIUS && s.minMembers == ArmyClusters.DEFAULT_MIN_MEMBERS, s.radius + " " + s.minMembers);
            ArmySnapshot.Entry n = s.entry("north");
            check("north-two-clusters", n.clusters.size() == 2 && n.omittedClusters == 0, describe(n));
            cluster("north-main", n.clusters.get(0), ArmyClusters.Kind.MAIN, 9, 600, 14);
            cluster("north-straggler", n.clusters.get(1), ArmyClusters.Kind.STRAGGLER, 2, 682, 10);
            check("north-last-is-main", n.hasLast() && n.lastDimension == 0 && near(n.lastX, 600) && near(n.lastZ, 14), describe(n));
            cluster("south-below-threshold-is-main", s.entry("south").clusters.get(0), ArmyClusters.Kind.MAIN, 3, 604, -60);
            ArmySnapshot.Entry m = s.entry("mine");
            check("empty-army", m.loaded == 0 && m.clusters.isEmpty() && !m.hasLast(), describe(m));
            check("orphan-ignored", s.entry("ghost") == null, names(s));
            check("request-answered-by-id", s.requestId == sent, s.requestId + " " + sent);
            check("world-time", s.worldTime > 0, "time " + s.worldTime);
        }, () -> true);

        // Limits of the server: one answer per 20 ticks, malformed requests are counted and change nothing.
        step("limit-send", 40, () -> {
            baseReplies = ArmyClient.replies();
            ArmyClient.request(); ArmyClient.request(); sent = ArmyClient.request();
        }, () -> true);
        step("limit-check", 40, () -> {
            check("rate-limit-one-answer", ArmyClient.replies() == baseReplies + 1, (ArmyClient.replies() - baseReplies) + " answers to 3 requests");
            check("rate-limit-first-served", ArmyClient.latest().requestId == sent - 2, ArmyClient.latest().requestId + " of " + sent);
        }, () -> true);
        step("malformed-send", 0, () -> {
            baseReplies = ArmyClient.replies();
            ArmyClient.send(new byte[]{9, 9, 9});
            ArmyClient.send(new byte[5000]);
            ArmyClient.send(new byte[]{1, 2, 0, 0, 0, 0});
        }, () -> true);
        step("malformed-wait", 40, () -> { }, () -> true);
        chatStep(mc, "counters", "rwarmy clusters", COUNTERS.pattern(), m -> {
            check("malformed-rejected", m.group(6).equals("3") && ArmyClient.replies() == baseReplies, m.group());
            check("over-limit-counted", Integer.parseInt(m.group(5)) >= 2, m.group());
            check("served-matches-client", Integer.parseInt(m.group(4)) == ArmyClient.replies(), m.group() + " client " + ArmyClient.replies());
            check("client-rejects-nothing", ArmyClient.malformed() == 0, "malformed " + ArmyClient.malformed());
        });
        step("command-lines", 2, () -> {
            String all = String.join("\n", chat);
            check("command-shows-clusters", all.contains("RWARMY clusterarmy north access=command loaded=11 clusters=2") && all.contains("RWARMY cluster north main dim=0") && all.contains("RWARMY cluster north straggler dim=0"), "chat lines");
        }, () -> true);

        // A real player without operator: rights come from the register, changes apply to the next snapshot.
        sync(mc, "deop", "without-operator", s -> s.entry("north") == null);
        step("deop-checks", 0, () -> {
            ArmySnapshot s = ArmyClient.latest();
            check("non-op-sees-own-only", names(s).equals("mine"), names(s));
            check("owner-has-command", s.entry("mine").command, "mine");
        }, () -> true);
        sync(mc, "grant-view", "view-north", s -> s.entry("north") != null);
        step("view-checks", 0, () -> {
            ArmySnapshot s = ArmyClient.latest();
            check("view-grant-visible", names(s).equals("mine,north"), names(s));
            check("view-is-not-command", !s.entry("north").command, describe(s.entry("north")));
            check("view-sees-clusters", s.entry("north").loaded == 11 && s.entry("north").clusters.size() == 2, describe(s.entry("north")));
        }, () -> true);
        sync(mc, "grant-command", "command-south", s -> s.entry("south") != null);
        step("command-checks", 0, () -> {
            ArmySnapshot s = ArmyClient.latest();
            check("command-grant-visible", names(s).equals("mine,north,south") && s.entry("south").command, names(s));
            check("hidden-stays-hidden", s.entry("hidden") == null, names(s));
        }, () -> true);
        sync(mc, "revoke", "revoked-south", s -> s.entry("south") == null);
        step("revoke-checks", 0, () -> check("revoke-applies-to-next", names(ArmyClient.latest()).equals("mine,north"), names(ArmyClient.latest())), () -> true);
        chatStep(mc, "non-op-command", "rwarmy clusters", COUNTERS.pattern(), m -> check("non-op-command-counts-visible", m.group(1).equals("2"), m.group()));

        // Members unload: the army stays with its last known position.
        sync(mc, "away", "unloaded", s -> s.entry("north") != null && s.entry("north").loaded == 0);
        step("away-checks", 0, () -> {
            ArmySnapshot.Entry n = ArmyClient.latest().entry("north");
            check("unloaded-stays-listed", n.clusters.isEmpty(), describe(n));
            check("unloaded-keeps-last", n.hasLast() && n.lastDimension == 0 && n.lastX >= 590 && n.lastX <= 690 && n.lastZ >= 0 && n.lastZ <= 24, describe(n));
            check("last-seen-time", n.lastSeen > 0 && n.lastSeen <= ArmyClient.latest().worldTime, n.lastSeen + " of " + ArmyClient.latest().worldTime);
            data.addProperty("last", n.lastDimension + "," + n.lastX + "," + n.lastY + "," + n.lastZ);
            check("client-still-connected", mc.world != null && mc.player != null, "connected");
            finish(mc, true, "");
        }, () -> true);
    }

    /** The server has RwArmies=0: no answer, no command, and the connection stays. */
    private void planOff() {
        Minecraft mc = Minecraft.getMinecraft();
        join(mc);
        step("settle", 40, () -> { }, () -> true);
        step("off-request", 0, () -> { baseReplies = ArmyClient.replies(); ArmyClient.request(); ArmyClient.send(new byte[]{9, 9, 9}); }, () -> true);
        step("off-wait", 100, () -> { }, () -> true);
        step("off-checks", 0, () -> {
            check("off-no-answer", ArmyClient.replies() == baseReplies && ArmyClient.latest() == null, "answers " + (ArmyClient.replies() - baseReplies));
            check("off-still-connected", mc.world != null && mc.player != null && mc.currentScreen == null, "connected");
        }, () -> true);
        chatStep(mc, "off-command", "rwarmy list", "Unknown command", m -> { check("off-no-command", true, m.group()); finish(mc, true, ""); });
    }

    // ------------------------------------------------------------------ steps
    private interface SnapshotCond { boolean ok(ArmySnapshot s); }
    private interface OnMatch { void accept(Matcher m) throws Exception; }

    private void spawn(Minecraft mc, String clone, int x, int z) {
        step("spawn " + clone + " " + x + " " + z, 5, () -> command(mc, "noppes clone spawn " + clone + " 1 " + x + "," + FOOT + "," + z), () -> true);
    }

    private void assign(Minecraft mc, String army, String clone, int count) {
        chatStep(mc, "assign " + army, "rwarmy assign " + army + " " + clone + " 200", "RWARMY assigned " + army + " npcs=" + count + " changed=" + count,
                m -> check("assigned-" + army, true, m.group()));
    }

    /** Asks every 25 ticks, as an open map would, until a fresh answer satisfies the condition. */
    private void poll(Minecraft mc, String name, SnapshotCond cond) {
        steps.add(new Step(name, 0, () -> { sent = ArmyClient.request(); pollWait = 25; }, () -> {
            ArmySnapshot s = ArmyClient.latest();
            if (s != null && s.requestId == sent && cond.ok(s)) { return true; }
            if (--pollWait <= 0) { sent = ArmyClient.request(); pollWait = 25; }
            return false;
        }));
    }

    /** One line for the console side of the scenario, then the fresh snapshot that shows its effect. */
    private void sync(Minecraft mc, String marker, String name, SnapshotCond cond) {
        step("sync " + marker, 0, () -> System.out.println("ARMYSNAP SYNC " + marker), () -> true);
        poll(mc, name, cond);
    }

    private void chatStep(Minecraft mc, String name, String cmd, String regex, OnMatch then) {
        Pattern p = Pattern.compile(regex);
        Matcher[] found = new Matcher[1];
        steps.add(new Step(name, 0, () -> { mark = chat.size(); command(mc, cmd); }, () -> (found[0] = find(p)) != null));
        steps.add(new Step(name + " check", 0, () -> then.accept(found[0]), () -> true));
    }

    private void step(String name, int wait, Act act, Cond until) { steps.add(new Step(name, wait, act, until)); }

    @SubscribeEvent
    public void chat(ClientChatReceivedEvent e) {
        String line = e.getMessage().getUnformattedText();
        chat.add(line);
        System.out.println("ARMYSNAP CHAT " + line);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || finished) { return; }
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (step == null) {
                step = steps.poll();
                if (step == null) { return; }
                delay = step.wait; acted = false; since = System.nanoTime();
                System.out.println("ARMYSNAP STEP " + step.name);
            }
            if (System.nanoTime() - since > 120000000000L) { throw new IllegalStateException("Timeout " + step.name + "; latest " + (ArmyClient.latest() == null ? "none" : names(ArmyClient.latest()))); }
            if (delay-- > 0) { return; }
            if (!acted) { acted = true; step.act.run(); if (finished) { return; } }
            if (step.until.ok()) { step = null; }
        } catch (Exception | LinkageError problem) { problem.printStackTrace(); finish(mc, false, problem.toString()); }
    }

    // ------------------------------------------------------------------ helpers
    private static String names(ArmySnapshot s) {
        List<String> out = new ArrayList<>();
        for (ArmySnapshot.Entry e : s.armies) { out.add(e.name); }
        return String.join(",", out);
    }

    private static String describe(ArmySnapshot.Entry e) {
        StringBuilder b = new StringBuilder(e.name + " loaded=" + e.loaded + " command=" + e.command + " last=" + e.lastX + "," + e.lastY + "," + e.lastZ + " seen=" + e.lastSeen);
        for (ArmyClusters.Cluster c : e.clusters) { b.append(' ').append(c.kind).append(':').append(c.size).append('@').append(c.x).append(',').append(c.z); }
        return b.toString();
    }

    private static boolean near(int a, int b) { return Math.abs(a - b) <= 2; }

    private void cluster(String key, ArmyClusters.Cluster c, ArmyClusters.Kind kind, int size, int x, int z) {
        check(key, c.kind == kind && c.size == size && c.dimension == 0 && near(c.x, x) && near(c.z, z) && Math.abs(c.y - FOOT) <= 2,
                c.kind + " size " + c.size + " at " + c.x + " " + c.y + " " + c.z);
    }

    private Matcher find(Pattern p) {
        synchronized (chat) {
            for (int i = mark; i < chat.size(); i++) {
                Matcher m = p.matcher(chat.get(i));
                if (m.find()) { return m; }
            }
        }
        return null;
    }

    private static void command(Minecraft mc, String text) { mc.player.sendChatMessage("/" + text); }

    private void check(String key, boolean ok, String detail) {
        JsonObject row = new JsonObject();
        row.addProperty("key", key); row.addProperty("ok", ok); row.addProperty("detail", detail);
        checks.add(row);
        System.out.println("ARMYSNAP CHECK " + key + " ok=" + ok + " " + detail);
        if (!ok) { throw new IllegalStateException(key + ": " + detail); }
    }

    private void finish(Minecraft mc, boolean ok, String error) {
        finished = true;
        try {
            if (mc.world != null) { mc.world.sendQuittingDisconnectingPacket(); mc.loadWorld(null); }
            // The render pass of this frame still runs: without a screen and a world EntityRenderer has nothing to draw.
            mc.displayGuiScreen(new GuiMainMenu());
            JsonObject proof = new JsonObject();
            proof.addProperty("success", ok); proof.addProperty("error", error);
            proof.add("checks", checks); proof.add("data", data);
            try (Writer writer = Files.newBufferedWriter(out.toPath(), StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(proof, writer);
            }
            System.out.println("ARMYSNAP DONE success=" + ok);
        } catch (Exception problem) { problem.printStackTrace(); }
        mc.shutdown();
    }
}
