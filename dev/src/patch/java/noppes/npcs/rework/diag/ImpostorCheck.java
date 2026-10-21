package noppes.npcs.rework.diag;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.far.FarClient;
import noppes.npcs.rework.far.FarLook;
import noppes.npcs.rework.far.ImpostorAtlas;
import noppes.npcs.rework.far.ImpostorBaker;
import noppes.npcs.rework.far.ImpostorFrames;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

/**
 * M5-V.4: primerjava pikslov impostor - model pri {@link #DISTANCE} blokih (D-046).
 *
 * <p>Za vsak testni videz (prvi opis, ki ga je klient prejel po kanalu {@code rwfar}, isti s petimi pravimi
 * skini CNPC in z velikostjo 10 in 2) videz speče ({@link ImpostorBaker#bakeNow}), nato v lasten framebuffer
 * {@link #S} x {@link #S} s perspektivo, ki ima pri {@link #DISTANCE} blokih toliko pikslov na blok kot
 * zaslon {@link #SCREEN_H} pri FOV 70 (ciljni zaslon raziskave M5-V.0 §3: NPC ~25 px), za vseh 8 kotov
 * narise (A) model z isto osvetlitvijo kot pri peki in (B) ploskev impostorja z okvirjem tega kota (mipmapi,
 * alfa test 0,1). Pokritost iz globine, barva iz RGB. Merila: IoU silhuete &gt;= {@link #IOU_MIN} in
 * povprecna razlika barve v preseku &lt;= {@link #DIFF_MAX} (od 255) za vsak videz; obcutljivost: impostor
 * enobarvnega skina proti modelu osnovnega videza mora dati razliko &gt; {@link #SENS_MIN}. Samo za harness.
 */
@SideOnly(Side.CLIENT)
public final class ImpostorCheck {
    public static final double DISTANCE = 56.0;
    public static final int SCREEN_H = 1080;
    public static final float FOV = 70.0f;
    public static final int S = 128;
    public static final double IOU_MIN = 0.70;
    public static final double DIFF_MAX = 32.0;
    public static final double SENS_MIN = 64.0;
    private static final String[] PRAVI_SKINI = {
        "textures/entity/elffemale/elffemale.png",
        "textures/entity/elffemale/elfmage.png",
        "textures/entity/elffemale/elfqueenformal.png",
        "textures/entity/elffemale/elfsoldierhelmet.png",
        "textures/entity/elfmale/blood_elf_mage.png",
    };
    private static final ResourceLocation SENS_TEX = new ResourceLocation("customnpcs", "textures/entity/rwcheck/impostor_sens.png");

    /** Izid enega videza cez 8 kotov. */
    public static final class Series {
        public final String name;
        public long covRef;
        public long covImp;
        public long inter;
        public long union;
        public double diffSum;
        public double iouMin = 1.0;
        public int tw;
        public int th;
        public float height;

        Series(String name) {
            this.name = name;
        }

        public double iou() {
            return this.union == 0 ? 0.0 : this.inter / (double) this.union;
        }

        public double diff() {
            return this.inter == 0 ? 255.0 : this.diffSum / this.inter;
        }

        boolean ok() {
            return this.covRef > 0 && iou() >= IOU_MIN && diff() <= DIFF_MAX;
        }

        @Override
        public String toString() {
            return String.format(java.util.Locale.ROOT, "%s: iou=%.3f (min kot %.3f) razlika=%.1f pikslov model/impostor=%d/%d okvir=%dx%d visina=%.2f",
                    this.name, iou(), this.iouMin, diff(), this.covRef, this.covImp, this.tw, this.th, this.height);
        }
    }

    public final List<Series> series = new ArrayList<Series>();
    public double sensitivity = -1.0;
    public int expected;
    public String error = "";

    public boolean ok() {
        if (!this.error.isEmpty() || this.series.size() != this.expected || this.sensitivity <= SENS_MIN) {
            return false;
        }
        for (Series s : this.series) {
            if (!s.ok()) {
                return false;
            }
        }
        return true;
    }

