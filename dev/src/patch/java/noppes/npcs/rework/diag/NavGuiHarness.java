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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumHand;
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
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.SubGuiNpcMovement;
import noppes.npcs.client.gui.mainmenu.GuiNpcAI;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.client.gui.util.GuiNpcMenu;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.nav.GuiNavBackend;

/** Controller-only GUI acceptance. Uses wand interaction, mouse handlers and real AI packets.
 * Does not call NavGuiData.save, setRwNavBackend, updateTasks or GUI actionPerformed directly.
 * Reflection is read-only and locates the existing translated top-menu button rectangles.
 */
@SideOnly(Side.CLIENT)
public final class NavGuiHarness {
    private final Properties job;
    private final File out;
    private final JsonArray assertions = new JsonArray();
    private final JsonArray screenshots = new JsonArray();
    private final boolean verify;
    private final boolean serverLibrary;
    private final boolean dedicated;
    private String phase = "MENU";
    private long since = System.nanoTime();
    private int delay;
    private int round;
    private String uuid;
    private volatile boolean ready;
    private volatile String setupError;
    private boolean finished;

    private NavGuiHarness(Properties job) {
        this.job = job;
        out = new File(job.getProperty("out"));
        verify = "verify".equals(job.getProperty("pass"));
        serverLibrary = "1".equals(job.getProperty("serverLibrary"));
        dedicated = job.containsKey("connect");
    }

