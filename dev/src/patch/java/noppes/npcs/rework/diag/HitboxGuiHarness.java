package noppes.npcs.rework.diag;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;
import java.util.concurrent.Future;
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
import noppes.npcs.client.gui.mainmenu.GuiNpcDisplay;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.client.gui.util.GuiNpcMenu;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.entity.HitboxGui;

/** M3.8b controller-only acceptance. Wand and normal mouse handlers send real Display packets.
 * Never writes a mode, calls GUI save/actionPerformed, or mutates server Display data.
 * Server-thread observations are read-only; setup/global/clone use existing commands.
 */
@SideOnly(Side.CLIENT)
public final class HitboxGuiHarness {
    // actor, mode at open, mode to save, HasHitbox at open, HasHitbox to save, global.
    private static final int[][] STEPS = {
        {0,0,1,1,1,1}, {0,1,2,1,1,1}, {0,2,0,1,1,1}, {0,0,0,1,1,1},
        {1,0,1,1,1,1}, {1,1,1,1,1,1}, {2,0,2,1,1,1},
        {2,2,2,1,1,0}, {2,2,2,1,1,0}, {2,2,2,1,0,1},
        {2,2,2,0,1,1}, {2,2,2,1,1,1}
    };
    private final Properties job;
    private final File out;
    private final boolean verify;
    private final JsonArray assertions = new JsonArray();
    private final JsonArray screenshots = new JsonArray();
    private final JsonObject uuids = new JsonObject();
    private final int originalGlobal;
    private String phase = "MENU";
    private long since = System.nanoTime();
    private int delay;
    private int round;
    private Future<?> task;
    private volatile String taskError;
    private volatile JsonObject observation;
    private boolean finished;

    private HitboxGuiHarness(Properties job) {
        this.job = job;
        out = new File(job.getProperty("out"));
        verify = "verify".equals(job.getProperty("pass"));
        originalGlobal = CustomNpcs.RwHitbox;
    }

