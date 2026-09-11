/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.model.ModelBiped
 *  net.minecraft.entity.Entity
 */
package noppes.npcs.client.model.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;

public class AniDancing {
    public static void setRotationAngles(float par1, float par2, float par3, float par4, float par5, float par6, Entity entity, ModelBiped model) {
        float dancing = (float)entity.ticksExisted / 4.0f;
        float dancing2 = (float)(entity.ticksExisted + 1) / 4.0f;
        dancing += (dancing2 - dancing) * Minecraft.getMinecraft().getRenderPartialTicks();
        float x = (float)Math.sin(dancing);
        float y = (float)Math.abs(Math.cos(dancing));
        model.bipedHeadwear.rotationPointX = model.bipedHead.rotationPointX = x * 0.75f;
        model.bipedHeadwear.rotationPointY = model.bipedHead.rotationPointY = y * 1.25f - 0.02f;
        model.bipedHeadwear.rotationPointZ = model.bipedHead.rotationPointZ = -y * 0.75f;
        model.bipedLeftArm.rotationPointX += x * 0.25f;
        model.bipedLeftArm.rotationPointY += y * 1.25f;
        model.bipedRightArm.rotationPointX += x * 0.25f;
        model.bipedRightArm.rotationPointY += y * 1.25f;
        model.bipedBody.rotationPointX = x * 0.25f;
    }
}

