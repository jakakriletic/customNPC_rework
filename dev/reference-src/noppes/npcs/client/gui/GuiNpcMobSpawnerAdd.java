/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.gui.GuiYesNo
 *  net.minecraft.client.gui.GuiYesNoCallback
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityList
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.text.translation.I18n
 *  net.minecraft.world.World
 */
package noppes.npcs.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import noppes.npcs.client.Client;
import noppes.npcs.client.controllers.ClientCloneController;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.IGuiData;
import noppes.npcs.constants.EnumPacketServer;

public class GuiNpcMobSpawnerAdd
extends GuiNPCInterface
implements GuiYesNoCallback,
IGuiData {
    private Entity toClone;
    private NBTTagCompound compound;
    private static boolean serverSide = false;
    private static int tab = 1;

    public GuiNpcMobSpawnerAdd(NBTTagCompound compound) {
        this.toClone = EntityList.createEntityFromNBT((NBTTagCompound)compound, (World)Minecraft.getMinecraft().world);
        this.compound = compound;
        this.setBackground("menubg.png");
        this.xSize = 256;
        this.ySize = 216;
    }

    @Override
    public void initGui() {
        super.initGui();
        String name = this.toClone.getName();
        this.addLabel(new GuiNpcLabel(0, "Save as", this.guiLeft + 4, this.guiTop + 6));
        this.addTextField(new GuiNpcTextField(0, this, this.fontRenderer, this.guiLeft + 4, this.guiTop + 18, 200, 20, name));
        this.addLabel(new GuiNpcLabel(1, "Tab", this.guiLeft + 10, this.guiTop + 50));
        this.addButton(new GuiNpcButton(2, this.guiLeft + 40, this.guiTop + 45, 20, 20, new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9"}, tab - 1));
        this.addButton(new GuiNpcButton(3, this.guiLeft + 4, this.guiTop + 95, new String[]{"clone.client", "clone.server"}, serverSide ? 1 : 0));
        this.addButton(new GuiNpcButton(0, this.guiLeft + 4, this.guiTop + 70, 80, 20, "gui.save"));
        this.addButton(new GuiNpcButton(1, this.guiLeft + 86, this.guiTop + 70, 80, 20, "gui.cancel"));
    }

    @Override
    public void buttonEvent(GuiButton guibutton) {
        int id = guibutton.id;
        if (id == 0) {
            String name = this.getTextField(0).getText();
            if (name.isEmpty()) {
                return;
            }
            int tab = ((GuiNpcButton)guibutton).getValue() + 1;
            if (!serverSide) {
                if (ClientCloneController.Instance.getCloneData(null, name, tab) != null) {
                    this.displayGuiScreen((GuiScreen)new GuiYesNo((GuiYesNoCallback)this, "", I18n.translateToLocal((String)"clone.overwrite"), 1));
                } else {
                    this.confirmClicked(true, 0);
                }
            } else {
                Client.sendData(EnumPacketServer.ClonePreSave, name, tab);
            }
        }
        if (id == 1) {
            this.close();
        }
        if (id == 2) {
            tab = ((GuiNpcButton)guibutton).getValue() + 1;
        }
        if (id == 3) {
            serverSide = ((GuiNpcButton)guibutton).getValue() == 1;
        }
    }

    public void confirmClicked(boolean confirm, int id) {
        if (confirm) {
            String name = this.getTextField(0).getText();
            if (!serverSide) {
                ClientCloneController.Instance.addClone(this.compound, name, tab);
            } else {
                Client.sendData(EnumPacketServer.CloneSave, name, tab);
            }
            this.close();
        } else {
            this.displayGuiScreen(this);
        }
    }

    @Override
    public void save() {
    }

    @Override
    public void setGuiData(NBTTagCompound compound) {
        if (compound.hasKey("NameExists")) {
            if (compound.getBoolean("NameExists")) {
                this.displayGuiScreen((GuiScreen)new GuiYesNo((GuiYesNoCallback)this, "", I18n.translateToLocal((String)"clone.overwrite"), 1));
            } else {
                this.confirmClicked(true, 0);
            }
        }
    }
}

