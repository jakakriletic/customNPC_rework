/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.renderer.entity.RenderLivingBase
 *  net.minecraft.client.renderer.texture.TextureMap
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.util.ResourceLocation
 */
package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.LogWriter;
import noppes.npcs.client.model.ModelWrapper;

public class NPCRendererHelper {
    private static final ModelWrapper wrapper = new ModelWrapper();

    public static String getTexture(RenderLivingBase render, Entity entity) {
        ResourceLocation location = render.func_110775_a(entity);
        if (location != null) {
            return location.toString();
        }
        return TextureMap.field_174945_f.toString();
    }

    public static void preRenderCallback(EntityLivingBase entity, float f, RenderLivingBase render) {
        render.func_77041_b(entity, f);
    }

    public static void renderModel(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, RenderLivingBase render, ModelBase main, ResourceLocation resource) {
        NPCRendererHelper.wrapper.mainModelOld = render.field_77045_g;
        if (!(main instanceof ModelWrapper)) {
            NPCRendererHelper.wrapper.wrapped = main;
            NPCRendererHelper.wrapper.texture = resource;
            render.field_77045_g = wrapper;
        }
        try {
            render.func_77036_a(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
        }
        catch (Exception e) {
            LogWriter.except(e);
        }
        render.field_77045_g = NPCRendererHelper.wrapper.mainModelOld;
    }

    public static float handleRotationFloat(EntityLivingBase entity, float par2, RenderLivingBase renderEntity) {
        return renderEntity.func_77044_a(entity, par2);
    }

    public static void drawLayers(EntityLivingBase entity, float p_177093_2_, float p_177093_3_, float p_177093_4_, float p_177093_5_, float p_177093_6_, float p_177093_7_, float p_177093_8_, RenderLivingBase renderEntity) {
        renderEntity.func_177093_a(entity, p_177093_2_, p_177093_3_, p_177093_4_, p_177093_5_, p_177093_6_, p_177093_7_, p_177093_8_);
    }
}

