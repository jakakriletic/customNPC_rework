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

    public void render(TileEntity var1, double x, double y, double z, float var8, int blockDamage, float alpha) {
        TileCopy tile = (TileCopy)var1;
        GlStateManager.pushMatrix();
        GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.disableBlend();
        GlStateManager.translate((double)x, (double)y, (double)z);
        this.drawSelectionBox(new BlockPos((int)tile.width, (int)tile.height, (int)tile.length));
        GlStateManager.translate((float)0.5f, (float)0.5f, (float)0.5f);
        GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GlStateManager.rotate((float)180.0f, (float)0.0f, (float)1.0f, (float)0.0f);
        Minecraft.getMinecraft().getRenderItem().renderItem(item, ItemCameraTransforms.TransformType.NONE);
        GlStateManager.popMatrix();
    }

    public void drawSelectionBox(BlockPos pos) {
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableBlend();
        AxisAlignedBB bb = new AxisAlignedBB(BlockPos.ORIGIN, pos);
        GlStateManager.translate((float)0.001f, (float)0.001f, (float)0.001f);
        RenderGlobal.drawSelectionBoundingBox((AxisAlignedBB)bb, (float)1.0f, (float)0.0f, (float)0.0f, (float)1.0f);
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
    }
}

