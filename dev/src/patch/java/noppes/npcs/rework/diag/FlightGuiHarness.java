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
import noppes.npcs.client.gui.SubGuiNpcMovement;
import noppes.npcs.client.gui.mainmenu.GuiNpcAI;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcMenu;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.movement.FlightMode;
import noppes.npcs.rework.movement.HoverMoveHelper;

/** P6 controller-only acceptance. Wand and normal mouse handlers send real AI packets.
 * Never writes a flight mode or calls GUI save/actionPerformed directly. Server-thread
 * observations are read-only; setup and clones use existing commands; the MOMENTUM actor
 * comes from its clone fixture.
 */
@SideOnly(Side.CLIENT)
public final class FlightGuiHarness {
    // actor, movementType at open, mode at open, mode to save, movementType to save.
    private static final int[][] STEPS = {
        {0,1,0,1,1}, {0,1,1,1,1}, {0,1,1,0,1},
        {1,0,0,1,1}, {2,1,2,2,1}, {0,1,0,1,0}
    };
    // verify: per actor in VERIFY_NAMES, movementType and mode (unchanged save).
    private static final String[] VERIFY_NAMES = {"FLGUI_0", "FLGUI_1", "FLGUI_2", "FLGUI_0_Clone", "FLGUI_1_Clone", "FLGUI_2_Clone"};
    private static final int[][] VERIFY = {{0,1}, {1,1}, {1,2}, {0,1}, {1,1}, {1,2}};
    private static final String[] LABELS = {"Fly: Orig", "Fly: Hover", "Fly: Mom."};
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
    private Future<?> task;
    private volatile String taskError;
    private volatile JsonObject observation;
    private volatile JsonObject cloneObservation;
    private boolean finished;

    private FlightGuiHarness(Properties job) {
        this.job = job;
        out = new File(job.getProperty("out"));
        verify = "verify".equals(job.getProperty("pass"));
    }

