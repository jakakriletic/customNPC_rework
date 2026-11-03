package noppes.npcs.rework.army;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Two request-only tabs. All actions use the same service as /rwarmy; no client authority. */
@SideOnly(Side.CLIENT)
public final class GuiArmyStaff extends GuiScreen {
    private final int npc;
    private final GuiScreen parent;
    private GuiTextField name;
    private String chosen = "", message = "Waiting for server...";
    private int tab, page, shape, stance, ticks, sentAt, lastId = -1;
    private boolean awaitingInspect;
    private final List<ArmySnapshot.Entry> rows = new ArrayList<ArmySnapshot.Entry>();
    public GuiArmyStaff(int npc) { this(npc, null); }
    public GuiArmyStaff(int npc, GuiScreen parent) { this.npc = npc; this.parent = parent; }
    @Override public void initGui() {
        int x = width / 2 - 200, y = height / 2 - 112;
        buttonList.clear();
        buttonList.add(new GuiButton(0, x, y + 18, 130, 20, "Nearby (256)"));
        buttonList.add(new GuiButton(1, x + 135, y + 18, 130, 20, "All armies"));
        buttonList.add(new GuiButton(2, x + 270, y + 18, 130, 20, "Refresh"));
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
        if (ticks == 0) {
            ArmyClient.request();
            if (npc >= 0) { awaitingInspect = true; lastId = ArmyClient.action(ArmyStaffCodec.Op.INSPECT, npc, "", 0, 0); }
        }
        updateRows();
    }
    public void received(ArmyStaffCodec.Response r) {
        if (r.id != lastId) { return; }
        message = r.text;
        if (awaitingInspect) { awaitingInspect = false; chosen = r.ok ? r.army : ""; name.setText(chosen); }
    }
    private void updateRows() {
        rows.clear();
        ArmySnapshot s = ArmyClient.latest();
        if (s != null) {
            for (ArmySnapshot.Entry e : s.armies) { if (tab == 1 || nearby(e, mc.player.dimension, mc.player.posX, mc.player.posZ)) { rows.add(e); } }
        }
        page = Math.max(0, Math.min(page, Math.max(0, (rows.size() - 1) / 4)));
        for (GuiButton b : buttonList) {
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
        if (ticks % 40 == 0) { ArmyClient.request(); }
        if (ticks - sentAt > 100 && "Waiting for server...".equals(message)) {
            message = "No reply. Server may have armies disabled; refresh or reopen.";
        }
        updateRows();
    }
    @Override protected void actionPerformed(GuiButton b) throws IOException {
        chosen = name.getText().trim();
        if (b.id >= 20 && b.id < 24) {
            chosen = rows.get(page * 4 + b.id - 20).name; name.setText(chosen);
            if (mc.player.getHeldItemMainhand().getItem() == ArmyStaff.item) { send(ArmyStaffCodec.Op.STATUS); }
            return;
        }
        switch (b.id) {
            case 0: case 1: tab = b.id; page = 0; updateRows(); break;
            case 2: ArmyClient.request(); break;
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
    private void send(ArmyStaffCodec.Op op) {
        if (!chosen.isEmpty() && !Army.validName(chosen)) { message = "Army name: 1-32 letters, digits, _ or -"; return; }
        awaitingInspect = false;
        lastId = ArmyClient.action(op, npc, chosen, shape, stance); sentAt = ticks; message = "Waiting for server...";
    }
    @Override protected void mouseClicked(int x, int y, int button) throws IOException { super.mouseClicked(x, y, button); name.mouseClicked(x, y, button); }
    @Override protected void keyTyped(char c, int key) throws IOException {
        if (!name.textboxKeyTyped(c, key)) { super.keyTyped(c, key); }
    }
    @Override public boolean doesGuiPauseGame() { return false; }
    @Override public void drawScreen(int x, int y, float partial) {
        drawDefaultBackground();
        int left = width / 2 - 200, top = height / 2 - 112;
        drawString(fontRenderer, npc >= 0 ? "NPC army (empty clears membership)" : "Army Command Staff", left, top + 3, 0xffffff);
        name.drawTextBox();
        super.drawScreen(x, y, partial);
        java.util.List<String> lines = fontRenderer.listFormattedStringToWidth(message, 400);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            drawString(fontRenderer, lines.get(i), left, top + 202 + i * 10, 0xffffff);
        }
    }
}
