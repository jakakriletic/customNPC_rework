package noppes.npcs.rework.net;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.RwWorldAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import noppes.npcs.CustomNpcs;
import noppes.npcs.LogWriter;
import noppes.npcs.Server;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.CustomNPCsScheduler;

/**
 * M5.13 (S7): prejemniki paketa utripa oci po {@code world.playerEntities} namesto po chunkih.
 *
 * <p>Original ({@code Server.sendAssociatedData}, CNPC 01Oct19) poisce prejemnike z
 * {@code world.getEntitiesWithinAABB(EntityPlayerMP.class, npcBox.grow(160, 160, 160))}. Vanilla
 * (1.12.2, Forge 14.23.5.2847) to naredi tako, da pregleda vsak nalozen chunk v obsegu kvadra
 * (~441 chunkov) in v njem vsak y-rez, nato pa obdrzi entitete, ki so razreda
 * {@code EntityPlayerMP}, prestanejo {@code EntitySelectors.NOT_SPECTATING} in katerih skatla
 * seka kvader. Utrip se zgodi 1/140 na tick na NPC-ja; pri 500 NPC-jih je to ~3,6 pometanj
 * 441 chunkov na tick in po profilu JFR 8. 10. 6,0-6,5 % CPU strezniske niti [M].
 *
 * <p>Zakaj je iskanje po {@code world.playerEntities} enako (CNPC 01Oct19, vanilla 1.12.2) [K]:
 * <ul>
 *   <li>{@code getEntitiesWithinAABB(Class, AABB)} uporabi predikat
 *       {@code EntitySelectors.NOT_SPECTATING}, ki je za igralca {@code !isSpectator()}
 *       (brez preverbe zivosti);</li>
 *   <li>iz chunka pride entiteta samo, ce je v njegovem seznamu ({@code addedToChunk}) in ce je
 *       chunk nalozen ({@code World.isChunkLoaded(x, z, true)}) - oboje preveri tudi
 *       {@link #scan};</li>
 *   <li>obseg pregledanih chunkov in y-rezov je konservativen: chunk ali rez izven obsega ne more
 *       vsebovati entitete, katere skatla seka kvader, dokler je polmer entitete pod
 *       {@code World.MAX_ENTITY_RADIUS} (igralec je sirok 0,6). Meje so preverjene nakljucno v
 *       {@code AssociatedPlayersTest};</li>
 *   <li>{@code world.playerEntities} vsebuje vse igralce sveta, zato nobenega ne izpusti; za
 *       nalozenost chunka in sekanje poskrbita gornji preverbi. Na strezniku so to
 *       {@code EntityPlayerMP} (tudi Forgeov {@code FakePlayer}, ki pa ni v svetu in ga
 *       {@code addedToChunk} izloci - tako kot ga izloci original).</li>
 * </ul>
 * Vrstni red seznama se razlikuje (po igralcih namesto po chunkih). Vsak prejemnik dobi svoj
 * paket z isto vsebino, zato vrstni red ni opazen.
 *
 * <ul>
 *   <li>0 = original ({@code Server.sendAssociatedData})</li>
 *   <li>1 = S7: prejemniki po {@code world.playerEntities}</li>
 *   <li>2 = preverba: oboje, poslje po seznamu originala, steje neujemanja (dvojna cena)</li>
 * </ul>
 * Privzeto 1 od 10. 10. 2026 (D-032): iskanje je 55,42 -> 0,12 us, idle-500 MSPT povp 2,824 -> 2,570 ms,
 * p95 4,325 -> 3,932, locitev drzi nad sumom. Stanje bere samo strezniska nit.
 */
public final class AssociatedPlayers {
    public static final int ORIGINAL = 0;
    public static final int SCAN = 1;
    public static final int VERIFY = 2;
    /** Stevilo nacinov (velikost tabel po nacinu). */
    public static final int MODES = 3;

    /** Polmer kvadra utripa oci v originalu. */
    public static final double BLINK_RANGE = 160.0;

    private static int mode = ORIGINAL;
    private static boolean timing;

