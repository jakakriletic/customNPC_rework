package noppes.npcs.rework.diag;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.render.NpcBatch;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * M14.2: visual proof of {@link NpcBatch} against the original in one frame
 * ({@code RenderWorldLastEvent}, same camera, partialTicks and entity state). Every pass draws
 * into an own framebuffer: original A ({@code RwNpcInstancing=0}), candidate B (1), original A2.
 * A == A2 bit for bit is the validity condition. A/B use the criteria frozen in M14.0 §5.2
 * (Q475) for the whole scene and for every NPC alone: equal silhouette mask (depth &lt; 1),
 * at least 99.9 % of covered pixels with every RGB channel within 2/255, no channel above 8/255,
 * equal alpha, depth within 1e-6; additionally every uncovered pixel bit-identical (state leak).
 * B must draw every NPC through the batch without fallback. Two negative controls (head pose
 * +5 degrees, U shift of one texel) must be rejected. Harness only ({@code -Drwharness}).
 */
@SideOnly(Side.CLIENT)
public final class NpcBatchCheck {
    /** Result of one A/B comparison. */
    public static final class Cmp {
        public long union, maskDiff, within2, over8, alphaDiff, depthOver, outsideDiff, repeatDiff = -1;
        public int maxChannel;
        public float maxDepth;
        public long draws = -1, fallbacks = -1;

        public boolean pass(long expectedDraws) {
            return this.union > 0 && this.maskDiff == 0 && this.within2 * 1000L >= this.union * 999L && this.over8 == 0
                    && this.alphaDiff == 0 && this.depthOver == 0 && this.outsideDiff == 0 && this.repeatDiff == 0
                    && this.draws == expectedDraws && this.fallbacks == 0;
        }

        /** Image criteria only (negative controls have no repeat/draw expectations). */
        boolean imageEqual() {
            return this.union > 0 && this.maskDiff == 0 && this.within2 * 1000L >= this.union * 999L && this.over8 == 0
                    && this.alphaDiff == 0 && this.depthOver == 0 && this.outsideDiff == 0;
        }

        @Override
        public String toString() {
            return "pokritih=" + this.union + " maska=" + this.maskDiff + " do2=" + this.within2 + " nad8=" + this.over8
                    + " maxKanal=" + this.maxChannel + " alfa=" + this.alphaDiff + " globina>1e-6=" + this.depthOver
                    + " maxGlobina=" + this.maxDepth + " zunaj=" + this.outsideDiff + " ponovitev=" + this.repeatDiff
                    + " izrisov=" + this.draws + " original=" + this.fallbacks;
        }
    }

    public int width;
    public int height;
    public int npcs;
    public Cmp scene;
    public Cmp poseControl;
    public Cmp uvControl;
    public int npcPassed;
    public int npcFailed;
    public String worst = "";
    public String firstFailed = "";
    public long worstWithin2PerMille = 1000;
    public int worstChannel;
    public int glError = -1;
    public String images = "";
    public String error = "";

    public boolean ok() {
        return this.error.isEmpty() && this.npcs > 0 && this.scene != null && this.scene.pass(this.npcs)
                && this.npcPassed == this.npcs && this.npcFailed == 0
                && this.poseControl != null && !this.poseControl.imageEqual()
                && this.uvControl != null && !this.uvControl.imageEqual() && this.glError == 0;
    }

