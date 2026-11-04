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
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
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
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.linked.LinkedClient;
import noppes.npcs.rework.linked.LinkedGuiCodec;
import noppes.npcs.rework.linked.LinkedWandLinkGui;
import noppes.npcs.rework.linked.LinkedWandPanel;
import noppes.npcs.rework.linked.WandSelection;

/**
 * M13.5 controller-only: the exit proof of M13. One hundred NPCs that are already placed are linked with one selection
 * in the wand list (one row, then "same name in radius") and one Link action, then an edit of one member reaches the
 * other 99. Real mouse and key handlers over a real connection; member state is read from the client's entities.
 */
@SideOnly(Side.CLIENT)
public final class LinkedHundredHarness {
    private interface Act { void run() throws Exception; }
    private interface Cond { boolean ok() throws Exception; }
    private static final class Step {
        final String name; final int wait; final Act act; final Cond until;
        Step(String n, int w, Act a, Cond c) { name = n; wait = w; act = a; until = c; }
    }
    static final String T1 = "customnpcs:textures/entity/humanfemale/magestephanie.png";
    static final String STEVE = "customnpcs:textures/entity/humanmale/steve.png";
    static final String TITLE = "M135 Edit";
    static final int MEMBERS = 100, FAR = 2, COOKS = 5, ROWS = MEMBERS + FAR + COOKS;

    private final Properties job;
    private final File out;
    private final JsonArray checks = new JsonArray(), images = new JsonArray();
    private final Deque<Step> steps = new ArrayDeque<Step>();
    private final List<String> chat = new ArrayList<String>();
    private Step step;
    private int delay, before, primary = -1;
    private boolean acted, finished;
    private long since;

    private LinkedHundredHarness(Properties p) { job = p; out = new File(p.getProperty("out")); }

