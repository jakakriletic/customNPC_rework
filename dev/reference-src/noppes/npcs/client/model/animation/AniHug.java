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

public class AniHug {
    public static void setRotationAngles(float par1, float par2, float par3, float par4, float par5, float par6, Entity entity, ModelBiped base) {
        float f6 = MathHelper.sin((float)(base.swingProgress * 3.141593f));
        float f7 = MathHelper.sin((float)((1.0f - (1.0f - base.swingProgress) * (1.0f - base.swingProgress)) * 3.141593f));
        base.bipedRightArm.rotateAngleZ = 0.0f;
        base.bipedLeftArm.rotateAngleZ = 0.0f;
        base.bipedRightArm.rotateAngleY = -(0.1f - f6 * 0.6f);
        base.bipedLeftArm.rotateAngleY = 0.1f;
        base.bipedRightArm.rotateAngleX = -1.570796f;
        base.bipedLeftArm.rotateAngleX = -1.570796f;
        base.bipedRightArm.rotateAngleX -= f6 * 1.2f - f7 * 0.4f;
        base.bipedRightArm.rotateAngleZ += MathHelper.cos((float)(par3 * 0.09f)) * 0.05f + 0.05f;
        base.bipedLeftArm.rotateAngleZ -= MathHelper.cos((float)(par3 * 0.09f)) * 0.05f + 0.05f;
        base.bipedRightArm.rotateAngleX += MathHelper.sin((float)(par3 * 0.067f)) * 0.05f;
        base.bipedLeftArm.rotateAngleX -= MathHelper.sin((float)(par3 * 0.067f)) * 0.05f;
    }
}

