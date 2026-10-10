package noppes.npcs.rework.nav;

/**
 * M5.17 (S14c): pomnjenje ocene tipa vozlisca za cas **celega iskanja poti**.
 *
 * <p>S14b (M5.11) pomni ocene samo med enim {@code isDirectPathBetweenPoints} (sledenje poti). Samo
 * iskanje poti ({@code PathFinder.findPath}) pa iste pozicije prav tako ocenjuje veckrat: vsako
 * vozlisce preveri svoje sosede, sosedje sosedov pa se prekrivajo. M5.9 je izmerila, da je v
 * prizoriscu nedosegljive tarce iskanje **51 % ticka** in **190,5 us na klic**, ker A* porabi ves
 * proracun 200 vozlisc **[M]** — to je tarca tega paketa.
 *
 * <p>Zakaj je izid enak originalu (ista razlaga kot S14b, Forge 14.23.5.2847) **[K]**:
 * {@link RwWalkNodeProcessor#getPathNodeType(net.minecraft.world.IBlockAccess, int, int, int)} je
 * odvisna od sveta, pozicije in polja {@code currentEntity}; med enim {@code findPath} je
 * {@code blockaccess} ista instanca {@code ChunkCache}, entiteta je ena sama, iskanje tece
 * sinhrono na strezniski niti in blokov v tem casu nihce ne postavlja. Pomnilnik se zato odpre ob
 * zacetku iskanja in zapre ob koncu.
 *
 * <ul>
 *   <li>0 = original</li>
 *   <li>1 = pomnjenje za cas enega iskanja poti</li>
 *   <li>2 = preverba: vsaka ocena se izracuna in primerja s pomnjeno (brez prihranka)</li>
 * </ul>
 * Privzeto 1 od 10. 10. 2026 (D-032): 239,5 M primerjav ocen brez neujemanja; v prizoriscu
 * nedosegljive tarce 184,0 -> 144,9 us na iskanje in MSPT povp 17,959 -> 15,253 ms (-15 %), v boju
 * nevtralno. Stanje bere samo strezniska nit.
 */
public final class PathSearchMemo {
    public static final int ORIGINAL = 0;
    public static final int ON = 1;
    public static final int VERIFY = 2;
    public static final int MODES = 3;

    /** Zgornja meja rez pomnilnika enega navigatorja (potenca 2). */
    public static final int MAX_SLOTS = 1024;

    private static int mode = ORIGINAL;

    static long searches;
    static long lookups;
    static long misses;
    static long mismatches;

    private PathSearchMemo() {
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
                return "pomnjenje tipa vozlisca med iskanjem poti";
            case VERIFY:
                return "preverba: ocene se izracunajo in primerjajo";
            default:
                return "original";
        }
    }

    public static void reset() {
        searches = 0;
        lookups = 0;
        misses = 0;
        mismatches = 0;
    }

    /** Iskanja poti, ki so tekla s pomnilnikom (nacina 1-2). */
    public static long searches() {
        return searches;
    }

    /** Ocene tipa vozlisca v teh iskanjih. */
    public static long lookups() {
        return lookups;
    }

    /** Dejanski izracuni (zgresitve pomnilnika). */
    public static long misses() {
        return misses;
    }

    /** Nacin 2: ocene, kjer se pomnjena vrednost ni ujemala z izracunano. */
    public static long mismatches() {
        return mismatches;
    }

    /** Delez ocen, ki jih je pomnilnik odgovoril brez izracuna (0, ce ocen ni bilo). */
    public static double hitRatio() {
        return lookups == 0 ? 0.0 : (double) (lookups - misses) / (double) lookups;
    }
}
