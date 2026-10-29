package noppes.npcs.rework.diag;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;
import javax.tools.JavaCompiler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.script.GuiScript;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.GuiTextArea;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.entity.data.DataScript;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.script.hooks.JavaLanguage;
import noppes.npcs.rework.script.hooks.ScriptHooks;

/** M6.12 controller-only acceptance of the existing NPC script GUI with Java (Q184-Q187).
 * The scripter item and normal GUI handlers (language/enabled buttons, tab "+", script list,
 * Esc) send the real ScriptDataGet/ScriptDataSave packets; the harness never writes a language
 * or script on the server. Server-thread work is read-only except clearing the probe stored-data
 * key and, for the no-compiler round only, swapping the Java compiler for an absent provider.
 * Edit pass: JS -> Java (JS text kept) -> Java with include -> JS -> Java without compiler ->
 * Java, then the existing clone commands. Verify pass: a second JVM reopens and saves both NPCs.
 */
@SideOnly(Side.CLIENT)
public final class ScriptGuiHarness {
    static final String KEY = "m612";
    static final String INCLUDE = "m612/helper.java";
    static final String JS1 = "function tick(e) {\n    e.npc.getStoreddata().put(\"m612\", \"js-1:\" + e.npc.getUUID());\n}";
    static final String JS2 = "function tick(e) {\n    e.npc.getStoreddata().put(\"m612\", \"js-2:\" + e.npc.getUUID());\n}";
    static final String JAVA1 = "import m612.Helper;\nimport noppes.npcs.api.event.NpcEvent;\nimport noppes.npcs.rework.script.NpcScript;\n\n"
            + "public class GuiJava extends NpcScript {\n    @Override\n    public void tick(NpcEvent.UpdateEvent e) {\n"
            + "        e.npc.getStoreddata().put(\"m612\", \"java-1:\" + Helper.tag() + \":\" + e.npc.getUUID());\n    }\n}";
    static final String JAVA2 = "import noppes.npcs.api.event.NpcEvent;\nimport noppes.npcs.rework.script.NpcScript;\n\n"
            + "public class GuiJava2 extends NpcScript {\n    @Override\n    public void tick(NpcEvent.UpdateEvent e) {\n"
            + "        e.npc.getStoreddata().put(\"m612\", \"java-2:\" + e.npc.getUUID());\n    }\n}";
    private static final String JS = "ECMAScript";
    /** Edit rounds: language to save, new tab code (null = keep), includes (0 keep, 1 all, 2 none),
     * expected stored-data prefix ("" = no script ran), expected console code ("" = none). */
    private static final String[][] EDIT = {
        {JS, JS1, "0", "js-1:", ""},
        {ScriptHooks.JAVA, null, "0", "", "unit.noPublicType"},
        {ScriptHooks.JAVA, JAVA1, "1", "java-1:helper:", ""},
        {JS, JS2, "2", "js-2:", ""},
        {ScriptHooks.JAVA, JAVA2, "0", "", "compiler.unavailable"},
        {ScriptHooks.JAVA, null, "0", "java-2:", ""},
    };
    private final Properties job;
    private final File out;
    private final boolean verify;
    private final JsonArray assertions = new JsonArray();
    private final JsonArray screenshots = new JsonArray();
    private final JsonObject uuids = new JsonObject();
    private String phase = "MENU";
    private long since = System.nanoTime();
    private int delay;
    private int round;
    private String savedLanguage = JS;
    private String savedCode = "";
    private Future<?> task;
    private volatile String taskError;
    private volatile JsonObject observation;
    private volatile Object realCompiler;
    private boolean finished;

    private ScriptGuiHarness(Properties job) {
        this.job = job;
        out = new File(job.getProperty("out"));
        verify = "verify".equals(job.getProperty("pass"));
        if (verify) { savedLanguage = ScriptHooks.JAVA; savedCode = JAVA2; }
    }

