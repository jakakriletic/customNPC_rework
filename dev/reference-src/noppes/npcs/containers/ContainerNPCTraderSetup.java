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
import noppes.npcs.roles.RoleTrader;

public class ContainerNPCTraderSetup
extends Container {
    public RoleTrader role;

    public ContainerNPCTraderSetup(EntityNPCInterface npc, EntityPlayer player) {
        this.role = (RoleTrader)npc.roleInterface;
        for (int i = 0; i < 18; ++i) {
            int x = 7;
            int y = 15;
            this.addSlotToContainer(new Slot((IInventory)this.role.inventoryCurrency, i + 18, x += i % 3 * 94, y += i / 3 * 22));
            this.addSlotToContainer(new Slot((IInventory)this.role.inventoryCurrency, i, x + 18, y));
            this.addSlotToContainer(new Slot((IInventory)this.role.inventorySold, i, x + 43, y));
        }
        for (int i1 = 0; i1 < 3; ++i1) {
            for (int l1 = 0; l1 < 9; ++l1) {
                this.addSlotToContainer(new Slot((IInventory)player.inventory, l1 + i1 * 9 + 9, 48 + l1 * 18, 147 + i1 * 18));
            }
        }
        for (int j1 = 0; j1 < 9; ++j1) {
            this.addSlotToContainer(new Slot((IInventory)player.inventory, j1, 48 + j1 * 18, 205));
        }
    }

    public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int i) {
        return ItemStack.EMPTY;
    }

    public boolean canInteractWith(EntityPlayer entityplayer) {
        return true;
    }
}

