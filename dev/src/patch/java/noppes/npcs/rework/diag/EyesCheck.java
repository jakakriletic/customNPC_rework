package noppes.npcs.rework.diag;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.ModelEyeData;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.rework.render.EyesList;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * M5-K K3: preverba, da oci iz display lista dajo isto sliko kot original.
 *
 * <p>V eni slicici ({@code RenderWorldLastEvent}, iste matrike, isti {@code partialTicks}) narise
 * vse NPC-je v lasten framebuffer: original {@code A} ({@code RwEyesList=0}), list {@code B} (1,
 * prvi izris kljuca posname list), list {@code B2} (predvajanje ze posnetih listov), original
 * {@code A2}; primerja barvo in globino. Serije: oci testnega sveta; vzorci (vsak NPC drug tip,
 * vzorec, sijaj, debelina in barva obrvi); utrip, ki mora iti po originalu (obrvi se premikajo s
 * casom, zato se tam slika ne primerja); vse tri se ponovijo v povecavi ({@value #ZOOM}x na sredino
 * slike prek projekcijske matrike), ker so oci z razdalje kamere le nekaj pikslov. Med preverbo noben NPC ne utripa (razen v seriji utrip);
 * oci se spremenijo samo v klientu in se povrnejo. Vse le za harness ({@code -Drwharness}).
 */
@SideOnly(Side.CLIENT)
public final class EyesCheck {
    /** Serije 0-2 v pogledu kamere, 3-5 iste v povecavi (oci so z razdalje kamere le nekaj pikslov). */
    private static final String[] IMENA = {"svet", "vzorci", "utrip", "svet x8", "vzorci x8", "utrip x8"};
    private static final float ZOOM = 8.0f;
    private static final int[] BARVE = {0x4040FF, 0xE5E533, 0x993333, 55610, 0xFFFFFF, 0};

    public static final class Series {
        public final String name;
        public long covered = -1;
        public long diffColor = -1;
        public long diffDepth = -1;
        public long diffReplay = -1;
        public long diffRepeat = -1;
        public long drawsList = -1;
        public long drawsOriginal = -1;
        public long compiles = -1;
        /** NPC-jev z vklopljenimi ocmi v tej seriji. */
        public int withEyes;

        Series(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name + ": oci=" + this.withEyes + " pikslov=" + this.covered + " razlik=" + this.diffColor + "/" + this.diffDepth
                    + " predvajanje=" + this.diffReplay + " ponovitev=" + this.diffRepeat + " izrisov z listom="
                    + this.drawsList + " original=" + this.drawsOriginal + " prevodov=" + this.compiles;
        }
    }

    public int npcs;
    /** NPC-jev z vklopljenimi ocmi. */
    public int withEyes;
    public final List<Series> series = new ArrayList<Series>();
    public long sensitivity = -1;
    /** Obcutljivost v povecavi ({@value #ZOOM}x na sredino slike). */
    public long sensitivityZoom = -1;
    public String error = "";

    public boolean ok() {
        if (!this.error.isEmpty() || this.series.size() != IMENA.length || this.withEyes == 0 || this.sensitivity <= 0
                || this.sensitivityZoom <= 0) {
            return false;
        }
        for (int i = 0; i < this.series.size(); i++) {
            Series s = this.series.get(i);
            boolean blink = i % 3 == 2;
            if (s.covered <= 0 || s.withEyes == 0) {
                return false;
            }
            if (!blink && (s.diffColor != 0 || s.diffDepth != 0 || s.diffReplay != 0 || s.diffRepeat != 0
                    || s.drawsList != 2L * s.withEyes || s.drawsOriginal != 0 || s.compiles <= 0)) {
                return false;
            }
            if (blink && (s.drawsList != 0 || s.drawsOriginal != 2L * s.withEyes)) {
                return false;
            }
        }
        return true;
    }

    public static EyesCheck run(Minecraft mc, float pt) {
        EyesCheck r = new EyesCheck();
        int saved = CustomNpcs.RwEyesList;
        Framebuffer fb = null;
        List<EntityCustomNpc> list = new ArrayList<EntityCustomNpc>();
        List<NBTSave> backup = new ArrayList<NBTSave>();
        try {
            if (!OpenGlHelper.isFramebufferEnabled()) {
                r.error = "framebuffer ni na voljo";
                return r;
            }
            for (Entity e : mc.world.loadedEntityList) {
                if (e instanceof EntityCustomNpc && !e.isDead) {
                    EntityCustomNpc n = (EntityCustomNpc)e;
                    list.add(n);
                    backup.add(new NBTSave(n.modelData.eyes));
                    n.modelData.eyes.blinkStart = 0L;
                    if (n.modelData.eyes.isEnabled()) {
                        r.withEyes++;
                    }
                }
            }
            r.npcs = list.size();
            fb = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
            fb.setFramebufferColor(0.0f, 0.0f, 0.0f, 0.0f);
            int n = mc.displayWidth * mc.displayHeight;
            ByteBuffer ca = BufferUtils.createByteBuffer(4 * n);
            FloatBuffer da = BufferUtils.createFloatBuffer(n);
            ByteBuffer cx = BufferUtils.createByteBuffer(4 * n);
            FloatBuffer dx = BufferUtils.createFloatBuffer(n);
            for (int i = 0; i < IMENA.length; i++) {
                int variant = i % 3;
                float zoom = i < 3 ? 1.0f : ZOOM;
                for (int j = 0; j < list.size(); j++) {
                    ModelEyeData e = list.get(j).modelData.eyes;
                    backup.get(j).restore(e);
                    e.blinkStart = 0L;
                    if (variant == 1) {
                        e.type = (byte)(j % 2);
                        e.pattern = (byte)(j / 2 % 3);
                        e.glint = j / 6 % 2 == 0;
                        e.browThickness = j / 12 % 5;
                        e.color = BARVE[j % BARVE.length];
                        e.browColor = BARVE[(j + 3) % BARVE.length];
                    }
                }
                EyesList.clear();
                Series s = new Series(IMENA[i]);
                for (EntityCustomNpc e : list) {
                    if (e.modelData.eyes.isEnabled()) {
                        s.withEyes++;
                    }
                }
                boolean blink = variant == 2;
                pass(mc, fb, list, pt, 0, blink, zoom, ca, da);
                s.covered = 0;
                for (int j = 0; j < n; j++) {
                    if (ca.getInt(4 * j) != 0) {
                        s.covered++;
                    }
                }
                long l0 = EyesList.drawsList, o0 = EyesList.drawsOriginal, c0 = EyesList.compiles;
                pass(mc, fb, list, pt, 1, blink, zoom, cx, dx);
                s.diffColor = HeadwearCheck.diffColor(ca, cx, n);
                s.diffDepth = HeadwearCheck.diffDepth(da, dx, n);
                pass(mc, fb, list, pt, 1, blink, zoom, cx, dx);
                s.drawsList = EyesList.drawsList - l0;
                s.drawsOriginal = EyesList.drawsOriginal - o0;
                s.compiles = EyesList.compiles - c0;
                s.diffReplay = HeadwearCheck.diffColor(ca, cx, n) + HeadwearCheck.diffDepth(da, dx, n);
                pass(mc, fb, list, pt, 0, blink, zoom, cx, dx);
                s.diffRepeat = HeadwearCheck.diffColor(ca, cx, n) + HeadwearCheck.diffDepth(da, dx, n);
                r.series.add(s);
                if (variant == 0) {
                    // Obcutljivost: druga barva oci mora spremeniti sliko.
                    for (EntityCustomNpc e : list) {
                        e.modelData.eyes.color ^= 0x808080;
                    }
                    pass(mc, fb, list, pt, 0, blink, zoom, cx, dx);
                    long d = HeadwearCheck.diffColor(ca, cx, n);
                    if (zoom == 1.0f) {
                        r.sensitivity = d;
                    } else {
                        r.sensitivityZoom = d;
                    }
                }
            }
        } catch (Exception e) {
            r.error = String.valueOf(e);
        } finally {
            CustomNpcs.RwEyesList = saved;
            for (int i = 0; i < list.size(); i++) {
                backup.get(i).restore(list.get(i).modelData.eyes);
            }
            EyesList.clear();
            mc.getFramebuffer().bindFramebuffer(true);
            if (fb != null) {
                fb.deleteFramebuffer();
            }
        }
        return r;
    }

    /** Kopija polj oci, ki jih preverba spremeni. */
    private static final class NBTSave {
        final byte type;
        final byte pattern;
        final int color;
        final boolean glint;
        final int browThickness;
        final int browColor;
        final long blinkStart;

        NBTSave(ModelEyeData e) {
            this.type = e.type;
            this.pattern = e.pattern;
            this.color = e.color;
            this.glint = e.glint;
            this.browThickness = e.browThickness;
            this.browColor = e.browColor;
            this.blinkStart = e.blinkStart;
        }

        void restore(ModelEyeData e) {
            e.type = this.type;
            e.pattern = this.pattern;
            e.color = this.color;
            e.glint = this.glint;
            e.browThickness = this.browThickness;
            e.browColor = this.browColor;
            e.blinkStart = this.blinkStart;
        }
    }

    private static void pass(Minecraft mc, Framebuffer fb, List<EntityCustomNpc> list, float pt, int mode, boolean blink, float zoom, ByteBuffer color, FloatBuffer depth) {
        CustomNpcs.RwEyesList = mode;
        if (blink) {
            // Sredina utripa (LayerEyes.drawBrows: 300 ms), pred vsakim prehodom znova.
            long t = System.currentTimeMillis() - 75L;
            for (EntityCustomNpc e : list) {
                e.modelData.eyes.blinkStart = t;
            }
        }
        fb.framebufferClear();
        fb.bindFramebuffer(true);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        if (zoom != 1.0f) {
            // P' = S * P: povecava x in y okoli sredine slike, globina ostane enaka.
            FloatBuffer p = BufferUtils.createFloatBuffer(16);
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, p);
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            GlStateManager.scale(zoom, zoom, 1.0f);
            GlStateManager.multMatrix(p);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        }
        RenderHelper.enableStandardItemLighting();
        mc.entityRenderer.enableLightmap();
        for (Entity e : list) {
            mc.getRenderManager().renderEntityStatic(e, pt, false);
        }
        mc.entityRenderer.disableLightmap();
        RenderHelper.disableStandardItemLighting();
        if (zoom != 1.0f) {
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        }
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        if (color != null) {
            color.clear();
            depth.clear();
            GL11.glReadPixels(0, 0, fb.framebufferWidth, fb.framebufferHeight, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, color);
            GL11.glReadPixels(0, 0, fb.framebufferWidth, fb.framebufferHeight, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depth);
        }
    }
}
