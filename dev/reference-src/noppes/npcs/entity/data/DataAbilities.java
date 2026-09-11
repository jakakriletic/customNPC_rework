/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.nbt.NBTTagCompound
 */
package noppes.npcs.entity.data;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.ability.AbstractAbility;
import noppes.npcs.constants.EnumAbilityType;
import noppes.npcs.entity.EntityNPCInterface;

public class DataAbilities {
    public List<AbstractAbility> abilities = new ArrayList<AbstractAbility>();
    public EntityNPCInterface npc;

    public DataAbilities(EntityNPCInterface npc) {
        this.npc = npc;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        return compound;
    }

    public void readToNBT(NBTTagCompound compound) {
    }

    public AbstractAbility getAbility(EnumAbilityType type) {
        EntityLivingBase target = this.npc.getAttackTarget();
        for (AbstractAbility ability : this.abilities) {
            if (!ability.isType(type) || !ability.canRun(target)) continue;
            return ability;
        }
        return null;
    }
}

