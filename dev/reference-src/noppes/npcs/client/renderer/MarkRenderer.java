/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.GLAllocation
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.Tessellator
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.util.ResourceLocation
 *  org.lwjgl.opengl.GL11
 */
package noppes.npcs.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.client.model.Model2DRenderer;
import noppes.npcs.controllers.data.MarkData;
import org.lwjgl.opengl.GL11;

public class MarkRenderer {
    public static ResourceLocation markExclamation = new ResourceLocation("customnpcs", "textures/marks/exclamation.png");
    public static ResourceLocation markQuestion = new ResourceLocation("customnpcs", "textures/marks/question.png");
    public static ResourceLocation markPointer = new ResourceLocation("customnpcs", "textures/marks/pointer.png");
    public static ResourceLocation markCross = new ResourceLocation("customnpcs", "textures/marks/cross.png");
    public static ResourceLocation markSkull = new ResourceLocation("customnpcs", "textures/marks/skull.png");
    public static ResourceLocation markStar = new ResourceLocation("customnpcs", "textures/marks/star.png");
    public static int displayList = -1;

    public static void render(EntityLivingBase entity, double x, double y, double z, MarkData.Mark mark) {
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.pushMatrix();
        int color = mark.color;
        float red = (float)(color >> 16 & 0xFF) / 255.0f;
        float blue = (float)(color >> 8 & 0xFF) / 255.0f;
        float green = (float)(color & 0xFF) / 255.0f;
        GlStateManager.color((float)red, (float)blue, (float)green);
        GlStateManager.translate((double)x, (double)(y + (double)entity.height + 0.6), (double)z);
        GlStateManager.rotate((float)(-entity.rotationYawHead), (float)0.0f, (float)1.0f, (float)0.0f);
        if (mark.type == 2) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(markExclamation);
        } else if (mark.type == 1) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(markQuestion);
        } else if (mark.type == 3) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(markPointer);
        } else if (mark.type == 5) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(markCross);
        } else if (mark.type == 4) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(markSkull);
        } else if (mark.type == 6) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(markStar);
        }
        if (displayList >= 0) {
            GlStateManager.callList((int)displayList);
        } else {
            displayList = GLAllocation.generateDisplayLists((int)1);
            GL11.glNewList((int)displayList, (int)4864);
            GlStateManager.translate((double)-0.5, (double)0.0, (double)0.0);
            Model2DRenderer.renderItemIn2D(Tessellator.getInstance().getBuffer(), 0.0f, 0.0f, 1.0f, 1.0f, 32, 32, 0.0625f);
            GL11.glEndList();
        }
        GlStateManager.popMatrix();
    }
}

