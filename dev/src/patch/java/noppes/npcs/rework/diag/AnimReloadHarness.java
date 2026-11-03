package noppes.npcs.rework.diag;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.entity.Entity;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.rework.anim.AnimBlend;
import noppes.npcs.rework.anim.AnimFileValidator;
import noppes.npcs.rework.anim.AnimLibraries;
import noppes.npcs.rework.anim.AnimReload;
import noppes.npcs.rework.anim.ReloadClient;

/** P7c controller-only acceptance of {@code /npcanim reload} on a real client with customnpcentities 0.7.0
 * (RwAnimHooks=1), against an integrated or a dedicated server in the same run directory. The server is
 * driven and read only through real commands: {@code /npcanim} and the optional mod's own {@code /cne}, whose
 * list and play go through the server NPC's {@code animations()}. The client's library, its action clips
 * and the poses of the patched buildPose are read directly. No compile types of the optional mod. */
@SideOnly(Side.CLIENT)
public final class AnimReloadHarness {
    private static final String ANIMATED = "dev.jakak.customnpcentities.entity.EntityAnimatedNpc";
    private static final String ID = "cave_bear", SENTINEL = "rw_p7c";
    private static final Pattern OK = Pattern.compile("NPCANIM OK rev=(\\d+) hash=(\\w+) overrides=(\\S+) changed=(\\S+) stopped=(\\d+)");
    private static final Pattern STATUS = Pattern.compile("NPCANIM STATUS rev=(\\d+) hash=(\\w+) overrides=(\\S+) busy=(\\d)");

    private interface Act { void run() throws Exception; }
    private interface Cond { boolean ok() throws Exception; }
    private static final class Step {
        final String name; final int wait; final Act act; final Cond until;
        Step(String name, int wait, Act act, Cond until) { this.name = name; this.wait = wait; this.act = act; this.until = until; }
    }

    private final Properties job;
    private final File out;
    private final boolean dedicated;
    private final JsonArray checks = new JsonArray();
    private final JsonArray screenshots = new JsonArray();
    private final JsonObject data = new JsonObject();
    private final List<String> chat = Collections.synchronizedList(new ArrayList<String>());
    private final Map<Integer, Map<String, float[]>> rendered = Collections.synchronizedMap(new HashMap<Integer, Map<String, float[]>>());
    private final Deque<Step> steps = new ArrayDeque<>();
    private final File file = new File(Loader.instance().getConfigDir(), "customnpcs/animations/" + ID + ".animation.json");
    private Step step;
    private int delay, mark;
    private boolean acted, finished;
    private long since = System.nanoTime();
    private List<String> jarNames;
    private int keptToken, keptEntity;
    private double startX, startZ, moved;
    private String lastHash = "none";
    private int lastRevision;

    private AnimReloadHarness(Properties job) {
        this.job = job;
        out = new File(job.getProperty("out"));
        dedicated = job.containsKey("connect");
    }

