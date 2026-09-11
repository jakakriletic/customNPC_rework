/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 */
package noppes.npcs.quests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.NpcMiscInventory;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.quests.QuestInterface;
import noppes.npcs.util.ValueUtil;

public class QuestItem
extends QuestInterface {
    public NpcMiscInventory items = new NpcMiscInventory(3);
    public boolean leaveItems = false;
    public boolean ignoreDamage = false;
    public boolean ignoreNBT = false;

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        this.items.setFromNBT(compound.getCompoundTag("Items"));
        this.leaveItems = compound.getBoolean("LeaveItems");
        this.ignoreDamage = compound.getBoolean("IgnoreDamage");
        this.ignoreNBT = compound.getBoolean("IgnoreNBT");
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        compound.setTag("Items", (NBTBase)this.items.getToNBT());
        compound.setBoolean("LeaveItems", this.leaveItems);
        compound.setBoolean("IgnoreDamage", this.ignoreDamage);
        compound.setBoolean("IgnoreNBT", this.ignoreNBT);
    }

    @Override
    public boolean isCompleted(EntityPlayer player) {
        List<ItemStack> questItems = NoppesUtilPlayer.countStacks(this.items, this.ignoreDamage, this.ignoreNBT);
        for (ItemStack reqItem : questItems) {
            if (NoppesUtilPlayer.compareItems(player, reqItem, this.ignoreDamage, this.ignoreNBT)) continue;
            return false;
        }
        return true;
    }

    public Map<ItemStack, Integer> getProgressSet(EntityPlayer player) {
        HashMap<ItemStack, Integer> map = new HashMap<ItemStack, Integer>();
        List<ItemStack> questItems = NoppesUtilPlayer.countStacks(this.items, this.ignoreDamage, this.ignoreNBT);
        for (ItemStack item : questItems) {
            if (NoppesUtilServer.IsItemStackNull(item)) continue;
            map.put(item, 0);
        }
        for (int i = 0; i < player.inventory.getSizeInventory(); ++i) {
            ItemStack item;
            item = player.inventory.getStackInSlot(i);
            if (NoppesUtilServer.IsItemStackNull(item)) continue;
            for (Map.Entry<ItemStack, Integer> questItem : map.entrySet()) {
                if (!NoppesUtilPlayer.compareItems(questItem.getKey(), item, this.ignoreDamage, this.ignoreNBT)) continue;
                map.put(questItem.getKey(), questItem.getValue() + item.getCount());
            }
        }
        return map;
    }

    @Override
    public void handleComplete(EntityPlayer player) {
        if (this.leaveItems) {
            return;
        }
        block0: for (ItemStack questitem : this.items.items) {
            if (questitem.isEmpty()) continue;
            int stacksize = questitem.getCount();
            for (int i = 0; i < player.inventory.getSizeInventory(); ++i) {
                ItemStack item = player.inventory.getStackInSlot(i);
                if (NoppesUtilServer.IsItemStackNull(item) || !NoppesUtilPlayer.compareItems(item, questitem, this.ignoreDamage, this.ignoreNBT)) continue;
                int size = item.getCount();
                if (stacksize - size >= 0) {
                    player.inventory.setInventorySlotContents(i, ItemStack.EMPTY);
                    item.splitStack(size);
                } else {
                    item.splitStack(stacksize);
                }
                if ((stacksize -= size) <= 0) continue block0;
            }
        }
    }

    @Override
    public IQuestObjective[] getObjectives(EntityPlayer player) {
        ArrayList<QuestItemObjective> list = new ArrayList<QuestItemObjective>();
        List<ItemStack> questItems = NoppesUtilPlayer.countStacks(this.items, this.ignoreDamage, this.ignoreNBT);
        for (ItemStack stack : questItems) {
            if (stack.isEmpty()) continue;
            list.add(new QuestItemObjective(player, stack));
        }
        return list.toArray(new IQuestObjective[list.size()]);
    }

    class QuestItemObjective
    implements IQuestObjective {
        private final EntityPlayer player;
        private final ItemStack questItem;

        public QuestItemObjective(EntityPlayer player, ItemStack item) {
            this.player = player;
            this.questItem = item;
        }

        @Override
        public int getProgress() {
            int count = 0;
            for (int i = 0; i < this.player.inventory.getSizeInventory(); ++i) {
                ItemStack item = this.player.inventory.getStackInSlot(i);
                if (NoppesUtilServer.IsItemStackNull(item) || !NoppesUtilPlayer.compareItems(this.questItem, item, QuestItem.this.ignoreDamage, QuestItem.this.ignoreNBT)) continue;
                count += item.getCount();
            }
            return ValueUtil.CorrectInt(count, 0, this.questItem.getCount());
        }

        @Override
        public void setProgress(int progress) {
            throw new CustomNPCsException("Cant set the progress of ItemQuests", new Object[0]);
        }

        @Override
        public int getMaxProgress() {
            return this.questItem.getCount();
        }

        @Override
        public boolean isCompleted() {
            return NoppesUtilPlayer.compareItems(this.player, this.questItem, QuestItem.this.ignoreDamage, QuestItem.this.ignoreNBT);
        }

        @Override
        public String getText() {
            return this.questItem.getDisplayName() + ": " + this.getProgress() + "/" + this.getMaxProgress();
        }
    }
}

