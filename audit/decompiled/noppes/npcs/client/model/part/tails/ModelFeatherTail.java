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

public class ModelFeatherTail
extends ModelRenderer {
    ModelRenderer feather1;
    ModelRenderer feather2;
    ModelRenderer feather3;
    ModelRenderer feather4;
    ModelRenderer feather5;

    public ModelFeatherTail(ModelBiped base) {
        super((ModelBase)base);
        int x = 56;
        int y = 16;
        this.feather1 = new ModelRenderer((ModelBase)base, x, y);
        this.feather1.func_78789_a(-1.5f, 0.0f, 0.0f, 3, 8, 0);
        this.feather1.func_78793_a(1.0f, -0.5f, 2.0f);
        this.setRotation(this.feather1, 1.482807f, 0.2602503f, 0.1487144f);
        this.func_78792_a(this.feather1);
        this.feather2 = new ModelRenderer((ModelBase)base, x, y);
        this.feather2.func_78789_a(-1.5f, 0.0f, 0.0f, 3, 8, 0);
        this.feather2.func_78793_a(0.0f, -0.5f, 1.0f);
        this.setRotation(this.feather2, 1.200559f, 0.3717861f, 0.1858931f);
        this.func_78792_a(this.feather2);
        this.feather3 = new ModelRenderer((ModelBase)base, x, y);
        this.feather3.field_78809_i = true;
        this.feather3.func_78789_a(-1.5f, -0.5f, 0.0f, 3, 8, 0);
        this.feather3.func_78793_a(-1.0f, 0.0f, 2.0f);
        this.setRotation(this.feather3, 1.256389f, -0.4089647f, -0.4833219f);
        this.func_78792_a(this.feather3);
        this.feather4 = new ModelRenderer((ModelBase)base, x, y);
        this.feather4.func_78789_a(-1.5f, 0.0f, 0.0f, 3, 8, 0);
        this.feather4.func_78793_a(0.0f, -0.5f, 2.0f);
        this.setRotation(this.feather4, 1.786329f, 0.0f, 0.0f);
        this.func_78792_a(this.feather4);
        this.feather5 = new ModelRenderer((ModelBase)base, x, y);
        this.feather5.field_78809_i = true;
        this.feather5.func_78789_a(-1.5f, 0.0f, 0.0f, 3, 8, 0);
        this.feather5.func_78793_a(-1.0f, -0.5f, 2.0f);
        this.setRotation(this.feather5, 1.570073f, -0.2602503f, -0.2230717f);
        this.func_78792_a(this.feather5);
    }

    private void setRotation(ModelRenderer model, float x, float y, float z) {
        model.field_78795_f = x;
        model.field_78796_g = y;
        model.field_78808_h = z;
    }
}

