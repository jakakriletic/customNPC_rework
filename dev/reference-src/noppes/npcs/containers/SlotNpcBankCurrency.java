/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.ItemStack
 */
package noppes.npcs.containers;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.containers.ContainerNPCBankInterface;

public class SlotNpcBankCurrency
extends Slot {
    public ItemStack item = ItemStack.EMPTY;

    public SlotNpcBankCurrency(ContainerNPCBankInterface containerplayer, IInventory iinventory, int i, int j, int k) {
        super(iinventory, i, j, k);
    }

    public int getSlotStackLimit() {
        return 64;
    }

    public boolean isItemValid(ItemStack itemstack) {
        if (NoppesUtilServer.IsItemStackNull(itemstack)) {
            return false;
        }
        return this.item.getItem() == itemstack.getItem() && (!this.item.getHasSubtypes() || this.item.getItemDamage() == itemstack.getItemDamage());
    }
}