    static boolean installIfRequested(String path) throws Exception {
        Properties p = new Properties();
        try (InputStreamReader in = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)) { p.load(in); }
        if (!"1".equals(p.getProperty("animReload"))) { return false; }
        if (!p.containsKey("out") || !p.containsKey("world")) { throw new IllegalArgumentException("Incomplete animReload job"); }
        AnimReloadHarness h = new AnimReloadHarness(p);
        h.plan();
        MinecraftForge.EVENT_BUS.register(h);
        System.out.println("P7C INSTALL world=" + p.getProperty("world") + " dedicated=" + h.dedicated);
        return true;
    }

    // ------------------------------------------------------------------ scenario
    private void plan() {
        Minecraft mc = Minecraft.getMinecraft();
        step("menu", 0, () -> { }, () -> mc.currentScreen instanceof GuiMainMenu);
        step("join", 10, () -> { mc.gameSettings.pauseOnLostFocus = false; connect(mc); }, () -> inWorld(mc));
        for (String c : new String[]{"gamemode 1", "gamerule doMobSpawning false", "gamerule doDaylightCycle false", "time set 6000", "tp @p 0 4 0 0 15"}) {
            step("setup " + c, 5, () -> command(mc, c), () -> true);
        }
        step("summon", 60, () -> command(mc, "summon customnpcentities:" + ID + " 0 4 8"), () -> bears(mc).size() == 1);
        step("baseline", 20, () -> {
            check("classes-patched", "1".equals(System.getProperty("rwcore.animpatch")) && "1".equals(System.getProperty("rwcore.blendpatch")), "journal, blend");
            jarNames = AnimLibraries.jarClipNames(ID);
            check("jar-baseline", names(bears(mc).get(0)).equals(jarNames) && !jarNames.contains("aaa_p7c"), jarNames.toString());
            check("client-not-reloaded", AnimReload.clientRevision() == 0 && AnimReload.clientOverrides() == 0, "rev " + AnimReload.clientRevision());
            AnimBlend.setProbe((npc, token, age, partial, w, state, from, built, pose) -> rendered.put(npc.getEntityId(), built));
            snapshot(mc, "before");
        }, () -> true);
        chatStep(mc, "status-0", "npcanim status", STATUS.pattern(), m -> check("status-initial", m.group(1).equals("0") && m.group(3).equals("none"), m.group()));

        // R1: pose, new first clip (shifts every free index), changed length and a new server event.
        step("play-before-r1", 5, () -> command(mc, "cne play idle"), () -> "idle".equals(playing(mc)));
        step("write-r1", 0, () -> write(r1()), () -> true);
        reload(mc, "r1", "reload " + ID, m -> {
            check("r1-commit", m.group(3).equals(ID) && m.group(4).equals(ID) && m.group(5).equals("1"), m.group());
            check("r1-client-revision", AnimReload.clientRevision() == Integer.parseInt(m.group(1)) && AnimReload.clientHash().equals(m.group(2)), AnimReload.clientRevision() + " " + AnimReload.clientHash());
            // The commit precedes this answer on the same connection: the old index is gone in the same order.
            check("r1-existing-action-stopped", playing(mc) == null, "client action " + playing(mc));
            Entity bear = bears(mc).get(0);
            check("r1-client-library", names(bear).contains("aaa_p7c") && Math.abs(length(bear, "hurt") - 0.9f) < 1e-4
                    && events(bear, "aaa_p7c") == 1, names(bear).toString());
            check("r1-client-sample", near(sample(bear, "idle"), 11, 22, 33), "idle sentinel");
            check("r1-jar-cache-untouched", AnimLibraries.jarClipNames(ID).equals(jarNames), "static cache");
            if (!dedicated) {
                Entity server = mc.getIntegratedServer().getWorld(0).getEntityByID(bear.getEntityId());
                check("r1-sides-separate", library(server) != library(bear) && names(server).equals(names(bear)), "one classloader, two snapshots");
            }
        });
        step("r1-rendered", 0, () -> { }, () -> renderedNear(11, 22, 33));
        chatStep(mc, "r1-server-list", "cne list", "aaa_p7c \\(3[.,]00s\\).*hurt \\(0[.,]90s\\)", m -> check("r1-server-library", true, m.group()));
        step("r1-shot", 0, () -> { check("r1-rendered", true, "idle sentinel in the patched buildPose"); snapshot(mc, "r1"); }, () -> true);
        // aaa_p7c has a lunge event only in the file: the server NPC moving proves the server event of the new clip.
        step("r1-play", 0, () -> { Entity b = bears(mc).get(0); startX = b.posX; startZ = b.posZ; moved = 0; command(mc, "cne play aaa_p7c"); }, () -> "aaa_p7c".equals(playing(mc)));
        step("r1-new-clip-rendered", 0, () -> { }, () -> { track(mc); return renderedNear(5, 6, 7); });
        step("r1-new-event", 0, () -> { }, () -> { track(mc); return moved > 1; });
        step("r1-new-checks", 0, () -> {
            check("r1-new-clip-index", true, "server index 128 resolves to aaa_p7c on the client; rendered");
            check("r1-server-event", true, "lunge from the file moved the server NPC " + moved);
            data.addProperty("lungeBlocks", moved);
        }, () -> playing(mc) == null);

        // Invalid file and names: nothing changes, the running action goes on.
        step("write-bad", 5, () -> { write(bad()); command(mc, "cne play aaa_p7c"); }, () -> "aaa_p7c".equals(playing(mc)));
        step("keep-token", 0, () -> keep(mc), () -> true);
        chatStep(mc, "bad-reload", "npcanim reload " + ID, "NPCANIM ERROR .*", m -> {
            check("invalid-reports-path", m.group().contains(ID + ".animation.json") && m.group().contains("animations.aaa_p7c.animation_length"), m.group());
            check("invalid-keeps-revision", AnimReload.clientRevision() == lastRevision && AnimReload.clientHash().equals(lastHash), "rev " + lastRevision);
            check("invalid-keeps-action", kept(mc), "token " + keptToken);
        });
        chatStep(mc, "bad-traversal", "npcanim reload ../evil", "NPCANIM ERROR unknown entity id", m -> check("traversal-refused", true, m.group()));
        chatStep(mc, "bad-unknown", "npcanim reload nosuch", "NPCANIM ERROR unknown entity id", m -> check("unknown-refused", true, m.group()));
        statusSame(mc, "invalid-status");

        // A client that refuses: the transaction aborts on the server and every client.
        step("fault-on", 60, () -> { write(r2()); ReloadClient.setFault(true); }, () -> true);
        chatStep(mc, "fault-reload", "npcanim reload " + ID, "NPCANIM ERROR not confirmed.*nothing changed", m -> {
            check("refusal-aborts", AnimReload.clientRevision() == lastRevision && AnimReload.clientHash().equals(lastHash) && names(bears(mc).get(0)).contains("aaa_p7c"), m.group());
            ReloadClient.setFault(false);
        });
        statusSame(mc, "refusal-status");
        chatStep(mc, "fault-list", "cne list", "aaa_p7c \\(", m -> check("refusal-server-unchanged", true, m.group()));

        // R2: one clip removed, a last one added, so the sorted indices move again.
        reload(mc, "r2", "reload " + ID, m -> {
            check("r2-commit", m.group(4).equals(ID), m.group());
            Entity bear = bears(mc).get(0);
            check("r2-client-library", names(bear).contains("zzz_p7c") && !names(bear).contains("aaa_p7c") && near(sample(bear, "idle"), 1, 2, 3), names(bear).toString());
        });
        chatStep(mc, "r2-removed", "cne play aaa_p7c", "No clip 'aaa_p7c'", m -> check("r2-removed-on-server", true, m.group()));
        step("r2-play", 0, () -> command(mc, "cne play zzz_p7c"), () -> "zzz_p7c".equals(playing(mc)));
        step("r2-rendered", 0, () -> { }, () -> renderedNear(8, 9, 10));
        step("r2-checks", 0, () -> check("r2-index-moved", true, "free clip plays under the same name on both sides"), () -> playing(mc) == null);

        // A new NPC uses the same revision on both sides.
        step("spawn-tp", 5, () -> command(mc, "tp @p 0 4 -4 180 15"), () -> true);
        step("spawn", 40, () -> command(mc, "summon customnpcentities:" + ID + " 0 4 -8"), () -> bears(mc).size() == 2);
        step("spawn-check", 10, () -> {
            List<Entity> all = bears(mc);
            check("spawn-same-snapshot", library(all.get(0)) == library(all.get(1)) && names(all.get(1)).contains("zzz_p7c"), "client table");
        }, () -> true);
        chatStep(mc, "spawn-list", "cne list", "zzz_p7c \\(3[.,]00s\\)", m -> check("spawn-server-library", true, m.group()));

        // Reload of unchanged files: a new revision, the same state, nothing is stopped.
        step("repeat-play", 0, () -> command(mc, "cne play zzz_p7c"), () -> "zzz_p7c".equals(playing(mc)));
        step("repeat-keep", 0, () -> keep(mc), () -> true);
        reload(mc, "repeat", "reload " + ID, m -> {
            check("repeat-same-hash", m.group(4).equals("none") && m.group(5).equals("0") && kept(mc), m.group());
        });
        if (dedicated) { planReconnect(mc); }

        // A removed file restores the jar's clips.
        step("cleanup-wait", 0, () -> { }, () -> playing(mc) == null);
        step("cleanup", 0, () -> Files.delete(file.toPath()), () -> !file.exists());
        reload(mc, "cleanup-reload", "reload", m -> {
            check("cleanup-commit", m.group(3).equals("none") && m.group(4).equals(ID), m.group());
            check("cleanup-client-jar", names(bears(mc).get(0)).equals(jarNames) && AnimReload.clientOverrides() == 0, names(bears(mc).get(0)).toString());
        });
        chatStep(mc, "cleanup-list", "cne list", "clips: ", m -> check("cleanup-server-jar", !m.group().isEmpty() && lastChatLacks("zzz_p7c"), lastChat()));
        step("end", 5, () -> finish(mc, true, ""), () -> true);
    }

    /** Dedicated only: a state belongs to one connection; a late join gets it first or does not stay. */
    private void planReconnect(Minecraft mc) {
        step("relog-wait", 0, () -> { }, () -> playing(mc) == null);
        step("relog-out", 0, () -> leave(mc), () -> AnimReload.clientRevision() == 0);
        step("relog-cleared", 0, () -> check("disconnect-clears", AnimReload.clientOverrides() == 0 && AnimReload.clientHash().equals("none"), "client state"), () -> true);
        step("kick-join", 40, () -> { ReloadClient.setFault(true); connect(mc); }, () -> mc.currentScreen instanceof GuiDisconnected);
        step("kick-check", 0, () -> {
            check("late-join-refusal-disconnects", AnimReload.clientRevision() == 0, "client refused; server disconnected it");
            ReloadClient.setFault(false);
            mc.displayGuiScreen(new GuiMainMenu());
        }, () -> mc.currentScreen instanceof GuiMainMenu);
        step("late-join", 60, () -> connect(mc), () -> inWorld(mc) && AnimReload.clientRevision() > 0 && !bears(mc).isEmpty());
        step("late-check", 5, () -> {
            check("late-join-state", AnimReload.clientRevision() == lastRevision && AnimReload.clientHash().equals(lastHash) && AnimReload.clientOverrides() == 1, AnimReload.clientRevision() + " " + AnimReload.clientHash());
            check("late-join-library", names(bears(mc).get(0)).contains("zzz_p7c"), names(bears(mc).get(0)).toString());
        }, () -> true);
        step("late-play", 0, () -> command(mc, "cne play zzz_p7c"), () -> "zzz_p7c".equals(playing(mc)));
        step("late-played", 0, () -> check("late-join-index", true, "new index resolves on the late client"), () -> playing(mc) == null);
        step("retrack-away", 0, () -> command(mc, "tp @p 2000 4 2000"), () -> bears(mc).isEmpty());
        step("retrack-back", 40, () -> command(mc, "tp @p 0 4 -4 180 15"), () -> !bears(mc).isEmpty());
        step("retrack-play", 20, () -> command(mc, "cne play zzz_p7c"), () -> "zzz_p7c".equals(playing(mc)));
        step("retrack-check", 0, () -> check("retrack-index", AnimReload.clientRevision() == lastRevision && names(bears(mc).get(0)).contains("zzz_p7c"), "new entity objects, same table"), () -> true);
    }

    private interface OnMatch { void accept(Matcher m) throws Exception; }

    /** Sends a command and waits for a chat line matching the regex (from this command on). */
    private void chatStep(Minecraft mc, String name, String cmd, String regex, OnMatch then) {
        Pattern p = Pattern.compile(regex);
        Matcher[] found = new Matcher[1];
        steps.add(new Step(name, 0, () -> { mark = chat.size(); command(mc, cmd); }, () -> (found[0] = find(p)) != null));
        steps.add(new Step(name + " check", 0, () -> then.accept(found[0]), () -> true));
    }

    private void reload(Minecraft mc, String name, String args, OnMatch then) {
        chatStep(mc, name, "npcanim " + args, OK.pattern(), m -> {
            lastRevision = Integer.parseInt(m.group(1));
            lastHash = m.group(2);
            then.accept(m);
        });
    }

    private void statusSame(Minecraft mc, String key) {
        chatStep(mc, key, "npcanim status", STATUS.pattern(), m -> check(key, Integer.parseInt(m.group(1)) == lastRevision && m.group(2).equals(lastHash), m.group()));
    }

    private void step(String name, int wait, Act act, Cond until) { steps.add(new Step(name, wait, act, until)); }

    @SubscribeEvent
    public void chat(ClientChatReceivedEvent e) {
        String line = e.getMessage().getUnformattedText();
        chat.add(line);
        System.out.println("P7C CHAT " + line);
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
                System.out.println("P7C STEP " + step.name);
            }
            if (System.nanoTime() - since > 90000000000L) { throw new IllegalStateException("Timeout " + step.name + "; chat tail " + lastChat()); }
            if (delay-- > 0) { return; }
            if (!acted) { acted = true; step.act.run(); if (finished) { return; } }
            if (step.until.ok()) { step = null; }
        } catch (Exception | LinkageError problem) { problem.printStackTrace(); finish(mc, false, problem.toString()); }
    }

    // ------------------------------------------------------------------ files
    private JsonObject jarFile() throws Exception {
        try (InputStream in = Class.forName("dev.jakak.customnpcentities.anim.AnimLibrary").getResourceAsStream("/assets/customnpcentities/animations/" + ID + ".animation.json")) {
            return new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static JsonObject sentinel(float x, float y, float z) {
        JsonArray v = new JsonArray();
        v.add(x); v.add(y); v.add(z);
        JsonObject key = new JsonObject(); key.add("vector", v);
        JsonObject keys = new JsonObject(); keys.add("0.0", key);
        JsonObject bone = new JsonObject(); bone.add("rotation", keys);
        return bone;
    }

    private static JsonObject clip(float x, float y, float z, boolean lunge) {
        JsonObject c = new JsonObject();
        c.addProperty("animation_length", 3.0f);
        JsonObject bones = new JsonObject(); bones.add(SENTINEL, sentinel(x, y, z));
        c.add("bones", bones);
        if (lunge) {
            JsonObject event = new JsonObject();
            event.addProperty("t", 0.1f); event.addProperty("type", "lunge"); event.addProperty("forward", 0.5f); event.addProperty("ticks", 6);
            JsonArray events = new JsonArray(); events.add(event);
            c.add("events", events);
        }
        return c;
    }

    private JsonObject r1() throws Exception {
        JsonObject root = jarFile(), all = root.getAsJsonObject("animations"), idleBones = all.getAsJsonObject("idle").getAsJsonObject("bones");
        idleBones.add(SENTINEL, sentinel(11, 22, 33));
        idleBones.add("neck", sentinel(40, 0, 0));
        all.getAsJsonObject("hurt").addProperty("animation_length", 0.9f);
        all.add("aaa_p7c", clip(5, 6, 7, true));
        return root;
    }

    private JsonObject r2() throws Exception {
        JsonObject root = jarFile(), all = root.getAsJsonObject("animations");
        all.getAsJsonObject("idle").getAsJsonObject("bones").add(SENTINEL, sentinel(1, 2, 3));
        all.add("zzz_p7c", clip(8, 9, 10, false));
        return root;
    }

    private JsonObject bad() throws Exception {
        JsonObject root = r1();
        root.getAsJsonObject("animations").getAsJsonObject("aaa_p7c").addProperty("animation_length", -1);
        if (AnimFileValidator.check(root.toString(), Collections.<String>emptyList()) == null) { throw new IllegalStateException("bad file passes"); }
        return root;
    }

    private void write(JsonElement root) throws Exception {
        file.getParentFile().mkdirs();
        Files.write(file.toPath(), root.toString().getBytes(StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------ client view
    private static Class<?> base() throws ClassNotFoundException { return Class.forName(ANIMATED); }

    private List<Entity> bears(Minecraft mc) throws Exception {
        List<Entity> out = new ArrayList<>();
        if (mc.world == null) { return out; }
        for (Entity e : new ArrayList<>(mc.world.loadedEntityList)) { if (base().isInstance(e)) { out.add(e); } }
        out.sort((a, b) -> Integer.compare(a.getEntityId(), b.getEntityId()));
        return out;
    }

    private static Object library(Entity npc) throws Exception { return base().getMethod("animations").invoke(npc); }

    @SuppressWarnings("unchecked")
    private static List<String> names(Entity npc) throws Exception {
        Object lib = library(npc);
        return (List<String>) lib.getClass().getMethod("sortedClipNames").invoke(lib);
    }

    private static Object clipOf(Entity npc, String name) throws Exception {
        Object lib = library(npc);
        return lib.getClass().getMethod("clipOrNull", String.class).invoke(lib, name);
    }

    private static float length(Entity npc, String name) throws Exception {
        Object c = clipOf(npc, name);
        return c.getClass().getField("length").getFloat(c);
    }

    private static int events(Entity npc, String name) throws Exception {
        Object c = clipOf(npc, name);
        return ((List<?>) c.getClass().getField("events").get(c)).size();
    }

    /** Rotation of the sentinel bone the clip writes, through the optional mod's own sampler. */
    private static float[] sample(Entity npc, String name) throws Exception {
        Object c = clipOf(npc, name);
        Class<?> poseClass = Class.forName("dev.jakak.customnpcentities.anim.Pose");
        Method sample = c.getClass().getMethod("sample", float.class, poseClass);
        Object pose = sample.invoke(c, 0.5f, poseClass.newInstance());
        return ((Map<String, float[]>) poseClass.getField("bones").get(pose)).get(SENTINEL);
    }

    private static boolean near(float[] v, float x, float y, float z) {
        return v != null && Math.abs(v[0] - x) < 0.01 && Math.abs(v[1] - y) < 0.01 && Math.abs(v[2] - z) < 0.01;
    }

    private boolean renderedNear(float x, float y, float z) {
        synchronized (rendered) {
            for (Map<String, float[]> pose : rendered.values()) { if (near(pose.get(SENTINEL), x, y, z)) { return true; } }
        }
        return false;
    }

    /** Clip the client resolves for any playing bear, or null. */
    private String playing(Minecraft mc) throws Exception {
        for (Entity bear : bears(mc)) {
            Object clip = base().getMethod("getActionClip").invoke(bear);
            if (clip != null) { return (String) clip.getClass().getField("name").get(clip); }
        }
        return null;
    }

    private void keep(Minecraft mc) throws Exception {
        for (Entity bear : bears(mc)) {
            if ((AnimBlend.token(bear) & 0xFF) != 0) { keptEntity = bear.getEntityId(); keptToken = AnimBlend.token(bear); return; }
        }
        throw new IllegalStateException("no playing bear");
    }

    private boolean kept(Minecraft mc) throws Exception {
        for (Entity bear : bears(mc)) { if (bear.getEntityId() == keptEntity) { return AnimBlend.token(bear) == keptToken; } }
        return false;
    }

    private void track(Minecraft mc) throws Exception {
        Entity bear = bears(mc).get(0);
        moved = Math.max(moved, Math.hypot(bear.posX - startX, bear.posZ - startZ));
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

    private String lastChat() { synchronized (chat) { return chat.isEmpty() ? "" : chat.get(chat.size() - 1); } }

    private boolean lastChatLacks(String text) {
        synchronized (chat) {
            for (int i = mark; i < chat.size(); i++) { if (chat.get(i).contains(text)) { return false; } }
        }
        return true;
    }

    private static boolean inWorld(Minecraft mc) { return mc.world != null && mc.player != null && mc.currentScreen == null; }

    private void connect(Minecraft mc) {
        if (dedicated) {
            mc.displayGuiScreen(new GuiConnecting(new GuiMainMenu(), mc, "127.0.0.1", Integer.parseInt(job.getProperty("connect"))));
        } else {
            mc.launchIntegratedServer(job.getProperty("world"), job.getProperty("world"),
                    new WorldSettings(HarnessJob.seedOf("customnpc-rework"), GameType.CREATIVE, false, false, WorldType.FLAT).enableCommands());
        }
    }

    private static void leave(Minecraft mc) {
        if (mc.world != null) { mc.world.sendQuittingDisconnectingPacket(); mc.loadWorld(null); }
        mc.displayGuiScreen(new GuiMainMenu());
    }

    private static void command(Minecraft mc, String text) { mc.player.sendChatMessage("/" + text); }

    private void check(String key, boolean ok, String detail) {
        JsonObject row = new JsonObject();
        row.addProperty("key", key); row.addProperty("ok", ok); row.addProperty("detail", detail);
        checks.add(row);
        System.out.println("P7C CHECK " + key + " ok=" + ok + " " + detail);
        if (!ok) { throw new IllegalStateException(key + ": " + detail); }
    }

    private void snapshot(Minecraft mc, String label) {
        String name = out.getName().replace(".json", "") + "-" + label + ".png";
        ScreenShotHelper.saveScreenshot(out.getParentFile(), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        screenshots.add(name);
    }

    private void finish(Minecraft mc, boolean ok, String error) {
        finished = true;
        AnimBlend.setProbe(null);
        try {
            leave(mc);
            JsonObject proof = new JsonObject();
            proof.addProperty("success", ok); proof.addProperty("error", error);
            proof.add("checks", checks); proof.add("screenshots", screenshots); proof.add("data", data);
            try (Writer writer = Files.newBufferedWriter(out.toPath(), StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(proof, writer);
            }
            System.out.println("P7C DONE success=" + ok);
        } catch (Exception problem) { problem.printStackTrace(); }
        mc.shutdown();
    }
}
