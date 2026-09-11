/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.model.ModelBiped
 *  net.minecraft.client.model.ModelRenderer
 */
package noppes.npcs.client.model.part.tails;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;

public class ModelTailFin
extends ModelRenderer {
    public ModelTailFin(ModelBiped base) {
        super((ModelBase)base);
        ModelRenderer Shape1 = new ModelRenderer((ModelBase)base, 0, 0);
        Shape1.func_78789_a(-2.0f, -2.0f, -2.0f, 3, 3, 8);
        Shape1.func_78793_a(0.5f, 0.0f, 1.0f);
        this.setRotation(Shape1, -0.669215f, 0.0f, 0.0f);
        this.func_78792_a(Shape1);
        ModelRenderer Shape2 = new ModelRenderer((ModelBase)base, 2, 2);
        Shape2.func_78789_a(-1.0f, -1.0f, 1.0f, 3, 2, 6);
        Shape2.func_78793_a(-0.5f, 3.0f, 4.5f);
        this.setRotation(Shape2, -0.2602503f, 0.0f, 0.0f);
        this.func_78792_a(Shape2);
        ModelRenderer Shape3 = new ModelRenderer((ModelBase)base, 0, 11);
        Shape3.func_78789_a(-1.0f, -1.0f, -1.0f, 3, 1, 6);
        Shape3.func_78793_a(0.5f, 5.0f, 12.0f);
        this.setRotation(Shape3, 0.0f, 1.07818f, 0.0f);
        this.func_78792_a(Shape3);
        ModelRenderer Shape4 = new ModelRenderer((ModelBase)base, 0, 11);
        Shape4.field_78809_i = true;
        Shape4.func_78789_a(-2.0f, 0.0f, -1.0f, 3, 1, 6);
        Shape4.func_78793_a(-0.5f, 4.0f, 12.0f);
        this.setRotation(Shape4, 0.0f, -1.003822f, 0.0f);
        this.func_78792_a(Shape4);
    }

    private void setRotation(ModelRenderer model, float x, float y, float z) {
        model.field_78795_f = x;
        model.field_78796_g = y;
        model.field_78808_h = z;
    }
}

