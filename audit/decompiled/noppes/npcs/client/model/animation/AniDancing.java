/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.model.ModelBiped
 *  net.minecraft.entity.Entity
 */
package noppes.npcs.client.model.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;

public class AniDancing {
    public static void setRotationAngles(float par1, float par2, float par3, float par4, float par5, float par6, Entity entity, ModelBiped model) {
        float dancing = (float)entity.field_70173_aa / 4.0f;
        float dancing2 = (float)(entity.field_70173_aa + 1) / 4.0f;
        dancing += (dancing2 - dancing) * Minecraft.func_71410_x().func_184121_ak();
        float x = (float)Math.sin(dancing);
        float y = (float)Math.abs(Math.cos(dancing));
        model.field_178720_f.field_78800_c = model.field_78116_c.field_78800_c = x * 0.75f;
        model.field_178720_f.field_78797_d = model.field_78116_c.field_78797_d = y * 1.25f - 0.02f;
        model.field_178720_f.field_78798_e = model.field_78116_c.field_78798_e = -y * 0.75f;
        model.field_178724_i.field_78800_c += x * 0.25f;
        model.field_178724_i.field_78797_d += y * 1.25f;
        model.field_178723_h.field_78800_c += x * 0.25f;
        model.field_178723_h.field_78797_d += y * 1.25f;
        model.field_78115_e.field_78800_c = x * 0.25f;
    }
}