    public static NpcBatchCheck run(Minecraft mc, float partialTicks, String imageBase) {
        NpcBatchCheck r = new NpcBatchCheck();
        int saved = CustomNpcs.RwNpcInstancing;
        Framebuffer fb = null;
        List<Entity> list = new ArrayList<Entity>();
        List<Long> blinks = new ArrayList<Long>();
        try {
            if (!OpenGlHelper.isFramebufferEnabled()) {
                r.error = "framebuffer ni na voljo";
                return r;
            }
            for (Entity e : mc.world.loadedEntityList) {
                if (e instanceof EntityNPCInterface && !e.isDead) {
                    list.add(e);
                }
            }
            r.npcs = list.size();
            // Eye blinks follow System.currentTimeMillis; frozen open so A == A2 tests only the renderer.
            for (Entity e : list) {
                if (e instanceof EntityCustomNpc) {
                    blinks.add(((EntityCustomNpc) e).modelData.eyes.blinkStart);
                    ((EntityCustomNpc) e).modelData.eyes.blinkStart = 0L;
                }
            }
            r.width = mc.displayWidth;
            r.height = mc.displayHeight;
            fb = new Framebuffer(r.width, r.height, true);
            fb.setFramebufferColor(0.0f, 0.0f, 0.0f, 0.0f);
            int n = r.width * r.height;
            ByteBuffer ca = BufferUtils.createByteBuffer(4 * n), cb = BufferUtils.createByteBuffer(4 * n), cc = BufferUtils.createByteBuffer(4 * n);
            FloatBuffer da = BufferUtils.createFloatBuffer(n), db = BufferUtils.createFloatBuffer(n), dc = BufferUtils.createFloatBuffer(n);
            // Only errors raised by the check itself are counted.
            for (int i = 0; i < 16 && GL11.glGetError() != GL11.GL_NO_ERROR; i++) { }
            NpcBatch.checkScope = true;
            // Warm-up: program, mesh upload and every lazy original list outside compared passes.
            pass(mc, fb, list, partialTicks, 1, null, null);
            pass(mc, fb, list, partialTicks, 0, null, null);

            r.scene = series(mc, fb, list, partialTicks, n, ca, da, cb, db, cc, dc);
            if (!imageBase.isEmpty()) {
                r.images = png(ca, r.width, r.height, imageBase + "-instancing-A.png", null) + ","
                        + png(cb, r.width, r.height, imageBase + "-instancing-B.png", null) + ","
                        + png(ca, r.width, r.height, imageBase + "-instancing-razlika.png", cb);
            }
            NpcBatch.testPoseDegrees = 5.0f;
            pass(mc, fb, list, partialTicks, 1, cc, dc);
            NpcBatch.testPoseDegrees = 0.0f;
            r.poseControl = compare(ca, da, cc, dc, n);
            NpcBatch.testUvShift = 1.0f / 64.0f;
            pass(mc, fb, list, partialTicks, 1, cc, dc);
            NpcBatch.testUvShift = 0.0f;
            r.uvControl = compare(ca, da, cc, dc, n);

            List<Entity> one = new ArrayList<Entity>(1);
            one.add(null);
            for (Entity e : list) {
                one.set(0, e);
                Cmp c = series(mc, fb, one, partialTicks, n, ca, da, cb, db, cc, dc);
                if (c.pass(1)) {
                    r.npcPassed++;
                } else {
                    r.npcFailed++;
                }
                if (!c.pass(1) && r.firstFailed.isEmpty()) {
                    r.firstFailed = e.getEntityId() + ": " + c;
                }
                long perMille = c.union == 0 ? 0 : c.within2 * 1000L / c.union;
                if (r.worst.isEmpty() || perMille < r.worstWithin2PerMille
                        || (perMille == r.worstWithin2PerMille && c.maxChannel > r.worstChannel)) {
                    r.worst = e.getEntityId() + ": " + c;
                    r.worstWithin2PerMille = perMille;
                    r.worstChannel = c.maxChannel;
                }
            }
            r.glError = GL11.glGetError();
        } catch (Exception e) {
            r.error = String.valueOf(e);
        } finally {
            NpcBatch.checkScope = false;
            NpcBatch.testPoseDegrees = 0.0f;
            NpcBatch.testUvShift = 0.0f;
            CustomNpcs.RwNpcInstancing = saved;
            int b = 0;
            for (Entity e : list) {
                if (e instanceof EntityCustomNpc && b < blinks.size()) {
                    ((EntityCustomNpc) e).modelData.eyes.blinkStart = blinks.get(b++);
                }
            }
            mc.getFramebuffer().bindFramebuffer(true);
            if (fb != null) {
                fb.deleteFramebuffer();
            }
        }
        return r;
    }

