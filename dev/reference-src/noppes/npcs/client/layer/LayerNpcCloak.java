/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.entity.RenderLiving
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.math.MathHelper
 */
package noppes.npcs.client.layer;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.ModelPartConfig;
import noppes.npcs.client.layer.LayerInterface;
import noppes.npcs.client.model.ModelPlayerAlt;
import noppes.npcs.constants.EnumParts;

public class LayerNpcCloak
extends LayerInterface {
    public LayerNpcCloak(RenderLiving render) {
        super(render);
    }

    @Override
    public void render(float par2, float par3, float par4, float par5, float par6, float par7) {
        if (this.npc.textureCloakLocation == null) {
            if (this.npc.display.getCapeTexture() == null || this.npc.display.getCapeTexture().isEmpty() || !(this.model instanceof ModelPlayerAlt)) {
                return;
            }
            this.npc.textureCloakLocation = new ResourceLocation(this.npc.display.getCapeTexture());
        }
        GlStateManager.color((float)0.0f, (float)0.0f, (float)0.0f);
        this.render.bindTexture(this.npc.textureCloakLocation);
        GlStateManager.pushMatrix();
        ModelPartConfig config = this.playerdata.getPartConfig(EnumParts.BODY);
        if (this.npc.isSneaking()) {
            GlStateManager.translate((float)0.0f, (float)0.2f, (float)0.0f);
        }
        GlStateManager.translate((float)config.transX, (float)config.transY, (float)config.transZ);
        GlStateManager.translate((float)0.0f, (float)0.0f, (float)0.125f);
        double d = this.npc.field_20066_r + (this.npc.field_20063_u - this.npc.field_20066_r) * (double)par7 - (this.npc.prevPosX + (this.npc.posX - this.npc.prevPosX) * (double)par7);
        double d1 = this.npc.field_20065_s + (this.npc.field_20062_v - this.npc.field_20065_s) * (double)par7 - (this.npc.prevPosY + (this.npc.posY - this.npc.prevPosY) * (double)par7);
        double d2 = this.npc.field_20064_t + (this.npc.field_20061_w - this.npc.field_20064_t) * (double)par7 - (this.npc.prevPosZ + (this.npc.posZ - this.npc.prevPosZ) * (double)par7);
        float f11 = this.npc.prevRenderYawOffset + (this.npc.renderYawOffset - this.npc.prevRenderYawOffset) * par7;
        double d3 = MathHelper.sin((float)(f11 * 3.141593f / 180.0f));
        double d4 = -MathHelper.cos((float)(f11 * 3.141593f / 180.0f));
        float f14 = (float)(d * d3 + d2 * d4) * 100.0f;
        float f15 = (float)(d * d4 - d2 * d3) * 100.0f;
        if (f14 < 0.0f) {
            f14 = 0.0f;
        }
        float f16 = this.npc.prevRotationYaw + (this.npc.rotationYaw - this.npc.prevRotationYaw) * par7;
        float f13 = 5.0f;
        if (this.npc.isSneaking()) {
            f13 += 25.0f;
        }
        GlStateManager.rotate((float)(6.0f + f14 / 2.0f + f13), (float)1.0f, (float)0.0f, (float)0.0f);
        GlStateManager.rotate((float)(f15 / 2.0f), (float)0.0f, (float)0.0f, (float)1.0f);
        GlStateManager.rotate((float)(-f15 / 2.0f), (float)0.0f, (float)1.0f, (float)0.0f);
        GlStateManager.rotate((float)180.0f, (float)0.0f, (float)1.0f, (float)0.0f);
        ((ModelPlayerAlt)this.model).renderCape(0.0625f);
        GlStateManager.popMatrix();
    }

    @Override
    public void rotate(float par1, float par2, float par3, float par4, float par5, float par6) {
    }
}

