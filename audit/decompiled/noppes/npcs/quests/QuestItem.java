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
        this.items.setFromNBT(compound.func_74775_l("Items"));
        this.leaveItems = compound.func_74767_n("LeaveItems");
        this.ignoreDamage = compound.func_74767_n("IgnoreDamage");
        this.ignoreNBT = compound.func_74767_n("IgnoreNBT");
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        compound.func_74782_a("Items", (NBTBase)this.items.getToNBT());
        compound.func_74757_a("LeaveItems", this.leaveItems);
        compound.func_74757_a("IgnoreDamage", this.ignoreDamage);
        compound.func_74757_a("IgnoreNBT", this.ignoreNBT);
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
        for (int i = 0; i < player.field_71071_by.func_70302_i_(); ++i) {
            ItemStack item;
            item = player.field_71071_by.func_70301_a(i);
            if (NoppesUtilServer.IsItemStackNull(item)) continue;
            for (Map.Entry<ItemStack, Integer> questItem : map.entrySet()) {
                if (!NoppesUtilPlayer.compareItems(questItem.getKey(), item, this.ignoreDamage, this.ignoreNBT)) continue;
                map.put(questItem.getKey(), questItem.getValue() + item.func_190916_E());
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
            if (questitem.func_190926_b()) continue;
            int stacksize = questitem.func_190916_E();
            for (int i = 0; i < player.field_71071_by.func_70302_i_(); ++i) {
                ItemStack item = player.field_71071_by.func_70301_a(i);
                if (NoppesUtilServer.IsItemStackNull(item) || !NoppesUtilPlayer.compareItems(item, questitem, this.ignoreDamage, this.ignoreNBT)) continue;
                int size = item.func_190916_E();
                if (stacksize - size >= 0) {
                    player.field_71071_by.func_70299_a(i, ItemStack.field_190927_a);
                    item.func_77979_a(size);
                } else {
                    item.func_77979_a(stacksize);
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
            if (stack.func_190926_b()) continue;
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
            for (int i = 0; i < this.player.field_71071_by.func_70302_i_(); ++i) {
                ItemStack item = this.player.field_71071_by.func_70301_a(i);
                if (NoppesUtilServer.IsItemStackNull(item) || !NoppesUtilPlayer.compareItems(this.questItem, item, QuestItem.this.ignoreDamage, QuestItem.this.ignoreNBT)) continue;
                count += item.func_190916_E();
            }
            return ValueUtil.CorrectInt(count, 0, this.questItem.func_190916_E());
        }

        @Override
        public void setProgress(int progress) {
            throw new CustomNPCsException("Cant set the progress of ItemQuests", new Object[0]);
        }

        @Override
        public int getMaxProgress() {
            return this.questItem.func_190916_E();
        }

        @Override
        public boolean isCompleted() {
            return NoppesUtilPlayer.compareItems(this.player, this.questItem, QuestItem.this.ignoreDamage, QuestItem.this.ignoreNBT);
        }

        @Override
        public String getText() {
            return this.questItem.func_82833_r() + ": " + this.getProgress() + "/" + this.getMaxProgress();
        }
    }
}

