package noppes.npcs.rework.diag;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import noppes.npcs.client.gui.GuiNpcMobSpawner;
import noppes.npcs.client.Client;
import noppes.npcs.constants.EnumPacketServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.client.gui.advanced.GuiNPCAdvancedLinkedNpc;
import noppes.npcs.client.gui.global.GuiNPCManageLinkedNpc;
import noppes.npcs.client.gui.mainmenu.GuiNPCGlobalMainMenu;
import noppes.npcs.client.gui.mainmenu.GuiNpcAdvanced;
import noppes.npcs.client.gui.mainmenu.GuiNpcDisplay;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.client.gui.util.GuiNpcMenu;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.linked.LinkedCategories;
import noppes.npcs.rework.linked.LinkedClient;
import noppes.npcs.rework.linked.LinkedGroupPanel;
import noppes.npcs.rework.linked.LinkedGuiCodec;

/** M13.4 controller-only real client: cloner, selective edits, unload/reload and rights revocation. */
@SideOnly(Side.CLIENT)
public final class LinkedClonerHarness {
    private interface Act { void run() throws Exception; }
    private interface Cond { boolean ok() throws Exception; }
    private static final class Step {
        final String name; final int wait; final Act act; final Cond until;
        Step(String n, int w, Act a, Cond c) { name = n; wait = w; act = a; until = c; }
    }
    private static final String T1 = "customnpcs:textures/entity/humanfemale/magestephanie.png";
    private static final String T2 = "customnpcs:textures/entity/humanfemale/doctorstephanie.png";
    private static final String STEVE = "customnpcs:textures/entity/humanmale/steve.png";
    private static final int NAME_OFF = LinkedCategories.DEFAULT & ~LinkedCategories.NAME;
    private static final int FINAL = NAME_OFF | LinkedCategories.SCRIPTS;

    private final Properties job;
    private final File out;
    private final JsonArray checks = new JsonArray(), images = new JsonArray();
    private final Deque<Step> steps = new ArrayDeque<Step>();
    private Step step;
    private int delay, before, poll, joins;
    private boolean acted, finished;
    private long since;
    private EntityNPCInterface a, b, c;

    private LinkedClonerHarness(Properties p) { job = p; out = new File(p.getProperty("out")); }

