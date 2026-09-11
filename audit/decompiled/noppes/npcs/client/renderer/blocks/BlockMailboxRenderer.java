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

    public void func_192841_a(TileEntity te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        int meta = 0;
        int type = this.type;
        if (te != null && te.func_174877_v() != BlockPos.field_177992_a) {
            meta = te.func_145832_p() | 4;
            type = te.func_145832_p() >> 2;
        }
        GlStateManager.func_179094_E();
        GlStateManager.func_179145_e();
        GlStateManager.func_179084_k();
        GlStateManager.func_179109_b((float)((float)x + 0.5f), (float)((float)y + 1.5f), (float)((float)z + 0.5f));
        GlStateManager.func_179114_b((float)180.0f, (float)0.0f, (float)0.0f, (float)1.0f);
        GlStateManager.func_179114_b((float)(90 * meta), (float)0.0f, (float)1.0f, (float)0.0f);
        if (type == 0) {
            this.func_147499_a(text1);
            this.model.func_78088_a(null, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0625f);
        }
        if (type == 1) {
            this.func_147499_a(text2);
            this.model2.func_78088_a(null, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0625f);
        }
        if (type == 2) {
            this.func_147499_a(text3);
            this.model2.func_78088_a(null, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0625f);
        }
        GlStateManager.func_179121_F();
    }
}

