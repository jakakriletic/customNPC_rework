/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.util.DamageSource
 */
package noppes.npcs.ai;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.ability.AbstractAbility;
import noppes.npcs.entity.EntityNPCInterface;

public class CombatHandler {
    private Map<EntityLivingBase, Float> aggressors = new HashMap<EntityLivingBase, Float>();
    private EntityNPCInterface npc;
    private long startTime = 0L;
    private int combatResetTimer = 0;

    public CombatHandler(EntityNPCInterface npc) {
        this.npc = npc;
    }

    public void update() {
        if (this.npc.isKilled()) {
            if (this.npc.isAttacking()) {
                this.reset();
            }
            return;
        }
        if (this.npc.getAttackTarget() != null && !this.npc.isAttacking()) {
            this.start();
        }
        if (!this.shouldCombatContinue()) {
            if (this.combatResetTimer++ > 40) {
                this.reset();
            }
            return;
        }
        this.combatResetTimer = 0;
    }

    private boolean shouldCombatContinue() {
        if (this.npc.getAttackTarget() == null) {
            return false;
        }
        return this.isValidTarget(this.npc.getAttackTarget());
    }

    public void damage(DamageSource source, float damageAmount) {
        this.combatResetTimer = 0;
        Entity e = NoppesUtilServer.GetDamageSourcee(source);
        if (e instanceof EntityLivingBase) {
            EntityLivingBase el = (EntityLivingBase)e;
            Float f = this.aggressors.get(el);
            if (f == null) {
                f = Float.valueOf(0.0f);
            }
            this.aggressors.put(el, Float.valueOf(f.floatValue() + damageAmount));
        }
    }

    public void start() {
        this.combatResetTimer = 0;
        this.startTime = this.npc.world.getWorldInfo().getWorldTotalTime();
        this.npc.getDataManager().set(EntityNPCInterface.Attacking, true);
        for (AbstractAbility ab : this.npc.abilities.abilities) {
            ab.startCombat();
        }
    }

    public void reset() {
        this.combatResetTimer = 0;
        this.aggressors.clear();
        this.npc.getDataManager().set(EntityNPCInterface.Attacking, false);
    }

    public boolean checkTarget() {
        if (this.aggressors.isEmpty() || this.npc.ticksExisted % 10 != 0) {
            return false;
        }
        EntityLivingBase target = this.npc.getAttackTarget();
        Float current = Float.valueOf(0.0f);
        if (this.isValidTarget(target)) {
            current = this.aggressors.get(target);
            if (current == null) {
                current = Float.valueOf(0.0f);
            }
        } else {
            target = null;
        }
        for (Map.Entry<EntityLivingBase, Float> entry : this.aggressors.entrySet()) {
            if (!(entry.getValue().floatValue() > current.floatValue()) || !this.isValidTarget(entry.getKey())) continue;
            current = entry.getValue();
            target = entry.getKey();
        }
        return target == null;
    }

    public boolean isValidTarget(EntityLivingBase target) {
        if (target == null || !target.isEntityAlive()) {
            return false;
        }
        if (target instanceof EntityPlayer && ((EntityPlayer)target).capabilities.disableDamage) {
            return false;
        }
        return this.npc.isInRange((Entity)target, this.npc.stats.aggroRange);
    }
}

