/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.BlockCrops
 *  net.minecraft.item.Item
 */
package net.minecraft.block;

import net.minecraft.block.BlockCrops;
import net.minecraft.item.Item;

public final class NpcBlockHelper {
    public static Item getCrop(BlockCrops crops) {
        return crops.getCrop();
    }
}

