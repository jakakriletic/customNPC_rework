/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.ItemStack
 */
package noppes.npcs;

import net.minecraft.item.ItemStack;
import noppes.npcs.api.entity.data.IData;
import noppes.npcs.api.wrapper.ItemStackWrapper;

public class ItemStackEmptyWrapper
extends ItemStackWrapper {
    public ItemStackEmptyWrapper() {
        super(ItemStack.EMPTY);
    }

    @Override
    public IData getTempdata() {
        return null;
    }

    @Override
    public IData getStoreddata() {
        return null;
    }
}

