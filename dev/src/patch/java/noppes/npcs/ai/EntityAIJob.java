/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.ai.EntityAIBase
 */
package noppes.npcs.ai;

import net.minecraft.entity.ai.EntityAIBase;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityAIJob
extends EntityAIBase {
    private EntityNPCInterface npc;

    public EntityAIJob(EntityNPCInterface npc) {
        this.npc = npc;
    }

    public boolean shouldExecute() {
        if (this.npc.isKilled() || this.npc.jobInterface == null) {
            return false;
        }
        return this.npc.jobInterface.aiShouldExecute();
    }

    public void startExecuting() {
        this.npc.jobInterface.aiStartExecuting();
    }

    public boolean shouldContinueExecuting() {
        if (this.npc.isKilled() || this.npc.jobInterface == null) {
            return false;
        }
        return this.npc.jobInterface.aiContinueExecute();
    }

    public void updateTask() {
        if (this.npc.jobInterface != null) {
            this.npc.jobInterface.aiUpdateTask();
        }
    }

    public void resetTask() {
        if (this.npc.jobInterface != null) {
            this.npc.jobInterface.resetTask();
        }
    }

    public int getMutexBits() {
        if (this.npc.jobInterface == null) {
            return super.getMutexBits();
        }
        return this.npc.jobInterface.getMutexBits();
    }
}

