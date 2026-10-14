package noppes.npcs.rework.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

/**
 * M5.16 (D-031): {@code World.getCollisionBoxes(Entity, AABB)} preskoci poizvedbo entitet, kadar v
 * svetu ni nikogar, ki bi lahko prispeval trkalno skatlo.
 *
 * <p>Original za vsak premik vsake entitete poisce entitete v kvadru in za vsako prebere
 * {@code getCollisionBoundingBox()} ter {@code entityIn.getCollisionBox(entity)}. Oba v
 * {@code Entity} vracata {@code null} (Entity:1272 in :2244), zato prispevajo skatlo **samo** razredi,
 * ki eno od njiju prepisejo (colon, minecart, shulker, modni razredi). V profilu JFR (8. 10.) je
 * {@code Entity.move} 26-29 % CPU strezniske niti idle-500, od tega poizvedba entitet za trke
 * **11,8 %** **[M]**.
 *
 * <p>Zato je preskok enak originalu, kadar [K]:
 * <ul>
 *   <li>v svetu ni **nobene** entitete, katere razred prepisuje {@code getCollisionBoundingBox()}
 *       ali {@code getCollisionBox(Entity)} (steje {@link CollisionWatcher}, konzervativno: razred se
 *       steje tudi, ce metoda vcasih vrne {@code null}), <b>in</b></li>
 *   <li>razred premikajoce se entitete ne prepisuje {@code getCollisionBox(Entity)}.</li>
 * </ul>
 * Takrat zanka originala ne bi dodala nobene skatle, {@code isRidingSameEntity},
 * {@code getCollisionBoundingBox} in {@code getCollisionBox} pa v vanilli nimajo stranskih ucinkov.
 * {@code GetCollisionBoxesEvent} ostane nedotaknjen, ker je izven zanke.
 *
 * <p>Svet, ki ga {@link CollisionWatcher} ne spremlja (se ni dobil {@code WorldEvent.Load}), ni
 * nikoli preskocen.
 *
 * <ul>
 *   <li>0 = original</li>
 *   <li>1 = preskok, kadar je dokazano brez prispevka</li>
 *   <li>2 = preverba: poizvedba tece, a se presteje, ali bi kdo res prispeval skatlo</li>
 * </ul>
 */
public final class CollisionGate {
    public static final int ORIGINAL = 0;
    public static final int ON = 1;
    public static final int VERIFY = 2;
    public static final int MODES = 3;

    /** Izidi {@link #decide}: poizvedba tece, poizvedba se preskoci, poizvedba tece in se preveri. */
    public static final int RUN = 0;
    public static final int SKIP_QUERY = 1;
    public static final int RUN_VERIFY = 2;

    /** Sveti, ki jih spremlja {@link CollisionWatcher}; vrednost je stevec prispevajocih entitet. */
    static final Map<World, int[]> COUNTS = new ConcurrentHashMap<World, int[]>();

    private static int mode = ORIGINAL;

    static long calls;
    static long skipped;
    static long compared;
    static long mismatches;

    // Enorezni predpomnilnik sveta, loceno po strani: isto svetovno zanko tika ena nit.
    private static World lastServerWorld;
    private static int[] lastServerCount;
    private static World lastClientWorld;
    private static int[] lastClientCount;

