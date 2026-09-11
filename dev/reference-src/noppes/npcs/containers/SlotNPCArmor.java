/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.inventory.EntityEquipmentSlot
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.ItemArmor
 *  net.minecraft.item.ItemBlock
 *  net.minecraft.item.ItemStack
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 */
package noppes.npcs.containers;

import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

class SlotNPCArmor
extends Slot {
    final EntityEquipmentSlot armorType;

    SlotNPCArmor(IInventory iinventory, int i, int j, int k, EntityEquipmentSlot l) {
        super(iinventory, i, j, k);
        this.armorType = l;
    }

    public int getSlotStackLimit() {
        return 1;
    }

    @SideOnly(value=Side.CLIENT)
    public String getSlotTexture() {
        return ItemArmor.EMPTY_SLOT_NAMES[this.armorType.getIndex()];
    }

    public boolean isItemValid(ItemStack itemstack) {
        if (itemstack.getItem() instanceof ItemArmor) {
            return ((ItemArmor)itemstack.getItem()).armorType == this.armorType;
        }
        if (itemstack.getItem() instanceof ItemBlock) {
            return this.armorType == EntityEquipmentSlot.HEAD;
        }
        return false;
    }
}

