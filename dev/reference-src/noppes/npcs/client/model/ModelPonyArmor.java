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
        this.head.addBox(-4.0f, -4.0f, -6.0f, 8, 8, 8, strech);
        this.head.setRotationPoint(f2, f3, f4);
        float f5 = 0.0f;
        float f6 = 0.0f;
        float f7 = 0.0f;
        this.Body = new ModelRenderer((ModelBase)this, 16, 16);
        this.Body.addBox(-4.0f, 4.0f, -2.0f, 8, 8, 4, strech);
        this.Body.setRotationPoint(f5, f6 + f, f7);
        this.BodyBack = new ModelRenderer((ModelBase)this, 0, 0);
        this.BodyBack.addBox(-4.0f, 4.0f, 6.0f, 8, 8, 8, strech);
        this.BodyBack.setRotationPoint(f5, f6 + f, f7);
        this.rightarm = new ModelRenderer((ModelBase)this, 0, 16);
        this.rightarm.addBox(-2.0f, 4.0f, -2.0f, 4, 12, 4, strech);
        this.rightarm.setRotationPoint(-3.0f, 8.0f + f, 0.0f);
        this.LeftArm = new ModelRenderer((ModelBase)this, 0, 16);
        this.LeftArm.mirror = true;
        this.LeftArm.addBox(-2.0f, 4.0f, -2.0f, 4, 12, 4, strech);
        this.LeftArm.setRotationPoint(3.0f, 8.0f + f, 0.0f);
        this.RightLeg = new ModelRenderer((ModelBase)this, 0, 16);
        this.RightLeg.addBox(-2.0f, 4.0f, -2.0f, 4, 12, 4, strech);
        this.RightLeg.setRotationPoint(-3.0f, 0.0f + f, 0.0f);
        this.LeftLeg = new ModelRenderer((ModelBase)this, 0, 16);
        this.LeftLeg.mirror = true;
        this.LeftLeg.addBox(-2.0f, 4.0f, -2.0f, 4, 12, 4, strech);
        this.LeftLeg.setRotationPoint(3.0f, 0.0f + f, 0.0f);
        this.rightarm2 = new ModelRenderer((ModelBase)this, 0, 16);
        this.rightarm2.addBox(-2.0f, 4.0f, -2.0f, 4, 12, 4, strech * 0.5f);
        this.rightarm2.setRotationPoint(-3.0f, 8.0f + f, 0.0f);
        this.LeftArm2 = new ModelRenderer((ModelBase)this, 0, 16);
        this.LeftArm2.mirror = true;
        this.LeftArm2.addBox(-2.0f, 4.0f, -2.0f, 4, 12, 4, strech * 0.5f);
        this.LeftArm2.setRotationPoint(3.0f, 8.0f + f, 0.0f);
        this.RightLeg2 = new ModelRenderer((ModelBase)this, 0, 16);
        this.RightLeg2.addBox(-2.0f, 4.0f, -2.0f, 4, 12, 4, strech * 0.5f);
        this.RightLeg2.setRotationPoint(-3.0f, 0.0f + f, 0.0f);
        this.LeftLeg2 = new ModelRenderer((ModelBase)this, 0, 16);
        this.LeftLeg2.mirror = true;
        this.LeftLeg2.addBox(-2.0f, 4.0f, -2.0f, 4, 12, 4, strech * 0.5f);
        this.LeftLeg2.setRotationPoint(3.0f, 0.0f + f, 0.0f);
    }

    public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity entity) {
        float f11;
        float f10;
        float f9;
        float f8;
        float f7;
        float f6;
        EntityNPCInterface npc = (EntityNPCInterface)entity;
        if (!this.isRiding) {
            boolean bl = this.isRiding = npc.currentAnimation == 1;
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
        this.head.rotateAngleY = f6;
        this.head.rotateAngleX = f7;
        if (!this.isFlying || !this.isPegasus) {
            f8 = MathHelper.cos((float)(f * 0.6662f + 3.141593f)) * 0.6f * f1;
            f9 = MathHelper.cos((float)(f * 0.6662f)) * 0.6f * f1;
            f10 = MathHelper.cos((float)(f * 0.6662f)) * 0.3f * f1;
            f11 = MathHelper.cos((float)(f * 0.6662f + 3.141593f)) * 0.3f * f1;
            this.rightarm.rotateAngleY = 0.0f;
            this.LeftArm.rotateAngleY = 0.0f;
            this.RightLeg.rotateAngleY = 0.0f;
            this.LeftLeg.rotateAngleY = 0.0f;
            this.rightarm2.rotateAngleY = 0.0f;
            this.LeftArm2.rotateAngleY = 0.0f;
            this.RightLeg2.rotateAngleY = 0.0f;
            this.LeftLeg2.rotateAngleY = 0.0f;
        } else {
            if (f1 < 0.9999f) {
                this.rainboom = false;
                f8 = MathHelper.sin((float)(0.0f - f1 * 0.5f));
                f9 = MathHelper.sin((float)(0.0f - f1 * 0.5f));
                f10 = MathHelper.sin((float)(f1 * 0.5f));
                f11 = MathHelper.sin((float)(f1 * 0.5f));
            } else {
                this.rainboom = true;
                f8 = 4.712f;
                f9 = 4.712f;
                f10 = 1.571f;
                f11 = 1.571f;
            }
            this.rightarm.rotateAngleY = 0.2f;
            this.LeftArm.rotateAngleY = -0.2f;
            this.RightLeg.rotateAngleY = -0.2f;
            this.LeftLeg.rotateAngleY = 0.2f;
            this.rightarm2.rotateAngleY = 0.2f;
            this.LeftArm2.rotateAngleY = -0.2f;
            this.RightLeg2.rotateAngleY = -0.2f;
            this.LeftLeg2.rotateAngleY = 0.2f;
        }
        if (this.isSleeping) {
            f8 = 4.712f;
            f9 = 4.712f;
            f10 = 1.571f;
            f11 = 1.571f;
        }
        this.rightarm.rotateAngleX = f8;
        this.LeftArm.rotateAngleX = f9;
        this.RightLeg.rotateAngleX = f10;
        this.LeftLeg.rotateAngleX = f11;
        this.rightarm.rotateAngleZ = 0.0f;
        this.LeftArm.rotateAngleZ = 0.0f;
        this.rightarm2.rotateAngleX = f8;
        this.LeftArm2.rotateAngleX = f9;
        this.RightLeg2.rotateAngleX = f10;
        this.LeftLeg2.rotateAngleX = f11;
        this.rightarm2.rotateAngleZ = 0.0f;
        this.LeftArm2.rotateAngleZ = 0.0f;
        if (this.heldItemRight != 0 && !this.rainboom && !this.isUnicorn) {
            this.rightarm.rotateAngleX = this.rightarm.rotateAngleX * 0.5f - 0.3141593f;
            this.rightarm2.rotateAngleX = this.rightarm2.rotateAngleX * 0.5f - 0.3141593f;
        }
        float f12 = 0.0f;
        if (f5 > -9990.0f && !this.isUnicorn) {
            f12 = MathHelper.sin((float)(MathHelper.sqrt((float)f5) * 3.141593f * 2.0f)) * 0.2f;
        }
        this.Body.rotateAngleY = (float)((double)f12 * 0.2);
        this.BodyBack.rotateAngleY = (float)((double)f12 * 0.2);
        float f13 = MathHelper.sin((float)this.Body.rotateAngleY) * 5.0f;
        float f14 = MathHelper.cos((float)this.Body.rotateAngleY) * 5.0f;
        float f15 = 4.0f;
        if (this.isSneak && !this.isFlying) {
            f15 = 0.0f;
        }
        if (this.isSleeping) {
            f15 = 2.6f;
        }
        if (this.rainboom) {
            this.rightarm.rotationPointZ = f13 + 2.0f;
            this.rightarm2.rotationPointZ = f13 + 2.0f;
            this.LeftArm.rotationPointZ = 0.0f - f13 + 2.0f;
            this.LeftArm2.rotationPointZ = 0.0f - f13 + 2.0f;
        } else {
            this.rightarm.rotationPointZ = f13 + 1.0f;
            this.rightarm2.rotationPointZ = f13 + 1.0f;
            this.LeftArm.rotationPointZ = 0.0f - f13 + 1.0f;
            this.LeftArm2.rotationPointZ = 0.0f - f13 + 1.0f;
        }
        this.rightarm.rotationPointX = 0.0f - f14 - 1.0f + f15;
        this.rightarm2.rotationPointX = 0.0f - f14 - 1.0f + f15;
        this.LeftArm.rotationPointX = f14 + 1.0f - f15;
        this.LeftArm2.rotationPointX = f14 + 1.0f - f15;
        this.RightLeg.rotationPointX = 0.0f - f14 - 1.0f + f15;
        this.RightLeg2.rotationPointX = 0.0f - f14 - 1.0f + f15;
        this.LeftLeg.rotationPointX = f14 + 1.0f - f15;
        this.LeftLeg2.rotationPointX = f14 + 1.0f - f15;
        this.rightarm.rotateAngleY += this.Body.rotateAngleY;
        this.rightarm2.rotateAngleY += this.Body.rotateAngleY;
        this.LeftArm.rotateAngleY += this.Body.rotateAngleY;
        this.LeftArm2.rotateAngleY += this.Body.rotateAngleY;
        this.LeftArm.rotateAngleX += this.Body.rotateAngleY;
        this.LeftArm2.rotateAngleX += this.Body.rotateAngleY;
        this.rightarm.rotationPointY = 8.0f;
        this.LeftArm.rotationPointY = 8.0f;
        this.RightLeg.rotationPointY = 4.0f;
        this.LeftLeg.rotationPointY = 4.0f;
        this.rightarm2.rotationPointY = 8.0f;
        this.LeftArm2.rotationPointY = 8.0f;
        this.RightLeg2.rotationPointY = 4.0f;
        this.LeftLeg2.rotationPointY = 4.0f;
        if (f5 > -9990.0f && !this.isUnicorn) {
            float f16 = f5;
            f16 = 1.0f - f5;
            f16 *= f16 * f16;
            f16 = 1.0f - f16;
            float f21 = MathHelper.sin((float)(f16 * 3.141593f));
            float f26 = MathHelper.sin((float)(f5 * 3.141593f));
            float f17 = f26 * -(this.head.rotateAngleX - 0.7f) * 0.75f;
        }
        if (this.isSneak && !this.isFlying) {
            float f35;
            float f33;
            float f31;
            float f17 = 0.4f;
            float f22 = 7.0f;
            float f27 = -4.0f;
            this.Body.rotateAngleX = f17;
            this.Body.rotationPointY = f22;
            this.Body.rotationPointZ = f27;
            this.BodyBack.rotateAngleX = f17;
            this.BodyBack.rotationPointY = f22;
            this.BodyBack.rotationPointZ = f27;
            this.RightLeg.rotateAngleX -= 0.0f;
            this.LeftLeg.rotateAngleX -= 0.0f;
            this.rightarm.rotateAngleX -= 0.4f;
            this.LeftArm.rotateAngleX -= 0.4f;
            this.RightLeg.rotationPointZ = 10.0f;
            this.LeftLeg.rotationPointZ = 10.0f;
            this.RightLeg.rotationPointY = 7.0f;
            this.LeftLeg.rotationPointY = 7.0f;
            this.RightLeg2.rotateAngleX -= 0.0f;
            this.LeftLeg2.rotateAngleX -= 0.0f;
            this.rightarm2.rotateAngleX -= 0.4f;
            this.LeftArm2.rotateAngleX -= 0.4f;
            this.RightLeg2.rotationPointZ = 10.0f;
            this.LeftLeg2.rotationPointZ = 10.0f;
            this.RightLeg2.rotationPointY = 7.0f;
            this.LeftLeg2.rotationPointY = 7.0f;
            if (this.isSleeping) {
                f31 = 2.0f;
                f33 = -1.0f;
                f35 = 1.0f;
            } else {
                f31 = 6.0f;
                f33 = -2.0f;
                f35 = 0.0f;
            }
            this.head.rotationPointY = f31;
            this.head.rotationPointZ = f33;
            this.head.rotationPointX = f35;
        } else {
            float f18 = 0.0f;
            float f23 = 0.0f;
            float f28 = 0.0f;
            this.Body.rotateAngleX = f18;
            this.Body.rotationPointY = f23;
            this.Body.rotationPointZ = f28;
            this.BodyBack.rotateAngleX = f18;
            this.BodyBack.rotationPointY = f23;
            this.BodyBack.rotationPointZ = f28;
            this.RightLeg.rotationPointZ = 10.0f;
            this.LeftLeg.rotationPointZ = 10.0f;
            this.RightLeg.rotationPointY = 8.0f;
            this.LeftLeg.rotationPointY = 8.0f;
            this.RightLeg2.rotationPointZ = 10.0f;
            this.LeftLeg2.rotationPointZ = 10.0f;
            this.RightLeg2.rotationPointY = 8.0f;
            this.LeftLeg2.rotationPointY = 8.0f;
            float f32 = MathHelper.cos((float)(f2 * 0.09f)) * 0.05f + 0.05f;
            float f34 = MathHelper.sin((float)(f2 * 0.067f)) * 0.05f;
            float f36 = 0.0f;
            float f37 = 0.0f;
            this.head.rotationPointY = f36;
            this.head.rotationPointZ = f37;
        }
        if (this.isSleeping) {
            this.rightarm.rotationPointZ += 6.0f;
            this.LeftArm.rotationPointZ += 6.0f;
            this.RightLeg.rotationPointZ -= 8.0f;
            this.LeftLeg.rotationPointZ -= 8.0f;
            this.rightarm.rotationPointY += 2.0f;
            this.LeftArm.rotationPointY += 2.0f;
            this.RightLeg.rotationPointY += 2.0f;
            this.LeftLeg.rotationPointY += 2.0f;
            this.rightarm2.rotationPointZ += 6.0f;
            this.LeftArm2.rotationPointZ += 6.0f;
            this.RightLeg2.rotationPointZ -= 8.0f;
            this.LeftLeg2.rotationPointZ -= 8.0f;
            this.rightarm2.rotationPointY += 2.0f;
            this.LeftArm2.rotationPointY += 2.0f;
            this.RightLeg2.rotationPointY += 2.0f;
            this.LeftLeg2.rotationPointY += 2.0f;
        }
        if (this.aimedBow && !this.isUnicorn) {
            float f20 = 0.0f;
            float f25 = 0.0f;
            this.rightarm.rotateAngleZ = 0.0f;
            this.rightarm.rotateAngleY = -(0.1f - f20 * 0.6f) + this.head.rotateAngleY;
            this.rightarm.rotateAngleX = 4.712f + this.head.rotateAngleX;
            this.rightarm.rotateAngleX -= f20 * 1.2f - f25 * 0.4f;
            float f29 = f2;
            this.rightarm.rotateAngleZ += MathHelper.cos((float)(f29 * 0.09f)) * 0.05f + 0.05f;
            this.rightarm.rotateAngleX += MathHelper.sin((float)(f29 * 0.067f)) * 0.05f;
            this.rightarm2.rotateAngleZ = 0.0f;
            this.rightarm2.rotateAngleY = -(0.1f - f20 * 0.6f) + this.head.rotateAngleY;
            this.rightarm2.rotateAngleX = 4.712f + this.head.rotateAngleX;
            this.rightarm2.rotateAngleX -= f20 * 1.2f - f25 * 0.4f;
            this.rightarm2.rotateAngleZ += MathHelper.cos((float)(f29 * 0.09f)) * 0.05f + 0.05f;
            this.rightarm2.rotateAngleX += MathHelper.sin((float)(f29 * 0.067f)) * 0.05f;
            this.rightarm.rotationPointZ += 1.0f;
            this.rightarm2.rotationPointZ += 1.0f;
        }
    }

    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        float scale = f5;
        this.setRotationAngles(f, f1, f2, f3, f4, f5, entity);
        this.head.render(scale);
        this.Body.render(scale);
        this.BodyBack.render(scale);
        this.LeftArm.render(scale);
        this.rightarm.render(scale);
        this.LeftLeg.render(scale);
        this.RightLeg.render(scale);
        this.LeftArm2.render(scale);
        this.rightarm2.render(scale);
        this.LeftLeg2.render(scale);
        this.RightLeg2.render(scale);
    }
}

