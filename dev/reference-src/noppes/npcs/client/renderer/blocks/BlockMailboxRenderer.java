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
import noppes.npcs.client.model.blocks.ModelMailboxUS;
import noppes.npcs.client.model.blocks.ModelMailboxWow;

public class BlockMailboxRenderer
extends TileEntitySpecialRenderer {
    private final ModelMailboxUS model = new ModelMailboxUS();
    private final ModelMailboxWow model2 = new ModelMailboxWow();
    private static final ResourceLocation text1 = new ResourceLocation("customnpcs", "textures/models/mailbox1.png");
    private static final ResourceLocation text2 = new ResourceLocation("customnpcs", "textures/models/mailbox2.png");
    private static final ResourceLocation text3 = new ResourceLocation("customnpcs", "textures/models/mailbox3.png");
    private int type;

    public BlockMailboxRenderer(int i) {
        this.type = i;
    }

    public void render(TileEntity te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        int meta = 0;
        int type = this.type;
        if (te != null && te.getPos() != BlockPos.ORIGIN) {
            meta = te.getBlockMetadata() | 4;
            type = te.getBlockMetadata() >> 2;
        }
        GlStateManager.pushMatrix();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.translate((float)((float)x + 0.5f), (float)((float)y + 1.5f), (float)((float)z + 0.5f));
        GlStateManager.rotate((float)180.0f, (float)0.0f, (float)0.0f, (float)1.0f);
        GlStateManager.rotate((float)(90 * meta), (float)0.0f, (float)1.0f, (float)0.0f);
        if (type == 0) {
            this.bindTexture(text1);
            this.model.render(null, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0625f);
        }
        if (type == 1) {
            this.bindTexture(text2);
            this.model2.render(null, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0625f);
        }
        if (type == 2) {
            this.bindTexture(text3);
            this.model2.render(null, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0625f);
        }
        GlStateManager.popMatrix();
    }
}

