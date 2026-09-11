/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.ClickType
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.ItemStack
 */
package noppes.npcs.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import noppes.npcs.EventHooks;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.event.RoleEvent;
import noppes.npcs.containers.ContainerNpcInterface;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleTrader;

public class ContainerNPCTrader
extends ContainerNpcInterface {
    public RoleTrader role;
    private EntityNPCInterface npc;

    public ContainerNPCTrader(EntityNPCInterface npc, EntityPlayer player) {
        super(player);
        this.npc = npc;
        this.role = (RoleTrader)npc.roleInterface;
        for (int i = 0; i < 18; ++i) {
            int x = 53;
            x += i % 3 * 72;
            int y = 7;
            y += i / 3 * 21;
            ItemStack item = (ItemStack)this.role.inventoryCurrency.items.get(i);
            ItemStack item2 = (ItemStack)this.role.inventoryCurrency.items.get(i + 18);
            if (item == null) {
                item = item2;
                item2 = null;
            }
            this.addSlotToContainer(new Slot((IInventory)this.role.inventorySold, i, x, y));
        }
        for (int i1 = 0; i1 < 3; ++i1) {
            for (int l1 = 0; l1 < 9; ++l1) {
                this.addSlotToContainer(new Slot((IInventory)player.inventory, l1 + i1 * 9 + 9, 32 + l1 * 18, 140 + i1 * 18));
            }
        }
        for (int j1 = 0; j1 < 9; ++j1) {
            this.addSlotToContainer(new Slot((IInventory)player.inventory, j1, 32 + j1 * 18, 198));
        }
    }

    public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int i) {
        return ItemStack.EMPTY;
    }

    public ItemStack slotClick(int i, int j, ClickType par3, EntityPlayer entityplayer) {
        ItemStack currency2;
        if (par3 != ClickType.PICKUP) {
            return ItemStack.EMPTY;
        }
        if (i < 0 || i >= 18) {
            return super.slotClick(i, j, par3, entityplayer);
        }
        if (j == 1) {
            return ItemStack.EMPTY;
        }
        Slot slot = (Slot)this.inventorySlots.get(i);
        if (slot == null || slot.getStack() == null) {
            return ItemStack.EMPTY;
        }
        ItemStack item = slot.getStack();
        if (!this.canGivePlayer(item, entityplayer)) {
            return ItemStack.EMPTY;
        }
        ItemStack currency = this.role.inventoryCurrency.getStackInSlot(i);
        if (!this.canBuy(currency, currency2 = this.role.inventoryCurrency.getStackInSlot(i + 18), entityplayer)) {
            RoleEvent.TradeFailedEvent event = new RoleEvent.TradeFailedEvent(entityplayer, this.npc.wrappedNPC, item, currency, currency2);
            EventHooks.onNPCRole(this.npc, event);
            return event.receiving == null ? ItemStack.EMPTY : event.receiving.getMCItemStack();
        }
        RoleEvent.TraderEvent event = new RoleEvent.TraderEvent(entityplayer, this.npc.wrappedNPC, item, currency, currency2);
        if (EventHooks.onNPCRole(this.npc, event)) {
            return ItemStack.EMPTY;
        }
        if (event.currency1 != null) {
            currency = event.currency1.getMCItemStack();
        }
        if (event.currency2 != null) {
            currency2 = event.currency2.getMCItemStack();
        }
        if (!this.canBuy(currency, currency2, entityplayer)) {
            return ItemStack.EMPTY;
        }
        NoppesUtilPlayer.consumeItem(entityplayer, currency, this.role.ignoreDamage, this.role.ignoreNBT);
        NoppesUtilPlayer.consumeItem(entityplayer, currency2, this.role.ignoreDamage, this.role.ignoreNBT);
        ItemStack soldItem = ItemStack.EMPTY;
        if (event.sold != null) {
            soldItem = event.sold.getMCItemStack();
            this.givePlayer(soldItem.copy(), entityplayer);
        }
        return soldItem;
    }

    public boolean canBuy(ItemStack currency, ItemStack currency2, EntityPlayer player) {
        if (NoppesUtilServer.IsItemStackNull(currency) && NoppesUtilServer.IsItemStackNull(currency2)) {
            return true;
        }
        if (NoppesUtilServer.IsItemStackNull(currency)) {
            currency = currency2;
            currency2 = ItemStack.EMPTY;
        }
        if (NoppesUtilPlayer.compareItems(currency, currency2, this.role.ignoreDamage, this.role.ignoreNBT)) {
            currency = currency.copy();
            currency.grow(currency2.getCount());
            currency2 = ItemStack.EMPTY;
        }
        if (NoppesUtilServer.IsItemStackNull(currency2)) {
            return NoppesUtilPlayer.compareItems(player, currency, this.role.ignoreDamage, this.role.ignoreNBT);
        }
        return NoppesUtilPlayer.compareItems(player, currency, this.role.ignoreDamage, this.role.ignoreNBT) && NoppesUtilPlayer.compareItems(player, currency2, this.role.ignoreDamage, this.role.ignoreNBT);
    }

    private boolean canGivePlayer(ItemStack item, EntityPlayer entityplayer) {
        int k1;
        ItemStack itemstack3 = entityplayer.inventory.getItemStack();
        if (NoppesUtilServer.IsItemStackNull(itemstack3)) {
            return true;
        }
        return NoppesUtilPlayer.compareItems(itemstack3, item, false, false) && (k1 = item.getCount()) > 0 && k1 + itemstack3.getCount() <= itemstack3.getMaxStackSize();
    }

    private void givePlayer(ItemStack item, EntityPlayer entityplayer) {
        int k1;
        ItemStack itemstack3 = entityplayer.inventory.getItemStack();
        if (NoppesUtilServer.IsItemStackNull(itemstack3)) {
            entityplayer.inventory.setItemStack(item);
        } else if (NoppesUtilPlayer.compareItems(itemstack3, item, false, false) && (k1 = item.getCount()) > 0 && k1 + itemstack3.getCount() <= itemstack3.getMaxStackSize()) {
            itemstack3.grow(k1);
        }
    }
}

