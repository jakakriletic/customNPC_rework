/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.model.ModelBiped
 *  net.minecraft.client.model.ModelRenderer
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.entity.RenderLiving
 *  net.minecraft.client.renderer.entity.layers.LayerRenderer
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.player.EntityPlayer
 */
package noppes.npcs.client.layer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import noppes.npcs.ModelData;
import noppes.npcs.ModelPartData;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.entity.EntityCustomNpc;

public abstract class LayerInterface
implements LayerRenderer {
    protected RenderLiving render;
    protected EntityCustomNpc npc;
    protected ModelData playerdata;
    public ModelBiped model;

    public LayerInterface(RenderLiving render) {
        this.render = render;
        this.model = (ModelBiped)render.func_177087_b();
    }

    public void setColor(ModelPartData data, EntityLivingBase entity) {
    }

    public void preRender(ModelPartData data) {
        if (data.playerTexture) {
            ClientProxy.bindTexture(this.npc.textureLocation);
        } else {
            ClientProxy.bindTexture(data.getResource());
        }
        if (this.npc.field_70737_aN > 0 || this.npc.field_70725_aQ > 0) {
            return;
        }
        int color = data.color;
        if (this.npc.display.getTint() != 0xFFFFFF) {
            color = data.color != 0xFFFFFF ? this.blend(data.color, this.npc.display.getTint(), 0.5f) : this.npc.display.getTint();
        }
        float red = (float)(color >> 16 & 0xFF) / 255.0f;
        float green = (float)(color >> 8 & 0xFF) / 255.0f;
        float blue = (float)(color & 0xFF) / 255.0f;
        GlStateManager.func_179131_c((float)red, (float)green, (float)blue, (float)(this.npc.func_82150_aj() ? 0.15f : 0.99f));
    }

    private int blend(int color1, int color2, float ratio) {
        if (ratio >= 1.0f) {
            return color2;
        }
        if (ratio <= 0.0f) {
            return color1;
        }
        int aR = (color1 & 0xFF0000) >> 16;
        int aG = (color1 & 0xFF00) >> 8;
        int aB = color1 & 0xFF;
        int bR = (color2 & 0xFF0000) >> 16;
        int bG = (color2 & 0xFF00) >> 8;
        int bB = color2 & 0xFF;
        int R = (int)((float)aR + (float)(bR - aR) * ratio);
        int G = (int)((float)aG + (float)(bG - aG) * ratio);
        int B = (int)((float)aB + (float)(bB - aB) * ratio);
        return R << 16 | G << 8 | B;
    }

    public void func_177141_a(EntityLivingBase entity, float par2, float par3, float par8, float par4, float par5, float par6, float par7) {
        this.npc = (EntityCustomNpc)entity;
        if (this.npc.func_98034_c((EntityPlayer)Minecraft.func_71410_x().field_71439_g)) {
            return;
        }
        this.playerdata = this.npc.modelData;
        this.model = (ModelBiped)this.render.func_177087_b();
        this.rotate(par2, par3, par4, par5, par6, par7);
        GlStateManager.func_179094_E();
        if (entity.func_82150_aj()) {
            GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)0.15f);
            GlStateManager.func_179132_a((boolean)false);
            GlStateManager.func_179147_l();
            GlStateManager.func_179112_b((int)770, (int)771);
            GlStateManager.func_179092_a((int)516, (float)0.003921569f);
        }
        if (this.npc.field_70737_aN > 0 || this.npc.field_70725_aQ > 0) {
            GlStateManager.func_179131_c((float)1.0f, (float)0.0f, (float)0.0f, (float)0.3f);
        }
        if (this.npc.func_70093_af()) {
            GlStateManager.func_179109_b((float)0.0f, (float)0.2f, (float)0.0f);
        }
        GlStateManager.func_179091_B();
        this.render(par2, par3, par4, par5, par6, par7);
        GlStateManager.func_179101_C();
        if (entity.func_82150_aj()) {
            GlStateManager.func_179084_k();
            GlStateManager.func_179092_a((int)516, (float)0.1f);
            GlStateManager.func_179132_a((boolean)true);
        }
        GlStateManager.func_179121_F();
    }

    public void setRotation(ModelRenderer model, float x, float y, float z) {
        model.field_78795_f = x;
        model.field_78796_g = y;
        model.field_78808_h = z;
    }

    public boolean func_177142_b() {
        return false;
    }

    public abstract void render(float var1, float var2, float var3, float var4, float var5, float var6);

    public abstract void rotate(float var1, float var2, float var3, float var4, float var5, float var6);
}

