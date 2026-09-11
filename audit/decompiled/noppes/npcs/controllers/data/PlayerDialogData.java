/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 */
package noppes.npcs.controllers.data;

import java.util.HashSet;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class PlayerDialogData {
    public HashSet<Integer> dialogsRead = new HashSet();

    public void loadNBTData(NBTTagCompound compound) {
        HashSet<Integer> dialogsRead = new HashSet<Integer>();
        if (compound == null) {
            return;
        }
        NBTTagList list = compound.func_150295_c("DialogData", 10);
        if (list == null) {
            return;
        }
        for (int i = 0; i < list.func_74745_c(); ++i) {
            NBTTagCompound nbttagcompound = list.func_150305_b(i);
            dialogsRead.add(nbttagcompound.func_74762_e("Dialog"));
        }
        this.dialogsRead = dialogsRead;
    }

    public void saveNBTData(NBTTagCompound compound) {
        NBTTagList list = new NBTTagList();
        for (int dia : this.dialogsRead) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.func_74768_a("Dialog", dia);
            list.func_74742_a((NBTBase)nbttagcompound);
        }
        compound.func_74782_a("DialogData", (NBTBase)list);
    }
}

