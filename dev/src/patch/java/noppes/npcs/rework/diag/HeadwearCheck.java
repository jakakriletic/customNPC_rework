package noppes.npcs.rework.diag;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.render.HeadwearList;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * M5-K K14: dokaz, da en display list pokrivala da isto sliko kot original.
 *
 * <p>V eni slicici ({@code RenderWorldLastEvent}, iste matrike kamere, isti {@code partialTicks},
 * isto stanje entitet) narise vse nalozene NPC-je v lasten framebuffer velikosti zaslona
 * veckrat: ogrevanje (nacina 1 in 2, prevedeta liste), nato original {@code A} (nacin 0),
 * {@code B} (nacin 1), {@code C} (nacin 2), se enkrat original {@code A2} in original brez
 * pokrivala {@code N} ({@code HeadWearType=0}). Po vsakem prehodu prebere barvo (RGBA8) in
 * globino (float) vseh pikslov.
 *
 * <p>Izid: {@code A == A2} (prehod je ponovljiv), {@code A == B} in {@code A == C} bitno v barvi in
 * globini, {@code A != N} (prehod pokrivalo res vidi), v {@code B} in {@code C} je bil vsak izris
 * pokrivala narisan z listom. Vse le za harness ({@code -Drwharness}).
 */
@SideOnly(Side.CLIENT)
public final class HeadwearCheck {
    /** Izid ene serije prehodov z isto teksturo (0 = skin NPC-ja, 1 = neprozorna testna tekstura). */
    public static final class Series {
        /** Pikslov, kjer je v A kaj narisano (globina &lt; 1). */
        public long covered = -1;
        public long diffRepeat = -1;
        public long diffMode1 = -1;
        public long diffDepthMode1 = -1;
        public long diffMode2 = -1;
        public long diffDepthMode2 = -1;
        public long listDrawsMode1 = -1;
        public long listDrawsMode2 = -1;

        boolean equal(int npcs) {
            return this.covered > 0 && this.diffRepeat == 0 && this.diffMode1 == 0 && this.diffDepthMode1 == 0
                    && this.diffMode2 == 0 && this.diffDepthMode2 == 0
                    && this.listDrawsMode1 >= npcs && this.listDrawsMode2 >= npcs;
        }
    }

    public int width;
    public int height;
    public int npcs;
    public int textures;
    public final Series skin = new Series();
    public final Series pattern = new Series();
    /** Pikslov, ki jih pokrivalo spremeni (testna tekstura, A proti N). */
    public long sensitivity = -1;
    public String error = "";

    public boolean ok() {
        return this.error.isEmpty() && this.skin.equal(this.npcs) && this.pattern.equal(this.npcs) && this.sensitivity > 0;
    }

