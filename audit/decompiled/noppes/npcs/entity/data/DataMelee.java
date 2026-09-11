/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.SharedMonsterAttributes
 *  net.minecraft.nbt.NBTTagCompound
 */
package noppes.npcs.entity.data;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.api.entity.data.INPCMelee;
import noppes.npcs.entity.EntityNPCInterface;

public class DataMelee
implements INPCMelee {
    private EntityNPCInterface npc;
    private int attackStrength = 5;
    private int attackSpeed = 20;
    private int attackRange = 2;
    private int knockback = 0;
    private int potionType = 0;
    private int potionDuration = 5;
    private int potionAmp = 0;

    public DataMelee(EntityNPCInterface npc) {
        this.npc = npc;
    }

    public void readFromNBT(NBTTagCompound compound) {
        this.attackSpeed = compound.func_74762_e("AttackSpeed");
        this.setStrength(compound.func_74762_e("AttackStrenght"));
        this.attackRange = compound.func_74762_e("AttackRange");
        this.knockback = compound.func_74762_e("KnockBack");
        this.potionType = compound.func_74762_e("PotionEffect");
        this.potionDuration = compound.func_74762_e("PotionDuration");
        this.potionAmp = compound.func_74762_e("PotionAmp");
    }

    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.func_74768_a("AttackStrenght", this.attackStrength);
        compound.func_74768_a("AttackSpeed", this.attackSpeed);
        compound.func_74768_a("AttackRange", this.attackRange);
        compound.func_74768_a("KnockBack", this.knockback);
        compound.func_74768_a("PotionEffect", this.potionType);
        compound.func_74768_a("PotionDuration", this.potionDuration);
        compound.func_74768_a("PotionAmp", this.potionAmp);
        return compound;
    }

    @Override
    public int getStrength() {
        return this.attackStrength;
    }

    @Override
    public void setStrength(int strength) {
        this.attackStrength = strength;
        this.npc.func_110148_a(SharedMonsterAttributes.field_111264_e).func_111128_a((double)this.attackStrength);
    }

    @Override
    public int getDelay() {
        return this.attackSpeed;
    }

    @Override
    public void setDelay(int speed) {
        this.attackSpeed = speed;
    }

    @Override
    public int getRange() {
        return this.attackRange;
    }

    @Override
    public void setRange(int range) {
        this.attackRange = range;
    }

    @Override
    public int getKnockback() {
        return this.knockback;
    }

    @Override
    public void setKnockback(int knockback) {
        this.knockback = knockback;
    }

    @Override
    public int getEffectType() {
        return this.potionType;
    }

    @Override
    public int getEffectTime() {
        return this.potionDuration;
    }

    @Override
    public int getEffectStrength() {
        return this.potionAmp;
    }

    @Override
    public void setEffect(int type, int strength, int time) {
        this.potionType = type;
        this.potionDuration = time;
        this.potionAmp = strength;
    }
}

