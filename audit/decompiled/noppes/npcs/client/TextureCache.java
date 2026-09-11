/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.texture.SimpleTexture
 *  net.minecraft.client.renderer.texture.TextureUtil
 *  net.minecraft.client.resources.IResourceManager
 *  net.minecraft.util.ResourceLocation
 */
package noppes.npcs.client;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.LogWriter;

public class TextureCache
extends SimpleTexture {
    private BufferedImage bufferedImage;
    private boolean textureUploaded;

    public TextureCache(ResourceLocation location) {
        super(location);
    }

    public int func_110552_b() {
        this.checkTextureUploaded();
        return super.func_110552_b();
    }

    private void checkTextureUploaded() {
        if (!this.textureUploaded && this.bufferedImage != null) {
            if (this.field_110568_b != null && this.field_110553_a != -1) {
                TextureUtil.func_147942_a((int)this.field_110553_a);
                this.field_110553_a = -1;
            }
            TextureUtil.func_110987_a((int)super.func_110552_b(), (BufferedImage)this.bufferedImage);
            this.textureUploaded = true;
        }
    }

    public void setImage(ResourceLocation location) {
        try {
            IResourceManager manager = Minecraft.func_71410_x().func_110442_L();
            BufferedImage bufferedimage = ImageIO.read(manager.func_110536_a(location).func_110527_b());
            int i = bufferedimage.getWidth();
            int j = bufferedimage.getHeight();
            this.bufferedImage = new BufferedImage(i * 4, j * 2, 1);
            Graphics g = this.bufferedImage.getGraphics();
            g.drawImage(bufferedimage, 0, 0, null);
            g.drawImage(bufferedimage, i, 0, null);
            g.drawImage(bufferedimage, i * 2, 0, null);
            g.drawImage(bufferedimage, i * 3, 0, null);
            g.drawImage(bufferedimage, 0, i, null);
            g.drawImage(bufferedimage, i, j, null);
            g.drawImage(bufferedimage, i * 2, j, null);
            g.drawImage(bufferedimage, i * 3, j, null);
            this.textureUploaded = false;
        }
        catch (Exception e) {
            LogWriter.error("Failed caching texture: " + location, e);
        }
    }

    public void func_110551_a(IResourceManager resourceManager) throws IOException {
    }
}

