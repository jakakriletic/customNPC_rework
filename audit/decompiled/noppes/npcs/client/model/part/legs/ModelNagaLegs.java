/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.model.ModelRenderer
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.math.MathHelper
 */
package noppes.npcs.client.model.part.legs;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.client.model.ModelPlaneRenderer;

public class ModelNagaLegs
extends ModelRenderer {
    private ModelRenderer nagaPart1;
    private ModelRenderer nagaPart2;
    private ModelRenderer nagaPart3;
    private ModelRenderer nagaPart4;
    private ModelRenderer nagaPart5;
    public boolean isRiding = false;
    public boolean isSneaking = false;
    public boolean isSleeping = false;
    public boolean isCrawling = false;

    public ModelNagaLegs(ModelBase base) {
        super(base);
        this.nagaPart1 = new ModelRenderer(base, 0, 0);
        ModelRenderer legPart = new ModelRenderer(base, 0, 16);
        legPart.func_78789_a(0.0f, -2.0f, -2.0f, 4, 4, 4);
        legPart.func_78793_a(-4.0f, 0.0f, 0.0f);
        this.nagaPart1.func_78792_a(legPart);
        legPart = new ModelRenderer(base, 0, 16);
        legPart.field_78809_i = true;
        legPart.func_78789_a(0.0f, -2.0f, -2.0f, 4, 4, 4);
        this.nagaPart1.func_78792_a(legPart);
        this.nagaPart2 = new ModelRenderer(base, 0, 0);
        this.nagaPart2.field_78805_m = this.nagaPart1.field_78805_m;
        this.nagaPart3 = new ModelRenderer(base, 0, 0);
        ModelPlaneRenderer plane = new ModelPlaneRenderer(base, 4, 24);
        plane.addBackPlane(0.0f, -2.0f, 0.0f, 4, 4);
        plane.func_78793_a(-4.0f, 0.0f, 0.0f);
        this.nagaPart3.func_78792_a((ModelRenderer)plane);
        plane = new ModelPlaneRenderer(base, 4, 24);
        plane.field_78809_i = true;
        plane.addBackPlane(0.0f, -2.0f, 0.0f, 4, 4);
        this.nagaPart3.func_78792_a((ModelRenderer)plane);
        plane = new ModelPlaneRenderer(base, 8, 24);
        plane.addBackPlane(0.0f, -2.0f, 6.0f, 4, 4);
        plane.func_78793_a(-4.0f, 0.0f, 0.0f);
        this.nagaPart3.func_78792_a((ModelRenderer)plane);
        plane = new ModelPlaneRenderer(base, 8, 24);
        plane.field_78809_i = true;
        plane.addBackPlane(0.0f, -2.0f, 6.0f, 4, 4);
        this.nagaPart3.func_78792_a((ModelRenderer)plane);
        plane = new ModelPlaneRenderer(base, 4, 26);
        plane.addTopPlane(0.0f, -2.0f, -6.0f, 4, 6);
        plane.func_78793_a(-4.0f, 0.0f, 0.0f);
        plane.field_78795_f = (float)Math.PI;
        this.nagaPart3.func_78792_a((ModelRenderer)plane);
        plane = new ModelPlaneRenderer(base, 4, 26);
        plane.field_78809_i = true;
        plane.addTopPlane(0.0f, -2.0f, -6.0f, 4, 6);
        plane.field_78795_f = (float)Math.PI;
        this.nagaPart3.func_78792_a((ModelRenderer)plane);
        plane = new ModelPlaneRenderer(base, 8, 26);
        plane.addTopPlane(0.0f, -2.0f, 0.0f, 4, 6);
        plane.func_78793_a(-4.0f, 0.0f, 0.0f);
        this.nagaPart3.func_78792_a((ModelRenderer)plane);
        plane = new ModelPlaneRenderer(base, 8, 26);
        plane.field_78809_i = true;
        plane.addTopPlane(0.0f, -2.0f, 0.0f, 4, 6);
        this.nagaPart3.func_78792_a((ModelRenderer)plane);
        plane = new ModelPlaneRenderer(base, 0, 26);
        plane.field_78795_f = 1.5707964f;
        plane.addSidePlane(0.0f, 0.0f, -2.0f, 6, 4);
        plane.func_78793_a(-4.0f, 0.0f, 0.0f);
        this.nagaPart3.func_78792_a((ModelRenderer)plane);
        plane = new ModelPlaneRenderer(base, 0, 26);
        plane.field_78795_f = 1.5707964f;
        plane.addSidePlane(4.0f, 0.0f, -2.0f, 6, 4);
        this.nagaPart3.func_78792_a((ModelRenderer)plane);
        this.nagaPart4 = new ModelRenderer(base, 0, 0);
        this.nagaPart4.field_78805_m = this.nagaPart3.field_78805_m;
        this.nagaPart5 = new ModelRenderer(base, 0, 0);
        legPart = new ModelRenderer(base, 56, 20);
        legPart.func_78789_a(0.0f, 0.0f, -2.0f, 2, 5, 2);
        legPart.func_78793_a(-2.0f, 0.0f, 0.0f);
        legPart.field_78795_f = 1.5707964f;
        this.nagaPart5.func_78792_a(legPart);
        legPart = new ModelRenderer(base, 56, 20);
        legPart.field_78809_i = true;
        legPart.func_78789_a(0.0f, 0.0f, -2.0f, 2, 5, 2);
        legPart.field_78795_f = 1.5707964f;
        this.nagaPart5.func_78792_a(legPart);
        this.func_78792_a(this.nagaPart1);
        this.func_78792_a(this.nagaPart2);
        this.func_78792_a(this.nagaPart3);
        this.func_78792_a(this.nagaPart4);
        this.func_78792_a(this.nagaPart5);
        this.nagaPart1.func_78793_a(0.0f, 14.0f, 0.0f);
        this.nagaPart2.func_78793_a(0.0f, 18.0f, 0.6f);
        this.nagaPart3.func_78793_a(0.0f, 22.0f, -0.3f);
        this.nagaPart4.func_78793_a(0.0f, 22.0f, 5.0f);
        this.nagaPart5.func_78793_a(0.0f, 22.0f, 10.0f);
    }

    public void setRotationAngles(float par1, float par2, float par3, float par4, float par5, float par6, Entity entity) {
        this.nagaPart1.field_78796_g = MathHelper.func_76134_b((float)(par1 * 0.6662f)) * 0.26f * par2;
        this.nagaPart2.field_78796_g = MathHelper.func_76134_b((float)(par1 * 0.6662f)) * 0.5f * par2;
        this.nagaPart3.field_78796_g = MathHelper.func_76134_b((float)(par1 * 0.6662f)) * 0.26f * par2;
        this.nagaPart4.field_78796_g = -MathHelper.func_76134_b((float)(par1 * 0.6662f)) * 0.16f * par2;
        this.nagaPart5.field_78796_g = -MathHelper.func_76134_b((float)(par1 * 0.6662f)) * 0.3f * par2;
        this.nagaPart1.func_78793_a(0.0f, 14.0f, 0.0f);
        this.nagaPart2.func_78793_a(0.0f, 18.0f, 0.6f);
        this.nagaPart3.func_78793_a(0.0f, 22.0f, -0.3f);
        this.nagaPart4.func_78793_a(0.0f, 22.0f, 5.0f);
        this.nagaPart5.func_78793_a(0.0f, 22.0f, 10.0f);
        this.nagaPart1.field_78795_f = 0.0f;
        this.nagaPart2.field_78795_f = 0.0f;
        this.nagaPart3.field_78795_f = 0.0f;
        this.nagaPart4.field_78795_f = 0.0f;
        this.nagaPart5.field_78795_f = 0.0f;
        if (this.isSleeping || this.isCrawling) {
            this.nagaPart3.field_78795_f = -1.5707964f;
            this.nagaPart4.field_78795_f = -1.5707964f;
            this.nagaPart5.field_78795_f = -1.5707964f;
            this.nagaPart3.field_78797_d -= 2.0f;
            this.nagaPart3.field_78798_e = 0.9f;
            this.nagaPart4.field_78797_d += 4.0f;
            this.nagaPart4.field_78798_e = 0.9f;
            this.nagaPart5.field_78797_d += 7.0f;
            this.nagaPart5.field_78798_e = 2.9f;
        }
        if (this.isRiding) {
            this.nagaPart1.field_78797_d -= 1.0f;
            this.nagaPart1.field_78795_f = -0.19634955f;
            this.nagaPart1.field_78798_e = -1.0f;
            this.nagaPart2.field_78797_d -= 4.0f;
            this.nagaPart2.field_78798_e = -1.0f;
            this.nagaPart3.field_78797_d -= 9.0f;
            this.nagaPart3.field_78798_e -= 1.0f;
            this.nagaPart4.field_78797_d -= 13.0f;
            this.nagaPart4.field_78798_e -= 1.0f;
            this.nagaPart5.field_78797_d -= 9.0f;
            this.nagaPart5.field_78798_e -= 1.0f;
            if (this.isSneaking) {
                this.nagaPart1.field_78798_e += 5.0f;
                this.nagaPart3.field_78798_e += 5.0f;
                this.nagaPart4.field_78798_e += 5.0f;
                this.nagaPart5.field_78798_e += 4.0f;
                this.nagaPart1.field_78797_d -= 1.0f;
                this.nagaPart2.field_78797_d -= 1.0f;
                this.nagaPart3.field_78797_d -= 1.0f;
                this.nagaPart4.field_78797_d -= 1.0f;
                this.nagaPart5.field_78797_d -= 1.0f;
            }
        } else if (this.isSneaking) {
            this.nagaPart1.field_78797_d -= 1.0f;
            this.nagaPart2.field_78797_d -= 1.0f;
            this.nagaPart3.field_78797_d -= 1.0f;
            this.nagaPart4.field_78797_d -= 1.0f;
            this.nagaPart5.field_78797_d -= 1.0f;
            this.nagaPart1.field_78798_e = 5.0f;
            this.nagaPart2.field_78798_e = 3.0f;
        }
    }

    public void func_78785_a(float par7) {
        if (this.field_78807_k || !this.field_78806_j) {
            return;
        }
        this.nagaPart1.func_78785_a(par7);
        this.nagaPart3.func_78785_a(par7);
        if (!this.isRiding) {
            this.nagaPart2.func_78785_a(par7);
        }
        GlStateManager.func_179094_E();
        GlStateManager.func_179152_a((float)0.74f, (float)0.7f, (float)0.85f);
        GlStateManager.func_179109_b((float)this.nagaPart3.field_78796_g, (float)0.66f, (float)0.06f);
        this.nagaPart4.func_78785_a(par7);
        GlStateManager.func_179121_F();
        GlStateManager.func_179094_E();
        GlStateManager.func_179109_b((float)(this.nagaPart3.field_78796_g + this.nagaPart4.field_78796_g), (float)0.0f, (float)0.0f);
        this.nagaPart5.func_78785_a(par7);
        GlStateManager.func_179121_F();
    }
}

