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
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumHand;
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

/**
 * M13.2b controller-only: the Linked NPCs manager through its real mouse and key handlers over a real connection. Groups
 * are made, NPCs linked and edited through the original GUIs; member state is read from the client's entities. Forged
 * packets are used only for the checks after the rights were taken away. Scene commands are setup, not proof.
 */
@SideOnly(Side.CLIENT)
public final class LinkedGuiHarness {
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
    private int delay, before, poll;
    private boolean acted, finished;
    private long since;
    private EntityNPCInterface a, b, c;

    private LinkedGuiHarness(Properties p) { job = p; out = new File(p.getProperty("out")); }

    static boolean installIfRequested(String path) throws Exception {
        Properties p = new Properties();
        try (Reader in = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)) { p.load(in); }
        if (!"1".equals(p.getProperty("linkedGui"))) { return false; }
        if (!p.containsKey("out") || !p.containsKey("connect") || !p.containsKey("phase")) { throw new IllegalArgumentException("linkedGui job"); }
        LinkedGuiHarness h = new LinkedGuiHarness(p);
        h.plan();
        MinecraftForge.EVENT_BUS.register(h);
        return true;
    }

    private void add(String n, int w, Act x, Cond u) { steps.add(new Step(n, w, x, u)); }
    private void act(String n, Act x) { add(n, 10, x, () -> true); }
    private void cmd(String text) { act(text, () -> Minecraft.getMinecraft().player.sendChatMessage("/" + text)); }
    private void until(String n, Cond u) { add(n, 5, () -> { }, u); }
    private void sync(String marker) { act("sync " + marker, () -> System.out.println("LINKEDGUI SYNC " + marker)); }
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

    private void plan() {
        Minecraft mc = Minecraft.getMinecraft();
        String phase = job.getProperty("phase");
        add("menu", 0, () -> { }, () -> mc.currentScreen instanceof GuiMainMenu);
        add("join", 10, () -> {
            mc.gameSettings.pauseOnLostFocus = false;
            mc.displayGuiScreen(new GuiConnecting(new GuiMainMenu(), mc, "127.0.0.1", Integer.parseInt(job.getProperty("connect"))));
        }, () -> mc.world != null && mc.player != null && mc.currentScreen == null);
        cmd("gamemode 1"); cmd("gamerule doMobSpawning false"); cmd("tp @p 600 4 0 0 0");
        add("load scene", 80, () -> { }, () -> mc.world.isBlockLoaded(new BlockPos(602, 4, 3)));
        cmd("replaceitem entity @p slot.hotbar.0 customnpcs:npcwand");
        if ("main".equals(phase)) { planMain(); }
        else if ("verify".equals(phase)) { planVerify(); }
        else { planOriginal(phase); }
        act("finish", () -> finish(true, ""));
    }

    private void planMain() {
        cmd("noppes clone spawn LGUI_A 1 598,4,3");
        cmd("noppes clone spawn LGUI_B 1 600,4,3");
        cmd("noppes clone spawn LGUI_C 1 602,4,3");
        until("spawned", () -> {
            a = npc("LGUI_A"); b = npc("LGUI_B"); c = npc("LGUI_C");
            return a != null && b != null && c != null;
        });
        // The panel before any group exists: the server answered the probe, nothing is selected yet.
        openManager(() -> c);
        until("panel appears", () -> gui().getButton(LinkedGroupPanel.APPLY) != null);
        act("panel probe", () -> {
            boolean off = !enabled(LinkedGroupPanel.APPLY) && !enabled(LinkedGroupPanel.UNDO) && !enabled(LinkedGroupPanel.FIRST_BOX);
            check("panel-probe", off && "Select a group".equals(gui().getLabel(101).label), gui().getLabel(101).label);
        });
        addGroup("LG1");
        addGroup("LG2");
        reply("select LG2", () -> row("LG2"));
        act("defaults", () -> check("group-defaults", boxes() == LinkedCategories.DEFAULT && !enabled(LinkedGroupPanel.APPLY) && !enabled(LinkedGroupPanel.UNDO) && enabled(LinkedGroupPanel.FIRST_BOX),
                LinkedCategories.toText(boxes())));
        act("scripts off", () -> check("scripts-off-by-default", !LinkedCategories.has(boxes(), LinkedCategories.SCRIPTS), LinkedCategories.toText(boxes())));
        image("defaults");
        closeMenu();

        link("A", () -> a);
        link("B", () -> b);
        until("B takes A's data", () -> shows(b, "LGUI_A", STEVE));
        act("link check", () -> check("original-link-transfers", shows(b, "LGUI_A", STEVE) && "LGUI_C".equals(c.display.getName()), state(b)));

        edit(() -> a, "LG_Alfa", T1);
        until("first edit reaches B", () -> shows(b, "LG_Alfa", T1));
        act("first edit check", () -> check("edit-reaches-member", shows(a, "LG_Alfa", T1) && "LGUI_C".equals(c.display.getName()), state(b)));

        // The box alone changes nothing on the server: only Apply does (Q398).
        openManager(() -> c);
        until("panel", () -> gui().getButton(LinkedGroupPanel.APPLY) != null);
        reply("select LG1", () -> row("LG1"));
        act("loaded", () -> {
            check("undo-enabled-after-edit", enabled(LinkedGroupPanel.UNDO), "kept version");
            check("loaded-count", gui().getLabel(101).label.endsWith(": 2"), gui().getLabel(101).label);
        });
        act("name box", () -> click(LinkedGroupPanel.FIRST_BOX + 1));
        add("box only", 30, () -> { }, () -> true);
        act("box check", () -> check("box-needs-apply", boxes() == NAME_OFF && enabled(LinkedGroupPanel.APPLY), LinkedCategories.toText(boxes())));
        reply("apply name off", () -> click(LinkedGroupPanel.APPLY));
        act("apply check", () -> {
            LinkedGuiCodec.Response r = LinkedClient.latest();
            check("apply-saved", r.result == LinkedGuiCodec.RESULT_OK && r.mask == NAME_OFF && boxes() == NAME_OFF && !enabled(LinkedGroupPanel.APPLY), LinkedCategories.toText(r.mask));
        });
        image("name-off");
        reply("other group", () -> row("LG2"));
        act("other group check", () -> check("other-group-own-categories", boxes() == LinkedCategories.DEFAULT, LinkedCategories.toText(boxes())));
        closeMenu();

        edit(() -> a, "LG_Beta", T2);
        until("skin reaches B", () -> T2.equals(b.display.getSkinTexture()));
        act("selective check", () -> check("name-off-blocks-name", shows(b, "LG_Alfa", T2) && shows(a, "LG_Beta", T2), state(b)));

        openManager(() -> c);
        until("panel 2", () -> gui().getButton(LinkedGroupPanel.APPLY) != null);
        reply("select LG1 again", () -> row("LG1"));
        act("kept boxes", () -> check("server-keeps-selection", boxes() == NAME_OFF, LinkedCategories.toText(boxes())));
        reply("undo", () -> click(LinkedGroupPanel.UNDO));
        act("undo reply", () -> check("undo-button", LinkedClient.latest().result == LinkedGuiCodec.RESULT_OK && enabled(LinkedGroupPanel.UNDO), LinkedClient.latest().text));
        until("undo reaches B", () -> shows(b, "LG_Alfa", T1));
        act("undo check", () -> check("undo-reaches-members", shows(b, "LG_Alfa", T1) && shows(a, "LG_Beta", T1), state(a)));
        image("undone");
        act("name box on", () -> click(LinkedGroupPanel.FIRST_BOX + 1));
        reply("apply name on", () -> click(LinkedGroupPanel.APPLY));
        until("name reaches A", () -> shows(a, "LG_Alfa", T1));
        act("name on check", () -> check("name-on-restores-name", shows(a, "LG_Alfa", T1) && boxes() == LinkedCategories.DEFAULT, state(a)));
        reply("second undo", () -> click(LinkedGroupPanel.UNDO));
        until("second undo reaches B", () -> shows(b, "LG_Beta", T2));
        act("second undo check", () -> check("second-undo-brings-back", shows(a, "LG_Beta", T2) && shows(b, "LG_Beta", T2), state(b)));
        act("final name", () -> click(LinkedGroupPanel.FIRST_BOX + 1));
        act("final scripts", () -> click(LinkedGroupPanel.FIRST_BOX + 6));
        reply("final apply", () -> click(LinkedGroupPanel.APPLY));
        act("final check", () -> check("final-selection", LinkedClient.latest().mask == FINAL && boxes() == FINAL, LinkedCategories.toText(LinkedClient.latest().mask)));

        // Rights are taken away while the screen is open.
        sync("deop");
        add("denied", 5, () -> poll = 0, () -> {
            LinkedGuiCodec.Response r = LinkedClient.latest();
            if (r.result == LinkedGuiCodec.RESULT_DENIED) { return true; }
            if (--poll <= 0) { LinkedClient.send(LinkedGuiCodec.Op.GET, 0, "LG1"); poll = 25; }
            return false;
        });
        reply("forged set", () -> LinkedClient.send(LinkedGuiCodec.Op.SET, LinkedCategories.ALL, "LG1"));
        act("forged set check", () -> check("forged-set-denied", LinkedClient.latest().result == LinkedGuiCodec.RESULT_DENIED, LinkedClient.latest().text));
        reply("forged undo", () -> LinkedClient.send(LinkedGuiCodec.Op.UNDO, 0, "LG1"));
        act("forged undo check", () -> check("forged-undo-denied", LinkedClient.latest().result == LinkedGuiCodec.RESULT_DENIED, LinkedClient.latest().text));
        // The panel reacts to a refused action of its own: selecting again asks the server and disables the controls.
        reply("reselect", () -> row("LG2"));
        act("disabled check", () -> check("controls-disabled-without-rights", !enabled(LinkedGroupPanel.APPLY) && !enabled(LinkedGroupPanel.UNDO) && !enabled(LinkedGroupPanel.FIRST_BOX)
                && "No rights to change groups".equals(gui().getLabel(102).label), gui().getLabel(102).label));
        image("denied");
        act("command denied", () -> Minecraft.getMinecraft().player.sendChatMessage("/rwlinked undo LG1"));
        act("malformed", () -> LinkedClient.send(new byte[]{1, 1, 0}));
        add("denied settle", 40, () -> { }, () -> true);
        sync("denied");
        act("end main", () -> {
            Minecraft mc = Minecraft.getMinecraft();
            check("connection-alive", mc.player != null && mc.getConnection() != null, "rights change and malformed packet kept the connection");
        });
        closeMenu();
    }

    private void planVerify() {
        until("scene", () -> { c = npc("LGUI_C"); return c != null && named("LG_Beta", T2) == 2; });
        act("members", () -> check("restart-members", named("LG_Beta", T2) == 2, "two members with the second-undo state"));
        openManager(() -> c);
        until("panel", () -> gui().getButton(LinkedGroupPanel.APPLY) != null);
        reply("select LG1", () -> row("LG1"));
        act("persisted", () -> {
            check("restart-selection", boxes() == FINAL, LinkedCategories.toText(boxes()));
            check("restart-undo", enabled(LinkedGroupPanel.UNDO), "kept version survived");
        });
        image("restart");
        reply("select LG2", () -> row("LG2"));
        act("other", () -> check("restart-other-group", boxes() == LinkedCategories.DEFAULT, LinkedCategories.toText(boxes())));
        closeMenu();
    }

    /** The GUI as it was: phase off (both sides 0), phase mixed (only the client at 1, server without the channel). */
    private void planOriginal(String phase) {
        until("scene", () -> { c = npc("LGUI_C"); return c != null; });
        openManager(() -> c);
        until("groups listed", () -> list().getList().contains("LG1"));
        act("select", () -> row("LG1"));
        add("no answer window", 60, () -> { }, () -> true);
        act("original buttons", () -> {
            Field f = GuiScreen.class.getDeclaredField("buttonList"); f.setAccessible(true);
            StringBuilder ids = new StringBuilder();
            for (GuiButton button : (List<GuiButton>) f.get(gui())) { ids.append(button.id).append(' '); }
            check(phase + "-original-buttons", "1 2 ".equals(ids.toString()), ids.toString());
            check(phase + "-no-labels", gui().getLabel(101) == null, "no panel labels");
            check(phase + "-no-reply", !"mixed".equals(phase) || LinkedClient.replies() == 0, "server without the channel");
        });
        image(phase);
        if ("off".equals(phase)) {
            act("command", () -> Minecraft.getMinecraft().player.sendChatMessage("/rwlinked list"));
            add("command settle", 20, () -> { }, () -> true);
        }
        closeMenu();
    }

    private void check(String key, boolean ok, String detail) {
        JsonObject r = new JsonObject();
        r.addProperty("key", key); r.addProperty("ok", ok); r.addProperty("detail", detail);
        checks.add(r);
        System.out.println("LINKEDGUI CHECK " + key + " ok=" + ok + " " + detail);
        if (!ok) { throw new IllegalStateException(key + ": " + detail); }
    }

    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || finished) { return; }
        try {
            if (step == null) {
                step = steps.poll();
                if (step == null) { return; }
                delay = step.wait; acted = false; since = System.nanoTime();
                System.out.println("LINKEDGUI STEP " + step.name);
            }
            if (System.nanoTime() - since > 120000000000L) { throw new IllegalStateException("Timeout " + step.name); }
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
            System.out.println("LINKEDGUI DONE success=" + ok);
        } catch (Exception ex) { ex.printStackTrace(); }
        mc.shutdown();
    }
}
