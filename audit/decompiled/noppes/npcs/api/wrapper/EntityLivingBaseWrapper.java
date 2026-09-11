/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.SharedMonsterAttributes
 *  net.minecraft.inventory.EntityEquipmentSlot
 *  net.minecraft.item.ItemStack
 *  net.minecraft.potion.Potion
 *  net.minecraft.potion.PotionEffect
 *  net.minecraft.util.EnumHand
 */
package noppes.npcs.api.wrapper;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumHand;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IEntityLivingBase;
import noppes.npcs.api.entity.data.IMark;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.EntityWrapper;
import noppes.npcs.controllers.data.MarkData;

public class EntityLivingBaseWrapper<T extends EntityLivingBase>
extends EntityWrapper<T>
implements IEntityLivingBase {
    public EntityLivingBaseWrapper(T entity) {
        super(entity);
    }

    @Override
    public float getHealth() {
        return ((EntityLivingBase)this.entity).func_110143_aJ();
    }

    @Override
    public void setHealth(float health) {
        ((EntityLivingBase)this.entity).func_70606_j(health);
    }

    @Override
    public float getMaxHealth() {
        return ((EntityLivingBase)this.entity).func_110138_aP();
    }

    @Override
    public void setMaxHealth(float health) {
        if (health < 0.0f) {
            return;
        }
        ((EntityLivingBase)this.entity).func_110148_a(SharedMonsterAttributes.field_111267_a).func_111128_a((double)health);
    }

    @Override
    public boolean isAttacking() {
        return ((EntityLivingBase)this.entity).func_70643_av() != null;
    }

    @Override
    public void setAttackTarget(IEntityLivingBase living) {
        if (living == null) {
            ((EntityLivingBase)this.entity).func_70604_c(null);
        } else {
            ((EntityLivingBase)this.entity).func_70604_c(living.getMCEntity());
        }
    }

    @Override
    public IEntityLivingBase getAttackTarget() {
        return (IEntityLivingBase)NpcAPI.Instance().getIEntity((Entity)((EntityLivingBase)this.entity).func_70643_av());
    }

    @Override
    public IEntityLivingBase getLastAttacked() {
        return (IEntityLivingBase)NpcAPI.Instance().getIEntity((Entity)((EntityLivingBase)this.entity).func_110144_aD());
    }

    @Override
    public int getLastAttackedTime() {
        return ((EntityLivingBase)this.entity).func_142013_aG();
    }

    @Override
    public boolean canSeeEntity(IEntity entity) {
        return ((EntityLivingBase)this.entity).func_70685_l(entity.getMCEntity());
    }

    @Override
    public void swingMainhand() {
        ((EntityLivingBase)this.entity).func_184609_a(EnumHand.MAIN_HAND);
    }

    @Override
    public void swingOffhand() {
        ((EntityLivingBase)this.entity).func_184609_a(EnumHand.OFF_HAND);
    }

    @Override
    public void addPotionEffect(int effect, int duration, int strength, boolean hideParticles) {
        Potion p = Potion.func_188412_a((int)effect);
        if (p == null) {
            return;
        }
        if (strength < 0) {
            strength = 0;
        } else if (strength > 255) {
            strength = 255;
        }
        if (duration < 0) {
            duration = 0;
        } else if (duration > 1000000) {
            duration = 1000000;
        }
        if (!p.func_76403_b()) {
            duration *= 20;
        }
        if (duration == 0) {
            ((EntityLivingBase)this.entity).func_184589_d(p);
        } else {
            ((EntityLivingBase)this.entity).func_70690_d(new PotionEffect(p, duration, strength, false, hideParticles));
        }
    }

    @Override
    public void clearPotionEffects() {
        ((EntityLivingBase)this.entity).func_70674_bp();
    }

    @Override
    public int getPotionEffect(int effect) {
        PotionEffect pf = ((EntityLivingBase)this.entity).func_70660_b(Potion.func_188412_a((int)effect));
        if (pf == null) {
            return -1;
        }
        return pf.func_76458_c();
    }

    @Override
    public IItemStack getMainhandItem() {
        return NpcAPI.Instance().getIItemStack(((EntityLivingBase)this.entity).func_184614_ca());
    }

    @Override
    public void setMainhandItem(IItemStack item) {
        ((EntityLivingBase)this.entity).func_184611_a(EnumHand.MAIN_HAND, item == null ? ItemStack.field_190927_a : item.getMCItemStack());
    }

    @Override
    public IItemStack getOffhandItem() {
        return NpcAPI.Instance().getIItemStack(((EntityLivingBase)this.entity).func_184592_cb());
    }

    @Override
    public void setOffhandItem(IItemStack item) {
        ((EntityLivingBase)this.entity).func_184611_a(EnumHand.OFF_HAND, item == null ? ItemStack.field_190927_a : item.getMCItemStack());
    }

    @Override
    public IItemStack getArmor(int slot) {
        if (slot < 0 || slot > 3) {
            throw new CustomNPCsException("Wrong slot id:" + slot, new Object[0]);
        }
        return NpcAPI.Instance().getIItemStack(((EntityLivingBase)this.entity).func_184582_a(this.getSlot(slot)));
    }

    @Override
    public void setArmor(int slot, IItemStack item) {
        if (slot < 0 || slot > 3) {
            throw new CustomNPCsException("Wrong slot id:" + slot, new Object[0]);
        }
        ((EntityLivingBase)this.entity).func_184201_a(this.getSlot(slot), item == null ? ItemStack.field_190927_a : item.getMCItemStack());
    }

    private EntityEquipmentSlot getSlot(int slot) {
        if (slot == 3) {
            return EntityEquipmentSlot.HEAD;
        }
        if (slot == 2) {
            return EntityEquipmentSlot.CHEST;
        }
        if (slot == 1) {
            return EntityEquipmentSlot.LEGS;
        }
        if (slot == 0) {
            return EntityEquipmentSlot.FEET;
        }
        return null;
    }

    @Override
    public float getRotation() {
        return ((EntityLivingBase)this.entity).field_70761_aq;
    }

    @Override
    public void setRotation(float rotation) {
        ((EntityLivingBase)this.entity).field_70761_aq = rotation;
    }

    @Override
    public int getType() {
        return 5;
    }

    @Override
    public boolean typeOf(int type) {
        return type == 5 ? true : super.typeOf(type);
    }

    @Override
    public boolean isChild() {
        return ((EntityLivingBase)this.entity).func_70631_g_();
    }

    @Override
    public IMark addMark(int type) {
        MarkData data = MarkData.get((EntityLivingBase)this.entity);
        return data.addMark(type);
    }

    @Override
    public void removeMark(IMark mark) {
        MarkData data = MarkData.get((EntityLivingBase)this.entity);
        data.marks.remove(mark);
        data.syncClients();
    }

    @Override
    public IMark[] getMarks() {
        MarkData data = MarkData.get((EntityLivingBase)this.entity);
        return data.marks.toArray(new IMark[data.marks.size()]);
    }

    @Override
    public float getMoveForward() {
        return ((EntityLivingBase)this.entity).field_191988_bg;
    }

    @Override
    public void setMoveForward(float move) {
        ((EntityLivingBase)this.entity).field_191988_bg = move;
    }

    @Override
    public float getMoveStrafing() {
        return ((EntityLivingBase)this.entity).field_70702_br;
    }

    @Override
    public void setMoveStrafing(float move) {
        ((EntityLivingBase)this.entity).field_70702_br = move;
    }

    @Override
    public float getMoveVertical() {
        return ((EntityLivingBase)this.entity).field_70701_bs;
    }

    @Override
    public void setMoveVertical(float move) {
        ((EntityLivingBase)this.entity).field_70701_bs = move;
    }
}

