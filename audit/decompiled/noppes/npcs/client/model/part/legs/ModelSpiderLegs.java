/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.model.ModelBiped
 *  net.minecraft.client.model.ModelRenderer
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.math.MathHelper
 */
package noppes.npcs.client.model.part.legs;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.ModelData;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.entity.EntityNPCInterface;

public class ModelSpiderLegs
extends ModelRenderer {
    private ModelRenderer spiderLeg1;
    private ModelRenderer spiderLeg2;
    private ModelRenderer spiderLeg3;
    private ModelRenderer spiderLeg4;
    private ModelRenderer spiderLeg5;
    private ModelRenderer spiderLeg6;
    private ModelRenderer spiderLeg7;
    private ModelRenderer spiderLeg8;
    private ModelRenderer spiderBody;
    private ModelRenderer spiderNeck;
    private ModelBiped base;

    public ModelSpiderLegs(ModelBiped base) {
        super((ModelBase)base);
        this.base = base;
        float var1 = 0.0f;
        int var2 = 15;
        this.spiderNeck = new ModelRenderer((ModelBase)base, 0, 0);
        this.spiderNeck.func_78790_a(-3.0f, -3.0f, -3.0f, 6, 6, 6, var1);
        this.spiderNeck.func_78793_a(0.0f, (float)var2, 2.0f);
        this.func_78792_a(this.spiderNeck);
        this.spiderBody = new ModelRenderer((ModelBase)base, 0, 12);
        this.spiderBody.func_78790_a(-5.0f, -4.0f, -6.0f, 10, 8, 12, var1);
        this.spiderBody.func_78793_a(0.0f, (float)var2, 11.0f);
        this.func_78792_a(this.spiderBody);
        this.spiderLeg1 = new ModelRenderer((ModelBase)base, 18, 0);
        this.spiderLeg1.func_78790_a(-15.0f, -1.0f, -1.0f, 16, 2, 2, var1);
        this.spiderLeg1.func_78793_a(-4.0f, (float)var2, 4.0f);
        this.func_78792_a(this.spiderLeg1);
        this.spiderLeg2 = new ModelRenderer((ModelBase)base, 18, 0);
        this.spiderLeg2.func_78790_a(-1.0f, -1.0f, -1.0f, 16, 2, 2, var1);
        this.spiderLeg2.func_78793_a(4.0f, (float)var2, 4.0f);
        this.func_78792_a(this.spiderLeg2);
        this.spiderLeg3 = new ModelRenderer((ModelBase)base, 18, 0);
        this.spiderLeg3.func_78790_a(-15.0f, -1.0f, -1.0f, 16, 2, 2, var1);
        this.spiderLeg3.func_78793_a(-4.0f, (float)var2, 3.0f);
        this.func_78792_a(this.spiderLeg3);
        this.spiderLeg4 = new ModelRenderer((ModelBase)base, 18, 0);
        this.spiderLeg4.func_78790_a(-1.0f, -1.0f, -1.0f, 16, 2, 2, var1);
        this.spiderLeg4.func_78793_a(4.0f, (float)var2, 3.0f);
        this.func_78792_a(this.spiderLeg4);
        this.spiderLeg5 = new ModelRenderer((ModelBase)base, 18, 0);
        this.spiderLeg5.func_78790_a(-15.0f, -1.0f, -1.0f, 16, 2, 2, var1);
        this.spiderLeg5.func_78793_a(-4.0f, (float)var2, 2.0f);
        this.func_78792_a(this.spiderLeg5);
        this.spiderLeg6 = new ModelRenderer((ModelBase)base, 18, 0);
        this.spiderLeg6.func_78790_a(-1.0f, -1.0f, -1.0f, 16, 2, 2, var1);
        this.spiderLeg6.func_78793_a(4.0f, (float)var2, 2.0f);
        this.func_78792_a(this.spiderLeg6);
        this.spiderLeg7 = new ModelRenderer((ModelBase)base, 18, 0);
        this.spiderLeg7.func_78790_a(-15.0f, -1.0f, -1.0f, 16, 2, 2, var1);
        this.spiderLeg7.func_78793_a(-4.0f, (float)var2, 1.0f);
        this.func_78792_a(this.spiderLeg7);
        this.spiderLeg8 = new ModelRenderer((ModelBase)base, 18, 0);
        this.spiderLeg8.func_78790_a(-1.0f, -1.0f, -1.0f, 16, 2, 2, var1);
        this.spiderLeg8.func_78793_a(4.0f, (float)var2, 1.0f);
        this.func_78792_a(this.spiderLeg8);
    }

    public void setRotationAngles(ModelData data, float par1, float par2, float par3, float par4, float par5, float par6, Entity entity) {
        this.field_78795_f = 0.0f;
        this.field_78797_d = 0.0f;
        this.field_78798_e = 0.0f;
        this.spiderBody.field_78797_d = 15.0f;
        this.spiderBody.field_78798_e = 11.0f;
        this.spiderNeck.field_78795_f = 0.0f;
        float var8 = 0.7853982f;
        this.spiderLeg1.field_78808_h = -var8;
        this.spiderLeg2.field_78808_h = var8;
        this.spiderLeg3.field_78808_h = -var8 * 0.74f;
        this.spiderLeg4.field_78808_h = var8 * 0.74f;
        this.spiderLeg5.field_78808_h = -var8 * 0.74f;
        this.spiderLeg6.field_78808_h = var8 * 0.74f;
        this.spiderLeg7.field_78808_h = -var8;
        this.spiderLeg8.field_78808_h = var8;
        float var9 = -0.0f;
        float var10 = 0.3926991f;
        this.spiderLeg1.field_78796_g = var10 * 2.0f + var9;
        this.spiderLeg2.field_78796_g = -var10 * 2.0f - var9;
        this.spiderLeg3.field_78796_g = var10 * 1.0f + var9;
        this.spiderLeg4.field_78796_g = -var10 * 1.0f - var9;
        this.spiderLeg5.field_78796_g = -var10 * 1.0f + var9;
        this.spiderLeg6.field_78796_g = var10 * 1.0f - var9;
        this.spiderLeg7.field_78796_g = -var10 * 2.0f + var9;
        this.spiderLeg8.field_78796_g = var10 * 2.0f - var9;
        float var11 = -(MathHelper.func_76134_b((float)(par1 * 0.6662f * 2.0f + 0.0f)) * 0.4f) * par2;
        float var12 = -(MathHelper.func_76134_b((float)(par1 * 0.6662f * 2.0f + (float)Math.PI)) * 0.4f) * par2;
        float var13 = -(MathHelper.func_76134_b((float)(par1 * 0.6662f * 2.0f + 1.5707964f)) * 0.4f) * par2;
        float var14 = -(MathHelper.func_76134_b((float)(par1 * 0.6662f * 2.0f + 4.712389f)) * 0.4f) * par2;
        float var15 = Math.abs(MathHelper.func_76126_a((float)(par1 * 0.6662f + 0.0f)) * 0.4f) * par2;
        float var16 = Math.abs(MathHelper.func_76126_a((float)(par1 * 0.6662f + (float)Math.PI)) * 0.4f) * par2;
        float var17 = Math.abs(MathHelper.func_76126_a((float)(par1 * 0.6662f + 1.5707964f)) * 0.4f) * par2;
        float var18 = Math.abs(MathHelper.func_76126_a((float)(par1 * 0.6662f + 4.712389f)) * 0.4f) * par2;
        this.spiderLeg1.field_78796_g += var11;
        this.spiderLeg2.field_78796_g += -var11;
        this.spiderLeg3.field_78796_g += var12;
        this.spiderLeg4.field_78796_g += -var12;
        this.spiderLeg5.field_78796_g += var13;
        this.spiderLeg6.field_78796_g += -var13;
        this.spiderLeg7.field_78796_g += var14;
        this.spiderLeg8.field_78796_g += -var14;
        this.spiderLeg1.field_78808_h += var15;
        this.spiderLeg2.field_78808_h += -var15;
        this.spiderLeg3.field_78808_h += var16;
        this.spiderLeg4.field_78808_h += -var16;
        this.spiderLeg5.field_78808_h += var17;
        this.spiderLeg6.field_78808_h += -var17;
        this.spiderLeg7.field_78808_h += var18;
        this.spiderLeg8.field_78808_h += -var18;
        if (this.base.field_78117_n) {
            this.field_78798_e = 5.0f;
            this.field_78797_d = -1.0f;
            this.spiderBody.field_78797_d = 16.0f;
            this.spiderBody.field_78798_e = 10.0f;
            this.spiderNeck.field_78795_f = -0.3926991f;
        }
        if (((EntityNPCInterface)entity).func_70608_bn() || ((EntityNPCInterface)entity).currentAnimation == 7) {
            this.field_78797_d = 12.0f * data.getPartConfig((EnumParts)EnumParts.LEG_LEFT).scaleY;
            this.field_78798_e = 15.0f * data.getPartConfig((EnumParts)EnumParts.LEG_LEFT).scaleY;
            this.field_78795_f = -1.5707964f;
        }
    }

    private void setRotation(ModelRenderer model, float x, float y, float z) {
        model.field_78795_f = x;
        model.field_78796_g = y;
        model.field_78808_h = z;
    }
}

