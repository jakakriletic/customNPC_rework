/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.ai.EntityAIBase
 */
package noppes.npcs.ai;

import net.minecraft.entity.ai.EntityAIBase;
import noppes.npcs.constants.AiMutex;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityAITransform
extends EntityAIBase {
    private EntityNPCInterface npc;

    public EntityAITransform(EntityNPCInterface npc) {
        this.npc = npc;
        this.func_75248_a(AiMutex.PASSIVE);
    }

    public boolean func_75250_a() {
        if (this.npc.isKilled() || this.npc.isAttacking() || this.npc.transform.editingModus) {
            return false;
        }
        return this.npc.field_70170_p.func_72820_D() % 24000L < 12000L ? this.npc.transform.isActive : !this.npc.transform.isActive;
    }

    public void func_75249_e() {
        this.npc.transform.transform(!this.npc.transform.isActive);
    }
}

