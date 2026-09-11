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
    private static final ResourceLocation field_110798_h = new ResourceLocation("textures/misc/enchanted_item_glint.png");

    public RenderProjectile() {
        super(Minecraft.func_71410_x().func_175598_ae());
    }

    public void doRenderProjectile(EntityProjectile projectile, double x, double y, double z, float entityYaw, float partialTicks) {
        Minecraft mc = Minecraft.func_71410_x();
        GlStateManager.func_179094_E();
        GlStateManager.func_179109_b((float)((float)x), (float)((float)y), (float)((float)z));
        GlStateManager.func_179091_B();
        float scale = (float)projectile.getSize() / 10.0f;
        ItemStack item = projectile.getItemDisplay();
        GlStateManager.func_179152_a((float)scale, (float)scale, (float)scale);
        if (projectile.isArrow()) {
            this.func_180548_c((Entity)projectile);
            GlStateManager.func_179114_b((float)(projectile.field_70126_B + (projectile.field_70177_z - projectile.field_70126_B) * partialTicks - 90.0f), (float)0.0f, (float)1.0f, (float)0.0f);
            GlStateManager.func_179114_b((float)(projectile.field_70127_C + (projectile.field_70125_A - projectile.field_70127_C) * partialTicks), (float)0.0f, (float)0.0f, (float)1.0f);
            Tessellator tessellator = Tessellator.func_178181_a();
            BufferBuilder BufferBuilder2 = tessellator.func_178180_c();
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
            GlStateManager.func_179091_B();
            float f9 = (float)projectile.arrowShake - partialTicks;
            if (f9 > 0.0f) {
                float f10 = -MathHelper.func_76126_a((float)(f9 * 3.0f)) * f9;
                GlStateManager.func_179114_b((float)f10, (float)0.0f, (float)0.0f, (float)1.0f);
            }
            GlStateManager.func_179114_b((float)45.0f, (float)1.0f, (float)0.0f, (float)0.0f);
            GlStateManager.func_179152_a((float)f8, (float)f8, (float)f8);
            GlStateManager.func_179109_b((float)-4.0f, (float)0.0f, (float)0.0f);
            if (this.field_188301_f) {
                GlStateManager.func_179142_g();
                GlStateManager.func_187431_e((int)this.func_188298_c((Entity)projectile));
            }
            GlStateManager.func_187432_a((float)f8, (float)0.0f, (float)0.0f);
            BufferBuilder2.func_181668_a(7, DefaultVertexFormats.field_181707_g);
            BufferBuilder2.func_181662_b(-7.0, -2.0, -2.0).func_187315_a((double)f4, (double)f6).func_181675_d();
            BufferBuilder2.func_181662_b(-7.0, -2.0, 2.0).func_187315_a((double)f5, (double)f6).func_181675_d();
            BufferBuilder2.func_181662_b(-7.0, 2.0, 2.0).func_187315_a((double)f5, (double)f7).func_181675_d();
            BufferBuilder2.func_181662_b(-7.0, 2.0, -2.0).func_187315_a((double)f4, (double)f7).func_181675_d();
            tessellator.func_78381_a();
            GlStateManager.func_187432_a((float)(-f8), (float)0.0f, (float)0.0f);
            BufferBuilder2.func_181668_a(7, DefaultVertexFormats.field_181707_g);
            BufferBuilder2.func_181662_b(-7.0, 2.0, -2.0).func_187315_a((double)f4, (double)f6).func_181675_d();
            BufferBuilder2.func_181662_b(-7.0, 2.0, 2.0).func_187315_a((double)f5, (double)f6).func_181675_d();
            BufferBuilder2.func_181662_b(-7.0, -2.0, 2.0).func_187315_a((double)f5, (double)f7).func_181675_d();
            BufferBuilder2.func_181662_b(-7.0, -2.0, -2.0).func_187315_a((double)f4, (double)f7).func_181675_d();
            tessellator.func_78381_a();
            for (int j = 0; j < 4; ++j) {
                GlStateManager.func_179114_b((float)90.0f, (float)1.0f, (float)0.0f, (float)0.0f);
                GlStateManager.func_187432_a((float)0.0f, (float)0.0f, (float)f8);
                BufferBuilder2.func_181668_a(7, DefaultVertexFormats.field_181707_g);
                BufferBuilder2.func_181662_b(-8.0, -2.0, 0.0).func_187315_a((double)f, (double)f2).func_181675_d();
                BufferBuilder2.func_181662_b(8.0, -2.0, 0.0).func_187315_a((double)f1, (double)f2).func_181675_d();
                BufferBuilder2.func_181662_b(8.0, 2.0, 0.0).func_187315_a((double)f1, (double)f3).func_181675_d();
                BufferBuilder2.func_181662_b(-8.0, 2.0, 0.0).func_187315_a((double)f, (double)f3).func_181675_d();
                tessellator.func_78381_a();
            }
            if (this.field_188301_f) {
                GlStateManager.func_187417_n();
                GlStateManager.func_179119_h();
            }
        } else if (projectile.is3D()) {
            GlStateManager.func_179114_b((float)(projectile.field_70126_B + (projectile.field_70177_z - projectile.field_70126_B) * partialTicks - 180.0f), (float)0.0f, (float)1.0f, (float)0.0f);
            GlStateManager.func_179114_b((float)(projectile.field_70127_C + (projectile.field_70125_A - projectile.field_70127_C) * partialTicks), (float)1.0f, (float)0.0f, (float)0.0f);
            GlStateManager.func_179137_b((double)0.0, (double)-0.125, (double)0.25);
            if (item.func_77973_b() instanceof ItemBlock && Block.func_149634_a((Item)item.func_77973_b()).func_176223_P().func_185901_i() == EnumBlockRenderType.ENTITYBLOCK_ANIMATED) {
                GlStateManager.func_179109_b((float)0.0f, (float)0.1875f, (float)-0.3125f);
                GlStateManager.func_179114_b((float)20.0f, (float)1.0f, (float)0.0f, (float)0.0f);
                GlStateManager.func_179114_b((float)45.0f, (float)0.0f, (float)1.0f, (float)0.0f);
                float f8 = 0.375f;
                GlStateManager.func_179152_a((float)(-f8), (float)(-f8), (float)f8);
            }
            mc.func_175599_af().func_181564_a(item, ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND);
        } else {
            GlStateManager.func_179091_B();
            GlStateManager.func_179152_a((float)0.5f, (float)0.5f, (float)0.5f);
            GlStateManager.func_179114_b((float)(-this.field_76990_c.field_78735_i), (float)0.0f, (float)1.0f, (float)0.0f);
            GlStateManager.func_179114_b((float)this.field_76990_c.field_78732_j, (float)1.0f, (float)0.0f, (float)0.0f);
            this.func_110776_a(TextureMap.field_110575_b);
            mc.func_175599_af().func_181564_a(item, ItemCameraTransforms.TransformType.NONE);
            GlStateManager.func_179101_C();
        }
        if (projectile.is3D() && projectile.glows()) {
            GlStateManager.func_179140_f();
        }
        GlStateManager.func_179101_C();
        GlStateManager.func_179121_F();
        GlStateManager.func_179145_e();
    }

    public void func_76986_a(Entity par1Entity, double par2, double par4, double par6, float par8, float par9) {
        this.doRenderProjectile((EntityProjectile)par1Entity, par2, par4, par6, par8, par9);
    }

    protected ResourceLocation func_110779_a(EntityProjectile projectile) {
        return projectile.isArrow() ? field_110780_a : TextureMap.field_110575_b;
    }

    protected ResourceLocation func_110775_a(Entity par1Entity) {
        return this.func_110779_a((EntityProjectile)par1Entity);
    }
}