    static boolean installIfRequested(String path) throws Exception {
        Properties p = new Properties();
        try (InputStreamReader in = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)) { p.load(in); }
        if (!"1".equals(p.getProperty("flightGui"))) { return false; }
        if (!p.containsKey("out") || !p.containsKey("world")) { throw new IllegalArgumentException("Incomplete flightGui job"); }
        MinecraftForge.EVENT_BUS.register(new FlightGuiHarness(p));
        System.out.println("FLGUI INSTALL pass=" + p.getProperty("pass"));
        return true;
    }

    private void enter(String next) {
        phase = next;
        since = System.nanoTime();
        delay = 10;
        System.out.println("FLGUI PHASE " + phase + " round=" + round);
    }

    private void check(String key, boolean ok, String detail) {
        JsonObject row = new JsonObject();
        row.addProperty("key", key); row.addProperty("ok", ok); row.addProperty("detail", detail);
        assertions.add(row);
        System.out.println("FLGUI CHECK " + key + " ok=" + ok + " " + detail);
        if (!ok) { throw new IllegalStateException(key + ": " + detail); }
    }

    private int rounds() { return verify ? VERIFY.length : STEPS.length; }
    private String name() { return verify ? VERIFY_NAMES[round] : "FLGUI_" + STEPS[round][0]; }
    private int movBefore() { return verify ? VERIFY[round][0] : STEPS[round][1]; }
    private int before() { return verify ? VERIFY[round][1] : STEPS[round][2]; }
    private int after() { return verify ? VERIFY[round][1] : STEPS[round][3]; }
    private int movAfter() { return verify ? VERIFY[round][0] : STEPS[round][4]; }

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
                            for (int i = 0; i < 3; i++) { command(s, "noppes clone spawn FLGUI_" + i + " 1 " + (i * 3) + ",4,0"); }
                        }
                    }});
                    enter("SETUP"); delay = 100;
                    break;
                case "SETUP":
                    if (!completed()) { break; }
                    check("integrated-player", mc.isIntegratedServerRunning() && mc.player != null, "real local player");
                    enter("POSITION");
                    break;
                case "POSITION":
                    submit(mc, new Runnable() { @Override public void run() {
                        EntityNPCInterface target = actor(mc.getIntegratedServer().getWorld(0), name());
                        if (target == null) { throw new IllegalStateException("Missing target " + name()); }
                        command(mc.getIntegratedServer(), "tp " + mc.player.getName() + " " + target.posX + " "
                                + target.posY + " " + (target.posZ + 2) + " 180 0");
                    }});
                    enter("OPEN"); delay = 40;
                    break;
                case "OPEN":
                    if (!completed() || actor(mc.world, name()) == null || mc.player.getHeldItemMainhand().getItem() != CustomItems.wand) { break; }
                    EntityNPCInterface npc = actor(mc.world, name());
                    if (uuids.has(name())) { check(key("uuid"), uuids.get(name()).getAsString().equals(npc.getUniqueID().toString()), name()); }
                    else { uuids.addProperty(name(), npc.getUniqueID().toString()); }
                    mc.playerController.interactWithEntity(mc.player, npc, EnumHand.MAIN_HAND);
                    enter("DISPLAY");
                    break;
                case "DISPLAY":
                    if (!(mc.currentScreen instanceof GuiNPCInterface2)) { break; }
                    top((GuiNPCInterface2) mc.currentScreen, 6);
                    enter("AI");
                    break;
                case "AI":
                    if (!(mc.currentScreen instanceof GuiNpcAI)) { break; }
                    GuiNpcAI ai = (GuiNpcAI) mc.currentScreen;
                    if (ai.getButton(2) == null || !ai.getButton(2).enabled) { break; }
                    click(ai, ai.getButton(2));
                    enter("MOVEMENT");
                    break;
                case "MOVEMENT":
                    movement(mc);
                    enter("CLOSE_MOVEMENT");
                    break;
                case "CLOSE_MOVEMENT":
                    click(root(mc), root(mc).getSubGui().getButton(66));
                    enter("SAVE");
                    break;
                case "SAVE":
                    check(key("ai-parent"), mc.currentScreen instanceof GuiNpcAI && !root(mc).hasSubGui(), String.valueOf(mc.currentScreen));
                    top((GuiNPCInterface2) mc.currentScreen, 0); // normal close sends MainmenuAISave
                    enter("SAVED"); delay = 60;
                    break;
                case "SAVED":
                    check(key("closed"), mc.currentScreen == null, "closed via menu");
                    final String target = name();
                    submit(mc, new Runnable() { @Override public void run() {
                        observation = observe(actor(mc.getIntegratedServer().getWorld(0), target));
                    }});
                    enter("OBSERVE");
                    break;
                case "OBSERVE":
                    if (!completed()) { break; }
                    serverCheck(key("server"), observation, after(), movAfter(), uuids.get(name()).getAsString());
                    if (++round < rounds()) { enter("POSITION"); }
                    else if (verify) { finish(mc, true, ""); }
                    else {
                        submit(mc, new Runnable() { @Override public void run() {
                            MinecraftServer s = mc.getIntegratedServer();
                            for (int i = 0; i < 3; i++) {
                                command(s, "noppes clone add FLGUI_" + i + " 1 FLGUI_Saved_" + i);
                                command(s, "noppes clone spawn FLGUI_Saved_" + i + " 1 " + (i * 3) + ",4,4 FLGUI_" + i + "_Clone");
                            }
                        }});
                        enter("CLONES"); delay = 60;
                    }
                    break;
                case "CLONES":
                    if (!completed()) { break; }
                    submit(mc, new Runnable() { @Override public void run() {
                        JsonObject clones = new JsonObject();
                        for (int i = 0; i < 3; i++) {
                            clones.add("FLGUI_" + i + "_Clone", observe(actor(mc.getIntegratedServer().getWorld(0), "FLGUI_" + i + "_Clone")));
                        }
                        cloneObservation = clones;
                    }});
                    enter("CLONE_CHECK");
                    break;
                case "CLONE_CHECK":
                    if (!completed()) { break; }
                    for (int i = 0; i < 3; i++) {
                        String clone = "FLGUI_" + i + "_Clone";
                        JsonObject o = cloneObservation.getAsJsonObject(clone);
                        int mode = i == 2 ? 2 : 1;
                        int movement = i == 0 ? 0 : 1;
                        check("clone" + i + "-new-uuid", !o.get("uuid").getAsString().equals(uuids.get("FLGUI_" + i).getAsString()), o.toString());
                        serverCheck("clone" + i, o, mode, movement, o.get("uuid").getAsString());
                        uuids.addProperty(clone, o.get("uuid").getAsString());
                    }
                    finish(mc, true, "");
                    break;
                default: throw new IllegalStateException(phase);
            }
        } catch (Exception | LinkageError problem) { problem.printStackTrace(); finish(mc, false, problem.toString()); }
    }

    private void movement(Minecraft mc) throws Exception {
        GuiNPCInterface root = root(mc);
        if (!(root.getSubGui() instanceof SubGuiNpcMovement)) { throw new IllegalStateException("No movement editor"); }
        SubGuiNpcMovement gui = (SubGuiNpcMovement) root.getSubGui();
        check(key("gui-movement"), gui.getButton(15).getValue() == movBefore(), "movement=" + gui.getButton(15).getValue());
        check(key("gui-present"), (gui.getButton(17) != null) == (movBefore() == 1), "button=" + (gui.getButton(17) != null));
        if (movBefore() == 0 && movAfter() == 1) {
            click(root, gui.getButton(15));
            check(key("gui-shown"), gui.getButton(15).getValue() == 1 && gui.getButton(17) != null, "after switching to flying");
        }
        if (gui.getButton(17) != null) {
            GuiNpcButton b = gui.getButton(17);
            check(key("gui-mode"), b.getValue() == (before() == 2 ? 0 : before()), b.displayString);
            check(key("gui-label"), LABELS[before()].equals(b.displayString), b.displayString);
            check(key("gui-enabled"), b.enabled == (before() != 2), "enabled=" + b.enabled);
            GuiButton type = gui.getButton(0);
            boolean inside = b.x >= gui.guiLeft && b.y >= gui.guiTop && b.x + b.width <= gui.guiLeft + gui.xSize
                    && b.y + b.height <= gui.guiTop + gui.ySize && b.x + b.width <= root.width && b.y + b.height <= root.height;
            boolean apart = type.x + type.width <= b.x && type.y == b.y;
            snapshot(mc);
            check(key("gui-bounds"), inside && apart, "button=" + b.x + "," + b.y + "," + b.width + "," + b.height
                    + " panel=" + gui.guiLeft + "," + gui.guiTop + "," + gui.xSize + "," + gui.ySize + " screen=" + root.width + "," + root.height);
            if (before() != 2) {
                for (int i = 0; i < (after() - before() + 2) % 2; i++) { click(root, gui.getButton(17)); }
            }
            check(key("gui-pending"), gui.getButton(17).displayString.equals(LABELS[after()]), gui.getButton(17).displayString);
        }
        if (movBefore() == 1 && movAfter() == 0) {
            click(root, gui.getButton(15));
            click(root, gui.getButton(15));
            check(key("gui-hidden"), gui.getButton(15).getValue() == 0 && gui.getButton(17) == null, "after switching to ground");
        }
        check(key("pending"), gui.getButton(15).getValue() == movAfter(), "movement=" + gui.getButton(15).getValue());
    }

    private void serverCheck(String key, JsonObject o, int mode, int movement, String uuid) {
        boolean hover = mode == FlightMode.HOVER.id() && movement == 1;
        check(key, o.get("mode").getAsInt() == mode
                && o.get("movement").getAsInt() == movement
                && o.get("nbtKey").getAsBoolean() == (mode != 0)
                && o.get("nbtMode").getAsInt() == mode
                && o.get("hover").getAsBoolean() == hover
                && o.get("uuid").getAsString().equals(uuid), o.toString());
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
        o.addProperty("mode", npc.getRwFlightMode().id()); o.addProperty("movement", npc.ais.movementType);
        o.addProperty("nbtKey", nbt.hasKey(FlightMode.KEY)); o.addProperty("nbtMode", nbt.getInteger(FlightMode.KEY));
        o.addProperty("hover", npc.getMoveHelper() instanceof HoverMoveHelper);
        o.addProperty("helper", npc.getMoveHelper().getClass().getSimpleName());
        o.addProperty("uuid", npc.getUniqueID().toString());
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

    private static GuiNPCInterface root(Minecraft mc) { return (GuiNPCInterface) mc.currentScreen; }

    private static void command(MinecraftServer server, String text) {
        if (server.getCommandManager().executeCommand(server, text) < 1) { throw new IllegalStateException("Command failed: " + text); }
    }

    private static void click(GuiNPCInterface gui, GuiButton button) {
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
            if (mc.world != null) { mc.world.sendQuittingDisconnectingPacket(); mc.loadWorld(null); }
            mc.displayGuiScreen(new GuiMainMenu());
            JsonObject proof = new JsonObject();
            proof.addProperty("success", ok); proof.addProperty("error", error);
            proof.addProperty("pass", verify ? "verify" : "edit"); proof.addProperty("rounds", round);
            proof.add("uuids", uuids); proof.add("assertions", assertions); proof.add("screenshots", screenshots);
            try (Writer writer = Files.newBufferedWriter(out.toPath(), StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(proof, writer);
            }
            System.out.println("FLGUI DONE success=" + ok + " pass=" + (verify ? "verify" : "edit"));
        } catch (Exception problem) { problem.printStackTrace(); }
        mc.shutdown();
    }
}
