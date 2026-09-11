/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.item.EntityItem
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.ItemStackHelper
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.util.NonNullList
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentString
 *  net.minecraft.world.World
 */
package noppes.npcs.blocks.tiles;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.blocks.tiles.TileColorable;

public abstract class TileNpcContainer
extends TileColorable
implements IInventory {
    public final NonNullList<ItemStack> inventoryContents = NonNullList.withSize((int)this.getSizeInventory(), (Object)ItemStack.EMPTY);
    public String customName = "";
    public int playerUsing = 0;

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        NBTTagList nbttaglist = compound.getTagList("Items", 10);
        if (compound.hasKey("CustomName", 8)) {
            this.customName = compound.getString("CustomName");
        }
        this.inventoryContents.clear();
        for (int i = 0; i < nbttaglist.tagCount(); ++i) {
            NBTTagCompound nbttagcompound1 = nbttaglist.getCompoundTagAt(i);
            int j = nbttagcompound1.getByte("Slot") & 0xFF;
            if (j < 0 || j >= this.inventoryContents.size()) continue;
            this.inventoryContents.set(j, (Object)new ItemStack(nbttagcompound1));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagList nbttaglist = new NBTTagList();
        for (int i = 0; i < this.inventoryContents.size(); ++i) {
            if (((ItemStack)this.inventoryContents.get(i)).isEmpty()) continue;
            NBTTagCompound tagCompound = new NBTTagCompound();
            tagCompound.setByte("Slot", (byte)i);
            ((ItemStack)this.inventoryContents.get(i)).writeToNBT(tagCompound);
            nbttaglist.appendTag((NBTBase)tagCompound);
        }
        compound.setTag("Items", (NBTBase)nbttaglist);
        if (this.hasCustomName()) {
            compound.setString("CustomName", this.customName);
        }
        return super.writeToNBT(compound);
    }

    public boolean receiveClientEvent(int id, int type) {
        if (id == 1) {
            this.playerUsing = type;
            return true;
        }
        return super.receiveClientEvent(id, type);
    }

    public int getSizeInventory() {
        return 54;
    }

    public ItemStack getStackInSlot(int index) {
        return (ItemStack)this.inventoryContents.get(index);
    }

    public ItemStack decrStackSize(int index, int count) {
        ItemStack itemstack = ItemStackHelper.getAndSplit(this.inventoryContents, (int)index, (int)count);
        if (!itemstack.isEmpty()) {
            this.markDirty();
        }
        return itemstack;
    }

    public ItemStack removeStackFromSlot(int index) {
        return (ItemStack)this.inventoryContents.set(index, (Object)ItemStack.EMPTY);
    }

    public void setInventorySlotContents(int index, ItemStack stack) {
        this.inventoryContents.set(index, (Object)stack);
        if (stack.getCount() > this.getInventoryStackLimit()) {
            stack.setCount(this.getInventoryStackLimit());
        }
        this.markDirty();
    }

    public ITextComponent getDisplayName() {
        return new TextComponentString(this.hasCustomName() ? this.customName : this.getName());
    }

    public abstract String getName();

    public boolean hasCustomName() {
        return !this.customName.isEmpty();
    }

    public int getInventoryStackLimit() {
        return 64;
    }

    public boolean isUsableByPlayer(EntityPlayer player) {
        return (player.isDead || this.world.getTileEntity(this.pos) == this) && player.getDistanceSq((double)this.pos.getX() + 0.5, (double)this.pos.getY() + 0.5, (double)this.pos.getZ() + 0.5) <= 64.0;
    }

    public void openInventory(EntityPlayer player) {
        ++this.playerUsing;
    }

    public void closeInventory(EntityPlayer player) {
        --this.playerUsing;
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

    public boolean isItemValidForSlot(int var1, ItemStack var2) {
        return true;
    }

    public void dropItems(World world, BlockPos pos) {
        for (int i1 = 0; i1 < this.getSizeInventory(); ++i1) {
            ItemStack itemstack = this.getStackInSlot(i1);
            if (NoppesUtilServer.IsItemStackNull(itemstack)) continue;
            float f = world.rand.nextFloat() * 0.8f + 0.1f;
            float f1 = world.rand.nextFloat() * 0.8f + 0.1f;
            float f2 = world.rand.nextFloat() * 0.8f + 0.1f;
            while (itemstack.getCount() > 0) {
                int j1 = world.rand.nextInt(21) + 10;
                if (j1 > itemstack.getCount()) {
                    j1 = itemstack.getCount();
                }
                itemstack.setCount(itemstack.getCount() - j1);
                EntityItem entityitem = new EntityItem(world, (double)((float)pos.getX() + f), (double)((float)pos.getY() + f1), (double)((float)pos.getZ() + f2), new ItemStack(itemstack.getItem(), j1, itemstack.getItemDamage()));
                float f3 = 0.05f;
                entityitem.motionX = (float)world.rand.nextGaussian() * f3;
                entityitem.motionY = (float)world.rand.nextGaussian() * f3 + 0.2f;
                entityitem.motionZ = (float)world.rand.nextGaussian() * f3;
                if (itemstack.hasTagCompound()) {
                    entityitem.getItem().setTagCompound(itemstack.getTagCompound().copy());
                }
                world.spawnEntity((Entity)entityitem);
            }
        }
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

