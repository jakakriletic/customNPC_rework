/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.model.ModelRenderer
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.GlStateManager$CullFace
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.util.math.MathHelper
 */
package noppes.npcs.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.entity.EntityNpcDragon;

public class ModelNpcDragon
extends ModelBase {
    private ModelRenderer head;
    private ModelRenderer neck;
    private ModelRenderer jaw;
    private ModelRenderer body;
    private ModelRenderer rearLeg;
    private ModelRenderer frontLeg;
    private ModelRenderer rearLegTip;
    private ModelRenderer frontLegTip;
    private ModelRenderer rearFoot;
    private ModelRenderer frontFoot;
    private ModelRenderer wing;
    private ModelRenderer wingTip;
    private float field_40317_s;

    public ModelNpcDragon(float scale) {
        this.field_78090_t = 256;
        this.field_78089_u = 256;
        this.func_78085_a("body.body", 0, 0);
        this.func_78085_a("wing.skin", -56, 88);
        this.func_78085_a("wingtip.skin", -56, 144);
        this.func_78085_a("rearleg.main", 0, 0);
        this.func_78085_a("rearfoot.main", 112, 0);
        this.func_78085_a("rearlegtip.main", 196, 0);
        this.func_78085_a("head.upperhead", 112, 30);
        this.func_78085_a("wing.bone", 112, 88);
        this.func_78085_a("head.upperlip", 176, 44);
        this.func_78085_a("jaw.jaw", 176, 65);
        this.func_78085_a("frontleg.main", 112, 104);
        this.func_78085_a("wingtip.bone", 112, 136);
        this.func_78085_a("frontfoot.main", 144, 104);
        this.func_78085_a("neck.box", 192, 104);
        this.func_78085_a("frontlegtip.main", 226, 138);
        this.func_78085_a("body.scale", 220, 53);
        this.func_78085_a("head.scale", 0, 0);
        this.func_78085_a("neck.scale", 48, 0);
        this.func_78085_a("head.nostril", 112, 0);
        float f = -16.0f;
        this.head = new ModelRenderer((ModelBase)this, "head");
        this.head.func_78786_a("upperlip", -6.0f, -1.0f, -24.0f, 12, 5, 16);
        this.head.func_78786_a("upperhead", -8.0f, -8.0f, -10.0f, 16, 16, 16);
        this.head.field_78809_i = true;
        this.head.func_78786_a("scale", -5.0f, -12.0f, -4.0f, 2, 4, 6);
        this.head.func_78786_a("nostril", -5.0f, -3.0f, -22.0f, 2, 2, 4);
        this.head.field_78809_i = false;
        this.head.func_78786_a("scale", 3.0f, -12.0f, -4.0f, 2, 4, 6);
        this.head.func_78786_a("nostril", 3.0f, -3.0f, -22.0f, 2, 2, 4);
        this.jaw = new ModelRenderer((ModelBase)this, "jaw");
        this.jaw.func_78793_a(0.0f, 4.0f, -8.0f);
        this.jaw.func_78786_a("jaw", -6.0f, 0.0f, -16.0f, 12, 4, 16);
        this.head.func_78792_a(this.jaw);
        this.neck = new ModelRenderer((ModelBase)this, "neck");
        this.neck.func_78786_a("box", -5.0f, -5.0f, -5.0f, 10, 10, 10);
        this.neck.func_78786_a("scale", -1.0f, -9.0f, -3.0f, 2, 4, 6);
        this.body = new ModelRenderer((ModelBase)this, "body");
        this.body.func_78793_a(0.0f, 4.0f, 8.0f);
        this.body.func_78786_a("body", -12.0f, 0.0f, -16.0f, 24, 24, 64);
        this.body.func_78786_a("scale", -1.0f, -6.0f, -10.0f, 2, 6, 12);
        this.body.func_78786_a("scale", -1.0f, -6.0f, 10.0f, 2, 6, 12);
        this.body.func_78786_a("scale", -1.0f, -6.0f, 30.0f, 2, 6, 12);
        this.wing = new ModelRenderer((ModelBase)this, "wing");
        this.wing.func_78793_a(-12.0f, 5.0f, 2.0f);
        this.wing.func_78786_a("bone", -56.0f, -4.0f, -4.0f, 56, 8, 8);
        this.wing.func_78786_a("skin", -56.0f, 0.0f, 2.0f, 56, 0, 56);
        this.wingTip = new ModelRenderer((ModelBase)this, "wingtip");
        this.wingTip.func_78793_a(-56.0f, 0.0f, 0.0f);
        this.wingTip.func_78786_a("bone", -56.0f, -2.0f, -2.0f, 56, 4, 4);
        this.wingTip.func_78786_a("skin", -56.0f, 0.0f, 2.0f, 56, 0, 56);
        this.wing.func_78792_a(this.wingTip);
        this.frontLeg = new ModelRenderer((ModelBase)this, "frontleg");
        this.frontLeg.func_78793_a(-12.0f, 20.0f, 2.0f);
        this.frontLeg.func_78786_a("main", -4.0f, -4.0f, -4.0f, 8, 24, 8);
        this.frontLegTip = new ModelRenderer((ModelBase)this, "frontlegtip");
        this.frontLegTip.func_78793_a(0.0f, 20.0f, -1.0f);
        this.frontLegTip.func_78786_a("main", -3.0f, -1.0f, -3.0f, 6, 24, 6);
        this.frontLeg.func_78792_a(this.frontLegTip);
        this.frontFoot = new ModelRenderer((ModelBase)this, "frontfoot");
        this.frontFoot.func_78793_a(0.0f, 23.0f, 0.0f);
        this.frontFoot.func_78786_a("main", -4.0f, 0.0f, -12.0f, 8, 4, 16);
        this.frontLegTip.func_78792_a(this.frontFoot);
        this.rearLeg = new ModelRenderer((ModelBase)this, "rearleg");
        this.rearLeg.func_78793_a(-16.0f, 16.0f, 42.0f);
        this.rearLeg.func_78786_a("main", -8.0f, -4.0f, -8.0f, 16, 32, 16);
        this.rearLegTip = new ModelRenderer((ModelBase)this, "rearlegtip");
        this.rearLegTip.func_78793_a(0.0f, 32.0f, -4.0f);
        this.rearLegTip.func_78786_a("main", -6.0f, -2.0f, 0.0f, 12, 32, 12);
        this.rearLeg.func_78792_a(this.rearLegTip);
        this.rearFoot = new ModelRenderer((ModelBase)this, "rearfoot");
        this.rearFoot.func_78793_a(0.0f, 31.0f, 4.0f);
        this.rearFoot.func_78786_a("main", -9.0f, 0.0f, -20.0f, 18, 6, 24);
        this.rearLegTip.func_78792_a(this.rearFoot);
    }

    public void func_78086_a(EntityLivingBase entityliving, float f, float f1, float f2) {
        this.field_40317_s = f2;
    }

    public void func_78088_a(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        int j;
        EntityNpcDragon entitydragon = (EntityNpcDragon)entity;
        GlStateManager.func_179094_E();
        float f6 = entitydragon.field_40173_aw + (entitydragon.field_40172_ax - entitydragon.field_40173_aw) * this.field_40317_s;
        this.jaw.field_78795_f = (float)(Math.sin(f6 * (float)Math.PI * 2.0f) + 1.0) * 0.2f;
        float f7 = (float)(Math.sin(f6 * (float)Math.PI * 2.0f - 1.0f) + 1.0);
        f7 = (f7 * f7 * 1.0f + f7 * 2.0f) * 0.05f;
        GlStateManager.func_179109_b((float)0.0f, (float)(f7 - 2.0f), (float)-3.0f);
        GlStateManager.func_179114_b((float)(f7 * 2.0f), (float)1.0f, (float)0.0f, (float)0.0f);
        float f8 = -30.0f;
        float f9 = 22.0f;
        float f10 = 0.0f;
        float f11 = 1.5f;
        double[] ad = entitydragon.func_40160_a(6, this.field_40317_s);
        float f12 = this.func_40307_a(entitydragon.func_40160_a(5, this.field_40317_s)[0] - entitydragon.func_40160_a(10, this.field_40317_s)[0]);
        float f13 = this.func_40307_a(entitydragon.func_40160_a(5, this.field_40317_s)[0] + (double)(f12 / 2.0f));
        f8 += 2.0f;
        float f14 = 0.0f;
        float f15 = f6 * 3.141593f * 2.0f;
        f8 = 20.0f;
        f9 = -12.0f;
        for (int i = 0; i < 5; ++i) {
            double[] ad3 = entitydragon.func_40160_a(5 - i, this.field_40317_s);
            f14 = (float)Math.cos((float)i * 0.45f + f15) * 0.15f;
            this.neck.field_78796_g = this.func_40307_a(ad3[0] - ad[0]) * (float)Math.PI / 180.0f * f11;
            this.neck.field_78795_f = f14 + (float)(ad3[1] - ad[1]) * (float)Math.PI / 180.0f * f11 * 5.0f;
            this.neck.field_78808_h = -this.func_40307_a(ad3[0] - (double)f13) * (float)Math.PI / 180.0f * f11;
            this.neck.field_78797_d = f8;
            this.neck.field_78798_e = f9;
            this.neck.field_78800_c = f10;
            f8 = (float)((double)f8 + Math.sin(this.neck.field_78795_f) * 10.0);
            f9 = (float)((double)f9 - Math.cos(this.neck.field_78796_g) * Math.cos(this.neck.field_78795_f) * 10.0);
            f10 = (float)((double)f10 - Math.sin(this.neck.field_78796_g) * Math.cos(this.neck.field_78795_f) * 10.0);
            this.neck.func_78785_a(f5);
        }
        this.head.field_78797_d = f8;
        this.head.field_78798_e = f9;
        this.head.field_78800_c = f10;
        double[] ad1 = entitydragon.func_40160_a(0, this.field_40317_s);
        this.head.field_78796_g = this.func_40307_a(ad1[0] - ad[0]) * (float)Math.PI / 180.0f * 1.0f;
        this.head.field_78808_h = -this.func_40307_a(ad1[0] - (double)f13) * (float)Math.PI / 180.0f * 1.0f;
        this.head.func_78785_a(f5);
        GlStateManager.func_179094_E();
        GlStateManager.func_179109_b((float)0.0f, (float)1.0f, (float)0.0f);
        if (entitydragon.field_70122_E) {
            GlStateManager.func_179114_b((float)(-f12 * f11 * 0.3f), (float)0.0f, (float)0.0f, (float)1.0f);
        } else {
            GlStateManager.func_179114_b((float)(-f12 * f11 * 1.0f), (float)0.0f, (float)0.0f, (float)1.0f);
        }
        GlStateManager.func_179109_b((float)0.0f, (float)-1.18f, (float)0.0f);
        this.body.field_78808_h = 0.0f;
        this.body.func_78785_a(f5);
        if (entitydragon.field_70122_E) {
            for (j = 0; j < 2; ++j) {
                GlStateManager.func_179089_o();
                this.wing.field_78795_f = 0.25f;
                this.wing.field_78796_g = 0.95f;
                this.wing.field_78808_h = -0.5f;
                this.wingTip.field_78808_h = -0.4f;
                this.frontLeg.field_78795_f = MathHelper.func_76134_b((float)((float)((double)(f * 0.6662f) + (j == 0 ? 0.0 : Math.PI)))) * 0.6f * f1 + 0.45f + f7 * 0.5f;
                this.frontLegTip.field_78795_f = -1.3f - f7 * 1.2f;
                this.frontFoot.field_78795_f = 0.85f + f7 * 0.5f;
                this.frontLeg.func_78785_a(f5);
                this.rearLeg.field_78795_f = MathHelper.func_76134_b((float)((float)((double)(f * 0.6662f) + (j == 0 ? Math.PI : 0.0)))) * 0.6f * f1 + 0.75f + f7 * 0.5f;
                this.rearLegTip.field_78795_f = -1.6f - f7 * 0.8f;
                this.rearLegTip.field_78797_d = 20.0f;
                this.rearLegTip.field_78798_e = 2.0f;
                this.rearFoot.field_78795_f = 0.85f + f7 * 0.2f;
                this.rearLeg.func_78785_a(f5);
                this.wing.func_78785_a(f5);
                GlStateManager.func_179152_a((float)-1.0f, (float)1.0f, (float)1.0f);
                if (j != 0) continue;
                GlStateManager.func_187407_a((GlStateManager.CullFace)GlStateManager.CullFace.FRONT);
            }
        } else {
            for (j = 0; j < 2; ++j) {
                GlStateManager.func_179089_o();
                float f16 = f6 * (float)Math.PI * 2.0f;
                this.wing.field_78795_f = 0.125f - (float)Math.cos(f16) * 0.2f;
                this.wing.field_78796_g = 0.25f;
                this.wing.field_78808_h = (float)(Math.sin(f16) + 0.125) * 0.8f;
                this.wingTip.field_78808_h = -((float)(Math.sin(f16 + 2.0f) + 0.5)) * 0.75f;
                this.rearLegTip.field_78797_d = 32.0f;
                this.rearLegTip.field_78798_e = -2.0f;
                this.rearLeg.field_78795_f = 1.0f + f7 * 0.1f;
                this.rearLegTip.field_78795_f = 0.5f + f7 * 0.1f;
                this.rearFoot.field_78795_f = 0.75f + f7 * 0.1f;
                this.frontLeg.field_78795_f = 1.3f + f7 * 0.1f;
                this.frontLegTip.field_78795_f = -0.5f - f7 * 0.1f;
                this.frontFoot.field_78795_f = 0.75f + f7 * 0.1f;
                this.wing.func_78785_a(f5);
                this.frontLeg.func_78785_a(f5);
                this.rearLeg.func_78785_a(f5);
                GlStateManager.func_179152_a((float)-1.0f, (float)1.0f, (float)1.0f);
                if (j != 0) continue;
                GlStateManager.func_187407_a((GlStateManager.CullFace)GlStateManager.CullFace.FRONT);
            }
        }
        GlStateManager.func_179121_F();
        GlStateManager.func_187407_a((GlStateManager.CullFace)GlStateManager.CullFace.BACK);
        GlStateManager.func_179129_p();
        f14 = -((float)Math.sin(f6 * 3.141593f * 2.0f)) * 0.0f;
        f15 = f6 * (float)Math.PI * 2.0f;
        f8 = 10.0f;
        f9 = 60.0f;
        f10 = 0.0f;
        ad = entitydragon.func_40160_a(11, this.field_40317_s);
        for (int k = 0; k < 12; ++k) {
            double[] ad2 = entitydragon.func_40160_a(12 + k, this.field_40317_s);
            f14 = (float)((double)f14 + Math.sin((float)k * 0.45f + f15) * (double)0.05f);
            this.neck.field_78796_g = (this.func_40307_a(ad2[0] - ad[0]) * f11 + 180.0f) * (float)Math.PI / 180.0f;
            this.neck.field_78795_f = f14 + (float)(ad2[1] - ad[1]) * (float)Math.PI / 180.0f * f11 * 5.0f;
            this.neck.field_78808_h = this.func_40307_a(ad2[0] - (double)f13) * (float)Math.PI / 180.0f * f11;
            this.neck.field_78797_d = f8;
            this.neck.field_78798_e = f9;
            this.neck.field_78800_c = f10;
            f8 = (float)((double)f8 + Math.sin(this.neck.field_78795_f) * 10.0);
            f9 = (float)((double)f9 - Math.cos(this.neck.field_78796_g) * Math.cos(this.neck.field_78795_f) * 10.0);
            f10 = (float)((double)f10 - Math.sin(this.neck.field_78796_g) * Math.cos(this.neck.field_78795_f) * 10.0);
            this.neck.func_78785_a(f5);
        }
        GlStateManager.func_179121_F();
    }

    private float func_40307_a(double d) {
        while (d >= 180.0) {
            d -= 360.0;
        }
        while (d < -180.0) {
            d += 360.0;
        }
        return (float)d;
    }
}

