/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.inventory.ClickType
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.InventoryBasic
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.ItemStack
 */
package noppes.npcs.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import noppes.npcs.EventHooks;
import noppes.npcs.api.IContainer;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.event.CustomContainerEvent;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.containers.ContainerNpcInterface;
import noppes.npcs.controllers.data.PlayerData;

public class ContainerCustomChest
extends ContainerNpcInterface {
    private InventoryBasic craftingMatrix;
    private IContainer container;
    public final int rows;

    public ContainerCustomChest(EntityPlayer player, int rows) {
        super(player);
        this.rows = rows;
        if (!player.field_70170_p.field_72995_K) {
            this.container = NpcAPI.Instance().getIContainer(this);
        }
        this.craftingMatrix = new InventoryBasic("crafting", false, rows * 9);
        for (int j1 = 0; j1 < 9; ++j1) {
            this.func_75146_a(new Slot((IInventory)player.field_71071_by, j1, j1 * 18 + 8, 89 + rows * 18));
        }
        for (int i1 = 0; i1 < 3; ++i1) {
            for (int l1 = 0; l1 < 9; ++l1) {
                this.func_75146_a(new Slot((IInventory)player.field_71071_by, l1 + i1 * 9 + 9, l1 * 18 + 8, 31 + rows * 18 + i1 * 18));
            }
        }
        for (int j = 0; j < rows; ++j) {
            for (int k = 0; k < 9; ++k) {
                this.func_75146_a(new Slot((IInventory)this.craftingMatrix, k + j * 9, 8 + k * 18, 18 + j * 18));
            }
        }
    }

    @Override
    public boolean func_75145_c(EntityPlayer playerIn) {
        return true;
    }

    public ItemStack func_184996_a(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        if (clickType == ClickType.QUICK_MOVE) {
            return ItemStack.field_190927_a;
        }
        if (slotId < 36) {
            return super.func_184996_a(slotId, dragType, clickType, player);
        }
        if (clickType != ClickType.PICKUP || dragType != 0 || !(player instanceof EntityPlayerMP) || this.container == null) {
            return ItemStack.field_190927_a;
        }
        Slot slot = (Slot)this.field_75151_b.get(slotId);
        if (slot == null) {
            return ItemStack.field_190927_a;
        }
        PlayerData data = PlayerData.get(player);
        IItemStack item = NpcAPI.Instance().getIItemStack(slot.func_75211_c());
        IItemStack heldItem = NpcAPI.Instance().getIItemStack(player.field_71071_by.func_70445_o());
        CustomContainerEvent.SlotClickedEvent event = new CustomContainerEvent.SlotClickedEvent(data.scriptData.getPlayer(), this.container, slotId, item, heldItem);
        EventHooks.onCustomChestClicked(event);
        player.field_71071_by.func_70437_b(event.heldItem == null ? ItemStack.field_190927_a : event.heldItem.getMCItemStack());
        ((EntityPlayerMP)player).func_71113_k();
        this.func_75141_a(slotId, event.slotItem == null ? ItemStack.field_190927_a : event.slotItem.getMCItemStack());
        this.func_75142_b();
        return ItemStack.field_190927_a;
    }

    public boolean func_94530_a(ItemStack stack, Slot slotId) {
        return slotId.field_75224_c == this.player.field_71071_by;
    }

    public void func_75134_a(EntityPlayer player) {
        super.func_75134_a(player);
        if (!player.field_70170_p.field_72995_K) {
            PlayerData data = PlayerData.get(player);
            CustomContainerEvent.CloseEvent event = new CustomContainerEvent.CloseEvent(data.scriptData.getPlayer(), this.container);
            EventHooks.onCustomChestClosed(event);
        }
    }
}