    private CollisionGate() {
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
            case ON:
                return "preskok poizvedbe, kadar nihce ne prispeva skatle";
            case VERIFY:
                return "preverba: poizvedba tece, steje se prispevek";
            default:
                return "original";
        }
    }

    public static void reset() {
        calls = 0;
        skipped = 0;
        compared = 0;
        mismatches = 0;
    }

    /**
     * Klicna tocka iz popravljenega {@code World.getCollisionBoxes}: nadomesti
     * {@code world.getEntitiesWithinAABBExcludingEntity(entity, box)}.
     */
    public static List<Entity> entitiesForCollision(World world, Entity entity, AxisAlignedBB box) {
        int m = mode;
        // Brez entitete original poizvedbe sploh ne pozene (klic je v veji entityIn != null), zato
        // tu ni poti do preskoka; ce bi vseeno prisla, tece original.
        if (m == ORIGINAL || entity == null) {
            return world.getEntitiesWithinAABBExcludingEntity(entity, box);
        }
        ++calls;
        int d = decide(m, contributorsFor(world),
                CollisionClasses.overridesGetCollisionBox(entity.getClass()));
        if (d == SKIP_QUERY) {
            ++skipped;
            return Collections.<Entity>emptyList();
        }
        List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(entity, box);
        if (d == RUN_VERIFY) {
            verify(entity, list);
        }
        return list;
    }

    /**
     * Odlocitev brez stanja (zato testljiva): poizvedba se preskoci samo v nacinu 1, kadar v svetu
     * ni nobene prispevajoce entitete <b>in</b> premikajoca se entiteta ne prepisuje
     * {@code getCollisionBox(Entity)}. Nespremljan svet ima {@code contributors} = -1 in ni nikoli
     * preskocen.
     */
    public static int decide(int m, int contributors, boolean overridesBox) {
        if (m != ON && m != VERIFY) {
            return RUN;
        }
        if (contributors != 0 || overridesBox) {
            return RUN;
        }
        return m == ON ? SKIP_QUERY : RUN_VERIFY;
    }

    /** Stevec prispevajocih entitet tega sveta ali -1, kadar svet ni spremljan (brez entitete tudi). */
    private static int contributorsFor(World world) {
        int[] count = cachedCount(world);
        return count == null ? -1 : count[0];
    }

    private static int[] cachedCount(World world) {
        if (world == null) {
            return null;
        }
        if (world.isRemote) {
            if (world == lastClientWorld) {
                return lastClientCount;
            }
            int[] c = COUNTS.get(world);
            lastClientWorld = world;
            lastClientCount = c;
            return c;
        }
        if (world == lastServerWorld) {
            return lastServerCount;
        }
        int[] c = COUNTS.get(world);
        lastServerWorld = world;
        lastServerCount = c;
        return c;
    }

    /** Nacin 2: ali bi kdo od najdenih res prispeval skatlo (ista pogoja kot zanka originala). */
    private static void verify(Entity entity, List<Entity> list) {
        for (int i = 0; i < list.size(); ++i) {
            Entity other = list.get(i);
            if (entity.isRidingSameEntity(other)) {
                continue;
            }
            ++compared;
            if (other.getCollisionBoundingBox() != null || entity.getCollisionBox(other) != null) {
                ++mismatches;
            }
        }
    }

    /**
     * Pozabi enorezni predpomnilnik sveta. Klice se ob razlozitvi sveta: brez tega bi vrata se
     * naprej brala stevec razlozenega sveta (in ga drzala v pomnilniku).
     */
    static void forgetCache() {
        lastServerWorld = null;
        lastServerCount = null;
        lastClientWorld = null;
        lastClientCount = null;
    }

    /** Stevec prispevajocih entitet v tem svetu ali -1, ce svet ni spremljan. */
    public static int contributors(World world) {
        int[] c = world == null ? null : COUNTS.get(world);
        return c == null ? -1 : c[0];
    }

    /** Spremljani sveti. */
    public static int watchedWorlds() {
        return COUNTS.size();
    }

    public static long calls() {
        return calls;
    }

    public static long skipped() {
        return skipped;
    }

    public static long compared() {
        return compared;
    }

    public static long mismatches() {
        return mismatches;
    }

    /** Vsota prispevajocih entitet po vseh spremljanih svetovih (za izpis ukaza). */
    public static int contributorsAll() {
        int sum = 0;
        for (int[] c : COUNTS.values()) {
            sum += c[0];
        }
        return sum;
    }
}
