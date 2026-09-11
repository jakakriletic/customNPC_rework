/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.properties.IProperty
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.ITickable
 *  net.minecraft.util.math.AxisAlignedBB
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.World
 */
package noppes.npcs.blocks.tiles;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import noppes.npcs.CustomNpcs;
import noppes.npcs.blocks.BlockNpcRedstone;
import noppes.npcs.blocks.tiles.TileNpcEntity;
import noppes.npcs.controllers.data.Availability;

public class TileRedstoneBlock
extends TileNpcEntity
implements ITickable {
    public int onRange = 12;
    public int offRange = 20;
    public int onRangeX = 12;
    public int onRangeY = 12;
    public int onRangeZ = 12;
    public int offRangeX = 20;
    public int offRangeY = 20;
    public int offRangeZ = 20;
    public boolean isDetailed = false;
    public Availability availability = new Availability();
    public boolean isActivated = false;
    private int ticks = 10;

    public void func_73660_a() {
        if (this.field_145850_b.field_72995_K) {
            return;
        }
        --this.ticks;
        if (this.ticks > 0) {
            return;
        }
        this.ticks = this.onRange > 10 ? 20 : 10;
        Block block = this.func_145838_q();
        if (block == null || !(block instanceof BlockNpcRedstone)) {
            return;
        }
        if (CustomNpcs.FreezeNPCs) {
            if (this.isActivated) {
                this.setActive(block, false);
            }
            return;
        }
        if (!this.isActivated) {
            int z;
            int y;
            int x = this.isDetailed ? this.onRangeX : this.onRange;
            List<EntityPlayer> list = this.getPlayerList(x, y = this.isDetailed ? this.onRangeY : this.onRange, z = this.isDetailed ? this.onRangeZ : this.onRange);
            if (list.isEmpty()) {
                return;
            }
            for (EntityPlayer player : list) {
                if (!this.availability.isAvailable(player)) continue;
                this.setActive(block, true);
                return;
            }
        } else {
            int x = this.isDetailed ? this.offRangeX : this.offRange;
            int y = this.isDetailed ? this.offRangeY : this.offRange;
            int z = this.isDetailed ? this.offRangeZ : this.offRange;
            List<EntityPlayer> list = this.getPlayerList(x, y, z);
            for (EntityPlayer player : list) {
                if (!this.availability.isAvailable(player)) continue;
                return;
            }
            this.setActive(block, false);
        }
    }

    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.func_177230_c() != newState.func_177230_c();
    }

    private void setActive(Block block, boolean bo) {
        this.isActivated = bo;
        IBlockState state = block.func_176223_P().func_177226_a((IProperty)BlockNpcRedstone.ACTIVE, (Comparable)Boolean.valueOf(this.isActivated));
        this.field_145850_b.func_180501_a(this.field_174879_c, state, 2);
        this.func_70296_d();
        this.field_145850_b.func_184138_a(this.field_174879_c, state, state, 3);
        block.func_176213_c(this.field_145850_b, this.field_174879_c, state);
    }

    private List<EntityPlayer> getPlayerList(int x, int y, int z) {
        return this.field_145850_b.func_72872_a(EntityPlayer.class, new AxisAlignedBB((double)this.field_174879_c.func_177958_n(), (double)this.field_174879_c.func_177956_o(), (double)this.field_174879_c.func_177952_p(), (double)(this.field_174879_c.func_177958_n() + 1), (double)(this.field_174879_c.func_177956_o() + 1), (double)(this.field_174879_c.func_177952_p() + 1)).func_72314_b((double)x, (double)y, (double)z));
    }

    @Override
    public void func_145839_a(NBTTagCompound compound) {
        super.func_145839_a(compound);
        this.onRange = compound.func_74762_e("BlockOnRange");
        this.offRange = compound.func_74762_e("BlockOffRange");
        this.isDetailed = compound.func_74767_n("BlockIsDetailed");
        if (compound.func_74764_b("BlockOnRangeX")) {
            this.isDetailed = true;
            this.onRangeX = compound.func_74762_e("BlockOnRangeX");
            this.onRangeY = compound.func_74762_e("BlockOnRangeY");
            this.onRangeZ = compound.func_74762_e("BlockOnRangeZ");
            this.offRangeX = compound.func_74762_e("BlockOffRangeX");
            this.offRangeY = compound.func_74762_e("BlockOffRangeY");
            this.offRangeZ = compound.func_74762_e("BlockOffRangeZ");
        }
        if (compound.func_74764_b("BlockActivated")) {
            this.isActivated = compound.func_74767_n("BlockActivated");
        }
        this.availability.readFromNBT(compound);
    }

    @Override
    public NBTTagCompound func_189515_b(NBTTagCompound compound) {
        compound.func_74768_a("BlockOnRange", this.onRange);
        compound.func_74768_a("BlockOffRange", this.offRange);
        compound.func_74757_a("BlockActivated", this.isActivated);
        compound.func_74757_a("BlockIsDetailed", this.isDetailed);
        if (this.isDetailed) {
            compound.func_74768_a("BlockOnRangeX", this.onRangeX);
            compound.func_74768_a("BlockOnRangeY", this.onRangeY);
            compound.func_74768_a("BlockOnRangeZ", this.onRangeZ);
            compound.func_74768_a("BlockOffRangeX", this.offRangeX);
            compound.func_74768_a("BlockOffRangeY", this.offRangeY);
            compound.func_74768_a("BlockOffRangeZ", this.offRangeZ);
        }
        this.availability.writeToNBT(compound);
        return super.func_189515_b(compound);
    }
}

