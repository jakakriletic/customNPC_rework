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
    public void fall(float distance, float damageMultiplier) {
        if (!this.canFly()) {
            super.fall(distance, damageMultiplier);
        }
    }

    protected void updateFallState(double y, boolean onGroundIn, IBlockState state, BlockPos pos) {
        if (!this.canFly()) {
            super.updateFallState(y, onGroundIn, state, pos);
        }
    }

    @Override
    public void travel(float par1, float par2, float par3) {
        if (!this.canFly()) {
            super.travel(par1, par2, par3);
            return;
        }
        if (!this.isInWater() && this.ais.movementType == 2) {
            this.motionY = -0.15;
        }
        if (this.isInWater() && this.ais.movementType == 1) {
            this.moveRelative(par1, par2, par3, 0.02f);
            this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
            this.motionX *= (double)0.8f;
            this.motionY *= (double)0.8f;
            this.motionZ *= (double)0.8f;
        } else if (this.isInLava()) {
            this.moveRelative(par1, par2, par3, 0.02f);
            this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
            this.motionX *= 0.5;
            this.motionY *= 0.5;
            this.motionZ *= 0.5;
        } else {
            float f2 = 0.91f;
            if (this.onGround) {
                f2 = this.world.getBlockState((BlockPos)new BlockPos((double)this.posX, (double)(this.getEntityBoundingBox().minY - 1.0), (double)this.posZ)).getBlock().slipperiness * 0.91f;
            }
            float f3 = 0.16277136f / (f2 * f2 * f2);
            this.moveRelative(par1, par2, par3, this.onGround ? 0.1f * f3 : 0.02f);
            f2 = 0.91f;
            if (this.onGround) {
                f2 = this.world.getBlockState((BlockPos)new BlockPos((double)this.posX, (double)(this.getEntityBoundingBox().minY - 1.0), (double)this.posZ)).getBlock().slipperiness * 0.91f;
            }
            this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
            this.motionX *= (double)f2;
            this.motionY *= (double)f2;
            this.motionZ *= (double)f2;
        }
        this.prevLimbSwingAmount = this.limbSwingAmount;
        double d1 = this.posX - this.prevPosX;
        double d0 = this.posZ - this.prevPosZ;
        float f4 = MathHelper.sqrt((double)(d1 * d1 + d0 * d0)) * 4.0f;
        if (f4 > 1.0f) {
            f4 = 1.0f;
        }
        this.limbSwingAmount += (f4 - this.limbSwingAmount) * 0.4f;
        this.limbSwing += this.limbSwingAmount;
    }

    public boolean isOnLadder() {
        return false;
    }
}

