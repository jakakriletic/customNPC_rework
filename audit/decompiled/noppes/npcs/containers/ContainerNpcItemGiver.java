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
import noppes.npcs.roles.JobItemGiver;

public class ContainerNpcItemGiver
extends Container {
    private JobItemGiver role;

    public ContainerNpcItemGiver(EntityNPCInterface npc, EntityPlayer player) {
        int j1;
        this.role = (JobItemGiver)npc.jobInterface;
        for (j1 = 0; j1 < 9; ++j1) {
            this.func_75146_a(new Slot((IInventory)this.role.inventory, j1, 6 + j1 * 18, 90));
        }
        for (int i1 = 0; i1 < 3; ++i1) {
            for (int l1 = 0; l1 < 9; ++l1) {
                this.func_75146_a(new Slot((IInventory)player.field_71071_by, l1 + i1 * 9 + 9, 6 + l1 * 18, 116 + i1 * 18));
            }
        }
        for (j1 = 0; j1 < 9; ++j1) {
            this.func_75146_a(new Slot((IInventory)player.field_71071_by, j1, 6 + j1 * 18, 174));
        }
    }

    public ItemStack func_82846_b(EntityPlayer par1EntityPlayer, int i) {
        return ItemStack.field_190927_a;
    }

    public boolean func_75145_c(EntityPlayer entityplayer) {
        return true;
    }
}