    static long calls;
    static long players;
    static long recipients;
    static long cells;
    static long compared;
    static long mismatches;

    static final long[] callsBy = new long[MODES];
    static final long[] nanosBy = new long[MODES];
    static final long[] recipientsBy = new long[MODES];
    static final long[] ticksBy = new long[MODES];
    private static int windowStartTick;

    private AssociatedPlayers() {
    }

    public static boolean isValidMode(int m) {
        return m >= ORIGINAL && m < MODES;
    }

    public static void setMode(int m) {
        mode = isValidMode(m) ? m : ORIGINAL;
    }

    public static int mode() {
        return mode;
    }

    public static String describe(int m) {
        switch (m) {
            case SCAN:
                return "prejemniki po playerEntities";
            case VERIFY:
                return "preverba: original in playerEntities";
            default:
                return "original";
        }
    }

    /** Merjenje ns okoli iskanja prejemnikov (dva klica nanoTime na iskanje). */
    public static void setTiming(boolean on) {
        timing = on;
    }

    public static boolean timing() {
        return timing;
    }

    /**
     * A/B v istem zagonu: preklop nacina med tekom. Ticki od zadnjega preklopa ali
     * {@link #reset(int)} se pristejejo dosedanjemu nacinu.
     */
    public static void switchMode(int m, int tick) {
        ticksBy[mode] += tick - windowStartTick;
        windowStartTick = tick;
        setMode(m);
    }

    /** Ticki v nacinu m od reseta (vkljucno s tekocim oknom, ce je m trenutni nacin). */
    public static long ticksIn(int m, int tick) {
        return ticksBy[m] + (m == mode ? tick - windowStartTick : 0);
    }

    public static void reset(int tick) {
        reset();
        windowStartTick = tick;
    }

    public static void reset() {
        calls = 0;
        players = 0;
        recipients = 0;
        cells = 0;
        compared = 0;
        mismatches = 0;
        Arrays.fill(callsBy, 0L);
        Arrays.fill(nanosBy, 0L);
        Arrays.fill(recipientsBy, 0L);
        Arrays.fill(ticksBy, 0L);
        windowStartTick = 0;
    }

    /**
     * Enakovredno {@code Server.sendAssociatedData}, le da prejemnike v nacinih 1-2 poisce po
     * {@code world.playerEntities}. Telo posiljanja je nespremenjeno prepisano iz originala.
     *
     * <p>Brez merjenja ({@link #setTiming}) nacin 0 poklice original neposredno. Z vklopljenim
     * merjenjem gre tudi nacin 0 skozi to telo, da je merjeni odsek v obeh nacinih natanko
     * iskanje prejemnikov (A/B v istem zagonu, M5.11). Poizvedba in posiljanje sta enaka.
     */
    public static void sendAssociatedData(Entity entity, EnumPacketClient type, Object... obs) {
        int m = mode;
        if (m == ORIGINAL && !timing) {
            Server.sendAssociatedData(entity, type, obs);
            return;
        }
        AxisAlignedBB box = entity.getEntityBoundingBox().grow(BLINK_RANGE, BLINK_RANGE, BLINK_RANGE);
        World world = entity.world;
        long t0 = timing ? System.nanoTime() : 0L;
        List<EntityPlayerMP> list = m == ORIGINAL
                ? world.<EntityPlayerMP>getEntitiesWithinAABB(EntityPlayerMP.class, box)
                : scan(world, box);
        if (timing) {
            nanosBy[m] += System.nanoTime() - t0;
        }
        ++calls;
        ++callsBy[m];
        players += world.playerEntities.size();
        recipients += list.size();
        recipientsBy[m] += list.size();
        cells += chunkCells(box);
        if (m == VERIFY) {
            List<EntityPlayerMP> original = world.getEntitiesWithinAABB(EntityPlayerMP.class, box);
            ++compared;
            mismatches += mismatch(ids(original), ids(list));
            list = original;
        }
        if (list.isEmpty()) {
            return;
        }
        final List<EntityPlayerMP> targets = list;
        CustomNPCsScheduler.runTack(() -> {
            ByteBuf buffer = Unpooled.buffer();
            try {
                if (!Server.fillBuffer(buffer, type, obs)) {
                    return;
                }
                LogWriter.debug("SendAssociatedData: " + type);
                for (EntityPlayerMP player : targets) {
                    CustomNpcs.Channel.sendTo(new FMLProxyPacket(new PacketBuffer(buffer.copy()), "CustomNPCs"), player);
                }
            } catch (IOException e) {
                LogWriter.error(type + " Errored", e);
            } finally {
                buffer.release();
            }
        });
    }

