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
        this.func_75248_a(4);
    }

    public boolean func_75250_a() {
        if (!this.npc.field_70122_E) {
            return false;
        }
        this.leapTarget = this.npc.func_70638_az();
        if (this.leapTarget == null || !this.npc.func_70635_at().func_75522_a((Entity)this.leapTarget)) {
            return false;
        }
        return !this.npc.isInRange((Entity)this.leapTarget, 4.0) && this.npc.isInRange((Entity)this.leapTarget, 8.0) ? this.npc.func_70681_au().nextInt(5) == 0 : false;
    }

    public boolean func_75253_b() {
        return !this.npc.field_70122_E;
    }

    public void func_75249_e() {
        double varX = this.leapTarget.field_70165_t - this.npc.field_70165_t;
        double varY = this.leapTarget.func_174813_aQ().field_72338_b - this.npc.func_174813_aQ().field_72338_b;
        double varZ = this.leapTarget.field_70161_v - this.npc.field_70161_v;
        float varF = MathHelper.func_76133_a((double)(varX * varX + varZ * varZ));
        float angle = this.getAngleForXYZ(varX, varY, varZ, varF);
        float yaw = (float)(Math.atan2(varX, varZ) * 180.0 / Math.PI);
        this.npc.field_70159_w = MathHelper.func_76126_a((float)(yaw / 180.0f * (float)Math.PI)) * MathHelper.func_76134_b((float)(angle / 180.0f * (float)Math.PI));
        this.npc.field_70179_y = MathHelper.func_76134_b((float)(yaw / 180.0f * (float)Math.PI)) * MathHelper.func_76134_b((float)(angle / 180.0f * (float)Math.PI));
        this.npc.field_70181_x = MathHelper.func_76126_a((float)((angle + 1.0f) / 180.0f * (float)Math.PI));
        this.npc.field_70159_w *= (double)this.leapSpeed;
        this.npc.field_70179_y *= (double)this.leapSpeed;
        this.npc.field_70181_x *= (double)this.leapSpeed;
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
        float var6 = var1 - MathHelper.func_76133_a((double)var4);
        float var7 = (float)(Math.atan2(var6, var2) * 180.0 / Math.PI);
        return var7;
    }
}

