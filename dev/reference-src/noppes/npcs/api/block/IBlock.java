/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.tileentity.TileEntity
 */
package noppes.npcs.api.block;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import noppes.npcs.api.IContainer;
import noppes.npcs.api.INbt;
import noppes.npcs.api.IPos;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.entity.data.IData;

public interface IBlock {
    public int getX();

    public int getY();

    public int getZ();

    public IPos getPos();

    public int getMetadata();

    public void setMetadata(int var1);

    public String getName();

    public void remove();

    public boolean isRemoved();

    public boolean isAir();

    public IBlock setBlock(String var1);

    public IBlock setBlock(IBlock var1);

    public boolean hasTileEntity();

    public boolean isContainer();

    public IContainer getContainer();

    public IData getTempdata();

    public IData getStoreddata();

    public IWorld getWorld();

    public INbt getTileEntityNBT();

    public void setTileEntityNBT(INbt var1);

    public TileEntity getMCTileEntity();

    public Block getMCBlock();

    public void blockEvent(int var1, int var2);

    public String getDisplayName();

    public IBlockState getMCBlockState();

    public void interact(int var1);
}

