/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.nbt.NBTTagCompound
 */
package noppes.npcs.client.gui.mainmenu;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.client.Client;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.GuiNpcTextureCloaks;
import noppes.npcs.client.gui.GuiNpcTextureOverlays;
import noppes.npcs.client.gui.model.GuiCreationParts;
import noppes.npcs.client.gui.select.GuiTextureSelection;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcButtonYesNo;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.IGuiData;
import noppes.npcs.client.gui.util.ITextfieldListener;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataDisplay;

public class GuiNpcDisplay
extends GuiNPCInterface2
implements ITextfieldListener,
IGuiData {
    private DataDisplay display;

    public GuiNpcDisplay(EntityNPCInterface npc) {
        super(npc, 1);
        this.display = npc.display;
        Client.sendData(EnumPacketServer.MainmenuDisplayGet, new Object[0]);
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        int y = this.guiTop + 4;
        this.addLabel(new GuiNpcLabel(0, "gui.name", this.guiLeft + 5, y + 5));
        this.addTextField(new GuiNpcTextField(0, this, this.field_146289_q, this.guiLeft + 50, y, 200, 20, this.display.getName()));
        this.addButton(new GuiNpcButton(0, this.guiLeft + 253, y, 110, 20, new String[]{"display.show", "display.hide", "display.showAttacking"}, this.display.getShowName()));
        this.addLabel(new GuiNpcLabel(11, "gui.title", this.guiLeft + 5, (y += 23) + 5));
        this.addTextField(new GuiNpcTextField(11, this, this.field_146289_q, this.guiLeft + 50, y, 200, 20, this.display.getTitle()));
        this.addLabel(new GuiNpcLabel(1, "display.model", this.guiLeft + 5, (y += 23) + 5));
        this.addButton(new GuiNpcButton(1, this.guiLeft + 50, y, 110, 20, "selectServer.edit"));
        this.addLabel(new GuiNpcLabel(2, "display.size", this.guiLeft + 175, y + 5));
        this.addTextField(new GuiNpcTextField(2, this, this.field_146289_q, this.guiLeft + 203, y, 40, 20, this.display.getSize() + ""));
        this.getTextField((int)2).numbersOnly = true;
        this.getTextField(2).setMinMaxDefault(1, 30, 5);
        this.addLabel(new GuiNpcLabel(3, "(1-30)", this.guiLeft + 246, y + 5));
        this.addLabel(new GuiNpcLabel(4, "display.texture", this.guiLeft + 5, (y += 23) + 5));
        this.addTextField(new GuiNpcTextField(3, this, this.field_146289_q, this.guiLeft + 80, y, 200, 20, this.display.skinType == 0 ? this.display.getSkinTexture() : this.display.getSkinUrl()));
        this.addButton(new GuiNpcButton(3, this.guiLeft + 325, y, 38, 20, "mco.template.button.select"));
        this.addButton(new GuiNpcButton(2, this.guiLeft + 283, y, 40, 20, new String[]{"display.texture", "display.player", "display.url"}, this.display.skinType));
        this.getButton(3).setEnabled(this.display.skinType == 0);
        if (this.display.skinType == 1 && !this.display.getSkinPlayer().isEmpty()) {
            this.getTextField(3).func_146180_a(this.display.getSkinPlayer());
        }
        this.addLabel(new GuiNpcLabel(8, "display.cape", this.guiLeft + 5, (y += 23) + 5));
        this.addTextField(new GuiNpcTextField(8, this, this.field_146289_q, this.guiLeft + 80, y, 200, 20, this.display.getCapeTexture()));
        this.addButton(new GuiNpcButton(8, this.guiLeft + 283, y, 80, 20, "display.selectTexture"));
        this.addLabel(new GuiNpcLabel(9, "display.overlay", this.guiLeft + 5, (y += 23) + 5));
        this.addTextField(new GuiNpcTextField(9, this, this.field_146289_q, this.guiLeft + 80, y, 200, 20, this.display.getOverlayTexture()));
        this.addButton(new GuiNpcButton(9, this.guiLeft + 283, y, 80, 20, "display.selectTexture"));
        this.addLabel(new GuiNpcLabel(5, "display.livingAnimation", this.guiLeft + 5, (y += 23) + 5));
        this.addButton(new GuiNpcButton(5, this.guiLeft + 120, y, 50, 20, new String[]{"gui.yes", "gui.no"}, this.display.getHasLivingAnimation() ? 0 : 1));
        this.addLabel(new GuiNpcLabel(6, "display.tint", this.guiLeft + 180, y + 5));
        String color = Integer.toHexString(this.display.getTint());
        while (color.length() < 6) {
            color = "0" + color;
        }
        this.addTextField(new GuiNpcTextField(6, this, this.guiLeft + 220, y, 60, 20, color));
        this.getTextField(6).func_146193_g(this.display.getTint());
        this.addLabel(new GuiNpcLabel(7, "display.visible", this.guiLeft + 5, (y += 23) + 5));
        this.addButton(new GuiNpcButton(7, this.guiLeft + 120, y, 50, 20, new String[]{"gui.yes", "gui.no", "gui.partly"}, this.display.getVisible()));
        this.addLabel(new GuiNpcLabel(13, "display.interactable", this.guiLeft + 180, y + 5));
        this.addButton(new GuiNpcButtonYesNo(13, this.guiLeft + 280, y, this.display.getHasHitbox()));
        this.addLabel(new GuiNpcLabel(10, "display.bossbar", this.guiLeft + 5, (y += 23) + 5));
        this.addButton(new GuiNpcButton(10, this.guiLeft + 60, y, 110, 20, new String[]{"display.hide", "display.show", "display.showAttacking"}, this.display.getBossbar()));
        this.addLabel(new GuiNpcLabel(12, "gui.color", this.guiLeft + 180, y + 5));
        this.addButton(new GuiNpcButton(12, this.guiLeft + 220, y, 110, 20, this.display.getBossColor(), "color.pink", "color.blue", "color.red", "color.green", "color.yellow", "color.purple", "color.white"));
    }

    @Override
    public void unFocused(GuiNpcTextField textfield) {
        if (textfield.field_175208_g == 0) {
            if (!textfield.isEmpty()) {
                this.display.setName(textfield.func_146179_b());
            } else {
                textfield.func_146180_a(this.display.getName());
            }
        } else if (textfield.field_175208_g == 2) {
            this.display.setSize(textfield.getInteger());
        } else if (textfield.field_175208_g == 3) {
            if (this.display.skinType == 2) {
                this.display.setSkinUrl(textfield.func_146179_b());
            } else if (this.display.skinType == 1) {
                this.display.setSkinPlayer(textfield.func_146179_b());
            } else {
                this.display.setSkinTexture(textfield.func_146179_b());
            }
        } else if (textfield.field_175208_g == 6) {
            int color = 0;
            try {
                color = Integer.parseInt(textfield.func_146179_b(), 16);
            }
            catch (NumberFormatException e) {
                color = 0xFFFFFF;
            }
            this.display.setTint(color);
            textfield.func_146193_g(this.display.getTint());
        } else if (textfield.field_175208_g == 8) {
            this.display.setCapeTexture(textfield.func_146179_b());
        } else if (textfield.field_175208_g == 9) {
            this.display.setOverlayTexture(textfield.func_146179_b());
        } else if (textfield.field_175208_g == 11) {
            this.display.setTitle(textfield.func_146179_b());
        }
    }

    @Override
    protected void func_146284_a(GuiButton guibutton) {
        GuiNpcButton button = (GuiNpcButton)guibutton;
        if (button.field_146127_k == 0) {
            this.display.setShowName(button.getValue());
        }
        if (button.field_146127_k == 1) {
            NoppesUtil.openGUI((EntityPlayer)this.player, new GuiCreationParts((EntityCustomNpc)this.npc));
        }
        if (button.field_146127_k == 2) {
            this.display.setSkinUrl("");
            this.display.setSkinPlayer(null);
            this.display.skinType = (byte)button.getValue();
            this.func_73866_w_();
        } else if (button.field_146127_k == 3) {
            this.setSubGui(new GuiTextureSelection(this.npc, this.npc.display.getSkinTexture()));
        } else if (button.field_146127_k == 5) {
            this.display.setHasLivingAnimation(button.getValue() == 0);
        } else if (button.field_146127_k == 7) {
            this.display.setVisible(button.getValue());
        } else if (button.field_146127_k == 8) {
            NoppesUtil.openGUI((EntityPlayer)this.player, (Object)new GuiNpcTextureCloaks(this.npc, this));
        } else if (button.field_146127_k == 9) {
            NoppesUtil.openGUI((EntityPlayer)this.player, (Object)new GuiNpcTextureOverlays(this.npc, this));
        } else if (button.field_146127_k == 10) {
            this.display.setBossbar(button.getValue());
        } else if (button.field_146127_k == 12) {
            this.display.setBossColor(button.getValue());
        } else if (button.field_146127_k == 13) {
            this.display.setHasHitbox(((GuiNpcButtonYesNo)button).getBoolean());
        }
    }

    @Override
    public void save() {
        if (this.display.skinType == 1) {
            this.display.loadProfile();
        }
        this.npc.textureLocation = null;
        this.field_146297_k.field_71438_f.func_72709_b((Entity)this.npc);
        this.field_146297_k.field_71438_f.func_72703_a((Entity)this.npc);
        Client.sendData(EnumPacketServer.MainmenuDisplaySave, this.display.writeToNBT(new NBTTagCompound()));
    }

    @Override
    public void setGuiData(NBTTagCompound compound) {
        this.display.readToNBT(compound);
        this.func_73866_w_();
    }
}

