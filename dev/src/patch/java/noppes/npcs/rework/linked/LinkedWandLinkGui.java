package noppes.npcs.rework.linked;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.SubGuiInterface;

/** M13.3: picks the group the selected NPCs of the wand list join. */
@SideOnly(Side.CLIENT)
public final class LinkedWandLinkGui extends SubGuiInterface {
    public static final int DONE = 0, CANCEL = 1;
    private final List<String> groups;
    private final int count;
    private GuiCustomScroll scroll;
    public String chosen;

    public LinkedWandLinkGui(List<String> groups, int count) {
        this.groups = new ArrayList<String>(groups);
        this.count = count;
        this.setBackground("menubg.png");
        this.closeOnEsc = true;
        this.xSize = 256;
        this.ySize = 216;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.addLabel(new GuiNpcLabel(0, "Link " + this.count + " selected NPCs to:", this.guiLeft + 6, this.guiTop + 6));
        if (this.scroll == null) {
            this.scroll = new GuiCustomScroll(this, 0);
            this.scroll.setSize(165, 192);
        }
        this.scroll.guiLeft = this.guiLeft + 4;
        this.scroll.guiTop = this.guiTop + 18;
        this.scroll.setList(this.groups);
        this.addScroll(this.scroll);
        this.addButton(new GuiNpcButton(DONE, this.guiLeft + 170, this.guiTop + 18, 82, 20, "gui.done"));
        this.addButton(new GuiNpcButton(CANCEL, this.guiLeft + 170, this.guiTop + 40, 82, 20, "gui.cancel"));
    }

    @Override
    public void buttonEvent(GuiButton button) {
        if (button.id == DONE && !this.scroll.hasSelected()) {
            return;
        }
        if (button.id == DONE) {
            this.chosen = this.scroll.getSelected();
        }
        this.close();
    }
}
