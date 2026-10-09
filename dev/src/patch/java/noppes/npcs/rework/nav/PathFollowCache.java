package noppes.npcs.rework.nav;

import java.util.Arrays;

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
 *
 * <p>M5.10: neodvisno od nacina lahko steje cas in kandidate sledenja poti ({@link #setTiming}).
 * Merjenje je privzeto izklopljeno; vklopi ga ukaz {@code /rwpath cas 1} (scenarij meritve).
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

    /** M5.10: stevilo predalov histograma kandidatov na en pathFollow (zadnji = HIST-1 ali vec). */
    public static final int HIST = 6;
    private static boolean timing;
    static long followCalls;
    static long followNanos;
    static long directCalls;
    static long directNanos;
    static long directTrue;
    static final long[] candidateHist = new long[HIST];

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
        followCalls = 0;
        followNanos = 0;
        directCalls = 0;
        directNanos = 0;
        directTrue = 0;
        Arrays.fill(candidateHist, 0L);
    }

    public static void setTiming(boolean on) {
        timing = on;
    }

    public static boolean timing() {
        return timing;
    }

    /** En pathFollow: trajanje (z vsemi kandidati) in stevilo klicev isDirectPathBetweenPoints. */
    static void recordFollow(long nanos, int candidates) {
        ++followCalls;
        followNanos += nanos;
        ++candidateHist[Math.min(Math.max(candidates, 0), HIST - 1)];
    }

    /** En klic isDirectPathBetweenPoints (kandidat) v katerem koli nacinu. */
    static void recordDirect(long nanos, boolean result) {
        ++directCalls;
        directNanos += nanos;
        if (result) {
            ++directTrue;
        }
    }

    /** Merjeni klici pathFollow (samo pri vklopljenem merjenju). */
    public static long followCalls() {
        return followCalls;
    }

    public static long followNanos() {
        return followNanos;
    }

    /** Merjeni klici isDirectPathBetweenPoints (kandidati) v katerem koli nacinu. */
    public static long directCalls() {
        return directCalls;
    }

    public static long directNanos() {
        return directNanos;
    }

    /** Kandidati, do katerih je bila prosta pot (pathFollow se pri prvem ustavi). */
    public static long directTrue() {
        return directTrue;
    }

    /** Histogram kandidatov na pathFollow kot "a/b/c/..." (predal i = i kandidatov, zadnji = HIST-1 ali vec). */
    public static String candidateHistogram() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < HIST; ++i) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(candidateHist[i]);
        }
        return sb.toString();
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
