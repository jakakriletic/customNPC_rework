/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.ItemStack
 */
package noppes.npcs.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import noppes.npcs.NoppesUtilServer;

class SlotNpcTraderItems
extends Slot {
    public SlotNpcTraderItems(IInventory iinventory, int i, int j, int k) {
        super(iinventory, i, j, k);
    }

    public ItemStack func_190901_a(EntityPlayer player, ItemStack itemstack) {
        if (NoppesUtilServer.IsItemStackNull(itemstack) || NoppesUtilServer.IsItemStackNull(this.func_75211_c())) {
            return itemstack;
        }
        if (itemstack.func_77973_b() != this.func_75211_c().func_77973_b()) {
            return itemstack;
        }
        itemstack.func_190918_g(1);
        return itemstack;
    }

    public int func_75219_a() {
        return 64;
    }

    public boolean func_75214_a(ItemStack itemstack) {
        return false;
    }
}

