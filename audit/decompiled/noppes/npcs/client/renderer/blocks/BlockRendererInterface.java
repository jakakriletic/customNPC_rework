/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.texture.TextureManager
 *  net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.util.ResourceLocation
 */
package noppes.npcs.client.renderer.blocks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

public abstract class BlockRendererInterface
extends TileEntitySpecialRenderer {
    protected static final ResourceLocation Stone = new ResourceLocation("customnpcs", "textures/cache/stone.png");
    protected static final ResourceLocation Iron = new ResourceLocation("customnpcs", "textures/cache/iron_block.png");
    protected static final ResourceLocation Gold = new ResourceLocation("customnpcs", "textures/cache/gold_block.png");
    protected static final ResourceLocation Diamond = new ResourceLocation("customnpcs", "textures/cache/diamond_block.png");
    protected static final ResourceLocation PlanksOak = new ResourceLocation("customnpcs", "textures/cache/planks_oak.png");
    protected static final ResourceLocation PlanksBigOak = new ResourceLocation("customnpcs", "textures/cache/planks_big_oak.png");
    protected static final ResourceLocation PlanksSpruce = new ResourceLocation("customnpcs", "textures/cache/planks_spruce.png");
    protected static final ResourceLocation PlanksBirch = new ResourceLocation("customnpcs", "textures/cache/planks_birch.png");
    protected static final ResourceLocation PlanksAcacia = new ResourceLocation("customnpcs", "textures/cache/planks_acacia.png");
    protected static final ResourceLocation PlanksJungle = new ResourceLocation("customnpcs", "textures/cache/planks_jungle.png");
    protected static final ResourceLocation Steel = new ResourceLocation("customnpcs", "textures/models/Steel.png");
    public static float[][] colorTable = new float[][]{{1.0f, 1.0f, 1.0f}, {0.95f, 0.7f, 0.2f}, {0.9f, 0.5f, 0.85f}, {0.6f, 0.7f, 0.95f}, {0.9f, 0.9f, 0.2f}, {0.5f, 0.8f, 0.1f}, {0.95f, 0.7f, 0.8f}, {0.3f, 0.3f, 0.3f}, {0.6f, 0.6f, 0.6f}, {0.3f, 0.6f, 0.7f}, {0.7f, 0.4f, 0.9f}, {0.2f, 0.4f, 0.8f}, {0.5f, 0.4f, 0.3f}, {0.4f, 0.5f, 0.2f}, {0.8f, 0.3f, 0.3f}, {0.1f, 0.1f, 0.1f}};

    public boolean playerTooFar(TileEntity tile) {
        double d8;
        double d7;
        Minecraft mc = Minecraft.func_71410_x();
        double d6 = mc.func_175606_aa().field_70165_t - (double)tile.func_174877_v().func_177958_n();
        return d6 * d6 + (d7 = mc.func_175606_aa().field_70163_u - (double)tile.func_174877_v().func_177956_o()) * d7 + (d8 = mc.func_175606_aa().field_70161_v - (double)tile.func_174877_v().func_177952_p()) * d8 > (double)(this.specialRenderDistance() * this.specialRenderDistance());
    }

    public int specialRenderDistance() {
        return 20;
    }

    public void setWoodTexture(int meta) {
        TextureManager manager = Minecraft.func_71410_x().func_110434_K();
        if (meta == 1) {
            manager.func_110577_a(PlanksSpruce);
        } else if (meta == 2) {
            manager.func_110577_a(PlanksBirch);
        } else if (meta == 3) {
            manager.func_110577_a(PlanksJungle);
        } else if (meta == 4) {
            manager.func_110577_a(PlanksAcacia);
        } else if (meta == 5) {
            manager.func_110577_a(PlanksBigOak);
        } else {
            manager.func_110577_a(PlanksOak);
        }
    }

    public static void setMaterialTexture(int meta) {
        TextureManager manager = Minecraft.func_71410_x().func_110434_K();
        if (meta == 1) {
            manager.func_110577_a(Stone);
        } else if (meta == 2) {
            manager.func_110577_a(Iron);
        } else if (meta == 3) {
            manager.func_110577_a(Gold);
        } else if (meta == 4) {
            manager.func_110577_a(Diamond);
        } else {
            manager.func_110577_a(PlanksOak);
        }
    }
}

