/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 */
package noppes.npcs.controllers.data;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.controllers.data.Quest;

public class QuestData {
    public Quest quest;
    public boolean isCompleted;
    public NBTTagCompound extraData = new NBTTagCompound();

    public QuestData(Quest quest) {
        this.quest = quest;
    }

    public void writeEntityToNBT(NBTTagCompound nbttagcompound) {
        nbttagcompound.setBoolean("QuestCompleted", this.isCompleted);
        nbttagcompound.setTag("ExtraData", (NBTBase)this.extraData);
    }

    public void readEntityFromNBT(NBTTagCompound nbttagcompound) {
        this.isCompleted = nbttagcompound.getBoolean("QuestCompleted");
        this.extraData = nbttagcompound.getCompoundTag("ExtraData");
    }
}

