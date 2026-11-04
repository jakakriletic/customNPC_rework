package noppes.npcs.rework.linked;

import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.SubGuiInterface;

/** M13.3: categories of a one-time copy from the primary row to the rest of the selection. */
@SideOnly(Side.CLIENT)
public final class LinkedWandCopyGui extends SubGuiInterface {
    public static final int FIRST_BOX = 100, DONE = 110, CANCEL = 111;
    private final String source;
    private final int count;
    public int mask = LinkedCategories.DEFAULT;
    public boolean accepted;

    public LinkedWandCopyGui(String source, int count) {
        this.source = source;
        this.count = count;
        this.setBackground("menubg.png");
        this.closeOnEsc = true;
        this.xSize = 256;
        this.ySize = 216;
    }

    @Override
    public void initGui() {
        super.initGui();
        int x = this.guiLeft + 24, y = this.guiTop + 34;
        this.addLabel(new GuiNpcLabel(0, "Copy from " + this.source, x, this.guiTop + 8));
        this.addLabel(new GuiNpcLabel(1, "to " + this.count + " selected NPCs, once, without a link:", x, this.guiTop + 20));
        for (int i = 0; i < LinkedGroupPanel.LABELS.length; i++) {
            this.addButton(new GuiNpcButton(FIRST_BOX + i, x + i % 2 * 106, y + i / 2 * 22, 102, 20, ""));
        }
        this.addButton(new GuiNpcButton(DONE, x, y + 96, 102, 20, "gui.done"));
        this.addButton(new GuiNpcButton(CANCEL, x + 106, y + 96, 102, 20, "gui.cancel"));
        refresh();
    }

    private void refresh() {
        for (int i = 0; i < LinkedGroupPanel.LABELS.length; i++) {
            this.getButton(FIRST_BOX + i).setDisplayText((LinkedCategories.has(this.mask, 1 << i) ? "[x] " : "[ ] ") + LinkedGroupPanel.LABELS[i]);
        }
        this.getButton(DONE).enabled = this.mask != 0;
    }

    @Override
    public void buttonEvent(GuiButton button) {
        if (button.id >= FIRST_BOX && button.id < FIRST_BOX + LinkedGroupPanel.LABELS.length) {
            this.mask ^= 1 << (button.id - FIRST_BOX);
            refresh();
            return;
        }
        this.accepted = button.id == DONE;
        this.close();
    }
}
