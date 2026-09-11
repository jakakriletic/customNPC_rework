/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.model.ModelRenderer
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.math.MathHelper
 */
package noppes.npcs.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.entity.EntityNPCInterface;

public class ModelPonyArmor
extends ModelBase {
    private boolean rainboom;
    public ModelRenderer head;
    public ModelRenderer Body;
    public ModelRenderer BodyBack;
    public ModelRenderer rightarm;
    public ModelRenderer LeftArm;
    public ModelRenderer RightLeg;
    public ModelRenderer LeftLeg;
    public ModelRenderer rightarm2;
    public ModelRenderer LeftArm2;
    public ModelRenderer RightLeg2;
    public ModelRenderer LeftLeg2;
    public boolean isPegasus = false;
    public boolean isUnicorn = false;
    public boolean isSleeping = false;
    public boolean isFlying = false;
    public boolean isGlow = false;
    public boolean isSneak = false;
    public boolean aimedBow;
    public int heldItemRight;

    public ModelPonyArmor(float f) {
        this.init(f, 0.0f);
    }

    public void init(float strech, float f) {
        float f2 = 0.0f;
        float f3 = 0.0f;
        float f4 = 0.0f;
        this.head = new ModelRenderer((ModelBase)this, 0, 0);
        this.head.func_78790_a(-4.0f, -4.0f, -6.0f, 8, 8, 8, strech);
        this.head.func_78793_a(f2, f3, f4);
        float f5 = 0.0f;
        float f6 = 0.0f;
        float f7 = 0.0f;
        this.Body = new ModelRenderer((ModelBase)this, 16, 16);
        this.Body.func_78790_a(-4.0f, 4.0f, -2.0f, 8, 8, 4, strech);
        this.Body.func_78793_a(f5, f6 + f, f7);
        this.BodyBack = new ModelRenderer((ModelBase)this, 0, 0);
        this.BodyBack.func_78790_a(-4.0f, 4.0f, 6.0f, 8, 8, 8, strech);
        this.BodyBack.func_78793_a(f5, f6 + f, f7);
        this.rightarm = new ModelRenderer((ModelBase)this, 0, 16);
        this.rightarm.func_78790_a(-2.0f, 4.0f, -2.0f, 4, 12, 4, strech);
        this.rightarm.func_78793_a(-3.0f, 8.0f + f, 0.0f);
        this.LeftArm = new ModelRenderer((ModelBase)this, 0, 16);
        this.LeftArm.field_78809_i = true;
        this.LeftArm.func_78790_a(-2.0f, 4.0f, -2.0f, 4, 12, 4, strech);
        this.LeftArm.func_78793_a(3.0f, 8.0f + f, 0.0f);
        this.RightLeg = new ModelRenderer((ModelBase)this, 0, 16);
        this.RightLeg.func_78790_a(-2.0f, 4.0f, -2.0f, 4, 12, 4, strech);
        this.RightLeg.func_78793_a(-3.0f, 0.0f + f, 0.0f);
        this.LeftLeg = new ModelRenderer((ModelBase)this, 0, 16);
        this.LeftLeg.field_78809_i = true;
        this.LeftLeg.func_78790_a(-2.0f, 4.0f, -2.0f, 4, 12, 4, strech);
        this.LeftLeg.func_78793_a(3.0f, 0.0f + f, 0.0f);
        this.rightarm2 = new ModelRenderer((ModelBase)this, 0, 16);
        this.rightarm2.func_78790_a(-2.0f, 4.0f, -2.0f, 4, 12, 4, strech * 0.5f);
        this.rightarm2.func_78793_a(-3.0f, 8.0f + f, 0.0f);
        this.LeftArm2 = new ModelRenderer((ModelBase)this, 0, 16);
        this.LeftArm2.field_78809_i = true;
        this.LeftArm2.func_78790_a(-2.0f, 4.0f, -2.0f, 4, 12, 4, strech * 0.5f);
        this.LeftArm2.func_78793_a(3.0f, 8.0f + f, 0.0f);
        this.RightLeg2 = new ModelRenderer((ModelBase)this, 0, 16);
        this.RightLeg2.func_78790_a(-2.0f, 4.0f, -2.0f, 4, 12, 4, strech * 0.5f);
        this.RightLeg2.func_78793_a(-3.0f, 0.0f + f, 0.0f);
        this.LeftLeg2 = new ModelRenderer((ModelBase)this, 0, 16);
        this.LeftLeg2.field_78809_i = true;
        this.LeftLeg2.func_78790_a(-2.0f, 4.0f, -2.0f, 4, 12, 4, strech * 0.5f);
        this.LeftLeg2.func_78793_a(3.0f, 0.0f + f, 0.0f);
    }

    public void func_78087_a(float f, float f1, float f2, float f3, float f4, float f5, Entity entity) {
        float f11;
        float f10;
        float f9;
        float f8;
        float f7;
        float f6;
        EntityNPCInterface npc = (EntityNPCInterface)entity;
        if (!this.field_78093_q) {
            boolean bl = this.field_78093_q = npc.currentAnimation == 1;
        }
        if (this.isSneak && (npc.currentAnimation == 7 || npc.currentAnimation == 2)) {
            this.isSneak = false;
        }
        this.rainboom = false;
        if (this.isSleeping) {
            f6 = 1.4f;
            f7 = 0.1f;
        } else {
            f6 = f3 / 57.29578f;
            f7 = f4 / 57.29578f;
        }
        this.head.field_78796_g = f6;
        this.head.field_78795_f = f7;
        if (!this.isFlying || !this.isPegasus) {
            f8 = MathHelper.func_76134_b((float)(f * 0.6662f + 3.141593f)) * 0.6f * f1;
            f9 = MathHelper.func_76134_b((float)(f * 0.6662f)) * 0.6f * f1;
            f10 = MathHelper.func_76134_b((float)(f * 0.6662f)) * 0.3f * f1;
            f11 = MathHelper.func_76134_b((float)(f * 0.6662f + 3.141593f)) * 0.3f * f1;
            this.rightarm.field_78796_g = 0.0f;
            this.LeftArm.field_78796_g = 0.0f;
            this.RightLeg.field_78796_g = 0.0f;
            this.LeftLeg.field_78796_g = 0.0f;
            this.rightarm2.field_78796_g = 0.0f;
            this.LeftArm2.field_78796_g = 0.0f;
            this.RightLeg2.field_78796_g = 0.0f;
            this.LeftLeg2.field_78796_g = 0.0f;
        } else {
            if (f1 < 0.9999f) {
                this.rainboom = false;
                f8 = MathHelper.func_76126_a((float)(0.0f - f1 * 0.5f));
                f9 = MathHelper.func_76126_a((float)(0.0f - f1 * 0.5f));
                f10 = MathHelper.func_76126_a((float)(f1 * 0.5f));
                f11 = MathHelper.func_76126_a((float)(f1 * 0.5f));
            } else {
                this.rainboom = true;
                f8 = 4.712f;
                f9 = 4.712f;
                f10 = 1.571f;
                f11 = 1.571f;
            }
            this.rightarm.field_78796_g = 0.2f;
            this.LeftArm.field_78796_g = -0.2f;
            this.RightLeg.field_78796_g = -0.2f;
            this.LeftLeg.field_78796_g = 0.2f;
            this.rightarm2.field_78796_g = 0.2f;
            this.LeftArm2.field_78796_g = -0.2f;
            this.RightLeg2.field_78796_g = -0.2f;
            this.LeftLeg2.field_78796_g = 0.2f;
        }
        if (this.isSleeping) {
            f8 = 4.712f;
            f9 = 4.712f;
            f10 = 1.571f;
            f11 = 1.571f;
        }
        this.rightarm.field_78795_f = f8;
        this.LeftArm.field_78795_f = f9;
        this.RightLeg.field_78795_f = f10;
        this.LeftLeg.field_78795_f = f11;
        this.rightarm.field_78808_h = 0.0f;
        this.LeftArm.field_78808_h = 0.0f;
        this.rightarm2.field_78795_f = f8;
        this.LeftArm2.field_78795_f = f9;
        this.RightLeg2.field_78795_f = f10;
        this.LeftLeg2.field_78795_f = f11;
        this.rightarm2.field_78808_h = 0.0f;
        this.LeftArm2.field_78808_h = 0.0f;
        if (this.heldItemRight != 0 && !this.rainboom && !this.isUnicorn) {
            this.rightarm.field_78795_f = this.rightarm.field_78795_f * 0.5f - 0.3141593f;
            this.rightarm2.field_78795_f = this.rightarm2.field_78795_f * 0.5f - 0.3141593f;
        }
        float f12 = 0.0f;
        if (f5 > -9990.0f && !this.isUnicorn) {
            f12 = MathHelper.func_76126_a((float)(MathHelper.func_76129_c((float)f5) * 3.141593f * 2.0f)) * 0.2f;
        }
        this.Body.field_78796_g = (float)((double)f12 * 0.2);
        this.BodyBack.field_78796_g = (float)((double)f12 * 0.2);
        float f13 = MathHelper.func_76126_a((float)this.Body.field_78796_g) * 5.0f;
        float f14 = MathHelper.func_76134_b((float)this.Body.field_78796_g) * 5.0f;
        float f15 = 4.0f;
        if (this.isSneak && !this.isFlying) {
            f15 = 0.0f;
        }
        if (this.isSleeping) {
            f15 = 2.6f;
        }
        if (this.rainboom) {
            this.rightarm.field_78798_e = f13 + 2.0f;
            this.rightarm2.field_78798_e = f13 + 2.0f;
            this.LeftArm.field_78798_e = 0.0f - f13 + 2.0f;
            this.LeftArm2.field_78798_e = 0.0f - f13 + 2.0f;
        } else {
            this.rightarm.field_78798_e = f13 + 1.0f;
            this.rightarm2.field_78798_e = f13 + 1.0f;
            this.LeftArm.field_78798_e = 0.0f - f13 + 1.0f;
            this.LeftArm2.field_78798_e = 0.0f - f13 + 1.0f;
        }
        this.rightarm.field_78800_c = 0.0f - f14 - 1.0f + f15;
        this.rightarm2.field_78800_c = 0.0f - f14 - 1.0f + f15;
        this.LeftArm.field_78800_c = f14 + 1.0f - f15;
        this.LeftArm2.field_78800_c = f14 + 1.0f - f15;
        this.RightLeg.field_78800_c = 0.0f - f14 - 1.0f + f15;
        this.RightLeg2.field_78800_c = 0.0f - f14 - 1.0f + f15;
        this.LeftLeg.field_78800_c = f14 + 1.0f - f15;
        this.LeftLeg2.field_78800_c = f14 + 1.0f - f15;
        this.rightarm.field_78796_g += this.Body.field_78796_g;
        this.rightarm2.field_78796_g += this.Body.field_78796_g;
        this.LeftArm.field_78796_g += this.Body.field_78796_g;
        this.LeftArm2.field_78796_g += this.Body.field_78796_g;
        this.LeftArm.field_78795_f += this.Body.field_78796_g;
        this.LeftArm2.field_78795_f += this.Body.field_78796_g;
        this.rightarm.field_78797_d = 8.0f;
        this.LeftArm.field_78797_d = 8.0f;
        this.RightLeg.field_78797_d = 4.0f;
        this.LeftLeg.field_78797_d = 4.0f;
        this.rightarm2.field_78797_d = 8.0f;
        this.LeftArm2.field_78797_d = 8.0f;
        this.RightLeg2.field_78797_d = 4.0f;
        this.LeftLeg2.field_78797_d = 4.0f;
        if (f5 > -9990.0f && !this.isUnicorn) {
            float f16 = f5;
            f16 = 1.0f - f5;
            f16 *= f16 * f16;
            f16 = 1.0f - f16;
            float f21 = MathHelper.func_76126_a((float)(f16 * 3.141593f));
            float f26 = MathHelper.func_76126_a((float)(f5 * 3.141593f));
            float f17 = f26 * -(this.head.field_78795_f - 0.7f) * 0.75f;
        }
        if (this.isSneak && !this.isFlying) {
            float f35;
            float f33;
            float f31;
            float f17 = 0.4f;
            float f22 = 7.0f;
            float f27 = -4.0f;
            this.Body.field_78795_f = f17;
            this.Body.field_78797_d = f22;
            this.Body.field_78798_e = f27;
            this.BodyBack.field_78795_f = f17;
            this.BodyBack.field_78797_d = f22;
            this.BodyBack.field_78798_e = f27;
            this.RightLeg.field_78795_f -= 0.0f;
            this.LeftLeg.field_78795_f -= 0.0f;
            this.rightarm.field_78795_f -= 0.4f;
            this.LeftArm.field_78795_f -= 0.4f;
            this.RightLeg.field_78798_e = 10.0f;
            this.LeftLeg.field_78798_e = 10.0f;
            this.RightLeg.field_78797_d = 7.0f;
            this.LeftLeg.field_78797_d = 7.0f;
            this.RightLeg2.field_78795_f -= 0.0f;
            this.LeftLeg2.field_78795_f -= 0.0f;
            this.rightarm2.field_78795_f -= 0.4f;
            this.LeftArm2.field_78795_f -= 0.4f;
            this.RightLeg2.field_78798_e = 10.0f;
            this.LeftLeg2.field_78798_e = 10.0f;
            this.RightLeg2.field_78797_d = 7.0f;
            this.LeftLeg2.field_78797_d = 7.0f;
            if (this.isSleeping) {
                f31 = 2.0f;
                f33 = -1.0f;
                f35 = 1.0f;
            } else {
                f31 = 6.0f;
                f33 = -2.0f;
                f35 = 0.0f;
            }
            this.head.field_78797_d = f31;
            this.head.field_78798_e = f33;
            this.head.field_78800_c = f35;
        } else {
            float f18 = 0.0f;
            float f23 = 0.0f;
            float f28 = 0.0f;
            this.Body.field_78795_f = f18;
            this.Body.field_78797_d = f23;
            this.Body.field_78798_e = f28;
            this.BodyBack.field_78795_f = f18;
            this.BodyBack.field_78797_d = f23;
            this.BodyBack.field_78798_e = f28;
            this.RightLeg.field_78798_e = 10.0f;
            this.LeftLeg.field_78798_e = 10.0f;
            this.RightLeg.field_78797_d = 8.0f;
            this.LeftLeg.field_78797_d = 8.0f;
            this.RightLeg2.field_78798_e = 10.0f;
            this.LeftLeg2.field_78798_e = 10.0f;
            this.RightLeg2.field_78797_d = 8.0f;
            this.LeftLeg2.field_78797_d = 8.0f;
            float f32 = MathHelper.func_76134_b((float)(f2 * 0.09f)) * 0.05f + 0.05f;
            float f34 = MathHelper.func_76126_a((float)(f2 * 0.067f)) * 0.05f;
            float f36 = 0.0f;
            float f37 = 0.0f;
            this.head.field_78797_d = f36;
            this.head.field_78798_e = f37;
        }
        if (this.isSleeping) {
            this.rightarm.field_78798_e += 6.0f;
            this.LeftArm.field_78798_e += 6.0f;
            this.RightLeg.field_78798_e -= 8.0f;
            this.LeftLeg.field_78798_e -= 8.0f;
            this.rightarm.field_78797_d += 2.0f;
            this.LeftArm.field_78797_d += 2.0f;
            this.RightLeg.field_78797_d += 2.0f;
            this.LeftLeg.field_78797_d += 2.0f;
            this.rightarm2.field_78798_e += 6.0f;
            this.LeftArm2.field_78798_e += 6.0f;
            this.RightLeg2.field_78798_e -= 8.0f;
            this.LeftLeg2.field_78798_e -= 8.0f;
            this.rightarm2.field_78797_d += 2.0f;
            this.LeftArm2.field_78797_d += 2.0f;
            this.RightLeg2.field_78797_d += 2.0f;
            this.LeftLeg2.field_78797_d += 2.0f;
        }
        if (this.aimedBow && !this.isUnicorn) {
            float f20 = 0.0f;
            float f25 = 0.0f;
            this.rightarm.field_78808_h = 0.0f;
            this.rightarm.field_78796_g = -(0.1f - f20 * 0.6f) + this.head.field_78796_g;
            this.rightarm.field_78795_f = 4.712f + this.head.field_78795_f;
            this.rightarm.field_78795_f -= f20 * 1.2f - f25 * 0.4f;
            float f29 = f2;
            this.rightarm.field_78808_h += MathHelper.func_76134_b((float)(f29 * 0.09f)) * 0.05f + 0.05f;
            this.rightarm.field_78795_f += MathHelper.func_76126_a((float)(f29 * 0.067f)) * 0.05f;
            this.rightarm2.field_78808_h = 0.0f;
            this.rightarm2.field_78796_g = -(0.1f - f20 * 0.6f) + this.head.field_78796_g;
            this.rightarm2.field_78795_f = 4.712f + this.head.field_78795_f;
            this.rightarm2.field_78795_f -= f20 * 1.2f - f25 * 0.4f;
            this.rightarm2.field_78808_h += MathHelper.func_76134_b((float)(f29 * 0.09f)) * 0.05f + 0.05f;
            this.rightarm2.field_78795_f += MathHelper.func_76126_a((float)(f29 * 0.067f)) * 0.05f;
            this.rightarm.field_78798_e += 1.0f;
            this.rightarm2.field_78798_e += 1.0f;
        }
    }

    public void func_78088_a(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        float scale = f5;
        this.func_78087_a(f, f1, f2, f3, f4, f5, entity);
        this.head.func_78785_a(scale);
        this.Body.func_78785_a(scale);
        this.BodyBack.func_78785_a(scale);
        this.LeftArm.func_78785_a(scale);
        this.rightarm.func_78785_a(scale);
        this.LeftLeg.func_78785_a(scale);
        this.RightLeg.func_78785_a(scale);
        this.LeftArm2.func_78785_a(scale);
        this.rightarm2.func_78785_a(scale);
        this.LeftLeg2.func_78785_a(scale);
        this.RightLeg2.func_78785_a(scale);
    }
}

