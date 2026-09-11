/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.EnumCreatureAttribute
 *  net.minecraft.entity.SharedMonsterAttributes
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 */
package noppes.npcs.entity.data;

import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.Resistances;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.entity.data.INPCMelee;
import noppes.npcs.api.entity.data.INPCRanged;
import noppes.npcs.api.entity.data.INPCStats;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataMelee;
import noppes.npcs.entity.data.DataRanged;
import noppes.npcs.util.ValueUtil;

public class DataStats
implements INPCStats {
    public int aggroRange = 16;
    public int maxHealth = 20;
    public int respawnTime = 20;
    public int spawnCycle = 0;
    public boolean hideKilledBody = false;
    public Resistances resistances = new Resistances();
    public boolean immuneToFire = false;
    public boolean potionImmune = false;
    public boolean canDrown = true;
    public boolean burnInSun = false;
    public boolean noFallDamage = false;
    public boolean ignoreCobweb = false;
    public int healthRegen = 1;
    public int combatRegen = 0;
    public EnumCreatureAttribute creatureType = EnumCreatureAttribute.UNDEFINED;
    public DataMelee melee;
    public DataRanged ranged;
    private EntityNPCInterface npc;

    public DataStats(EntityNPCInterface npc) {
        this.npc = npc;
        this.melee = new DataMelee(npc);
        this.ranged = new DataRanged(npc);
    }

    public void readToNBT(NBTTagCompound compound) {
        this.resistances.readToNBT(compound.getCompoundTag("Resistances"));
        this.setMaxHealth(compound.getInteger("MaxHealth"));
        this.hideKilledBody = compound.getBoolean("HideBodyWhenKilled");
        this.aggroRange = compound.getInteger("AggroRange");
        this.respawnTime = compound.getInteger("RespawnTime");
        this.spawnCycle = compound.getInteger("SpawnCycle");
        this.creatureType = EnumCreatureAttribute.values()[compound.getInteger("CreatureType")];
        this.healthRegen = compound.getInteger("HealthRegen");
        this.combatRegen = compound.getInteger("CombatRegen");
        this.immuneToFire = compound.getBoolean("ImmuneToFire");
        this.potionImmune = compound.getBoolean("PotionImmune");
        this.canDrown = compound.getBoolean("CanDrown");
        this.burnInSun = compound.getBoolean("BurnInSun");
        this.noFallDamage = compound.getBoolean("NoFallDamage");
        this.npc.setImmuneToFire(this.immuneToFire);
        this.ignoreCobweb = compound.getBoolean("IgnoreCobweb");
        this.melee.readFromNBT(compound);
        this.ranged.readFromNBT(compound);
    }

    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setTag("Resistances", (NBTBase)this.resistances.writeToNBT());
        compound.setInteger("MaxHealth", this.maxHealth);
        compound.setInteger("AggroRange", this.aggroRange);
        compound.setBoolean("HideBodyWhenKilled", this.hideKilledBody);
        compound.setInteger("RespawnTime", this.respawnTime);
        compound.setInteger("SpawnCycle", this.spawnCycle);
        compound.setInteger("CreatureType", this.creatureType.ordinal());
        compound.setInteger("HealthRegen", this.healthRegen);
        compound.setInteger("CombatRegen", this.combatRegen);
        compound.setBoolean("ImmuneToFire", this.immuneToFire);
        compound.setBoolean("PotionImmune", this.potionImmune);
        compound.setBoolean("CanDrown", this.canDrown);
        compound.setBoolean("BurnInSun", this.burnInSun);
        compound.setBoolean("NoFallDamage", this.noFallDamage);
        compound.setBoolean("IgnoreCobweb", this.ignoreCobweb);
        this.melee.writeToNBT(compound);
        this.ranged.writeToNBT(compound);
        return compound;
    }

    @Override
    public void setMaxHealth(int maxHealth) {
        if (maxHealth == this.maxHealth) {
            return;
        }
        this.maxHealth = maxHealth;
        this.npc.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue((double)maxHealth);
        this.npc.updateClient = true;
    }

    @Override
    public int getMaxHealth() {
        return this.maxHealth;
    }

    @Override
    public float getResistance(int type) {
        if (type == 0) {
            return this.resistances.melee;
        }
        if (type == 1) {
            return this.resistances.arrow;
        }
        if (type == 2) {
            return this.resistances.explosion;
        }
        if (type == 3) {
            return this.resistances.knockback;
        }
        return 1.0f;
    }

    @Override
    public void setResistance(int type, float value) {
        value = ValueUtil.correctFloat(value, 0.0f, 2.0f);
        if (type == 0) {
            this.resistances.melee = value;
        } else if (type == 1) {
            this.resistances.arrow = value;
        } else if (type == 2) {
            this.resistances.explosion = value;
        } else if (type == 3) {
            this.resistances.knockback = value;
        }
    }

    @Override
    public int getCombatRegen() {
        return this.combatRegen;
    }

    @Override
    public void setCombatRegen(int regen) {
        this.combatRegen = regen;
    }

    @Override
    public int getHealthRegen() {
        return this.healthRegen;
    }

    @Override
    public void setHealthRegen(int regen) {
        this.healthRegen = regen;
    }

    @Override
    public INPCMelee getMelee() {
        return this.melee;
    }

    @Override
    public INPCRanged getRanged() {
        return this.ranged;
    }

    @Override
    public boolean getImmune(int type) {
        if (type == 0) {
            return this.potionImmune;
        }
        if (type == 1) {
            return !this.noFallDamage;
        }
        if (type == 2) {
            return this.burnInSun;
        }
        if (type == 3) {
            return this.immuneToFire;
        }
        if (type == 4) {
            return !this.canDrown;
        }
        if (type == 5) {
            return this.ignoreCobweb;
        }
        throw new CustomNPCsException("Unknown immune type: " + type, new Object[0]);
    }

    @Override
    public void setImmune(int type, boolean bo) {
        if (type == 0) {
            this.potionImmune = bo;
        } else if (type == 1) {
            this.noFallDamage = !bo;
        } else if (type == 2) {
            this.burnInSun = bo;
        } else if (type == 3) {
            this.npc.setImmuneToFire(bo);
        } else if (type == 4) {
            this.canDrown = !bo;
        } else if (type == 5) {
            this.ignoreCobweb = bo;
        } else {
            throw new CustomNPCsException("Unknown immune type: " + type, new Object[0]);
        }
    }

    @Override
    public int getCreatureType() {
        return this.creatureType.ordinal();
    }

    @Override
    public void setCreatureType(int type) {
        this.creatureType = EnumCreatureAttribute.values()[type];
    }

    @Override
    public int getRespawnType() {
        return this.spawnCycle;
    }

    @Override
    public void setRespawnType(int type) {
        this.spawnCycle = type;
    }

    @Override
    public int getRespawnTime() {
        return this.respawnTime;
    }

    @Override
    public void setRespawnTime(int seconds) {
        this.respawnTime = seconds;
    }

    @Override
    public boolean getHideDeadBody() {
        return this.hideKilledBody;
    }

    @Override
    public void setHideDeadBody(boolean hide) {
        this.hideKilledBody = hide;
        this.npc.updateClient = true;
    }

    @Override
    public int getAggroRange() {
        return this.aggroRange;
    }

    @Override
    public void setAggroRange(int range) {
        this.aggroRange = range;
    }
}

