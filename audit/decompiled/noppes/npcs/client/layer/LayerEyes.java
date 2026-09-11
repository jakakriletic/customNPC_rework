/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.BufferBuilder
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.GlStateManager$DestFactor
 *  net.minecraft.client.renderer.GlStateManager$SourceFactor
 *  net.minecraft.client.renderer.OpenGlHelper
 *  net.minecraft.client.renderer.Tessellator
 *  net.minecraft.client.renderer.entity.RenderLiving
 *  net.minecraft.client.renderer.vertex.DefaultVertexFormats
 */
package noppes.npcs.client.layer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import noppes.npcs.client.layer.LayerInterface;

public class LayerEyes
extends LayerInterface {
    private BufferBuilder tessellator;

    public LayerEyes(RenderLiving render) {
        super(render);
    }

    @Override
    public void render(float par2, float par3, float par4, float par5, float par6, float par7) {
        if (!this.playerdata.eyes.isEnabled()) {
            return;
        }
        GlStateManager.func_179094_E();
        this.model.field_78116_c.func_78794_c(0.0625f);
        GlStateManager.func_179152_a((float)par7, (float)par7, (float)(-par7));
        GlStateManager.func_179109_b((float)0.0f, (float)((this.playerdata.eyes.type == 1 ? 1 : 2) - this.playerdata.eyes.eyePos), (float)0.0f);
        GlStateManager.func_179091_B();
        GlStateManager.func_179103_j((int)7425);
        GlStateManager.func_179090_x();
        GlStateManager.func_179147_l();
        GlStateManager.func_187428_a((GlStateManager.SourceFactor)GlStateManager.SourceFactor.SRC_ALPHA, (GlStateManager.DestFactor)GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, (GlStateManager.SourceFactor)GlStateManager.SourceFactor.ONE, (GlStateManager.DestFactor)GlStateManager.DestFactor.ZERO);
        GlStateManager.func_179089_o();
        GlStateManager.func_179118_c();
        GlStateManager.func_179132_a((boolean)false);
        int i = this.npc.func_70070_b();
        int j = i % 65536;
        int k = i / 65536;
        OpenGlHelper.func_77475_a((int)OpenGlHelper.field_77476_b, (float)j, (float)k);
        Minecraft.func_71410_x().field_71460_t.func_191514_d(true);
        this.tessellator = Tessellator.func_178181_a().func_178180_c();
        this.tessellator.func_181668_a(7, DefaultVertexFormats.field_181706_f);
        this.drawLeft();
        this.drawRight();
        this.drawBrows();
        Tessellator.func_178181_a().func_78381_a();
        Minecraft.func_71410_x().field_71460_t.func_191514_d(false);
        GlStateManager.func_179132_a((boolean)true);
        GlStateManager.func_179084_k();
        GlStateManager.func_179103_j((int)7424);
        GlStateManager.func_179141_d();
        GlStateManager.func_179129_p();
        GlStateManager.func_179101_C();
        GlStateManager.func_179121_F();
        GlStateManager.func_179098_w();
    }

    private void drawLeft() {
        if (this.playerdata.eyes.pattern == 2) {
            return;
        }
        this.drawRect(3.0, -5.0, 1.0, -4.0, 0xF6F6F6, 4.01);
        this.drawRect(2.0, -5.0, 1.0, -4.0, this.playerdata.eyes.color, 4.011);
        if (this.playerdata.eyes.glint && this.npc.func_70089_S()) {
            this.drawRect(1.5, -4.9, 1.9, -4.5, -1, 4.012);
        }
        if (this.playerdata.eyes.type == 1) {
            this.drawRect(3.0, -4.0, 1.0, -3.0, 0xFFFFFF, 4.01);
            this.drawRect(2.0, -4.0, 1.0, -3.0, this.playerdata.eyes.color, 4.011);
        }
    }

    private void drawRight() {
        if (this.playerdata.eyes.pattern == 1) {
            return;
        }
        this.drawRect(-3.0, -5.0, -1.0, -4.0, 0xF6F6F6, 4.01);
        this.drawRect(-2.0, -5.0, -1.0, -4.0, this.playerdata.eyes.color, 4.011);
        if (this.playerdata.eyes.glint && this.npc.func_70089_S()) {
            this.drawRect(-1.5, -4.9, -1.1, -4.5, -1, 4.012);
        }
        if (this.playerdata.eyes.type == 1) {
            this.drawRect(-3.0, -4.0, -1.0, -3.0, 0xFFFFFF, 4.01);
            this.drawRect(-2.0, -4.0, -1.0, -3.0, this.playerdata.eyes.color, 4.011);
        }
    }

    private void drawBrows() {
        float offsetY = 0.0f;
        if (this.playerdata.eyes.blinkStart > 0L && this.npc.func_70089_S() && this.npc.field_70725_aQ == 0) {
            float f = (float)(System.currentTimeMillis() - this.playerdata.eyes.blinkStart) / 150.0f;
            if (f > 1.0f) {
                f = 2.0f - f;
            }
            if (f < 0.0f) {
                this.playerdata.eyes.blinkStart = 0L;
                f = 0.0f;
            }
            offsetY = (float)(this.playerdata.eyes.type == 1 ? 2 : 1) * f;
            this.drawRect(-3.0, -5.0, -1.0, -5.0f + offsetY, this.playerdata.eyes.skinColor, 4.013);
            this.drawRect(3.0, -5.0, 1.0, -5.0f + offsetY, this.playerdata.eyes.skinColor, 4.013);
        }
        if (this.playerdata.eyes.browThickness > 0) {
            float thickness = (float)this.playerdata.eyes.browThickness / 10.0f;
            this.drawRect(-3.0, -5.0f + offsetY, -1.0, -5.0f - thickness + offsetY, this.playerdata.eyes.browColor, 4.014);
            this.drawRect(1.0, -5.0f + offsetY, 3.0, -5.0f - thickness + offsetY, this.playerdata.eyes.browColor, 4.014);
        }
    }

    public void drawRect(double x, double y, double x2, double y2, int color, double z) {
        double j1;
        if (x < x2) {
            j1 = x;
            x = x2;
            x2 = j1;
        }
        if (y < y2) {
            j1 = y;
            y = y2;
            y2 = j1;
        }
        float f1 = (float)(color >> 16 & 0xFF) / 255.0f;
        float f2 = (float)(color >> 8 & 0xFF) / 255.0f;
        float f3 = (float)(color & 0xFF) / 255.0f;
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.tessellator.func_181662_b(x, y, z).func_181666_a(f1, f2, f3, 1.0f).func_181675_d();
        this.tessellator.func_181662_b(x, y2, z).func_181666_a(f1, f2, f3, 1.0f).func_181675_d();
        this.tessellator.func_181662_b(x2, y2, z).func_181666_a(f1, f2, f3, 1.0f).func_181675_d();
        this.tessellator.func_181662_b(x2, y, z).func_181666_a(f1, f2, f3, 1.0f).func_181675_d();
    }

    @Override
    public void rotate(float par1, float par2, float par3, float par4, float par5, float par6) {
    }
}

