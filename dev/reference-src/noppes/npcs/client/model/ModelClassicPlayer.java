/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.math.MathHelper
 */
package noppes.npcs.client.model;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.client.model.ModelPlayerAlt;

public class ModelClassicPlayer
extends ModelPlayerAlt {
    public ModelClassicPlayer(float scale) {
        super(scale, false);
    }

    @Override
    public void setRotationAngles(float par1, float par2, float par3, float par4, float par5, float par6, Entity entity) {
        super.setRotationAngles(par1, par2, par3, par4, par5, par6, entity);
        float j = 2.0f;
        if (entity.isSprinting()) {
            j = 1.0f;
        }
        this.bipedRightArm.rotateAngleX += MathHelper.cos((float)(par1 * 0.6662f + (float)Math.PI)) * j * par2;
        this.bipedLeftArm.rotateAngleX += MathHelper.cos((float)(par1 * 0.6662f)) * j * par2;
        this.bipedLeftArm.rotateAngleZ += (MathHelper.cos((float)(par1 * 0.2812f)) - 1.0f) * par2;
        this.bipedRightArm.rotateAngleZ += (MathHelper.cos((float)(par1 * 0.2312f)) + 1.0f) * par2;
        this.bipedLeftArmwear.rotateAngleX = this.bipedLeftArm.rotateAngleX;
        this.bipedLeftArmwear.rotateAngleY = this.bipedLeftArm.rotateAngleY;
        this.bipedLeftArmwear.rotateAngleZ = this.bipedLeftArm.rotateAngleZ;
        this.bipedRightArmwear.rotateAngleX = this.bipedRightArm.rotateAngleX;
        this.bipedRightArmwear.rotateAngleY = this.bipedRightArm.rotateAngleY;
        this.bipedRightArmwear.rotateAngleZ = this.bipedRightArm.rotateAngleZ;
    }
}

