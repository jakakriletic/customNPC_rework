/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.model.ModelBiped$ArmPose
 *  net.minecraft.client.model.ModelRenderer
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.math.MathHelper
 */
package noppes.npcs.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.client.model.ModelBipedAlt;
import noppes.npcs.entity.EntityNPCInterface;

public class ModelNPCGolem
extends ModelBipedAlt {
    private ModelRenderer bipedLowerBody;

    public ModelNPCGolem(float scale) {
        super(scale);
        this.init(0.0f, 0.0f);
    }

    public void init(float f, float f1) {
        int short1 = 128;
        int short2 = 128;
        float f2 = -7.0f;
        this.bipedHead = new ModelRenderer((ModelBase)this).setTextureSize(short1, short2);
        this.bipedHead.setRotationPoint(0.0f, f2, -2.0f);
        this.bipedHead.setTextureOffset(0, 0).addBox(-4.0f, -12.0f, -5.5f, 8, 10, 8, f);
        this.bipedHead.setTextureOffset(24, 0).addBox(-1.0f, -5.0f, -7.5f, 2, 4, 2, f);
        this.bipedHeadwear = new ModelRenderer((ModelBase)this).setTextureSize(short1, short2);
        this.bipedHeadwear.setRotationPoint(0.0f, f2, -2.0f);
        this.bipedHeadwear.setTextureOffset(0, 85).addBox(-4.0f, -12.0f, -5.5f, 8, 10, 8, f + 0.5f);
        this.bipedBody = new ModelRenderer((ModelBase)this).setTextureSize(short1, short2);
        this.bipedBody.setRotationPoint(0.0f, 0.0f + f2, 0.0f);
        this.bipedBody.setTextureOffset(0, 40).addBox(-9.0f, -2.0f, -6.0f, 18, 12, 11, f + 0.2f);
        this.bipedBody.setTextureOffset(0, 21).addBox(-9.0f, -2.0f, -6.0f, 18, 8, 11, f);
        this.bipedLowerBody = new ModelRenderer((ModelBase)this).setTextureSize(short1, short2);
        this.bipedLowerBody.setRotationPoint(0.0f, 0.0f + f2, 0.0f);
        this.bipedLowerBody.setTextureOffset(0, 70).addBox(-4.5f, 10.0f, -3.0f, 9, 5, 6, f + 0.5f);
        this.bipedLowerBody.setTextureOffset(30, 70).addBox(-4.5f, 6.0f, -3.0f, 9, 9, 6, f + 0.4f);
        this.bipedRightArm = new ModelRenderer((ModelBase)this).setTextureSize(short1, short2);
        this.bipedRightArm.setRotationPoint(0.0f, f2, 0.0f);
        this.bipedRightArm.setTextureOffset(60, 21).addBox(-13.0f, -2.5f, -3.0f, 4, 30, 6, f + 0.2f);
        this.bipedRightArm.setTextureOffset(80, 21).addBox(-13.0f, -2.5f, -3.0f, 4, 20, 6, f);
        this.bipedRightArm.setTextureOffset(100, 21).addBox(-13.0f, -2.5f, -3.0f, 4, 20, 6, f + 1.0f);
        this.bipedLeftArm = new ModelRenderer((ModelBase)this).setTextureSize(short1, short2);
        this.bipedLeftArm.setRotationPoint(0.0f, f2, 0.0f);
        this.bipedLeftArm.setTextureOffset(60, 58).addBox(9.0f, -2.5f, -3.0f, 4, 30, 6, f + 0.2f);
        this.bipedLeftArm.setTextureOffset(80, 58).addBox(9.0f, -2.5f, -3.0f, 4, 20, 6, f);
        this.bipedLeftArm.setTextureOffset(100, 58).addBox(9.0f, -2.5f, -3.0f, 4, 20, 6, f + 1.0f);
        this.bipedLeftLeg = new ModelRenderer((ModelBase)this, 0, 22).setTextureSize(short1, short2);
        this.bipedLeftLeg.setRotationPoint(-4.0f, 18.0f + f2, 0.0f);
        this.bipedLeftLeg.setTextureOffset(37, 0).addBox(-3.5f, -3.0f, -3.0f, 6, 16, 5, f);
        this.bipedRightLeg = new ModelRenderer((ModelBase)this, 0, 22).setTextureSize(short1, short2);
        this.bipedRightLeg.mirror = true;
        this.bipedRightLeg.setTextureOffset(60, 0).addBox(-3.5f, -3.0f, -3.0f, 6, 16, 5, f);
        this.bipedRightLeg.setRotationPoint(5.0f, 18.0f + f2, 0.0f);
    }

    public void render(Entity par1Entity, float par2, float par3, float par4, float par5, float par6, float par7) {
        super.render(par1Entity, par2, par3, par4, par5, par6, par7);
        this.bipedLowerBody.render(par7);
    }

    @Override
    public void setRotationAngles(float par1, float par2, float par3, float par4, float par5, float par6, Entity entity) {
        EntityNPCInterface npc = (EntityNPCInterface)entity;
        this.isRiding = npc.isRiding();
        if (this.isSneak && (npc.currentAnimation == 7 || npc.currentAnimation == 2)) {
            this.isSneak = false;
        }
        this.bipedHead.rotateAngleY = par4 / 57.295776f;
        this.bipedHead.rotateAngleX = par5 / 57.295776f;
        this.bipedHeadwear.rotateAngleY = this.bipedHead.rotateAngleY;
        this.bipedHeadwear.rotateAngleX = this.bipedHead.rotateAngleX;
        this.bipedLeftLeg.rotateAngleX = -1.5f * this.triangleWave(par1, 13.0f) * par2;
        this.bipedRightLeg.rotateAngleX = 1.5f * this.triangleWave(par1, 13.0f) * par2;
        this.bipedLeftLeg.rotateAngleY = 0.0f;
        this.bipedRightLeg.rotateAngleY = 0.0f;
        float f6 = MathHelper.sin((float)(this.swingProgress * (float)Math.PI));
        float f7 = MathHelper.sin((float)((16.0f - (1.0f - this.swingProgress) * (1.0f - this.swingProgress)) * (float)Math.PI));
        if ((double)this.swingProgress > 0.0) {
            this.bipedRightArm.rotateAngleZ = 0.0f;
            this.bipedLeftArm.rotateAngleZ = 0.0f;
            this.bipedRightArm.rotateAngleY = -(0.1f - f6 * 0.6f);
            this.bipedLeftArm.rotateAngleY = 0.1f - f6 * 0.6f;
            this.bipedRightArm.rotateAngleX = 0.0f;
            this.bipedLeftArm.rotateAngleX = 0.0f;
            this.bipedRightArm.rotateAngleX = -1.5707964f;
            this.bipedLeftArm.rotateAngleX = -1.5707964f;
            this.bipedRightArm.rotateAngleX -= f6 * 1.2f - f7 * 0.4f;
            this.bipedLeftArm.rotateAngleX -= f6 * 1.2f - f7 * 0.4f;
        } else if (this.rightArmPose == ModelBiped.ArmPose.BOW_AND_ARROW) {
            float f1 = 0.0f;
            float f3 = 0.0f;
            this.bipedRightArm.rotateAngleZ = 0.0f;
            this.bipedRightArm.rotateAngleX = -1.5707964f + this.bipedHead.rotateAngleX;
            this.bipedRightArm.rotateAngleX -= f1 * 1.2f - f3 * 0.4f;
            this.bipedRightArm.rotateAngleZ += MathHelper.cos((float)(par3 * 0.09f)) * 0.05f + 0.05f;
            this.bipedRightArm.rotateAngleX += MathHelper.sin((float)(par3 * 0.067f)) * 0.05f;
            this.bipedLeftArm.rotateAngleX = (-0.2f - 1.5f * this.triangleWave(par1, 13.0f)) * par2;
            this.bipedBody.rotateAngleY = -(0.1f - f1 * 0.6f) + this.bipedHead.rotateAngleY;
            this.bipedRightArm.rotateAngleY = -(0.1f - f1 * 0.6f) + this.bipedHead.rotateAngleY;
            this.bipedLeftArm.rotateAngleY = 0.1f - f1 * 0.6f + this.bipedHead.rotateAngleY;
        } else {
            this.bipedRightArm.rotateAngleX = (-0.2f + 1.5f * this.triangleWave(par1, 13.0f)) * par2;
            this.bipedLeftArm.rotateAngleX = (-0.2f - 1.5f * this.triangleWave(par1, 13.0f)) * par2;
            this.bipedBody.rotateAngleY = 0.0f;
            this.bipedRightArm.rotateAngleY = 0.0f;
            this.bipedLeftArm.rotateAngleY = 0.0f;
            this.bipedRightArm.rotateAngleZ = 0.0f;
            this.bipedLeftArm.rotateAngleZ = 0.0f;
        }
        if (this.isRiding) {
            this.bipedRightArm.rotateAngleX += -0.62831855f;
            this.bipedLeftArm.rotateAngleX += -0.62831855f;
            this.bipedLeftLeg.rotateAngleX = -1.2566371f;
            this.bipedRightLeg.rotateAngleX = -1.2566371f;
            this.bipedLeftLeg.rotateAngleY = 0.31415927f;
            this.bipedRightLeg.rotateAngleY = -0.31415927f;
        }
    }

    private float triangleWave(float par1, float par2) {
        return (Math.abs(par1 % par2 - par2 * 0.5f) - par2 * 0.25f) / (par2 * 0.25f);
    }
}

