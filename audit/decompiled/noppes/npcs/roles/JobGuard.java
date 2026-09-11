/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityList
 *  net.minecraft.entity.monster.EntityCreeper
 *  net.minecraft.entity.monster.EntityMob
 *  net.minecraft.entity.passive.EntityAnimal
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraftforge.fml.common.registry.EntityEntry
 *  net.minecraftforge.fml.common.registry.ForgeRegistries
 */
package noppes.npcs.roles;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import noppes.npcs.NBTTags;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.JobInterface;

public class JobGuard
extends JobInterface {
    public List<String> targets = new ArrayList<String>();

    public JobGuard(EntityNPCInterface npc) {
        super(npc);
    }

    public boolean isEntityApplicable(Entity entity) {
        if (entity instanceof EntityPlayer || entity instanceof EntityNPCInterface) {
            return false;
        }
        return this.targets.contains("entity." + EntityList.func_75621_b((Entity)entity) + ".name");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbttagcompound) {
        nbttagcompound.func_74782_a("GuardTargets", (NBTBase)NBTTags.nbtStringList(this.targets));
        return nbttagcompound;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbttagcompound) {
        String name;
        Class cl;
        this.targets = NBTTags.getStringList(nbttagcompound.func_150295_c("GuardTargets", 10));
        if (nbttagcompound.func_74767_n("GuardAttackAnimals")) {
            for (EntityEntry ent : ForgeRegistries.ENTITIES.getValues()) {
                cl = ent.getEntityClass();
                name = "entity." + ent.getName() + ".name";
                if (!EntityAnimal.class.isAssignableFrom(cl) || this.targets.contains(name)) continue;
                this.targets.add(name);
            }
        }
        if (nbttagcompound.func_74767_n("GuardAttackMobs")) {
            for (EntityEntry ent : ForgeRegistries.ENTITIES.getValues()) {
                cl = ent.getEntityClass();
                name = "entity." + ent.getName() + ".name";
                if (!EntityMob.class.isAssignableFrom(cl) || EntityCreeper.class.isAssignableFrom(cl) || this.targets.contains(name)) continue;
                this.targets.add(name);
            }
        }
        if (nbttagcompound.func_74767_n("GuardAttackCreepers")) {
            for (EntityEntry ent : ForgeRegistries.ENTITIES.getValues()) {
                cl = ent.getEntityClass();
                name = "entity." + ent.getName() + ".name";
                if (!EntityCreeper.class.isAssignableFrom(cl) || this.targets.contains(name)) continue;
                this.targets.add(name);
            }
        }
    }
}

