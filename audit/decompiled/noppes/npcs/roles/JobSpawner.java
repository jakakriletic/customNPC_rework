/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLiving
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  org.apache.commons.lang3.RandomStringUtils
 */
package noppes.npcs.roles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntityLivingBase;
import noppes.npcs.api.entity.data.role.IJobSpawner;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.JobInterface;
import org.apache.commons.lang3.RandomStringUtils;

public class JobSpawner
extends JobInterface
implements IJobSpawner {
    public NBTTagCompound compound6;
    public NBTTagCompound compound5;
    public NBTTagCompound compound4;
    public NBTTagCompound compound3;
    public NBTTagCompound compound2;
    public NBTTagCompound compound1;
    public String title1;
    public String title2;
    public String title3;
    public String title4;
    public String title5;
    public String title6;
    private int number = 0;
    public List<EntityLivingBase> spawned = new ArrayList<EntityLivingBase>();
    private Map<String, Long> cooldown = new HashMap<String, Long>();
    private String id = RandomStringUtils.random((int)8, (boolean)true, (boolean)true);
    public boolean doesntDie = false;
    public int spawnType = 0;
    public int xOffset = 0;
    public int yOffset = 0;
    public int zOffset = 0;
    private EntityLivingBase target;
    public boolean despawnOnTargetLost = true;

    public JobSpawner(EntityNPCInterface npc) {
        super(npc);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        this.saveCompound(this.compound1, "SpawnerNBT1", compound);
        this.saveCompound(this.compound2, "SpawnerNBT2", compound);
        this.saveCompound(this.compound3, "SpawnerNBT3", compound);
        this.saveCompound(this.compound4, "SpawnerNBT4", compound);
        this.saveCompound(this.compound5, "SpawnerNBT5", compound);
        this.saveCompound(this.compound6, "SpawnerNBT6", compound);
        compound.func_74778_a("SpawnerId", this.id);
        compound.func_74757_a("SpawnerDoesntDie", this.doesntDie);
        compound.func_74768_a("SpawnerType", this.spawnType);
        compound.func_74768_a("SpawnerXOffset", this.xOffset);
        compound.func_74768_a("SpawnerYOffset", this.yOffset);
        compound.func_74768_a("SpawnerZOffset", this.zOffset);
        compound.func_74757_a("DespawnOnTargetLost", this.despawnOnTargetLost);
        return compound;
    }

    public NBTTagCompound getTitles() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.func_74778_a("Title1", this.getTitle(this.compound1));
        compound.func_74778_a("Title2", this.getTitle(this.compound2));
        compound.func_74778_a("Title3", this.getTitle(this.compound3));
        compound.func_74778_a("Title4", this.getTitle(this.compound4));
        compound.func_74778_a("Title5", this.getTitle(this.compound5));
        compound.func_74778_a("Title6", this.getTitle(this.compound6));
        return compound;
    }

    private String getTitle(NBTTagCompound compound) {
        if (compound != null && compound.func_74764_b("ClonedName")) {
            return compound.func_74779_i("ClonedName");
        }
        return "gui.selectnpc";
    }

    private void saveCompound(NBTTagCompound save, String name, NBTTagCompound compound) {
        if (save != null) {
            compound.func_74782_a(name, (NBTBase)save);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        this.compound1 = compound.func_74775_l("SpawnerNBT1");
        this.compound2 = compound.func_74775_l("SpawnerNBT2");
        this.compound3 = compound.func_74775_l("SpawnerNBT3");
        this.compound4 = compound.func_74775_l("SpawnerNBT4");
        this.compound5 = compound.func_74775_l("SpawnerNBT5");
        this.compound6 = compound.func_74775_l("SpawnerNBT6");
        this.id = compound.func_74779_i("SpawnerId");
        this.doesntDie = compound.func_74767_n("SpawnerDoesntDie");
        this.spawnType = compound.func_74762_e("SpawnerType");
        this.xOffset = compound.func_74762_e("SpawnerXOffset");
        this.yOffset = compound.func_74762_e("SpawnerYOffset");
        this.zOffset = compound.func_74762_e("SpawnerZOffset");
        this.despawnOnTargetLost = compound.func_74767_n("DespawnOnTargetLost");
    }

    public void cleanCompound(NBTTagCompound compound) {
        compound.func_82580_o("SpawnerNBT1");
        compound.func_82580_o("SpawnerNBT2");
        compound.func_82580_o("SpawnerNBT3");
        compound.func_82580_o("SpawnerNBT4");
        compound.func_82580_o("SpawnerNBT5");
        compound.func_82580_o("SpawnerNBT6");
    }

    public void setJobCompound(int i, NBTTagCompound compound) {
        if (i == 1) {
            this.compound1 = compound;
        }
        if (i == 2) {
            this.compound2 = compound;
        }
        if (i == 3) {
            this.compound3 = compound;
        }
        if (i == 4) {
            this.compound4 = compound;
        }
        if (i == 5) {
            this.compound5 = compound;
        }
        if (i == 6) {
            this.compound6 = compound;
        }
    }

    @Override
    public void aiUpdateTask() {
        if (this.spawned.isEmpty()) {
            if (this.spawnType == 0 && this.spawnEntity(this.number + 1) == null && !this.doesntDie) {
                this.npc.func_70106_y();
            }
            if (this.spawnType == 1) {
                if (this.number >= 6 && !this.doesntDie) {
                    this.npc.func_70106_y();
                } else {
                    this.spawnEntity(this.compound1);
                    this.spawnEntity(this.compound2);
                    this.spawnEntity(this.compound3);
                    this.spawnEntity(this.compound4);
                    this.spawnEntity(this.compound5);
                    this.spawnEntity(this.compound6);
                    this.number = 6;
                }
            }
            if (this.spawnType == 2) {
                ArrayList<NBTTagCompound> list = new ArrayList<NBTTagCompound>();
                if (this.compound1 != null && this.compound1.func_74764_b("id")) {
                    list.add(this.compound1);
                }
                if (this.compound2 != null && this.compound2.func_74764_b("id")) {
                    list.add(this.compound2);
                }
                if (this.compound3 != null && this.compound3.func_74764_b("id")) {
                    list.add(this.compound3);
                }
                if (this.compound4 != null && this.compound4.func_74764_b("id")) {
                    list.add(this.compound4);
                }
                if (this.compound5 != null && this.compound5.func_74764_b("id")) {
                    list.add(this.compound5);
                }
                if (this.compound6 != null && this.compound6.func_74764_b("id")) {
                    list.add(this.compound6);
                }
                if (!list.isEmpty()) {
                    NBTTagCompound compound = (NBTTagCompound)list.get(this.npc.func_70681_au().nextInt(list.size()));
                    this.spawnEntity(compound);
                } else if (!this.doesntDie) {
                    this.npc.func_70106_y();
                }
            }
        } else {
            this.checkSpawns();
        }
    }

    public void checkSpawns() {
        Iterator<EntityLivingBase> iterator = this.spawned.iterator();
        while (iterator.hasNext()) {
            EntityLivingBase spawn = iterator.next();
            if (this.shouldDelete(spawn)) {
                spawn.field_70128_L = true;
                iterator.remove();
                continue;
            }
            this.checkTarget(spawn);
        }
    }

    public void checkTarget(EntityLivingBase entity) {
        if (entity instanceof EntityLiving) {
            EntityLiving liv = (EntityLiving)entity;
            if (liv.func_70638_az() == null || this.npc.func_70681_au().nextInt(100) == 1) {
                liv.func_70624_b(this.target);
            }
        } else if (entity.func_70643_av() == null || this.npc.func_70681_au().nextInt(100) == 1) {
            entity.func_70604_c(this.target);
        }
    }

    public boolean shouldDelete(EntityLivingBase entity) {
        return !this.npc.isInRange((Entity)entity, 60.0) || entity.field_70128_L || entity.func_110143_aJ() <= 0.0f || this.despawnOnTargetLost && this.target == null;
    }

    private EntityLivingBase getTarget() {
        EntityLivingBase target = this.getTarget((EntityLivingBase)this.npc);
        if (target != null) {
            return target;
        }
        for (EntityLivingBase entity : this.spawned) {
            target = this.getTarget(entity);
            if (target == null) continue;
            return target;
        }
        return null;
    }

    private EntityLivingBase getTarget(EntityLivingBase entity) {
        if (entity instanceof EntityLiving) {
            this.target = ((EntityLiving)entity).func_70638_az();
            if (this.target != null && !this.target.field_70128_L && this.target.func_110143_aJ() > 0.0f) {
                return this.target;
            }
        }
        this.target = entity.func_70643_av();
        if (this.target != null && !this.target.field_70128_L && this.target.func_110143_aJ() > 0.0f) {
            return this.target;
        }
        return null;
    }

    private boolean isEmpty() {
        if (this.compound1 != null && this.compound1.func_74764_b("id")) {
            return false;
        }
        if (this.compound2 != null && this.compound2.func_74764_b("id")) {
            return false;
        }
        if (this.compound3 != null && this.compound3.func_74764_b("id")) {
            return false;
        }
        if (this.compound4 != null && this.compound4.func_74764_b("id")) {
            return false;
        }
        if (this.compound5 != null && this.compound5.func_74764_b("id")) {
            return false;
        }
        return this.compound6 == null || !this.compound6.func_74764_b("id");
    }

    private void setTarget(EntityLivingBase base, EntityLivingBase target) {
        if (base instanceof EntityLiving) {
            ((EntityLiving)base).func_70624_b(target);
        } else {
            base.func_70604_c(target);
        }
    }

    @Override
    public boolean aiShouldExecute() {
        if (this.isEmpty() || this.npc.isKilled()) {
            return false;
        }
        this.target = this.getTarget();
        if (this.npc.func_70681_au().nextInt(30) == 1 && this.spawned.isEmpty()) {
            this.spawned = this.getNearbySpawned();
        }
        if (!this.spawned.isEmpty()) {
            this.checkSpawns();
        }
        return this.target != null;
    }

    @Override
    public boolean aiContinueExecute() {
        return this.aiShouldExecute();
    }

    @Override
    public void resetTask() {
        this.reset();
    }

    @Override
    public void aiStartExecuting() {
        this.number = 0;
        for (EntityLivingBase entity : this.spawned) {
            int i = entity.getEntityData().func_74762_e("NpcSpawnerNr");
            if (i > this.number) {
                this.number = i;
            }
            this.setTarget(entity, this.npc.func_70638_az());
        }
    }

    @Override
    public void reset() {
        this.number = 0;
        if (this.spawned.isEmpty()) {
            this.spawned = this.getNearbySpawned();
        }
        this.target = null;
        this.checkSpawns();
    }

    @Override
    public void killed() {
        this.reset();
    }

    private EntityLivingBase spawnEntity(NBTTagCompound compound) {
        double z;
        double y;
        if (compound == null || !compound.func_74764_b("id")) {
            return null;
        }
        double x = this.npc.field_70165_t + (double)this.xOffset - 0.5 + (double)this.npc.func_70681_au().nextFloat();
        Entity entity = NoppesUtilServer.spawnClone(compound, x, y = this.npc.field_70163_u + (double)this.yOffset, z = this.npc.field_70161_v + (double)this.zOffset - 0.5 + (double)this.npc.func_70681_au().nextFloat(), this.npc.field_70170_p);
        if (entity == null || !(entity instanceof EntityLivingBase)) {
            return null;
        }
        EntityLivingBase living = (EntityLivingBase)entity;
        living.getEntityData().func_74778_a("NpcSpawnerId", this.id);
        living.getEntityData().func_74768_a("NpcSpawnerNr", this.number);
        this.setTarget(living, this.npc.func_70638_az());
        living.func_70107_b(x, y, z);
        if (living instanceof EntityNPCInterface) {
            EntityNPCInterface snpc = (EntityNPCInterface)living;
            snpc.stats.spawnCycle = 4;
            snpc.stats.respawnTime = 0;
            snpc.ais.returnToStart = false;
        }
        this.spawned.add(living);
        return living;
    }

    private NBTTagCompound getCompound(int i) {
        if (i <= 1 && this.compound1 != null && this.compound1.func_74764_b("id")) {
            this.number = 1;
            return this.compound1;
        }
        if (i <= 2 && this.compound2 != null && this.compound2.func_74764_b("id")) {
            this.number = 2;
            return this.compound2;
        }
        if (i <= 3 && this.compound3 != null && this.compound3.func_74764_b("id")) {
            this.number = 3;
            return this.compound3;
        }
        if (i <= 4 && this.compound4 != null && this.compound4.func_74764_b("id")) {
            this.number = 4;
            return this.compound4;
        }
        if (i <= 5 && this.compound5 != null && this.compound5.func_74764_b("id")) {
            this.number = 5;
            return this.compound5;
        }
        if (i <= 6 && this.compound6 != null && this.compound6.func_74764_b("id")) {
            this.number = 6;
            return this.compound6;
        }
        return null;
    }

    private List<EntityLivingBase> getNearbySpawned() {
        ArrayList<EntityLivingBase> spawnList = new ArrayList<EntityLivingBase>();
        List list = this.npc.field_70170_p.func_72872_a(EntityLivingBase.class, this.npc.func_174813_aQ().func_72314_b(40.0, 40.0, 40.0));
        for (EntityLivingBase entity : list) {
            if (!entity.getEntityData().func_74779_i("NpcSpawnerId").equals(this.id) || entity.field_70128_L) continue;
            spawnList.add(entity);
        }
        return spawnList;
    }

    public boolean isOnCooldown(String name) {
        if (!this.cooldown.containsKey(name)) {
            return false;
        }
        long time = this.cooldown.get(name);
        return System.currentTimeMillis() < time + 1200000L;
    }

    public boolean hasPixelmon() {
        return this.compound1 != null && this.compound1.func_74779_i("id").equals("pixelmontainer");
    }

    @Override
    public IEntityLivingBase spawnEntity(int i) {
        NBTTagCompound compound = this.getCompound(i + 1);
        if (compound == null) {
            return null;
        }
        EntityLivingBase base = this.spawnEntity(compound);
        if (base == null) {
            return null;
        }
        return (IEntityLivingBase)NpcAPI.Instance().getIEntity((Entity)base);
    }

    @Override
    public void removeAllSpawned() {
        for (EntityLivingBase entity : this.spawned) {
            entity.field_70128_L = true;
        }
        this.spawned = new ArrayList<EntityLivingBase>();
    }
}

