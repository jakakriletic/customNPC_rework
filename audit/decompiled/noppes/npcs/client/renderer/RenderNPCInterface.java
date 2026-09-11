/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.minecraft.MinecraftProfileTexture
 *  com.mojang.authlib.minecraft.MinecraftProfileTexture$Type
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.GlStateManager$DestFactor
 *  net.minecraft.client.renderer.GlStateManager$SourceFactor
 *  net.minecraft.client.renderer.IImageBuffer
 *  net.minecraft.client.renderer.entity.RenderLiving
 *  net.minecraft.client.renderer.texture.ITextureObject
 *  net.minecraft.client.renderer.texture.TextureManager
 *  net.minecraft.client.resources.DefaultPlayerSkin
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLiving
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.util.ResourceLocation
 *  org.lwjgl.opengl.GL11
 */
package noppes.npcs.client.renderer;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import java.io.File;
import java.security.MessageDigest;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.client.ImageDownloadAlt;
import noppes.npcs.client.renderer.ImageBufferDownloadAlt;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import org.lwjgl.opengl.GL11;

public class RenderNPCInterface<T extends EntityNPCInterface>
extends RenderLiving<T> {
    public static int LastTextureTick;

    public RenderNPCInterface(ModelBase model, float f) {
        super(Minecraft.func_71410_x().func_175598_ae(), model, f);
    }

    public void renderName(T npc, double d, double d1, double d2) {
        if (npc == null || !this.func_177070_b((EntityLiving)npc) || this.field_76990_c.field_78734_h == null) {
            return;
        }
        double d0 = npc.func_70068_e(this.field_76990_c.field_78734_h);
        if (d0 > 512.0) {
            return;
        }
        if (((EntityNPCInterface)((Object)npc)).messages != null) {
            float height = ((EntityNPCInterface)((Object)npc)).baseHeight / 5.0f * (float)((EntityNPCInterface)((Object)npc)).display.getSize();
            float offset = ((EntityNPCInterface)((Object)npc)).field_70131_O * (1.2f + (!((EntityNPCInterface)((Object)npc)).display.showName() ? 0.0f : (((EntityNPCInterface)((Object)npc)).display.getTitle().isEmpty() ? 0.15f : 0.25f)));
            ((EntityNPCInterface)((Object)npc)).messages.renderMessages(d, d1 + (double)offset, d2, 0.666667f * height, ((EntityNPCInterface)((Object)npc)).isInRange(this.field_76990_c.field_78734_h, 4.0));
        }
        float scale = ((EntityNPCInterface)((Object)npc)).baseHeight / 5.0f * (float)((EntityNPCInterface)((Object)npc)).display.getSize();
        if (((EntityNPCInterface)((Object)npc)).display.showName()) {
            this.renderLivingLabel((EntityNPCInterface)((Object)npc), (float)d, (float)d1 + ((EntityNPCInterface)((Object)npc)).field_70131_O - 0.06f * scale, (float)d2, 64, ((EntityNPCInterface)((Object)npc)).func_70005_c_(), ((EntityNPCInterface)((Object)npc)).display.getTitle());
        }
    }

    public void func_76979_b(Entity par1Entity, double par2, double par4, double par6, float par8, float par9) {
        EntityNPCInterface npc = (EntityNPCInterface)par1Entity;
        this.field_76989_e = npc.field_70130_N;
        if (!npc.isKilled()) {
            super.func_76979_b(par1Entity, par2, par4, par6, par8, par9);
        }
    }

    protected void renderLivingLabel(EntityNPCInterface npc, float d, float d1, float d2, int i, String name, String title) {
        FontRenderer fontrenderer = this.func_76983_a();
        float f1 = npc.baseHeight / 5.0f * (float)npc.display.getSize();
        float f2 = 0.01666667f * f1;
        GlStateManager.func_179094_E();
        GlStateManager.func_179109_b((float)d, (float)d1, (float)d2);
        GL11.glNormal3f((float)0.0f, (float)1.0f, (float)0.0f);
        GlStateManager.func_179114_b((float)(-this.field_76990_c.field_78735_i), (float)0.0f, (float)1.0f, (float)0.0f);
        GlStateManager.func_179114_b((float)this.field_76990_c.field_78732_j, (float)1.0f, (float)0.0f, (float)0.0f);
        float height = f1 / 6.5f * 2.0f;
        int color = npc.getFaction().color;
        GlStateManager.func_179140_f();
        GlStateManager.func_179132_a((boolean)false);
        GlStateManager.func_179109_b((float)0.0f, (float)height, (float)0.0f);
        GlStateManager.func_179147_l();
        GlStateManager.func_187428_a((GlStateManager.SourceFactor)GlStateManager.SourceFactor.SRC_ALPHA, (GlStateManager.DestFactor)GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, (GlStateManager.SourceFactor)GlStateManager.SourceFactor.ONE, (GlStateManager.DestFactor)GlStateManager.DestFactor.ZERO);
        if (!title.isEmpty()) {
            title = "<" + title + ">";
            float f3 = 0.01666667f * f1 * 0.6f;
            GlStateManager.func_179109_b((float)0.0f, (float)(-f1 / 6.5f * 0.4f), (float)0.0f);
            GlStateManager.func_179152_a((float)(-f3), (float)(-f3), (float)f3);
            fontrenderer.func_78276_b(title, -fontrenderer.func_78256_a(title) / 2, 0, color);
            GlStateManager.func_179152_a((float)(1.0f / -f3), (float)(1.0f / -f3), (float)(1.0f / f3));
            GlStateManager.func_179109_b((float)0.0f, (float)(f1 / 6.5f * 0.85f), (float)0.0f);
        }
        GlStateManager.func_179152_a((float)(-f2), (float)(-f2), (float)f2);
        if (npc.isInRange(this.field_76990_c.field_78734_h, 4.0)) {
            GlStateManager.func_179097_i();
            fontrenderer.func_78276_b(name, -fontrenderer.func_78256_a(name) / 2, 0, color + 0x55000000);
            GlStateManager.func_179126_j();
        }
        GlStateManager.func_179132_a((boolean)true);
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        fontrenderer.func_78276_b(name, -fontrenderer.func_78256_a(name) / 2, 0, color);
        GlStateManager.func_179145_e();
        GlStateManager.func_179084_k();
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GlStateManager.func_179121_F();
    }

    protected void renderColor(EntityNPCInterface npc) {
        if (npc.field_70737_aN <= 0 && npc.field_70725_aQ <= 0) {
            float red = (float)(npc.display.getTint() >> 16 & 0xFF) / 255.0f;
            float green = (float)(npc.display.getTint() >> 8 & 0xFF) / 255.0f;
            float blue = (float)(npc.display.getTint() & 0xFF) / 255.0f;
            GlStateManager.func_179131_c((float)red, (float)green, (float)blue, (float)1.0f);
        }
    }

    private void renderLiving(T npc, double d, double d1, double d2, float xoffset, float yoffset, float zoffset) {
    }

    protected void applyRotations(T npc, float f, float f1, float f2) {
        if (((EntityNPCInterface)((Object)npc)).func_70089_S() && ((EntityNPCInterface)((Object)npc)).func_70608_bn()) {
            GlStateManager.func_179114_b((float)((EntityNPCInterface)((Object)npc)).ais.orientation, (float)0.0f, (float)1.0f, (float)0.0f);
            GlStateManager.func_179114_b((float)this.func_77037_a((EntityLivingBase)npc), (float)0.0f, (float)0.0f, (float)1.0f);
            GlStateManager.func_179114_b((float)270.0f, (float)0.0f, (float)1.0f, (float)0.0f);
        } else if (((EntityNPCInterface)((Object)npc)).func_70089_S() && ((EntityNPCInterface)((Object)npc)).currentAnimation == 7) {
            GlStateManager.func_179114_b((float)(270.0f - f1), (float)0.0f, (float)1.0f, (float)0.0f);
            float scale = (float)((EntityCustomNpc)((Object)npc)).display.getSize() / 5.0f;
            GlStateManager.func_179109_b((float)(-scale + ((EntityCustomNpc)((Object)npc)).modelData.getLegsY() * scale), (float)0.14f, (float)0.0f);
            GlStateManager.func_179114_b((float)270.0f, (float)0.0f, (float)0.0f, (float)1.0f);
            GlStateManager.func_179114_b((float)270.0f, (float)0.0f, (float)1.0f, (float)0.0f);
        } else {
            super.func_77043_a(npc, f, f1, f2);
        }
    }

    protected void preRenderCallback(T npc, float f) {
        this.renderColor((EntityNPCInterface)((Object)npc));
        int size = ((EntityNPCInterface)((Object)npc)).display.getSize();
        GlStateManager.func_179152_a((float)(((EntityNPCInterface)((Object)npc)).scaleX / 5.0f * (float)size), (float)(((EntityNPCInterface)((Object)npc)).scaleY / 5.0f * (float)size), (float)(((EntityNPCInterface)((Object)npc)).scaleZ / 5.0f * (float)size));
    }

    public void doRender(T npc, double d, double d1, double d2, float f, float f1) {
        if (((EntityNPCInterface)((Object)npc)).isKilled() && ((EntityNPCInterface)((Object)npc)).stats.hideKilledBody && ((EntityNPCInterface)((Object)npc)).field_70725_aQ > 20) {
            return;
        }
        if (((EntityNPCInterface)((Object)npc)).display.getBossbar() != 1 && (((EntityNPCInterface)((Object)npc)).display.getBossbar() != 2 || !((EntityNPCInterface)((Object)npc)).isAttacking()) || ((EntityNPCInterface)((Object)npc)).isKilled() || ((EntityNPCInterface)((Object)npc)).field_70725_aQ > 20 || ((EntityNPCInterface)((Object)npc)).canSee((Entity)Minecraft.func_71410_x().field_71439_g)) {
            // empty if block
        }
        if (((EntityNPCInterface)((Object)npc)).ais.getStandingType() == 3 && !((EntityNPCInterface)((Object)npc)).isWalking() && !((EntityNPCInterface)((Object)npc)).isInteracting()) {
            ((EntityNPCInterface)((Object)npc)).field_70760_ar = ((EntityNPCInterface)((Object)npc)).field_70761_aq = (float)((EntityNPCInterface)((Object)npc)).ais.orientation;
        }
        super.func_76986_a(npc, d, d1, d2, f, f1);
    }

    protected void renderModel(T npc, float par2, float par3, float par4, float par5, float par6, float par7) {
        super.func_77036_a(npc, par2, par3, par4, par5, par6, par7);
        if (!((EntityNPCInterface)((Object)npc)).display.getOverlayTexture().isEmpty()) {
            GlStateManager.func_179143_c((int)515);
            if (((EntityNPCInterface)((Object)npc)).textureGlowLocation == null) {
                ((EntityNPCInterface)((Object)npc)).textureGlowLocation = new ResourceLocation(((EntityNPCInterface)((Object)npc)).display.getOverlayTexture());
            }
            this.func_110776_a(((EntityNPCInterface)((Object)npc)).textureGlowLocation);
            float f1 = 1.0f;
            GlStateManager.func_179147_l();
            GlStateManager.func_179112_b((int)1, (int)1);
            GlStateManager.func_179140_f();
            if (((EntityNPCInterface)((Object)npc)).func_82150_aj()) {
                GlStateManager.func_179132_a((boolean)false);
            } else {
                GlStateManager.func_179132_a((boolean)true);
            }
            GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            GlStateManager.func_179094_E();
            GlStateManager.func_179152_a((float)1.001f, (float)1.001f, (float)1.001f);
            this.field_77045_g.func_78088_a(npc, par2, par3, par4, par5, par6, par7);
            GlStateManager.func_179121_F();
            GlStateManager.func_179145_e();
            GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)f1);
            GlStateManager.func_179143_c((int)515);
            GlStateManager.func_179084_k();
        }
    }

    protected float handleRotationFloat(T npc, float par2) {
        if (((EntityNPCInterface)((Object)npc)).isKilled() || !((EntityNPCInterface)((Object)npc)).display.getHasLivingAnimation()) {
            return 0.0f;
        }
        return super.func_77044_a(npc, par2);
    }

    protected void renderLivingAt(T npc, double d, double d1, double d2) {
        this.field_76989_e = (float)((EntityNPCInterface)((Object)npc)).display.getSize() / 10.0f;
        float xOffset = 0.0f;
        float yOffset = ((EntityNPCInterface)((Object)npc)).currentAnimation == 0 ? ((EntityNPCInterface)((Object)npc)).ais.bodyOffsetY / 10.0f - 0.5f : 0.0f;
        float zOffset = 0.0f;
        if (((EntityNPCInterface)((Object)npc)).func_70089_S()) {
            if (((EntityNPCInterface)((Object)npc)).func_70608_bn()) {
                xOffset = (float)(-Math.cos(Math.toRadians(180 - ((EntityNPCInterface)((Object)npc)).ais.orientation)));
                zOffset = (float)(-Math.sin(Math.toRadians(((EntityNPCInterface)((Object)npc)).ais.orientation)));
                yOffset += 0.14f;
            } else if (((EntityNPCInterface)((Object)npc)).currentAnimation == 1 || npc.func_184218_aH()) {
                yOffset -= 0.5f - ((EntityCustomNpc)((Object)npc)).modelData.getLegsY() * 0.8f;
            }
        }
        xOffset = xOffset / 5.0f * (float)((EntityNPCInterface)((Object)npc)).display.getSize();
        yOffset = yOffset / 5.0f * (float)((EntityNPCInterface)((Object)npc)).display.getSize();
        zOffset = zOffset / 5.0f * (float)((EntityNPCInterface)((Object)npc)).display.getSize();
        super.func_77039_a(npc, d + (double)xOffset, d1 + (double)yOffset, d2 + (double)zOffset);
    }

    public ResourceLocation getEntityTexture(T npc) {
        if (((EntityNPCInterface)((Object)npc)).textureLocation == null) {
            if (((EntityNPCInterface)((Object)npc)).display.skinType == 0) {
                ((EntityNPCInterface)((Object)npc)).textureLocation = new ResourceLocation(((EntityNPCInterface)((Object)npc)).display.getSkinTexture());
            } else {
                if (LastTextureTick < 5) {
                    return DefaultPlayerSkin.func_177335_a();
                }
                if (((EntityNPCInterface)((Object)npc)).display.skinType == 1 && ((EntityNPCInterface)((Object)npc)).display.playerProfile != null) {
                    Minecraft minecraft = Minecraft.func_71410_x();
                    Map map = minecraft.func_152342_ad().func_152788_a(((EntityNPCInterface)((Object)npc)).display.playerProfile);
                    if (map.containsKey(MinecraftProfileTexture.Type.SKIN)) {
                        ((EntityNPCInterface)((Object)npc)).textureLocation = minecraft.func_152342_ad().func_152792_a((MinecraftProfileTexture)map.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
                    }
                } else if (((EntityNPCInterface)((Object)npc)).display.skinType == 2) {
                    try {
                        MessageDigest digest = MessageDigest.getInstance("MD5");
                        byte[] hash = digest.digest(((EntityNPCInterface)((Object)npc)).display.getSkinUrl().getBytes("UTF-8"));
                        StringBuilder sb = new StringBuilder(2 * hash.length);
                        for (byte b : hash) {
                            sb.append(String.format("%02x", b & 0xFF));
                        }
                        ((EntityNPCInterface)((Object)npc)).textureLocation = new ResourceLocation("skins/" + sb.toString());
                        this.loadSkin(null, ((EntityNPCInterface)((Object)npc)).textureLocation, ((EntityNPCInterface)((Object)npc)).display.getSkinUrl());
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
            }
        }
        if (((EntityNPCInterface)((Object)npc)).textureLocation == null) {
            return DefaultPlayerSkin.func_177335_a();
        }
        return ((EntityNPCInterface)((Object)npc)).textureLocation;
    }

    private void loadSkin(File file, ResourceLocation resource, String par1Str) {
        TextureManager texturemanager = Minecraft.func_71410_x().func_110434_K();
        if (texturemanager.func_110581_b(resource) != null) {
            return;
        }
        ImageDownloadAlt object = new ImageDownloadAlt(file, par1Str, DefaultPlayerSkin.func_177335_a(), (IImageBuffer)new ImageBufferDownloadAlt());
        texturemanager.func_110579_a(resource, (ITextureObject)object);
    }
}

