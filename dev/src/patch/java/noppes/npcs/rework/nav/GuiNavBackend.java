package noppes.npcs.rework.nav;

import net.minecraft.client.gui.GuiButton;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.SubGuiInterface;

/** Small movement subdialog; runtime library classes are never loaded on the client. */
public final class GuiNavBackend extends SubGuiInterface {
    private final NavGuiState state;

    public GuiNavBackend(NavGuiState state) {
        this.state = state;
        setBackground("menubg.png");
        xSize = 256;
        ySize = 190;
        closeOnEsc = true;
    }

    @Override
    public void initGui() {
        super.initGui();
        addLabel(new GuiNpcLabel(0, "Navigation backend", guiLeft + 8, guiTop + 8));
        addLabel(new GuiNpcLabel(1, "Choice", guiLeft + 8, guiTop + 31));
        addButton(new GuiNpcButton(0, guiLeft + 92, guiTop + 26, 150, 20,
                new String[] {"Vanilla", "Baritone"}, state.choice()));
        getButton(0).setEnabled(state.ready());
        addLabel(new GuiNpcLabel(2, "Actual at open: " + state.actualName(), guiLeft + 8, guiTop + 57));
        addLabel(new GuiNpcLabel(3, "Reason: " + state.reasonText(), guiLeft + 8, guiTop + 69));
        addLabel(new GuiNpcLabel(4, state.pending() ? "Selection not saved yet." : "State captured on opening AI.",
                guiLeft + 8, guiTop + 89));
        addLabel(new GuiNpcLabel(5, "Changes apply on leaving AI.", guiLeft + 8, guiTop + 101));
        addLabel(new GuiNpcLabel(6, "Reopen AI to refresh status.", guiLeft + 8, guiTop + 113));
        addLabel(new GuiNpcLabel(7, "Cost depends on scene.", guiLeft + 8, guiTop + 135));
        addLabel(new GuiNpcLabel(8, "High NPC counts not certified.", guiLeft + 8, guiTop + 147));
        addButton(new GuiNpcButton(66, guiLeft + 182, guiTop + 166, 60, 20, "gui.done"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            state.choose(((GuiNpcButton) button).getValue());
            initGui();
        } else if (button.id == 66) {
            close();
        }
    }
}
