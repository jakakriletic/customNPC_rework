/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.model.ModelRenderer
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.math.MathHelper
 */
package noppes.npcs.client.model.part.legs;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

public class ModelMermaidLegs
extends ModelRenderer {
    private ModelRenderer top;
    private ModelRenderer middle;
    private ModelRenderer bottom;
    private ModelRenderer fin1;
    private ModelRenderer fin2;

    public ModelMermaidLegs(ModelBase base) {
        super(base);
        this.field_78801_a = 64.0f;
        this.field_78799_b = 32.0f;
        this.top = new ModelRenderer(base, 0, 16);
        this.top.func_78789_a(-2.0f, -2.5f, -2.0f, 8, 9, 4);
        this.top.func_78793_a(-2.0f, 14.0f, 1.0f);
        this.setRotation(this.top, 0.26f, 0.0f, 0.0f);
        this.middle = new ModelRenderer(base, 28, 0);
        this.middle.func_78789_a(0.0f, 0.0f, 0.0f, 7, 6, 4);
        this.middle.func_78793_a(-1.5f, 6.5f, -1.0f);
        this.setRotation(this.middle, 0.86f, 0.0f, 0.0f);
        this.top.func_78792_a(this.middle);
        this.bottom = new ModelRenderer(base, 24, 16);
        this.bottom.func_78789_a(0.0f, 0.0f, 0.0f, 6, 7, 3);
        this.bottom.func_78793_a(0.5f, 6.0f, 0.5f);
        this.setRotation(this.bottom, 0.15f, 0.0f, 0.0f);
        this.middle.func_78792_a(this.bottom);
        this.fin1 = new ModelRenderer(base, 0, 0);
        this.fin1.func_78789_a(0.0f, 0.0f, 0.0f, 5, 9, 1);
        this.fin1.func_78793_a(0.0f, 4.5f, 1.0f);
        this.setRotation(this.fin1, 0.05f, 0.0f, 0.5911399f);
        this.bottom.func_78792_a(this.fin1);
        this.fin2 = new ModelRenderer(base, 0, 0);
        this.fin2.field_78809_i = true;
        this.fin2.func_78789_a(-5.0f, 0.0f, 0.0f, 5, 9, 1);
        this.fin2.func_78793_a(6.0f, 4.5f, 1.0f);
        this.setRotation(this.fin2, 0.05f, 0.0f, -0.591143f);
        this.bottom.func_78792_a(this.fin2);
    }

    public void func_78785_a(float f5) {
        if (this.field_78807_k || !this.field_78806_j) {
            return;
        }
        this.top.func_78785_a(f5);
    }

    private void setRotation(ModelRenderer model, float x, float y, float z) {
        model.field_78795_f = x;
        model.field_78796_g = y;
        model.field_78808_h = z;
    }

    public void setRotationAngles(float par1, float par2, float par3, float par4, float par5, float par6, Entity entity) {
        float ani = MathHelper.func_76126_a((float)(par1 * 0.6662f));
        if ((double)ani > 0.2) {
            ani /= 3.0f;
        }
        this.top.field_78795_f = 0.26f - ani * 0.2f * par2;
        this.middle.field_78795_f = 0.86f - ani * 0.24f * par2;
        this.bottom.field_78795_f = 0.15f - ani * 0.28f * par2;
        this.fin2.field_78795_f = this.fin1.field_78795_f = 0.05f - ani * 0.35f * par2;
    }
}