    static boolean installIfRequested(String path) throws Exception {
        Properties p = new Properties();
        try (InputStreamReader in = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)) { p.load(in); }
        if (!"1".equals(p.getProperty("hitboxGui"))) { return false; }
        if (!p.containsKey("out") || !p.containsKey("world")) { throw new IllegalArgumentException("Incomplete hitboxGui job"); }
        MinecraftForge.EVENT_BUS.register(new HitboxGuiHarness(p));
        System.out.println("HBGUI INSTALL pass=" + p.getProperty("pass"));
        return true;
    }

    private void enter(String next) {
        phase = next;
        since = System.nanoTime();
        delay = 10;
        System.out.println("HBGUI PHASE " + phase + " round=" + round);
    }

    private void check(String key, boolean ok, String detail) {
        JsonObject row = new JsonObject();
        row.addProperty("key", key); row.addProperty("ok", ok); row.addProperty("detail", detail);
        assertions.add(row);
        System.out.println("HBGUI CHECK " + key + " ok=" + ok + " " + detail);
        if (!ok) { throw new IllegalStateException(key + ": " + detail); }
    }

    private String name() { return "HBGUI_" + (verify ? round % 3 : STEPS[round][0]) + (verify && round >= 3 ? "_Clone" : ""); }
    private int before() { return verify ? round % 3 : STEPS[round][1]; }
    private int after() { return verify ? round % 3 : STEPS[round][2]; }
    private boolean hasBefore() { return verify || STEPS[round][3] == 1; }
    private boolean hasAfter() { return verify || STEPS[round][4] == 1; }
    private int global() { return verify ? 1 : STEPS[round][5]; }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || finished) { return; }
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (taskError != null) { throw new IllegalStateException(taskError); }
            if (System.nanoTime() - since > 180000000000L) { throw new IllegalStateException("Timeout " + phase); }
            if (delay-- > 0) { return; }
            switch (phase) {
                case "MENU":
                    if (!(mc.currentScreen instanceof GuiMainMenu)) { break; }
                    check("default-global", originalGlobal == 0, "RwHitbox=" + originalGlobal);
                    mc.gameSettings.pauseOnLostFocus = false;
                    File level = new File(new File(new File(mc.mcDataDir, "saves"), job.getProperty("world")), "level.dat");
                    check("world-existence", level.isFile() == verify, "exists=" + level.isFile());
                    enter("JOIN");
                    WorldSettings settings = verify ? null : new WorldSettings(HarnessJob.seedOf("customnpc-rework"),
                            GameType.CREATIVE, false, false, WorldType.FLAT).enableCommands();
                    mc.launchIntegratedServer(job.getProperty("world"), job.getProperty("world"), settings);
                    break;
                case "JOIN":
                    if (mc.world == null || mc.player == null || mc.currentScreen != null) { break; }
                    final String player = mc.player.getName();
                    submit(mc, new Runnable() { @Override public void run() {
                        MinecraftServer s = mc.getIntegratedServer();
                        command(s, "setworldspawn 0 4 0"); command(s, "gamemode 1 " + player);
                        command(s, "tp " + player + " 0 4 2 180 0");
                        command(s, "replaceitem entity " + player + " slot.hotbar.0 customnpcs:npcwand");
                        command(s, "gamerule doMobSpawning false"); command(s, "time set 1000");
                        if (!verify) {
                            for (int i = 0; i < 3; i++) { command(s, "noppes clone spawn HBGUI_" + i + " 1 " + (i * 3) + ",4,0"); }
                        }
                    }});
                    enter("SETUP"); delay = 100;
                    break;
                case "SETUP":
                    if (!completed()) { break; }
                    check("integrated-player", mc.isIntegratedServerRunning() && mc.player != null, "real local player");
                    enter("GLOBAL");
                    break;
                case "GLOBAL":
                    submit(mc, new Runnable() { @Override public void run() {
                        command(mc.getIntegratedServer(), "rwhitbox global " + global());
                        EntityNPCInterface target = actor(mc.getIntegratedServer().getWorld(0), name());
                        if (target == null) { throw new IllegalStateException("Missing target " + name()); }
                        command(mc.getIntegratedServer(), "tp " + mc.player.getName() + " " + target.posX + " "
                                + target.posY + " " + (target.posZ + 2) + " 180 0");
                        // The existing global command does not broadcast. Request normal UPDATE_NPC,
                        // so the next assertions exercise actual spawn serialization on both sides.
                        for (Entity entity : mc.getIntegratedServer().getWorld(0).loadedEntityList) {
                            if (entity instanceof EntityNPCInterface && entity.getName().startsWith("HBGUI_")) {
                                ((EntityNPCInterface) entity).updateClient = true;
                            }
                        }
                    }});
                    enter("OPEN"); delay = 60;
                    break;
                case "OPEN":
                    if (!completed() || actor(mc.world, name()) == null || mc.player.getHeldItemMainhand().getItem() != CustomItems.wand) { break; }
                    EntityNPCInterface npc = actor(mc.world, name());
                    if (uuids.has(name())) { check(key("uuid"), uuids.get(name()).getAsString().equals(npc.getUniqueID().toString()), name()); }
                    else { uuids.addProperty(name(), npc.getUniqueID().toString()); }
                    check(key("spawn-stored"), npc.getRwHitboxModeForGui() == before(), "mode=" + npc.getRwHitboxModeForGui());
                    check(key("spawn-effective"), npc.getRwHitboxEffective() == (global() == 1 && hasBefore() ? before() : 0), "effective=" + npc.getRwHitboxEffective());
                    mc.playerController.interactWithEntity(mc.player, npc, EnumHand.MAIN_HAND);
                    enter("DISPLAY");
                    break;
                case "DISPLAY":
                    if (!(mc.currentScreen instanceof GuiNpcDisplay)) { break; }
                    GuiNpcDisplay gui = (GuiNpcDisplay) mc.currentScreen;
                    check(key("gui-mode"), gui.getButton(14).enabled && gui.getButton(14).getValue() == before(), gui.getButton(14).displayString);
                    check(key("gui-label"), gui.getButton(14).displayString.equals(HitboxGui.labels()[before()]), gui.getButton(14).displayString);
                    check(key("gui-hitbox"), gui.getButton(13).getValue() == (hasBefore() ? 1 : 0), "value=" + gui.getButton(13).getValue());
                    check(key("gui-hint"), HitboxGui.HINT.equals(gui.getLabel(15).label), gui.getLabel(15).label);
                    GuiButton b = gui.getButton(14);
                    snapshot(mc);
                    check(key("gui-bounds"), b.x >= 0 && b.y >= 0 && b.x + b.width <= gui.width && b.y + b.height <= gui.height,
                            "button=" + b.x + "," + b.y + "," + b.width + "," + b.height + " screen=" + gui.width + "," + gui.height);
                    for (int i = 0; i < (after() - before() + 3) % 3; i++) { click(gui, gui.getButton(14)); }
                    if (hasBefore() != hasAfter()) { click(gui, gui.getButton(13)); }
                    check(key("pending"), gui.getButton(14).getValue() == after() && gui.getButton(13).getValue() == (hasAfter() ? 1 : 0), "pending=" + after());
                    enter("SAVE");
                    break;
                case "SAVE":
                    top((GuiNPCInterface2) mc.currentScreen, 0); // normal close invokes Display save packet
                    enter("SAVED"); delay = 60;
                    break;
                case "SAVED":
                    check(key("closed"), mc.currentScreen == null, "closed via menu");
                    submit(mc, new Runnable() { @Override public void run() {
                        observation = observe(actor(mc.getIntegratedServer().getWorld(0), name()));
                    }});
                    enter("OBSERVE");
                    break;
                case "OBSERVE":
                    if (!completed()) { break; }
                    check(key("server"), observation.get("stored").getAsInt() == after()
                            && observation.get("hasHitbox").getAsBoolean() == hasAfter()
                            && observation.get("effective").getAsInt() == (global() == 1 && hasAfter() ? after() : 0)
                            && observation.get("nbtMode").getAsInt() == after()
                            && observation.get("nbtKey").getAsBoolean() == (after() != 0)
                            && observation.get("uuid").getAsString().equals(uuids.get(name()).getAsString()), observation.toString());
                    EntityNPCInterface current = actor(mc.world, name());
                    check(key("client-saved"), current.getRwHitboxModeForGui() == after() && current.display.getHasHitbox() == hasAfter()
                            && current.getRwHitboxEffective() == (global() == 1 && hasAfter() ? after() : 0), "stored=" + current.getRwHitboxModeForGui());
                    if (++round < (verify ? 6 : STEPS.length)) { enter("GLOBAL"); }
                    else if (verify) { finish(mc, true, ""); }
                    else {
                        submit(mc, new Runnable() { @Override public void run() {
                            MinecraftServer s = mc.getIntegratedServer();
                            for (int i = 0; i < 3; i++) {
                                command(s, "noppes clone add HBGUI_" + i + " 1 HBGUI_Saved_" + i);
                                command(s, "noppes clone spawn HBGUI_Saved_" + i + " 1 " + (i * 3) + ",4,4 HBGUI_" + i + "_Clone");
                            }
                        }});
                        enter("CLONES"); delay = 60;
                    }
                    break;
                case "CLONES":
                    if (!completed()) { break; }
                    for (int i = 0; i < 3; i++) {
                        EntityNPCInterface clone = actor(mc.world, "HBGUI_" + i + "_Clone");
                        check("clone" + i, clone != null && clone.getRwHitboxModeForGui() == i && clone.getRwHitboxEffective() == i
                                && !clone.getUniqueID().toString().equals(uuids.get("HBGUI_" + i).getAsString()), "mode=" + (clone == null ? -1 : clone.getRwHitboxModeForGui()));
                        uuids.addProperty("HBGUI_" + i + "_Clone", clone.getUniqueID().toString());
                    }
                    finish(mc, true, "");
                    break;
                default: throw new IllegalStateException(phase);
            }
        } catch (Exception | LinkageError problem) { problem.printStackTrace(); finish(mc, false, problem.toString()); }
    }

    private String key(String suffix) { return "r" + round + "-" + suffix; }

    private void submit(Minecraft mc, final Runnable work) {
        task = mc.getIntegratedServer().addScheduledTask(new Runnable() { @Override public void run() {
            try { work.run(); } catch (Exception | LinkageError problem) { taskError = problem.toString(); }
        }});
    }

    private boolean completed() throws Exception {
        if (task == null || !task.isDone()) { return false; }
        task.get();
        if (taskError != null) { throw new IllegalStateException(taskError); }
        return true;
    }

    private static JsonObject observe(EntityNPCInterface npc) {
        if (npc == null) { throw new IllegalStateException("Missing server actor"); }
        NBTTagCompound nbt = npc.writeToNBT(new NBTTagCompound());
        JsonObject o = new JsonObject();
        o.addProperty("stored", npc.getRwHitboxMode()); o.addProperty("effective", npc.getRwHitboxEffective());
        o.addProperty("hasHitbox", npc.display.getHasHitbox()); o.addProperty("nbtMode", nbt.getInteger("RwHitboxMode"));
        o.addProperty("nbtKey", nbt.hasKey("RwHitboxMode")); o.addProperty("uuid", npc.getUniqueID().toString());
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

    private static void click(GuiNPCInterface2 gui, GuiButton button) {
        if (button == null || !button.enabled || !button.visible) { throw new IllegalStateException("Unavailable button"); }
        int x = button.x + 2, y = button.y + 2;
        gui.drawScreen(x, y, 0); gui.mouseClicked(x, y, 0); gui.mouseReleased(x, y, 0);
    }

    private static void top(GuiNPCInterface2 gui, int id) throws Exception {
        Field menu = GuiNPCInterface2.class.getDeclaredField("menu"); menu.setAccessible(true);
        Field buttons = GuiNpcMenu.class.getDeclaredField("topButtons"); buttons.setAccessible(true);
        for (GuiButton b : (GuiButton[]) buttons.get(menu.get(gui))) { if (b.id == id) { click(gui, b); return; } }
        throw new IllegalStateException("Missing menu button " + id);
    }

    private void snapshot(Minecraft mc) {
        String name = out.getName().replace(".json", "") + "-r" + round + ".png";
        ScreenShotHelper.saveScreenshot(out.getParentFile(), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        if (!new File(new File(out.getParentFile(), "screenshots"), name).isFile()) { throw new IllegalStateException("Missing screenshot"); }
        screenshots.add(name);
    }

    private void finish(Minecraft mc, boolean ok, String error) {
        finished = true;
        try {
            if (mc.getIntegratedServer() != null) {
                mc.getIntegratedServer().addScheduledTask(new Runnable() { @Override public void run() {
                    command(mc.getIntegratedServer(), "rwhitbox global " + originalGlobal);
                }}).get();
            }
            if (mc.world != null) { mc.world.sendQuittingDisconnectingPacket(); mc.loadWorld(null); }
            mc.displayGuiScreen(new GuiMainMenu());
            JsonObject proof = new JsonObject();
            proof.addProperty("success", ok); proof.addProperty("error", error);
            proof.addProperty("pass", verify ? "verify" : "edit"); proof.addProperty("rounds", round);
            proof.add("uuids", uuids); proof.add("assertions", assertions); proof.add("screenshots", screenshots);
            try (Writer writer = Files.newBufferedWriter(out.toPath(), StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(proof, writer);
            }
            System.out.println("HBGUI DONE success=" + ok + " pass=" + (verify ? "verify" : "edit"));
        } catch (Exception problem) { problem.printStackTrace(); }
        mc.shutdown();
    }
}
