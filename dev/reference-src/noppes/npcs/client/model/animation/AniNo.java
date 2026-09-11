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
import noppes.npcs.entity.EntityNPCInterface;

public class AniNo {
    public static void setRotationAngles(float par1, float par2, float par3, float par4, float par5, float par6, Entity entity, ModelBiped model) {
        float ticks = (float)(entity.ticksExisted - ((EntityNPCInterface)entity).animationStart) / 8.0f;
        float ticks2 = (float)(entity.ticksExisted + 1 - ((EntityNPCInterface)entity).animationStart) / 8.0f;
        ticks += (ticks2 - ticks) * Minecraft.getMinecraft().getRenderPartialTicks();
        float ani = (ticks %= 2.0f) - 0.5f;
        if (ticks > 1.0f) {
            ani = 1.5f - ticks;
        }
        model.bipedHead.rotateAngleY = ani;
    }
}