    /** Prejemniki po {@code world.playerEntities} z istim izborom kot vanilla poizvedba. */
    static List<EntityPlayerMP> scan(World world, AxisAlignedBB box) {
        return scanList(world, world.playerEntities, EntityPlayerMP.class, box);
    }

    /**
     * Izid, enak {@code world.getEntitiesWithinAABB(clazz, box)}, z obhodom seznama {@code source}
     * namesto chunkov. Ista koda stori izbor za prejemnike (vir {@code world.playerEntities}) in
     * za preverbo v svetu ({@link #probe}, vir {@code world.loadedEntityList}).
     */
    static <T extends Entity> List<T> scanList(World world, List<? extends Entity> source, Class<T> clazz,
            AxisAlignedBB box) {
        List<T> out = new ArrayList<T>();
        for (int i = 0; i < source.size(); ++i) {
            Entity entity = source.get(i);
            if (!clazz.isInstance(entity)) {
                continue;
            }
            // Vrstni red preverb je od najcenejse naprej; izid je konjunkcija (accepts).
            boolean spectator = entity instanceof EntityPlayer && ((EntityPlayer) entity).isSpectator();
            boolean intersects = !spectator && entity.getEntityBoundingBox().intersects(box);
            boolean inLoadedChunk = intersects && entity.addedToChunk
                    && RwWorldAccess.isChunkLoaded(world, entity.chunkCoordX, entity.chunkCoordZ, true);
            if (accepts(true, spectator, inLoadedChunk, intersects)) {
                out.add(clazz.cast(entity));
            }
        }
        return out;
    }

    /**
     * Preverba enakosti v svetu brez posiljanja paketov: za {@code samples} NPC-jev primerja
     * vanilla poizvedbo in {@link #scanList} na petih kvadrih (polmer 1, 16 in 160 blokov ter 160
     * blokov premaknjeno po y za +-200, kar preveri rezanje y-rezov). Steje v {@link #compared()}
     * in {@link #mismatches()}.
     *
     * <p>Razred poizvedbe je {@code EntityNPCInterface} in ne {@code EntityPlayerMP}, ker na
     * dediciranem strezniku scenarija ni igralcev. Izbor chunkov, y-rezov in geometrija v vanilli
     * niso odvisni od razreda, zato je to preverba iste trditve na stotinah entitet; predikata
     * {@code isSpectator} ta pot ne doseze (velja le za igralce in je v {@link #scanList} prepisan
     * iz {@code EntitySelectors.NOT_SPECTATING}).
     *
     * @return stevilo primerjav
     */
    public static long probe(World world, int samples) {
        double[] radii = {1.0, 16.0, BLINK_RANGE, BLINK_RANGE, BLINK_RANGE};
        double[] yShift = {0.0, 0.0, 0.0, 200.0, -200.0};
        long done = 0;
        int taken = 0;
        List<Entity> all = world.loadedEntityList;
        for (int i = 0; i < all.size() && taken < samples; ++i) {
            Entity entity = all.get(i);
            if (!(entity instanceof EntityNPCInterface)) {
                continue;
            }
            ++taken;
            for (int k = 0; k < radii.length; ++k) {
                AxisAlignedBB box = entity.getEntityBoundingBox().grow(radii[k], radii[k], radii[k])
                        .offset(0.0, yShift[k], 0.0);
                List<EntityNPCInterface> original =
                        world.getEntitiesWithinAABB(EntityNPCInterface.class, box);
                List<EntityNPCInterface> scanned =
                        scanList(world, all, EntityNPCInterface.class, box);
                ++compared;
                ++done;
                mismatches += mismatch(entityIds(original), entityIds(scanned));
            }
        }
        return done;
    }

