/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.init.Blocks
 *  net.minecraft.init.Items
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 */
package noppes.npcs.constants;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public enum EnumCompanionTalent {
    INVENTORY(Item.getItemFromBlock((Block)Blocks.CRAFTING_TABLE)),
    ARMOR((Item)Items.IRON_CHESTPLATE),
    SWORD(Items.DIAMOND_SWORD),
    RANGED((Item)Items.BOW),
    ACROBATS((Item)Items.LEATHER_BOOTS),
    INTEL(Items.BOOK);

    public ItemStack item;

    private EnumCompanionTalent(Item item) {
        this.item = new ItemStack(item);
    }
}

