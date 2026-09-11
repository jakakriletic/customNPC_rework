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
        this.func_75248_a(this.navOverride ? AiMutex.PATHING : AiMutex.LOOK + AiMutex.PASSIVE);
    }

    public boolean func_75250_a() {
        this.attackTarget = this.npc.func_70638_az();
        if (this.attackTarget == null || !this.attackTarget.func_70089_S() || !this.npc.isInRange((Entity)this.attackTarget, this.npc.stats.aggroRange) || this.npc.inventory.getProjectile() == null) {
            return false;
        }
        return this.npc.stats.ranged.getMeleeRange() < 1 || !this.npc.isInRange((Entity)this.attackTarget, this.npc.stats.ranged.getMeleeRange());
    }

    public void func_75251_c() {
        this.attackTarget = null;
        this.npc.func_70624_b(null);
        this.npc.func_70661_as().func_75499_g();
        this.moveTries = 0;
        this.hasFired = false;
        this.rangedAttackTime = this.npc.stats.ranged.getDelayMin() / 2;
    }

    public void func_75246_d() {
        this.npc.func_70671_ap().func_75651_a((Entity)this.attackTarget, 30.0f, 30.0f);
        double var1 = this.npc.func_70092_e(this.attackTarget.field_70165_t, this.attackTarget.func_174813_aQ().field_72338_b, this.attackTarget.field_70161_v);
        float range = this.npc.stats.ranged.getRange() * this.npc.stats.ranged.getRange();
        if (!this.navOverride && this.npc.ais.directLOS) {
            int v;
            this.moveTries = this.npc.func_70635_at().func_75522_a((Entity)this.attackTarget) ? ++this.moveTries : 0;
            int n = v = this.npc.ais.tacticalVariant == 0 ? 20 : 5;
            if (var1 <= (double)range && this.moveTries >= v) {
                this.npc.func_70661_as().func_75499_g();
            } else {
                this.npc.func_70661_as().func_75497_a((Entity)this.attackTarget, 1.0);
            }
        }
        if (this.rangedAttackTime-- <= 0 && var1 <= (double)range && (this.npc.func_70635_at().func_75522_a((Entity)this.attackTarget) || this.npc.stats.ranged.getFireType() == 2)) {
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
                        indirect = !this.npc.func_70635_at().func_75522_a((Entity)this.attackTarget);
                    }
                }
                this.npc.func_82196_d(this.attackTarget, indirect ? 1.0f : 0.0f);
                if (this.npc.currentAnimation != 6) {
                    this.npc.func_184609_a(EnumHand.MAIN_HAND);
                }
            }
        }
    }

    public boolean hasFired() {
        return this.hasFired;
    }

    public void navOverride(boolean nav) {
        this.navOverride = nav;
        this.func_75248_a(this.navOverride ? AiMutex.PATHING : AiMutex.LOOK + AiMutex.PASSIVE);
    }
}

