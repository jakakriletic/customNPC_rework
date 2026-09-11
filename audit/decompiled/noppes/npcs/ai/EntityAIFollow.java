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

public class EntityAIFollow
extends EntityAIBase {
    private EntityNPCInterface npc;
    private EntityLivingBase owner;
    public int updateTick = 0;

    public EntityAIFollow(EntityNPCInterface npc) {
        this.npc = npc;
        this.func_75248_a(AiMutex.PASSIVE + AiMutex.LOOK);
    }

    public boolean func_75250_a() {
        if (!this.canExcute()) {
            return false;
        }
        return !this.npc.isInRange((Entity)this.owner, this.npc.followRange());
    }

    public boolean canExcute() {
        return this.npc.func_70089_S() && this.npc.isFollower() && !this.npc.isAttacking() && (this.owner = this.npc.getOwner()) != null && this.npc.ais.animationType != 1;
    }

    public void func_75249_e() {
        this.updateTick = 10;
    }

    public boolean func_75253_b() {
        return !this.npc.func_70661_as().func_75500_f() && !this.npc.isInRange((Entity)this.owner, 2.0) && this.canExcute();
    }

    public void func_75251_c() {
        this.owner = null;
        this.npc.func_70661_as().func_75499_g();
    }

    public void func_75246_d() {
        ++this.updateTick;
        if (this.updateTick < 10) {
            return;
        }
        this.updateTick = 0;
        this.npc.func_70671_ap().func_75651_a((Entity)this.owner, 10.0f, (float)this.npc.func_70646_bf());
        double distance = this.npc.func_70068_e((Entity)this.owner);
        double speed = 1.0 + distance / 150.0;
        if (speed > 3.0) {
            speed = 3.0;
        }
        if (this.owner.func_70051_ag()) {
            speed += 0.5;
        }
        if (this.npc.func_70661_as().func_75497_a((Entity)this.owner, speed) || this.npc.isInRange((Entity)this.owner, 16.0)) {
            return;
        }
        this.npc.tpTo(this.owner);
    }
}

