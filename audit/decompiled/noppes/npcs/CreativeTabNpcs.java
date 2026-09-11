/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.creativetab.CreativeTabs
 *  net.minecraft.init.Items
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 */
package noppes.npcs;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class CreativeTabNpcs
extends CreativeTabs {
    public Item item = Items.field_151054_z;
    public int meta = 0;

    public CreativeTabNpcs(String label) {
        super(label);
    }

    public ItemStack func_78016_d() {
        return new ItemStack(this.item, 1, this.meta);
    }
}