    static boolean installIfRequested(String path) throws Exception {
        Properties p = new Properties();
        try (InputStreamReader in = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)) { p.load(in); }
        if (!"1".equals(p.getProperty("navGui"))) { return false; }
        if (!p.containsKey("out") || !p.containsKey("world")) { throw new IllegalArgumentException("Incomplete navGui job"); }
        MinecraftForge.EVENT_BUS.register(new NavGuiHarness(p));
        System.out.println("NAVGUI-CLIENT INSTALL pass=" + p.getProperty("pass"));
        return true;
    }

    private void enter(String next) {
        phase = next;
        since = System.nanoTime();
        delay = 10; // let each GUI render before reading/clicking it
        System.out.println("NAVGUI-CLIENT PHASE " + phase + " round=" + round);
    }

    private void check(String key, boolean ok, String detail) {
        JsonObject row = new JsonObject();
        row.addProperty("key", key);
        row.addProperty("ok", ok);
        row.addProperty("detail", detail);
        assertions.add(row);
        System.out.println("NAVGUI-CLIENT CHECK " + key + " ok=" + ok + " " + detail);
        if (!ok) { throw new IllegalStateException(key + ": " + detail); }
    }

    @SubscribeEvent
    public void chat(ClientChatReceivedEvent e) {
        if (e.getMessage().getUnformattedText().contains("NAVGUI-CLIENT-SCENE")) { ready = true; }
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || finished) { return; }
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (setupError != null) { throw new IllegalStateException(setupError); }
            if (System.nanoTime() - since > 180000000000L) { throw new IllegalStateException("Timeout " + phase); }
            if (delay-- > 0) { return; }
            switch (phase) {
                case "MENU":
                    if (!(mc.currentScreen instanceof GuiMainMenu)) { break; }
                    check("client-library", Loader.isModLoaded("npcbaritone") == "1".equals(job.getProperty("clientLibrary")),
                            "loaded=" + Loader.isModLoaded("npcbaritone"));
                    check("local-default", CustomNpcs.RwNavBackend == 0, "local=" + CustomNpcs.RwNavBackend);
                    mc.gameSettings.pauseOnLostFocus = false;
                    enter("JOIN");
                    if (dedicated) {
                        mc.displayGuiScreen(new GuiConnecting(new GuiMainMenu(), mc, "127.0.0.1", Integer.parseInt(job.getProperty("connect"))));
                    } else {
                        File level = new File(new File(new File(mc.mcDataDir, "saves"), job.getProperty("world")), "level.dat");
                        check("world-existence", level.isFile() == verify, "exists=" + level.isFile());
                        WorldSettings settings = verify ? null : new WorldSettings(HarnessJob.seedOf("customnpc-rework"),
                                GameType.CREATIVE, false, false, WorldType.FLAT).enableCommands();
                        mc.launchIntegratedServer(job.getProperty("world"), job.getProperty("world"), settings);
                    }
                    break;
                case "JOIN":
                    if (mc.world == null || mc.player == null || mc.currentScreen != null) { break; }
                    System.out.println("NAVGUI-CLIENT PLAYER " + mc.player.getName());
                    if (!dedicated) { setup(mc, false); }
                    enter("PREP");
                    delay = 100; // player chunks load after teleport
                    break;
                case "PREP":
                    if (!dedicated) { ready = false; setup(mc, true); }
                    enter("SCENE");
                    break;
                case "SCENE":
                    if (!ready || actor(mc) == null || mc.player.getHeldItemMainhand().getItem() != CustomItems.wand) { break; }
                    uuid = actor(mc).getUniqueID().toString();
                    check("connection", mc.isIntegratedServerRunning() != dedicated, "dedicated=" + dedicated);
                    enter("OPEN");
                    break;
                case "OPEN":
                    if (mc.currentScreen != null) { throw new IllegalStateException("Unexpected screen before wand"); }
                    mc.playerController.interactWithEntity(mc.player, actor(mc), EnumHand.MAIN_HAND);
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
                    GuiNPCInterface root = root(mc);
                    if (!(root.getSubGui() instanceof SubGuiNpcMovement)) { break; }
                    SubGuiNpcMovement movement = (SubGuiNpcMovement) root.getSubGui();
                    int expectedMovement = !verify && round == 2 ? 1 : 0;
                    check("r" + round + "-movement", movement.getButton(15).getValue() == expectedMovement, "movement=" + movement.getButton(15).getValue());
                    click(root, movement.getButton(16));
                    enter("BACKEND");
                    break;
                case "BACKEND":
                    if (!(root(mc).getSubGui() instanceof GuiNavBackend)) { break; }
                    inspect(mc);
                    snapshot(mc, "r" + round);
                    if (verify || round == 5) {
                        click(root(mc), root(mc).getSubGui().getButton(66));
                        enter("CLOSE_MOVEMENT");
                    } else if (round == 1 || round == 2) {
                        click(root(mc), root(mc).getSubGui().getButton(66));
                        enter("CHANGE_MOVEMENT");
                    } else {
                        GuiNavBackend backend = (GuiNavBackend) root(mc).getSubGui();
                        String actual = backend.getLabel(2).label;
                        click(root(mc), backend.getButton(0));
                        backend = (GuiNavBackend) root(mc).getSubGui();
                        check("r" + round + "-pending", backend.getLabel(4).label.equals("Selection not saved yet."), backend.getLabel(4).label);
                        check("r" + round + "-snapshot", backend.getLabel(2).label.equals(actual), backend.getLabel(2).label);
                        enter("PENDING");
                    }
                    break;
                case "PENDING":
                    snapshot(mc, "pending" + round);
                    click(root(mc), root(mc).getSubGui().getButton(66));
                    enter("CLOSE_MOVEMENT");
                    break;
                case "CHANGE_MOVEMENT":
                    GuiNPCInterface moving = root(mc).getSubGui();
                    click(root(mc), moving.getButton(15));
                    if (round == 2) { click(root(mc), moving.getButton(15)); }
                    enter("CLOSE_MOVEMENT");
                    break;
                case "CLOSE_MOVEMENT":
                    click(root(mc), root(mc).getSubGui().getButton(66));
                    enter("SAVE");
                    break;
                case "SAVE":
                    check("r" + round + "-ai-parent", mc.currentScreen instanceof GuiNpcAI && !root(mc).hasSubGui(), mc.currentScreen.getClass().getName());
                    top((GuiNPCInterface2) mc.currentScreen, 0);
                    enter("SAVED");
                    delay = 40; // real server tick rebuild, then fresh GET on next open
                    break;
                case "SAVED":
                    check("r" + round + "-closed", mc.currentScreen == null, "closed");
                    check("r" + round + "-same-entity", actor(mc) != null && uuid.equals(actor(mc).getUniqueID().toString()), uuid);
                    if (verify || round == 5) { finish(mc, true, ""); }
                    else { round++; enter("OPEN"); }
                    break;
                default: throw new IllegalStateException(phase);
            }
        } catch (Exception | LinkageError problem) {
            problem.printStackTrace();
            finish(mc, false, problem.toString());
        }
    }

    private void inspect(Minecraft mc) {
        GuiNavBackend backend = (GuiNavBackend) root(mc).getSubGui();
        int choice = verify || round == 1 || round == 2 || round == 3 || round == 5 ? 1 : 0;
        boolean flying = !verify && round == 2;
        String reason = choice == 0 ? "Selected vanilla" : !serverLibrary ? "npcbaritone missing" : flying ? "Flying/swimming unsupported" : "Active";
        String actual = choice == 1 && serverLibrary && !flying ? "Baritone" : "Vanilla";
        check("r" + round + "-choice", backend.getButton(0).enabled && backend.getButton(0).getValue() == choice, "choice=" + backend.getButton(0).getValue());
        check("r" + round + "-actual", backend.getLabel(2).label.equals("Actual at open: " + actual), backend.getLabel(2).label);
        check("r" + round + "-reason", backend.getLabel(3).label.equals("Reason: " + reason), backend.getLabel(3).label);
        check("r" + round + "-saved", backend.getLabel(4).label.equals("State captured on opening AI."), backend.getLabel(4).label);
    }

    private EntityNPCInterface actor(Minecraft mc) {
        EntityNPCInterface found = null;
        for (Entity e : mc.world.loadedEntityList) {
            if (!(e instanceof EntityNPCInterface) || !"NAVGUI_Click".equals(e.getName())) { continue; }
            if (found != null) { throw new IllegalStateException("Duplicate actor"); }
            found = (EntityNPCInterface) e;
        }
        return found;
    }

    private static GuiNPCInterface root(Minecraft mc) { return (GuiNPCInterface) mc.currentScreen; }

    private static void click(GuiNPCInterface root, GuiButton button) {
        if (button == null || !button.enabled || !button.visible) { throw new IllegalStateException("Unavailable button"); }
        int x = button.x + 2;
        int y = button.y + 2;
        // CNPC top buttons use hover from the last draw, rather than the click arguments.
        // Draw with the synthetic pointer before routing the normal mouse event.
        root.drawScreen(x, y, 0.0F);
        root.mouseClicked(x, y, 0);
        root.mouseReleased(x, y, 0);
    }

    private static void top(GuiNPCInterface2 gui, int id) throws Exception {
        Field menu = GuiNPCInterface2.class.getDeclaredField("menu");
        menu.setAccessible(true);
        Field buttons = GuiNpcMenu.class.getDeclaredField("topButtons");
        buttons.setAccessible(true);
        for (GuiButton b : (GuiButton[]) buttons.get(menu.get(gui))) {
            if (b.id == id) { click(gui, b); return; }
        }
        throw new IllegalStateException("Missing top button " + id);
    }

    private void setup(final Minecraft mc, final boolean spawn) {
        final MinecraftServer server = mc.getIntegratedServer();
        final String name = mc.player.getName();
        server.addScheduledTask(new Runnable() {
            @Override public void run() {
                try {
                    if (!spawn) {
                        command(server, "setworldspawn 0 4 0");
                        command(server, "gamemode 1 " + name);
                        command(server, "tp " + name + " 0 4 2 180 0");
                        command(server, "replaceitem entity " + name + " slot.hotbar.0 customnpcs:npcwand");
                        command(server, "gamerule doMobSpawning false");
                        command(server, "time set 1000");
                    } else {
                        if (!verify) { command(server, "noppes clone spawn NAVGUI_Click 1 0,4,0"); }
                        ready = true;
                    }
                } catch (Exception problem) { setupError = problem.toString(); }
            }
        });
    }

    private static void command(MinecraftServer server, String text) {
        int count = server.getCommandManager().executeCommand(server, text);
        if (count < 1) { throw new IllegalStateException("Command failed: " + text); }
    }

    private void snapshot(Minecraft mc, String label) {
        String name = out.getName().replace(".json", "") + "-" + label + ".png";
        ScreenShotHelper.saveScreenshot(out.getParentFile(), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        File file = new File(new File(out.getParentFile(), "screenshots"), name);
        if (!file.isFile()) { throw new IllegalStateException("Missing screenshot " + file); }
        screenshots.add(file.getName());
    }

    private void finish(Minecraft mc, boolean ok, String error) {
        finished = true;
        try {
            if (mc.world != null) { mc.world.sendQuittingDisconnectingPacket(); mc.loadWorld(null); }
            // Match vanilla's disconnect flow: shutdown only stops the next game loop.
            // Its current frame still renders, so release in-game focus before player is null.
            mc.displayGuiScreen(new GuiMainMenu());
            JsonObject proof = new JsonObject();
            proof.addProperty("success", ok);
            proof.addProperty("error", error);
            proof.addProperty("pass", verify ? "verify" : "edit");
            proof.addProperty("dedicated", dedicated);
            proof.addProperty("uuid", uuid);
            proof.add("assertions", assertions);
            proof.add("screenshots", screenshots);
            try (Writer writer = Files.newBufferedWriter(out.toPath(), StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(proof, writer);
            }
            System.out.println("NAVGUI-CLIENT DONE success=" + ok + " pass=" + (verify ? "verify" : "edit"));
        } catch (Exception problem) { problem.printStackTrace(); }
        mc.shutdown();
    }
}
