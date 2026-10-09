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
 *   <li>3 = M5.11 (S14b): original, posamezne ocene tipa vozlisca pomnjene po poziciji za cas klica</li>
 *   <li>4 = preverba nacina 3 (kot 2)</li>
 * </ul>
 * Stevci {@code klicev/branj/iskanjChunka} v nacinih 3 in 4 pomenijo klice, ocene tipa vozlisca
 * in dejanske izracune (zgresitve pomnilnika).
 * Stanje bere samo strezniska nit.
 *
 * <p>M5.10: neodvisno od nacina lahko steje cas in kandidate sledenja poti ({@link #setTiming}).
 * Merjenje je privzeto izklopljeno; vklopi ga ukaz {@code /rwpath cas 1} (scenarij meritve).
 *
 * <p>M5.11: merjenje steje tudi loceno po nacinu (nacin ob zacetku pathFollow oz. klica) in
 * ticke v vsakem nacinu ({@link #switchMode}), da se nacina primerjata v istem boju z
 * izmenjujocimi se okni.
 */
public final class PathFollowCache {
    public static final int ORIGINAL = 0;
    public static final int MEMO = 1;
    public static final int VERIFY = 2;
    public static final int NODE_MEMO = 3;
    public static final int VERIFY_NODE = 4;
    /** Stevilo nacinov (velikost tabel po nacinu). */
    public static final int MODES = 5;

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
    // M5.11: isto po nacinu
    static final long[] followCallsBy = new long[MODES];
    static final long[] followNanosBy = new long[MODES];
    static final long[] directCallsBy = new long[MODES];
    static final long[] directNanosBy = new long[MODES];
    static final long[] ticksBy = new long[MODES];
    private static int windowStartTick;

    private PathFollowCache() {
    }

    public static boolean isValidMode(int m) {
        return m >= ORIGINAL && m < MODES;
    }

    public static void setMode(int m) {
        mode = isValidMode(m) ? m : ORIGINAL;
    }

    /**
     * M5.11: preklop med tekom; ticki od zadnjega preklopa ali {@link #reset(int)} se pristejejo
     * dosedanjemu nacinu.
     */
    public static void switchMode(int m, int tick) {
        ticksBy[mode] += tick - windowStartTick;
        windowStartTick = tick;
        setMode(m);
    }

    /** Ticki v nacinu m od reset (vkljucno s tekocim oknom, ce je m trenutni nacin). */
    public static long ticksIn(int m, int tick) {
        return ticksBy[m] + (m == mode ? tick - windowStartTick : 0);
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
            case NODE_MEMO:
                return "pomnjenje tipa vozlisca";
            case VERIFY_NODE:
                return "preverba: original in pomnjenje tipa vozlisca";
            default:
                return "original";
        }
    }

    /** Reset stevcev; tekoce okno nacina se zacne pri ticku tick. */
    public static void reset(int tick) {
        reset();
        windowStartTick = tick;
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
        Arrays.fill(followCallsBy, 0L);
        Arrays.fill(followNanosBy, 0L);
        Arrays.fill(directCallsBy, 0L);
        Arrays.fill(directNanosBy, 0L);
        Arrays.fill(ticksBy, 0L);
        windowStartTick = 0;
    }

    public static void setTiming(boolean on) {
        timing = on;
    }

    public static boolean timing() {
        return timing;
    }

    /** En pathFollow: trajanje (z vsemi kandidati) in stevilo klicev isDirectPathBetweenPoints. */
    static void recordFollow(int m, long nanos, int candidates) {
        ++followCallsBy[m];
        followNanosBy[m] += nanos;
        ++followCalls;
        followNanos += nanos;
        ++candidateHist[Math.min(Math.max(candidates, 0), HIST - 1)];
    }

    /** En klic isDirectPathBetweenPoints (kandidat) v katerem koli nacinu. */
    static void recordDirect(int m, long nanos, boolean result) {
        ++directCallsBy[m];
        directNanosBy[m] += nanos;
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

    /**
     * M5.11: stevci po nacinu kot "m:tickov:sledenj:sledenjNs:kandidatov:kandidatNs" za vsak
     * nacin z vsaj enim tickom ali klicem, loceno s ';' (prazno, ce ni nicesar).
     */
    public static String perMode(int tick) {
        StringBuilder sb = new StringBuilder();
        for (int m = 0; m < MODES; ++m) {
            long t = ticksIn(m, tick);
            if (t == 0 && followCallsBy[m] == 0 && directCallsBy[m] == 0) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(m).append(':').append(t).append(':').append(followCallsBy[m]).append(':').append(followNanosBy[m])
                    .append(':').append(directCallsBy[m]).append(':').append(directNanosBy[m]);
        }
        return sb.toString();
    }

    /** Klici isDirectPathBetweenPoints v nacinih 1-4. */
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
