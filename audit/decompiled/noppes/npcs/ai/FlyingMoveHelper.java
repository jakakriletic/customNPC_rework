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

    public void func_75641_c() {
        if (this.field_188491_h == EntityMoveHelper.Action.MOVE_TO && this.courseChangeCooldown-- <= 0) {
            this.courseChangeCooldown = 4;
            double d0 = this.field_75646_b - this.entity.field_70165_t;
            double d1 = this.field_75647_c - this.entity.field_70163_u;
            double d2 = this.field_75644_d - this.entity.field_70161_v;
            double d3 = d0 * d0 + d1 * d1 + d2 * d2;
            if ((d3 = (double)MathHelper.func_76133_a((double)d3)) > 0.5 && this.isNotColliding(this.field_75646_b, this.field_75647_c, this.field_75644_d, d3)) {
                double speed = this.entity.func_110148_a(SharedMonsterAttributes.field_111263_d).func_111126_e() / 2.5;
                if (d3 < 3.0 && speed > (double)0.1f) {
                    speed = 0.1f;
                }
                this.entity.field_70159_w += d0 / d3 * speed;
                this.entity.field_70181_x += d1 / d3 * speed;
                this.entity.field_70179_y += d2 / d3 * speed;
                this.entity.field_70761_aq = this.entity.field_70177_z = -((float)Math.atan2(this.entity.field_70159_w, this.entity.field_70179_y)) * 180.0f / (float)Math.PI;
            } else {
                this.field_188491_h = EntityMoveHelper.Action.WAIT;
            }
        }
    }

    private boolean isNotColliding(double p_179926_1_, double p_179926_3_, double p_179926_5_, double p_179926_7_) {
        double d4 = (p_179926_1_ - this.entity.field_70165_t) / p_179926_7_;
        double d5 = (p_179926_3_ - this.entity.field_70163_u) / p_179926_7_;
        double d6 = (p_179926_5_ - this.entity.field_70161_v) / p_179926_7_;
        AxisAlignedBB axisalignedbb = this.entity.func_174813_aQ();
        int i = 1;
        while ((double)i < p_179926_7_) {
            if (!this.entity.field_70170_p.func_184144_a((Entity)this.entity, axisalignedbb = axisalignedbb.func_72317_d(d4, d5, d6)).isEmpty()) {
                return false;
            }
            ++i;
        }
        return true;
    }
}