    /** A (0), B (1), A2 (0); A stays in ca/da, B in cb/db. */
    private static Cmp series(Minecraft mc, Framebuffer fb, List<Entity> list, float pt, int n,
            ByteBuffer ca, FloatBuffer da, ByteBuffer cb, FloatBuffer db, ByteBuffer cc, FloatBuffer dc) {
        pass(mc, fb, list, pt, 0, ca, da);
        long d0 = NpcBatch.draws;
        long f0 = total(NpcBatch.fallbacks());
        pass(mc, fb, list, pt, 1, cb, db);
        Cmp c = compare(ca, da, cb, db, n);
        c.draws = NpcBatch.draws - d0;
        c.fallbacks = total(NpcBatch.fallbacks()) - f0;
        pass(mc, fb, list, pt, 0, cc, dc);
        long repeat = 0;
        for (int i = 0; i < n; i++) {
            if (ca.getInt(4 * i) != cc.getInt(4 * i) || Float.floatToRawIntBits(da.get(i)) != Float.floatToRawIntBits(dc.get(i))) {
                repeat++;
            }
        }
        c.repeatDiff = repeat;
        return c;
    }

    private static long total(long[] values) {
        long t = 0;
        for (long v : values) {
            t += v;
        }
        return t;
    }

    static Cmp compare(ByteBuffer ca, FloatBuffer da, ByteBuffer cb, FloatBuffer db, int n) {
        Cmp c = new Cmp();
        for (int i = 0; i < n; i++) {
            float za = da.get(i), zb = db.get(i);
            boolean inA = za < 1.0f, inB = zb < 1.0f;
            if (!inA && !inB) {
                if (ca.getInt(4 * i) != cb.getInt(4 * i)) {
                    c.outsideDiff++;
                }
                continue;
            }
            c.union++;
            if (inA != inB) {
                c.maskDiff++;
            }
            int worst = 0;
            for (int k = 0; k < 3; k++) {
                worst = Math.max(worst, Math.abs((ca.get(4 * i + k) & 0xFF) - (cb.get(4 * i + k) & 0xFF)));
            }
            c.maxChannel = Math.max(c.maxChannel, worst);
            if (worst <= 2) {
                c.within2++;
            }
            if (worst > 8) {
                c.over8++;
            }
            if (ca.get(4 * i + 3) != cb.get(4 * i + 3)) {
                c.alphaDiff++;
            }
            float dz = Math.abs(za - zb);
            c.maxDepth = Math.max(c.maxDepth, dz);
            if (dz > 1.0e-6f) {
                c.depthOver++;
            }
        }
        return c;
    }

    private static void pass(Minecraft mc, Framebuffer fb, List<Entity> list, float pt, int mode, ByteBuffer color, FloatBuffer depth) {
        CustomNpcs.RwNpcInstancing = mode;
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

    /** PNG of one pass (bottom-up rows), or 32x amplified |A - B| when {@code other} is given. */
    private static String png(ByteBuffer c, int w, int h, String path, ByteBuffer other) {
        try {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int i = 4 * (y * w + x);
                    int argb;
                    if (other == null) {
                        argb = (c.get(i + 3) & 0xFF) << 24 | (c.get(i) & 0xFF) << 16 | (c.get(i + 1) & 0xFF) << 8 | (c.get(i + 2) & 0xFF);
                    } else {
                        int rr = Math.min(255, 32 * Math.abs((c.get(i) & 0xFF) - (other.get(i) & 0xFF)));
                        int gg = Math.min(255, 32 * Math.abs((c.get(i + 1) & 0xFF) - (other.get(i + 1) & 0xFF)));
                        int bb = Math.min(255, 32 * Math.abs((c.get(i + 2) & 0xFF) - (other.get(i + 2) & 0xFF)));
                        argb = 0xFF000000 | rr << 16 | gg << 8 | bb;
                    }
                    img.setRGB(x, h - 1 - y, argb);
                }
            }
            File f = new File(path);
            ImageIO.write(img, "png", f);
            return f.getName();
        } catch (Exception e) {
            return "napaka:" + e;
        }
    }
}
