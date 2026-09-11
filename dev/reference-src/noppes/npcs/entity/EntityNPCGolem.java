/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.world.World
 */
package noppes.npcs.entity;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import noppes.npcs.ModelData;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityNPCGolem
extends EntityNPCInterface {
    public EntityNPCGolem(World world) {
        super(world);
        this.display.setSkinTexture("customnpcs:textures/entity/golem/Iron Golem.png");
        this.width = 1.4f;
        this.height = 2.5f;
    }

    @Override
    public void updateHitbox() {
        this.currentAnimation = (Integer)this.dataManager.get(Animation);
        if (this.currentAnimation == 2) {
            this.height = 0.5f;
            this.width = 0.5f;
        } else if (this.currentAnimation == 1) {
            this.width = 1.4f;
            this.height = 2.0f;
        } else {
            this.width = 1.4f;
            this.height = 2.5f;
        }
    }

    @Override
    public void onUpdate() {
        this.isDead = true;
        this.setNoAI(true);
        if (!this.world.isRemote) {
            NBTTagCompound compound = new NBTTagCompound();
            this.writeToNBT(compound);
            EntityCustomNpc npc = new EntityCustomNpc(this.world);
            npc.readFromNBT(compound);
            ModelData data = npc.modelData;
            data.setEntityClass(EntityNPCGolem.class);
            this.world.spawnEntity((Entity)npc);
        }
        super.onUpdate();
    }
}

