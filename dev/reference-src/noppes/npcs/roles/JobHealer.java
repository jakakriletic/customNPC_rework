/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.monster.EntityMob
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.potion.Potion
 *  net.minecraft.potion.PotionEffect
 */
package noppes.npcs.roles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import noppes.npcs.NBTTags;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.JobInterface;
import noppes.npcs.util.ValueUtil;

public class JobHealer
extends JobInterface {
    private int healTicks = 0;
    public int range = 8;
    public byte type = (byte)2;
    public int speed = 20;
    public HashMap<Integer, Integer> effects = new HashMap();
    private List<EntityLivingBase> affected = new ArrayList<EntityLivingBase>();

    public JobHealer(EntityNPCInterface npc) {
        super(npc);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbttagcompound) {
        nbttagcompound.setInteger("HealerRange", this.range);
        nbttagcompound.setByte("HealerType", this.type);
        nbttagcompound.setTag("BeaconEffects", (NBTBase)NBTTags.nbtIntegerIntegerMap(this.effects));
        nbttagcompound.setInteger("HealerSpeed", this.speed);
        return nbttagcompound;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbttagcompound) {
        this.range = nbttagcompound.getInteger("HealerRange");
        this.type = nbttagcompound.getByte("HealerType");
        this.effects = NBTTags.getIntegerIntegerMap(nbttagcompound.getTagList("BeaconEffects", 10));
        this.speed = ValueUtil.CorrectInt(nbttagcompound.getInteger("HealerSpeed"), 10, Integer.MAX_VALUE);
    }

    @Override
    public boolean aiShouldExecute() {
        ++this.healTicks;
        if (this.healTicks < this.speed) {
            return false;
        }
        this.healTicks = 0;
        this.affected = this.npc.world.getEntitiesWithinAABB(EntityLivingBase.class, this.npc.getEntityBoundingBox().grow((double)this.range, (double)this.range / 2.0, (double)this.range));
        return !this.affected.isEmpty();
    }

    @Override
    public boolean aiContinueExecute() {
        return false;
    }

    @Override
    public void aiStartExecuting() {
        for (EntityLivingBase entity : this.affected) {
            boolean isEnemy = false;
            isEnemy = entity instanceof EntityPlayer ? this.npc.faction.isAggressiveToPlayer((EntityPlayer)entity) : (entity instanceof EntityNPCInterface ? this.npc.faction.isAggressiveToNpc((EntityNPCInterface)entity) : entity instanceof EntityMob);
            if (entity == this.npc || this.type == 0 && isEnemy || this.type == 1 && !isEnemy) continue;
            for (Integer potionEffect : this.effects.keySet()) {
                Potion p = Potion.getPotionById((int)potionEffect);
                if (p == null) continue;
                entity.addPotionEffect(new PotionEffect(p, 100, this.effects.get(potionEffect).intValue()));
            }
        }
        this.affected.clear();
    }
}

