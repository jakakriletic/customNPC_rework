/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 */
package noppes.npcs.client.gui.global;

import net.minecraft.client.gui.GuiButton;
import noppes.npcs.client.gui.global.SubGuiNpcDialogOption;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.DialogOption;

public class SubGuiNpcDialogOptions
extends SubGuiInterface {
    private Dialog dialog;

    public SubGuiNpcDialogOptions(Dialog dialog) {
        this.dialog = dialog;
        this.setBackground("menubg.png");
        this.xSize = 256;
        this.ySize = 216;
        this.closeOnEsc = true;
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        this.addLabel(new GuiNpcLabel(66, "dialog.options", this.guiLeft, this.guiTop + 4));
        this.getLabel(66).center(this.xSize);
        for (int i = 0; i < 6; ++i) {
            String optionString = "";
            DialogOption option = this.dialog.options.get(i);
            if (option != null && option.optionType != 2) {
                optionString = optionString + option.title;
            }
            this.addLabel(new GuiNpcLabel(i + 10, i + 1 + ": ", this.guiLeft + 4, this.guiTop + 16 + i * 32));
            this.addLabel(new GuiNpcLabel(i, optionString, this.guiLeft + 14, this.guiTop + 12 + i * 32));
            this.addButton(new GuiNpcButton(i, this.guiLeft + 13, this.guiTop + 21 + i * 32, 60, 20, "selectServer.edit"));
        }
        this.addButton(new GuiNpcButton(66, this.guiLeft + 82, this.guiTop + 194, 98, 20, "gui.done"));
    }

    @Override
    protected void func_146284_a(GuiButton guibutton) {
        int id = guibutton.field_146127_k;
        if (id < 6) {
            DialogOption option = this.dialog.options.get(id);
            if (option == null) {
                option = new DialogOption();
                this.dialog.options.put(id, option);
                option.optionColor = SubGuiNpcDialogOption.LastColor;
            }
            this.setSubGui(new SubGuiNpcDialogOption(option));
        }
        if (id == 66) {
            this.close();
        }
    }
}

