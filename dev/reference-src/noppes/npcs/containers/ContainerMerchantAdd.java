/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.IMerchant
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.InventoryBasic
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.ItemStack
 *  net.minecraft.world.World
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 */
package noppes.npcs.containers;

import net.minecraft.entity.IMerchant;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.containers.ContainerNpcInterface;

public class ContainerMerchantAdd
extends ContainerNpcInterface {
    private IMerchant theMerchant;
    private InventoryBasic merchantInventory;
    private final World world;

    public ContainerMerchantAdd(EntityPlayer player, IMerchant par2IMerchant, World par3World) {
        super(player);
        int i;
        this.theMerchant = par2IMerchant;
        this.world = par3World;
        this.merchantInventory = new InventoryBasic("", false, 3);
        this.addSlotToContainer(new Slot((IInventory)this.merchantInventory, 0, 36, 53));
        this.addSlotToContainer(new Slot((IInventory)this.merchantInventory, 1, 62, 53));
        this.addSlotToContainer(new Slot((IInventory)this.merchantInventory, 2, 120, 53));
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlotToContainer(new Slot((IInventory)player.inventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (i = 0; i < 9; ++i) {
            this.addSlotToContainer(new Slot((IInventory)player.inventory, i, 8 + i * 18, 142));
        }
    }

    public void detectAndSendChanges() {
        super.detectAndSendChanges();
    }

    public void onCraftMatrixChanged(IInventory par1IInventory) {
        super.onCraftMatrixChanged(par1IInventory);
    }

    public void setCurrentRecipeIndex(int par1) {
    }

    @SideOnly(value=Side.CLIENT)
    public void updateProgressBar(int par1, int par2) {
    }

    public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int par2) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = (Slot)this.inventorySlots.get(par2);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (par2 != 0 && par2 != 1 && par2 != 2 ? (par2 >= 3 && par2 < 30 ? !this.mergeItemStack(itemstack1, 30, 39, false) : par2 >= 30 && par2 < 39 && !this.mergeItemStack(itemstack1, 3, 30, false)) : !this.mergeItemStack(itemstack1, 3, 39, false)) {
                return ItemStack.EMPTY;
            }
            if (itemstack1.getCount() == 0) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(par1EntityPlayer, itemstack1);
        }
        return itemstack;
    }

    public void onContainerClosed(EntityPlayer par1EntityPlayer) {
        super.onContainerClosed(par1EntityPlayer);
        this.theMerchant.setCustomer((EntityPlayer)null);
        super.onContainerClosed(par1EntityPlayer);
        if (!this.world.isRemote) {
            ItemStack itemstack = this.merchantInventory.removeStackFromSlot(0);
            if (!NoppesUtilServer.IsItemStackNull(itemstack)) {
                par1EntityPlayer.dropItem(itemstack, false);
            }
            if (!NoppesUtilServer.IsItemStackNull(itemstack = this.merchantInventory.removeStackFromSlot(1))) {
                par1EntityPlayer.dropItem(itemstack, false);
            }
        }
    }
}