    static boolean installIfRequested(String path) throws Exception {
        Properties p = new Properties();
        try (Reader in = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)) { p.load(in); }
        if (!"1".equals(p.getProperty("linkedCloner"))) { return false; }
        if (!p.containsKey("out") || !p.containsKey("connect") || !p.containsKey("phase")) { throw new IllegalArgumentException("linkedCloner job"); }
        LinkedClonerHarness h = new LinkedClonerHarness(p);
        h.plan();
        MinecraftForge.EVENT_BUS.register(h);
        return true;
    }

    private void connect() {
        Minecraft mc = Minecraft.getMinecraft();
        joins++;
        mc.displayGuiScreen(new GuiConnecting(new GuiMainMenu(), mc, "127.0.0.1", Integer.parseInt(job.getProperty("connect"))));
    }
    private void add(String n, int w, Act x, Cond u) { steps.add(new Step(n, w, x, u)); }
    private void act(String n, Act x) { add(n, 10, x, () -> true); }
    private void cmd(String text) { act(text, () -> Minecraft.getMinecraft().player.sendChatMessage("/" + text)); }
    private void until(String n, Cond u) { add(n, 5, () -> { }, u); }
    private void sync(String marker) { act("sync " + marker, () -> System.out.println("LINKEDCLONER SYNC " + marker)); }
    private GuiNPCInterface gui() { return (GuiNPCInterface) Minecraft.getMinecraft().currentScreen; }
    private GuiCustomScroll list() { return gui().getScroll(0); }

    private EntityNPCInterface npc(String name) {
        for (Entity e : Minecraft.getMinecraft().world.loadedEntityList) {
            if (e instanceof EntityNPCInterface && e.getName().equals(name)) { return (EntityNPCInterface) e; }
        }
        return null;
    }
    private int named(String name, String texture) {
        int n = 0;
        for (Entity e : Minecraft.getMinecraft().world.loadedEntityList) {
            if (e instanceof EntityNPCInterface && e.getName().equals(name) && texture.equals(((EntityNPCInterface) e).display.getSkinTexture())) { n++; }
        }
        return n;
    }
    private static boolean shows(EntityNPCInterface n, String name, String texture) {
        return name.equals(n.display.getName()) && texture.equals(n.display.getSkinTexture());
    }
    private static String state(EntityNPCInterface n) { return n.display.getName() + "/" + n.display.getSkinTexture(); }

    /** Draws the screen with the mouse there, as a frame does, then one press and release. */
    private void press(int x, int y) throws Exception {
        GuiNPCInterface g = gui();
        SubGuiInterface sub = g.hasSubGui() ? g.getSubGui() : null;
        if (sub != null) { sub.drawScreen(x, y, 0); } else { g.drawScreen(x, y, 0); }
        g.mouseClicked(x, y, 0);
        g.mouseReleased(x, y, 0);
    }
    private void click(int id) throws Exception {
        GuiNPCInterface g = gui();
        GuiButton b = (g.hasSubGui() ? g.getSubGui() : g).getButton(id);
        if (b == null || !b.enabled || !b.visible) { throw new IllegalStateException("Unavailable button " + id); }
        press(b.x + 3, b.y + 3);
    }
    private void row(String name) throws Exception {
        GuiCustomScroll s = list();
        int index = s.getList().indexOf(name);
        if (index < 0) { throw new IllegalStateException("No row " + name); }
        press(s.guiLeft + 10, s.guiTop + 14 * index + 4 + 3);
    }
    private void type(GuiNpcTextField field, String value) throws Exception {
        press(field.x + 3, field.y + 3);
        GuiNPCInterface g = gui();
        g.keyTyped((char) 0, 207); // End, then backspace: normal text field input.
        for (int i = 0; i < 80; i++) { g.keyTyped((char) 0, 14); }
        for (char ch : value.toCharArray()) { g.keyTyped(ch, 0); }
    }
    private void top(int id) throws Exception {
        GuiNPCInterface2 g = (GuiNPCInterface2) Minecraft.getMinecraft().currentScreen;
        Field m = GuiNPCInterface2.class.getDeclaredField("menu"); m.setAccessible(true);
        Field t = GuiNpcMenu.class.getDeclaredField("topButtons"); t.setAccessible(true);
        for (GuiButton button : (GuiButton[]) t.get(m.get(g))) {
            if (button.id == id) {
                int x = button.x + 2, y = button.y + 2;
                g.drawScreen(x, y, 0); g.mouseClicked(x, y, 0); g.mouseReleased(x, y, 0);
                return;
            }
        }
        throw new IllegalStateException("Menu " + id);
    }
    private void wand(String label, Supplier<EntityNPCInterface> who) {
        add("wand " + label, 20, () -> {
            Minecraft mc = Minecraft.getMinecraft();
            mc.playerController.interactWithEntity(mc.player, who.get(), EnumHand.MAIN_HAND);
        }, () -> Minecraft.getMinecraft().currentScreen instanceof GuiNpcDisplay);
    }

    private void openManager(Supplier<EntityNPCInterface> who) {
        wand("manager", who);
        add("display settle", 30, () -> { }, () -> true);
        act("global", () -> top(5));
        until("wait global", () -> Minecraft.getMinecraft().currentScreen instanceof GuiNPCGlobalMainMenu);
        add("linked", 15, () -> click(16), () -> Minecraft.getMinecraft().currentScreen instanceof GuiNPCManageLinkedNpc);
    }
    private void closeMenu() { act("close menu", () -> top(0)); add("closed", 5, () -> { }, () -> Minecraft.getMinecraft().currentScreen == null); }

    private void reply(String name, Act action) {
        add(name, 5, () -> { before = LinkedClient.replies(); action.run(); }, () -> LinkedClient.replies() > before);
    }
    private int boxes() {
        int mask = 0;
        for (int i = 0; i < 8; i++) {
            if (gui().getButton(LinkedGroupPanel.FIRST_BOX + i).displayString.startsWith("[x] ")) { mask |= 1 << i; }
        }
        return mask;
    }
    private boolean enabled(int id) { return gui().getButton(id).enabled; }
    private void addGroup(String name) {
        act("add " + name, () -> click(1));
        add("edit " + name, 5, () -> { }, () -> gui().hasSubGui());
        act("type " + name, () -> type(gui().getSubGui().getTextField(0), name));
        act("done " + name, () -> click(0));
        until("listed " + name, () -> !gui().hasSubGui() && list().getList().contains(name));
    }
    private void link(String label, Supplier<EntityNPCInterface> who) {
        wand("link " + label, who);
        add("display settle " + label, 30, () -> { }, () -> true);
        act("advanced " + label, () -> top(4));
        until("wait advanced " + label, () -> Minecraft.getMinecraft().currentScreen instanceof GuiNpcAdvanced);
        add("advanced settle " + label, 20, () -> { }, () -> true);
        act("linked list " + label, () -> click(13));
        until("wait list " + label, () -> Minecraft.getMinecraft().currentScreen instanceof GuiNPCAdvancedLinkedNpc && list().getList().contains("LG1"));
        add("click group " + label, 5, () -> row("LG1"), () -> "LG1".equals(list().getSelected()));
        add("link settle " + label, 20, () -> { }, () -> true);
        closeMenu();
    }
    /** Edits the name and skin of an NPC through the real display tab; closing the menu saves it to the group. */
    private void edit(Supplier<EntityNPCInterface> who, String name, String texture) {
        wand("edit " + name, who);
        add("display settle " + name, 30, () -> { }, () -> gui().getTextField(0) != null);
        act("name " + name, () -> type(gui().getTextField(0), name));
        act("skin " + name, () -> type(gui().getTextField(3), texture));
        act("leave fields " + name, () -> press(gui().getTextField(11).x + 3, gui().getTextField(11).y + 3));
        closeMenu();
    }
    private void screen(String name) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        String file = out.getName().replace(".json", "") + "-" + name + ".png";
        ScreenShotHelper.saveScreenshot(out.getParentFile(), file, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        images.add(file);
    }
    private void image(String name) { act("image " + name, () -> screen(name)); }


    private final java.util.List<String> chat = new java.util.ArrayList<String>();
    @SubscribeEvent public void chat(ClientChatReceivedEvent event) { chat.add(event.getMessage().getUnformattedText()); }
    private boolean said(String text) { for (String line : chat) if (line.contains(text)) return true; return false; }
    private boolean gate(String text) throws Exception {
        File f = new File(job.getProperty("gate"));
        return f.isFile() && Files.readAllLines(f.toPath(), StandardCharsets.UTF_8).contains(text);
    }
    private EntityNPCInterface at(int x) {
        for (Entity e : Minecraft.getMinecraft().world.loadedEntityList) {
            if (e instanceof EntityNPCInterface && Math.abs(e.posX - (x + 0.5)) < 1 && e.posZ > 2 && e.posZ < 5)
                return (EntityNPCInterface)e;
        }
        return null;
    }
    private void teleport(int x) {
        cmd("tp @p " + x + " 4 0 0 -90");
        add("load " + x, 60, () -> {}, () -> Minecraft.getMinecraft().world.isBlockLoaded(new BlockPos(x, 4, 3)));
    }
    private void tool(String name) {
        cmd("replaceitem entity @p slot.hotbar.0 customnpcs:" + name);
        act("equipped " + name, () -> {
            net.minecraft.item.Item item = Minecraft.getMinecraft().player.getHeldItemMainhand().getItem();
            if (!("customnpcs:" + name).equals(String.valueOf(item.getRegistryName()))) {
                throw new IllegalStateException("Expected tool customnpcs:" + name + ", held " + item.getRegistryName());
            }
        });
    }
    private void openCloner(int x, int tab) {
        add("cloner " + x, 10, () -> {
            Minecraft mc = Minecraft.getMinecraft();
            BlockPos p = new BlockPos(x, 3, 3);
            mc.playerController.processRightClickBlock(mc.player, mc.world, p, EnumFacing.UP,
                    new Vec3d(x + 0.5, 4, 3.5), EnumHand.MAIN_HAND);
        }, () -> Minecraft.getMinecraft().currentScreen instanceof GuiNpcMobSpawner);
        add("cloner tab " + tab, 15, () -> {
            GuiButton button = gui().getTopButton(tab);
            press(button.x + 3, button.y + 3);
        }, () -> gui().getScroll(0).getList().contains("LCGuard"));
    }
    private void place(int x, int tab, boolean linked, String clone) {
        openCloner(x, tab);
        if (linked) {
            until("capability", () -> gui().getButton(7) != null);
            act("toggle linked", () -> click(7));
        }
        act("select " + clone, () -> row(clone));
        if (linked) {
            act("linked placement controls " + x, () -> check("linked-controls-" + x,
                    gui().getButton(7).displayString.equals("Linked: Yes") && !gui().getButton(2).enabled,
                    "explicit toggle; mob spawner disabled"));
            image("cloner-" + x);
        }
        act("place " + x, () -> click(1));
        until("placed " + x, () -> Minecraft.getMinecraft().currentScreen == null && at(x) != null);
    }
    private void plan() {
        Minecraft mc = Minecraft.getMinecraft();
        String phase = job.getProperty("phase");
        add("menu", 0, () -> {}, () -> mc.currentScreen instanceof GuiMainMenu);
        add("join", 10, () -> {
            mc.gameSettings.pauseOnLostFocus = false;
            connect();
        }, () -> {
            // Q426: a stalled FML login handshake times out to GuiDisconnected; reconnect at most twice.
            if (joins < 3 && mc.world == null && mc.currentScreen instanceof GuiDisconnected) {
                System.out.println("LINKEDCLONER JOIN retry " + joins + " after handshake timeout");
                connect(); since = System.nanoTime();
            }
            return mc.world != null && mc.player != null && mc.currentScreen == null;
        });
        cmd("gamemode 1"); cmd("gamerule doMobSpawning false");
        teleport(600);
        if ("main".equals(phase)) main();
        else if ("verify".equals(phase)) verify();
        else original(phase);
        act("finish", () -> finish(true, ""));
    }
    private void main() {
        tool("npcmobcloner");
        place(600, 5, true, "LCGuard"); // server clone; initializes the group
        until("new group feedback", () -> said("LCGuard (new group)"));
        act("new group", () -> check("new-group-feedback", at(600) != null && said("LCGuard (new group)"), "chat from server"));
        sync("first"); until("first file recorded", () -> gate("first"));
        place(604, 3, true, "LCGuard"); // local clone; group must win over its different skin
        until("local group data", () -> STEVE.equals(at(604).display.getSkinTexture()));
        act("local clone", () -> check("local-clone-existing-group", said("LCGuard (existing group kept)") && STEVE.equals(at(604).display.getSkinTexture()),
                state(at(604))));
        sync("local"); until("local file checked", () -> gate("local"));
        teleport(604);
        place(608, 5, false, "LCPlain");
        act("plain clone", () -> check("plain-clone", at(608).display.getName().equals("LCPlain"), state(at(608))));
        cmd("rwlinked categories LCGuard appearance");
        sync("far-before"); until("far file recorded", () -> gate("far-before"));
        teleport(1000);
        place(1002, 5, true, "LCGuard");
        until("far member", () -> at(1002) != null && STEVE.equals(at(1002).display.getSkinTexture()));
        act("far", () -> check("far-clone", STEVE.equals(at(1002).display.getSkinTexture()), state(at(1002))));
        sync("far"); until("far file checked", () -> gate("far"));
        teleport(600);
        sync("unload");
        until("server confirmed unload", () -> gate("unloaded1"));
        tool("npcwand");
        edit(() -> at(600), "LC_Edited", T2);
        until("near update", () -> at(604) != null && T2.equals(at(604).display.getSkinTexture()));
        until("notification", () -> said("updated 1 loaded NPCs; pending 0"));
        act("selective edit", () -> {
            check("loaded-categories", shows(at(600), "LC_Edited", T2) && shows(at(604), "LCGuard", T2)
                    && shows(at(608), "LCPlain", STEVE), state(at(604)));
            check("update-notification", said("updated 1 loaded NPCs; pending 0") && said("total unknown"), String.join(" | ", chat));
        });
        image("update-chat");
        sync("edited-unloaded");
        until("server checked unloaded edit", () -> gate("edited1"));
        teleport(1000);
        until("far receives latest", () -> at(1002) != null && shows(at(1002), "LCGuard", T2));
        act("load update", () -> check("unloaded-member-updated", shows(at(1002), "LCGuard", T2), state(at(1002))));
        image("reloaded-member");
        sync("loaded1");
        until("server own fields", () -> gate("loaded1"));
        teleport(600);
        sync("unload2");
        until("server confirmed second unload", () -> gate("unloaded2"));
        edit(() -> at(600), "LC_Final", T1);
        until("second near update", () -> T1.equals(at(604).display.getSkinTexture()));
        act("second edit", () -> check("second-edit", shows(at(604), "LCGuard", T1), state(at(604))));
        sync("restart-ready");
        until("server checked second unloaded edit", () -> gate("restart-ready"));
        // Leave the cloner open while the console revokes operator status, then press its real Spawn button.
        teleport(610);
        tool("npcmobcloner");
        openCloner(612, 5);
        until("rights before revoke", () -> gui().getButton(7) != null);
        act("stale toggle", () -> { click(7); row("LCGuard"); });
        sync("deop");
        until("console removed rights", () -> gate("deopped"));
        act("stale spawn", () -> click(1));
        add("denied window", 60, () -> {}, () -> true);
        act("stale denied", () -> check("stale-screen-denied", at(612) == null && Minecraft.getMinecraft().getConnection() != null, "no spawned entity"));
        act("forged after deop", () -> Client.sendData(EnumPacketServer.SpawnMob, true, 612, 3, 3, "LCGuard", 1, true, "LCForbidden"));
        add("forged window", 40, () -> {}, () -> true);
        act("forged denied", () -> check("forged-spawn-denied", at(612) == null, "no spawned entity after forged packet"));
        image("denied");
        sync("denied");
    }
    private void verify() {
        until("near saved", () -> at(600) != null && at(604) != null && at(608) != null);
        act("near restart", () -> check("restart-near", shows(at(600), "LC_Final", T1) && shows(at(604), "LCGuard", T1)
                && shows(at(608), "LCPlain", STEVE), state(at(604))));
        teleport(1000);
        until("far restart", () -> at(1002) != null && shows(at(1002), "LCGuard", T1));
        act("far restart check", () -> check("restart-unloaded-member", shows(at(1002), "LCGuard", T1), state(at(1002))));
        image("restart");
        sync("restart-loaded");
        until("server checked restart", () -> gate("restart-done"));
    }
    private void original(String phase) {
        int target = "off".equals(phase) ? 616 : 620;
        teleport(target - 2);
        tool("npcmobcloner");
        openCloner(target, 5);
        add("no reply window", 60, () -> {}, () -> true);
        act("original screen", () -> {
            check(phase + "-original-gui", gui().getButton(7) == null && gui().getLabel(7) == null && gui().getButton(1) != null
                    && gui().getButton(2).enabled, "original buttons without linked toggle");
            check(phase + "-no-reply", LinkedClient.replies() == 0, "no server channel response");
        });
        image(phase);
        act("select plain", () -> row("LCPlain"));
        act("plain original", () -> click(1));
        until("plain spawned", () -> at(target) != null);
        act("original spawn", () -> check(phase + "-original-spawn", shows(at(target), "LCPlain", STEVE), state(at(target))));
    }

    private void check(String key, boolean ok, String detail) {
        JsonObject r = new JsonObject();
        r.addProperty("key", key); r.addProperty("ok", ok); r.addProperty("detail", detail);
        checks.add(r);
        System.out.println("LINKEDCLONER CHECK " + key + " ok=" + ok + " " + detail);
        if (!ok) { throw new IllegalStateException(key + ": " + detail); }
    }

    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || finished) { return; }
        try {
            if (step == null) {
                step = steps.poll();
                if (step == null) { return; }
                delay = step.wait; acted = false; since = System.nanoTime();
                System.out.println("LINKEDCLONER STEP " + step.name);
            }
            if (System.nanoTime() - since > 120000000000L) { throw new IllegalStateException("Timeout " + step.name + " screen="
                    + (Minecraft.getMinecraft().currentScreen == null ? "none" : Minecraft.getMinecraft().currentScreen.getClass().getSimpleName())); }
            if (delay-- > 0) { return; }
            if (!acted) { acted = true; step.act.run(); if (finished) { return; } }
            if (step.until.ok()) { step = null; }
        } catch (Exception | LinkageError ex) {
            ex.printStackTrace();
            finish(false, ex.toString());
        }
    }

    private void finish(boolean ok, String error) {
        finished = true;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (mc.world != null) { mc.world.sendQuittingDisconnectingPacket(); mc.loadWorld(null); }
            mc.displayGuiScreen(new GuiMainMenu());
            JsonObject p = new JsonObject();
            p.addProperty("success", ok); p.addProperty("error", error); p.addProperty("phase", job.getProperty("phase"));
            p.add("checks", checks); p.add("screenshots", images);
            try (Writer w = Files.newBufferedWriter(out.toPath(), StandardCharsets.UTF_8)) { new GsonBuilder().setPrettyPrinting().create().toJson(p, w); }
            System.out.println("LINKEDCLONER DONE success=" + ok);
        } catch (Exception ex) { ex.printStackTrace(); }
        mc.shutdown();
    }
}


