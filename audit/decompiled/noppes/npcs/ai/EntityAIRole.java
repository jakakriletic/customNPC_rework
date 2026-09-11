/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.ai.EntityAIBase
 */
package noppes.npcs.ai;

import net.minecraft.entity.ai.EntityAIBase;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityAIRole
extends EntityAIBase {
    private EntityNPCInterface npc;

    public EntityAIRole(EntityNPCInterface npc) {
        this.npc = npc;
    }

    public boolean func_75250_a() {
        if (this.npc.isKilled() || this.npc.roleInterface == null) {
            return false;
        }
        return this.npc.roleInterface.aiShouldExecute();
    }

    public void func_75249_e() {
        this.npc.roleInterface.aiStartExecuting();
    }

    public boolean func_75253_b() {
        if (this.npc.isKilled() || this.npc.roleInterface == null) {
            return false;
        }
        return this.npc.roleInterface.aiContinueExecute();
    }

    public void func_75246_d() {
        if (this.npc.roleInterface != null) {
            this.npc.roleInterface.aiUpdateTask();
        }
    }
}

