/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.ai.EntityAIBase
 */
package noppes.npcs.ai;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import noppes.npcs.constants.AiMutex;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityAISprintToTarget
extends EntityAIBase {
    private EntityNPCInterface npc;

    public EntityAISprintToTarget(EntityNPCInterface par1EntityLiving) {
        this.npc = par1EntityLiving;
        this.func_75248_a(AiMutex.PASSIVE);
    }

    public boolean func_75250_a() {
        EntityLivingBase runTarget = this.npc.func_70638_az();
        if (runTarget == null || this.npc.func_70661_as().func_75500_f()) {
            return false;
        }
        switch (this.npc.ais.onAttack) {
            case 0: {
                return !this.npc.isInRange((Entity)runTarget, 8.0) ? this.npc.field_70122_E : false;
            }
            case 2: {
                return this.npc.isInRange((Entity)runTarget, 7.0) ? this.npc.field_70122_E : false;
            }
        }
        return false;
    }

    public boolean func_75253_b() {
        return this.npc.func_70089_S() && this.npc.field_70122_E && this.npc.field_70737_aN <= 0 && this.npc.field_70159_w != 0.0 && this.npc.field_70179_y != 0.0;
    }

    public void func_75249_e() {
        this.npc.func_70031_b(true);
    }

    public void func_75251_c() {
        this.npc.func_70031_b(false);
    }
}

