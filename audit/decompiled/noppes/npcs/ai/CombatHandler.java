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
        if (this.npc.func_70638_az() != null && !this.npc.isAttacking()) {
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
        if (this.npc.func_70638_az() == null) {
            return false;
        }
        return this.isValidTarget(this.npc.func_70638_az());
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
        this.startTime = this.npc.field_70170_p.func_72912_H().func_82573_f();
        this.npc.func_184212_Q().func_187227_b(EntityNPCInterface.Attacking, (Object)true);
        for (AbstractAbility ab : this.npc.abilities.abilities) {
            ab.startCombat();
        }
    }

    public void reset() {
        this.combatResetTimer = 0;
        this.aggressors.clear();
        this.npc.func_184212_Q().func_187227_b(EntityNPCInterface.Attacking, (Object)false);
    }

    public boolean checkTarget() {
        if (this.aggressors.isEmpty() || this.npc.field_70173_aa % 10 != 0) {
            return false;
        }
        EntityLivingBase target = this.npc.func_70638_az();
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
        if (target == null || !target.func_70089_S()) {
            return false;
        }
        if (target instanceof EntityPlayer && ((EntityPlayer)target).field_71075_bZ.field_75102_a) {
            return false;
        }
        return this.npc.isInRange((Entity)target, this.npc.stats.aggroRange);
    }
}

