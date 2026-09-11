/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.item.EntityItem
 */
package noppes.npcs.api.entity;

import net.minecraft.entity.item.EntityItem;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.item.IItemStack;

public interface IEntityItem<T extends EntityItem>
extends IEntity<T> {
    public String getOwner();

    public void setOwner(String var1);

    public int getPickupDelay();

    public void setPickupDelay(int var1);

    @Override
    public long getAge();

    public void setAge(long var1);

    public int getLifeSpawn();

    public void setLifeSpawn(int var1);

    public IItemStack getItem();

    public void setItem(IItemStack var1);
}

