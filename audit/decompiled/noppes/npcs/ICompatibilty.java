/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.NBTTagCompound
 */
package noppes.npcs;

import net.minecraft.nbt.NBTTagCompound;

public interface ICompatibilty {
    public int getVersion();

    public void setVersion(int var1);

    public NBTTagCompound writeToNBT(NBTTagCompound var1);
}