    public static ImpostorCheck run(Minecraft mc) {
        ImpostorCheck r = new ImpostorCheck();
        Framebuffer fb = null;
        DynamicTexture dyn = null;
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        try {
            if (!ImpostorBaker.available()) {
                r.error = "framebuffer ni na voljo";
                return r;
            }
            NBTTagCompound base = FarClient.anyDescription();
            if (base == null) {
                r.error = "klient nima nobenega opisa videza";
                return r;
            }
            List<String> names = new ArrayList<String>();
            List<NBTTagCompound> descs = new ArrayList<NBTTagCompound>();
            names.add("osnova");
            descs.add(base);
            for (String path : PRAVI_SKINI) {
                NBTTagCompound d = base.copy();
                d.setString("Texture", "customnpcs:" + path);
                d.setByte("UsingSkinUrl", (byte) 0);
                names.add(path.substring(path.lastIndexOf('/') + 1));
                descs.add(d);
            }
            for (int size : new int[] {10, 2}) {
                NBTTagCompound d = base.copy();
                d.setInteger("Size", size);
                names.add("velikost" + size);
                descs.add(d);
            }
            r.expected = descs.size();
            dyn = new DynamicTexture(64, 64);
            int[] t = dyn.getTextureData();
            for (int i = 0; i < t.length; ++i) {
                t[i] = 0xFFFF00FF;
            }
            dyn.updateDynamicTexture();
            mc.getTextureManager().loadTexture(SENS_TEX, dyn);
            NBTTagCompound sensDesc = base.copy();
            sensDesc.setString("Texture", SENS_TEX.toString());
            sensDesc.setByte("UsingSkinUrl", (byte) 0);
            ImpostorAtlas.Entry sens = ImpostorBaker.bakeNow(mc, FarLook.hash(sensDesc), sensDesc);
            if (sens == null) {
                r.error = "peka obcutljivosti ni uspela";
                return r;
            }

            fb = new Framebuffer(S, S, true);
            fb.setFramebufferColor(0.0f, 0.0f, 0.0f, 0.0f);
            IntBuffer cbuf = BufferUtils.createIntBuffer(S * S);
            FloatBuffer dbuf = BufferUtils.createFloatBuffer(S * S);
            float fovy = (float) Math.toDegrees(2.0 * Math.atan(S * Math.tan(Math.toRadians(FOV / 2.0)) / SCREEN_H));
            ImpostorAtlas atlas = ImpostorBaker.atlas();
            for (int i = 0; i < descs.size(); ++i) {
                NBTTagCompound desc = descs.get(i);
                ImpostorAtlas.Entry e = ImpostorBaker.bakeNow(mc, FarLook.hash(desc), desc);
                EntityNPCInterface npc = ImpostorBaker.createEntity(mc, desc);
                if (e == null || npc == null) {
                    r.error = "peka videza " + names.get(i) + " ni uspela";
                    return r;
                }
                Render<Entity> render = mc.getRenderManager().getEntityRenderObject(npc);
                Series s = new Series(names.get(i));
                s.tw = e.tw;
                s.th = e.th;
                s.height = e.height;
                double cy = e.bottom + e.height * 0.5;
                double sensSum = 0.0;
                long sensInter = 0;
                for (int k = 0; k < ImpostorFrames.ANGLES; ++k) {
                    begin(fb, fovy, cy);
                    ImpostorBaker.drawModel(mc, render, npc, k * 45.0f);
                    int[] refC = ImpostorBaker.readArgb(S, S, cbuf);
                    float[] refD = readDepth(dbuf);
                    begin(fb, fovy, cy);
                    drawImpostor(atlas, e, k);
                    int[] impC = ImpostorBaker.readArgb(S, S, cbuf);
                    float[] impD = readDepth(dbuf);
                    long in0 = s.inter;
                    long un0 = s.union;
                    compare(s, refC, refD, impC, impD);
                    long un = s.union - un0;
                    s.iouMin = Math.min(s.iouMin, un == 0 ? 0.0 : (s.inter - in0) / (double) un);
                    if (i == 0) {
                        begin(fb, fovy, cy);
                        drawImpostor(atlas, sens, k);
                        int[] sC = ImpostorBaker.readArgb(S, S, cbuf);
                        float[] sD = readDepth(dbuf);
                        Series x = new Series("obcutljivost");
                        compare(x, refC, refD, sC, sD);
                        sensSum += x.diffSum;
                        sensInter += x.inter;
                    }
                }
                if (i == 0) {
                    r.sensitivity = sensInter == 0 ? 0.0 : sensSum / sensInter;
                }
                r.series.add(s);
            }
        } catch (Exception e) {
            r.error = String.valueOf(e);
        } finally {
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.popMatrix();
            mc.getFramebuffer().bindFramebuffer(true);
            if (fb != null) {
                fb.deleteFramebuffer();
            }
            if (dyn != null) {
                mc.getTextureManager().deleteTexture(SENS_TEX);
            }
            GlStateManager.enableCull();
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        }
        return r;
    }

