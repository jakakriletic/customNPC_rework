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
        ((EntityLiving)this.entity).getNavigator().clearPath();
        ((EntityLiving)this.entity).getNavigator().tryMoveToXYZ(x, y, z, speed * 0.7);
    }

    @Override
    public void clearNavigation() {
        ((EntityLiving)this.entity).getNavigator().clearPath();
    }

    @Override
    public IPos getNavigationPath() {
        if (!this.isNavigating()) {
            return null;
        }
        PathPoint point = ((EntityLiving)this.entity).getNavigator().getPath().getFinalPathPoint();
        if (point == null) {
            return null;
        }
        return new BlockPosWrapper(new BlockPos(point.x, point.y, point.z));
    }

    @Override
    public boolean isNavigating() {
        return !((EntityLiving)this.entity).getNavigator().noPath();
    }

    @Override
    public boolean isAttacking() {
        return super.isAttacking() || ((EntityLiving)this.entity).getAttackTarget() != null;
    }

    @Override
    public void setAttackTarget(IEntityLivingBase living) {
        if (living == null) {
            ((EntityLiving)this.entity).setAttackTarget(null);
        } else {
            ((EntityLiving)this.entity).setAttackTarget(living.getMCEntity());
        }
        super.setAttackTarget(living);
    }

    @Override
    public IEntityLivingBase getAttackTarget() {
        IEntityLivingBase base = (IEntityLivingBase)NpcAPI.Instance().getIEntity((Entity)((EntityLiving)this.entity).getAttackTarget());
        return base != null ? base : super.getAttackTarget();
    }

    @Override
    public boolean canSeeEntity(IEntity entity) {
        return ((EntityLiving)this.entity).getEntitySenses().canSee(entity.getMCEntity());
    }

    @Override
    public void jump() {
        ((EntityLiving)this.entity).getJumpHelper().setJumping();
    }
}

