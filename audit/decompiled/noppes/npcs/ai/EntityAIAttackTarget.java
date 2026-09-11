/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.ai.EntityAIBase
 *  net.minecraft.pathfinding.Path
 *  net.minecraft.util.EnumHand
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.World
 */
package noppes.npcs.ai;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import noppes.npcs.constants.AiMutex;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityAIAttackTarget
extends EntityAIBase {
    private World world;
    private EntityNPCInterface npc;
    private EntityLivingBase entityTarget;
    private int attackTick = 0;
    private Path entityPathEntity;
    private int field_75445_i;
    private boolean navOverride = false;

    public EntityAIAttackTarget(EntityNPCInterface par1EntityLiving) {
        this.npc = par1EntityLiving;
        this.world = par1EntityLiving.field_70170_p;
        this.func_75248_a(this.navOverride ? AiMutex.PATHING : AiMutex.LOOK + AiMutex.PASSIVE);
    }

    public boolean func_75250_a() {
        EntityLivingBase entitylivingbase = this.npc.func_70638_az();
        if (entitylivingbase == null || !entitylivingbase.func_70089_S()) {
            return false;
        }
        int melee = this.npc.stats.ranged.getMeleeRange();
        if (!(this.npc.inventory.getProjectile() == null || melee > 0 && this.npc.isInRange((Entity)entitylivingbase, melee))) {
            return false;
        }
        this.entityTarget = entitylivingbase;
        this.entityPathEntity = this.npc.func_70661_as().func_75494_a((Entity)entitylivingbase);
        return this.entityPathEntity != null;
    }

    public boolean func_75253_b() {
        this.entityTarget = this.npc.func_70638_az();
        if (this.entityTarget == null) {
            this.entityTarget = this.npc.func_70643_av();
        }
        if (this.entityTarget == null || !this.entityTarget.func_70089_S()) {
            return false;
        }
        if (!this.npc.isInRange((Entity)this.entityTarget, this.npc.stats.aggroRange)) {
            return false;
        }
        int melee = this.npc.stats.ranged.getMeleeRange();
        if (melee > 0 && !this.npc.isInRange((Entity)this.entityTarget, melee)) {
            return false;
        }
        return this.npc.func_180485_d(new BlockPos((Entity)this.entityTarget));
    }

    public void func_75249_e() {
        if (!this.navOverride) {
            this.npc.func_70661_as().func_75484_a(this.entityPathEntity, 1.3);
        }
        this.field_75445_i = 0;
    }

    public void func_75251_c() {
        this.entityPathEntity = null;
        this.entityTarget = null;
        this.npc.func_70661_as().func_75499_g();
    }

    public void func_75246_d() {
        this.npc.func_70671_ap().func_75651_a((Entity)this.entityTarget, 30.0f, 30.0f);
        if (!this.navOverride && --this.field_75445_i <= 0) {
            this.field_75445_i = 4 + this.npc.func_70681_au().nextInt(7);
            this.npc.func_70661_as().func_75497_a((Entity)this.entityTarget, (double)1.3f);
        }
        this.attackTick = Math.max(this.attackTick - 1, 0);
        double y = this.entityTarget.field_70163_u;
        if (this.entityTarget.func_174813_aQ() != null) {
            y = this.entityTarget.func_174813_aQ().field_72338_b;
        }
        double distance = this.npc.func_70092_e(this.entityTarget.field_70165_t, y, this.entityTarget.field_70161_v);
        double minRange = this.npc.field_70130_N * 2.0f * this.npc.field_70130_N * 2.0f + this.entityTarget.field_70130_N;
        double range = (float)(this.npc.stats.melee.getRange() * this.npc.stats.melee.getRange()) + this.entityTarget.field_70130_N;
        if (minRange > range) {
            range = minRange;
        }
        if (distance <= range && (this.npc.canSee((Entity)this.entityTarget) || distance < minRange) && this.attackTick <= 0) {
            this.attackTick = this.npc.stats.melee.getDelay();
            this.npc.func_184609_a(EnumHand.MAIN_HAND);
            this.npc.func_70652_k((Entity)this.entityTarget);
        }
    }

    public void navOverride(boolean nav) {
        this.navOverride = nav;
        this.func_75248_a(this.navOverride ? AiMutex.PATHING : AiMutex.LOOK + AiMutex.PASSIVE);
    }
}

