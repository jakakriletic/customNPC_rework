/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLiving
 *  net.minecraft.pathfinding.PathPoint
 *  net.minecraft.util.math.BlockPos
 */
package noppes.npcs.api.wrapper;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.BlockPos;
import noppes.npcs.api.IPos;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IEntityLiving;
import noppes.npcs.api.entity.IEntityLivingBase;
import noppes.npcs.api.wrapper.BlockPosWrapper;
import noppes.npcs.api.wrapper.EntityLivingBaseWrapper;

public class EntityLivingWrapper<T extends EntityLiving>
extends EntityLivingBaseWrapper<T>
implements IEntityLiving {
    public EntityLivingWrapper(T entity) {
        super(entity);
    }

    @Override
    public void navigateTo(double x, double y, double z, double speed) {
        ((EntityLiving)this.entity).func_70661_as().func_75499_g();
        ((EntityLiving)this.entity).func_70661_as().func_75492_a(x, y, z, speed * 0.7);
    }

    @Override
    public void clearNavigation() {
        ((EntityLiving)this.entity).func_70661_as().func_75499_g();
    }

    @Override
    public IPos getNavigationPath() {
        if (!this.isNavigating()) {
            return null;
        }
        PathPoint point = ((EntityLiving)this.entity).func_70661_as().func_75505_d().func_75870_c();
        if (point == null) {
            return null;
        }
        return new BlockPosWrapper(new BlockPos(point.field_75839_a, point.field_75837_b, point.field_75838_c));
    }

    @Override
    public boolean isNavigating() {
        return !((EntityLiving)this.entity).func_70661_as().func_75500_f();
    }

    @Override
    public boolean isAttacking() {
        return super.isAttacking() || ((EntityLiving)this.entity).func_70638_az() != null;
    }

    @Override
    public void setAttackTarget(IEntityLivingBase living) {
        if (living == null) {
            ((EntityLiving)this.entity).func_70624_b(null);
        } else {
            ((EntityLiving)this.entity).func_70624_b(living.getMCEntity());
        }
        super.setAttackTarget(living);
    }

    @Override
    public IEntityLivingBase getAttackTarget() {
        IEntityLivingBase base = (IEntityLivingBase)NpcAPI.Instance().getIEntity((Entity)((EntityLiving)this.entity).func_70638_az());
        return base != null ? base : super.getAttackTarget();
    }

    @Override
    public boolean canSeeEntity(IEntity entity) {
        return ((EntityLiving)this.entity).func_70635_at().func_75522_a(entity.getMCEntity());
    }

    @Override
    public void jump() {
        ((EntityLiving)this.entity).func_70683_ar().func_75660_a();
    }
}