    static boolean installIfRequested(String path) throws Exception {
        Properties p = new Properties();
        try (Reader in = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)) { p.load(in); }
        if (!"1".equals(p.getProperty("linkedHundred"))) { return false; }
        if (!p.containsKey("out") || !p.containsKey("connect") || !p.containsKey("phase")) { throw new IllegalArgumentException("linkedHundred job"); }
        LinkedHundredHarness h = new LinkedHundredHarness(p);
        h.plan();
        MinecraftForge.EVENT_BUS.register(h);
        return true;
    }

    private void add(String n, int w, Act x, Cond u) { steps.add(new Step(n, w, x, u)); }
    private void act(String n, Act x) { add(n, 10, x, () -> true); }
    private void cmd(String text) { act(text, () -> Minecraft.getMinecraft().player.sendChatMessage("/" + text)); }
    private void until(String n, Cond u) { add(n, 5, () -> { }, u); }
    private void settle(String n, int ticks) { add(n, ticks, () -> { }, () -> true); }
    private GuiNPCInterface gui() { return (GuiNPCInterface) Minecraft.getMinecraft().currentScreen; }
    private GuiNpcRemoteEditor list() {
        GuiScreen s = Minecraft.getMinecraft().currentScreen;
        return s instanceof GuiNpcRemoteEditor ? (GuiNpcRemoteEditor) s : null;
    }
    private LinkedWandPanel panel() { return list().rwPanel(); }
    private WandSelection sel() { return panel().selection(); }
    private boolean ready() { return list() != null && panel() != null && panel().active() && !panel().pending(); }
    private String label(int id) { return gui().getLabel(id).label; }

    @SubscribeEvent public void chat(ClientChatReceivedEvent event) { chat.add(event.getMessage().getUnformattedText()); }
    private boolean said(String text) { for (String line : chat) { if (line.contains(text)) { return true; } } return false; }

    /** Loaded NPCs with that name; near = the grid in front of the player, far = the two past the radius. */
    private List<EntityNPCInterface> named(String name, Boolean near) {
        List<EntityNPCInterface> found = new ArrayList<EntityNPCInterface>();
        for (Entity e : Minecraft.getMinecraft().world.loadedEntityList) {
            if (e instanceof EntityNPCInterface && !e.isDead && name.equals(((EntityNPCInterface) e).display.getName())
                    && (near == null || near == (e.posZ < 30))) {
                found.add((EntityNPCInterface) e);
            }
        }
        return found;
    }
    private List<EntityNPCInterface> members() { return named("WGuard", true); }
    private static int skins(List<EntityNPCInterface> npcs, String texture) {
        int n = 0;
        for (EntityNPCInterface npc : npcs) { if (texture.equals(npc.display.getSkinTexture())) { n++; } }
        return n;
    }
    private static int titled(List<EntityNPCInterface> npcs) {
        int n = 0;
        for (EntityNPCInterface npc : npcs) { if (TITLE.equals(npc.display.getTitle())) { n++; } }
        return n;
    }
    private static Set<Integer> ids(List<EntityNPCInterface> npcs) {
        Set<Integer> ids = new HashSet<Integer>();
        for (EntityNPCInterface npc : npcs) { ids.add(npc.getEntityId()); }
        return ids;
    }
    private static String counts(List<EntityNPCInterface> npcs) {
        return npcs.size() + " npcs, " + skins(npcs, T1) + " group skin, " + skins(npcs, STEVE) + " steve, " + titled(npcs) + " titled";
    }

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
    private static String buttonIds(GuiScreen screen) throws Exception {
        Field f = GuiScreen.class.getDeclaredField("buttonList"); f.setAccessible(true);
        TreeSet<Integer> ids = new TreeSet<Integer>();
        for (GuiButton button : (List<GuiButton>) f.get(screen)) { ids.add(button.id); }
        return ids.toString();
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
    /** Turn the camera to the grid for a picture of the members; no server action depends on the view. */
    private void view(String name) {
        act("look " + name, () -> { Minecraft.getMinecraft().player.rotationYaw = 0F; Minecraft.getMinecraft().player.rotationPitch = 20F; });
        settle("look settle " + name, 20);
        image(name);
    }

    private void plan() {
        Minecraft mc = Minecraft.getMinecraft();
        String phase = job.getProperty("phase");
        add("menu", 0, () -> { }, () -> mc.currentScreen instanceof GuiMainMenu);
        add("join", 10, () -> {
            mc.gameSettings.pauseOnLostFocus = false;
            mc.displayGuiScreen(new GuiConnecting(new GuiMainMenu(), mc, "127.0.0.1", Integer.parseInt(job.getProperty("connect"))));
        }, () -> mc.world != null && mc.player != null && mc.currentScreen == null);
        cmd("gamemode 1"); cmd("gamerule doMobSpawning false"); cmd("tp @p 600 4 0 0 -90");
        add("load scene", 80, () -> { }, () -> mc.world.isBlockLoaded(new BlockPos(604, 4, 20)) && mc.world.isBlockLoaded(new BlockPos(603, 4, 45)));
        cmd("replaceitem entity @p slot.hotbar.0 customnpcs:npcwand");
        if ("main".equals(phase)) { planMain(); } else { planOff(); }
        act("finish", () -> finish(true, ""));
    }

    private void planMain() {
        // The scene is placed before the list is opened: 100 members in a 10 x 10 grid, distractors with another name
        // inside the radius and two of the same name past it.
        cmd("noppes clone grid WGuard 1 10 10 595,4,4");
        cmd("noppes clone grid WCook 1 5 1 598,4,20");
        cmd("noppes clone spawn WGuard 1 600,4,45");
        cmd("noppes clone spawn WGuard 1 603,4,45");
        until("spawned", () -> members().size() == MEMBERS && named("WGuard", false).size() == FAR && named("WCook", null).size() == COOKS);
        settle("scene settle", 60);

        openList("main");
        until("rows", () -> ready() && sel().visibleCount() == ROWS);
        act("rows", () -> {
            boolean sorted = true;
            for (int i = 1; i < sel().visibleCount(); i++) { sorted &= sel().row(i - 1).distance <= sel().row(i).distance; }
            check("rows-from-server", sel().visibleCount() == ROWS && sorted && sel().count() == 0
                    && list().getScroll(0).getList().size() == ROWS && "WGuard".equals(sel().row(0).name),
                    sel().visibleCount() + " rows; " + label(LinkedWandPanel.LABEL_SELECTION));
            check("placed-before-list", skins(members(), STEVE) == MEMBERS && titled(members()) == 0, counts(members()));
        });
        image("list");

        // One selection: the nearest row, then every row with its name within the radius field (32 by default).
        act("first row", () -> pressRow(list().getScroll(0), 0));
        act("same name", () -> click(LinkedWandPanel.SAME_NAME));
        act("selection", () -> {
            Set<Integer> chosen = new HashSet<Integer>();
            for (int id : sel().ids()) { chosen.add(id); }
            primary = sel().primaryRow().id;
            boolean farOut = true;
            for (int id : ids(named("WGuard", false))) { farOut &= !chosen.contains(id); }
            check("one-selection-100", chosen.size() == MEMBERS && chosen.equals(ids(members())) && farOut
                    && label(LinkedWandPanel.LABEL_SELECTION).startsWith("Selected 100 of " + ROWS),
                    chosen.size() + " selected; " + label(LinkedWandPanel.LABEL_SELECTION));
        });
        image("selected");

        add("link dialog", 5, () -> click(LinkedWandPanel.LINK), () -> list().hasSubGui() && list().getSubGui() instanceof LinkedWandLinkGui);
        act("link group row", () -> {
            GuiCustomScroll s = list().getSubGui().getScroll(0);
            pressRow(s, s.getList().indexOf("LW1"));
        });
        act("link group chosen", () -> check("link-dialog-group", "LW1".equals(list().getSubGui().getScroll(0).getSelected()),
                String.join("|", list().getSubGui().getScroll(0).getList())));
        reply("link done", () -> click(LinkedWandLinkGui.DONE));
        act("link reply", () -> {
            LinkedGuiCodec.Response r = LinkedClient.latest();
            check("link-reply-100", r.result == LinkedGuiCodec.RESULT_OK && r.loaded == MEMBERS && r.text.equals("Linked 100 to LW1"), r.text);
        });
        until("group reaches members", () -> skins(members(), T1) == MEMBERS);
        act("link check", () -> check("link-reaches-100", skins(members(), T1) == MEMBERS && skins(named("WGuard", false), STEVE) == FAR
                && skins(named("WCook", null), STEVE) == COOKS, counts(members()) + "; far " + counts(named("WGuard", false))));
        until("refreshed after link", () -> ready() && sel().primaryRow() != null && sel().primaryRow().id == primary
                && label(LinkedWandPanel.LABEL_SELECTION).endsWith("; LW1 100 loaded"));
        act("group shown", () -> check("group-and-count-shown", sel().text(sel().primaryIndex()).endsWith(" : WGuard [LW1 100]")
                && sel().count() == MEMBERS && label(LinkedWandPanel.LABEL_SELECTION).endsWith("; LW1 100 loaded"),
                sel().text(sel().primaryIndex()) + " / " + label(LinkedWandPanel.LABEL_SELECTION)));
        image("linked");

        // A change of one member through the normal editor reaches the other 99 through the group.
        add("edit", 5, () -> click(LinkedWandPanel.EDIT), () -> Minecraft.getMinecraft().currentScreen instanceof GuiNpcDisplay);
        add("display settle", 30, () -> { }, () -> gui().getTextField(11) != null);
        act("edited member", () -> check("edit-member", "WGuard".equals(gui().getTextField(0).getText()), gui().getTextField(0).getText()));
        act("title", () -> type(gui().getTextField(11), TITLE));
        act("leave title", () -> press(gui().getTextField(0).x + 3, gui().getTextField(0).y + 3));
        act("close menu", () -> top(0));
        until("closed", () -> Minecraft.getMinecraft().currentScreen == null);
        until("notification", () -> said("Group LW1: updated 99 loaded NPCs; pending 0"));
        until("change reaches members", () -> titled(members()) == MEMBERS);
        act("change check", () -> {
            check("update-notification-99", said("Group LW1: updated 99 loaded NPCs; pending 0"), String.join(" | ", chat));
            check("change-reaches-100", titled(members()) == MEMBERS && skins(members(), T1) == MEMBERS
                    && titled(named("WGuard", false)) == 0 && titled(named("WCook", null)) == 0,
                    counts(members()) + "; far " + counts(named("WGuard", false)));
        });
        view("members");
    }

    /** Both sides at 0 after the restart: the original list, the links and the change kept. */
    private void planOff() {
        until("scene", () -> members().size() == MEMBERS && named("WGuard", false).size() == FAR && named("WCook", null).size() == COOKS);
        openList("off");
        until("original rows", () -> originalRowsOk(list().getScroll(0).getList()));
        settle("no answer window", 60);
        act("original", () -> {
            check("off-original-list", list().rwPanel() == null && originalRowsOk(list().getScroll(0).getList())
                    && "[0, 1, 2, 3, 4, 5]".equals(buttonIds(list())) && list().getTextField(LinkedWandPanel.FILTER) == null
                    && list().getLabel(LinkedWandPanel.LABEL_SELECTION) == null,
                    buttonIds(list()) + " " + list().getScroll(0).getList().size() + " rows");
            check("off-state-kept", skins(members(), T1) == MEMBERS && titled(members()) == MEMBERS
                    && skins(named("WGuard", false), STEVE) == FAR && titled(named("WGuard", false)) == 0,
                    counts(members()) + "; far " + counts(named("WGuard", false)));
        });
        image("off");
        act("leave list", () -> Minecraft.getMinecraft().displayGuiScreen(null));
        settle("leave settle", 10);
    }

    /** Only WGuard and WCook rows, no repeated row; the original list keys rows by "distance : name" (Q408). */
    private static boolean originalRowsOk(List<String> rows) {
        int cook = 0, guard = 0;
        for (String r : rows) {
            if (r.endsWith(" : WCook")) { cook++; } else if (r.endsWith(" : WGuard")) { guard++; } else { return false; }
        }
        return cook >= 1 && guard >= 2 && rows.size() <= ROWS && new TreeSet<String>(rows).size() == rows.size();
    }

    private void check(String key, boolean ok, String detail) {
        JsonObject r = new JsonObject();
        r.addProperty("key", key); r.addProperty("ok", ok); r.addProperty("detail", detail);
        checks.add(r);
        System.out.println("LINKEDHUNDRED CHECK " + key + " ok=" + ok + " " + detail);
        if (!ok) { throw new IllegalStateException(key + ": " + detail); }
    }

    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || finished) { return; }
        try {
            if (step == null) {
                step = steps.poll();
                if (step == null) { return; }
                delay = step.wait; acted = false; since = System.nanoTime();
                System.out.println("LINKEDHUNDRED STEP " + step.name);
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
            System.out.println("LINKEDHUNDRED DONE success=" + ok);
        } catch (Exception ex) { ex.printStackTrace(); }
        mc.shutdown();
    }
}
