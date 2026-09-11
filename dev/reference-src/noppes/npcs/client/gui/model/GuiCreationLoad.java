/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 */
package noppes.npcs.client.gui.model;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import noppes.npcs.client.controllers.Preset;
import noppes.npcs.client.controllers.PresetController;
import noppes.npcs.client.gui.model.GuiCreationScreenInterface;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.ICustomScrollListener;
import noppes.npcs.entity.EntityNPCInterface;

public class GuiCreationLoad
extends GuiCreationScreenInterface
implements ICustomScrollListener {
    private List<String> list = new ArrayList<String>();
    private GuiCustomScroll scroll;

    public GuiCreationLoad(EntityNPCInterface npc) {
        super(npc);
        this.active = 5;
        this.xOffset = 60;
        PresetController.instance.load();
    }

    @Override
    public void initGui() {
        super.initGui();
        if (this.scroll == null) {
            this.scroll = new GuiCustomScroll(this, 0);
        }
        this.list.clear();
        for (Preset preset : PresetController.instance.presets.values()) {
            this.list.add(preset.name);
        }
        this.scroll.setList(this.list);
        this.scroll.guiLeft = this.guiLeft;
        this.scroll.guiTop = this.guiTop + 45;
        this.scroll.setSize(100, this.ySize - 96);
        this.addScroll(this.scroll);
        this.addButton(new GuiNpcButton(10, this.guiLeft, this.guiTop + this.ySize - 46, 120, 20, "gui.remove"));
    }

    @Override
    protected void actionPerformed(GuiButton btn) {
        super.actionPerformed(btn);
        if (btn.id == 10 && this.scroll.hasSelected()) {
            PresetController.instance.removePreset(this.scroll.getSelected());
            this.initGui();
        }
    }

    @Override
    public void scrollClicked(int i, int j, int k, GuiCustomScroll scroll) {
        Preset preset = PresetController.instance.getPreset(scroll.getSelected());
        this.playerdata.readFromNBT(preset.data.writeToNBT());
        this.initGui();
    }

    @Override
    public void scrollDoubleClicked(String selection, GuiCustomScroll scroll) {
    }
}

