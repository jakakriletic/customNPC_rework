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
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
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
import noppes.npcs.entity.EntityNPCInterface;
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
 *
 * <p><b>L2</b> (M5-V.6, {@code RwFarView=2}, D-048): sledeni NPC, ki ga vanilla ponudi v izris (prehod 0) in je
 * dlje od {@code RwImpostorStart} ({@link ImpostorL2}, s histerezo), ima pecen videz in je stoj, viden, ziv,
 * ne jaha in ni jahan, dobi preklican {@code RenderLivingEvent.Pre} (brez popravka rendererja CNPC; senca ostane
 * vanilla) in gre kot impostor v isti {@link BufferBuilder}. Videz se izracuna na klientu iz prave entitete
 * ({@link FarLook#describe} spawn NBT, isti opis kot na strezniku), najvec {@link #L2_HASHES_PER_FRAME} na
 * slicico, znova po vsakem branju spawn podatkov ({@code rwLookVersion}). Dokler videz ni pecen, vanilla
 * narise model. Sledeni NPC, ki ga vanilla zaradi dometa upodobitve ne ponudi ({@code isInRangeToRender3d},
 * npr. NPC brez hitboxa onkraj ~38 blokov pri RD 8), se narise kot impostor do konca megle, ne glede na prag.
 *
 * <p><b>Senčila</b> (M5-V.7, D-050): {@link FarCompat} - z aktivnim paketom senčil OptiFine in {@code RwFarShaders=0}
 * klient rise original (brez preklica L2, brez impostorjev in brez skrivanja mrtvih), v senčnem prehodu se
 * impostorji ne risejo.
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

    /** M5-V.6: najvec izracunov videza L2 na slicico. */
    public static final int L2_HASHES_PER_FRAME = 16;
    /** Zadnja slicica L2: narisanih skupaj, od tega sledenih izven dometa upodobitve vanille. */
    public static int lastL2;
    public static int lastL2Range;
    /** Zbirni L2 od {@link #resetStats}: min/max narisanih na slicico, max izven dometa, preklici, brez peke. */
    public static int statL2Min = Integer.MAX_VALUE;
    public static int statL2Max = -1;
    public static int statL2RangeMax = -1;
    public static long statL2Cancelled;
    public static long statL2Unbaked;
    /** Skupaj od zagona: izracunani videzi L2, napake izracuna. */
    public static long l2Hashes;
    public static long l2HashErrors;

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
    /**
     * Harness M5-V.7: predpona imen posebnih NPC-jev ({@code null} = brez sledenja) in njihovi stevci od
     * {@link #resetStats}: ime -&gt; {ponujen v prehodu 0, preklican L2, narisan pol-viden, narisan izven dometa vanille,
     * ponujen med jahanjem, preklican med jahanjem}.
     */
    public static String specialPrefix;
    public static final Map<String, long[]> SPECIAL = new java.util.TreeMap<String, long[]>();
    public static final int SP_OFFERED = 0;
    public static final int SP_CANCELLED = 1;
    public static final int SP_HALF = 2;
    public static final int SP_RANGE = 3;
    public static final int SP_RIDING = 4;
    public static final int SP_RIDING_CANCELLED = 5;

    private static final Map<Integer, ImpostorAtlas.Entry> LOOKS = new HashMap<Integer, ImpostorAtlas.Entry>();
    private static final List<Object[]> HALF = new ArrayList<Object[]>();
    /** L2: entitete s preklicanim vanilla izrisom v tej slicici. */
    private static final List<EntityNPCInterface> L2 = new ArrayList<EntityNPCInterface>();
    /** L2: {@code entityId -> {hash videza (0 = ni), rwLookVersion, v L2 (0/1)}} za trenutni svet. */
    private static final Map<Integer, long[]> L2_STATE = new HashMap<Integer, long[]>();
    private static WorldClient l2World;
    private static int l2Budget;
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
        statL2Min = Integer.MAX_VALUE;
        statL2Max = -1;
        statL2RangeMax = -1;
        statL2Cancelled = 0L;
        statL2Unbaked = 0L;
        SPECIAL.clear();
    }

    /**
     * Harness: {@code ime=ponujen/preklican/polViden/izvenDometa/jahanje/jahanjePreklican@RRGGBB;...} za vse posebne
     * NPC-je v svetu (tudi tiste brez stevcev) ali prazen niz brez sledenja; {@code @RRGGBB} je povprecna barva
     * pecenega okvirja 0 videza L2 ({@code @-}: brez videza L2 ali brez peke).
     */
    public static String specialSummary(WorldClient world) {
        if (specialPrefix == null) {
            return "";
        }
        Map<String, String> rgb = new HashMap<String, String>();
        if (world != null) {
            for (Entity en : world.loadedEntityList) {
                if (en instanceof EntityNPCInterface && special((EntityNPCInterface) en) != null) {
                    long[] s = L2_STATE.get(en.getEntityId());
                    Integer c = s == null ? null : ImpostorBaker.BAKED_RGB.get(s[0]);
                    if (c != null && c >= 0) {
                        rgb.put(((EntityNPCInterface) en).display.getName(), String.format("%06X", c));
                    }
                }
            }
        }
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, long[]> me : SPECIAL.entrySet()) {
            if (b.length() > 0) {
                b.append(";");
            }
            b.append(me.getKey()).append("=");
            long[] c = me.getValue();
            for (int i = 0; i < c.length; ++i) {
                b.append(i == 0 ? "" : "/").append(c[i]);
            }
            String col = rgb.get(me.getKey());
            b.append("@").append(col == null ? "-" : col);
        }
        return b.toString();
    }

    /** Stevci posebnega NPC-ja ali {@code null} (brez sledenja ali drugo ime). */
    static long[] special(EntityNPCInterface npc) {
        if (specialPrefix == null) {
            return null;
        }
        String name = npc.display.getName();
        if (name == null || !name.startsWith(specialPrefix)) {
            return null;
        }
        long[] c = SPECIAL.get(name);
        if (c == null) {
            c = new long[6];
            SPECIAL.put(name, c);
        }
        return c;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev) {
        if (ev.phase == TickEvent.Phase.START) {
            ++frameNo;
            L2.clear();
            l2Budget = L2_HASHES_PER_FRAME;
            if ((frameNo & 1023) == 0 && l2World != null) {
                for (java.util.Iterator<Integer> it = L2_STATE.keySet().iterator(); it.hasNext();) {
                    if (l2World.getEntityByID(it.next()) == null) {
                        it.remove();
                    }
                }
            }
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
        int compat = FarCompat.mode(CustomNpcs.RwFarShaders);
        if (compat == FarCompat.SKIP) {
            ++FarCompat.shadowSkips;
            return;
        }
        hookFrame = frameNo;
        if (!active() || view == null) {
            return;
        }
        if (compat == FarCompat.OFF) {
            ++FarCompat.offFrames;
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
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderLivingPre(RenderLivingEvent.Pre<EntityLivingBase> ev) {
        EntityLivingBase e = ev.getEntity();
        if (CustomNpcs.RwFarView < 1) {
            return;
        }
        int compat = FarCompat.mode(CustomNpcs.RwFarShaders);
        if (compat == FarCompat.OFF) {
            return;
        }
        if (e.isDead && FarClient.STORE.containsKey(e.getEntityId())) {
            ++deadHidden;
            ev.setCanceled(true);
            return;
        }
        // M5-V.6: samo izris sveta (prehod 0); GUI-ji in peka rišejo v prehodu -1 ali entiteto izven sveta.
        if (CustomNpcs.RwFarView >= 2 && e instanceof EntityNPCInterface && MinecraftForgeClient.getRenderPass() == 0) {
            Minecraft mc = Minecraft.getMinecraft();
            Entity view = mc.getRenderViewEntity();
            EntityNPCInterface npc = (EntityNPCInterface) e;
            long[] sp = compat == FarCompat.DRAW ? special(npc) : null;
            boolean riding = sp != null && (npc.isRiding() || npc.isBeingRidden());
            if (sp != null) {
                ++sp[SP_OFFERED];
                if (riding) {
                    ++sp[SP_RIDING];
                }
            }
            if (view != null && l2(mc, npc, npc.getDistanceSq(view), false) != null) {
                ev.setCanceled(true);
                if (compat == FarCompat.SKIP) {
                    // Senčni prehod: impostor se ne narise, model tudi ne (senčna mapa brez oddaljenih NPC-jev).
                    return;
                }
                L2.add(npc);
                ++statL2Cancelled;
                if (sp != null) {
                    ++sp[SP_CANCELLED];
                    if (riding) {
                        ++sp[SP_RIDING_CANCELLED];
                    }
                }
            }
        }
    }

    /** Ali je izris v tej slicici potreben (L3 nadomestki ali L2). */
    private static boolean active() {
        return CustomNpcs.RwFarView >= 2 || (CustomNpcs.RwFarView >= 1 && !FarClient.STORE.isEmpty());
    }

    /**
     * L2: atlas videza, ce se ta NPC v tej slicici narise kot impostor, sicer {@code null} (vanilla model).
     * {@code beyondRange}: vanilla ga zaradi dometa upodobitve ne narise, prag in histereza ne veljata.
     */
    private static ImpostorAtlas.Entry l2(Minecraft mc, EntityNPCInterface npc, double distSq, boolean beyondRange) {
        WorldClient w = mc.world;
        if (w == null || npc.world != w || w.getEntityByID(npc.getEntityId()) != npc) {
            return null;
        }
        if (w != l2World) {
            L2_STATE.clear();
            l2World = w;
        }
        int id = npc.getEntityId();
        long[] s = L2_STATE.get(id);
        if (!beyondRange) {
            boolean in = ImpostorL2.next(s != null && s[2] != 0L, distSq, ImpostorL2.start(CustomNpcs.RwImpostorStart));
            if (s != null) {
                s[2] = in ? 1L : 0L;
            }
            if (!in) {
                return null;
            }
        }
        // Ne isInvisible(): CNPC ga vrne za vsak NpcVisible != 0, pol-viden (2) pa je pol-prozoren impostor (HALF).
        if (npc.isKilled() || npc.display.getVisible() == 1 || npc.currentAnimation != 0
                || npc.isRiding() || npc.isBeingRidden()) {
            return null;
        }
        if (s == null || s[1] != npc.rwLookVersion) {
            if (l2Budget <= 0) {
                return null;
            }
            --l2Budget;
            long hash = lookHash(npc);
            if (s == null) {
                s = new long[] {hash, npc.rwLookVersion, beyondRange ? 0L : 1L};
                L2_STATE.put(id, s);
            } else {
                s[0] = hash;
                s[1] = npc.rwLookVersion;
            }
        }
        if (s[0] == 0L) {
            return null;
        }
        ImpostorAtlas atlas = ImpostorBaker.current();
        ImpostorAtlas.Entry ae = atlas == null ? null : atlas.get(s[0]);
        if (ae == null) {
            ++statL2Unbaked;
            if (FarClient.DESCS.containsKey(s[0])) {
                ImpostorBaker.want(s[0]);
            } else {
                // Opis je izpadel iz LRU pred peko: naslednjic znova.
                s[1] = Long.MIN_VALUE;
            }
        }
        return ae;
    }

    /** Videz prave entitete na klientu (isti opis kot streznikov {@code FarServer.fillLooks}); 0 ob napaki. */
    private static long lookHash(EntityNPCInterface npc) {
        try {
            ResourceLocation key = EntityList.getKey(npc);
            NBTTagCompound desc = FarLook.describe(key == null ? "" : key.toString(), npc.writeSpawnData());
            long hash = FarLook.hash(desc);
            FarClient.DESCS.put(hash, desc);
            ++l2Hashes;
            return hash;
        } catch (RuntimeException ex) {
            ++l2HashErrors;
            return 0L;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderWorldLast(RenderWorldLastEvent ev) {
        if (hookFrame == frameNo || !active() || FarCompat.mode(CustomNpcs.RwFarShaders) != FarCompat.DRAW) {
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
            finish(t0, 0, 0, 0, unbaked, 0, 0.0, 0, 0, fallback);
            L2.clear();
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
                HALF.add(new Object[] {ae, new double[] {dx, y0 - cy, y1 - cy, dz, FarInterp.yawOf(e[3]), light}});
                continue;
            }
            if (!begun) {
                b.begin(GL11.GL_QUADS, FORMAT);
                begun = true;
            }
            quad(b, atlas, ae, FarInterp.yawOf(e[3]), dx, y0 - cy, y1 - cy, dz, light, 255);
        }
        // M5-V.6: L2 - sledeni NPC-ji s preklicanim vanilla izrisom (frustum je preveril vanilla) in tisti, ki jih
        // vanilla zaradi dometa upodobitve ne ponudi (lasten frustum, do konca megle).
        int l2 = 0;
        int l2Range = 0;
        if (CustomNpcs.RwFarView >= 2) {
            for (EntityNPCInterface npc : L2) {
                ImpostorAtlas.Entry ae = atlas.get(L2_STATE.get(npc.getEntityId())[0]);
                if (ae != null) {
                    begun = l2Quad(b, atlas, ae, npc, pt, cx, cy, cz, begun);
                    ++l2;
                }
            }
            double fogEnd = mc.gameSettings.renderDistanceChunks * 16.0;
            for (Entity en : world.loadedEntityList) {
                if (!(en instanceof EntityNPCInterface) || en.isInRangeToRender3d(cx, cy, cz)) {
                    continue;
                }
                double ex = en.lastTickPosX + (en.posX - en.lastTickPosX) * pt - cx;
                double ez = en.lastTickPosZ + (en.posZ - en.lastTickPosZ) * pt - cz;
                if (ex * ex + ez * ez > fogEnd * fogEnd) {
                    continue;
                }
                EntityNPCInterface npc = (EntityNPCInterface) en;
                ImpostorAtlas.Entry ae = l2(mc, npc, 0.0, true);
                if (ae == null) {
                    continue;
                }
                double ey = en.lastTickPosY + (en.posY - en.lastTickPosY) * pt;
                double hw = ae.width * 0.5;
                double y0 = ey + ae.bottom;
                double y1 = y0 + ae.height;
                double x = ex + cx;
                double z = ez + cz;
                boolean in = frustum != null ? frustum.isBoxInFrustum(x - hw, y0, z - hw, x + hw, y1, z + hw)
                        : camera.isBoundingBoxInFrustum(new AxisAlignedBB(x - hw, y0, z - hw, x + hw, y1, z + hw));
                if (in) {
                    begun = l2Quad(b, atlas, ae, npc, pt, cx, cy, cz, begun);
                    ++l2;
                    ++l2Range;
                    long[] sp = special(npc);
                    if (sp != null) {
                        ++sp[SP_RANGE];
                    }
                }
            }
        }
        L2.clear();
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
                    quad(b, atlas, (ImpostorAtlas.Entry) h[0], (float) p[4], p[0], p[1], p[2], p[3], (int) p[5], HALF_ALPHA);
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
        finish(t0, drawn, live, culled, unbaked, HALF.size(), maxDist, l2, l2Range, fallback);
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

    /**
     * L2: ploskev zive entitete (interpoliran polozaj in yaw telesa, svetloba kot vanilla
     * {@code getBrightnessForRender}); pol-vidni gredo v {@link #HALF}. Vrne, ali je glavni medpomnilnik zacet.
     */
    private static boolean l2Quad(BufferBuilder b, ImpostorAtlas atlas, ImpostorAtlas.Entry ae, EntityNPCInterface npc,
            float pt, double cx, double cy, double cz, boolean begun) {
        double dx = npc.lastTickPosX + (npc.posX - npc.lastTickPosX) * pt - cx;
        double y = npc.lastTickPosY + (npc.posY - npc.lastTickPosY) * pt - cy;
        double dz = npc.lastTickPosZ + (npc.posZ - npc.lastTickPosZ) * pt - cz;
        float yaw = npc.prevRenderYawOffset + MathHelper.wrapDegrees(npc.renderYawOffset - npc.prevRenderYawOffset) * pt;
        int light = npc.getBrightnessForRender();
        double y0 = y + ae.bottom;
        double y1 = y0 + ae.height;
        if (npc.display.getVisible() == 2) {
            long[] sp = special(npc);
            if (sp != null) {
                ++sp[SP_HALF];
            }
            HALF.add(new Object[] {ae, new double[] {dx, y0, y1, dz, yaw, light}});
            return begun;
        }
        if (!begun) {
            b.begin(GL11.GL_QUADS, FORMAT);
        }
        quad(b, atlas, ae, yaw, dx, y0, y1, dz, light, 255);
        return true;
    }

    /** Ena ploskev: oglisca zgoraj levo, spodaj levo, spodaj desno, zgoraj desno (kot {@code ImpostorCheck}). */
    private static void quad(BufferBuilder b, ImpostorAtlas atlas, ImpostorAtlas.Entry ae, float npcYaw,
            double dx, double y0, double y1, double dz, int light, int alpha) {
        float[] r = FarInterp.right(dx, dz);
        int k = ImpostorFrames.frameFor(ImpostorFrames.yawOf(dx, dz), npcYaw);
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

    private static void finish(long t0, int drawn, int live, int culled, int unbaked, int half, double maxDist,
            int l2, int l2Range, boolean fallback) {
        lastDrawn = drawn;
        lastL2 = l2;
        lastL2Range = l2Range;
        statL2Min = Math.min(statL2Min, l2);
        statL2Max = Math.max(statL2Max, l2);
        statL2RangeMax = Math.max(statL2RangeMax, l2Range);
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
                        + " izris: slicic=%d usPovp=%.1f usMax=%.1f"
                        + " L2: start=%d zadnja=%d izvenDometa=%d preklicev=%d brezPeke=%d videzov=%d napak=%d",
                CustomNpcs.RwFarView, FarClient.size(), ImpostorBaker.current() == null ? 0 : ImpostorBaker.current().size,
                ImpostorBaker.inAtlas(), ImpostorBaker.baked, ImpostorBaker.failures,
                RenderEntitiesPatch.patched() ? 1 : 0, hookFrames, fallbackFrames,
                lastDrawn, lastLive, lastCulled, lastUnbaked, lastHalf, lastMaxDist,
                statFrames, statFrames == 0 ? 0.0 : statNs / 1000.0 / statFrames, statNsMax / 1000.0,
                ImpostorL2.start(CustomNpcs.RwImpostorStart), lastL2, lastL2Range, statL2Cancelled, statL2Unbaked, l2Hashes, l2HashErrors)
                + " " + FarCompat.status(CustomNpcs.RwFarShaders);
    }
}