    /** Kamera na (0, cy, DISTANCE), gleda proti -Z (kot pri peki), perspektiva z navpicnim FOV fovy. */
    private static void begin(Framebuffer fb, float fovy, double cy) {
        fb.framebufferClear();
        fb.bindFramebuffer(true);
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.loadIdentity();
        Project.gluPerspective(fovy, 1.0f, 1.0f, 500.0f);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.loadIdentity();
        GlStateManager.translate(0.0, -cy, -DISTANCE);
    }

    private static void drawImpostor(ImpostorAtlas atlas, ImpostorAtlas.Entry e, int k) {
        float[] uv = atlas.uv(e, k);
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.disableBlend();
        GlStateManager.disableCull();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.bindTexture(atlas.textureId());
        float hw = e.width * 0.5f;
        float y0 = e.bottom;
        float y1 = e.bottom + e.height;
        Tessellator tes = Tessellator.getInstance();
        BufferBuilder b = tes.getBuffer();
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        b.pos(-hw, y1, 0.0).tex(uv[0], uv[1]).endVertex();
        b.pos(-hw, y0, 0.0).tex(uv[0], uv[3]).endVertex();
        b.pos(hw, y0, 0.0).tex(uv[2], uv[3]).endVertex();
        b.pos(hw, y1, 0.0).tex(uv[2], uv[1]).endVertex();
        tes.draw();
        GlStateManager.enableCull();
    }

    private static float[] readDepth(FloatBuffer buf) {
        buf.clear();
        GL11.glReadPixels(0, 0, S, S, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, buf);
        float[] d = new float[S * S];
        buf.get(d);
        return d;
    }

    /**
     * Pokritost iz globine (&lt; 1), barva iz RGB. Barva je v vrsticah od zgoraj, globina od spodaj; primerja
     * se isti piksel, zato se globina obrne.
     */
    static void compare(Series s, int[] refC, float[] refD, int[] impC, float[] impD) {
        for (int y = 0; y < S; ++y) {
            int di = (S - 1 - y) * S;
            for (int x = 0; x < S; ++x) {
                boolean a = refD[di + x] < 1.0f;
                boolean b = impD[di + x] < 1.0f;
                if (a) {
                    ++s.covRef;
                }
                if (b) {
                    ++s.covImp;
                }
                if (a || b) {
                    ++s.union;
                }
                if (a && b) {
                    ++s.inter;
                    int p = refC[y * S + x];
                    int q = impC[y * S + x];
                    s.diffSum += (Math.abs(((p >> 16) & 0xFF) - ((q >> 16) & 0xFF)) + Math.abs(((p >> 8) & 0xFF) - ((q >> 8) & 0xFF))
                            + Math.abs((p & 0xFF) - (q & 0xFF))) / 3.0;
                }
            }
        }
    }
}
