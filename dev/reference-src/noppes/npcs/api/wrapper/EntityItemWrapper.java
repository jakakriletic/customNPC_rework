/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.item.EntityItem
 *  net.minecraft.item.ItemStack
 */
package noppes.npcs.api.wrapper;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntityItem;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.EntityWrapper;

public class EntityItemWrapper<T extends EntityItem>
extends EntityWrapper<T>
implements IEntityItem {
    public EntityItemWrapper(T entity) {
        super(entity);
    }

    @Override
    public String getOwner() {
        return ((EntityItem)this.entity).getOwner();
    }

    @Override
    public void setOwner(String name) {
        ((EntityItem)this.entity).setOwner(name);
    }

    @Override
    public int getPickupDelay() {
        return ((EntityItem)this.entity).pickupDelay;
    }

    @Override
    public void setPickupDelay(int delay) {
        ((EntityItem)this.entity).setPickupDelay(delay);
    }

    @Override
    public int getType() {
        return 6;
    }

    @Override
    public long getAge() {
        return ((EntityItem)this.entity).age;
    }

    @Override
    public void setAge(long age) {
        age = Math.max(Math.min(age, Integer.MAX_VALUE), Integer.MIN_VALUE);
        ((EntityItem)this.entity).age = (int)age;
    }

    @Override
    public int getLifeSpawn() {
        return ((EntityItem)this.entity).lifespan;
    }

    @Override
    public void setLifeSpawn(int age) {
        ((EntityItem)this.entity).lifespan = age;
    }

    @Override
    public IItemStack getItem() {
        return NpcAPI.Instance().getIItemStack(((EntityItem)this.entity).getItem());
    }

    @Override
    public void setItem(IItemStack item) {
        ItemStack stack = item == null ? ItemStack.EMPTY : item.getMCItemStack();
        ((EntityItem)this.entity).setItem(stack);
    }
}

