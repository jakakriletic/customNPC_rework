/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraftforge.fml.common.registry.EntityEntry
 *  net.minecraftforge.fml.common.registry.ForgeRegistries
 */
package noppes.npcs;

import java.util.HashMap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import noppes.npcs.ModelEyeData;
import noppes.npcs.ModelPartConfig;
import noppes.npcs.ModelPartData;
import noppes.npcs.constants.EnumParts;

public class ModelDataShared {
    protected ModelPartConfig arm1 = new ModelPartConfig();
    protected ModelPartConfig arm2 = new ModelPartConfig();
    protected ModelPartConfig body = new ModelPartConfig();
    protected ModelPartConfig leg1 = new ModelPartConfig();
    protected ModelPartConfig leg2 = new ModelPartConfig();
    protected ModelPartConfig head = new ModelPartConfig();
    protected ModelPartData legParts = new ModelPartData("legs");
    public ModelEyeData eyes = new ModelEyeData();
    public Class<? extends EntityLivingBase> entityClass;
    protected EntityLivingBase entity;
    public NBTTagCompound extra = new NBTTagCompound();
    protected HashMap<EnumParts, ModelPartData> parts = new HashMap();

    public NBTTagCompound writeToNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        if (this.entityClass != null) {
            compound.func_74778_a("EntityClass", this.entityClass.getCanonicalName());
        }
        compound.func_74782_a("ArmsConfig", (NBTBase)this.arm1.writeToNBT());
        compound.func_74782_a("BodyConfig", (NBTBase)this.body.writeToNBT());
        compound.func_74782_a("LegsConfig", (NBTBase)this.leg1.writeToNBT());
        compound.func_74782_a("HeadConfig", (NBTBase)this.head.writeToNBT());
        compound.func_74782_a("LegParts", (NBTBase)this.legParts.writeToNBT());
        compound.func_74782_a("Eyes", (NBTBase)this.eyes.writeToNBT());
        compound.func_74782_a("ExtraData", (NBTBase)this.extra);
        NBTTagList list = new NBTTagList();
        for (EnumParts e : this.parts.keySet()) {
            NBTTagCompound item = this.parts.get((Object)e).writeToNBT();
            item.func_74778_a("PartName", e.name);
            list.func_74742_a((NBTBase)item);
        }
        compound.func_74782_a("Parts", (NBTBase)list);
        return compound;
    }

    public void readFromNBT(NBTTagCompound compound) {
        this.setEntityName(compound.func_74779_i("EntityClass"));
        this.arm1.readFromNBT(compound.func_74775_l("ArmsConfig"));
        this.body.readFromNBT(compound.func_74775_l("BodyConfig"));
        this.leg1.readFromNBT(compound.func_74775_l("LegsConfig"));
        this.head.readFromNBT(compound.func_74775_l("HeadConfig"));
        this.legParts.readFromNBT(compound.func_74775_l("LegParts"));
        this.eyes.readFromNBT(compound.func_74775_l("Eyes"));
        this.extra = compound.func_74775_l("ExtraData");
        HashMap<EnumParts, ModelPartData> parts = new HashMap<EnumParts, ModelPartData>();
        NBTTagList list = compound.func_150295_c("Parts", 10);
        for (int i = 0; i < list.func_74745_c(); ++i) {
            NBTTagCompound item = list.func_150305_b(i);
            String name = item.func_74779_i("PartName");
            ModelPartData part = new ModelPartData(name);
            part.readFromNBT(item);
            EnumParts e = EnumParts.FromName(name);
            if (e == null) continue;
            parts.put(e, part);
        }
        this.parts = parts;
        this.updateTransate();
    }

    private void updateTransate() {
        for (EnumParts part : EnumParts.values()) {
            float y;
            float x;
            ModelPartConfig body;
            ModelPartConfig config = this.getPartConfig(part);
            if (config == null) continue;
            if (part == EnumParts.HEAD) {
                config.setTranslate(0.0f, this.getBodyY(), 0.0f);
                continue;
            }
            if (part == EnumParts.ARM_LEFT) {
                body = this.getPartConfig(EnumParts.BODY);
                x = (1.0f - body.scaleX) * 0.25f + (1.0f - config.scaleX) * 0.075f;
                y = this.getBodyY() + (1.0f - config.scaleY) * -0.1f;
                config.setTranslate(-x, y, 0.0f);
                if (config.notShared) continue;
                ModelPartConfig arm = this.getPartConfig(EnumParts.ARM_RIGHT);
                arm.copyValues(config);
                continue;
            }
            if (part == EnumParts.ARM_RIGHT) {
                body = this.getPartConfig(EnumParts.BODY);
                x = (1.0f - body.scaleX) * 0.25f + (1.0f - config.scaleX) * 0.075f;
                y = this.getBodyY() + (1.0f - config.scaleY) * -0.1f;
                config.setTranslate(x, y, 0.0f);
                continue;
            }
            if (part == EnumParts.LEG_LEFT) {
                config.setTranslate(config.scaleX * 0.125f - 0.113f, this.getLegsY(), 0.0f);
                if (config.notShared) continue;
                ModelPartConfig leg = this.getPartConfig(EnumParts.LEG_RIGHT);
                leg.copyValues(config);
                continue;
            }
            if (part == EnumParts.LEG_RIGHT) {
                config.setTranslate((1.0f - config.scaleX) * 0.125f, this.getLegsY(), 0.0f);
                continue;
            }
            if (part != EnumParts.BODY) continue;
            config.setTranslate(0.0f, this.getBodyY(), 0.0f);
        }
    }

    public void setEntityName(String string) {
        this.entityClass = null;
        this.entity = null;
        for (EntityEntry ent : ForgeRegistries.ENTITIES.getValues()) {
            Class c = ent.getEntityClass();
            if (!c.getCanonicalName().equals(string) || !EntityLivingBase.class.isAssignableFrom(c)) continue;
            this.entityClass = c.asSubclass(EntityLivingBase.class);
            break;
        }
    }

    public void setEntityClass(Class<? extends EntityLivingBase> entityClass) {
        this.entityClass = entityClass;
        this.entity = null;
        this.extra = new NBTTagCompound();
    }

    public Class<? extends EntityLivingBase> getEntityClass() {
        return this.entityClass;
    }

    public float offsetY() {
        if (this.entity == null) {
            return -this.getBodyY();
        }
        return this.entity.field_70131_O - 1.8f;
    }

    public void clearEntity() {
        this.entity = null;
    }

    public ModelPartData getPartData(EnumParts type) {
        if (type == EnumParts.LEGS) {
            return this.legParts;
        }
        if (type == EnumParts.EYES) {
            return this.eyes;
        }
        return this.parts.get((Object)type);
    }

    public ModelPartConfig getPartConfig(EnumParts type) {
        if (type == EnumParts.BODY) {
            return this.body;
        }
        if (type == EnumParts.ARM_LEFT) {
            return this.arm1;
        }
        if (type == EnumParts.ARM_RIGHT) {
            return this.arm2;
        }
        if (type == EnumParts.LEG_LEFT) {
            return this.leg1;
        }
        if (type == EnumParts.LEG_RIGHT) {
            return this.leg2;
        }
        return this.head;
    }

    public void removePart(EnumParts type) {
        this.parts.remove((Object)type);
    }

    public ModelPartData getOrCreatePart(EnumParts type) {
        if (type == null) {
            return null;
        }
        if (type == EnumParts.EYES) {
            return this.eyes;
        }
        ModelPartData part = this.getPartData(type);
        if (part == null) {
            part = new ModelPartData(type.name);
            this.parts.put(type, part);
        }
        return part;
    }

    public float getBodyY() {
        return (1.0f - this.body.scaleY) * 0.75f + this.getLegsY();
    }

    public float getLegsY() {
        ModelPartConfig legs = this.leg1;
        if (this.leg2.notShared && this.leg2.scaleY > this.leg1.scaleY) {
            legs = this.leg2;
        }
        return (1.0f - legs.scaleY) * 0.75f;
    }
}

