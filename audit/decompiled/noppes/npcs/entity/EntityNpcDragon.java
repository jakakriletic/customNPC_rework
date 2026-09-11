/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.EnumParticleTypes
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.world.World
 */
package noppes.npcs.entity;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import noppes.npcs.ModelData;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityNpcDragon
extends EntityNPCInterface {
    public double[][] field_40162_d = new double[64][3];
    public int field_40164_e = -1;
    public float field_40173_aw = 0.0f;
    public float field_40172_ax = 0.0f;
    public int field_40178_aA = 0;
    public boolean isFlying = false;
    private boolean exploded = false;

    public EntityNpcDragon(World world) {
        super(world);
        this.scaleX = 0.4f;
        this.scaleY = 0.4f;
        this.scaleZ = 0.4f;
        this.display.setSkinTexture("customnpcs:textures/entity/dragon/BlackDragon.png");
        this.field_70130_N = 1.8f;
        this.field_70131_O = 1.4f;
    }

    public double func_70042_X() {
        return 1.1;
    }

    public double[] func_40160_a(int i, float f) {
        double d1;
        f = 1.0f - f;
        int j = this.field_40164_e - i * 1 & 0x3F;
        int k = this.field_40164_e - i * 1 - 1 & 0x3F;
        double[] ad = new double[3];
        double d = this.field_40162_d[j][0];
        for (d1 = this.field_40162_d[k][0] - d; d1 < -180.0; d1 += 360.0) {
        }
        while (d1 >= 180.0) {
            d1 -= 360.0;
        }
        ad[0] = d + d1 * (double)f;
        d = this.field_40162_d[j][1];
        d1 = this.field_40162_d[k][1] - d;
        ad[1] = d + d1 * (double)f;
        ad[2] = this.field_40162_d[j][2] + (this.field_40162_d[k][2] - this.field_40162_d[j][2]) * (double)f;
        return ad;
    }

    @Override
    public void func_70071_h_() {
        this.field_70128_L = true;
        this.func_94061_f(true);
        if (!this.field_70170_p.field_72995_K) {
            NBTTagCompound compound = new NBTTagCompound();
            this.func_189511_e(compound);
            EntityCustomNpc npc = new EntityCustomNpc(this.field_70170_p);
            npc.func_70020_e(compound);
            ModelData data = npc.modelData;
            data.setEntityClass(EntityNpcDragon.class);
            this.field_70170_p.func_72838_d((Entity)npc);
        }
        super.func_70071_h_();
    }

    @Override
    public void func_70636_d() {
        this.field_40173_aw = this.field_40172_ax;
        if (this.field_70170_p.field_72995_K && this.func_110143_aJ() <= 0.0f) {
            if (!this.exploded) {
                this.exploded = true;
                float f = (this.field_70146_Z.nextFloat() - 0.5f) * 8.0f;
                float f2 = (this.field_70146_Z.nextFloat() - 0.5f) * 4.0f;
                float f4 = (this.field_70146_Z.nextFloat() - 0.5f) * 8.0f;
                this.field_70170_p.func_175688_a(EnumParticleTypes.EXPLOSION_LARGE, this.field_70165_t + (double)f, this.field_70163_u + 2.0 + (double)f2, this.field_70161_v + (double)f4, 0.0, 0.0, 0.0, new int[0]);
            }
        } else {
            this.exploded = false;
            float f1 = 0.2f / (MathHelper.func_76133_a((double)(this.field_70159_w * this.field_70159_w + this.field_70179_y * this.field_70179_y)) * 10.0f + 1.0f);
            f1 = 0.045f;
            this.field_40172_ax += (f1 *= (float)Math.pow(2.0, this.field_70181_x)) * 0.5f;
        }
        super.func_70636_d();
    }

    @Override
    public void updateHitbox() {
        this.field_70130_N = 1.8f;
        this.field_70131_O = 1.4f;
    }
}

