/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.ModelBiped
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.math.MathHelper
 */
package noppes.npcs.client.model.animation;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

public class AniHug {
    public static void setRotationAngles(float par1, float par2, float par3, float par4, float par5, float par6, Entity entity, ModelBiped base) {
        float f6 = MathHelper.func_76126_a((float)(base.field_78095_p * 3.141593f));
        float f7 = MathHelper.func_76126_a((float)((1.0f - (1.0f - base.field_78095_p) * (1.0f - base.field_78095_p)) * 3.141593f));
        base.field_178723_h.field_78808_h = 0.0f;
        base.field_178724_i.field_78808_h = 0.0f;
        base.field_178723_h.field_78796_g = -(0.1f - f6 * 0.6f);
        base.field_178724_i.field_78796_g = 0.1f;
        base.field_178723_h.field_78795_f = -1.570796f;
        base.field_178724_i.field_78795_f = -1.570796f;
        base.field_178723_h.field_78795_f -= f6 * 1.2f - f7 * 0.4f;
        base.field_178723_h.field_78808_h += MathHelper.func_76134_b((float)(par3 * 0.09f)) * 0.05f + 0.05f;
        base.field_178724_i.field_78808_h -= MathHelper.func_76134_b((float)(par3 * 0.09f)) * 0.05f + 0.05f;
        base.field_178723_h.field_78795_f += MathHelper.func_76126_a((float)(par3 * 0.067f)) * 0.05f;
        base.field_178724_i.field_78795_f -= MathHelper.func_76126_a((float)(par3 * 0.067f)) * 0.05f;
    }
}

