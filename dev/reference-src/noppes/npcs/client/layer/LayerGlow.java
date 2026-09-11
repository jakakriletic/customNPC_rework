/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.OpenGlHelper
 *  net.minecraft.client.renderer.entity.layers.LayerRenderer
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLiving
 *  net.minecraft.util.ResourceLocation
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 */
package noppes.npcs.client.layer;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.client.renderer.RenderCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

@SideOnly(value=Side.CLIENT)
public class LayerGlow
implements LayerRenderer<EntityNPCInterface> {
    private final RenderCustomNpc renderer;

    public LayerGlow(RenderCustomNpc npcRenderer) {
        this.renderer = npcRenderer;
    }

    public void doRenderLayer(EntityNPCInterface npc, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        if (npc.display.getOverlayTexture().isEmpty()) {
            return;
        }
        if (npc.textureGlowLocation == null) {
            npc.textureGlowLocation = new ResourceLocation(npc.display.getOverlayTexture());
        }
        this.renderer.bindTexture(npc.textureGlowLocation);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc((int)1, (int)1);
        GlStateManager.disableLighting();
        GlStateManager.depthFunc((int)514);
        int c0 = 61680;
        int i = c0 % 65536;
        int j = c0 / 65536;
        OpenGlHelper.setLightmapTextureCoords((int)OpenGlHelper.lightmapTexUnit, (float)((float)i / 1.0f), (float)((float)j / 1.0f));
        GlStateManager.enableLighting();
        GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.renderer.getMainModel().render((Entity)npc, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        this.renderer.setLightmap((EntityLiving)npc);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.depthFunc((int)515);
    }

    public boolean shouldCombineTextures() {
        return true;
    }
}

