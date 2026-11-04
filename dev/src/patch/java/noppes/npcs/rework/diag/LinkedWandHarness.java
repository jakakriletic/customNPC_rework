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
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
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
import noppes.npcs.client.gui.GuiNpcRemoteEditor;
import noppes.npcs.client.gui.mainmenu.GuiNpcDisplay;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.client.gui.util.GuiNpcMenu;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.linked.LinkedCategories;
import noppes.npcs.rework.linked.LinkedClient;
import noppes.npcs.rework.linked.LinkedGuiCodec;
import noppes.npcs.rework.linked.LinkedWandCopyGui;
import noppes.npcs.rework.linked.LinkedWandLinkGui;
import noppes.npcs.rework.linked.LinkedWandPanel;
import noppes.npcs.rework.linked.WandSelection;

/**
 * M13.3 controller-only: the wand list with multiple selection through its real mouse and key handlers over a real
 * connection. Only the state of Shift and Ctrl is put in by the harness (LWJGL keys cannot be pressed from code). Member
 * state is read from the client's entities; forged packets are used only after the rights were taken away.
 */
@SideOnly(Side.CLIENT)
public final class LinkedWandHarness {
    private interface Act { void run() throws Exception; }
    private interface Cond { boolean ok() throws Exception; }
    private static final class Step {
        final String name; final int wait; final Act act; final Cond until;
        Step(String n, int w, Act a, Cond c) { name = n; wait = w; act = a; until = c; }
    }
    static final String T1 = "customnpcs:textures/entity/humanfemale/magestephanie.png";
    static final String T2 = "customnpcs:textures/entity/humanfemale/doctorstephanie.png";
    static final String STEVE = "customnpcs:textures/entity/humanmale/steve.png";

    private final Properties job;
    private final File out;
    private final JsonArray checks = new JsonArray(), images = new JsonArray();
    private final Deque<Step> steps = new ArrayDeque<Step>();
    private Step step;
    private int delay, before, poll;
    private boolean acted, finished;
    private long since;
    private final List<EntityNPCInterface> guards = new ArrayList<EntityNPCInterface>();
    private EntityNPCInterface src, cook;
    private int farId = -1;

    private LinkedWandHarness(Properties p) { job = p; out = new File(p.getProperty("out")); }

