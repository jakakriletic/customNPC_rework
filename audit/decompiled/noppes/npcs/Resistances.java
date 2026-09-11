/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.DamageSource
 */
package noppes.npcs;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;

public class Resistances {
    public float knockback = 1.0f;
    public float arrow = 1.0f;
    public float melee = 1.0f;
    public float explosion = 1.0f;

    public NBTTagCompound writeToNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.func_74776_a("Knockback", this.knockback);
        compound.func_74776_a("Arrow", this.arrow);
        compound.func_74776_a("Melee", this.melee);
        compound.func_74776_a("Explosion", this.explosion);
        return compound;
    }

    public void readToNBT(NBTTagCompound compound) {
        this.knockback = compound.func_74760_g("Knockback");
        this.arrow = compound.func_74760_g("Arrow");
        this.melee = compound.func_74760_g("Melee");
        this.explosion = compound.func_74760_g("Explosion");
    }

    public float applyResistance(DamageSource source, float damage) {
        if (source.field_76373_n.equals("arrow") || source.field_76373_n.equals("thrown") || source.func_76352_a()) {
            damage *= 2.0f - this.arrow;
        } else if (source.field_76373_n.equals("player") || source.field_76373_n.equals("mob")) {
            damage *= 2.0f - this.melee;
        } else if (source.field_76373_n.equals("explosion") || source.field_76373_n.equals("explosion.player")) {
            damage *= 2.0f - this.explosion;
        }
        return damage;
    }
}

