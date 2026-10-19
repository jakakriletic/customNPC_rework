package noppes.npcs.rework.diag;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.render.HeadwearSkip;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * M5-K K14b: preverba, da pokrivalo brez prozornih stirikotnikov da isto sliko kot original
 * (empiricna za fragmente na robni crti, glej {@code HeadwearSkip}).
 *
 * <p>V eni slicici ({@code RenderWorldLastEvent}, iste matrike kamere, isti {@code partialTicks},
 * isto stanje entitet) narise vse nalozene NPC-je v lasten framebuffer: original {@code A}
 * ({@code RwHeadwearSkip=0}), K14b {@code B} (1), se enkrat original {@code A2}; po vsakem prehodu
 * prebere barvo (RGBA8) in globino (float) vseh pikslov. Serije tekstur: skin NPC-jev (prozorno
 * pokrivalo), nakljucen vzorec z delno prozornostjo (vsi trije primeri izpusta) in pet pravih
 * skinov CNPC z vidnim pokrivalom. Izid: {@code A == A2}, {@code A == B} bitno, v {@code B} vsak
 * izris pokrivala brez vrnitve na original, vzorec spremeni sliko ({@code HeadWearType=0} proti 1).
 * Vse le za harness ({@code -Drwharness}).
 */
@SideOnly(Side.CLIENT)
public final class HeadwearCheck {
    private static final String[] PRAVI_SKINI = {
        "textures/entity/elffemale/elffemale.png",
        "textures/entity/elffemale/elfmage.png",
        "textures/entity/elffemale/elfqueenformal.png",
        "textures/entity/elffemale/elfsoldierhelmet.png",
        "textures/entity/elfmale/blood_elf_mage.png",
    };

    /** Izid ene serije prehodov z isto teksturo. */
    public static final class Series {
        public final String name;
        public long covered = -1;
        public long diffColor = -1;
        public long diffDepth = -1;
        public long diffRepeat = -1;
        public long draws = -1;
        public long fallbacks = -1;
        public long partsNone = -1;
        public long partsSome = -1;
        public long partsAll = -1;

        Series(String name) {
            this.name = name;
        }

        boolean equal(int npcs) {
            return this.covered > 0 && this.diffColor == 0 && this.diffDepth == 0 && this.diffRepeat == 0
                    && this.draws >= npcs && this.fallbacks == 0;
        }

        @Override
        public String toString() {
            return this.name + ": pikslov=" + this.covered + " razlik=" + this.diffColor + "/" + this.diffDepth
                    + " ponovitev=" + this.diffRepeat + " izrisov=" + this.draws + " original=" + this.fallbacks
                    + " delov nic/nekaj/vse=" + this.partsNone + "/" + this.partsSome + "/" + this.partsAll;
        }
    }

    public int width;
    public int height;
    public int npcs;
    public final List<Series> series = new ArrayList<Series>();
    /** Pikslov, ki jih pokrivalo z vzorcem spremeni (HeadWearType 1 proti 0). */
    public long sensitivity = -1;
    public String error = "";

    public boolean ok() {
        if (!this.error.isEmpty() || this.sensitivity <= 0 || this.series.size() != 2 + PRAVI_SKINI.length) {
            return false;
        }
        long some = 0;
        long none = 0;
        for (Series s : this.series) {
            if (!s.equal(this.npcs)) {
                return false;
            }
            some += s.partsSome;
            none += s.partsNone;
        }
        // Preverba mora pokriti oba izpusta: cel del in posamezne stirikotnike.
        return some > 0 && none > 0;
    }

