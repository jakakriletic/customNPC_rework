/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.BufferBuilder
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.Tessellator
 *  net.minecraft.client.renderer.block.model.ItemCameraTransforms$TransformType
 *  net.minecraft.client.renderer.entity.Render
 *  net.minecraft.client.renderer.texture.TextureMap
 *  net.minecraft.client.renderer.vertex.DefaultVertexFormats
 *  net.minecraft.entity.Entity
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemBlock
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.EnumBlockRenderType
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.math.MathHelper
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 */
package noppes.npcs.client.renderer;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.entity.EntityProjectile;

@SideOnly(value=Side.CLIENT)
public class RenderProjectile
extends Render {
    public boolean renderWithColor = true;
    private static final ResourceLocation field_110780_a = new ResourceLocation("textures/entity/arrow.png");
    private static final ResourceLocation RES_ITEM_GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");

    public RenderProjectile() {
        super(Minecraft.getMinecraft().getRenderManager());
    }

    public void doRenderProjectile(EntityProjectile projectile, double x, double y, double z, float entityYaw, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float)((float)x), (float)((float)y), (float)((float)z));
        GlStateManager.enableRescaleNormal();
        float scale = (float)projectile.getSize() / 10.0f;
        ItemStack item = projectile.getItemDisplay();
        GlStateManager.scale((float)scale, (float)scale, (float)scale);
        if (projectile.isArrow()) {
            this.bindEntityTexture((Entity)projectile);
            GlStateManager.rotate((float)(projectile.prevRotationYaw + (projectile.rotationYaw - projectile.prevRotationYaw) * partialTicks - 90.0f), (float)0.0f, (float)1.0f, (float)0.0f);
            GlStateManager.rotate((float)(projectile.prevRotationPitch + (projectile.rotationPitch - projectile.prevRotationPitch) * partialTicks), (float)0.0f, (float)0.0f, (float)1.0f);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder BufferBuilder2 = tessellator.getBuffer();
            int i = 0;
            float f = 0.0f;
            float f1 = 0.5f;
            float f2 = (float)(0 + i * 10) / 32.0f;
            float f3 = (float)(5 + i * 10) / 32.0f;
            float f4 = 0.0f;
            float f5 = 0.15625f;
            float f6 = (float)(5 + i * 10) / 32.0f;
            float f7 = (float)(10 + i * 10) / 32.0f;
            float f8 = 0.05625f;
            GlStateManager.enableRescaleNormal();
            float f9 = (float)projectile.arrowShake - partialTicks;
            if (f9 > 0.0f) {
                float f10 = -MathHelper.sin((float)(f9 * 3.0f)) * f9;
                GlStateManager.rotate((float)f10, (float)0.0f, (float)0.0f, (float)1.0f);
            }
            GlStateManager.rotate((float)45.0f, (float)1.0f, (float)0.0f, (float)0.0f);
            GlStateManager.scale((float)f8, (float)f8, (float)f8);
            GlStateManager.translate((float)-4.0f, (float)0.0f, (float)0.0f);
            if (this.renderOutlines) {
                GlStateManager.enableColorMaterial();
                GlStateManager.enableOutlineMode((int)this.getTeamColor((Entity)projectile));
            }
            GlStateManager.glNormal3f((float)f8, (float)0.0f, (float)0.0f);
            BufferBuilder2.begin(7, DefaultVertexFormats.POSITION_TEX);
            BufferBuilder2.pos(-7.0, -2.0, -2.0).tex((double)f4, (double)f6).endVertex();
            BufferBuilder2.pos(-7.0, -2.0, 2.0).tex((double)f5, (double)f6).endVertex();
            BufferBuilder2.pos(-7.0, 2.0, 2.0).tex((double)f5, (double)f7).endVertex();
            BufferBuilder2.pos(-7.0, 2.0, -2.0).tex((double)f4, (double)f7).endVertex();
            tessellator.draw();
            GlStateManager.glNormal3f((float)(-f8), (float)0.0f, (float)0.0f);
            BufferBuilder2.begin(7, DefaultVertexFormats.POSITION_TEX);
            BufferBuilder2.pos(-7.0, 2.0, -2.0).tex((double)f4, (double)f6).endVertex();
            BufferBuilder2.pos(-7.0, 2.0, 2.0).tex((double)f5, (double)f6).endVertex();
            BufferBuilder2.pos(-7.0, -2.0, 2.0).tex((double)f5, (double)f7).endVertex();
            BufferBuilder2.pos(-7.0, -2.0, -2.0).tex((double)f4, (double)f7).endVertex();
            tessellator.draw();
            for (int j = 0; j < 4; ++j) {
                GlStateManager.rotate((float)90.0f, (float)1.0f, (float)0.0f, (float)0.0f);
                GlStateManager.glNormal3f((float)0.0f, (float)0.0f, (float)f8);
                BufferBuilder2.begin(7, DefaultVertexFormats.POSITION_TEX);
                BufferBuilder2.pos(-8.0, -2.0, 0.0).tex((double)f, (double)f2).endVertex();
                BufferBuilder2.pos(8.0, -2.0, 0.0).tex((double)f1, (double)f2).endVertex();
                BufferBuilder2.pos(8.0, 2.0, 0.0).tex((double)f1, (double)f3).endVertex();
                BufferBuilder2.pos(-8.0, 2.0, 0.0).tex((double)f, (double)f3).endVertex();
                tessellator.draw();
            }
            if (this.renderOutlines) {
                GlStateManager.disableOutlineMode();
                GlStateManager.disableColorMaterial();
            }
        } else if (projectile.is3D()) {
            GlStateManager.rotate((float)(projectile.prevRotationYaw + (projectile.rotationYaw - projectile.prevRotationYaw) * partialTicks - 180.0f), (float)0.0f, (float)1.0f, (float)0.0f);
            GlStateManager.rotate((float)(projectile.prevRotationPitch + (projectile.rotationPitch - projectile.prevRotationPitch) * partialTicks), (float)1.0f, (float)0.0f, (float)0.0f);
            GlStateManager.translate((double)0.0, (double)-0.125, (double)0.25);
            if (item.getItem() instanceof ItemBlock && Block.getBlockFromItem((Item)item.getItem()).getDefaultState().getRenderType() == EnumBlockRenderType.ENTITYBLOCK_ANIMATED) {
                GlStateManager.translate((float)0.0f, (float)0.1875f, (float)-0.3125f);
                GlStateManager.rotate((float)20.0f, (float)1.0f, (float)0.0f, (float)0.0f);
                GlStateManager.rotate((float)45.0f, (float)0.0f, (float)1.0f, (float)0.0f);
                float f8 = 0.375f;
                GlStateManager.scale((float)(-f8), (float)(-f8), (float)f8);
            }
            mc.getRenderItem().renderItem(item, ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND);
        } else {
            GlStateManager.enableRescaleNormal();
            GlStateManager.scale((float)0.5f, (float)0.5f, (float)0.5f);
            GlStateManager.rotate((float)(-this.renderManager.playerViewY), (float)0.0f, (float)1.0f, (float)0.0f);
            GlStateManager.rotate((float)this.renderManager.playerViewX, (float)1.0f, (float)0.0f, (float)0.0f);
            this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            mc.getRenderItem().renderItem(item, ItemCameraTransforms.TransformType.NONE);
            GlStateManager.disableRescaleNormal();
        }
        if (projectile.is3D() && projectile.glows()) {
            GlStateManager.disableLighting();
        }
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
        GlStateManager.enableLighting();
    }

    public void doRender(Entity par1Entity, double par2, double par4, double par6, float par8, float par9) {
        this.doRenderProjectile((EntityProjectile)par1Entity, par2, par4, par6, par8, par9);
    }

    protected ResourceLocation func_110779_a(EntityProjectile projectile) {
        return projectile.isArrow() ? field_110780_a : TextureMap.LOCATION_BLOCKS_TEXTURE;
    }

    protected ResourceLocation getEntityTexture(Entity par1Entity) {
        return this.func_110779_a((EntityProjectile)par1Entity);
    }
}

