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
        super(Minecraft.getMinecraft().getRenderManager(), model, f);
    }

    public void renderName(T npc, double d, double d1, double d2) {
        if (npc == null || !this.canRenderName((EntityLiving)npc) || this.renderManager.renderViewEntity == null) {
            return;
        }
        double d0 = npc.getDistanceSq(this.renderManager.renderViewEntity);
        if (d0 > 512.0) {
            return;
        }
        if (((EntityNPCInterface)((Object)npc)).messages != null) {
            float height = ((EntityNPCInterface)((Object)npc)).baseHeight / 5.0f * (float)((EntityNPCInterface)((Object)npc)).display.getSize();
            float offset = ((EntityNPCInterface)((Object)npc)).height * (1.2f + (!((EntityNPCInterface)((Object)npc)).display.showName() ? 0.0f : (((EntityNPCInterface)((Object)npc)).display.getTitle().isEmpty() ? 0.15f : 0.25f)));
            ((EntityNPCInterface)((Object)npc)).messages.renderMessages(d, d1 + (double)offset, d2, 0.666667f * height, ((EntityNPCInterface)((Object)npc)).isInRange(this.renderManager.renderViewEntity, 4.0));
        }
        float scale = ((EntityNPCInterface)((Object)npc)).baseHeight / 5.0f * (float)((EntityNPCInterface)((Object)npc)).display.getSize();
        if (((EntityNPCInterface)((Object)npc)).display.showName()) {
            this.renderLivingLabel((EntityNPCInterface)((Object)npc), (float)d, (float)d1 + ((EntityNPCInterface)((Object)npc)).height - 0.06f * scale, (float)d2, 64, ((EntityNPCInterface)((Object)npc)).getName(), ((EntityNPCInterface)((Object)npc)).display.getTitle());
        }
    }

    public void doRenderShadowAndFire(Entity par1Entity, double par2, double par4, double par6, float par8, float par9) {
        EntityNPCInterface npc = (EntityNPCInterface)par1Entity;
        this.shadowSize = npc.width;
        if (!npc.isKilled()) {
            super.doRenderShadowAndFire(par1Entity, par2, par4, par6, par8, par9);
        }
    }

    protected void renderLivingLabel(EntityNPCInterface npc, float d, float d1, float d2, int i, String name, String title) {
        FontRenderer fontrenderer = this.getFontRendererFromRenderManager();
        float f1 = npc.baseHeight / 5.0f * (float)npc.display.getSize();
        float f2 = 0.01666667f * f1;
        GlStateManager.pushMatrix();
        GlStateManager.translate((float)d, (float)d1, (float)d2);
        GL11.glNormal3f((float)0.0f, (float)1.0f, (float)0.0f);
        GlStateManager.rotate((float)(-this.renderManager.playerViewY), (float)0.0f, (float)1.0f, (float)0.0f);
        GlStateManager.rotate((float)this.renderManager.playerViewX, (float)1.0f, (float)0.0f, (float)0.0f);
        float height = f1 / 6.5f * 2.0f;
        int color = npc.getFaction().color;
        GlStateManager.disableLighting();
        GlStateManager.depthMask((boolean)false);
        GlStateManager.translate((float)0.0f, (float)height, (float)0.0f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate((GlStateManager.SourceFactor)GlStateManager.SourceFactor.SRC_ALPHA, (GlStateManager.DestFactor)GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, (GlStateManager.SourceFactor)GlStateManager.SourceFactor.ONE, (GlStateManager.DestFactor)GlStateManager.DestFactor.ZERO);
        if (!title.isEmpty()) {
            title = "<" + title + ">";
            float f3 = 0.01666667f * f1 * 0.6f;
            GlStateManager.translate((float)0.0f, (float)(-f1 / 6.5f * 0.4f), (float)0.0f);
            GlStateManager.scale((float)(-f3), (float)(-f3), (float)f3);
            fontrenderer.drawString(title, -fontrenderer.getStringWidth(title) / 2, 0, color);
            GlStateManager.scale((float)(1.0f / -f3), (float)(1.0f / -f3), (float)(1.0f / f3));
            GlStateManager.translate((float)0.0f, (float)(f1 / 6.5f * 0.85f), (float)0.0f);
        }
        GlStateManager.scale((float)(-f2), (float)(-f2), (float)f2);
        if (npc.isInRange(this.renderManager.renderViewEntity, 4.0)) {
            GlStateManager.disableDepth();
            fontrenderer.drawString(name, -fontrenderer.getStringWidth(name) / 2, 0, color + 0x55000000);
            GlStateManager.enableDepth();
        }
        GlStateManager.depthMask((boolean)true);
        GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        fontrenderer.drawString(name, -fontrenderer.getStringWidth(name) / 2, 0, color);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GlStateManager.popMatrix();
    }

    protected void renderColor(EntityNPCInterface npc) {
        if (npc.hurtTime <= 0 && npc.deathTime <= 0) {
            float red = (float)(npc.display.getTint() >> 16 & 0xFF) / 255.0f;
            float green = (float)(npc.display.getTint() >> 8 & 0xFF) / 255.0f;
            float blue = (float)(npc.display.getTint() & 0xFF) / 255.0f;
            GlStateManager.color((float)red, (float)green, (float)blue, (float)1.0f);
        }
    }

    private void renderLiving(T npc, double d, double d1, double d2, float xoffset, float yoffset, float zoffset) {
    }

    protected void applyRotations(T npc, float f, float f1, float f2) {
        if (((EntityNPCInterface)((Object)npc)).isEntityAlive() && ((EntityNPCInterface)((Object)npc)).isPlayerSleeping()) {
            GlStateManager.rotate((float)((EntityNPCInterface)((Object)npc)).ais.orientation, (float)0.0f, (float)1.0f, (float)0.0f);
            GlStateManager.rotate((float)this.getDeathMaxRotation((EntityLivingBase)npc), (float)0.0f, (float)0.0f, (float)1.0f);
            GlStateManager.rotate((float)270.0f, (float)0.0f, (float)1.0f, (float)0.0f);
        } else if (((EntityNPCInterface)((Object)npc)).isEntityAlive() && ((EntityNPCInterface)((Object)npc)).currentAnimation == 7) {
            GlStateManager.rotate((float)(270.0f - f1), (float)0.0f, (float)1.0f, (float)0.0f);
            float scale = (float)((EntityCustomNpc)((Object)npc)).display.getSize() / 5.0f;
            GlStateManager.translate((float)(-scale + ((EntityCustomNpc)((Object)npc)).modelData.getLegsY() * scale), (float)0.14f, (float)0.0f);
            GlStateManager.rotate((float)270.0f, (float)0.0f, (float)0.0f, (float)1.0f);
            GlStateManager.rotate((float)270.0f, (float)0.0f, (float)1.0f, (float)0.0f);
        } else {
            super.applyRotations(npc, f, f1, f2);
        }
    }

    protected void preRenderCallback(T npc, float f) {
        this.renderColor((EntityNPCInterface)((Object)npc));
        int size = ((EntityNPCInterface)((Object)npc)).display.getSize();
        GlStateManager.scale((float)(((EntityNPCInterface)((Object)npc)).scaleX / 5.0f * (float)size), (float)(((EntityNPCInterface)((Object)npc)).scaleY / 5.0f * (float)size), (float)(((EntityNPCInterface)((Object)npc)).scaleZ / 5.0f * (float)size));
    }

    public void doRender(T npc, double d, double d1, double d2, float f, float f1) {
        if (((EntityNPCInterface)((Object)npc)).isKilled() && ((EntityNPCInterface)((Object)npc)).stats.hideKilledBody && ((EntityNPCInterface)((Object)npc)).deathTime > 20) {
            return;
        }
        if (((EntityNPCInterface)((Object)npc)).display.getBossbar() != 1 && (((EntityNPCInterface)((Object)npc)).display.getBossbar() != 2 || !((EntityNPCInterface)((Object)npc)).isAttacking()) || ((EntityNPCInterface)((Object)npc)).isKilled() || ((EntityNPCInterface)((Object)npc)).deathTime > 20 || ((EntityNPCInterface)((Object)npc)).canSee((Entity)Minecraft.getMinecraft().player)) {
            // empty if block
        }
        if (((EntityNPCInterface)((Object)npc)).ais.getStandingType() == 3 && !((EntityNPCInterface)((Object)npc)).isWalking() && !((EntityNPCInterface)((Object)npc)).isInteracting()) {
            ((EntityNPCInterface)((Object)npc)).prevRenderYawOffset = ((EntityNPCInterface)((Object)npc)).renderYawOffset = (float)((EntityNPCInterface)((Object)npc)).ais.orientation;
        }
        super.doRender(npc, d, d1, d2, f, f1);
    }

    protected void renderModel(T npc, float par2, float par3, float par4, float par5, float par6, float par7) {
        super.renderModel(npc, par2, par3, par4, par5, par6, par7);
        if (!((EntityNPCInterface)((Object)npc)).display.getOverlayTexture().isEmpty()) {
            GlStateManager.depthFunc((int)515);
            if (((EntityNPCInterface)((Object)npc)).textureGlowLocation == null) {
                ((EntityNPCInterface)((Object)npc)).textureGlowLocation = new ResourceLocation(((EntityNPCInterface)((Object)npc)).display.getOverlayTexture());
            }
            this.bindTexture(((EntityNPCInterface)((Object)npc)).textureGlowLocation);
            float f1 = 1.0f;
            GlStateManager.enableBlend();
            GlStateManager.blendFunc((int)1, (int)1);
            GlStateManager.disableLighting();
            if (((EntityNPCInterface)((Object)npc)).isInvisible()) {
                GlStateManager.depthMask((boolean)false);
            } else {
                GlStateManager.depthMask((boolean)true);
            }
            GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            GlStateManager.pushMatrix();
            GlStateManager.scale((float)1.001f, (float)1.001f, (float)1.001f);
            this.mainModel.render(npc, par2, par3, par4, par5, par6, par7);
            GlStateManager.popMatrix();
            GlStateManager.enableLighting();
            GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f, (float)f1);
            GlStateManager.depthFunc((int)515);
            GlStateManager.disableBlend();
        }
    }

    protected float handleRotationFloat(T npc, float par2) {
        if (((EntityNPCInterface)((Object)npc)).isKilled() || !((EntityNPCInterface)((Object)npc)).display.getHasLivingAnimation()) {
            return 0.0f;
        }
        return super.handleRotationFloat(npc, par2);
    }

    protected void renderLivingAt(T npc, double d, double d1, double d2) {
        this.shadowSize = (float)((EntityNPCInterface)((Object)npc)).display.getSize() / 10.0f;
        float xOffset = 0.0f;
        float yOffset = ((EntityNPCInterface)((Object)npc)).currentAnimation == 0 ? ((EntityNPCInterface)((Object)npc)).ais.bodyOffsetY / 10.0f - 0.5f : 0.0f;
        float zOffset = 0.0f;
        if (((EntityNPCInterface)((Object)npc)).isEntityAlive()) {
            if (((EntityNPCInterface)((Object)npc)).isPlayerSleeping()) {
                xOffset = (float)(-Math.cos(Math.toRadians(180 - ((EntityNPCInterface)((Object)npc)).ais.orientation)));
                zOffset = (float)(-Math.sin(Math.toRadians(((EntityNPCInterface)((Object)npc)).ais.orientation)));
                yOffset += 0.14f;
            } else if (((EntityNPCInterface)((Object)npc)).currentAnimation == 1 || npc.isRiding()) {
                yOffset -= 0.5f - ((EntityCustomNpc)((Object)npc)).modelData.getLegsY() * 0.8f;
            }
        }
        xOffset = xOffset / 5.0f * (float)((EntityNPCInterface)((Object)npc)).display.getSize();
        yOffset = yOffset / 5.0f * (float)((EntityNPCInterface)((Object)npc)).display.getSize();
        zOffset = zOffset / 5.0f * (float)((EntityNPCInterface)((Object)npc)).display.getSize();
        super.renderLivingAt(npc, d + (double)xOffset, d1 + (double)yOffset, d2 + (double)zOffset);
    }

    public ResourceLocation getEntityTexture(T npc) {
        if (((EntityNPCInterface)((Object)npc)).textureLocation == null) {
            if (((EntityNPCInterface)((Object)npc)).display.skinType == 0) {
                ((EntityNPCInterface)((Object)npc)).textureLocation = new ResourceLocation(((EntityNPCInterface)((Object)npc)).display.getSkinTexture());
            } else {
                if (LastTextureTick < 5) {
                    return DefaultPlayerSkin.getDefaultSkinLegacy();
                }
                if (((EntityNPCInterface)((Object)npc)).display.skinType == 1 && ((EntityNPCInterface)((Object)npc)).display.playerProfile != null) {
                    Minecraft minecraft = Minecraft.getMinecraft();
                    Map map = minecraft.getSkinManager().loadSkinFromCache(((EntityNPCInterface)((Object)npc)).display.playerProfile);
                    if (map.containsKey(MinecraftProfileTexture.Type.SKIN)) {
                        ((EntityNPCInterface)((Object)npc)).textureLocation = minecraft.getSkinManager().loadSkin((MinecraftProfileTexture)map.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
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
            return DefaultPlayerSkin.getDefaultSkinLegacy();
        }
        return ((EntityNPCInterface)((Object)npc)).textureLocation;
    }

    private void loadSkin(File file, ResourceLocation resource, String par1Str) {
        TextureManager texturemanager = Minecraft.getMinecraft().getTextureManager();
        if (texturemanager.getTexture(resource) != null) {
            return;
        }
        ImageDownloadAlt object = new ImageDownloadAlt(file, par1Str, DefaultPlayerSkin.getDefaultSkinLegacy(), (IImageBuffer)new ImageBufferDownloadAlt());
        texturemanager.loadTexture(resource, (ITextureObject)object);
    }
}