    /**
     * Cisti pogoj, pod katerim je igralec v izidu vanilla poizvedbe (testirano brez sveta).
     *
     * @param playerMp entiteta je {@code EntityPlayerMP} (razred poizvedbe)
     * @param spectator {@code isSpectator()} - edina preverba v {@code NOT_SPECTATING} za igralca
     * @param inLoadedChunk je v seznamu entitet nalozenega chunka ({@code addedToChunk})
     * @param intersects skatla igralca seka kvader
     */
    public static boolean accepts(boolean playerMp, boolean spectator, boolean inLoadedChunk, boolean intersects) {
        return playerMp && !spectator && inLoadedChunk && intersects;
    }

    /**
     * Koliko chunkov pregleda vanilla poizvedba za ta kvader (meje iz
     * {@code World.getEntitiesWithinAABB}). Samo za zapis meritve.
     */
    public static long chunkCells(AxisAlignedBB box) {
        return chunkCells(box.minX, box.maxX, box.minZ, box.maxZ, World.MAX_ENTITY_RADIUS);
    }

    /** Glej {@link #chunkCells(AxisAlignedBB)}; locena oblika za test brez sveta. */
    public static long chunkCells(double minX, double maxX, double minZ, double maxZ, double radius) {
        int x0 = MathHelper.floor((minX - radius) / 16.0);
        int x1 = MathHelper.ceil((maxX + radius) / 16.0);
        int z0 = MathHelper.floor((minZ - radius) / 16.0);
        int z1 = MathHelper.ceil((maxZ + radius) / 16.0);
        return (long) Math.max(0, x1 - x0) * Math.max(0, z1 - z0);
    }

    static int[] ids(List<EntityPlayerMP> list) {
        return entityIds(list);
    }

    static int[] entityIds(List<? extends Entity> list) {
        int[] out = new int[list.size()];
        for (int i = 0; i < out.length; ++i) {
            out[i] = list.get(i).getEntityId();
        }
        return out;
    }

    /** Stevilo id-jev, ki so samo v enem od seznamov (vrstni red ni neujemanje). */
    public static int mismatch(int[] a, int[] b) {
        return missing(a, b) + missing(b, a);
    }

    private static int missing(int[] a, int[] b) {
        int n = 0;
        for (int i = 0; i < a.length; ++i) {
            boolean found = false;
            for (int j = 0; j < b.length; ++j) {
                if (b[j] == a[i]) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                ++n;
            }
        }
        return n;
    }

    /** Iskanja prejemnikov v nacinih 1-2. */
    public static long calls() {
        return calls;
    }

    /** Pregledani vnosi {@code world.playerEntities} (vsota po iskanjih). */
    public static long players() {
        return players;
    }

    /** Najdeni prejemniki (vsota po iskanjih). */
    public static long recipients() {
        return recipients;
    }

    /** Chunki, ki bi jih pregledala vanilla poizvedba (vsota po iskanjih). */
    public static long cells() {
        return cells;
    }

    public static long compared() {
        return compared;
    }

    public static long mismatches() {
        return mismatches;
    }

    /**
     * Stevci po nacinu kot "m:tickov:iskanj:ns:prejemnikov" za vsak nacin z vsaj enim tickom ali
     * iskanjem, loceno s podpicjem (prazno, ce ni nicesar).
     */
    public static String perMode(int tick) {
        StringBuilder sb = new StringBuilder();
        for (int m = 0; m < MODES; ++m) {
            long t = ticksIn(m, tick);
            if (t == 0 && callsBy[m] == 0) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(m).append(':').append(t).append(':').append(callsBy[m]).append(':').append(nanosBy[m])
                    .append(':').append(recipientsBy[m]);
        }
        return sb.toString();
    }
}
