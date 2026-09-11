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

public class ModelCanineTail
extends ModelRenderer {
    ModelRenderer Base_1;
    ModelRenderer BaseB_1;
    ModelRenderer Mid_1;
    ModelRenderer Mid_2;
    ModelRenderer MidB_1;
    ModelRenderer End_1;

    public ModelCanineTail(ModelBiped base) {
        super((ModelBase)base);
        this.Base_1 = new ModelRenderer((ModelBase)base, 56, 16);
        this.Base_1.func_78789_a(-1.0f, 0.0f, -3.0f, 2, 3, 2);
        this.Base_1.func_78793_a(0.0f, 1.0f, -1.2f);
        this.setRotation(this.Base_1, -0.4490659f, 3.141593f, 0.0f);
        this.func_78792_a(this.Base_1);
        this.BaseB_1 = new ModelRenderer((ModelBase)base, 56, 16);
        this.BaseB_1.func_78789_a(-0.5f, 0.0f, -1.5f, 1, 3, 1);
        this.Base_1.func_78792_a(this.BaseB_1);
        this.Mid_1 = new ModelRenderer((ModelBase)base, 56, 20);
        this.Mid_1.func_78789_a(-1.0f, 3.0f, -2.8f, 2, 2, 2);
        this.setRotation(this.Mid_1, -0.16f, 0.0f, 0.0f);
        this.Base_1.func_78792_a(this.Mid_1);
        this.Mid_2 = new ModelRenderer((ModelBase)base, 56, 22);
        this.Mid_2.func_78789_a(-1.5f, 5.0f, -1.5f, 3, 6, 2);
        this.Mid_2.func_78793_a(0.0f, 0.0f, -1.5f);
        this.setRotation(this.Mid_2, -0.0f, 0.0f, 0.0f);
        this.Mid_1.func_78792_a(this.Mid_2);
        ModelRenderer Mid_2b = new ModelRenderer((ModelBase)base, 56, 23);
        Mid_2b.func_78789_a(-1.5f, 5.0f, -1.5f, 3, 6, 1);
        this.setRotation(Mid_2b, -0.0f, (float)Math.PI, 0.0f);
        this.Mid_2.func_78792_a(Mid_2b);
        this.MidB_1 = new ModelRenderer((ModelBase)base, 56, 20);
        this.MidB_1.func_78789_a(-0.5f, 3.0f, -1.0f, 1, 2, 1);
        this.Mid_1.func_78792_a(this.MidB_1);
        this.End_1 = new ModelRenderer((ModelBase)base, 56, 29);
        this.End_1.func_78789_a(-1.0f, 10.7f, -1.0f, 2, 1, 2);
        this.Mid_2.func_78792_a(this.End_1);
    }

    private void setRotation(ModelRenderer model, float x, float y, float z) {
        model.field_78795_f = x;
        model.field_78796_g = y;
        model.field_78808_h = z;
    }

    public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity entity) {
        this.Base_1.field_78795_f = -0.5490659f - f1 * 0.7f;
        this.Base_1.field_78796_g = 3.141593f + this.field_78796_g * 0.1f;
        this.Mid_1.field_78796_g = this.field_78796_g * 0.2f;
        this.Mid_2.field_78796_g = this.field_78796_g * 0.2f;
    }
}

