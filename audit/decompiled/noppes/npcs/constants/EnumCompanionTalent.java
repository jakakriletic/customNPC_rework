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
    INVENTORY(Item.func_150898_a((Block)Blocks.field_150462_ai)),
    ARMOR((Item)Items.field_151030_Z),
    SWORD(Items.field_151048_u),
    RANGED((Item)Items.field_151031_f),
    ACROBATS((Item)Items.field_151021_T),
    INTEL(Items.field_151122_aG);

    public ItemStack item;

    private EnumCompanionTalent(Item item) {
        this.item = new ItemStack(item);
    }
}

