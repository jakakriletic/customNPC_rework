/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.EntityCreature
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.ai.EntityAITarget
 */
package noppes.npcs.ai.target;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITarget;
import noppes.npcs.constants.AiMutex;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityAIOwnerHurtTarget
extends EntityAITarget {
    EntityNPCInterface npc;
    EntityLivingBase theTarget;
    private int field_142050_e;

    public EntityAIOwnerHurtTarget(EntityNPCInterface npc) {
        super((EntityCreature)npc, false);
        this.npc = npc;
        this.func_75248_a(AiMutex.PASSIVE);
    }

    public boolean func_75250_a() {
        if (!this.npc.isFollower() || this.npc.roleInterface == null || !this.npc.roleInterface.defendOwner()) {
            return false;
        }
        EntityLivingBase entitylivingbase = this.npc.getOwner();
        if (entitylivingbase == null) {
            return false;
        }
        this.theTarget = entitylivingbase.func_110144_aD();
        int i = entitylivingbase.func_142013_aG();
        return i != this.field_142050_e && this.func_75296_a(this.theTarget, false);
    }

    public void func_75249_e() {
        this.field_75299_d.func_70624_b(this.theTarget);
        EntityLivingBase entitylivingbase = this.npc.getOwner();
        if (entitylivingbase != null) {
            this.field_142050_e = entitylivingbase.func_142013_aG();
        }
        super.func_75249_e();
    }
}

