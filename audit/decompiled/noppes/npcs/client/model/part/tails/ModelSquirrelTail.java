/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.model.ModelBiped
 *  net.minecraft.client.model.ModelRenderer
 *  net.minecraft.entity.Entity
 */
package noppes.npcs.client.model.part.tails;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

public class ModelSquirrelTail
extends ModelRenderer {
    private ModelBiped base;

    public ModelSquirrelTail(ModelBiped base) {
        super((ModelBase)base);
        this.base = base;
        ModelRenderer Shape1 = new ModelRenderer((ModelBase)base, 0, 0);
        Shape1.func_78789_a(-1.0f, -1.0f, -1.0f, 2, 2, 3);
        Shape1.func_78793_a(0.0f, -1.0f, 3.0f);
        this.setRotation(Shape1, 0.0f, 0.0f, 0.0f);
        this.func_78792_a(Shape1);
        ModelRenderer Shape2 = new ModelRenderer((ModelBase)base, 0, 9);
        Shape2.func_78789_a(-2.0f, -5.0f, -1.0f, 4, 5, 3);
        Shape2.func_78793_a(0.0f, 0.0f, 1.0f);
        this.setRotation(Shape2, -0.37f, 0.0f, 0.0f);
        Shape1.func_78792_a(Shape2);
        ModelRenderer Shape3 = new ModelRenderer((ModelBase)base, 0, 18);
        Shape3.func_78789_a(-2.466667f, -6.0f, -1.0f, 5, 7, 3);
        Shape3.func_78793_a(0.0f, -5.0f, 0.0f);
        this.setRotation(Shape3, 0.3f, 0.0f, 0.0f);
        Shape2.func_78792_a(Shape3);
        ModelRenderer Shape4 = new ModelRenderer((ModelBase)base, 25, 0);
        Shape4.func_78789_a(-3.0f, -0.6f, -1.0f, 6, 5, 3);
        Shape4.func_78793_a(0.0f, -5.0f, 1.0f);
        this.setRotation(Shape4, 2.5f, 0.0f, 0.0f);
        Shape3.func_78792_a(Shape4);
        ModelRenderer Shape5 = new ModelRenderer((ModelBase)base, 25, 10);
        Shape5.func_78789_a(-3.0f, -2.0f, -1.0f, 6, 3, 5);
        Shape5.func_78793_a(0.0f, 3.5f, 0.0f);
        this.setRotation(Shape5, -2.5f, 0.0f, 0.0f);
        Shape4.func_78792_a(Shape5);
    }

    public void setRotationAngles(float par1, float par2, float par3, float par4, float par5, float par6, Entity entity) {
    }

    private void setRotation(ModelRenderer model, float x, float y, float z) {
        model.field_78795_f = x;
        model.field_78796_g = y;
        model.field_78808_h = z;
    }
}

