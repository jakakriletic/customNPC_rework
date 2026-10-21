package noppes.npcs.rework.far;

import java.lang.reflect.Method;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;
import noppes.npcs.rework.core.RenderEntitiesPatch;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * M5-V.5: izris L3 - nadomestki iz {@link FarClient} kot impostorji iz {@link ImpostorAtlas} (raziskava
 * M5-V.0 §5.3, §5.5, D-047).
 *
 * <p>Vsi nadomestki gredo v en {@link BufferBuilder} (format {@link #FORMAT} = polozaj, UV, barva, normala,
 * lightmap; normala je za senčilnike) in en klic risanja; pol-vidni (zastavica {@code F_HALF_VISIBLE}) v drugega
 * z alfo 0,15 brez pisanja globine, kot vanilla pri nevidnih entitetah. Ploskev je cilindricni billboard
 * (vrti se samo okoli navpicne osi proti kameri), okvir izbere {@link ImpostorFrames#frameFor} iz smeri kamera
 * -&gt; NPC in yaw nadomestka, svetloba je {@code World.getCombinedLight} v koordinatah lightmapa (osvezena ob
 * menjavi bloka in krozno na {@link #LIGHT_REFRESH} slicic). Polozaj je interpoliran ({@link FarInterp}).
 *
 * <p><b>Siv</b>: nadomestek se ne narise, ce ima {@code WorldClient} zivo entiteto z istim {@code entityId}
 * (vanilla jo narise sam); streznik posilja nadomestke 8 blokov znotraj sledilnika, zato NPC na meji ne
 * izgine (D-044). Nepecen videz se ne narise (L3 brez nadomestne silhuete); zato streznik videz izracuna ze
 * {@link FarBand#PREFETCH} blokov pred pasom. Mrtva entiteta z nadomestkom (unicena, a do naslednjega ticka se v
 * chunku) se ne narise ({@link #onRenderLivingPre}), sicer bi bila hkrati z impostorjem.
 *
 * <p><b>Mesto v slicici</b>: coremod ({@code RenderEntitiesPatch}) poklice {@link #renderEntities} v
 * {@code RenderGlobal.renderEntities} tik pred {@code "blockentities"}, v prehodu 0 - tam so megla, lightmap
 * in matrike kamere stanje entitet. Ce klica v tej slicici ni bilo (poseg ni uspel), se narise v
 * {@code RenderWorldLastEvent} z lastnim frustumom in rocno nastavljeno meglo (rezerva).
 */
@SideOnly(Side.CLIENT)
public final class FarRender {
    /** Polozaj, UV, barva, normala, lightmap. */
    public static final VertexFormat FORMAT = new VertexFormat()
            .addElement(DefaultVertexFormats.POSITION_3F)
            .addElement(DefaultVertexFormats.TEX_2F)
            .addElement(DefaultVertexFormats.COLOR_4UB)
            .addElement(DefaultVertexFormats.NORMAL_3B)
            .addElement(DefaultVertexFormats.PADDING_1B)
            .addElement(DefaultVertexFormats.TEX_2S);
    /** Alfa pol-vidnih (vanilla {@code RenderLivingBase.renderModel}: 0,15). */
    public static final int HALF_ALPHA = 38;
    /** Vsak nadomestek dobi svezo svetlobo vsaj na toliko slicic (potenca 2). */
    public static final int LIGHT_REFRESH = 128;
    /** Polna nebesna svetloba, dokler chunk na klientu ni nalozen. */
    static final int FULL_SKY = 15 << 20;

    /** Slicice z izrisom prek coremoda / prek rezerve. */
    public static long hookFrames;
    public static long fallbackFrames;
    /** Zadnja slicica: narisanih, preskocenih zaradi zive entitete, izven frustuma, brez pecenega videza, pol-vidnih. */
    public static int lastDrawn;
    public static int lastLive;
    public static int lastCulled;
    public static int lastUnbaked;
    public static int lastHalf;
    /** Najvecja vodoravna razdalja narisanega nadomestka v zadnji slicici (bloki). */
    public static double lastMaxDist;
    /** Zbirni stevci od {@link #resetStats}: slicice z izrisom, min/max narisanih, max ostalih, cas. */
    public static long statFrames;
    public static int statDrawnMin = Integer.MAX_VALUE;
    public static int statDrawnMax = -1;
    public static int statLiveMax = -1;
    public static int statCulledMax = -1;
    public static int statUnbakedMax = -1;
    public static double statMaxDist;
    public static long statNs;
    public static long statNsMax;
    public static long statFallback;
    /** Izrisi mrtvih entitet z nadomestkom, ki jih je {@link #onRenderLivingPre} preklical (siv). */
    public static long deadHidden;

    /** Harness: slicica brez izrisa (A/B pikslov). */
    public static boolean suppress;
    /** Harness: nadomestek, katerega pravokotnik na zaslonu se zapise ({@link #NO_PROBE} = noben). */
    public static final int NO_PROBE = Integer.MIN_VALUE;
    public static int probeId = NO_PROBE;
    /** Pravokotnik sonde v pikslih okna {@code {x0, y0, x1, y1}} (izvor spodaj levo) ali {@code null}. */
    public static float[] probeRect;
    public static double probeDist;
    /** Harness: id-ji, narisani v zadnji slicici (samo s {@link #trackIds}). */
    public static boolean trackIds;
    public static final Set<Integer> DRAWN_IDS = new HashSet<Integer>();

    private static final Map<Integer, ImpostorAtlas.Entry> LOOKS = new HashMap<Integer, ImpostorAtlas.Entry>();
    private static final List<Object[]> HALF = new ArrayList<Object[]>();
    private static final float[] UV = new float[4];
    private static final FloatBuffer MODELVIEW = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer PROJECTION = BufferUtils.createFloatBuffer(16);
    private static long frameNo;
    private static long hookFrame = -1L;
    private static int sweep;
    private static Method setupFog;
    private static boolean setupFogMissing;

    private FarRender() {
    }

    /** Klice {@link FarClient#install}. */
    static void install() {
        MinecraftForge.EVENT_BUS.register(new FarRender());
    }

    public static void resetStats() {
        statFrames = 0L;
        statDrawnMin = Integer.MAX_VALUE;
        statDrawnMax = -1;
        statLiveMax = -1;
        statCulledMax = -1;
        statUnbakedMax = -1;
        statMaxDist = 0.0;
        statNs = 0L;
        statNsMax = 0L;
        statFallback = 0L;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev) {
        if (ev.phase == TickEvent.Phase.START) {
            ++frameNo;
        }
    }

    /**
     * Klic iz {@code RenderGlobal.renderEntities} (coremod, pred {@code "blockentities"}); argumenti so
     * argumenti te metode.
     */
    public static void renderEntities(Entity view, ICamera camera, float partialTicks) {
        if (MinecraftForgeClient.getRenderPass() != 0) {
            return;
        }
        hookFrame = frameNo;
        if (CustomNpcs.RwFarView < 1 || FarClient.STORE.isEmpty() || view == null) {
            return;
        }
        ++hookFrames;
        draw(Minecraft.getMinecraft(), view, camera, partialTicks, false);
    }

    /**
     * Siv: {@code SPacketDestroyEntities} entiteto takoj odstrani iz {@code entitiesById} (impostor se ze narise),
     * v seznamih chunka pa ostane do naslednjega klientskega ticka in bi jo vanilla narisal se 1..n slicic hkrati z
     * impostorjem. Mrtva entiteta, ki ima nadomestek, se zato ne narise.
     */
    @SubscribeEvent
    public void onRenderLivingPre(RenderLivingEvent.Pre<EntityLivingBase> ev) {
        EntityLivingBase e = ev.getEntity();
        if (e.isDead && CustomNpcs.RwFarView >= 1 && FarClient.STORE.containsKey(e.getEntityId())) {
            ++deadHidden;
            ev.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderWorldLast(RenderWorldLastEvent ev) {
        if (hookFrame == frameNo || CustomNpcs.RwFarView < 1 || FarClient.STORE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        Entity view = mc.getRenderViewEntity();
        if (view == null || mc.world == null) {
            return;
        }
        float pt = ev.getPartialTicks();
        Frustum frustum = new Frustum();
        frustum.setPosition(view.lastTickPosX + (view.posX - view.lastTickPosX) * pt,
                view.lastTickPosY + (view.posY - view.lastTickPosY) * pt,
                view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt);
        ++fallbackFrames;
        mc.entityRenderer.enableLightmap();
        boolean fog = fog(mc.entityRenderer, pt);
        draw(mc, view, frustum, pt, true);
        if (fog) {
            GlStateManager.disableFog();
        }
        mc.entityRenderer.disableLightmap();
    }

    /** Megla kot pri terenu ({@code EntityRenderer.setupFog(0, pt)}, zasebna); brez nje izris brez megle. */
    private static boolean fog(EntityRenderer er, float pt) {
        if (setupFogMissing) {
            return false;
        }
        try {
            if (setupFog == null) {
                setupFog = ReflectionHelper.findMethod(EntityRenderer.class, "setupFog", "func_78468_a", int.class, float.class);
            }
            setupFog.invoke(er, 0, pt);
            return true;
        } catch (Exception e) {
            setupFogMissing = true;
            return false;
        }
    }

    private static void draw(Minecraft mc, Entity view, ICamera camera, float pt, boolean fallback) {
        WorldClient world = mc.world;
        if (world == null) {
            return;
        }
        long t0 = System.nanoTime();
        int drawn = 0;
        int live = 0;
        int culled = 0;
        int unbaked = 0;
        double maxDist = 0.0;
        if (trackIds) {
            DRAWN_IDS.clear();
        }
        probeRect = null;
        ImpostorAtlas atlas = ImpostorBaker.current();
        if (suppress || atlas == null) {
            unbaked = suppress ? 0 : FarClient.STORE.size();
            finish(t0, 0, 0, 0, unbaked, 0, 0.0, fallback);
            return;
        }
        double cx = view.lastTickPosX + (view.posX - view.lastTickPosX) * pt;
        double cy = view.lastTickPosY + (view.posY - view.lastTickPosY) * pt;
        double cz = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt;
        Frustum frustum = camera instanceof Frustum ? (Frustum) camera : null;
        int now = (int) System.currentTimeMillis();
        ++sweep;
        LOOKS.clear();
        HALF.clear();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Tessellator tes = Tessellator.getInstance();
        BufferBuilder b = tes.getBuffer();
        boolean begun = false;
        for (Map.Entry<Integer, int[]> me : FarClient.STORE.entrySet()) {
            int id = me.getKey();
            int[] e = me.getValue();
            if (world.getEntityByID(id) != null) {
                ++live;
                continue;
            }
            ImpostorAtlas.Entry ae = look(atlas, e[4]);
            if (ae == null) {
                ++unbaked;
                continue;
            }
            float a = FarInterp.fraction(e[FarInterp.E_T], now, FarInterp.DURATION_MS);
            double x = FarInterp.lerpBlocks(e[FarInterp.E_PX], e[0], a);
            double y = FarInterp.lerpBlocks(e[FarInterp.E_PY], e[1], a);
            double z = FarInterp.lerpBlocks(e[FarInterp.E_PZ], e[2], a);
            double hw = ae.width * 0.5;
            double y0 = y + ae.bottom;
            double y1 = y0 + ae.height;
            boolean in = frustum != null ? frustum.isBoxInFrustum(x - hw, y0, z - hw, x + hw, y1, z + hw)
                    : camera.isBoundingBoxInFrustum(new AxisAlignedBB(x - hw, y0, z - hw, x + hw, y1, z + hw));
            if (!in) {
                ++culled;
                continue;
            }
            if (e[FarInterp.E_LIGHT] < 0 || ((id + sweep) & (LIGHT_REFRESH - 1)) == 0) {
                pos.setPos(x, y + ae.height * 0.5, z);
                e[FarInterp.E_LIGHT] = world.isBlockLoaded(pos) ? world.getCombinedLight(pos, 0) : -1;
            }
            int light = e[FarInterp.E_LIGHT] < 0 ? FULL_SKY : e[FarInterp.E_LIGHT];
            double dx = x - cx;
            double dz = z - cz;
            maxDist = Math.max(maxDist, Math.sqrt(dx * dx + dz * dz));
            if (trackIds) {
                DRAWN_IDS.add(id);
            }
            if (id == probeId) {
                probe(mc, ae, dx, y0 - cy, y1 - cy, dz);
            }
            ++drawn;
            if ((e[5] & FarCodec.F_HALF_VISIBLE) != 0) {
                HALF.add(new Object[] {ae, new double[] {dx, y0 - cy, y1 - cy, dz}, e});
                continue;
            }
            if (!begun) {
                b.begin(GL11.GL_QUADS, FORMAT);
                begun = true;
            }
            quad(b, atlas, ae, e, dx, y0 - cy, y1 - cy, dz, light, 255);
        }
        if (begun || !HALF.isEmpty()) {
            RenderHelper.disableStandardItemLighting();
            GlStateManager.enableTexture2D();
            GlStateManager.bindTexture(atlas.textureId());
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
            GlStateManager.disableBlend();
            GlStateManager.disableCull();
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            if (begun) {
                tes.draw();
            }
            if (!HALF.isEmpty()) {
                b.begin(GL11.GL_QUADS, FORMAT);
                for (Object[] h : HALF) {
                    double[] p = (double[]) h[1];
                    int[] e = (int[]) h[2];
                    quad(b, atlas, (ImpostorAtlas.Entry) h[0], e, p[0], p[1], p[2], p[3],
                            e[FarInterp.E_LIGHT] < 0 ? FULL_SKY : e[FarInterp.E_LIGHT], HALF_ALPHA);
                }
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                GlStateManager.depthMask(false);
                GlStateManager.alphaFunc(GL11.GL_GREATER, 0.003921569f);
                tes.draw();
                GlStateManager.depthMask(true);
                GlStateManager.disableBlend();
                GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
            }
            GlStateManager.enableCull();
            RenderHelper.enableStandardItemLighting();
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        }
        finish(t0, drawn, live, culled, unbaked, HALF.size(), maxDist, fallback);
        HALF.clear();
    }

    private static ImpostorAtlas.Entry look(ImpostorAtlas atlas, int lookId) {
        if (LOOKS.containsKey(lookId)) {
            return LOOKS.get(lookId);
        }
        Long hash = lookId == 0 ? null : FarClient.LOOK_IDS.get(lookId);
        ImpostorAtlas.Entry ae = hash == null ? null : atlas.get(hash);
        if (ae == null && hash != null) {
            ImpostorBaker.want(hash);
        }
        LOOKS.put(lookId, ae);
        return ae;
    }

    /** Ena ploskev: oglisca zgoraj levo, spodaj levo, spodaj desno, zgoraj desno (kot {@code ImpostorCheck}). */
    private static void quad(BufferBuilder b, ImpostorAtlas atlas, ImpostorAtlas.Entry ae, int[] e,
            double dx, double y0, double y1, double dz, int light, int alpha) {
        float[] r = FarInterp.right(dx, dz);
        int k = ImpostorFrames.frameFor(ImpostorFrames.yawOf(dx, dz), FarInterp.yawOf(e[3]));
        atlas.uvInto(ae, k, UV);
        double hw = ae.width * 0.5;
        double rx = r[0] * hw;
        double rz = r[1] * hw;
        float nx = -r[1];
        float nz = r[0];
        int lu = (light >>> 16) & 0xFFFF;
        int lv = light & 0xFFFF;
        b.pos(dx - rx, y1, dz - rz).tex(UV[0], UV[1]).color(255, 255, 255, alpha).normal(nx, 0.0f, nz).lightmap(lu, lv).endVertex();
        b.pos(dx - rx, y0, dz - rz).tex(UV[0], UV[3]).color(255, 255, 255, alpha).normal(nx, 0.0f, nz).lightmap(lu, lv).endVertex();
        b.pos(dx + rx, y0, dz + rz).tex(UV[2], UV[3]).color(255, 255, 255, alpha).normal(nx, 0.0f, nz).lightmap(lu, lv).endVertex();
        b.pos(dx + rx, y1, dz + rz).tex(UV[2], UV[1]).color(255, 255, 255, alpha).normal(nx, 0.0f, nz).lightmap(lu, lv).endVertex();
    }

    /** Pravokotnik sonde na zaslonu iz trenutnih matrik (relativne koordinate kot pri izrisu). */
    private static void probe(Minecraft mc, ImpostorAtlas.Entry ae, double dx, double y0, double y1, double dz) {
        MODELVIEW.clear();
        PROJECTION.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODELVIEW);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION);
        float[] m = new float[16];
        float[] p = new float[16];
        MODELVIEW.get(m);
        PROJECTION.get(p);
        float[] r = FarInterp.right(dx, dz);
        double hw = ae.width * 0.5;
        float[] rect = null;
        for (int i = 0; i < 4; ++i) {
            double s = (i & 1) == 0 ? -hw : hw;
            double vy = (i & 2) == 0 ? y0 : y1;
            float[] w = FarInterp.project(m, p, dx + r[0] * s, vy, dz + r[1] * s, mc.displayWidth, mc.displayHeight);
            if (w == null) {
                return;
            }
            rect = rect == null ? new float[] {w[0], w[1], w[0], w[1]}
                    : new float[] {Math.min(rect[0], w[0]), Math.min(rect[1], w[1]), Math.max(rect[2], w[0]), Math.max(rect[3], w[1])};
        }
        probeRect = rect;
        probeDist = Math.sqrt(dx * dx + dz * dz);
    }

    private static void finish(long t0, int drawn, int live, int culled, int unbaked, int half, double maxDist, boolean fallback) {
        lastDrawn = drawn;
        lastLive = live;
        lastCulled = culled;
        lastUnbaked = unbaked;
        lastHalf = half;
        lastMaxDist = maxDist;
        long dt = System.nanoTime() - t0;
        ++statFrames;
        statDrawnMin = Math.min(statDrawnMin, drawn);
        statDrawnMax = Math.max(statDrawnMax, drawn);
        statLiveMax = Math.max(statLiveMax, live);
        statCulledMax = Math.max(statCulledMax, culled);
        statUnbakedMax = Math.max(statUnbakedMax, unbaked);
        statMaxDist = Math.max(statMaxDist, maxDist);
        statNs += dt;
        statNsMax = Math.max(statNsMax, dt);
        if (fallback) {
            ++statFallback;
        }
    }

    /** Vrstica za {@code /rwimpostor} in dnevnik. */
    public static String status() {
        return String.format(java.util.Locale.ROOT,
                "RWIMPOSTOR rwFarView=%d nadomestkov=%d atlas=%d videzov=%d peceno=%d napak=%d poseg=%d slicicPoseg=%d slicicRezerva=%d"
                        + " zadnja: narisanih=%d zivih=%d izvenFrustuma=%d brezPeke=%d polVidnih=%d najdlje=%.1f"
                        + " izris: slicic=%d usPovp=%.1f usMax=%.1f",
                CustomNpcs.RwFarView, FarClient.size(), ImpostorBaker.current() == null ? 0 : ImpostorBaker.current().size,
                ImpostorBaker.inAtlas(), ImpostorBaker.baked, ImpostorBaker.failures,
                RenderEntitiesPatch.patched() ? 1 : 0, hookFrames, fallbackFrames,
                lastDrawn, lastLive, lastCulled, lastUnbaked, lastHalf, lastMaxDist,
                statFrames, statFrames == 0 ? 0.0 : statNs / 1000.0 / statFrames, statNsMax / 1000.0);
    }
}
