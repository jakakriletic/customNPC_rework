/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.ai.EntityAIBase
 */
package noppes.npcs.ai;

import net.minecraft.entity.ai.EntityAIBase;
import noppes.npcs.ability.AbstractAbility;
import noppes.npcs.ability.IAbilityUpdate;
import noppes.npcs.constants.EnumAbilityType;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityAIAbilities
extends EntityAIBase {
    private EntityNPCInterface npc;
    private IAbilityUpdate ability;

    public EntityAIAbilities(EntityNPCInterface npc) {
        this.npc = npc;
    }

    public boolean shouldExecute() {
        if (!this.npc.isAttacking()) {
            return false;
        }
        this.ability = (IAbilityUpdate)((Object)this.npc.abilities.getAbility(EnumAbilityType.UPDATE));
        return this.ability != null;
    }

    public boolean shouldContinueExecuting() {
        return this.npc.isAttacking() && this.ability.isActive();
    }

    public void updateTask() {
        this.ability.update();
    }

    public void resetTask() {
        ((AbstractAbility)((Object)this.ability)).endAbility();
        this.ability = null;
    }
}

