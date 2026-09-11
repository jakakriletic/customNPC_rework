/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.EntityLiving
 */
package noppes.npcs.api.entity;

import net.minecraft.entity.EntityLiving;
import noppes.npcs.api.IPos;
import noppes.npcs.api.entity.IEntityLivingBase;

public interface IEntityLiving<T extends EntityLiving>
extends IEntityLivingBase<T> {
    public boolean isNavigating();

    public void clearNavigation();

    public void navigateTo(double var1, double var3, double var5, double var7);

    public void jump();

    @Override
    public T getMCEntity();

    public IPos getNavigationPath();
}

