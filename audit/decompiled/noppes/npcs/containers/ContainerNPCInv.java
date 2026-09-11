/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.EntityEquipmentSlot
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.ItemStack
 */
package noppes.npcs.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import noppes.npcs.containers.SlotNPCArmor;
import noppes.npcs.entity.EntityNPCInterface;

public class ContainerNPCInv
extends Container {
    public ContainerNPCInv(EntityNPCInterface npc, EntityPlayer player) {
        this.func_75146_a(new SlotNPCArmor(npc.inventory, 0, 9, 22, EntityEquipmentSlot.HEAD));
        this.func_75146_a(new SlotNPCArmor(npc.inventory, 1, 9, 40, EntityEquipmentSlot.CHEST));
        this.func_75146_a(new SlotNPCArmor(npc.inventory, 2, 9, 58, EntityEquipmentSlot.LEGS));
        this.func_75146_a(new SlotNPCArmor(npc.inventory, 3, 9, 76, EntityEquipmentSlot.FEET));
        this.func_75146_a(new Slot((IInventory)npc.inventory, 4, 81, 22));
        this.func_75146_a(new Slot((IInventory)npc.inventory, 5, 81, 40));
        this.func_75146_a(new Slot((IInventory)npc.inventory, 6, 81, 58));
        for (int l = 0; l < 9; ++l) {
            this.func_75146_a(new Slot((IInventory)npc.inventory, l + 7, 191, 16 + l * 21));
        }
        for (int i1 = 0; i1 < 3; ++i1) {
            for (int l1 = 0; l1 < 9; ++l1) {
                this.func_75146_a(new Slot((IInventory)player.field_71071_by, l1 + i1 * 9 + 9, l1 * 18 + 8, 113 + i1 * 18));
            }
        }
        for (int j1 = 0; j1 < 9; ++j1) {
            this.func_75146_a(new Slot((IInventory)player.field_71071_by, j1, j1 * 18 + 8, 171));
        }
    }

    public ItemStack func_82846_b(EntityPlayer par1EntityPlayer, int i) {
        return ItemStack.field_190927_a;
    }

    public boolean func_75145_c(EntityPlayer entityplayer) {
        return true;
    }
}