    static boolean installIfRequested(String path) throws Exception {
        Properties p = new Properties();
        try (InputStreamReader in = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)) { p.load(in); }
        if (!"1".equals(p.getProperty("scriptGui"))) { return false; }
        if (!p.containsKey("out") || !p.containsKey("world")) { throw new IllegalArgumentException("Incomplete scriptGui job"); }
        MinecraftForge.EVENT_BUS.register(new ScriptGuiHarness(p));
        System.out.println("M612 INSTALL pass=" + p.getProperty("pass"));
        return true;
    }

    private int rounds() { return verify ? 2 : EDIT.length; }
    private String name() { return verify && round == 1 ? "M612_A_Clone" : "M612_A"; }
    private String language() { return verify ? ScriptHooks.JAVA : EDIT[round][0]; }
    private String code() { return verify ? null : EDIT[round][1]; }
    private int includes() { return verify ? 0 : Integer.parseInt(EDIT[round][2]); }
    private String marker() { return verify ? "java-2:" : EDIT[round][3]; }
    private String console() { return verify ? "" : EDIT[round][4]; }
    private String expectedCode() { return code() == null ? savedCode : code(); }
    private List<String> expectedIncludes() {
        return !verify && round >= 2 && round < 3 ? Collections.singletonList(INCLUDE) : Collections.<String>emptyList();
    }

    private void enter(String next) {
        phase = next;
        since = System.nanoTime();
        delay = 10;
        System.out.println("M612 PHASE " + phase + " round=" + round);
    }

    private void check(String key, boolean ok, String detail) {
        JsonObject row = new JsonObject();
        row.addProperty("key", key); row.addProperty("ok", ok); row.addProperty("detail", detail);
        assertions.add(row);
        System.out.println("M612 CHECK " + key + " ok=" + ok + " " + detail);
        if (!ok) { throw new IllegalStateException(key + ": " + detail); }
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || finished) { return; }
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (taskError != null) { throw new IllegalStateException(taskError); }
            if (System.nanoTime() - since > 180000000000L) { throw new IllegalStateException("Timeout " + phase); }
            if (delay-- > 0) { return; }
            switch (phase) {
                case "MENU": {
                    if (!(mc.currentScreen instanceof GuiMainMenu)) { break; }
                    check("config-classpath", !CustomNpcs.RwJavaClasspath.trim().isEmpty(), "RwJavaClasspath from CustomNpcs.cfg");
                    check("language-registered", JavaLanguage.EXTENSION.equals(ScriptController.Instance.languages.get(ScriptHooks.JAVA))
                            && ".js".equals(ScriptController.Instance.languages.get(JS)), ScriptController.Instance.languages.toString());
                    mc.gameSettings.pauseOnLostFocus = false;
                    File level = new File(new File(new File(mc.mcDataDir, "saves"), job.getProperty("world")), "level.dat");
                    check("world-existence", level.isFile() == verify, "exists=" + level.isFile());
                    enter("JOIN");
                    WorldSettings settings = verify ? null : new WorldSettings(HarnessJob.seedOf("customnpc-rework"),
                            GameType.CREATIVE, false, false, WorldType.FLAT).enableCommands();
                    mc.launchIntegratedServer(job.getProperty("world"), job.getProperty("world"), settings);
                    break;
                }
                case "JOIN": {
                    if (mc.world == null || mc.player == null || mc.currentScreen != null) { break; }
                    final String player = mc.player.getName();
                    submit(mc, new Runnable() { @Override public void run() {
                        MinecraftServer s = mc.getIntegratedServer();
                        command(s, "setworldspawn 0 4 0"); command(s, "gamemode 1 " + player);
                        command(s, "replaceitem entity " + player + " slot.hotbar.0 customnpcs:npcscripter");
                        command(s, "gamerule doMobSpawning false"); command(s, "time set 1000");
                        if (!verify) { command(s, "noppes clone spawn M612_A 1 0,4,0"); }
                        JsonObject o = new JsonObject();
                        o.addProperty("defaultLanguage", new DataScript(null).getLanguage());
                        o.addProperty("include", ScriptController.Instance.scripts.containsKey(INCLUDE)
                                && ScriptController.Instance.scripts.get(INCLUDE).contains("class Helper"));
                        observation = o;
                    }});
                    enter("SETUP"); delay = 100;
                    break;
                }
                case "SETUP":
                    if (!completed()) { break; }
                    check("integrated-player", mc.isIntegratedServerRunning() && mc.player != null, "real local player");
                    check("default-language", JS.equals(observation.get("defaultLanguage").getAsString()), observation.toString());
                    check("include-loaded", observation.get("include").getAsBoolean(), "world scripts/java/" + INCLUDE);
                    enter("PLACE");
                    break;
                case "PLACE":
                    submit(mc, new Runnable() { @Override public void run() {
                        EntityNPCInterface target = actor(mc.getIntegratedServer().getWorld(0), name());
                        if (target == null) { throw new IllegalStateException("Missing target " + name()); }
                        command(mc.getIntegratedServer(), "tp " + mc.player.getName() + " " + target.posX + " "
                                + target.posY + " " + (target.posZ + 2) + " 180 0");
                    }});
                    enter("OPEN"); delay = 40;
                    break;
                case "OPEN": {
                    if (!completed() || actor(mc.world, name()) == null || mc.player.getHeldItemMainhand().getItem() != CustomItems.scripter) { break; }
                    EntityNPCInterface npc = actor(mc.world, name());
                    if (!uuids.has(name())) { uuids.addProperty(name(), npc.getUniqueID().toString()); }
                    check(key("uuid"), uuids.get(name()).getAsString().equals(npc.getUniqueID().toString()), name());
                    mc.playerController.interactWithEntity(mc.player, npc, EnumHand.MAIN_HAND);
                    enter("GUI");
                    break;
                }
                case "GUI": {
                    if (!(mc.currentScreen instanceof GuiScript) || ((GuiScript) mc.currentScreen).languages.isEmpty()) { break; }
                    GuiScript gui = (GuiScript) mc.currentScreen;
                    check(key("gui-languages"), gui.languages.containsKey(JS) && gui.languages.containsKey(ScriptHooks.JAVA)
                            && Collections.singletonList(INCLUDE).equals(gui.languages.get(ScriptHooks.JAVA)), gui.languages.toString());
                    check(key("gui-language"), savedLanguage.equals(gui.getButton(103).displayString)
                            && savedLanguage.equals(gui.handler.getLanguage()), gui.getButton(103).displayString);
                    String consoleText = ((GuiTextArea) gui.get(2)).getText();
                    if (round > 0 && !verify && !EDIT[round - 1][4].isEmpty()) {
                        check(key("gui-console"), consoleText.contains(EDIT[round - 1][4]), "console has " + EDIT[round - 1][4]);
                    }
                    snapshot(mc, "settings");
                    if (!savedCode.isEmpty()) {
                        click(gui, gui.getTopButton(1));
                        check(key("gui-kept-source"), savedCode.equals(((GuiTextArea) gui.get(2)).getText()), "tab 1 shows saved source");
                        click(gui, gui.getTopButton(0));
                    }
                    if (!savedLanguage.equals(language())) { click(gui, gui.getButton(103)); }
                    check(key("pending-language"), language().equals(gui.getButton(103).displayString) && language().equals(gui.handler.getLanguage()),
                            gui.getButton(103).displayString);
                    if (!gui.handler.getEnabled()) { click(gui, gui.getButton(104)); }
                    check(key("pending-enabled"), gui.handler.getEnabled(), "enabled");
                    if (gui.handler.getScripts().isEmpty()) { click(gui, gui.getTopButton(12)); } else { click(gui, gui.getTopButton(1)); }
                    if (code() != null) { ((GuiTextArea) gui.get(2)).setText(code()); }
                    if (includes() != 0) {
                        click(gui, gui.getButton(107));
                        check(key("script-list"), gui.hasSubGui(), "GuiScriptList open");
                        click(gui, gui.getSubGui().getButton(includes() == 1 ? 3 : 4));
                        click(gui, gui.getSubGui().getButton(66));
                        check(key("script-list-closed"), !gui.hasSubGui(), "done");
                    }
                    ScriptContainer tab = gui.handler.getScripts().get(0);
                    check(key("pending-tab"), gui.handler.getScripts().size() == 1 && expectedCode().equals(tab.script)
                            && expectedIncludes().equals(tab.scripts), "tabs=" + gui.handler.getScripts().size() + " includes=" + tab.scripts);
                    if (!verify && round == 4) {
                        submit(mc, new Runnable() { @Override public void run() { realCompiler = swapCompiler(null, true); }});
                    }
                    enter("SAVE");
                    break;
                }
                case "SAVE":
                    if (!completed()) { break; }
                    ((GuiNPCInterface) mc.currentScreen).keyTyped((char) 0, 1); // Esc: normal close sends ScriptDataSave
                    enter("SAVED"); delay = 30;
                    break;
                case "SAVED":
                    check(key("closed"), mc.currentScreen == null, "closed with Esc");
                    submit(mc, new Runnable() { @Override public void run() {
                        EntityNPCInterface npc = actor(mc.getIntegratedServer().getWorld(0), name());
                        observation = observe(npc);
                        npc.wrappedNPC.getStoreddata().remove(KEY);
                    }});
                    enter("STORED");
                    break;
                case "STORED": {
                    if (!completed()) { break; }
                    JsonObject o = observation;
                    check(key("server-saved"), language().equals(o.get("language").getAsString()) && o.get("enabled").getAsBoolean()
                            && o.get("tabs").getAsInt() == 1 && expectedCode().equals(o.get("code").getAsString())
                            && expectedIncludes().toString().equals(o.get("includes").getAsString())
                            && language().equals(o.get("nbtLanguage").getAsString()) && uuids.get(name()).getAsString().equals(o.get("uuid").getAsString()),
                            o.toString());
                    savedLanguage = language(); savedCode = expectedCode();
                    enter("RUN"); delay = 80;
                    break;
                }
                case "RUN":
                    submit(mc, new Runnable() { @Override public void run() {
                        observation = observe(actor(mc.getIntegratedServer().getWorld(0), name()));
                        if (!verify && round == 4) { swapCompiler(realCompiler, false); }
                    }});
                    enter("RAN");
                    break;
                case "RAN": {
                    if (!completed()) { break; }
                    JsonObject o = observation;
                    String expected = marker().isEmpty() ? "" : marker() + uuids.get(name()).getAsString();
                    check(key("ran"), expected.equals(o.get("marker").getAsString()), "marker=" + o.get("marker").getAsString());
                    if (!console().isEmpty()) { check(key("server-console"), o.get("console").getAsString().contains(console()), "console has " + console()); }
                    if (++round < rounds()) { enter("PLACE"); }
                    else if (verify) { finish(mc, true, ""); }
                    else {
                        submit(mc, new Runnable() { @Override public void run() {
                            MinecraftServer s = mc.getIntegratedServer();
                            command(s, "noppes clone add M612_A 1 M612_Saved");
                            command(s, "noppes clone spawn M612_Saved 1 4,4,0 M612_A_Clone");
                        }});
                        enter("CLONE"); delay = 30;
                    }
                    break;
                }
                case "CLONE":
                    if (!completed()) { break; }
                    submit(mc, new Runnable() { @Override public void run() {
                        EntityNPCInterface clone = actor(mc.getIntegratedServer().getWorld(0), "M612_A_Clone");
                        observation = observe(clone);
                        clone.wrappedNPC.getStoreddata().remove(KEY);
                    }});
                    enter("CLONED");
                    break;
                case "CLONED": {
                    if (!completed()) { break; }
                    JsonObject o = observation;
                    check("clone-saved", ScriptHooks.JAVA.equals(o.get("language").getAsString()) && o.get("enabled").getAsBoolean()
                            && JAVA2.equals(o.get("code").getAsString()) && !o.get("uuid").getAsString().equals(uuids.get("M612_A").getAsString()), o.toString());
                    uuids.addProperty("M612_A_Clone", o.get("uuid").getAsString());
                    enter("CLONE_RUN"); delay = 80;
                    break;
                }
                case "CLONE_RUN":
                    submit(mc, new Runnable() { @Override public void run() {
                        observation = observe(actor(mc.getIntegratedServer().getWorld(0), "M612_A_Clone"));
                    }});
                    enter("CLONE_RAN");
                    break;
                case "CLONE_RAN":
                    if (!completed()) { break; }
                    check("clone-ran", ("java-2:" + uuids.get("M612_A_Clone").getAsString()).equals(observation.get("marker").getAsString()),
                            "marker=" + observation.get("marker").getAsString());
                    finish(mc, true, "");
                    break;
                default: throw new IllegalStateException(phase);
            }
        } catch (Exception | LinkageError problem) { problem.printStackTrace(); finish(mc, false, problem.toString()); }
    }

    private String key(String suffix) { return "r" + round + "-" + suffix; }

    /** Swaps JavaNpcScripts' lazily created compiler; absent = the real JdkScriptCompiler(null) path. */
    private static Object swapCompiler(Object replacement, boolean absent) {
        try {
            Field field = Class.forName("noppes.npcs.rework.script.hooks.JavaNpcScripts").getDeclaredField("compiler");
            field.setAccessible(true);
            Object previous = field.get(null);
            if (absent) {
                Constructor<?> c = Class.forName("noppes.npcs.rework.script.JdkScriptCompiler").getDeclaredConstructor(JavaCompiler.class);
                c.setAccessible(true);
                replacement = c.newInstance((Object) null);
            }
            field.set(null, replacement);
            return previous;
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    private void submit(Minecraft mc, final Runnable work) {
        task = mc.getIntegratedServer().addScheduledTask(new Runnable() { @Override public void run() {
            try { work.run(); } catch (Exception | LinkageError problem) { taskError = problem.toString(); }
        }});
    }

    private boolean completed() throws Exception {
        if (task == null) { return true; }
        if (!task.isDone()) { return false; }
        task.get();
        task = null;
        if (taskError != null) { throw new IllegalStateException(taskError); }
        return true;
    }

    private static JsonObject observe(EntityNPCInterface npc) {
        if (npc == null) { throw new IllegalStateException("Missing server actor"); }
        NBTTagCompound nbt = npc.writeToNBT(new NBTTagCompound());
        JsonObject o = new JsonObject();
        List<ScriptContainer> tabs = npc.script.getScripts();
        o.addProperty("language", npc.script.getLanguage()); o.addProperty("enabled", npc.script.getEnabled());
        o.addProperty("tabs", tabs.size()); o.addProperty("code", tabs.isEmpty() ? "" : tabs.get(0).script);
        o.addProperty("includes", tabs.isEmpty() ? "[]" : tabs.get(0).scripts.toString());
        o.addProperty("nbtLanguage", nbt.getString("ScriptLanguage")); o.addProperty("uuid", npc.getUniqueID().toString());
        Object marker = npc.wrappedNPC.getStoreddata().get(KEY);
        o.addProperty("marker", marker == null ? "" : marker.toString());
        StringBuilder console = new StringBuilder();
        for (String line : npc.script.getConsoleText().values()) { console.append(line).append('\n'); }
        o.addProperty("console", console.toString());
        return o;
    }

    private static EntityNPCInterface actor(World world, String name) {
        EntityNPCInterface found = null;
        for (Entity e : world.loadedEntityList) {
            if (!(e instanceof EntityNPCInterface) || !name.equals(e.getName())) { continue; }
            if (found != null) { throw new IllegalStateException("Duplicate " + name); }
            found = (EntityNPCInterface) e;
        }
        return found;
    }

    private static void command(MinecraftServer server, String text) {
        if (server.getCommandManager().executeCommand(server, text) < 1) { throw new IllegalStateException("Command failed: " + text); }
    }

    private static void click(GuiNPCInterface gui, GuiButton button) {
        if (button == null || !button.enabled || !button.visible) { throw new IllegalStateException("Unavailable button"); }
        int x = button.x + 2, y = button.y + 2;
        gui.drawScreen(x, y, 0); gui.mouseClicked(x, y, 0); gui.mouseReleased(x, y, 0);
    }

    private void snapshot(Minecraft mc, String what) {
        String name = out.getName().replace(".json", "") + "-r" + round + "-" + what + ".png";
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
            proof.addProperty("pass", verify ? "verify" : "edit"); proof.addProperty("rounds", round);
            proof.add("uuids", uuids); proof.add("assertions", assertions); proof.add("screenshots", screenshots);
            try (Writer writer = Files.newBufferedWriter(out.toPath(), StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(proof, writer);
            }
            System.out.println("M612 DONE success=" + ok + " pass=" + (verify ? "verify" : "edit"));
        } catch (Exception problem) { problem.printStackTrace(); }
        mc.shutdown();
    }
}
