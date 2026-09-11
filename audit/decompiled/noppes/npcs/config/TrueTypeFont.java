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
        InputStream stream = Minecraft.func_71410_x().func_110442_L().func_110536_a(resource).func_110527_b();
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
        GlStateManager.func_179131_c((float)r, (float)g, (float)b, (float)1.0f);
        GlStateManager.func_179147_l();
        GlStateManager.func_179094_E();
        GlStateManager.func_179109_b((float)x, (float)y, (float)0.0f);
        GlStateManager.func_179152_a((float)this.scale, (float)this.scale, (float)1.0f);
        float i = 0.0f;
        for (Glyph gl : cache.glyphs) {
            if (gl.type != GlyphType.NORMAL) {
                if (gl.type == GlyphType.RESET) {
                    GlStateManager.func_179131_c((float)r, (float)g, (float)b, (float)1.0f);
                    continue;
                }
                if (gl.type != GlyphType.COLOR) continue;
                GlStateManager.func_179131_c((float)((float)(gl.color >> 16 & 0xFF) / 255.0f), (float)((float)(gl.color >> 8 & 0xFF) / 255.0f), (float)((float)(gl.color & 0xFF) / 255.0f), (float)1.0f);
                continue;
            }
            GlStateManager.func_179144_i((int)gl.texture);
            this.drawTexturedModalRect(i, 0.0f, (float)gl.x * this.textureScale(), (float)gl.y * this.textureScale(), (float)gl.width * this.textureScale(), (float)gl.height * this.textureScale());
            i += (float)gl.width * this.textureScale();
        }
        GlStateManager.func_179084_k();
        GlStateManager.func_179121_F();
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
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
                    g.color = Minecraft.func_71410_x().field_71466_p.func_175064_b(next);
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
        TextureUtil.func_110987_a((int)cache.textureId, (BufferedImage)cache.bufferedImage);
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
        BufferBuilder tessellator = Tessellator.func_178181_a().func_178180_c();
        tessellator.func_181668_a(7, DefaultVertexFormats.field_181707_g);
        tessellator.func_78914_f();
        tessellator.func_181662_b((double)x, (double)(y + height), (double)zLevel).func_187315_a((double)(textureX * f), (double)((textureY + height) * f1)).func_181675_d();
        tessellator.func_181662_b((double)(x + width), (double)(y + height), (double)zLevel).func_187315_a((double)((textureX + width) * f), (double)((textureY + height) * f1)).func_181675_d();
        tessellator.func_181662_b((double)(x + width), (double)y, (double)zLevel).func_187315_a((double)((textureX + width) * f), (double)(textureY * f1)).func_181675_d();
        tessellator.func_181662_b((double)x, (double)y, (double)zLevel).func_187315_a((double)(textureX * f), (double)(textureY * f1)).func_181675_d();
        Tessellator.func_178181_a().func_78381_a();
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
            GlStateManager.func_179150_h((int)cache.textureId);
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
        int textureId = GlStateManager.func_179146_y();
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

