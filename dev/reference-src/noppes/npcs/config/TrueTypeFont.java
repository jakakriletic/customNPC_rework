/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.BufferBuilder
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.Tessellator
 *  net.minecraft.client.renderer.texture.TextureUtil
 *  net.minecraft.client.renderer.vertex.DefaultVertexFormats
 *  net.minecraft.util.ResourceLocation
 */
package noppes.npcs.config;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.util.LRUHashMap;

public class TrueTypeFont {
    private static final int MaxWidth = 512;
    private static final List<Font> allFonts = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts());
    private List<Font> usedFonts = new ArrayList<Font>();
    private LinkedHashMap<String, GlyphCache> textcache = new LRUHashMap<String, GlyphCache>(100);
    private Map<Character, Glyph> glyphcache = new HashMap<Character, Glyph>();
    private List<TextureCache> textures = new ArrayList<TextureCache>();
    private Font font;
    private int lineHeight = 1;
    private Graphics2D globalG = (Graphics2D)new BufferedImage(1, 1, 2).getGraphics();
    public float scale = 1.0f;
    private int specialChar = 167;

    public TrueTypeFont(Font font, float scale) {
        this.font = font;
        this.scale = scale;
        this.globalG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        this.lineHeight = this.globalG.getFontMetrics(font).getHeight();
    }

    public TrueTypeFont(ResourceLocation resource, int fontSize, float scale) throws IOException, FontFormatException {
        InputStream stream = Minecraft.getMinecraft().getResourceManager().getResource(resource).getInputStream();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font font = Font.createFont(0, stream);
        ge.registerFont(font);
        this.font = font.deriveFont(0, fontSize);
        this.scale = scale;
        this.globalG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        this.lineHeight = this.globalG.getFontMetrics(font).getHeight();
    }

    public void setSpecial(char c) {
        this.specialChar = c;
    }

    public void draw(String text, float x, float y, int color) {
        GlyphCache cache = this.getOrCreateCache(text);
        float r = (float)(color >> 16 & 0xFF) / 255.0f;
        float g = (float)(color >> 8 & 0xFF) / 255.0f;
        float b = (float)(color & 0xFF) / 255.0f;
        GlStateManager.color((float)r, (float)g, (float)b, (float)1.0f);
        GlStateManager.enableBlend();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float)x, (float)y, (float)0.0f);
        GlStateManager.scale((float)this.scale, (float)this.scale, (float)1.0f);
        float i = 0.0f;
        for (Glyph gl : cache.glyphs) {
            if (gl.type != GlyphType.NORMAL) {
                if (gl.type == GlyphType.RESET) {
                    GlStateManager.color((float)r, (float)g, (float)b, (float)1.0f);
                    continue;
                }
                if (gl.type != GlyphType.COLOR) continue;
                GlStateManager.color((float)((float)(gl.color >> 16 & 0xFF) / 255.0f), (float)((float)(gl.color >> 8 & 0xFF) / 255.0f), (float)((float)(gl.color & 0xFF) / 255.0f), (float)1.0f);
                continue;
            }
            GlStateManager.bindTexture((int)gl.texture);
            this.drawTexturedModalRect(i, 0.0f, (float)gl.x * this.textureScale(), (float)gl.y * this.textureScale(), (float)gl.width * this.textureScale(), (float)gl.height * this.textureScale());
            i += (float)gl.width * this.textureScale();
        }
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }

    private GlyphCache getOrCreateCache(String text) {
        GlyphCache cache = this.textcache.get(text);
        if (cache != null) {
            return cache;
        }
        cache = new GlyphCache();
        for (int i = 0; i < text.length(); ++i) {
            char next;
            int index;
            char c = text.charAt(i);
            if (c == this.specialChar && i + 1 < text.length() && (index = "0123456789abcdefklmnor".indexOf(next = text.toLowerCase(Locale.ENGLISH).charAt(i + 1))) >= 0) {
                Glyph g = new Glyph();
                if (index < 16) {
                    g.type = GlyphType.COLOR;
                    g.color = Minecraft.getMinecraft().fontRenderer.getColorCode(next);
                } else {
                    g.type = index == 16 ? GlyphType.RANDOM : (index == 17 ? GlyphType.BOLD : (index == 18 ? GlyphType.STRIKETHROUGH : (index == 19 ? GlyphType.UNDERLINE : (index == 20 ? GlyphType.ITALIC : GlyphType.RESET))));
                }
                cache.glyphs.add(g);
                ++i;
                continue;
            }
            Glyph g = this.getOrCreateGlyph(c);
            cache.glyphs.add(g);
            cache.width += g.width;
            cache.height = Math.max(cache.height, g.height);
        }
        this.textcache.put(text, cache);
        return cache;
    }

    private Glyph getOrCreateGlyph(char c) {
        Glyph g = this.glyphcache.get(Character.valueOf(c));
        if (g != null) {
            return g;
        }
        TextureCache cache = this.getCurrentTexture();
        Font font = this.getFontForChar(c);
        FontMetrics metrics = this.globalG.getFontMetrics(font);
        g = new Glyph();
        g.width = Math.max(metrics.charWidth(c), 1);
        g.height = Math.max(metrics.getHeight(), 1);
        if (cache.x + g.width >= 512) {
            cache.x = 0;
            cache.y += this.lineHeight + 1;
            if (cache.y >= 512) {
                cache.full = true;
                cache = this.getCurrentTexture();
            }
        }
        g.x = cache.x;
        g.y = cache.y;
        cache.x += g.width + 3;
        this.lineHeight = Math.max(this.lineHeight, g.height);
        cache.g.setFont(font);
        cache.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        cache.g.drawString(c + "", g.x, g.y + metrics.getAscent());
        g.texture = cache.textureId;
        TextureUtil.uploadTextureImage((int)cache.textureId, (BufferedImage)cache.bufferedImage);
        this.glyphcache.put(Character.valueOf(c), g);
        return g;
    }

    private TextureCache getCurrentTexture() {
        TextureCache cache = null;
        for (TextureCache t : this.textures) {
            if (t.full) continue;
            cache = t;
            break;
        }
        if (cache == null) {
            cache = new TextureCache();
            this.textures.add(cache);
        }
        return cache;
    }

    public void drawCentered(String text, float x, float y, int color) {
        this.draw(text, x - (float)this.width(text) / 2.0f, y, color);
    }

    private Font getFontForChar(char c) {
        if (this.font.canDisplay(c)) {
            return this.font;
        }
        for (Font f : this.usedFonts) {
            if (!f.canDisplay(c)) continue;
            return f;
        }
        Font fa = new Font("Arial Unicode MS", 0, this.font.getSize());
        if (fa.canDisplay(c)) {
            return fa;
        }
        for (Font f : allFonts) {
            if (!f.canDisplay(c)) continue;
            f = f.deriveFont(0, this.font.getSize());
            this.usedFonts.add(f);
            return f;
        }
        return this.font;
    }

    public void drawTexturedModalRect(float x, float y, float textureX, float textureY, float width, float height) {
        float f = 0.00390625f;
        float f1 = 0.00390625f;
        boolean zLevel = false;
        BufferBuilder tessellator = Tessellator.getInstance().getBuffer();
        tessellator.begin(7, DefaultVertexFormats.POSITION_TEX);
        tessellator.noColor();
        tessellator.pos((double)x, (double)(y + height), (double)zLevel).tex((double)(textureX * f), (double)((textureY + height) * f1)).endVertex();
        tessellator.pos((double)(x + width), (double)(y + height), (double)zLevel).tex((double)((textureX + width) * f), (double)((textureY + height) * f1)).endVertex();
        tessellator.pos((double)(x + width), (double)y, (double)zLevel).tex((double)((textureX + width) * f), (double)(textureY * f1)).endVertex();
        tessellator.pos((double)x, (double)y, (double)zLevel).tex((double)(textureX * f), (double)(textureY * f1)).endVertex();
        Tessellator.getInstance().draw();
    }

    public int width(String text) {
        GlyphCache cache = this.getOrCreateCache(text);
        return (int)((float)cache.width * this.scale * this.textureScale());
    }

    public int height(String text) {
        if (text == null || text.trim().isEmpty()) {
            return (int)((float)this.lineHeight * this.scale * this.textureScale());
        }
        GlyphCache cache = this.getOrCreateCache(text);
        return Math.max(1, (int)((float)cache.height * this.scale * this.textureScale()));
    }

    private float textureScale() {
        return 0.5f;
    }

    public void dispose() {
        for (TextureCache cache : this.textures) {
            GlStateManager.deleteTexture((int)cache.textureId);
        }
        this.textcache.clear();
    }

    public String getFontName() {
        return this.font.getFontName();
    }

    class GlyphCache {
        public int width;
        public int height;
        List<Glyph> glyphs = new ArrayList<Glyph>();

        GlyphCache() {
        }
    }

    class Glyph {
        GlyphType type = GlyphType.NORMAL;
        int color = -1;
        int x;
        int y;
        int height;
        int width;
        int texture;

        Glyph() {
        }
    }

    class TextureCache {
        int x;
        int y;
        int textureId = GlStateManager.generateTexture();
        BufferedImage bufferedImage = new BufferedImage(512, 512, 2);
        Graphics2D g = (Graphics2D)this.bufferedImage.getGraphics();
        boolean full;

        TextureCache() {
        }
    }

    static enum GlyphType {
        NORMAL,
        COLOR,
        RANDOM,
        BOLD,
        STRIKETHROUGH,
        UNDERLINE,
        ITALIC,
        RESET,
        OTHER;

    }
}

