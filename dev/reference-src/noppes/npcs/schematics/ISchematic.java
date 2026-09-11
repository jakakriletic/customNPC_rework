/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.nbt.NBTTagCompound
 */
package noppes.npcs.schematics;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;

public interface ISchematic {
    public short getWidth();

    public short getHeight();

    public short getLength();

    public int getTileEntitySize();

    public NBTTagCompound getTileEntity(int var1);

    public String getName();

    public IBlockState getBlockState(int var1, int var2, int var3);

    public IBlockState getBlockState(int var1);

    public NBTTagCompound getNBT();
}

