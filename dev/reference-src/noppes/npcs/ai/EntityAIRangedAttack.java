/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.IRangedAttackMob
 *  net.minecraft.entity.ai.EntityAIBase
 *  net.minecraft.util.EnumHand
 */
package noppes.npcs.ai;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.EnumHand;
import noppes.npcs.constants.AiMutex;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityAIRangedAttack
extends EntityAIBase {
    private final EntityNPCInterface npc;
    private EntityLivingBase attackTarget;
    private int rangedAttackTime = 0;
    private int moveTries = 0;
    private int burstCount = 0;
    private int attackTick = 0;
    private boolean hasFired = false;
    private boolean navOverride = false;

    public EntityAIRangedAttack(IRangedAttackMob par1IRangedAttackMob) {
        if (!(par1IRangedAttackMob instanceof EntityLivingBase)) {
            throw new IllegalArgumentException("ArrowAttackGoal requires Mob implements RangedAttackMob");
        }
        this.npc = (EntityNPCInterface)par1IRangedAttackMob;
        this.rangedAttackTime = this.npc.stats.ranged.getDelayMin() / 2;
        this.setMutexBits(this.navOverride ? AiMutex.PATHING : AiMutex.LOOK + AiMutex.PASSIVE);
    }

    public boolean shouldExecute() {
        this.attackTarget = this.npc.getAttackTarget();
        if (this.attackTarget == null || !this.attackTarget.isEntityAlive() || !this.npc.isInRange((Entity)this.attackTarget, this.npc.stats.aggroRange) || this.npc.inventory.getProjectile() == null) {
            return false;
        }
        return this.npc.stats.ranged.getMeleeRange() < 1 || !this.npc.isInRange((Entity)this.attackTarget, this.npc.stats.ranged.getMeleeRange());
    }

    public void resetTask() {
        this.attackTarget = null;
        this.npc.setAttackTarget(null);
        this.npc.getNavigator().clearPath();
        this.moveTries = 0;
        this.hasFired = false;
        this.rangedAttackTime = this.npc.stats.ranged.getDelayMin() / 2;
    }

    public void updateTask() {
        this.npc.getLookHelper().setLookPositionWithEntity((Entity)this.attackTarget, 30.0f, 30.0f);
        double var1 = this.npc.getDistanceSq(this.attackTarget.posX, this.attackTarget.getEntityBoundingBox().minY, this.attackTarget.posZ);
        float range = this.npc.stats.ranged.getRange() * this.npc.stats.ranged.getRange();
        if (!this.navOverride && this.npc.ais.directLOS) {
            int v;
            this.moveTries = this.npc.getEntitySenses().canSee((Entity)this.attackTarget) ? ++this.moveTries : 0;
            int n = v = this.npc.ais.tacticalVariant == 0 ? 20 : 5;
            if (var1 <= (double)range && this.moveTries >= v) {
                this.npc.getNavigator().clearPath();
            } else {
                this.npc.getNavigator().tryMoveToEntityLiving((Entity)this.attackTarget, 1.0);
            }
        }
        if (this.rangedAttackTime-- <= 0 && var1 <= (double)range && (this.npc.getEntitySenses().canSee((Entity)this.attackTarget) || this.npc.stats.ranged.getFireType() == 2)) {
            if (this.burstCount++ <= this.npc.stats.ranged.getBurst()) {
                this.rangedAttackTime = this.npc.stats.ranged.getBurstDelay();
            } else {
                this.burstCount = 0;
                this.hasFired = true;
                this.rangedAttackTime = this.npc.stats.ranged.getDelayRNG();
            }
            if (this.burstCount > 1) {
                boolean indirect = false;
                switch (this.npc.stats.ranged.getFireType()) {
                    case 1: {
                        indirect = var1 > (double)range / 2.0;
                        break;
                    }
                    case 2: {
                        indirect = !this.npc.getEntitySenses().canSee((Entity)this.attackTarget);
                    }
                }
                this.npc.attackEntityWithRangedAttack(this.attackTarget, indirect ? 1.0f : 0.0f);
                if (this.npc.currentAnimation != 6) {
                    this.npc.swingArm(EnumHand.MAIN_HAND);
                }
            }
        }
    }

    public boolean hasFired() {
        return this.hasFired;
    }

    public void navOverride(boolean nav) {
        this.navOverride = nav;
        this.setMutexBits(this.navOverride ? AiMutex.PATHING : AiMutex.LOOK + AiMutex.PASSIVE);
    }
}

