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
            this.func_75146_a(new Slot((IInventory)this.bank.currencyInventory, i, x, y += i * 22));
        }
        for (i = 0; i < 6; ++i) {
            x = 142;
            y = 38;
            this.func_75146_a(new Slot((IInventory)this.bank.upgradeInventory, i, x, y += i * 22));
        }
        for (int j1 = 0; j1 < 9; ++j1) {
            this.func_75146_a(new Slot((IInventory)player.field_71071_by, j1, 8 + j1 * 18, 171));
        }
    }

    public ItemStack func_82846_b(EntityPlayer par1EntityPlayer, int i) {
        return ItemStack.field_190927_a;
    }

    public boolean func_75145_c(EntityPlayer entityplayer) {
        return true;
    }

    public void setBank(Bank bank2) {
        for (int i = 0; i < 6; ++i) {
            this.bank.currencyInventory.func_70299_a(i, bank2.currencyInventory.func_70301_a(i));
            this.bank.upgradeInventory.func_70299_a(i, bank2.upgradeInventory.func_70301_a(i));
        }
    }
}

