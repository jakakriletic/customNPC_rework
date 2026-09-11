/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.IInventory
 */
package noppes.npcs.api;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import noppes.npcs.api.item.IItemStack;

public interface IContainer {
    public int getSize();

    public IItemStack getSlot(int var1);

    public void setSlot(int var1, IItemStack var2);

    public IInventory getMCInventory();

    public Container getMCContainer();

    public int count(IItemStack var1, boolean var2, boolean var3);

    public IItemStack[] getItems();
}

