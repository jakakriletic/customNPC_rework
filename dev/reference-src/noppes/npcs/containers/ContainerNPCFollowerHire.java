/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.InventoryBasic
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.ItemStack
 */
package noppes.npcs.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.containers.ContainerNpcInterface;
import noppes.npcs.containers.SlotNpcMercenaryCurrency;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleFollower;

public class ContainerNPCFollowerHire
extends ContainerNpcInterface {
    public InventoryBasic currencyMatrix;
    public RoleFollower role;

    public ContainerNPCFollowerHire(EntityNPCInterface npc, EntityPlayer player) {
        super(player);
        this.role = (RoleFollower)npc.roleInterface;
        this.currencyMatrix = new InventoryBasic("currency", false, 1);
        this.addSlotToContainer(new SlotNpcMercenaryCurrency(this.role, (IInventory)this.currencyMatrix, 0, 44, 35));
        for (int i1 = 0; i1 < 3; ++i1) {
            for (int l1 = 0; l1 < 9; ++l1) {
                this.addSlotToContainer(new Slot((IInventory)player.inventory, l1 + i1 * 9 + 9, 8 + l1 * 18, 84 + i1 * 18));
            }
        }
        for (int j1 = 0; j1 < 9; ++j1) {
            this.addSlotToContainer(new Slot((IInventory)player.inventory, j1, 8 + j1 * 18, 142));
        }
    }

    public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int i) {
        return ItemStack.EMPTY;
    }

    public void onContainerClosed(EntityPlayer entityplayer) {
        ItemStack itemstack;
        super.onContainerClosed(entityplayer);
        if (!(entityplayer.world.isRemote || NoppesUtilServer.IsItemStackNull(itemstack = this.currencyMatrix.removeStackFromSlot(0)) || entityplayer.world.isRemote)) {
            entityplayer.entityDropItem(itemstack, 0.0f);
        }
    }
}

