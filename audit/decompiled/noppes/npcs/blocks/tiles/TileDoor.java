/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockDoor
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.network.NetworkManager
 *  net.minecraft.network.play.server.SPacketUpdateTileEntity
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.util.ITickable
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.World
 */
package noppes.npcs.blocks.tiles;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import noppes.npcs.CustomItems;
import noppes.npcs.blocks.tiles.TileNpcEntity;

public class TileDoor
extends TileNpcEntity
implements ITickable {
    public int ticksExisted = 0;
    public Block blockModel = CustomItems.scriptedDoor;
    public boolean needsClientUpdate = false;
    public TileEntity renderTile;
    public boolean renderTileErrored = true;
    public ITickable renderTileUpdate = null;

    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.func_177230_c() != newState.func_177230_c();
    }

    @Override
    public void func_145839_a(NBTTagCompound compound) {
        super.func_145839_a(compound);
        this.setDoorNBT(compound);
    }

    public void setDoorNBT(NBTTagCompound compound) {
        this.blockModel = (Block)Block.field_149771_c.func_82594_a((Object)new ResourceLocation(compound.func_74779_i("ScriptDoorBlockModel")));
        if (this.blockModel == null || !(this.blockModel instanceof BlockDoor)) {
            this.blockModel = CustomItems.scriptedDoor;
        }
        this.renderTileUpdate = null;
        this.renderTile = null;
        this.renderTileErrored = false;
    }

    @Override
    public NBTTagCompound func_189515_b(NBTTagCompound compound) {
        this.getDoorNBT(compound);
        return super.func_189515_b(compound);
    }

    public NBTTagCompound getDoorNBT(NBTTagCompound compound) {
        compound.func_74778_a("ScriptDoorBlockModel", Block.field_149771_c.func_177774_c((Object)this.blockModel) + "");
        return compound;
    }

    public void setItemModel(Block block) {
        if (block == null || !(block instanceof BlockDoor)) {
            block = CustomItems.scriptedDoor;
        }
        if (this.blockModel == block) {
            return;
        }
        this.blockModel = block;
        this.needsClientUpdate = true;
    }

    public void func_73660_a() {
        if (this.renderTileUpdate != null) {
            try {
                this.renderTileUpdate.func_73660_a();
            }
            catch (Exception e) {
                this.renderTileUpdate = null;
            }
        }
        ++this.ticksExisted;
        if (this.ticksExisted >= 10) {
            this.ticksExisted = 0;
            if (this.needsClientUpdate) {
                this.func_70296_d();
                IBlockState state = this.field_145850_b.func_180495_p(this.field_174879_c);
                this.field_145850_b.func_184138_a(this.field_174879_c, state, state, 3);
                this.needsClientUpdate = false;
            }
        }
    }

    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        this.handleUpdateTag(pkt.func_148857_g());
    }

    public void handleUpdateTag(NBTTagCompound compound) {
        this.setDoorNBT(compound);
    }

    public SPacketUpdateTileEntity func_189518_D_() {
        return new SPacketUpdateTileEntity(this.field_174879_c, 0, this.func_189517_E_());
    }

    public NBTTagCompound func_189517_E_() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.func_74768_a("x", this.field_174879_c.func_177958_n());
        compound.func_74768_a("y", this.field_174879_c.func_177956_o());
        compound.func_74768_a("z", this.field_174879_c.func_177952_p());
        this.getDoorNBT(compound);
        return compound;
    }
}