    public static HeadwearCheck run(Minecraft mc, float partialTicks, File png) {
        HeadwearCheck r = new HeadwearCheck();
        int savedMode = CustomNpcs.RwHeadwearList;
        int savedType = CustomNpcs.HeadWearType;
        Framebuffer fb = null;
        TextureManager tm = mc.getTextureManager();
        Map<ResourceLocation, ITextureObject> swapped = new LinkedHashMap<ResourceLocation, ITextureObject>();
        DynamicTexture dyn = null;
        try {
            if (!OpenGlHelper.isFramebufferEnabled()) {
                r.error = "framebuffer ni na voljo";
                return r;
            }
            List<Entity> list = new ArrayList<Entity>();
            for (Entity e : mc.world.loadedEntityList) {
                if (e instanceof EntityNPCInterface && !e.isDead) {
                    list.add(e);
                }
            }
            r.npcs = list.size();
            r.width = mc.displayWidth;
            r.height = mc.displayHeight;
            fb = new Framebuffer(r.width, r.height, true);
            fb.setFramebufferColor(0.0f, 0.0f, 0.0f, 0.0f);
            int n = r.width * r.height;
            ByteBuffer ca = BufferUtils.createByteBuffer(4 * n);
            FloatBuffer da = BufferUtils.createFloatBuffer(n);
            ByteBuffer cx = BufferUtils.createByteBuffer(4 * n);
            FloatBuffer dx = BufferUtils.createFloatBuffer(n);
            CustomNpcs.HeadWearType = 1;
            // Ogrevanje: lista nacinov 1 in 2 se prevedeta zunaj primerjanih prehodov.
            pass(mc, fb, list, partialTicks, 1, null, null);
            pass(mc, fb, list, partialTicks, 2, null, null);
            series(mc, fb, list, partialTicks, n, ca, da, cx, dx, r.skin);

            // Obmocje klobuka je v skinu fixture prozorno (alfa test zavrze vse fragmente pokrivala),
            // zato se ponovi z neprozorno teksturo, kjer ima vsak teksel svojo barvo.
            dyn = new DynamicTexture(64, 64);
            int[] t = dyn.getTextureData();
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    t[y * 64 + x] = 0xFF000000 | (x * 4) << 16 | (y * 4) << 8 | (((x + y) & 1) == 0 ? 0x33 : 0xCC);
                }
            }
            dyn.updateDynamicTexture();
            for (Entity e : list) {
                ResourceLocation loc = ((EntityNPCInterface)e).textureLocation;
                if (loc != null && !swapped.containsKey(loc)) {
                    swapped.put(loc, tm.getTexture(loc));
                    tm.loadTexture(loc, dyn);
                }
            }
            r.textures = swapped.size();
            series(mc, fb, list, partialTicks, n, ca, da, cx, dx, r.pattern);
            if (png != null) {
                writePng(ca, r.width, r.height, png);
            }
            CustomNpcs.HeadWearType = 0;
            pass(mc, fb, list, partialTicks, 0, cx, dx);
            r.sensitivity = diffColor(ca, cx, n);
        } catch (Exception e) {
            r.error = String.valueOf(e);
        } finally {
            CustomNpcs.RwHeadwearList = savedMode;
            CustomNpcs.HeadWearType = savedType;
            for (Map.Entry<ResourceLocation, ITextureObject> en : swapped.entrySet()) {
                if (en.getValue() != null) {
                    // SimpleTexture se ob tem znova nalozi iz vira (kot ob F3+T).
                    tm.loadTexture(en.getKey(), en.getValue());
                } else {
                    tm.deleteTexture(en.getKey());
                }
            }
            if (dyn != null) {
                dyn.deleteGlTexture();
            }
            mc.getFramebuffer().bindFramebuffer(true);
            if (fb != null) {
                fb.deleteFramebuffer();
            }
        }
        return r;
    }

    /** A (0), B (1), C (2), A2 (0); A ostane v ca/da. */
    private static void series(Minecraft mc, Framebuffer fb, List<Entity> list, float pt, int n,
            ByteBuffer ca, FloatBuffer da, ByteBuffer cx, FloatBuffer dx, Series s) {
        pass(mc, fb, list, pt, 0, ca, da);
        s.covered = 0;
        for (int i = 0; i < n; i++) {
            if (da.get(i) < 1.0f) {
                s.covered++;
            }
        }
        long d0 = HeadwearList.drawsList;
        pass(mc, fb, list, pt, 1, cx, dx);
        s.listDrawsMode1 = HeadwearList.drawsList - d0;
        s.diffMode1 = diffColor(ca, cx, n);
        s.diffDepthMode1 = diffDepth(da, dx, n);
        d0 = HeadwearList.drawsList;
        pass(mc, fb, list, pt, 2, cx, dx);
        s.listDrawsMode2 = HeadwearList.drawsList - d0;
        s.diffMode2 = diffColor(ca, cx, n);
        s.diffDepthMode2 = diffDepth(da, dx, n);
        pass(mc, fb, list, pt, 0, cx, dx);
        s.diffRepeat = diffColor(ca, cx, n) + diffDepth(da, dx, n);
    }

    private static void pass(Minecraft mc, Framebuffer fb, List<Entity> list, float pt, int mode, ByteBuffer color, FloatBuffer depth) {
        CustomNpcs.RwHeadwearList = mode;
        fb.framebufferClear();
        fb.bindFramebuffer(true);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        RenderHelper.enableStandardItemLighting();
        mc.entityRenderer.enableLightmap();
        for (Entity e : list) {
            mc.getRenderManager().renderEntityStatic(e, pt, false);
        }
        mc.entityRenderer.disableLightmap();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        if (color != null) {
            color.clear();
            depth.clear();
            GL11.glReadPixels(0, 0, fb.framebufferWidth, fb.framebufferHeight, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, color);
            GL11.glReadPixels(0, 0, fb.framebufferWidth, fb.framebufferHeight, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depth);
        }
    }

    static long diffColor(ByteBuffer a, ByteBuffer b, int n) {
        long d = 0;
        for (int i = 0; i < n; i++) {
            if (a.getInt(4 * i) != b.getInt(4 * i)) {
                d++;
            }
        }
        return d;
    }

    static long diffDepth(FloatBuffer a, FloatBuffer b, int n) {
        long d = 0;
        for (int i = 0; i < n; i++) {
            if (Float.floatToRawIntBits(a.get(i)) != Float.floatToRawIntBits(b.get(i))) {
                d++;
            }
        }
        return d;
    }

    private static void writePng(ByteBuffer c, int w, int h, File f) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = 4 * (y * w + x);
                int rgba = ((c.get(i + 3) & 0xFF) << 24) | ((c.get(i) & 0xFF) << 16) | ((c.get(i + 1) & 0xFF) << 8) | (c.get(i + 2) & 0xFF);
                img.setRGB(x, h - 1 - y, rgba);
            }
        }
        if (f.getParentFile() != null) {
            f.getParentFile().mkdirs();
        }
        ImageIO.write(img, "png", f);
    }
}
