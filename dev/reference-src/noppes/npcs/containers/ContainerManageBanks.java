/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.ItemStack
 */
package noppes.npcs.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import noppes.npcs.controllers.data.Bank;

public class ContainerManageBanks
extends Container {
    public Bank bank = new Bank();

    public ContainerManageBanks(EntityPlayer player) {
        int y;
        int x;
        int i;
        for (i = 0; i < 6; ++i) {
            x = 36;
            y = 38;
            this.addSlotToContainer(new Slot((IInventory)this.bank.currencyInventory, i, x, y += i * 22));
        }
        for (i = 0; i < 6; ++i) {
            x = 142;
            y = 38;
            this.addSlotToContainer(new Slot((IInventory)this.bank.upgradeInventory, i, x, y += i * 22));
        }
        for (int j1 = 0; j1 < 9; ++j1) {
            this.addSlotToContainer(new Slot((IInventory)player.inventory, j1, 8 + j1 * 18, 171));
        }
    }

    public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int i) {
        return ItemStack.EMPTY;
    }

    public boolean canInteractWith(EntityPlayer entityplayer) {
        return true;
    }

    public void setBank(Bank bank2) {
        for (int i = 0; i < 6; ++i) {
            this.bank.currencyInventory.setInventorySlotContents(i, bank2.currencyInventory.getStackInSlot(i));
            this.bank.upgradeInventory.setInventorySlotContents(i, bank2.upgradeInventory.getStackInSlot(i));
        }
    }
}

