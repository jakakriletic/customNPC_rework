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
        this.renderer.func_110776_a(npc.textureGlowLocation);
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b((int)1, (int)1);
        GlStateManager.func_179140_f();
        GlStateManager.func_179143_c((int)514);
        int c0 = 61680;
        int i = c0 % 65536;
        int j = c0 / 65536;
        OpenGlHelper.func_77475_a((int)OpenGlHelper.field_77476_b, (float)((float)i / 1.0f), (float)((float)j / 1.0f));
        GlStateManager.func_179145_e();
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.renderer.func_177087_b().func_78088_a((Entity)npc, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        this.renderer.func_177105_a((EntityLiving)npc);
        GlStateManager.func_179084_k();
        GlStateManager.func_179141_d();
        GlStateManager.func_179143_c((int)515);
    }

    public boolean func_177142_b() {
        return true;
    }
}

