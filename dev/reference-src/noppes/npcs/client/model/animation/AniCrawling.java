/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.ModelBiped
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.math.MathHelper
 */
package noppes.npcs.client.model.animation;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

public class AniCrawling {
    public static void setRotationAngles(float par1, float par2, float par3, float par4, float par5, float par6, Entity entity, ModelBiped model) {
        model.bipedHead.rotateAngleZ = -par4 / 57.295776f;
        model.bipedHead.rotateAngleY = 0.0f;
        model.bipedHeadwear.rotateAngleX = model.bipedHead.rotateAngleX = -0.95993114f;
        model.bipedHeadwear.rotateAngleY = model.bipedHead.rotateAngleY;
        model.bipedHeadwear.rotateAngleZ = model.bipedHead.rotateAngleZ;
        if ((double)par2 > 0.25) {
            par2 = 0.25f;
        }
        float movement = MathHelper.cos((float)(par1 * 0.8f + (float)Math.PI)) * par2;
        model.bipedLeftArm.rotateAngleX = (float)Math.PI - movement * 0.25f;
        model.bipedLeftArm.rotateAngleY = movement * -0.46f;
        model.bipedLeftArm.rotateAngleZ = movement * -0.2f;
        model.bipedLeftArm.rotationPointY = 2.0f - movement * 9.0f;
        model.bipedRightArm.rotateAngleX = (float)Math.PI + movement * 0.25f;
        model.bipedRightArm.rotateAngleY = movement * -0.4f;
        model.bipedRightArm.rotateAngleZ = movement * -0.2f;
        model.bipedRightArm.rotationPointY = 2.0f + movement * 9.0f;
        model.bipedBody.rotateAngleY = movement * 0.1f;
        model.bipedBody.rotateAngleX = 0.0f;
        model.bipedBody.rotateAngleZ = movement * 0.1f;
        model.bipedLeftLeg.rotateAngleX = movement * 0.1f;
        model.bipedLeftLeg.rotateAngleY = movement * 0.1f;
        model.bipedLeftLeg.rotateAngleZ = -0.122173056f - movement * 0.25f;
        model.bipedLeftLeg.rotationPointY = 10.4f + movement * 9.0f;
        model.bipedLeftLeg.rotationPointZ = movement * 0.6f;
        model.bipedRightLeg.rotateAngleX = movement * -0.1f;
        model.bipedRightLeg.rotateAngleY = movement * 0.1f;
        model.bipedRightLeg.rotateAngleZ = 0.122173056f - movement * 0.25f;
        model.bipedRightLeg.rotationPointY = 10.4f - movement * 9.0f;
        model.bipedRightLeg.rotationPointZ = movement * -0.6f;
    }
}

