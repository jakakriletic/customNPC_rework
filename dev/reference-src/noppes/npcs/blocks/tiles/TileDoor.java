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
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.setDoorNBT(compound);
    }

    public void setDoorNBT(NBTTagCompound compound) {
        this.blockModel = (Block)Block.REGISTRY.getObject((Object)new ResourceLocation(compound.getString("ScriptDoorBlockModel")));
        if (this.blockModel == null || !(this.blockModel instanceof BlockDoor)) {
            this.blockModel = CustomItems.scriptedDoor;
        }
        this.renderTileUpdate = null;
        this.renderTile = null;
        this.renderTileErrored = false;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        this.getDoorNBT(compound);
        return super.writeToNBT(compound);
    }

    public NBTTagCompound getDoorNBT(NBTTagCompound compound) {
        compound.setString("ScriptDoorBlockModel", Block.REGISTRY.getNameForObject((Object)this.blockModel) + "");
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

    public void update() {
        if (this.renderTileUpdate != null) {
            try {
                this.renderTileUpdate.update();
            }
            catch (Exception e) {
                this.renderTileUpdate = null;
            }
        }
        ++this.ticksExisted;
        if (this.ticksExisted >= 10) {
            this.ticksExisted = 0;
            if (this.needsClientUpdate) {
                this.markDirty();
                IBlockState state = this.world.getBlockState(this.pos);
                this.world.notifyBlockUpdate(this.pos, state, state, 3);
                this.needsClientUpdate = false;
            }
        }
    }

    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        this.handleUpdateTag(pkt.getNbtCompound());
    }

    public void handleUpdateTag(NBTTagCompound compound) {
        this.setDoorNBT(compound);
    }

    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, this.getUpdateTag());
    }

    public NBTTagCompound getUpdateTag() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setInteger("x", this.pos.getX());
        compound.setInteger("y", this.pos.getY());
        compound.setInteger("z", this.pos.getZ());
        this.getDoorNBT(compound);
        return compound;
    }
}

