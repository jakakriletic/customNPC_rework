/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.world.WorldServer
 */
package noppes.npcs.ability;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.WorldServer;
import noppes.npcs.ability.AbstractAbility;
import noppes.npcs.ability.IAbilityDamaged;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.constants.EnumAbilityType;
import noppes.npcs.entity.EntityNPCInterface;

public class AbilityBlock
extends AbstractAbility
implements IAbilityDamaged {
    public AbilityBlock(EntityNPCInterface npc) {
        super(npc);
    }

    @Override
    public boolean canRun(EntityLivingBase target) {
        return super.canRun(target);
    }

    @Override
    public boolean isType(EnumAbilityType type) {
        return type == EnumAbilityType.ATTACKED;
    }

    @Override
    public void handleEvent(NpcEvent.DamagedEvent event) {
        WorldServer world = (WorldServer)this.npc.func_130014_f_();
        world.func_72960_a((Entity)this.npc, (byte)29);
        event.setCanceled(true);
        this.endAbility();
    }
}

