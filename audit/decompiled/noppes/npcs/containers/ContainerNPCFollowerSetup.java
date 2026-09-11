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
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleFollower;

public class ContainerNPCFollowerSetup
extends Container {
    private RoleFollower role;

    public ContainerNPCFollowerSetup(EntityNPCInterface npc, EntityPlayer player) {
        int i1;
        this.role = (RoleFollower)npc.roleInterface;
        for (i1 = 0; i1 < 3; ++i1) {
            this.func_75146_a(new Slot((IInventory)this.role.inventory, i1, 44, 39 + i1 * 25));
        }
        for (i1 = 0; i1 < 3; ++i1) {
            for (int l1 = 0; l1 < 9; ++l1) {
                this.func_75146_a(new Slot((IInventory)player.field_71071_by, l1 + i1 * 9 + 9, 8 + l1 * 18, 113 + i1 * 18));
            }
        }
        for (int j1 = 0; j1 < 9; ++j1) {
            this.func_75146_a(new Slot((IInventory)player.field_71071_by, j1, 8 + j1 * 18, 171));
        }
    }

    public ItemStack func_82846_b(EntityPlayer par1EntityPlayer, int i) {
        ItemStack itemstack = ItemStack.field_190927_a;
        Slot slot = (Slot)this.field_75151_b.get(i);
        if (slot != null && slot.func_75216_d()) {
            ItemStack itemstack1 = slot.func_75211_c();
            itemstack = itemstack1.func_77946_l();
            if (i >= 0 && i < 3 ? !this.func_75135_a(itemstack1, 3, 38, true) : (i >= 3 && i < 30 ? !this.func_75135_a(itemstack1, 30, 38, false) : (i >= 30 && i < 38 ? !this.func_75135_a(itemstack1, 3, 29, false) : !this.func_75135_a(itemstack1, 3, 38, false)))) {
                return ItemStack.field_190927_a;
            }
            if (itemstack1.func_190916_E() == 0) {
                slot.func_75215_d(ItemStack.field_190927_a);
            } else {
                slot.func_75218_e();
            }
            if (itemstack1.func_190916_E() != itemstack.func_190916_E()) {
                slot.func_190901_a(par1EntityPlayer, itemstack1);
            } else {
                return ItemStack.field_190927_a;
            }
        }
        return itemstack;
    }

    public boolean func_75145_c(EntityPlayer entityplayer) {
        return true;
    }
}

