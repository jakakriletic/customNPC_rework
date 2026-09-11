/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.world.World
 */
package noppes.npcs.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import noppes.npcs.CustomNpcs;
import noppes.npcs.ModelData;
import noppes.npcs.ModelPartData;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.entity.EntityNPCFlying;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityCustomNpc
extends EntityNPCFlying {
    public ModelData modelData = new ModelData();

    public EntityCustomNpc(World world) {
        super(world);
        if (!CustomNpcs.EnableDefaultEyes) {
            this.modelData.eyes.type = (byte)-1;
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        if (compound.hasKey("NpcModelData")) {
            this.modelData.readFromNBT(compound.getCompoundTag("NpcModelData"));
        }
        super.readEntityFromNBT(compound);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setTag("NpcModelData", (NBTBase)this.modelData.writeToNBT());
    }

    public boolean writeToNBTOptional(NBTTagCompound compound) {
        String s;
        boolean bo = super.writeToNBTAtomically(compound);
        if (bo && (s = this.getEntityString()).equals("minecraft:customnpcs.customnpc")) {
            compound.setString("id", "customnpcs:customnpc");
        }
        return bo;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.isRemote()) {
            EntityLivingBase entity;
            ModelPartData particles = this.modelData.getPartData(EnumParts.PARTICLES);
            if (particles != null && !this.isKilled()) {
                CustomNpcs.proxy.spawnParticle((EntityLivingBase)this, "ModelData", this.modelData, particles);
            }
            if ((entity = this.modelData.getEntity(this)) != null) {
                try {
                    entity.onUpdate();
                }
                catch (Exception exception) {
                    // empty catch block
                }
                EntityUtil.Copy((EntityLivingBase)this, entity);
            }
        }
        this.modelData.eyes.update(this);
    }

    public boolean startRiding(Entity par1Entity, boolean force) {
        boolean b = super.startRiding(par1Entity, force);
        this.updateHitbox();
        return b;
    }

    @Override
    public void updateHitbox() {
        EntityLivingBase entity = this.modelData.getEntity(this);
        if (this.modelData == null || entity == null) {
            this.baseHeight = 1.9f - this.modelData.getBodyY() + (this.modelData.getPartConfig((EnumParts)EnumParts.HEAD).scaleY - 1.0f) / 2.0f;
            super.updateHitbox();
        } else {
            if (entity instanceof EntityNPCInterface) {
                ((EntityNPCInterface)entity).updateHitbox();
            }
            this.width = entity.width / 5.0f * (float)this.display.getSize();
            this.height = entity.height / 5.0f * (float)this.display.getSize();
            if (this.width < 0.1f) {
                this.width = 0.1f;
            }
            if (this.height < 0.1f) {
                this.height = 0.1f;
            }
            if (!this.display.getHasHitbox() || this.isKilled() && this.stats.hideKilledBody) {
                this.width = 1.0E-5f;
            }
            if ((double)(this.width / 2.0f) > World.MAX_ENTITY_RADIUS) {
                World.MAX_ENTITY_RADIUS = this.width / 2.0f;
            }
            this.setPosition(this.posX, this.posY, this.posZ);
        }
    }
}