    public static HeadwearCheck run(Minecraft mc, float partialTicks) {
        HeadwearCheck r = new HeadwearCheck();
        int savedSkip = CustomNpcs.RwHeadwearSkip;
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
            // Ogrevanje: odlocitve in delni listi K14b nastanejo zunaj primerjanih prehodov.
            pass(mc, fb, list, partialTicks, 1, null, null);
            r.series.add(series(mc, fb, list, partialTicks, n, ca, da, cx, dx, "skin"));

            // Vzorec: vsak teksel svoja barva, v obmocju pokrivala (x 32..63, y 0..15) ~45 % alfa 0,
            // ~5 % alfa 1..25 (alfa test jih zavrze, maska jih obdrzi), ostalo neprozorno.
            dyn = new DynamicTexture(64, 64);
            int[] t = dyn.getTextureData();
            Random rnd = new Random(1419);
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    int a = 0xFF;
                    if (x >= 32 && y < 16) {
                        int p = rnd.nextInt(100);
                        a = p < 45 ? 0 : (p < 50 ? 1 + rnd.nextInt(25) : 0xFF);
                    }
                    t[y * 64 + x] = a << 24 | (x * 4) << 16 | (y * 4) << 8 | (((x + y) & 1) == 0 ? 0x33 : 0xCC);
                }
            }
            // Ena 8 x 8 ploskev (desno, 32..39 x 8..15) z robom enega teksla povsem prozorna: del brez
            // stirikotnikov (maska vzame rob, ker ga rob obraza lahko vzorci).
            for (int y = 7; y < 17; y++) {
                for (int x = 31; x < 41; x++) {
                    t[y * 64 + x] &= 0x00FFFFFF;
                }
            }
            dyn.updateDynamicTexture();
            HeadwearSkip.setTestImage(dyn, t, 64, 64);
            swapAll(tm, list, swapped, dyn);
            pass(mc, fb, list, partialTicks, 1, null, null);
            r.series.add(series(mc, fb, list, partialTicks, n, ca, da, cx, dx, "vzorec"));
            CustomNpcs.HeadWearType = 0;
            pass(mc, fb, list, partialTicks, 0, cx, dx);
            r.sensitivity = diffColor(ca, cx, n);
            CustomNpcs.HeadWearType = 1;

            for (String path : PRAVI_SKINI) {
                ITextureObject st = new SimpleTexture(new ResourceLocation("customnpcs", path));
                swapAll(tm, list, swapped, st);
                pass(mc, fb, list, partialTicks, 1, null, null);
                r.series.add(series(mc, fb, list, partialTicks, n, ca, da, cx, dx, path.substring(path.lastIndexOf('/') + 1)));
            }
        } catch (Exception e) {
            r.error = String.valueOf(e);
        } finally {
            CustomNpcs.RwHeadwearSkip = savedSkip;
            CustomNpcs.HeadWearType = savedType;
            for (Map.Entry<ResourceLocation, ITextureObject> en : swapped.entrySet()) {
                if (en.getValue() != null) {
                    // SimpleTexture se ob tem znova nalozi iz vira (kot ob F3+T).
                    tm.loadTexture(en.getKey(), en.getValue());
                } else {
                    tm.deleteTexture(en.getKey());
                }
            }
            HeadwearSkip.clearTestImages();
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

    /** Teksture vseh NPC-jev zamenja s {@code tex}; prvic zapomni original. */
    private static void swapAll(TextureManager tm, List<Entity> list, Map<ResourceLocation, ITextureObject> swapped, ITextureObject tex) {
        for (Entity e : list) {
            ResourceLocation loc = ((EntityNPCInterface)e).textureLocation;
            if (loc == null) {
                continue;
            }
            if (!swapped.containsKey(loc)) {
                swapped.put(loc, tm.getTexture(loc));
            }
            if (tm.getTexture(loc) != tex) {
                tm.loadTexture(loc, tex);
            }
        }
    }

    /** A (0), B (1), A2 (0); A ostane v ca/da. */
    private static Series series(Minecraft mc, Framebuffer fb, List<Entity> list, float pt, int n,
            ByteBuffer ca, FloatBuffer da, ByteBuffer cx, FloatBuffer dx, String name) {
        Series s = new Series(name);
        pass(mc, fb, list, pt, 0, ca, da);
        s.covered = 0;
        for (int i = 0; i < n; i++) {
            if (da.get(i) < 1.0f) {
                s.covered++;
            }
        }
        long d0 = HeadwearSkip.draws, f0 = HeadwearSkip.fallbacks + HeadwearSkip.stateFallbacks;
        long pn = HeadwearSkip.partsNone, ps = HeadwearSkip.partsSome, pa = HeadwearSkip.partsAll;
        pass(mc, fb, list, pt, 1, cx, dx);
        s.draws = HeadwearSkip.draws - d0;
        s.fallbacks = HeadwearSkip.fallbacks + HeadwearSkip.stateFallbacks - f0;
        s.partsNone = HeadwearSkip.partsNone - pn;
        s.partsSome = HeadwearSkip.partsSome - ps;
        s.partsAll = HeadwearSkip.partsAll - pa;
        s.diffColor = diffColor(ca, cx, n);
        s.diffDepth = diffDepth(da, dx, n);
        pass(mc, fb, list, pt, 0, cx, dx);
        s.diffRepeat = diffColor(ca, cx, n) + diffDepth(da, dx, n);
        return s;
    }

    private static void pass(Minecraft mc, Framebuffer fb, List<Entity> list, float pt, int mode, ByteBuffer color, FloatBuffer depth) {
        CustomNpcs.RwHeadwearSkip = mode;
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
}
