package noppes.npcs.rework.diag;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.rework.far.FarClient;
import noppes.npcs.rework.far.FarRender;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * M5-V.5: preverba izrisa L3 v pravi slicici (D-047), samo za harness.
 *
 * <p><b>Vidnost</b> (V23): sonda je najbolj oddaljen narisan nadomestek. Harness kamero usmeri vanjo in nastavi
 * FOV {@link #FOV} (povecava kot daljnogled; megla in globina ostaneta igrini). V treh zaporednih slicicah s
 * fiksno kamero se prebere okno okoli njenega pravokotnika na zaslonu (+{@link #MARGIN} px): A z izrisom L3, B brez
 * ({@link FarRender#suppress}), C spet z njim. NPC je viden, ce se A in B razlikujeta v vsaj enem pikslu za
 * vsaj {@link #DIFF_MIN} (od 255) v kaksnem kanalu; kontrola: A in C morata biti enaka (sicer se okno spreminja
 * samo od sebe in se poskusi znova, najvec {@link #ATTEMPTS}-krat). Megla ostane, kot jo nastavi igra.
 *
 * <p><b>Siv</b> (V24, {@link Seam}): med hojo kamere cez mejo sledilnika se vsako slicico zbere mnozica
 * narisanih NPC-jev (ziva entiteta prek {@code RenderLivingEvent.Post} ali impostor iz
 * {@link FarRender#DRAWN_IDS}). NPC, ki je bil narisan v prejsnji slicici in v tej ni, je <b>vrzel</b>, razen ce
 * ga ni vec nikjer in je bil na zunanjem robu pasu, ali ce je izven frustuma.
 */
@SideOnly(Side.CLIENT)
public final class FarDrawCheck {
    public static final int MARGIN = 3;
    public static final int DIFF_MIN = 3;
    public static final int ATTEMPTS = 5;
    /** Najvec slicic cakanja, da je sonda narisana. */
    public static final int WAIT_FRAMES = 240;
    /**
     * FOV preverbe (stopinje): sonda na 368 blokih je pri FOV 70 in 480 vrsticah ~1 x 2 piksla (pod mejo
     * zanesljive rasterizacije), pri 10 ~7 x 14. Megla je odvisna od razdalje, ne od FOV, zato ostane enaka.
     */
    public static final float FOV = 10.0f;
    /**
     * Sonda mora biti vsaj toliko blokov pred koncem megle (RD * 16, GL_LINEAR): dlje je NPC povsem v barvi megle
     * in neviden tudi v originalu (vd 10: diagonalni NPC na 168,8 > 160). Pri vd 24 meja 372 >= 368 (merilo).
     */
    public static final double FOG_MARGIN = 12.0;
    /** Slicice po usmeritvi kamere in FOV, preden se zacne A/B. */
    public static final int SETTLE_FRAMES = 20;

    public int probeId = FarRender.NO_PROBE;
    public double probeDist = -1.0;
    public int rectW;
    public int rectH;
    public int changed = -1;
    public int maxDiff = -1;
    public int control = -1;
    public int attempts;
    public String error = "";
    public boolean done;
    /** Smer kamere proti sondi (stopinje), ki jo harness drzi med preverbo. */
    public float aimYaw;
    public float aimPitch;

    private int step;
    private int waited;
    private int x0;
    private int y0;
    private int w;
    private int h;
    private int[] a;
    private int[] b;

    public boolean ok() {
        return this.done && this.error.isEmpty() && this.changed >= 1 && this.control == 0;
    }

    /** Izbere sondo: najbolj oddaljen (vodoravno) nadomestek brez zive entitete, ki je se pred koncem megle. */
    public void start(Minecraft mc) {
        Entity view = mc.getRenderViewEntity();
        double limit = mc.gameSettings.renderDistanceChunks * 16 - FOG_MARGIN;
        double best = -1.0;
        for (Integer id : FarClient.ids()) {
            if (mc.world.getEntityByID(id) != null) {
                continue;
            }
            double[] p = FarClient.position(id);
            double dx = p[0] - view.posX;
            double dz = p[2] - view.posZ;
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > best && d <= limit) {
                best = d;
                this.probeId = id;
            }
        }
        if (this.probeId == FarRender.NO_PROBE) {
            this.error = "ni nadomestka za sondo";
            this.done = true;
            return;
        }
        FarRender.probeId = this.probeId;
        double[] p = FarClient.position(this.probeId);
        double dx = p[0] - view.posX;
        double dy = p[1] + 0.9 - (view.posY + view.getEyeHeight());
        double dz = p[2] - view.posZ;
        this.aimYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        this.aimPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        this.step = 0;
        this.waited = -SETTLE_FRAMES;
    }

    /** Klice harness v {@code RenderWorldLastEvent} (najnizja prednost), po izrisu L3 te slicice. */
    public void onFrame(Minecraft mc) {
        if (this.done) {
            return;
        }
        float[] rect = FarRender.probeRect;
        switch (this.step) {
            case 0:
                if (this.waited < 0) {
                    ++this.waited;
                    return;
                }
                if (rect == null) {
                    if (++this.waited > WAIT_FRAMES) {
                        end("sonda " + this.probeId + " ni narisana v " + WAIT_FRAMES + " slicicah");
                    }
                    return;
                }
                this.probeDist = FarRender.probeDist;
                this.x0 = Math.max(0, (int) Math.floor(rect[0]) - MARGIN);
                this.y0 = Math.max(0, (int) Math.floor(rect[1]) - MARGIN);
                int x1 = Math.min(mc.displayWidth, (int) Math.ceil(rect[2]) + MARGIN);
                int y1 = Math.min(mc.displayHeight, (int) Math.ceil(rect[3]) + MARGIN);
                this.w = x1 - this.x0;
                this.h = y1 - this.y0;
                this.rectW = (int) Math.ceil(rect[2] - rect[0]);
                this.rectH = (int) Math.ceil(rect[3] - rect[1]);
                if (this.w <= 0 || this.h <= 0) {
                    end("pravokotnik sonde je izven zaslona");
                    return;
                }
                this.a = read(this.x0, this.y0, this.w, this.h);
                FarRender.suppress = true;
                this.step = 1;
                return;
            case 1:
                this.b = read(this.x0, this.y0, this.w, this.h);
                FarRender.suppress = false;
                this.step = 2;
                return;
            default:
                int[] c = read(this.x0, this.y0, this.w, this.h);
                ++this.attempts;
                int[] ab = compare(this.a, this.b);
                int[] ac = compare(this.a, c);
                this.changed = ab[0];
                this.maxDiff = ab[1];
                this.control = ac[0];
                if (this.control != 0 && this.attempts < ATTEMPTS) {
                    this.step = 0;
                    this.waited = -2;
                    return;
                }
                end("");
                return;
        }
    }

    private void end(String err) {
        this.error = err;
        this.done = true;
        FarRender.suppress = false;
        FarRender.probeId = FarRender.NO_PROBE;
    }

    private static int[] read(int x, int y, int w, int h) {
        IntBuffer buf = BufferUtils.createIntBuffer(w * h);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 4);
        GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
        GL11.glReadPixels(x, y, w, h, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buf);
        int[] px = new int[w * h];
        buf.get(px);
        return px;
    }

    /** {@code {pikslov z razliko >= DIFF_MIN, najvecja razlika kanala}}. */
    static int[] compare(int[] p, int[] q) {
        int n = 0;
        int max = 0;
        for (int i = 0; i < p.length; ++i) {
            int d = Math.max(Math.abs(((p[i] >> 16) & 0xFF) - ((q[i] >> 16) & 0xFF)),
                    Math.max(Math.abs(((p[i] >> 8) & 0xFF) - ((q[i] >> 8) & 0xFF)), Math.abs((p[i] & 0xFF) - (q[i] & 0xFF))));
            max = Math.max(max, d);
            if (d >= DIFF_MIN) {
                ++n;
            }
        }
        return new int[] {n, max};
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "sonda=%d razdalja=%.1f pravokotnik=%dx%d spremenjenih=%d najvecRazlika=%d kontrola=%d poskusov=%d %s",
                this.probeId, this.probeDist, this.rectW, this.rectH, this.changed, this.maxDiff, this.control, this.attempts, this.error);
    }

    /** V24: prehod cez mejo sledilnika (glej opis razreda). */
    public static final class Seam {
        /** Hitrost hoje igralca v vanilli (bloki na tick). */
        public static final double STEP = 0.2158;
        /** Pot nazaj (bloki) - vsi NPC-ji od ~20 do 64 blokov prestopijo mejo 64. */
        public static final double DISTANCE = 48.0;
        /** Toliko blokov od zunanjega roba pasu se izginotje steje kot izhod iz pasu, ne vrzel. */
        public static final double EDGE = 4.0;

        public final double outer;
        public int frames;
        public int gaps;
        public int liveToImpostor;
        public int impostorToLive;
        public int doubles;
        public int leftBand;
        public int culledExcused;
        public int maxWatched;
        /** Preklicani izrisi mrtvih entitet z nadomestkom med prehodom ({@code FarRender.deadHidden}). */
        public long deadHidden;
        public long deadHiddenAtStart;
        public String firstGaps = "";
        public String error = "";
        public final Set<Integer> liveRendered = new HashSet<Integer>();
        private final Map<Integer, Boolean> prev = new HashMap<Integer, Boolean>();
        private final Map<Integer, double[]> lastPos = new HashMap<Integer, double[]>();

        public Seam(double outer) {
            this.outer = outer;
        }

        public boolean ok() {
            return this.error.isEmpty() && this.frames > 0 && this.gaps == 0 && this.doubles == 0
                    && this.liveToImpostor > 0 && this.impostorToLive > 0;
        }

        /** Polozaj kamere na ticku {@code t} poti (nazaj do {@link #DISTANCE}, nato naprej na zacetek). */
        public static double offset(int t) {
            int half = (int) Math.ceil(DISTANCE / STEP);
            if (t <= half) {
                return Math.min(DISTANCE, t * STEP);
            }
            return Math.max(0.0, DISTANCE - (t - half) * STEP);
        }

        public static int ticks() {
            return 2 * (int) Math.ceil(DISTANCE / STEP) + 1;
        }

        /** Ena slicica: {@link #liveRendered} je napolnjen iz {@code RenderLivingEvent.Post} te slicice. */
        public void onFrame(Minecraft mc) {
            Entity view = mc.getRenderViewEntity();
            if (view == null || mc.world == null) {
                return;
            }
            ++this.frames;
            Map<Integer, Boolean> now = new HashMap<Integer, Boolean>();
            for (Integer id : this.liveRendered) {
                now.put(id, Boolean.TRUE);
                Entity e = mc.world.getEntityByID(id);
                if (e != null) {
                    this.lastPos.put(id, new double[] {e.posX, e.posY, e.posZ});
                }
            }
            for (Integer id : FarRender.DRAWN_IDS) {
                if (now.containsKey(id)) {
                    ++this.doubles;
                    continue;
                }
                now.put(id, Boolean.FALSE);
                double[] p = FarClient.position(id);
                if (p != null) {
                    this.lastPos.put(id, p);
                }
            }
            Frustum frustum = null;
            for (Map.Entry<Integer, Boolean> was : this.prev.entrySet()) {
                int id = was.getKey();
                Boolean is = now.get(id);
                if (is != null) {
                    if (was.getValue() && !is) {
                        ++this.liveToImpostor;
                    } else if (!was.getValue() && is) {
                        ++this.impostorToLive;
                    }
                    continue;
                }
                double[] p = this.lastPos.get(id);
                Entity live = mc.world.getEntityByID(id);
                boolean proxy = FarClient.ids().contains(id);
                boolean known = live != null || proxy;
                double cheb = p == null ? -1.0 : Math.max(Math.abs(p[0] - view.posX), Math.abs(p[2] - view.posZ));
                if (!known && cheb >= this.outer - EDGE) {
                    ++this.leftBand;
                    continue;
                }
                if (live != null) {
                    if (frustum == null) {
                        frustum = new Frustum();
                        frustum.setPosition(view.posX, view.posY, view.posZ);
                    }
                    if (!frustum.isBoundingBoxInFrustum(live.getRenderBoundingBox())) {
                        ++this.culledExcused;
                        continue;
                    }
                }
                ++this.gaps;
                if (this.gaps <= 5) {
                    this.firstGaps += String.format(Locale.ROOT, "%s[slicica %d id %d bil %s cheb %.1f ziv %d nadomestek %d]",
                            this.firstGaps.isEmpty() ? "" : " ", this.frames, id, was.getValue() ? "ziv" : "impostor", cheb,
                            live != null ? 1 : 0, proxy ? 1 : 0);
                }
            }
            this.prev.clear();
            this.prev.putAll(now);
            this.maxWatched = Math.max(this.maxWatched, now.size());
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "slicic=%d vrzeli=%d zivDoImpostorja=%d impostorDoZivega=%d dvojnih=%d izPasu=%d izvenFrustuma=%d najvecNarisanih=%d mrtvihSkritih=%d %s %s",
                    this.frames, this.gaps, this.liveToImpostor, this.impostorToLive, this.doubles, this.leftBand, this.culledExcused,
                    this.maxWatched, this.deadHidden, this.firstGaps, this.error);
        }
    }
}
