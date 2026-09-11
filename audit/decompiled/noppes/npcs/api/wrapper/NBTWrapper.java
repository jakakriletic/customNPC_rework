/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagDouble
 *  net.minecraft.nbt.NBTTagFloat
 *  net.minecraft.nbt.NBTTagInt
 *  net.minecraft.nbt.NBTTagIntArray
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.nbt.NBTTagString
 */
package noppes.npcs.api.wrapper;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.INbt;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.util.NBTJsonUtil;

public class NBTWrapper
implements INbt {
    private NBTTagCompound compound;

    public NBTWrapper(NBTTagCompound compound) {
        this.compound = compound;
    }

    @Override
    public void remove(String key) {
        this.compound.func_82580_o(key);
    }

    @Override
    public boolean has(String key) {
        return this.compound.func_74764_b(key);
    }

    @Override
    public boolean getBoolean(String key) {
        return this.compound.func_74767_n(key);
    }

    @Override
    public void setBoolean(String key, boolean value) {
        this.compound.func_74757_a(key, value);
    }

    @Override
    public short getShort(String key) {
        return this.compound.func_74765_d(key);
    }

    @Override
    public void setShort(String key, short value) {
        this.compound.func_74777_a(key, value);
    }

    @Override
    public int getInteger(String key) {
        return this.compound.func_74762_e(key);
    }

    @Override
    public void setInteger(String key, int value) {
        this.compound.func_74768_a(key, value);
    }

    @Override
    public byte getByte(String key) {
        return this.compound.func_74771_c(key);
    }

    @Override
    public void setByte(String key, byte value) {
        this.compound.func_74774_a(key, value);
    }

    @Override
    public long getLong(String key) {
        return this.compound.func_74763_f(key);
    }

    @Override
    public void setLong(String key, long value) {
        this.compound.func_74772_a(key, value);
    }

    @Override
    public double getDouble(String key) {
        return this.compound.func_74769_h(key);
    }

    @Override
    public void setDouble(String key, double value) {
        this.compound.func_74780_a(key, value);
    }

    @Override
    public float getFloat(String key) {
        return this.compound.func_74760_g(key);
    }

    @Override
    public void setFloat(String key, float value) {
        this.compound.func_74776_a(key, value);
    }

    @Override
    public String getString(String key) {
        return this.compound.func_74779_i(key);
    }

    @Override
    public void setString(String key, String value) {
        this.compound.func_74778_a(key, value);
    }

    @Override
    public byte[] getByteArray(String key) {
        return this.compound.func_74770_j(key);
    }

    @Override
    public void setByteArray(String key, byte[] value) {
        this.compound.func_74773_a(key, value);
    }

    @Override
    public int[] getIntegerArray(String key) {
        return this.compound.func_74759_k(key);
    }

    @Override
    public void setIntegerArray(String key, int[] value) {
        this.compound.func_74783_a(key, value);
    }

    @Override
    public Object[] getList(String key, int type) {
        NBTTagList list = this.compound.func_150295_c(key, type);
        Object[] nbts = new Object[list.func_74745_c()];
        for (int i = 0; i < list.func_74745_c(); ++i) {
            if (list.func_150303_d() == 10) {
                nbts[i] = NpcAPI.Instance().getINbt(list.func_150305_b(i));
                continue;
            }
            if (list.func_150303_d() == 8) {
                nbts[i] = list.func_150307_f(i);
                continue;
            }
            if (list.func_150303_d() == 6) {
                nbts[i] = list.func_150309_d(i);
                continue;
            }
            if (list.func_150303_d() == 5) {
                nbts[i] = Float.valueOf(list.func_150308_e(i));
                continue;
            }
            if (list.func_150303_d() == 3) {
                nbts[i] = list.func_186858_c(i);
                continue;
            }
            if (list.func_150303_d() != 11) continue;
            nbts[i] = list.func_150306_c(i);
        }
        return nbts;
    }

    @Override
    public int getListType(String key) {
        NBTBase b = this.compound.func_74781_a(key);
        if (b == null) {
            return 0;
        }
        if (b.func_74732_a() != 9) {
            throw new CustomNPCsException("NBT tag " + key + " isn't a list", new Object[0]);
        }
        return ((NBTTagList)b).func_150303_d();
    }

    @Override
    public void setList(String key, Object[] value) {
        NBTTagList list = new NBTTagList();
        for (Object nbt : value) {
            if (nbt instanceof INbt) {
                list.func_74742_a((NBTBase)((INbt)nbt).getMCNBT());
                continue;
            }
            if (nbt instanceof String) {
                list.func_74742_a((NBTBase)new NBTTagString((String)nbt));
                continue;
            }
            if (nbt instanceof Double) {
                list.func_74742_a((NBTBase)new NBTTagDouble(((Double)nbt).doubleValue()));
                continue;
            }
            if (nbt instanceof Float) {
                list.func_74742_a((NBTBase)new NBTTagFloat(((Float)nbt).floatValue()));
                continue;
            }
            if (nbt instanceof Integer) {
                list.func_74742_a((NBTBase)new NBTTagInt(((Integer)nbt).intValue()));
                continue;
            }
            if (!(nbt instanceof int[])) continue;
            list.func_74742_a((NBTBase)new NBTTagIntArray((int[])nbt));
        }
        this.compound.func_74782_a(key, (NBTBase)list);
    }

    @Override
    public INbt getCompound(String key) {
        return NpcAPI.Instance().getINbt(this.compound.func_74775_l(key));
    }

    @Override
    public void setCompound(String key, INbt value) {
        if (value == null) {
            throw new CustomNPCsException("Value cant be null", new Object[0]);
        }
        this.compound.func_74782_a(key, (NBTBase)value.getMCNBT());
    }

    @Override
    public String[] getKeys() {
        return this.compound.func_150296_c().toArray(new String[this.compound.func_150296_c().size()]);
    }

    @Override
    public int getType(String key) {
        return this.compound.func_150299_b(key);
    }

    @Override
    public NBTTagCompound getMCNBT() {
        return this.compound;
    }

    @Override
    public String toJsonString() {
        return NBTJsonUtil.Convert(this.compound);
    }

    @Override
    public boolean isEqual(INbt nbt) {
        if (nbt == null) {
            return false;
        }
        return this.compound.equals((Object)nbt.getMCNBT());
    }

    @Override
    public void clear() {
        for (String name : this.compound.func_150296_c()) {
            this.compound.func_82580_o(name);
        }
    }

    @Override
    public void merge(INbt nbt) {
        this.compound.func_179237_a(nbt.getMCNBT());
    }
}

