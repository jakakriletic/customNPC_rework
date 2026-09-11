/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.particle.Particle
 *  net.minecraft.client.renderer.BufferBuilder
 *  net.minecraft.entity.Entity
 *  net.minecraft.world.World
 */
package noppes.npcs.client.fx;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public class EntityRainbowFX
extends Particle {
    public static float[][] colorTable = new float[][]{{1.0f, 0.0f, 0.0f}, {1.0f, 0.5f, 0.0f}, {1.0f, 1.0f, 0.0f}, {0.0f, 1.0f, 0.0f}, {0.0f, 0.0f, 1.0f}, {0.0f, 4375.0f, 0.0f, 1.0f}, {0.5625f, 0.0f, 1.0f}};
    float reddustParticleScale;

    public EntityRainbowFX(World world, double d, double d1, double d2, double f, double f1, double f2) {
        this(world, d, d1, d2, 1.0f, f, f1, f2);
    }

    public EntityRainbowFX(World world, double d, double d1, double d2, float f, double f1, double f2, double f3) {
        super(world, d, d1, d2, 0.0, 0.0, 0.0);
        this.field_187129_i *= (double)0.1f;
        this.field_187130_j *= (double)0.1f;
        this.field_187131_k *= (double)0.1f;
        if (f1 == 0.0) {
            f1 = 1.0;
        }
        int i = world.field_73012_v.nextInt(colorTable.length);
        this.field_70552_h = colorTable[i][0];
        this.field_70553_i = colorTable[i][1];
        this.field_70551_j = colorTable[i][2];
        this.field_70544_f *= 0.75f;
        this.field_70544_f *= f;
        this.reddustParticleScale = this.field_70544_f;
        this.field_70547_e = (int)(16.0 / (Math.random() * 0.8 + 0.2));
        this.field_70547_e = (int)((float)this.field_70547_e * f);
    }

    public void func_180434_a(BufferBuilder tessellator, Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        float f6 = ((float)this.field_70546_d + f) / (float)this.field_70547_e * 32.0f;
        if (f6 < 0.0f) {
            f6 = 0.0f;
        } else if (f6 > 1.0f) {
            f6 = 1.0f;
        }
        this.field_70544_f = this.reddustParticleScale * f6;
        super.func_180434_a(tessellator, entity, f, f1, f2, f3, f4, f5);
    }

    public void func_189213_a() {
        this.field_187123_c = this.field_187126_f;
        this.field_187124_d = this.field_187127_g;
        this.field_187125_e = this.field_187128_h;
        if (this.field_70546_d++ >= this.field_70547_e) {
            this.func_187112_i();
        }
        this.func_70536_a(7 - this.field_70546_d * 8 / this.field_70547_e);
        this.func_187110_a(this.field_187129_i, this.field_187130_j, this.field_187131_k);
        if (this.field_187127_g == this.field_187124_d) {
            this.field_187129_i *= 1.1;
            this.field_187131_k *= 1.1;
        }
        this.field_187129_i *= (double)0.96f;
        this.field_187130_j *= (double)0.96f;
        this.field_187131_k *= (double)0.96f;
        if (this.field_187132_l) {
            this.field_187129_i *= (double)0.7f;
            this.field_187131_k *= (double)0.7f;
        }
    }
}

