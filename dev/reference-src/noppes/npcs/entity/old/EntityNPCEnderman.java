/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.monster.EntityEnderman
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.world.World
 */
package noppes.npcs.entity.old;

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import noppes.npcs.ModelData;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.old.EntityNpcEnderchibi;

public class EntityNPCEnderman
extends EntityNpcEnderchibi {
    public EntityNPCEnderman(World world) {
        super(world);
        this.display.setSkinTexture("customnpcs:textures/entity/enderman/enderman.png");
        this.display.setOverlayTexture("customnpcs:textures/overlays/ender_eyes.png");
        this.width = 0.6f;
        this.height = 2.9f;
    }

    @Override
    public void updateHitbox() {
        if (this.currentAnimation == 2) {
            this.height = 0.2f;
            this.width = 0.2f;
        } else if (this.currentAnimation == 1) {
            this.width = 0.6f;
            this.height = 2.3f;
        } else {
            this.width = 0.6f;
            this.height = 2.9f;
        }
        this.width = this.width / 5.0f * (float)this.display.getSize();
        this.height = this.height / 5.0f * (float)this.display.getSize();
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
            data.setEntityClass(EntityEnderman.class);
            this.world.spawnEntity((Entity)npc);
        }
        super.onUpdate();
    }
}

