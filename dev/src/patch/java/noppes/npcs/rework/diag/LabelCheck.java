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
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.render.LabelCache;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * M5-K K4: preverba, da napis iz display lista da isto sliko kot original.
 *
 * <p>V eni slicici ({@code RenderWorldLastEvent}, iste matrike, isti {@code partialTicks}) narise
 * vse NPC-je v lasten framebuffer: ogrevanje z {@code RwLabelCache=1} (prvi izris niza je original,
 * drugi posname list), nato original {@code A}, list {@code B}, original {@code A2}; primerja barvo
 * in globino. Serije: imena testnega sveta; imena in naslovi z barvnimi kodami, krepko, lezece in
 * sumniki (stran Unicode); niz s {@code §k}, ki mora iti po originalu. Imena se spremenijo samo v
 * klientu in se povrnejo. Vse le za harness ({@code -Drwharness}).
 */
@SideOnly(Side.CLIENT)
public final class LabelCheck {
    private static final String[][] NIZI = {
        null,
        {"§cRdeči §lkrepki §r§oNPC", "§eŠčž Česen §r12345"},
        {"§kskrito ime", "naslov"},
    };
    private static final String[] IMENA = {"skin", "kode", "obfuscated"};

    public static final class Series {
        public final String name;
        public long covered = -1;
        public long diffColor = -1;
        public long diffDepth = -1;
        public long diffRepeat = -1;
        public long drawsList = -1;
        public long drawsOriginal = -1;

        Series(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name + ": pikslov=" + this.covered + " razlik=" + this.diffColor + "/" + this.diffDepth
                    + " ponovitev=" + this.diffRepeat + " izrisov z listom=" + this.drawsList + " original=" + this.drawsOriginal;
        }
    }

    public int npcs;
    /** NPC-jev v dometu imen (√512 bloka od kamere). */
    public int labelled;
    public final List<Series> series = new ArrayList<Series>();
    public long sensitivity = -1;
    public String error = "";

    public boolean ok() {
        if (!this.error.isEmpty() || this.series.size() != NIZI.length || this.labelled == 0 || this.sensitivity <= 0) {
            return false;
        }
        for (int i = 0; i < this.series.size(); i++) {
            Series s = this.series.get(i);
            boolean obf = i == 2;
            // §k izbere nakljucne znake ob vsakem izrisu (tudi A proti A2 se razlikuje), zato se
            // tam preveri samo, da ime gre po originalu.
            if (s.covered <= 0 || (!obf && (s.diffColor != 0 || s.diffDepth != 0 || s.diffRepeat != 0))) {
                return false;
            }
            // Brez §k se vsak napis nariše z listom; ime s §k gre po originalu, naslov z listom.
            if (!obf && (s.drawsList < 2L * this.labelled || s.drawsOriginal != 0)) {
                return false;
            }
            if (obf && (s.drawsOriginal < this.labelled || s.drawsList < this.labelled)) {
                return false;
            }
        }
        return true;
    }

    public static LabelCheck run(Minecraft mc, float pt) {
        LabelCheck r = new LabelCheck();
        int saved = CustomNpcs.RwLabelCache;
        Framebuffer fb = null;
        List<EntityNPCInterface> list = new ArrayList<EntityNPCInterface>();
        List<String[]> names = new ArrayList<String[]>();
        try {
            if (!OpenGlHelper.isFramebufferEnabled()) {
                r.error = "framebuffer ni na voljo";
                return r;
            }
            Entity view = mc.getRenderViewEntity();
            for (Entity e : mc.world.loadedEntityList) {
                if (e instanceof EntityNPCInterface && !e.isDead) {
                    EntityNPCInterface n = (EntityNPCInterface)e;
                    list.add(n);
                    names.add(new String[] {n.display.getName(), n.display.getTitle()});
                    if (view != null && n.getDistanceSq(view) <= 512.0) {
                        r.labelled++;
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
            for (int i = 0; i < NIZI.length; i++) {
                if (NIZI[i] != null) {
                    for (EntityNPCInterface e : list) {
                        e.display.setName(NIZI[i][0]);
                        e.display.setTitle(NIZI[i][1]);
                    }
                }
                LabelCache.clear();
                pass(mc, fb, list, pt, 1, null, null);
                pass(mc, fb, list, pt, 1, null, null);
                Series s = new Series(IMENA[i]);
                pass(mc, fb, list, pt, 0, ca, da);
                s.covered = 0;
                for (int j = 0; j < n; j++) {
                    if (ca.getInt(4 * j) != 0) {
                        s.covered++;
                    }
                }
                long l0 = LabelCache.drawsList, o0 = LabelCache.drawsOriginal;
                pass(mc, fb, list, pt, 1, cx, dx);
                s.drawsList = LabelCache.drawsList - l0;
                s.drawsOriginal = LabelCache.drawsOriginal - o0;
                s.diffColor = HeadwearCheck.diffColor(ca, cx, n);
                s.diffDepth = HeadwearCheck.diffDepth(da, dx, n);
                pass(mc, fb, list, pt, 0, cx, dx);
                s.diffRepeat = HeadwearCheck.diffColor(ca, cx, n) + HeadwearCheck.diffDepth(da, dx, n);
                r.series.add(s);
                if (i == 1) {
                    // Obcutljivost: drug naslov mora spremeniti sliko.
                    for (EntityNPCInterface e : list) {
                        e.display.setTitle("drug naslov");
                    }
                    pass(mc, fb, list, pt, 0, cx, dx);
                    r.sensitivity = HeadwearCheck.diffColor(ca, cx, n);
                }
            }
        } catch (Exception e) {
            r.error = String.valueOf(e);
        } finally {
            CustomNpcs.RwLabelCache = saved;
            for (int i = 0; i < list.size(); i++) {
                list.get(i).display.setName(names.get(i)[0]);
                list.get(i).display.setTitle(names.get(i)[1]);
            }
            LabelCache.clear();
            mc.getFramebuffer().bindFramebuffer(true);
            if (fb != null) {
                fb.deleteFramebuffer();
            }
        }
        return r;
    }

    private static void pass(Minecraft mc, Framebuffer fb, List<EntityNPCInterface> list, float pt, int mode, ByteBuffer color, FloatBuffer depth) {
        CustomNpcs.RwLabelCache = mode;
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
}
