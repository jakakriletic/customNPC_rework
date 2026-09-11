/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.math.MathHelper
 */
package noppes.npcs.client.model;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.client.model.ModelPlayerAlt;

public class ModelClassicPlayer
extends ModelPlayerAlt {
    public ModelClassicPlayer(float scale) {
        super(scale, false);
    }

    @Override
    public void func_78087_a(float par1, float par2, float par3, float par4, float par5, float par6, Entity entity) {
        super.func_78087_a(par1, par2, par3, par4, par5, par6, entity);
        float j = 2.0f;
        if (entity.func_70051_ag()) {
            j = 1.0f;
        }
        this.field_178723_h.field_78795_f += MathHelper.func_76134_b((float)(par1 * 0.6662f + (float)Math.PI)) * j * par2;
        this.field_178724_i.field_78795_f += MathHelper.func_76134_b((float)(par1 * 0.6662f)) * j * par2;
        this.field_178724_i.field_78808_h += (MathHelper.func_76134_b((float)(par1 * 0.2812f)) - 1.0f) * par2;
        this.field_178723_h.field_78808_h += (MathHelper.func_76134_b((float)(par1 * 0.2312f)) + 1.0f) * par2;
        this.field_178734_a.field_78795_f = this.field_178724_i.field_78795_f;
        this.field_178734_a.field_78796_g = this.field_178724_i.field_78796_g;
        this.field_178734_a.field_78808_h = this.field_178724_i.field_78808_h;
        this.field_178732_b.field_78795_f = this.field_178723_h.field_78795_f;
        this.field_178732_b.field_78796_g = this.field_178723_h.field_78796_g;
        this.field_178732_b.field_78808_h = this.field_178723_h.field_78808_h;
    }
}

