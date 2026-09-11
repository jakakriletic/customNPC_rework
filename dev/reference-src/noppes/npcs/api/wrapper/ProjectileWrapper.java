/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.math.MathHelper
 */
package noppes.npcs.api.wrapper;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IProjectile;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.ThrowableWrapper;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.entity.EntityProjectile;

public class ProjectileWrapper<T extends EntityProjectile>
extends ThrowableWrapper<T>
implements IProjectile {
    public ProjectileWrapper(T entity) {
        super(entity);
    }

    @Override
    public IItemStack getItem() {
        return NpcAPI.Instance().getIItemStack(((EntityProjectile)this.entity).getItemDisplay());
    }

    @Override
    public void setItem(IItemStack item) {
        if (item == null) {
            ((EntityProjectile)this.entity).setThrownItem(ItemStack.EMPTY);
        } else {
            ((EntityProjectile)this.entity).setThrownItem(item.getMCItemStack());
        }
    }

    @Override
    public boolean getHasGravity() {
        return ((EntityProjectile)this.entity).hasGravity();
    }

    @Override
    public void setHasGravity(boolean bo) {
        ((EntityProjectile)this.entity).setHasGravity(bo);
    }

    @Override
    public int getAccuracy() {
        return ((EntityProjectile)this.entity).accuracy;
    }

    @Override
    public void setAccuracy(int accuracy) {
        ((EntityProjectile)this.entity).accuracy = accuracy;
    }

    @Override
    public void setHeading(IEntity entity) {
        this.setHeading(entity.getX(), entity.getMCEntity().getEntityBoundingBox().minY + (double)(entity.getHeight() / 2.0f), entity.getZ());
    }

    @Override
    public void setHeading(double x, double y, double z) {
        float varF = ((EntityProjectile)this.entity).hasGravity() ? MathHelper.sqrt((double)((x -= ((EntityProjectile)this.entity).posX) * x + (z -= ((EntityProjectile)this.entity).posZ) * z)) : 0.0f;
        float angle = ((EntityProjectile)this.entity).getAngleForXYZ(x, y -= ((EntityProjectile)this.entity).posY, z, varF, false);
        float acc = 20.0f - (float)MathHelper.floor((float)((float)((EntityProjectile)this.entity).accuracy / 5.0f));
        ((EntityProjectile)this.entity).shoot(x, y, z, angle, acc);
    }

    @Override
    public void setHeading(float yaw, float pitch) {
        ((EntityProjectile)this.entity).prevRotationYaw = ((EntityProjectile)this.entity).rotationYaw = yaw;
        ((EntityProjectile)this.entity).prevRotationPitch = ((EntityProjectile)this.entity).rotationPitch = pitch;
        double varX = -MathHelper.sin((float)(yaw / 180.0f * (float)Math.PI)) * MathHelper.cos((float)(pitch / 180.0f * (float)Math.PI));
        double varZ = MathHelper.cos((float)(yaw / 180.0f * (float)Math.PI)) * MathHelper.cos((float)(pitch / 180.0f * (float)Math.PI));
        double varY = -MathHelper.sin((float)(pitch / 180.0f * (float)Math.PI));
        float acc = 20.0f - (float)MathHelper.floor((float)((float)((EntityProjectile)this.entity).accuracy / 5.0f));
        ((EntityProjectile)this.entity).shoot(varX, varY, varZ, -pitch, acc);
    }

    @Override
    public int getType() {
        return 7;
    }

    @Override
    public boolean typeOf(int type) {
        return type == 7 ? true : super.typeOf(type);
    }

    @Override
    public void enableEvents() {
        if (ScriptContainer.Current == null) {
            throw new CustomNPCsException("Can only be called during scripts", new Object[0]);
        }
        if (!((EntityProjectile)this.entity).scripts.contains(ScriptContainer.Current)) {
            ((EntityProjectile)this.entity).scripts.add(ScriptContainer.Current);
        }
    }
}