    static boolean installIfRequested(String path) throws Exception {
        Properties p = new Properties();
        try (Reader in = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)) { p.load(in); }
        if (!"1".equals(p.getProperty("linkedWand"))) { return false; }
        if (!p.containsKey("out") || !p.containsKey("connect") || !p.containsKey("phase")) { throw new IllegalArgumentException("linkedWand job"); }
        LinkedWandHarness h = new LinkedWandHarness(p);
        h.plan();
        MinecraftForge.EVENT_BUS.register(h);
        return true;
    }

    private void add(String n, int w, Act x, Cond u) { steps.add(new Step(n, w, x, u)); }
    private void act(String n, Act x) { add(n, 10, x, () -> true); }
    private void cmd(String text) { act(text, () -> Minecraft.getMinecraft().player.sendChatMessage("/" + text)); }
    private void until(String n, Cond u) { add(n, 5, () -> { }, u); }
    private void settle(String n, int ticks) { add(n, ticks, () -> { }, () -> true); }
    private void sync(String marker) { act("sync " + marker, () -> System.out.println("LINKEDWAND SYNC " + marker)); }
    private GuiNPCInterface gui() { return (GuiNPCInterface) Minecraft.getMinecraft().currentScreen; }
    private GuiNpcRemoteEditor list() {
        GuiScreen s = Minecraft.getMinecraft().currentScreen;
        return s instanceof GuiNpcRemoteEditor ? (GuiNpcRemoteEditor) s : null;
    }
    private LinkedWandPanel panel() { return list().rwPanel(); }
    private WandSelection sel() { return panel().selection(); }
    private boolean ready() { return list() != null && panel() != null && panel().active() && !panel().pending(); }

    private List<EntityNPCInterface> named(String name) {
        List<EntityNPCInterface> found = new ArrayList<EntityNPCInterface>();
        for (Entity e : Minecraft.getMinecraft().world.loadedEntityList) {
            if (e instanceof EntityNPCInterface && !e.isDead && name.equals(((EntityNPCInterface) e).display.getName())) { found.add((EntityNPCInterface) e); }
        }
        return found;
    }
    private int skins(List<EntityNPCInterface> npcs, String name, String texture) {
        int n = 0;
        for (EntityNPCInterface npc : npcs) {
            if (name.equals(npc.display.getName()) && texture.equals(npc.display.getSkinTexture())) { n++; }
        }
        return n;
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
    private void pressRow(GuiCustomScroll s, int index) throws Exception {
        press(s.guiLeft + 10, s.guiTop + 14 * index + 4 + 3);
    }
    /** A click on the row of that NPC, with the modifier keys as given. */
    private void row(int id, boolean shift, boolean ctrl) throws Exception {
        int index = -1;
        for (int i = 0; i < sel().visibleCount(); i++) { if (sel().row(i).id == id) { index = i; } }
        if (index < 0) { throw new IllegalStateException("No row for " + id); }
        rowAt(index, shift, ctrl);
    }
    private void rowAt(int index, boolean shift, boolean ctrl) throws Exception {
        LinkedWandPanel.shiftKey = () -> shift;
        LinkedWandPanel.ctrlKey = () -> ctrl;
        try { pressRow(list().getScroll(0), index); }
        finally { LinkedWandPanel.shiftKey = () -> false; LinkedWandPanel.ctrlKey = () -> false; }
    }
    private void type(GuiNpcTextField field, String value) throws Exception {
        press(field.x + 3, field.y + 3);
        GuiNPCInterface g = gui();
        g.keyTyped((char) 0, 207); // End, then backspace: normal text field input.
        for (int i = 0; i < 40; i++) { g.keyTyped((char) 0, 14); }
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
    @SuppressWarnings("unchecked")
    private static List<GuiButton> buttons(GuiScreen screen) throws Exception {
        Field f = GuiScreen.class.getDeclaredField("buttonList"); f.setAccessible(true);
        return (List<GuiButton>) f.get(screen);
    }
    private static String ids(GuiScreen screen) throws Exception {
        TreeSet<Integer> ids = new TreeSet<Integer>();
        for (GuiButton button : buttons(screen)) { ids.add(button.id); }
        return ids.toString();
    }
    /** The yes button of the vanilla dialog, through its own mouse handler. */
    private void yes() throws Exception {
        GuiScreen dialog = Minecraft.getMinecraft().currentScreen;
        for (GuiButton button : buttons(dialog)) {
            if (button.id == 0) {
                int x = button.x + 3, y = button.y + 3;
                dialog.drawScreen(x, y, 0);
                Method m = GuiScreen.class.getDeclaredMethod("mouseClicked", int.class, int.class, int.class);
                m.setAccessible(true);
                m.invoke(dialog, x, y, 0);
                return;
            }
        }
        throw new IllegalStateException("No yes button");
    }
    /** Right click with the wand into the air opens the list, as the original item does. */
    private void openList(String label) {
        add("open list " + label, 10, () -> {
            Minecraft mc = Minecraft.getMinecraft();
            mc.playerController.processRightClick(mc.player, mc.world, EnumHand.MAIN_HAND);
        }, () -> list() != null);
    }
    private void reply(String name, Act action) {
        add(name, 5, () -> { before = LinkedClient.replies(); action.run(); }, () -> LinkedClient.replies() > before);
    }
    private void screen(String name) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        String file = out.getName().replace(".json", "") + "-" + name + ".png";
        ScreenShotHelper.saveScreenshot(out.getParentFile(), file, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        images.add(file);
    }
    private void image(String name) { act("image " + name, () -> screen(name)); }
    private boolean guardSelection() {
        if (sel().count() != guards.size()) { return false; }
        for (EntityNPCInterface g : guards) {
            boolean in = false;
            for (int id : sel().ids()) { in |= id == g.getEntityId(); }
            if (!in) { return false; }
        }
        return true;
    }
    private String label(int id) { return gui().getLabel(id).label; }

    private void plan() {
        Minecraft mc = Minecraft.getMinecraft();
        String phase = job.getProperty("phase");
        add("menu", 0, () -> { }, () -> mc.currentScreen instanceof GuiMainMenu);
        add("join", 10, () -> {
            mc.gameSettings.pauseOnLostFocus = false;
            mc.displayGuiScreen(new GuiConnecting(new GuiMainMenu(), mc, "127.0.0.1", Integer.parseInt(job.getProperty("connect"))));
        }, () -> mc.world != null && mc.player != null && mc.currentScreen == null);
        cmd("gamemode 1"); cmd("gamerule doMobSpawning false"); cmd("tp @p 600 4 0 0 -90");
        add("load scene", 80, () -> { }, () -> mc.world.isBlockLoaded(new BlockPos(604, 4, 3)));
        cmd("replaceitem entity @p slot.hotbar.0 customnpcs:npcwand");
        if ("main".equals(phase)) { planMain(); } else { planOriginal(phase); }
        act("finish", () -> finish(true, ""));
    }

    private void planMain() {
        cmd("noppes clone spawn WGuard 1 598,4,3");
        cmd("noppes clone spawn WGuard 1 600,4,3");
        cmd("noppes clone spawn WGuard 1 602,4,3");
        cmd("noppes clone spawn WGuard 1 600,4,45");
        cmd("noppes clone spawn WCook 1 604,4,3");
        cmd("noppes clone spawn WSrc 1 596,4,3");
        until("spawned", () -> {
            guards.clear();
            for (EntityNPCInterface g : named("WGuard")) { if (g.posZ < 10) { guards.add(g); } }
            List<EntityNPCInterface> s = named("WSrc"), c = named("WCook");
            src = s.isEmpty() ? null : s.get(0); cook = c.isEmpty() ? null : c.get(0);
            return guards.size() == 3 && src != null && cook != null;
        });
        settle("scene settle", 40);

        // The panel appears only after the server sent the rows.
        openList("main");
        until("rows", () -> ready() && sel().visibleCount() == 6);
        act("panel", () -> {
            boolean controls = list().getButton(LinkedWandPanel.LINK) != null && list().getButton(LinkedWandPanel.COPY) != null
                    && list().getTextField(LinkedWandPanel.FILTER) != null && list().getButton(LinkedWandPanel.EDIT) != null
                    && list().getButton(LinkedWandPanel.FREEZE) != null && list().getButton(LinkedWandPanel.RESET_ALL) != null;
            check("panel-active", controls, ids(list()));
            boolean sorted = true;
            for (int i = 1; i < sel().visibleCount(); i++) { sorted &= sel().row(i - 1).distance <= sel().row(i).distance; }
            check("rows-from-server", sel().visibleCount() == 6 && sorted && sel().count() == 0 && list().getScroll(0).getList().size() == 6,
                    String.join("|", list().getScroll(0).getList()));
            for (int i = 0; i < sel().visibleCount(); i++) {
                if (sel().row(i).distance > 32) { farId = sel().row(i).id; }
            }
            check("far-row-listed", farId >= 0, "row past 32 blocks");
        });
        image("list");

        act("filter", () -> type(list().getTextField(LinkedWandPanel.FILTER), "wgu"));
        act("filter check", () -> {
            boolean guardsOnly = sel().visibleCount() == 4;
            for (int i = 0; i < sel().visibleCount(); i++) { guardsOnly &= "WGuard".equals(sel().row(i).name); }
            check("filter-by-name", guardsOnly && list().getScroll(0).getList().size() == 4, String.join("|", list().getScroll(0).getList()));
        });
        act("filter clear", () -> type(list().getTextField(LinkedWandPanel.FILTER), ""));
        until("filter cleared", () -> sel().visibleCount() == 6);

        act("guard row", () -> row(guards.get(0).getEntityId(), false, false));
        act("same name", () -> click(LinkedWandPanel.SAME_NAME));
        act("same name check", () -> check("same-name-radius", guardSelection() && sel().primaryRow().id == guards.get(0).getEntityId(),
                label(LinkedWandPanel.LABEL_SELECTION)));
        image("same-name");

        // Ctrl adds rows; the last one clicked is the source of the copy.
        act("ctrl 1", () -> row(guards.get(0).getEntityId(), false, false));
        act("ctrl 2", () -> row(guards.get(1).getEntityId(), false, true));
        act("ctrl 3", () -> row(guards.get(2).getEntityId(), false, true));
        act("ctrl src", () -> row(src.getEntityId(), false, true));
        act("ctrl check", () -> check("ctrl-selection", sel().count() == 4 && sel().primaryRow().id == src.getEntityId()
                && list().getButton(LinkedWandPanel.COPY).enabled, label(LinkedWandPanel.LABEL_SELECTION)));
        add("copy dialog", 5, () -> click(LinkedWandPanel.COPY), () -> list().hasSubGui() && list().getSubGui() instanceof LinkedWandCopyGui);
        for (int box : new int[]{1, 2, 3, 4, 5, 7}) {
            act("copy box " + box, () -> click(LinkedWandCopyGui.FIRST_BOX + box));
        }
        act("copy mask", () -> check("copy-categories", ((LinkedWandCopyGui) list().getSubGui()).mask == LinkedCategories.APPEARANCE,
                LinkedCategories.toText(((LinkedWandCopyGui) list().getSubGui()).mask)));
        image("copy");
        reply("copy done", () -> click(LinkedWandCopyGui.DONE));
        act("copy reply", () -> {
            LinkedGuiCodec.Response r = LinkedClient.latest();
            check("copy-reply", r.result == LinkedGuiCodec.RESULT_OK && r.loaded == 3 && r.text.equals("Copied to 3 from WSrc"), r.text);
        });
        until("copy reaches guards", () -> skins(guards, "WGuard", T2) == 3);
        act("copy check", () -> check("copy-appearance-only", skins(guards, "WGuard", T2) == 3 && state(cook).equals("WCook/" + STEVE)
                && state(src).equals("WSrc/" + T2), state(guards.get(0)) + " " + state(cook)));

        until("refreshed after copy", this::ready);
        act("shift 0", () -> rowAt(0, false, false));
        act("shift 2", () -> rowAt(2, true, false));
        act("shift check", () -> check("shift-range", sel().count() == 3 && sel().isSelected(0) && sel().isSelected(1) && sel().isSelected(2)
                && sel().primaryIndex() == 2, label(LinkedWandPanel.LABEL_SELECTION)));

        act("link row", () -> row(guards.get(1).getEntityId(), false, false));
        act("link same name", () -> click(LinkedWandPanel.SAME_NAME));
        add("link dialog", 5, () -> click(LinkedWandPanel.LINK), () -> list().hasSubGui() && list().getSubGui() instanceof LinkedWandLinkGui);
        act("link group row", () -> {
            GuiCustomScroll s = list().getSubGui().getScroll(0);
            pressRow(s, s.getList().indexOf("LW1"));
        });
        act("link group chosen", () -> check("link-dialog-groups", "LW1".equals(list().getSubGui().getScroll(0).getSelected())
                && list().getSubGui().getScroll(0).getList().size() == 2, String.join("|", list().getSubGui().getScroll(0).getList())));
        reply("link done", () -> click(LinkedWandLinkGui.DONE));
        act("link reply", () -> {
            LinkedGuiCodec.Response r = LinkedClient.latest();
            check("link-reply", r.result == LinkedGuiCodec.RESULT_OK && r.text.equals("Linked 3 to LW1"), r.text);
        });
        until("group reaches guards", () -> skins(guards, "WGuard", T1) == 3);
        act("link check", () -> check("link-reaches-members", skins(guards, "WGuard", T1) == 3 && state(cook).equals("WCook/" + STEVE), state(guards.get(2))));
        until("refreshed after link", this::ready);
        act("group shown", () -> {
            int index = -1;
            for (int i = 0; i < sel().visibleCount(); i++) { if (sel().row(i).id == guards.get(1).getEntityId()) { index = i; } }
            String row = sel().text(index);
            check("group-and-count-shown", row.endsWith(" : WGuard [LW1 3]") && label(LinkedWandPanel.LABEL_SELECTION).endsWith("; LW1 3 loaded"),
                    row + " / " + label(LinkedWandPanel.LABEL_SELECTION));
            boolean inside = true;
            for (int labelId : new int[]{LinkedWandPanel.LABEL_SELECTION, LinkedWandPanel.LABEL_STATUS}) {
                GuiNpcLabel footer = list().getLabel(labelId);
                int right = footer.x + Minecraft.getMinecraft().fontRenderer.getStringWidth(footer.label);
                int bottom = footer.y + Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT;
                inside &= footer.x >= list().guiLeft + 5 && right <= list().guiLeft + 252
                        && footer.y >= list().guiTop + 188 && bottom <= list().guiTop + 212;
                for (GuiButton button : buttons(list())) {
                    inside &= right <= button.x || footer.x >= button.x + button.width
                            || bottom <= button.y || footer.y >= button.y + button.height;
                }
            }
            check("footer-within-frame", inside, label(LinkedWandPanel.LABEL_SELECTION) + " / " + label(LinkedWandPanel.LABEL_STATUS));
        });
        image("linked");

        act("reset row", () -> row(cook.getEntityId(), false, false));
        reply("reset", () -> click(LinkedWandPanel.RESET));
        act("reset check", () -> check("reset-selected", LinkedClient.latest().result == LinkedGuiCodec.RESULT_OK
                && "Reset 1".equals(LinkedClient.latest().text), LinkedClient.latest().text));

        until("before delete", this::ready);
        act("delete far", () -> row(farId, false, false));
        act("delete cook", () -> row(cook.getEntityId(), false, true));
        add("delete dialog", 5, () -> click(LinkedWandPanel.DELETE), () -> Minecraft.getMinecraft().currentScreen instanceof GuiYesNo);
        add("delete yes", 10, () -> { before = LinkedClient.replies(); yes(); },
                () -> LinkedClient.replies() > before && ready() && sel().visibleCount() == 4);
        act("delete check", () -> {
            boolean gone = true;
            for (int i = 0; i < sel().visibleCount(); i++) { gone &= sel().row(i).id != farId && sel().row(i).id != cook.getEntityId(); }
            check("delete-selected", gone && "Deleted 2".equals(LinkedClient.latest().text) && sel().count() == 0, LinkedClient.latest().text);
        });

        act("edit row", () -> row(src.getEntityId(), false, false));
        add("edit", 5, () -> click(LinkedWandPanel.EDIT), () -> Minecraft.getMinecraft().currentScreen instanceof GuiNpcDisplay);
        add("edit settle", 20, () -> { }, () -> gui().getTextField(0) != null);
        act("edit check", () -> check("edit-primary", "WSrc".equals(gui().getTextField(0).getText()), gui().getTextField(0).getText()));
        act("close menu", () -> top(0));
        until("closed", () -> Minecraft.getMinecraft().currentScreen == null);

        // Rights are taken away; forged batches must change nothing.
        sync("deop");
        add("denied", 5, () -> poll = 0, () -> {
            LinkedGuiCodec.Response r = LinkedClient.latest();
            if (r != null && r.result == LinkedGuiCodec.RESULT_DENIED) { return true; }
            if (--poll <= 0) { LinkedClient.send(LinkedGuiCodec.BatchOp.LIST, 0, -1, "", new int[0]); poll = 25; }
            return false;
        });
        reply("forged link", () -> LinkedClient.send(LinkedGuiCodec.BatchOp.LINK, 0, -1, "LW2", guardIds()));
        act("forged link check", () -> check("forged-link-denied", LinkedClient.latest().result == LinkedGuiCodec.RESULT_DENIED, LinkedClient.latest().text));
        reply("forged delete", () -> LinkedClient.send(LinkedGuiCodec.BatchOp.DELETE, 0, -1, "", guardIds()));
        act("forged delete check", () -> check("forged-delete-denied", LinkedClient.latest().result == LinkedGuiCodec.RESULT_DENIED, LinkedClient.latest().text));
        act("malformed", () -> LinkedClient.send(new byte[]{1, 3, 0}));
        openList("denied");
        settle("denied window", 60);
        act("denied list", () -> check("no-rights-original-list", panel() != null && !panel().active() && list().getButton(LinkedWandPanel.LINK) == null
                && list().getTextField(LinkedWandPanel.FILTER) == null, ids(list())));
        image("denied");
        act("leave list", () -> Minecraft.getMinecraft().displayGuiScreen(null));
        settle("denied settle", 20);
        act("alive", () -> {
            Minecraft mc = Minecraft.getMinecraft();
            check("connection-alive", mc.player != null && mc.getConnection() != null && skins(guards, "WGuard", T1) == 3,
                    "rights change and malformed packet kept the connection and the members");
        });
        sync("denied");
    }

    private int[] guardIds() {
        int[] ids = new int[guards.size()];
        for (int i = 0; i < ids.length; i++) { ids[i] = guards.get(i).getEntityId(); }
        return ids;
    }

    /** The list as it was: phase off (both sides 0), phase mixed (only the client at 1, server without the channel). */
    private void planOriginal(String phase) {
        until("scene", () -> named("WGuard").size() == 3 && named("WSrc").size() == 1);
        openList(phase);
        // The original list keys rows by "distance : name", so same-name NPCs at one distance share a row (Q408).
        until("original rows", () -> originalRowsOk(list().getScroll(0).getList()));
        settle("no answer window", 60);
        act("original", () -> {
            boolean panel = "off".equals(phase) ? list().rwPanel() == null : list().rwPanel() != null && !list().rwPanel().active();
            check(phase + "-original-list", panel && originalRowsOk(list().getScroll(0).getList()) && "[0, 1, 2, 3, 4, 5]".equals(ids(list())) && list().getTextField(LinkedWandPanel.FILTER) == null
                    && list().getLabel(LinkedWandPanel.LABEL_SELECTION) == null, ids(list()) + " " + String.join("|", list().getScroll(0).getList()));
            check(phase + "-no-reply", !"mixed".equals(phase) || LinkedClient.replies() == 0, "server without the channel");
            check(phase + "-state-kept", skins(named("WGuard"), "WGuard", T1) == 3, "linked guards after the restart");
        });
        image(phase);
        act("leave list", () -> Minecraft.getMinecraft().displayGuiScreen(null));
        settle("leave settle", 10);
    }

    /** One WSrc row and one to three WGuard rows, nothing else and no repeated row. */
    private static boolean originalRowsOk(List<String> rows) {
        int src = 0, guard = 0;
        for (String r : rows) {
            if (r.endsWith(" : WSrc")) { src++; } else if (r.endsWith(" : WGuard")) { guard++; } else { return false; }
        }
        return src == 1 && guard >= 1 && guard <= 3 && new TreeSet<String>(rows).size() == rows.size();
    }

    private void check(String key, boolean ok, String detail) {
        JsonObject r = new JsonObject();
        r.addProperty("key", key); r.addProperty("ok", ok); r.addProperty("detail", detail);
        checks.add(r);
        System.out.println("LINKEDWAND CHECK " + key + " ok=" + ok + " " + detail);
        if (!ok) { throw new IllegalStateException(key + ": " + detail); }
    }

    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || finished) { return; }
        try {
            if (step == null) {
                step = steps.poll();
                if (step == null) { return; }
                delay = step.wait; acted = false; since = System.nanoTime();
                System.out.println("LINKEDWAND STEP " + step.name);
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
            System.out.println("LINKEDWAND DONE success=" + ok);
        } catch (Exception ex) { ex.printStackTrace(); }
        mc.shutdown();
    }
}
