/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.util.text.translation.I18n
 */
package noppes.npcs.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.ITextfieldListener;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.controllers.data.Faction;

public class SubGuiNpcFactionPoints
extends SubGuiInterface
implements ITextfieldListener {
    private Faction faction;

    public SubGuiNpcFactionPoints(Faction faction) {
        this.faction = faction;
        this.setBackground("menubg.png");
        this.xSize = 256;
        this.ySize = 216;
        this.closeOnEsc = true;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.addLabel(new GuiNpcLabel(2, "faction.default", this.guiLeft + 4, this.guiTop + 33));
        this.addTextField(new GuiNpcTextField(2, this, this.guiLeft + 8 + this.fontRenderer.getStringWidth(this.getLabel((int)2).label), this.guiTop + 28, 70, 20, this.faction.defaultPoints + ""));
        this.getTextField(2).setMaxStringLength(6);
        this.getTextField((int)2).numbersOnly = true;
        String title = I18n.translateToLocal((String)"faction.unfriendly") + "<->" + I18n.translateToLocal((String)"faction.neutral");
        this.addLabel(new GuiNpcLabel(3, title, this.guiLeft + 4, this.guiTop + 80));
        this.addTextField(new GuiNpcTextField(3, this, this.guiLeft + 8 + this.fontRenderer.getStringWidth(title), this.guiTop + 75, 70, 20, this.faction.neutralPoints + ""));
        title = I18n.translateToLocal((String)"faction.neutral") + "<->" + I18n.translateToLocal((String)"faction.friendly");
        this.addLabel(new GuiNpcLabel(4, title, this.guiLeft + 4, this.guiTop + 105));
        this.addTextField(new GuiNpcTextField(4, this, this.guiLeft + 8 + this.fontRenderer.getStringWidth(title), this.guiTop + 100, 70, 20, this.faction.friendlyPoints + ""));
        this.getTextField((int)3).numbersOnly = true;
        this.getTextField((int)4).numbersOnly = true;
        if (this.getTextField((int)3).x > this.getTextField((int)4).x) {
            this.getTextField((int)4).x = this.getTextField((int)3).x;
        } else {
            this.getTextField((int)3).x = this.getTextField((int)4).x;
        }
        this.addButton(new GuiNpcButton(66, this.guiLeft + 20, this.guiTop + 192, 90, 20, "gui.done"));
    }

    @Override
    public void unFocused(GuiNpcTextField textfield) {
        if (textfield.id == 2) {
            this.faction.defaultPoints = textfield.getInteger();
        } else if (textfield.id == 3) {
            this.faction.neutralPoints = textfield.getInteger();
        } else if (textfield.id == 4) {
            this.faction.friendlyPoints = textfield.getInteger();
        }
    }

    @Override
    protected void actionPerformed(GuiButton guibutton) {
        int id = guibutton.id;
        if (id == 66) {
            this.close();
        }
    }
}

