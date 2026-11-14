package noppes.npcs.rework.army;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/** Global NPC editor: create a player-owned army in the shared world register. */
@SideOnly(Side.CLIENT)
public final class GuiArmyRegistry extends GuiScreen {
    private static final int ROWS = 5;
    private final GuiScreen parent;
    private GuiTextField name;
    private GuiButton create, previous, next;
    private String message = "";
    private int page, ticks, pending = -1, sentAt;

    public GuiArmyRegistry(GuiScreen parent) {
        this.parent = parent;
    }

    @Override public void initGui() {
        int left = width / 2 - 126, top = height / 2 - 91;
        String input = name == null ? "" : name.getText();
        name = new GuiTextField(0, fontRenderer, left + 1, top + 29, 169, 18);
        name.setMaxStringLength(32);
        name.setText(input);
        name.setFocused(true);
        Keyboard.enableRepeatEvents(true);
        buttonList.add(create = new GuiButton(0, left + 176, top + 28, 76, 20, "Create army"));
        buttonList.add(previous = new GuiButton(1, left, top + 147, 30, 20, "<"));
        buttonList.add(next = new GuiButton(2, left + 34, top + 147, 30, 20, ">"));
        buttonList.add(new GuiButton(3, left + 172, top + 147, 80, 20, "Done"));
        if (ticks == 0) { ArmyClient.request(); }
        updateButtons();
    }

    private List<ArmySnapshot.Entry> armies() {
        ArmySnapshot snapshot = ArmyClient.latest();
        return snapshot == null ? Collections.emptyList() : snapshot.armies;
    }

    private void updateButtons() {
        int count = armies().size();
        page = Math.max(0, Math.min(page, (count - 1) / ROWS));
        previous.enabled = page > 0;
        next.enabled = (page + 1) * ROWS < count;
        create.enabled = pending < 0 && Army.validName(name.getText().trim());
    }

    boolean received(ArmyStaffCodec.Response reply) {
        if (reply.id != pending) { return false; }
        pending = -1;
        message = reply.text;
        if (reply.ok) {
            name.setText("");
            ArmyClient.request();
        }
        updateButtons();
        return true;
    }

    @Override public void updateScreen() {
        ticks++;
        name.updateCursorCounter();
        if (ticks % 40 == 0) { ArmyClient.request(); }
        if (pending >= 0 && ticks - sentAt > 100) {
            pending = -1;
            message = "No reply from the server.";
        }
        updateButtons();
    }

    @Override protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:
                pending = ArmyClient.action(ArmyStaffCodec.Op.CREATE, -1, name.getText().trim(), 0, 0);
                sentAt = ticks;
                message = "Creating army...";
                break;
            case 1: page--; break;
            case 2: page++; break;
            case 3: mc.displayGuiScreen(parent); return;
            default: break;
        }
        updateButtons();
    }

    @Override protected void mouseClicked(int x, int y, int button) throws IOException {
        name.mouseClicked(x, y, button);
        super.mouseClicked(x, y, button);
    }

    @Override protected void keyTyped(char character, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(parent); }
        else if (key == Keyboard.KEY_RETURN && create.enabled) { actionPerformed(create); }
        else if (!name.textboxKeyTyped(character, key)) { super.keyTyped(character, key); }
        updateButtons();
    }

    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }
    @Override public boolean doesGuiPauseGame() { return false; }

    @Override public void drawScreen(int mouseX, int mouseY, float partial) {
        drawDefaultBackground();
        int left = width / 2 - 126, top = height / 2 - 91;
        drawCenteredString(fontRenderer, "Armies", width / 2, top, 0xffffff);
        drawString(fontRenderer, "Army name (letters, digits, _ or -)", left, top + 17, 0xffffff);
        name.drawTextBox();
        fontRenderer.drawSplitString(message, left, top + 52, 252, 0xffffff);
        List<ArmySnapshot.Entry> entries = armies();
        for (int i = page * ROWS; i < Math.min(entries.size(), (page + 1) * ROWS); i++) {
            ArmySnapshot.Entry army = entries.get(i);
            int y = top + 78 + (i % ROWS) * 13;
            drawString(fontRenderer, army.name, left, y, 0xffffff);
            String loaded = army.loaded + " loaded";
            drawString(fontRenderer, loaded, left + 252 - fontRenderer.getStringWidth(loaded), y, 0xaaaaaa);
        }
        super.drawScreen(mouseX, mouseY, partial);
    }
}
