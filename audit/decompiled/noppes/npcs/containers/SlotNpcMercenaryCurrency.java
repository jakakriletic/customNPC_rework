/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 */
package noppes.npcs.containers;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import noppes.npcs.roles.RoleFollower;

class SlotNpcMercenaryCurrency
extends Slot {
    RoleFollower role;

    public SlotNpcMercenaryCurrency(RoleFollower role, IInventory inv, int i, int j, int k) {
        super(inv, i, j, k);
        this.role = role;
    }

    public int func_75219_a() {
        return 64;
    }

    public boolean func_75214_a(ItemStack itemstack) {
        Item item = itemstack.func_77973_b();
        for (ItemStack is : this.role.inventory.items) {
            if (item != is.func_77973_b() || itemstack.func_77981_g() && itemstack.func_77952_i() != is.func_77952_i()) continue;
            return true;
        }
        return false;
    }
}

