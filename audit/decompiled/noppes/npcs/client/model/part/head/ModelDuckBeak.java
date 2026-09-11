/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.model.ModelBiped
 *  net.minecraft.client.model.ModelRenderer
 *  net.minecraft.client.renderer.GlStateManager
 */
package noppes.npcs.client.model.part.head;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;

public class ModelDuckBeak
extends ModelRenderer {
    ModelRenderer Top3;
    ModelRenderer Top2;
    ModelRenderer Bottom;
    ModelRenderer Left;
    ModelRenderer Right;
    ModelRenderer Middle;
    ModelRenderer Top;

    public ModelDuckBeak(ModelBiped base) {
        super((ModelBase)base);
        this.Top3 = new ModelRenderer((ModelBase)base, 14, 0);
        this.Top3.func_78789_a(0.0f, 0.0f, 0.0f, 2, 1, 3);
        this.Top3.func_78793_a(-1.0f, -2.0f, -5.0f);
        this.setRotation(this.Top3, 0.3346075f, 0.0f, 0.0f);
        this.func_78792_a(this.Top3);
        this.Top2 = new ModelRenderer((ModelBase)base, 0, 0);
        this.Top2.func_78789_a(0.0f, 0.0f, -0.4f, 4, 1, 3);
        this.Top2.func_78793_a(-2.0f, -3.0f, -2.0f);
        this.setRotation(this.Top2, 0.3346075f, 0.0f, 0.0f);
        this.func_78792_a(this.Top2);
        this.Bottom = new ModelRenderer((ModelBase)base, 24, 0);
        this.Bottom.func_78789_a(0.0f, 0.0f, 0.0f, 2, 1, 5);
        this.Bottom.func_78793_a(-1.0f, -1.0f, -5.0f);
        this.func_78792_a(this.Bottom);
        this.Left = new ModelRenderer((ModelBase)base, 0, 4);
        this.Left.field_78809_i = true;
        this.Left.func_78789_a(0.0f, 0.0f, 0.0f, 1, 3, 2);
        this.Left.func_78793_a(0.98f, -3.0f, -2.0f);
        this.func_78792_a(this.Left);
        this.Right = new ModelRenderer((ModelBase)base, 0, 4);
        this.Right.func_78789_a(0.0f, 0.0f, 0.0f, 1, 3, 2);
        this.Right.func_78793_a(-1.98f, -3.0f, -2.0f);
        this.func_78792_a(this.Right);
        this.Middle = new ModelRenderer((ModelBase)base, 3, 0);
        this.Middle.func_78789_a(0.0f, 0.0f, 0.0f, 2, 1, 3);
        this.Middle.func_78793_a(-1.0f, -2.0f, -5.0f);
        this.func_78792_a(this.Middle);
        this.Top = new ModelRenderer((ModelBase)base, 6, 4);
        this.Top.func_78789_a(0.0f, 0.0f, 0.0f, 2, 2, 1);
        this.Top.func_78793_a(-1.0f, -4.4f, -1.0f);
        this.func_78792_a(this.Top);
    }

    public void func_78785_a(float f) {
        GlStateManager.func_179094_E();
        GlStateManager.func_179109_b((float)0.0f, (float)0.0f, (float)(-1.0f * f));
        GlStateManager.func_179152_a((float)0.82f, (float)0.82f, (float)0.7f);
        super.func_78785_a(f);
        GlStateManager.func_179121_F();
    }

    private void setRotation(ModelRenderer model, float x, float y, float z) {
        model.field_78795_f = x;
        model.field_78796_g = y;
        model.field_78808_h = z;
    }
}

