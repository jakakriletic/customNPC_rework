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
    public void func_70037_a(NBTTagCompound compound) {
        if (compound.func_74764_b("NpcModelData")) {
            this.modelData.readFromNBT(compound.func_74775_l("NpcModelData"));
        }
        super.func_70037_a(compound);
    }

    @Override
    public void func_70014_b(NBTTagCompound compound) {
        super.func_70014_b(compound);
        compound.func_74782_a("NpcModelData", (NBTBase)this.modelData.writeToNBT());
    }

    public boolean func_70039_c(NBTTagCompound compound) {
        String s;
        boolean bo = super.func_184198_c(compound);
        if (bo && (s = this.func_70022_Q()).equals("minecraft:customnpcs.customnpc")) {
            compound.func_74778_a("id", "customnpcs:customnpc");
        }
        return bo;
    }

    @Override
    public void func_70071_h_() {
        super.func_70071_h_();
        if (this.isRemote()) {
            EntityLivingBase entity;
            ModelPartData particles = this.modelData.getPartData(EnumParts.PARTICLES);
            if (particles != null && !this.isKilled()) {
                CustomNpcs.proxy.spawnParticle((EntityLivingBase)this, "ModelData", this.modelData, particles);
            }
            if ((entity = this.modelData.getEntity(this)) != null) {
                try {
                    entity.func_70071_h_();
                }
                catch (Exception exception) {
                    // empty catch block
                }
                EntityUtil.Copy((EntityLivingBase)this, entity);
            }
        }
        this.modelData.eyes.update(this);
    }

    public boolean func_184205_a(Entity par1Entity, boolean force) {
        boolean b = super.func_184205_a(par1Entity, force);
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
            this.field_70130_N = entity.field_70130_N / 5.0f * (float)this.display.getSize();
            this.field_70131_O = entity.field_70131_O / 5.0f * (float)this.display.getSize();
            if (this.field_70130_N < 0.1f) {
                this.field_70130_N = 0.1f;
            }
            if (this.field_70131_O < 0.1f) {
                this.field_70131_O = 0.1f;
            }
            if (!this.display.getHasHitbox() || this.isKilled() && this.stats.hideKilledBody) {
                this.field_70130_N = 1.0E-5f;
            }
            if ((double)(this.field_70130_N / 2.0f) > World.MAX_ENTITY_RADIUS) {
                World.MAX_ENTITY_RADIUS = this.field_70130_N / 2.0f;
            }
            this.func_70107_b(this.field_70165_t, this.field_70163_u, this.field_70161_v);
        }
    }
}

