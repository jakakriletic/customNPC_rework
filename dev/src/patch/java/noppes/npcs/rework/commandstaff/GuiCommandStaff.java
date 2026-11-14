package noppes.npcs.rework.commandstaff;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public final class GuiCommandStaff extends GuiScreen {
    private static final String[] ON = {"Follow", "Wander", "Guard"};
    private static final String[] OFF = {"Stay", "Stationary", "Attack"};
    private int count, all, any;

    GuiCommandStaff(int count, int all, int any) { received(count, all, any); }

    @Override public void initGui() {
        int x = width / 2 - 105, y = height / 2 - 57;
        buttonList.clear();
        for (int i = 0; i < 3; i++) {
            buttonList.add(new GuiButton(2 * i, x, y + 20 + i * 23, 101, 20, OFF[i]));
            buttonList.add(new GuiButton(2 * i + 1, x + 109, y + 20 + i * 23, 101, 20, ON[i]));
        }
        buttonList.add(new GuiButton(6, x, y + 93, 101, 20, "Restore NPC AI"));
        buttonList.add(new GuiButton(7, x + 109, y + 93, 101, 20, "Done"));
        updateButtons();
    }

    void received(int selected, int all, int any) {
        this.count = selected;
        this.all = all;
        this.any = any;
        updateButtons();
    }

    private void updateButtons() {
        for (GuiButton button : buttonList) {
            int id = button.id;
            button.enabled = id == 7 || count > 0;
            if (id >= 6) { continue; }
            int group = id / 2, bit = 1 << group;
            boolean on = id % 2 == 1;
            boolean current = on ? (all & bit) != 0 : (any & bit) == 0;
            button.displayString = ((count > 0 && current) ? "> " : "") + (on ? ON[group] : OFF[group]);
        }
    }

    @Override protected void actionPerformed(GuiButton button) {
        if (button.id == 7) { mc.displayGuiScreen(null); }
        else if (button.id == 6) { StaffClient.send(StaffNetwork.RESTORE, 0, 0); }
        else {
            int bit = 1 << (button.id / 2);
            StaffClient.send(StaffNetwork.MODES, bit, button.id % 2 == 1 ? bit : 0);
        }
    }

    @Override public boolean doesGuiPauseGame() { return false; }

    @Override public void drawScreen(int x, int y, float partial) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, "Command Staff  ·  " + count + " selected", width / 2, height / 2 - 55, 0xf2cf70);
        super.drawScreen(x, y, partial);
    }
}
