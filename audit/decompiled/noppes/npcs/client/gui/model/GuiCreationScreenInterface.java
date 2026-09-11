/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.nbt.NBTTagCompound
 *  org.lwjgl.input.Keyboard
 */
package noppes.npcs.client.gui.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.ModelData;
import noppes.npcs.client.Client;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.mainmenu.GuiNpcDisplay;
import noppes.npcs.client.gui.model.GuiCreationEntities;
import noppes.npcs.client.gui.model.GuiCreationExtra;
import noppes.npcs.client.gui.model.GuiCreationLoad;
import noppes.npcs.client.gui.model.GuiCreationParts;
import noppes.npcs.client.gui.model.GuiCreationScale;
import noppes.npcs.client.gui.model.GuiPresetSave;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcSlider;
import noppes.npcs.client.gui.util.ISliderListener;
import noppes.npcs.client.gui.util.ISubGuiListener;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import org.lwjgl.input.Keyboard;

public abstract class GuiCreationScreenInterface
extends GuiNPCInterface
implements ISubGuiListener,
ISliderListener {
    public static String Message = "";
    public EntityLivingBase entity;
    private boolean saving = false;
    protected boolean hasSaving = true;
    public int active = 0;
    private EntityPlayer player;
    public int xOffset = 0;
    public ModelData playerdata;
    protected NBTTagCompound original = new NBTTagCompound();
    private static float rotation = 0.5f;

    public GuiCreationScreenInterface(EntityNPCInterface npc) {
        super(npc);
        this.playerdata = ((EntityCustomNpc)npc).modelData;
        this.original = this.playerdata.writeToNBT();
        this.xSize = 400;
        this.ySize = 240;
        this.xOffset = 140;
        this.player = Minecraft.func_71410_x().field_71439_g;
        this.closeOnEsc = true;
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        this.entity = this.playerdata.getEntity(this.npc);
        Keyboard.enableRepeatEvents((boolean)true);
        this.addButton(new GuiNpcButton(1, this.guiLeft + 62, this.guiTop, 60, 20, "gui.entity"));
        if (this.entity == null) {
            this.addButton(new GuiNpcButton(2, this.guiLeft, this.guiTop + 23, 60, 20, "gui.parts"));
        } else if (!(this.entity instanceof EntityNPCInterface)) {
            GuiCreationExtra gui = new GuiCreationExtra(this.npc);
            gui.playerdata = this.playerdata;
            if (!gui.getData(this.entity).isEmpty()) {
                this.addButton(new GuiNpcButton(2, this.guiLeft, this.guiTop + 23, 60, 20, "gui.extra"));
            } else if (this.active == 2) {
                this.field_146297_k.func_147108_a((GuiScreen)new GuiCreationEntities(this.npc));
                return;
            }
        }
        if (this.entity == null) {
            this.addButton(new GuiNpcButton(3, this.guiLeft + 62, this.guiTop + 23, 60, 20, "gui.scale"));
        }
        if (this.hasSaving) {
            this.addButton(new GuiNpcButton(4, this.guiLeft, this.guiTop + this.ySize - 24, 60, 20, "gui.save"));
            this.addButton(new GuiNpcButton(5, this.guiLeft + 62, this.guiTop + this.ySize - 24, 60, 20, "gui.load"));
        }
        if (this.getButton(this.active) == null) {
            this.openGui(new GuiCreationEntities(this.npc));
            return;
        }
        this.getButton((int)this.active).field_146124_l = false;
        this.addButton(new GuiNpcButton(66, this.guiLeft + this.xSize - 20, this.guiTop, 20, 20, "X"));
        this.addLabel(new GuiNpcLabel(0, Message, this.guiLeft + 120, this.guiTop + this.ySize - 10, 0xFF0000));
        this.getLabel(0).center(this.xSize - 120);
        this.addSlider(new GuiNpcSlider(this, 500, this.guiLeft + this.xOffset + 142, this.guiTop + 210, 120, 20, rotation));
    }

    @Override
    protected void func_146284_a(GuiButton btn) {
        super.func_146284_a(btn);
        if (btn.field_146127_k == 1) {
            this.openGui(new GuiCreationEntities(this.npc));
        }
        if (btn.field_146127_k == 2) {
            if (this.entity == null) {
                this.openGui(new GuiCreationParts(this.npc));
            } else {
                this.openGui(new GuiCreationExtra(this.npc));
            }
        }
        if (btn.field_146127_k == 3) {
            this.openGui(new GuiCreationScale(this.npc));
        }
        if (btn.field_146127_k == 4) {
            this.setSubGui(new GuiPresetSave(this, this.playerdata));
        }
        if (btn.field_146127_k == 5) {
            this.openGui(new GuiCreationLoad(this.npc));
        }
        if (btn.field_146127_k == 66) {
            this.save();
            NoppesUtil.openGUI(this.player, new GuiNpcDisplay(this.npc));
        }
    }

    @Override
    public void func_73864_a(int i, int j, int k) {
        if (!this.saving) {
            super.func_73864_a(i, j, k);
        }
    }

    @Override
    public void func_73863_a(int x, int y, float f) {
        super.func_73863_a(x, y, f);
        Object entity = this.entity = this.playerdata.getEntity(this.npc);
        if (entity == null) {
            entity = this.npc;
        } else {
            EntityUtil.Copy((EntityLivingBase)this.npc, entity);
        }
        this.drawNpc((EntityLivingBase)entity, this.xOffset + 200, 200, 2.0f, (int)(rotation * 360.0f - 180.0f));
    }

    @Override
    public void func_146281_b() {
        Keyboard.enableRepeatEvents((boolean)false);
    }

    @Override
    public void save() {
        NBTTagCompound newCompound = this.playerdata.writeToNBT();
        Client.sendData(EnumPacketServer.MainmenuDisplaySave, this.npc.display.writeToNBT(new NBTTagCompound()));
        Client.sendData(EnumPacketServer.ModelDataSave, newCompound);
    }

    public void openGui(GuiScreen gui) {
        this.field_146297_k.func_147108_a(gui);
    }

    @Override
    public void subGuiClosed(SubGuiInterface subgui) {
        this.func_73866_w_();
    }

    @Override
    public void mouseDragged(GuiNpcSlider slider) {
        if (slider.field_146127_k == 500) {
            rotation = slider.sliderValue;
            slider.setString("" + (int)(rotation * 360.0f));
        }
    }

    @Override
    public void mousePressed(GuiNpcSlider slider) {
    }

    @Override
    public void mouseReleased(GuiNpcSlider slider) {
    }
}

