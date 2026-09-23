/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLiving
 *  net.minecraft.entity.SharedMonsterAttributes
 *  net.minecraft.entity.ai.EntityMoveHelper
 *  net.minecraft.entity.ai.EntityMoveHelper$Action
 *  net.minecraft.util.math.AxisAlignedBB
 *  net.minecraft.util.math.MathHelper
 */
package noppes.npcs.ai;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.entity.EntityNPCInterface;

public class FlyingMoveHelper
extends EntityMoveHelper {
    private EntityNPCInterface entity;
    private int courseChangeCooldown;

    public FlyingMoveHelper(EntityNPCInterface entity) {
        super((EntityLiving)entity);
        this.entity = entity;
    }

    public void onUpdateMoveHelper() {
        if (this.action == EntityMoveHelper.Action.MOVE_TO && this.courseChangeCooldown-- <= 0) {
            this.courseChangeCooldown = 4;
            double d0 = this.posX - this.entity.posX;
            double d1 = this.posY - this.entity.posY;
            double d2 = this.posZ - this.entity.posZ;
            double d3 = d0 * d0 + d1 * d1 + d2 * d2;
            if ((d3 = (double)MathHelper.sqrt((double)d3)) > 0.5 && this.isNotColliding(this.posX, this.posY, this.posZ, d3)) {
                double speed = this.entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue() / 2.5;
                if (d3 < 3.0 && speed > (double)0.1f) {
                    speed = 0.1f;
                }
                this.entity.motionX += d0 / d3 * speed;
                this.entity.motionY += d1 / d3 * speed;
                this.entity.motionZ += d2 / d3 * speed;
                this.entity.renderYawOffset = this.entity.rotationYaw = -((float)Math.atan2(this.entity.motionX, this.entity.motionZ)) * 180.0f / (float)Math.PI;
            } else {
                this.action = EntityMoveHelper.Action.WAIT;
            }
        }
    }

    private boolean isNotColliding(double p_179926_1_, double p_179926_3_, double p_179926_5_, double p_179926_7_) {
        double d4 = (p_179926_1_ - this.entity.posX) / p_179926_7_;
        double d5 = (p_179926_3_ - this.entity.posY) / p_179926_7_;
        double d6 = (p_179926_5_ - this.entity.posZ) / p_179926_7_;
        AxisAlignedBB axisalignedbb = this.entity.getEntityBoundingBox();
        int i = 1;
        while ((double)i < p_179926_7_) {
            if (!this.entity.world.getCollisionBoxes((Entity)this.entity, axisalignedbb = axisalignedbb.offset(d4, d5, d6)).isEmpty()) {
                return false;
            }
            ++i;
        }
        return true;
    }
}

