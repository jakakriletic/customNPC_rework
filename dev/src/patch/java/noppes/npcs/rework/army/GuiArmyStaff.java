package noppes.npcs.rework.army;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Three request-only tabs (M11.8b: Permissions). All actions use the same service as /rwarmy; no client authority. */
@SideOnly(Side.CLIENT)
public final class GuiArmyStaff extends GuiScreen {
    private final int npc;
    private final GuiScreen parent;
    private GuiTextField name;
    private String chosen = "", message = "Waiting for server...", player = "";
    private int tab, page, shape, stance, ticks, sentAt, lastId = -1;
    private boolean awaitingInspect, awaitingPlayers;
    private boolean armySelection, batch;
    private final Set<String> selectedArmies = new LinkedHashSet<String>();
    private final List<String> pending = new ArrayList<String>();
    private ArmyStaffCodec.PermOp pendingOp;
    private int pendingKind, messageLine;
    private final List<String> players = new ArrayList<String>();
    private final List<ArmySnapshot.Entry> rows = new ArrayList<ArmySnapshot.Entry>();
    public GuiArmyStaff(int npc) { this(npc, null); }
    public GuiArmyStaff(int npc, GuiScreen parent) { this.npc = npc; this.parent = parent; }
    @Override public void initGui() {
        int x = width / 2 - 200, y = height / 2 - 112;
        buttonList.clear();
        buttonList.add(new GuiButton(0, x, y + 18, 97, 20, "Nearby (256)"));
        buttonList.add(new GuiButton(1, x + 101, y + 18, 97, 20, "All armies"));
        buttonList.add(new GuiButton(13, x + 202, y + 18, 97, 20, "Permissions"));
        buttonList.add(new GuiButton(2, x + 303, y + 18, 97, 20, "Refresh"));
        for (int i = 0; i < 4; i++) { buttonList.add(new GuiButton(20 + i, x, y + 43 + i * 20, 400, 20, "")); }
        buttonList.add(new GuiButton(3, x, y + 125, 45, 20, "<"));
        buttonList.add(new GuiButton(4, x + 50, y + 125, 45, 20, ">"));
        name = new GuiTextField(50, fontRenderer, x + 105, y + 126, 180, 18);
        name.setMaxStringLength(32); name.setText(chosen);
        buttonList.add(new GuiButton(5, x + 290, y + 125, 110, 20, npc >= 0 ? "Set NPC army" : "Assign within 16"));
        buttonList.add(new GuiButton(6, x, y + 150, 130, 20, "Mode: " + ArmyStaffCodec.SHAPES[shape]));
        buttonList.add(new GuiButton(7, x + 135, y + 150, 130, 20, "Stance: " + ArmyStaffCodec.STANCES[stance]));
        buttonList.add(new GuiButton(8, x + 270, y + 150, 130, 20, "Aim / right-click"));
        buttonList.add(new GuiButton(9, x, y + 175, 95, 20, "Stop"));
        buttonList.add(new GuiButton(10, x + 100, y + 175, 95, 20, "Return here"));
        buttonList.add(new GuiButton(11, x + 200, y + 175, 95, 20, "Order status"));
        buttonList.add(new GuiButton(12, x + 300, y + 175, 100, 20, "Done"));
        // M11.8b: Permissions tab; none = revoke the right, selected = the army in the field, all = every army.
        buttonList.add(new GuiButton(30, x + 290, y + 125, 110, 20, "Show grants"));
        String[] labels = {"none", "selected", "all"};
        for (int i = 0; i < 3; i++) {
            buttonList.add(new GuiButton(31 + i, x + i * 101, y + 150, 97, 20, "View: " + labels[i]));
            buttonList.add(new GuiButton(34 + i, x + i * 101, y + 175, 97, 20, "Command: " + labels[i]));
        }
        buttonList.add(new GuiButton(37, x + 303, y + 150, 97, 20, "Revoke army"));
        if (ticks == 0) {
            ArmyClient.request();
            if (npc >= 0) { awaitingInspect = true; lastId = ArmyClient.action(ArmyStaffCodec.Op.INSPECT, npc, "", 0, 0); }
        }
        updateRows();
    }
    public void received(ArmyStaffCodec.Response r) {
        if (r.id != lastId) { return; }
        message = r.text;
        messageLine = 0;
        if (!r.ok) {
            if (!pending.isEmpty()) { message += "\nStopped; remaining checked armies were not changed."; }
            pending.clear();
        }
        if (pending.isEmpty()) { batch = false; }
        if (awaitingPlayers) {
            awaitingPlayers = false; players.clear();
            if (r.ok) { String[] lines = r.text.split("\n"); for (int i = 1; i < lines.length; i++) { players.add(lines[i]); } }
            message = r.ok ? "Select a player, then a right. Players: " + players.size() : r.text;
            updateRows();
        }
        if (awaitingInspect) { awaitingInspect = false; chosen = r.ok ? r.army : ""; name.setText(chosen); }
    }
    private void updateRows() {
        rows.clear();
        ArmySnapshot s = ArmyClient.latest();
        if (s != null) {
            for (ArmySnapshot.Entry e : s.armies) { if (tab != 0 || nearby(e, mc.player.dimension, mc.player.posX, mc.player.posZ)) { rows.add(e); } }
        }
        int size = tab == 2 && !armySelection ? players.size() : rows.size();
        page = Math.max(0, Math.min(page, Math.max(0, (size - 1) / 4)));
        boolean staff = mc.player.getHeldItemMainhand().getItem() == ArmyStaff.item;
        for (GuiButton b : buttonList) {
            if (b.id == 13) { b.displayString = tab != 2 ? "Permissions" : armySelection ? "Players" : "Select armies"; }
            if (b.id == 2) { b.displayString = tab == 2 && !armySelection ? "Find players" : "Refresh"; }
            if (b.id >= 5 && b.id <= 11) { b.visible = tab != 2; }
            if (b.id >= 30 && b.id <= 37) {
                b.visible = tab == 2;
                b.enabled = staff && !player.isEmpty() && !batch && (b.id != 32 && b.id != 35 && b.id != 37
                        || !selectedArmies.isEmpty() || Army.validName(name.getText().trim()));
            }
            if (tab == 2 && !armySelection && b.id >= 20 && b.id < 24) {
                int index = page * 4 + b.id - 20; b.visible = index < players.size();
                if (b.visible) { b.displayString = (players.get(index).equalsIgnoreCase(player) ? "> " : "") + players.get(index); }
                continue;
            }
            if (tab == 2 && armySelection && b.id >= 20 && b.id < 24) {
                int index = page * 4 + b.id - 20; b.visible = index < rows.size();
                if (b.visible) { String army = rows.get(index).name; b.displayString = (selectedArmies.contains(army) ? "[x] " : "[ ] ") + army; }
                b.enabled = !batch;
                continue;
            }
            if (tab == 2 && (b.id <= 4 || b.id == 12 || b.id == 13 || b.id >= 20 && b.id < 24)) { b.enabled = !batch; }
            if (b.id >= 6 && b.id <= 11) {
                boolean command = false;
                if (s != null) { for (ArmySnapshot.Entry e : s.armies) { if (e.name.equalsIgnoreCase(name.getText().trim())) { command = e.command; } } }
                b.enabled = mc.player.getHeldItemMainhand().getItem() == ArmyStaff.item && (b.id == 11 || command);
            }
            if (b.id >= 20 && b.id < 24) {
                int index = page * 4 + b.id - 20; b.visible = index < rows.size();
                if (b.visible) {
                    ArmySnapshot.Entry e = rows.get(index);
                    b.displayString = e.name + "  n=" + e.loaded + (e.command ? " [command]" : " [view]")
                            + (e.hasLast() ? " d" + e.lastDimension + " " + e.lastX + "," + e.lastZ : " (no position)");
                }
            }
        }
    }
    /** Any currently loaded cluster in this dimension within 256 horizontal blocks. */
    public static boolean nearby(ArmySnapshot.Entry e, int dimension, double x, double z) {
        for (ArmyClusters.Cluster c : e.clusters) {
            if (c.dimension == dimension && (c.x - x) * (c.x - x) + (c.z - z) * (c.z - z) <= 256 * 256) { return true; }
        }
        return false;
    }
    @Override public void updateScreen() {
        ticks++; name.updateCursorCounter();
        if (!pending.isEmpty() && ArmyClient.staffReply() != null && ArmyClient.staffReply().id == lastId && ticks - sentAt >= 6) {
            String army = pending.remove(0); perm(pendingOp, pendingKind, army);
        }
        if (ticks % 40 == 0) { ArmyClient.request(); }
        if (ticks - sentAt > 100 && "Waiting for server...".equals(message)) {
            pending.clear(); batch = false;
            message = "No reply. Server may have armies disabled; refresh or reopen.";
        }
        updateRows();
    }
    @Override protected void actionPerformed(GuiButton b) throws IOException {
        chosen = name.getText().trim();
        if (tab == 2 && b.id >= 20 && b.id < 24) {
            if (armySelection) {
                String army = rows.get(page * 4 + b.id - 20).name;
                if (!selectedArmies.remove(army)) { selectedArmies.add(army); }
                message = "Checked for next action: " + String.join(", ", selectedArmies); messageLine = 0;
            } else {
                player = players.get(page * 4 + b.id - 20); selectedArmies.clear(); perm(ArmyStaffCodec.PermOp.GRANTS, 0);
            }
            updateRows(); return;
        }
        if (b.id >= 30 && b.id <= 37) {
            if (b.id == 30) { perm(ArmyStaffCodec.PermOp.GRANTS, 0); }
            else if (b.id == 37) { selected(ArmyStaffCodec.PermOp.REVOKE, 2); }
            else {
                int kind = b.id < 34 ? 0 : 1, scope = (b.id - 31) % 3;
                ArmyStaffCodec.PermOp op = scope == 0 ? ArmyStaffCodec.PermOp.REVOKE : ArmyStaffCodec.PermOp.GRANT;
                if (scope == 1) { selected(op, kind); } else { perm(op, kind, ArmyCommandService.ALL); }
            }
            return;
        }
        if (b.id >= 20 && b.id < 24) {
            chosen = rows.get(page * 4 + b.id - 20).name; name.setText(chosen);
            if (mc.player.getHeldItemMainhand().getItem() == ArmyStaff.item) { send(ArmyStaffCodec.Op.STATUS); }
            return;
        }
        switch (b.id) {
            case 0: case 1: tab = b.id; page = 0; updateRows(); break;
            case 13:
                if (tab != 2) { tab = 2; armySelection = false; page = 0; players(""); }
                else { armySelection = !armySelection; page = 0; updateRows(); }
                break;
            case 2: if (tab == 2 && !armySelection) { players(name.getText().trim()); } else { ArmyClient.request(); } break;
            case 3: page--; updateRows(); break;
            case 4: page++; updateRows(); break;
            case 5:
                if (npc < 0) { mc.displayGuiScreen(new net.minecraft.client.gui.GuiYesNo((yes, id) -> {
                    mc.displayGuiScreen(this); if (yes) { send(ArmyStaffCodec.Op.NEAR); }
                }, "Assign loaded NPCs within 16 blocks?", "Up to 256; existing army permissions apply.", 0)); }
                else { send(ArmyStaffCodec.Op.ASSIGN); } break;
            case 6: shape = (shape + 1) % ArmyStaffCodec.SHAPES.length; initGui(); break;
            case 7: stance = 1 - stance; initGui(); break;
            case 8:
                if (Army.validName(chosen)) { ArmyStaffClient.arm(chosen, shape, stance); }
                else { message = "Select an army first."; } break;
            case 9: send(ArmyStaffCodec.Op.STOP); break;
            case 10: send(ArmyStaffCodec.Op.RETURN); break;
            case 11: send(ArmyStaffCodec.Op.STATUS); break;
            case 12: mc.displayGuiScreen(parent); break;
            default: break;
        }
    }
    private void selected(ArmyStaffCodec.PermOp op, int kind) {
        if (selectedArmies.isEmpty()) { perm(op, kind); return; }
        pending.clear(); pending.addAll(selectedArmies); pendingOp = op; pendingKind = kind; batch = true;
        String army = pending.remove(0); perm(op, kind, army);
    }
    private void players(String prefix) {
        if (!prefix.isEmpty() && !ArmyStaffCodec.validPlayer(prefix)) { message = "Player prefix: up to 16 letters, digits or _"; return; }
        awaitingInspect = false; awaitingPlayers = true;
        lastId = ArmyClient.perm(ArmyStaffCodec.PermOp.PLAYERS, 0, prefix, ""); sentAt = ticks; message = "Waiting for server...";
        updateRows();
    }
    private void perm(ArmyStaffCodec.PermOp op, int kind) { perm(op, kind, chosen); }
    private void perm(ArmyStaffCodec.PermOp op, int kind, String army) {
        if (!ArmyStaffCodec.validPlayer(player)) { message = "Select a player first."; return; }
        if (!army.isEmpty() && !Army.validName(army)) { message = "Army name: 1-32 letters, digits, _ or -"; return; }
        awaitingInspect = false; awaitingPlayers = false;
        lastId = ArmyClient.perm(op, kind, player, army); sentAt = ticks; message = "Waiting for server...";
    }
    /** Shorter grant lines for the selected player; the server text stays complete in the reply. */
    private String shown() {
        if (tab != 2) { return message; }
        StringBuilder out = new StringBuilder();
        for (String line : message.split("\n")) {
            line = line.startsWith("RWARMY ") ? line.substring(7) : line;
            String prefix = "grant " + player + " by ";
            if (line.regionMatches(true, 0, prefix, 0, prefix.length())) { line = "by " + line.substring(prefix.length()); }
            out.append(out.length() == 0 ? "" : "\n").append(line);
        }
        return out.toString();
    }
    private void send(ArmyStaffCodec.Op op) {
        if (!chosen.isEmpty() && !Army.validName(chosen)) { message = "Army name: 1-32 letters, digits, _ or -"; return; }
        awaitingInspect = false;
        awaitingPlayers = false;
        lastId = ArmyClient.action(op, npc, chosen, shape, stance); sentAt = ticks; message = "Waiting for server...";
    }
    @Override protected void mouseClicked(int x, int y, int button) throws IOException { super.mouseClicked(x, y, button); name.mouseClicked(x, y, button); }
    @Override protected void keyTyped(char c, int key) throws IOException {
        if (!name.textboxKeyTyped(c, key)) { super.keyTyped(c, key); }
    }
    @Override public boolean doesGuiPauseGame() { return false; }
    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (tab == 2 && wheel != 0) {
            int max = Math.max(0, fontRenderer.listFormattedStringToWidth(shown(), 400).size() - 3);
            messageLine = Math.max(0, Math.min(max, messageLine + (wheel < 0 ? 1 : -1)));
        }
    }
    @Override public void drawScreen(int x, int y, float partial) {
        drawDefaultBackground();
        int left = width / 2 - 200, top = height / 2 - 112;
        drawString(fontRenderer, tab == 2 ? "Permissions: " + (player.isEmpty() ? "choose player" : player) + " | checked: " + selectedArmies.size()
                : npc >= 0 ? "NPC army (empty clears membership)" : "Army Command Staff", left, top + 3, 0xffffff);
        name.drawTextBox();
        super.drawScreen(x, y, partial);
        java.util.List<String> lines = fontRenderer.listFormattedStringToWidth(shown(), 400);
        int start = tab == 2 ? Math.min(messageLine, Math.max(0, lines.size() - 3)) : 0;
        for (int i = 0; i < Math.min(tab == 2 ? 3 : 2, lines.size() - start); i++) {
            drawString(fontRenderer, lines.get(i + start), left, top + (tab == 2 ? 200 : 202) + i * 10, 0xffffff);
        }
    }
}
