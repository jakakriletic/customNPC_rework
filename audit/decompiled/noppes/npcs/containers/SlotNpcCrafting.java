/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.InventoryCrafting
 *  net.minecraft.inventory.SlotCrafting
 *  net.minecraft.item.ItemStack
 *  net.minecraftforge.fml.common.FMLCommonHandler
 */
package noppes.npcs.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import noppes.npcs.NoppesUtilServer;

public class SlotNpcCrafting
extends SlotCrafting {
    private final InventoryCrafting craftMatrix;

    public SlotNpcCrafting(EntityPlayer player, InventoryCrafting craftingInventory, IInventory inventory, int slotIndex, int x, int y) {
        super(player, craftingInventory, inventory, slotIndex, x, y);
        this.craftMatrix = craftingInventory;
    }

    public ItemStack func_190901_a(EntityPlayer player, ItemStack itemStack) {
        FMLCommonHandler.instance().firePlayerCraftingEvent(player, itemStack, (IInventory)this.craftMatrix);
        this.func_75208_c(itemStack);
        for (int i = 0; i < this.craftMatrix.func_70302_i_(); ++i) {
            ItemStack itemstack2;
            ItemStack itemstack1 = this.craftMatrix.func_70301_a(i);
            if (NoppesUtilServer.IsItemStackNull(itemstack1)) continue;
            this.craftMatrix.func_70298_a(i, 1);
            if (!itemstack1.func_77973_b().hasContainerItem(itemstack1) || !NoppesUtilServer.IsItemStackNull(itemstack2 = itemstack1.func_77973_b().getContainerItem(itemstack1)) && itemstack2.func_77984_f() && itemstack2.func_77952_i() > itemstack2.func_77958_k() || player.field_71071_by.func_70441_a(itemstack2)) continue;
            if (NoppesUtilServer.IsItemStackNull(this.craftMatrix.func_70301_a(i))) {
                this.craftMatrix.func_70299_a(i, itemstack2);
                continue;
            }
            player.func_71019_a(itemstack2, false);
        }
        return itemStack;
    }
}

