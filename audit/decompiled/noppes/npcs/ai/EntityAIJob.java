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

    public boolean func_75250_a() {
        if (this.npc.isKilled() || this.npc.jobInterface == null) {
            return false;
        }
        return this.npc.jobInterface.aiShouldExecute();
    }

    public void func_75249_e() {
        this.npc.jobInterface.aiStartExecuting();
    }

    public boolean func_75253_b() {
        if (this.npc.isKilled() || this.npc.jobInterface == null) {
            return false;
        }
        return this.npc.jobInterface.aiContinueExecute();
    }

    public void func_75246_d() {
        if (this.npc.jobInterface != null) {
            this.npc.jobInterface.aiUpdateTask();
        }
    }

    public void func_75251_c() {
        if (this.npc.jobInterface != null) {
            this.npc.jobInterface.resetTask();
        }
    }

    public int func_75247_h() {
        if (this.npc.jobInterface == null) {
            return super.func_75247_h();
        }
        return this.npc.jobInterface.getMutexBits();
    }
}

