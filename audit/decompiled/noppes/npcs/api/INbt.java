/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.NBTTagCompound
 */
package noppes.npcs.api;

import net.minecraft.nbt.NBTTagCompound;

public interface INbt {
    public void remove(String var1);

    public boolean has(String var1);

    public boolean getBoolean(String var1);

    public void setBoolean(String var1, boolean var2);

    public short getShort(String var1);

    public void setShort(String var1, short var2);

    public int getInteger(String var1);

    public void setInteger(String var1, int var2);

    public byte getByte(String var1);

    public void setByte(String var1, byte var2);

    public long getLong(String var1);

    public void setLong(String var1, long var2);

    public double getDouble(String var1);

    public void setDouble(String var1, double var2);

    public float getFloat(String var1);

    public void setFloat(String var1, float var2);

    public String getString(String var1);

    public void setString(String var1, String var2);

    public byte[] getByteArray(String var1);

    public void setByteArray(String var1, byte[] var2);

    public int[] getIntegerArray(String var1);

    public void setIntegerArray(String var1, int[] var2);

    public Object[] getList(String var1, int var2);

    public int getListType(String var1);

    public void setList(String var1, Object[] var2);

    public INbt getCompound(String var1);

    public void setCompound(String var1, INbt var2);

    public String[] getKeys();

    public int getType(String var1);

    public NBTTagCompound getMCNBT();

    public String toJsonString();

    public boolean isEqual(INbt var1);

    public void clear();

    public void merge(INbt var1);
}

