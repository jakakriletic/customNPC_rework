/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.RenderHelper
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.JsonToNBT
 *  net.minecraft.nbt.NBTException
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.util.math.BlockPos
 *  net.minecraftforge.fml.common.registry.EntityRegistry
 */
package noppes.npcs.client.gui;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import noppes.npcs.client.Client;
import noppes.npcs.client.gui.SubGuiNpcTextArea;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.IGuiData;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.constants.EnumPacketServer;

public class GuiNbtBook
extends GuiNPCInterface
implements IGuiData {
    private int x;
    private int y;
    private int z;
    private TileEntity tile;
    private IBlockState state;
    private ItemStack blockStack;
    private int entityId;
    private Entity entity;
    private NBTTagCompound originalCompound;
    private NBTTagCompound compound;
    private String faultyText = null;
    private String errorMessage = null;

    public GuiNbtBook(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.setBackground("menubg.png");
        this.xSize = 256;
        this.ySize = 216;
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        int y = this.guiTop + 40;
        if (this.state != null) {
            this.addLabel(new GuiNpcLabel(11, "x: " + this.x + ", y: " + y + ", z: " + this.z, this.guiLeft + 60, this.guiTop + 6));
            this.addLabel(new GuiNpcLabel(12, "id: " + Block.field_149771_c.func_177774_c((Object)this.state.func_177230_c()), this.guiLeft + 60, this.guiTop + 16));
        }
        if (this.entity != null) {
            this.addLabel(new GuiNpcLabel(12, "id: " + EntityRegistry.getEntry(this.entity.getClass()).getRegistryName(), this.guiLeft + 60, this.guiTop + 6));
        }
        this.addButton(new GuiNpcButton(0, this.guiLeft + 38, this.guiTop + 144, 180, 20, "nbt.edit"));
        this.getButton((int)0).field_146124_l = this.compound != null && !this.compound.func_82582_d();
        this.addLabel(new GuiNpcLabel(0, "", this.guiLeft + 4, this.guiTop + 167));
        this.addLabel(new GuiNpcLabel(1, "", this.guiLeft + 4, this.guiTop + 177));
        this.addButton(new GuiNpcButton(66, this.guiLeft + 128, this.guiTop + 190, 120, 20, "gui.close"));
        this.addButton(new GuiNpcButton(67, this.guiLeft + 4, this.guiTop + 190, 120, 20, "gui.save"));
        if (this.errorMessage != null) {
            this.getButton((int)67).field_146124_l = false;
            int i = this.errorMessage.indexOf(" at: ");
            if (i > 0) {
                this.getLabel((int)0).label = this.errorMessage.substring(0, i);
                this.getLabel((int)1).label = this.errorMessage.substring(i);
            } else {
                this.getLabel((int)0).label = this.errorMessage;
            }
        }
        if (this.getButton((int)67).field_146124_l && this.originalCompound != null) {
            this.getButton((int)67).field_146124_l = !this.originalCompound.equals((Object)this.compound);
        }
    }

    @Override
    protected void func_146284_a(GuiButton guibutton) {
        int id = guibutton.field_146127_k;
        if (id == 0) {
            if (this.faultyText != null) {
                this.setSubGui(new SubGuiNpcTextArea(this.compound.toString(), this.faultyText).enableHighlighting());
            } else {
                this.setSubGui(new SubGuiNpcTextArea(this.compound.toString()).enableHighlighting());
            }
        }
        if (id == 67) {
            this.getLabel((int)0).label = "Saved";
            if (this.compound.equals((Object)this.originalCompound)) {
                return;
            }
            if (this.tile == null) {
                Client.sendData(EnumPacketServer.NbtBookSaveEntity, this.entityId, this.compound);
                return;
            }
            Client.sendData(EnumPacketServer.NbtBookSaveBlock, this.x, this.y, this.z, this.compound);
            this.originalCompound = this.compound.func_74737_b();
            this.getButton((int)67).field_146124_l = false;
        }
        if (id == 66) {
            this.close();
        }
    }

    @Override
    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        super.func_73863_a(mouseX, mouseY, partialTicks);
        if (this.hasSubGui()) {
            return;
        }
        if (this.state != null) {
            GlStateManager.func_179094_E();
            GlStateManager.func_179109_b((float)(this.guiLeft + 4), (float)(this.guiTop + 4), (float)0.0f);
            GlStateManager.func_179152_a((float)3.0f, (float)3.0f, (float)3.0f);
            RenderHelper.func_74520_c();
            this.field_146296_j.func_180450_b(this.blockStack, 0, 0);
            this.field_146296_j.func_175030_a(this.field_146289_q, this.blockStack, 0, 0);
            RenderHelper.func_74518_a();
            GlStateManager.func_179121_F();
        }
        if (this.entity instanceof EntityLivingBase) {
            this.drawNpc((EntityLivingBase)this.entity, 20, 80, 1.0f, 0);
        }
    }

    @Override
    public void closeSubGui(SubGuiInterface gui) {
        super.closeSubGui(gui);
        if (gui instanceof SubGuiNpcTextArea) {
            try {
                this.compound = JsonToNBT.func_180713_a((String)((SubGuiNpcTextArea)gui).text);
                this.faultyText = null;
                this.errorMessage = null;
            }
            catch (NBTException e) {
                this.errorMessage = e.getLocalizedMessage();
                this.faultyText = ((SubGuiNpcTextArea)gui).text;
            }
            this.func_73866_w_();
        }
    }

    @Override
    public void save() {
    }

    @Override
    public void setGuiData(NBTTagCompound compound) {
        if (compound.func_74764_b("EntityId")) {
            this.entityId = compound.func_74762_e("EntityId");
            this.entity = this.player.field_70170_p.func_73045_a(this.entityId);
        } else {
            this.tile = this.player.field_70170_p.func_175625_s(new BlockPos(this.x, this.y, this.z));
            this.state = this.player.field_70170_p.func_180495_p(new BlockPos(this.x, this.y, this.z));
            this.blockStack = this.state.func_177230_c().func_185473_a(this.player.field_70170_p, new BlockPos(this.x, this.y, this.z), this.state);
        }
        this.originalCompound = compound.func_74775_l("Data");
        this.compound = this.originalCompound.func_74737_b();
        this.func_73866_w_();
    }
}

