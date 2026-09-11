/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 */
package noppes.npcs.api.wrapper;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import noppes.npcs.api.item.IItemBlock;
import noppes.npcs.api.wrapper.ItemStackWrapper;

public class ItemBlockWrapper
extends ItemStackWrapper
implements IItemBlock {
    protected String blockName;

    protected ItemBlockWrapper(ItemStack item) {
        super(item);
        Block b = Block.getBlockFromItem((Item)item.getItem());
        this.blockName = Block.REGISTRY.getNameForObject((Object)b) + "";
    }

    @Override
    public int getType() {
        return 2;
    }

    @Override
    public String getBlockName() {
        return this.blockName;
    }
}

