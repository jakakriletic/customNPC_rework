package noppes.npcs.rework.nav;

/**
 * M5.9: stevci in merjenje **iskanja poti** ({@code PathNavigate.getPathToPos}) kopenskega NPC-ja.
 *
 * <p>Zakaj: M5.2 (proracun iskanj na tick), M5.6 (deljenje poti) in S5 (negativni predpomnilnik)
 * imajo podlago samo, ce iskanje poti kje preseze sum. V obstojecih celicah je po profilu JFR
 * (8. 10.) iskanje poti **2 %** ticka boja, ker je tarca dosegljiva in blizu. Raziskava M5.0
 * napoveduje [H], da je pri **nedosegljivi** tarci drugace: {@code EntityAIAttackTarget.shouldExecute}
 * pozene celo iskanje ob vsaki oceni (vrsta AI taskov ocenjuje mirujoce taske vsak 3. tick), pri
 * tekocem tasku pa {@code updateTask} vsakih 4-10 tickov poklice {@code tryMoveToEntityLiving}.
 *
 * <p>Kaj se steje (vse poti vodijo skozi {@code getPathToPos}, tudi
 * {@code getPathToEntityLiving} in {@code getPathToXYZ}) [K]:
 * <ul>
 *   <li><b>iz pomnilnika</b> — vanilla vrne obstojeco pot brez iskanja, kadar ta ni konacana in je
 *       cilj isti ({@code currentPath != null && !isFinished() && pos.equals(targetPos)});</li>
 *   <li><b>brez poti</b> — {@code null} (navigator ne more navigirati ali iskalnik ni nasel nicesar);</li>
 *   <li><b>cela</b> — zadnja tocka poti je ciljni blok (razdalja 0);</li>
 *   <li><b>delna</b> — zadnja tocka poti je blizje NPC-ju od cilja; vanilla iskalnik vrne najboljso
 *       delno pot, kadar cilj ni dosegljiv. Prav ta primer je vprasanje M5.9.</li>
 * </ul>
 *
 * <p>Merjenje je privzeto izklopljeno ({@code /rwpath iskanje 1} ga vklopi); obnasanje je v obeh
 * primerih isti klic {@code super}. Stanje bere samo strezniska nit.
 */
public final class PathSearch {
    /** Izid klica: vanilla je vrnila obstojeco pot brez iskanja. */
    public static final int CACHED = 0;
    /** Izid klica: {@code null}. */
    public static final int NONE = 1;
    /** Izid klica: pot do ciljnega bloka. */
    public static final int FULL = 2;
    /** Izid klica: delna pot (cilj ni dosegljiv). */
    public static final int PARTIAL = 3;

    private static boolean timing;

    static long calls;
    static long nanos;
    static long cached;
    static long none;
    static long full;
    static long partial;
    /** Vsota Cebisevih razdalj od zadnje tocke delne poti do cilja (za povprecje). */
    static long partialDistance;
    static long maxNanos;

    private PathSearch() {
    }

    public static void setTiming(boolean on) {
        timing = on;
    }

    public static boolean timing() {
        return timing;
    }

    public static void reset() {
        calls = 0;
        nanos = 0;
        cached = 0;
        none = 0;
        full = 0;
        partial = 0;
        partialDistance = 0;
        maxNanos = 0;
    }

    /**
     * Razvrstitev izida brez poznavanja Minecrafta (testirano brez sveta).
     *
     * @param hasPath klic je vrnil pot
     * @param sameAsCurrent vrnjena pot je ista instanca kot pot pred klicem (vanilla bliznjica)
     * @param finalDistance Cebiseva razdalja od zadnje tocke poti do ciljnega bloka
     */
    public static int classify(boolean hasPath, boolean sameAsCurrent, int finalDistance) {
        if (!hasPath) {
            return NONE;
        }
        if (sameAsCurrent) {
            return CACHED;
        }
        return finalDistance == 0 ? FULL : PARTIAL;
    }

    /** Zapis enega klica {@code getPathToPos}; {@code nanosTaken} je cel klic vkljucno z bliznjico. */
    static void record(int outcome, long nanosTaken, int finalDistance) {
        ++calls;
        nanos += nanosTaken;
        if (nanosTaken > maxNanos) {
            maxNanos = nanosTaken;
        }
        switch (outcome) {
            case CACHED:
                ++cached;
                break;
            case NONE:
                ++none;
                break;
            case FULL:
                ++full;
                break;
            default:
                ++partial;
                partialDistance += finalDistance;
                break;
        }
    }

    /** Vsi klici {@code getPathToPos} med merjenjem (vkljucno z bliznjico brez iskanja). */
    public static long calls() {
        return calls;
    }

    public static long nanos() {
        return nanos;
    }

    public static long maxNanos() {
        return maxNanos;
    }

    /** Klici, ki jih je vanilla odgovorila z obstojeco potjo (brez iskanja). */
    public static long cached() {
        return cached;
    }

    public static long none() {
        return none;
    }

    public static long full() {
        return full;
    }

    public static long partial() {
        return partial;
    }

    /** Povprecna razdalja zadnje tocke delne poti od cilja (0, ce delnih poti ni). */
    public static double averagePartialDistance() {
        return partial == 0 ? 0.0 : (double) partialDistance / (double) partial;
    }
}
