/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.RenderGlobal
 *  net.minecraft.client.renderer.RenderHelper
 *  net.minecraft.client.renderer.block.model.ItemCameraTransforms$TransformType
 *  net.minecraft.item.ItemStack
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.util.math.AxisAlignedBB
 *  net.minecraft.util.math.BlockPos
 */
package noppes.npcs.client.renderer.blocks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import noppes.npcs.CustomItems;
import noppes.npcs.blocks.tiles.TileCopy;
import noppes.npcs.client.renderer.blocks.BlockRendererInterface;
import noppes.npcs.schematics.Schematic;

public class BlockCopyRenderer
extends BlockRendererInterface {
    private static final ItemStack item = new ItemStack(CustomItems.copy);
    public static Schematic schematic = null;
    public static BlockPos pos = null;

    public void func_192841_a(TileEntity var1, double x, double y, double z, float var8, int blockDamage, float alpha) {
        TileCopy tile = (TileCopy)var1;
        GlStateManager.func_179094_E();
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        RenderHelper.func_74519_b();
        GlStateManager.func_179084_k();
        GlStateManager.func_179137_b((double)x, (double)y, (double)z);
        this.drawSelectionBox(new BlockPos((int)tile.width, (int)tile.height, (int)tile.length));
        GlStateManager.func_179109_b((float)0.5f, (float)0.5f, (float)0.5f);
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GlStateManager.func_179114_b((float)180.0f, (float)0.0f, (float)1.0f, (float)0.0f);
        Minecraft.func_71410_x().func_175599_af().func_181564_a(item, ItemCameraTransforms.TransformType.NONE);
        GlStateManager.func_179121_F();
    }

    public void drawSelectionBox(BlockPos pos) {
        GlStateManager.func_179090_x();
        GlStateManager.func_179140_f();
        GlStateManager.func_179129_p();
        GlStateManager.func_179084_k();
        AxisAlignedBB bb = new AxisAlignedBB(BlockPos.field_177992_a, pos);
        GlStateManager.func_179109_b((float)0.001f, (float)0.001f, (float)0.001f);
        RenderGlobal.func_189697_a((AxisAlignedBB)bb, (float)1.0f, (float)0.0f, (float)0.0f, (float)1.0f);
        GlStateManager.func_179098_w();
        GlStateManager.func_179145_e();
        GlStateManager.func_179089_o();
        GlStateManager.func_179084_k();
    }
}

