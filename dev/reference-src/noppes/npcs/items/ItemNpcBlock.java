/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.item.ItemBlock
 */
package noppes.npcs.items;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;

public class ItemNpcBlock
extends ItemBlock {
    public ItemNpcBlock(Block block) {
        super(block);
        String name = block.getUnlocalizedName().substring(5);
        this.setRegistryName(name);
        this.setUnlocalizedName(name);
    }
}

