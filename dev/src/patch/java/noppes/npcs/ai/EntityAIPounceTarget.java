/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.ai.EntityAIBase
 *  net.minecraft.util.math.MathHelper
 */
package noppes.npcs.ai;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityAIPounceTarget
extends EntityAIBase {
    private EntityNPCInterface npc;
    private EntityLivingBase leapTarget;
    private float leapSpeed = 1.3f;

    public EntityAIPounceTarget(EntityNPCInterface leapingEntity) {
        this.npc = leapingEntity;
        this.setMutexBits(4);
    }

    public boolean shouldExecute() {
        if (!this.npc.onGround) {
            return false;
        }
        this.leapTarget = this.npc.getAttackTarget();
        if (this.leapTarget == null || !this.npc.getEntitySenses().canSee((Entity)this.leapTarget)) {
            return false;
        }
        return !this.npc.isInRange((Entity)this.leapTarget, 4.0) && this.npc.isInRange((Entity)this.leapTarget, 8.0) ? this.npc.getRNG().nextInt(5) == 0 : false;
    }

    public boolean shouldContinueExecuting() {
        return !this.npc.onGround;
    }

    public void startExecuting() {
        double varX = this.leapTarget.posX - this.npc.posX;
        double varY = this.leapTarget.getEntityBoundingBox().minY - this.npc.getEntityBoundingBox().minY;
        double varZ = this.leapTarget.posZ - this.npc.posZ;
        float varF = MathHelper.sqrt((double)(varX * varX + varZ * varZ));
        float angle = this.getAngleForXYZ(varX, varY, varZ, varF);
        float yaw = (float)(Math.atan2(varX, varZ) * 180.0 / Math.PI);
        this.npc.motionX = MathHelper.sin((float)(yaw / 180.0f * (float)Math.PI)) * MathHelper.cos((float)(angle / 180.0f * (float)Math.PI));
        this.npc.motionZ = MathHelper.cos((float)(yaw / 180.0f * (float)Math.PI)) * MathHelper.cos((float)(angle / 180.0f * (float)Math.PI));
        this.npc.motionY = MathHelper.sin((float)((angle + 1.0f) / 180.0f * (float)Math.PI));
        this.npc.motionX *= (double)this.leapSpeed;
        this.npc.motionZ *= (double)this.leapSpeed;
        this.npc.motionY *= (double)this.leapSpeed;
    }

    public float getAngleForXYZ(double varX, double varY, double varZ, double horiDist) {
        float g = 0.1f;
        float var1 = this.leapSpeed * this.leapSpeed;
        double var2 = (double)g * horiDist;
        double var3 = (double)g * horiDist * horiDist + 2.0 * varY * (double)var1;
        double var4 = (double)(var1 * var1) - (double)g * var3;
        if (var4 < 0.0) {
            return 90.0f;
        }
        float var6 = var1 - MathHelper.sqrt((double)var4);
        float var7 = (float)(Math.atan2(var6, var2) * 180.0 / Math.PI);
        return var7;
    }
}

