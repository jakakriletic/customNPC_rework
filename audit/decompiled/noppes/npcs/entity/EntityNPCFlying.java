/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.entity.MoverType
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.world.World
 */
package noppes.npcs.entity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.MoverType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import noppes.npcs.entity.EntityNPCInterface;

public abstract class EntityNPCFlying
extends EntityNPCInterface {
    public EntityNPCFlying(World world) {
        super(world);
    }

    @Override
    public boolean canFly() {
        return this.ais.movementType > 0;
    }

    @Override
    public void func_180430_e(float distance, float damageMultiplier) {
        if (!this.canFly()) {
            super.func_180430_e(distance, damageMultiplier);
        }
    }

    protected void func_184231_a(double y, boolean onGroundIn, IBlockState state, BlockPos pos) {
        if (!this.canFly()) {
            super.func_184231_a(y, onGroundIn, state, pos);
        }
    }

    @Override
    public void func_191986_a(float par1, float par2, float par3) {
        if (!this.canFly()) {
            super.func_191986_a(par1, par2, par3);
            return;
        }
        if (!this.func_70090_H() && this.ais.movementType == 2) {
            this.field_70181_x = -0.15;
        }
        if (this.func_70090_H() && this.ais.movementType == 1) {
            this.func_191958_b(par1, par2, par3, 0.02f);
            this.func_70091_d(MoverType.SELF, this.field_70159_w, this.field_70181_x, this.field_70179_y);
            this.field_70159_w *= (double)0.8f;
            this.field_70181_x *= (double)0.8f;
            this.field_70179_y *= (double)0.8f;
        } else if (this.func_180799_ab()) {
            this.func_191958_b(par1, par2, par3, 0.02f);
            this.func_70091_d(MoverType.SELF, this.field_70159_w, this.field_70181_x, this.field_70179_y);
            this.field_70159_w *= 0.5;
            this.field_70181_x *= 0.5;
            this.field_70179_y *= 0.5;
        } else {
            float f2 = 0.91f;
            if (this.field_70122_E) {
                f2 = this.field_70170_p.func_180495_p((BlockPos)new BlockPos((double)this.field_70165_t, (double)(this.func_174813_aQ().field_72338_b - 1.0), (double)this.field_70161_v)).func_177230_c().field_149765_K * 0.91f;
            }
            float f3 = 0.16277136f / (f2 * f2 * f2);
            this.func_191958_b(par1, par2, par3, this.field_70122_E ? 0.1f * f3 : 0.02f);
            f2 = 0.91f;
            if (this.field_70122_E) {
                f2 = this.field_70170_p.func_180495_p((BlockPos)new BlockPos((double)this.field_70165_t, (double)(this.func_174813_aQ().field_72338_b - 1.0), (double)this.field_70161_v)).func_177230_c().field_149765_K * 0.91f;
            }
            this.func_70091_d(MoverType.SELF, this.field_70159_w, this.field_70181_x, this.field_70179_y);
            this.field_70159_w *= (double)f2;
            this.field_70181_x *= (double)f2;
            this.field_70179_y *= (double)f2;
        }
        this.field_184618_aE = this.field_70721_aZ;
        double d1 = this.field_70165_t - this.field_70169_q;
        double d0 = this.field_70161_v - this.field_70166_s;
        float f4 = MathHelper.func_76133_a((double)(d1 * d1 + d0 * d0)) * 4.0f;
        if (f4 > 1.0f) {
            f4 = 1.0f;
        }
        this.field_70721_aZ += (f4 - this.field_70721_aZ) * 0.4f;
        this.field_184619_aG += this.field_70721_aZ;
    }

    public boolean func_70617_f_() {
        return false;
    }
}

