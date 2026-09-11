/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.ItemStackHelper
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.NonNullList
 *  net.minecraft.util.text.ITextComponent
 */
package noppes.npcs;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import noppes.npcs.NBTTags;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;

public class NpcMiscInventory
implements IInventory {
    public final NonNullList<ItemStack> items;
    public int stackLimit = 64;
    private int size;

    public NpcMiscInventory(int size) {
        this.size = size;
        this.items = NonNullList.withSize((int)size, (Object)ItemStack.EMPTY);
    }

    public NBTTagCompound getToNBT() {
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        nbttagcompound.setTag("NpcMiscInv", (NBTBase)NBTTags.nbtItemStackList(this.items));
        return nbttagcompound;
    }

    public void setFromNBT(NBTTagCompound nbttagcompound) {
        NBTTags.getItemStackList(nbttagcompound.getTagList("NpcMiscInv", 10), this.items);
    }

    public int getSizeInventory() {
        return this.size;
    }

    public ItemStack getStackInSlot(int index) {
        return (ItemStack)this.items.get(index);
    }

    public ItemStack decrStackSize(int index, int count) {
        return ItemStackHelper.getAndSplit(this.items, (int)index, (int)count);
    }

    public boolean decrStackSize(ItemStack eating, int decrease) {
        for (int slot = 0; slot < this.items.size(); ++slot) {
            ItemStack item = (ItemStack)this.items.get(slot);
            if (item.isEmpty() || eating != item || item.getCount() < decrease) continue;
            item.splitStack(decrease);
            if (item.getCount() <= 0) {
                this.items.set(slot, (Object)ItemStack.EMPTY);
            }
            return true;
        }
        return false;
    }

    public ItemStack removeStackFromSlot(int var1) {
        return (ItemStack)this.items.set(var1, (Object)ItemStack.EMPTY);
    }

    public void setInventorySlotContents(int var1, ItemStack var2) {
        if (var1 >= this.getSizeInventory()) {
            return;
        }
        this.items.set(var1, (Object)var2);
    }

    public int getInventoryStackLimit() {
        return this.stackLimit;
    }

    public boolean isUsableByPlayer(EntityPlayer var1) {
        return true;
    }

    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        return true;
    }

    public void markDirty() {
    }

    public boolean addItemStack(ItemStack item) {
        ItemStack mergable;
        boolean merged = false;
        while (!(mergable = this.getMergableItem(item)).isEmpty() && mergable.getCount() > 0) {
            int size = mergable.getMaxStackSize() - mergable.getCount();
            if (size > item.getCount()) {
                mergable.setCount(mergable.getMaxStackSize());
                item.setCount(item.getCount() - size);
                merged = true;
                continue;
            }
            mergable.setCount(mergable.getCount() + item.getCount());
            item.setCount(0);
        }
        if (item.getCount() <= 0) {
            return true;
        }
        int slot = this.firstFreeSlot();
        if (slot >= 0) {
            this.items.set(slot, (Object)item.copy());
            item.setCount(0);
            return true;
        }
        return merged;
    }

    public ItemStack getMergableItem(ItemStack item) {
        for (ItemStack is : this.items) {
            if (!NoppesUtilPlayer.compareItems(item, is, false, false) || is.getCount() >= is.getMaxStackSize()) continue;
            return is;
        }
        return ItemStack.EMPTY;
    }

    public int firstFreeSlot() {
        for (int i = 0; i < this.getSizeInventory(); ++i) {
            if (!((ItemStack)this.items.get(i)).isEmpty()) continue;
            return i;
        }
        return -1;
    }

    public void setSize(int i) {
        this.size = i;
    }

    public String getName() {
        return "Npc Misc Inventory";
    }

    public boolean hasCustomName() {
        return true;
    }

    public ITextComponent getDisplayName() {
        return null;
    }

    public void openInventory(EntityPlayer player) {
    }

    public void closeInventory(EntityPlayer player) {
    }

    public int getField(int id) {
        return 0;
    }

    public void setField(int id, int value) {
    }

    public int getFieldCount() {
        return 0;
    }

    public void clear() {
    }

    public boolean isEmpty() {
        for (int slot = 0; slot < this.getSizeInventory(); ++slot) {
            ItemStack item = this.getStackInSlot(slot);
            if (NoppesUtilServer.IsItemStackNull(item) || item.isEmpty()) continue;
            return false;
        }
        return true;
    }
}

