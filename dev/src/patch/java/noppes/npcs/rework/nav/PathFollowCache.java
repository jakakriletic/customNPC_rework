package noppes.npcs.rework.nav;

/**
 * M5-S S14: stikalo in stevci za sledenje poti brez hash iskanja chunka za vsak blok.
 *
 * <p>Profil JFR 8. 10. (boj-500): 48 % CPU strezniske niti je v vanilla
 * {@code PathNavigate.pathFollow -> PathNavigateGround.isDirectPathBetweenPoints}, od tega 27 % v
 * {@code ChunkProviderServer.getLoadedChunk} za vsak prebrani blok. {@link RwPathNavigateGround}
 * v nacinu {@link #MEMO} bere iste bloke prek {@link MemoBlockAccess}.
 *
 * <ul>
 *   <li>0 = original ({@code super.isDirectPathBetweenPoints})</li>
 *   <li>1 = isti algoritem, bloki prek predpomnilnika chunkov</li>
 *   <li>2 = preverba: oboje, vrne original, steje neujemanja (samo za scenarij, dvojna cena)</li>
 * </ul>
 * Stanje bere samo strezniska nit.
 */
public final class PathFollowCache {
    public static final int ORIGINAL = 0;
    public static final int MEMO = 1;
    public static final int VERIFY = 2;

    private static int mode = ORIGINAL;
    static long calls;
    static long lookups;
    static long misses;
    static long compared;
    static long mismatches;

    private PathFollowCache() {
    }

    public static boolean isValidMode(int m) {
        return m >= ORIGINAL && m <= VERIFY;
    }

    public static void setMode(int m) {
        mode = isValidMode(m) ? m : ORIGINAL;
    }

    public static int mode() {
        return mode;
    }

    public static String describe(int m) {
        switch (m) {
            case MEMO:
                return "predpomnilnik chunkov";
            case VERIFY:
                return "preverba: original in predpomnilnik";
            default:
                return "original";
        }
    }

    public static void reset() {
        calls = 0;
        lookups = 0;
        misses = 0;
        compared = 0;
        mismatches = 0;
    }

    /** Klici isDirectPathBetweenPoints v nacinu 1 ali 2. */
    public static long calls() {
        return calls;
    }

    /** Branja blokov prek predpomnilnika. */
    public static long lookups() {
        return lookups;
    }

    /** Iskanja chunka v svetu (zgresitve predpomnilnika). */
    public static long misses() {
        return misses;
    }

    public static long compared() {
        return compared;
    }

    public static long mismatches() {
        return mismatches;
    }
}
