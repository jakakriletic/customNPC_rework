/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.ai.EntityAIBase
 */
package noppes.npcs.ai;

import noppes.npcs.rework.entity.MountGuard;

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
        this.setMutexBits(AiMutex.PASSIVE + AiMutex.LOOK);
    }

    public boolean shouldExecute() {
        if (!this.canExcute()) {
            return false;
        }
        return !this.npc.isInRange((Entity)this.owner, this.npc.followRange());
    }

    public boolean canExcute() {
        // M3.4: jahac ne isce poti, ko krmili nosilec (v ORIGINAL vedno false).
        if (MountGuard.riderMovementBlocked(this.npc)) {
            return false;
        }
        return this.npc.isEntityAlive() && this.npc.isFollower() && !this.npc.isAttacking() && (this.owner = this.npc.getOwner()) != null && this.npc.ais.animationType != 1;
    }

    public void startExecuting() {
        this.updateTick = 10;
    }

    public boolean shouldContinueExecuting() {
        return !this.npc.getNavigator().noPath() && !this.npc.isInRange((Entity)this.owner, 2.0) && this.canExcute();
    }

    public void resetTask() {
        this.owner = null;
        this.npc.getNavigator().clearPath();
    }

    public void updateTask() {
        ++this.updateTick;
        if (this.updateTick < 10) {
            return;
        }
        this.updateTick = 0;
        this.npc.getLookHelper().setLookPositionWithEntity((Entity)this.owner, 10.0f, (float)this.npc.getVerticalFaceSpeed());
        double distance = this.npc.getDistanceSq((Entity)this.owner);
        double speed = 1.0 + distance / 150.0;
        if (speed > 3.0) {
            speed = 3.0;
        }
        if (this.owner.isSprinting()) {
            speed += 0.5;
        }
        if (this.npc.getNavigator().tryMoveToEntityLiving((Entity)this.owner, speed) || this.npc.isInRange((Entity)this.owner, 16.0)) {
            return;
        }
        this.npc.tpTo(this.owner);
    }
}

