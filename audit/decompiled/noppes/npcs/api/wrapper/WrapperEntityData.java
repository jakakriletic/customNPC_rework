/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLiving
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.item.EntityItem
 *  net.minecraft.entity.monster.EntityMob
 *  net.minecraft.entity.passive.EntityAnimal
 *  net.minecraft.entity.passive.EntityTameable
 *  net.minecraft.entity.passive.EntityVillager
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.entity.projectile.EntityArrow
 *  net.minecraft.entity.projectile.EntityThrowable
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.ResourceLocation
 *  net.minecraftforge.common.capabilities.Capability
 *  net.minecraftforge.common.capabilities.CapabilityInject
 *  net.minecraftforge.common.capabilities.ICapabilityProvider
 *  net.minecraftforge.event.AttachCapabilitiesEvent
 */
package noppes.npcs.api.wrapper;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import noppes.npcs.LogWriter;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.wrapper.AnimalWrapper;
import noppes.npcs.api.wrapper.ArrowWrapper;
import noppes.npcs.api.wrapper.EntityItemWrapper;
import noppes.npcs.api.wrapper.EntityLivingBaseWrapper;
import noppes.npcs.api.wrapper.EntityLivingWrapper;
import noppes.npcs.api.wrapper.EntityWrapper;
import noppes.npcs.api.wrapper.MonsterWrapper;
import noppes.npcs.api.wrapper.PixelmonWrapper;
import noppes.npcs.api.wrapper.PlayerWrapper;
import noppes.npcs.api.wrapper.ProjectileWrapper;
import noppes.npcs.api.wrapper.ThrowableWrapper;
import noppes.npcs.api.wrapper.VillagerWrapper;
import noppes.npcs.controllers.PixelmonHelper;
import noppes.npcs.entity.EntityProjectile;

public class WrapperEntityData
implements ICapabilityProvider {
    @CapabilityInject(value=WrapperEntityData.class)
    public static Capability<WrapperEntityData> ENTITYDATA_CAPABILITY = null;
    public IEntity base;
    private static final ResourceLocation key = new ResourceLocation("customnpcs", "entitydata");

    public WrapperEntityData(IEntity base) {
        this.base = base;
    }

    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == ENTITYDATA_CAPABILITY;
    }

    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (this.hasCapability(capability, facing)) {
            return (T)this;
        }
        return null;
    }

    public static IEntity get(Entity entity) {
        if (entity == null) {
            return null;
        }
        WrapperEntityData data = (WrapperEntityData)entity.getCapability(ENTITYDATA_CAPABILITY, null);
        if (data == null) {
            LogWriter.warn("Unable to get EntityData for " + entity);
            return WrapperEntityData.getData((Entity)entity).base;
        }
        return data.base;
    }

    public static void register(AttachCapabilitiesEvent<Entity> event) {
        event.addCapability(key, (ICapabilityProvider)WrapperEntityData.getData((Entity)event.getObject()));
    }

    private static WrapperEntityData getData(Entity entity) {
        if (entity == null || entity.field_70170_p == null || entity.field_70170_p.field_72995_K) {
            return null;
        }
        if (entity instanceof EntityPlayerMP) {
            return new WrapperEntityData(new PlayerWrapper<EntityPlayerMP>((EntityPlayerMP)entity));
        }
        if (PixelmonHelper.isPixelmon(entity)) {
            return new WrapperEntityData(new PixelmonWrapper<EntityTameable>((EntityTameable)entity));
        }
        if (entity instanceof EntityAnimal) {
            return new WrapperEntityData(new AnimalWrapper<EntityAnimal>((EntityAnimal)entity));
        }
        if (entity instanceof EntityMob) {
            return new WrapperEntityData(new MonsterWrapper<EntityMob>((EntityMob)entity));
        }
        if (entity instanceof EntityLiving) {
            return new WrapperEntityData(new EntityLivingWrapper<EntityLiving>((EntityLiving)entity));
        }
        if (entity instanceof EntityLivingBase) {
            return new WrapperEntityData(new EntityLivingBaseWrapper<EntityLivingBase>((EntityLivingBase)entity));
        }
        if (entity instanceof EntityVillager) {
            return new WrapperEntityData(new VillagerWrapper<EntityVillager>((EntityVillager)entity));
        }
        if (entity instanceof EntityItem) {
            return new WrapperEntityData(new EntityItemWrapper<EntityItem>((EntityItem)entity));
        }
        if (entity instanceof EntityProjectile) {
            return new WrapperEntityData(new ProjectileWrapper<EntityProjectile>((EntityProjectile)entity));
        }
        if (entity instanceof EntityThrowable) {
            return new WrapperEntityData(new ThrowableWrapper<EntityThrowable>((EntityThrowable)entity));
        }
        if (entity instanceof EntityArrow) {
            return new WrapperEntityData(new ArrowWrapper<EntityArrow>((EntityArrow)entity));
        }
        return new WrapperEntityData(new EntityWrapper<Entity>(entity));
    }
}

