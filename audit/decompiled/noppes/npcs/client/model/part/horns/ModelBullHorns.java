/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.model.ModelBiped
 *  net.minecraft.client.model.ModelRenderer
 */
package noppes.npcs.client.model.part.horns;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;

public class ModelBullHorns
extends ModelRenderer {
    public ModelBullHorns(ModelBiped base) {
        super((ModelBase)base);
        ModelRenderer Left1 = new ModelRenderer((ModelBase)base, 36, 16);
        Left1.field_78809_i = true;
        Left1.func_78789_a(0.0f, 0.0f, 0.0f, 2, 2, 2);
        Left1.func_78793_a(4.0f, -8.0f, -2.0f);
        this.func_78792_a(Left1);
        ModelRenderer Right1 = new ModelRenderer((ModelBase)base, 36, 16);
        Right1.func_78789_a(-3.0f, 0.0f, 0.0f, 2, 2, 2);
        Right1.func_78793_a(-3.0f, -8.0f, -2.0f);
        this.func_78792_a(Right1);
        ModelRenderer Left2 = new ModelRenderer((ModelBase)base, 12, 16);
        Left2.field_78809_i = true;
        Left2.func_78789_a(0.0f, 0.0f, 0.0f, 2, 2, 2);
        Left2.func_78793_a(5.0f, -8.0f, -2.0f);
        this.setRotation(Left2, 0.0371786f, 0.3346075f, -0.2602503f);
        this.func_78792_a(Left2);
        ModelRenderer Right2 = new ModelRenderer((ModelBase)base, 12, 16);
        Right2.func_78789_a(-2.0f, 0.0f, 0.0f, 2, 2, 2);
        Right2.func_78793_a(-5.0f, -8.0f, -2.0f);
        this.setRotation(Right2, 0.0371786f, -0.3346075f, 0.2602503f);
        this.func_78792_a(Right2);
        ModelRenderer Left3 = new ModelRenderer((ModelBase)base, 13, 17);
        Left3.field_78809_i = true;
        Left3.func_78789_a(-1.0f, 0.0f, 0.0f, 2, 1, 1);
        Left3.func_78793_a(7.0f, -8.0f, -2.0f);
        this.setRotation(Left3, 0.2602503f, 0.8551081f, -0.4089647f);
        this.func_78792_a(Left3);
        ModelRenderer Right3 = new ModelRenderer((ModelBase)base, 13, 17);
        Right3.func_78789_a(-1.0f, 0.0f, 0.0f, 2, 1, 1);
        Right3.func_78793_a(-7.0f, -8.0f, -2.0f);
        this.setRotation(Right3, -0.2602503f, -0.8551081f, 0.4089647f);
        this.func_78792_a(Right3);
    }

    private void setRotation(ModelRenderer model, float x, float y, float z) {
        model.field_78795_f = x;
        model.field_78796_g = y;
        model.field_78808_h = z;
    }
}

