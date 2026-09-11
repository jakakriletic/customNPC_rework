/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.math.BlockPos
 */
package noppes.npcs.client.renderer.blocks;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import noppes.npcs.client.model.blocks.ModelCarpentryBench;

public class BlockCarpentryBenchRenderer
extends TileEntitySpecialRenderer {
    private final ModelCarpentryBench model = new ModelCarpentryBench();
    private static final ResourceLocation TEXTURE = new ResourceLocation("customnpcs", "textures/models/carpentrybench.png");

    public void render(TileEntity te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        int rotation = 0;
        if (te != null && te.getPos() != BlockPos.ORIGIN) {
            rotation = te.getBlockMetadata() % 4;
        }
        GlStateManager.pushMatrix();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.translate((float)((float)x + 0.5f), (float)((float)y + 1.4f), (float)((float)z + 0.5f));
        GlStateManager.scale((float)0.95f, (float)0.95f, (float)0.95f);
        GlStateManager.rotate((float)180.0f, (float)0.0f, (float)0.0f, (float)1.0f);
        GlStateManager.rotate((float)(90 * rotation), (float)0.0f, (float)1.0f, (float)0.0f);
        this.bindTexture(TEXTURE);
        this.model.render(null, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0625f);
        GlStateManager.popMatrix();
    }
}

