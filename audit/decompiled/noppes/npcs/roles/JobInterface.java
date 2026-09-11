/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTTagCompound
 */
package noppes.npcs.roles;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.api.entity.data.INPCJob;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityNPCInterface;

public abstract class JobInterface
implements INPCJob {
    public EntityNPCInterface npc;
    public boolean overrideMainHand = false;
    public boolean overrideOffHand = false;

    public JobInterface(EntityNPCInterface npc) {
        this.npc = npc;
    }

    public abstract NBTTagCompound writeToNBT(NBTTagCompound var1);

    public abstract void readFromNBT(NBTTagCompound var1);

    public void killed() {
    }

    public void delete() {
    }

    public boolean aiShouldExecute() {
        return false;
    }

    public boolean aiContinueExecute() {
        return this.aiShouldExecute();
    }

    public void aiStartExecuting() {
    }

    public void aiUpdateTask() {
    }

    public void reset() {
    }

    public void resetTask() {
    }

    public IItemStack getMainhand() {
        return null;
    }

    public IItemStack getOffhand() {
        return null;
    }

    public boolean isFollowing() {
        return false;
    }

    public int getMutexBits() {
        return 0;
    }

    public ItemStack stringToItem(String s) {
        Item item;
        String[] split;
        if (s.isEmpty()) {
            return ItemStack.field_190927_a;
        }
        int damage = 0;
        if (s.contains(" - ") && (split = s.split(" - ")).length == 2) {
            try {
                damage = Integer.parseInt(split[1]);
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
            s = split[0];
        }
        if ((item = Item.func_111206_d((String)s)) == null) {
            return ItemStack.field_190927_a;
        }
        return new ItemStack(item, 1, damage);
    }

    public String itemToString(ItemStack item) {
        if (item == null || item.func_190926_b()) {
            return "";
        }
        return Item.field_150901_e.func_177774_c((Object)item.func_77973_b()) + " - " + item.func_77952_i();
    }

    @Override
    public int getType() {
        return this.npc.advanced.job;
    }
}

