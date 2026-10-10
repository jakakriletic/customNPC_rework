package noppes.npcs.rework.nav;

/**
 * M5.6 (S5): negativni predpomnilnik iskanja poti — po neuspelem iskanju NPC isto tarco nekaj
 * tickov ne isce vec.
 *
 * <p>Podlaga je M5.9: pri vidni, a nedosegljivi tarci je iskanje poti **51 % ticka** (nedosegljiva-500)
 * proti 2,45 % pri dosegljivi tarci, eno iskanje pa 180,5 proti 3,5 us, ker vanilla A* porabi ves
 * proracun 200 vozlisc, kadar cilja ne najde **[M]**. Isti NPC isto tarco isce spet v nekaj tickih:
 * {@code EntityAIAttackTarget.shouldExecute} pokice iskanje ob vsaki oceni (vsak 3. tick, dokler
 * task ne tece), {@code updateTask} pa vsakih 4-10 tickov **[K]**.
 *
 * <p><b>To je sprememba obnasanja</b> (D-007): ce se pot medtem odpre, NPC reagira do {@link #ttl()}
 * tickov kasneje. Zato stikalo in privzeto original. Nacin 2 (preverba) iskanje vseeno pozene in
 * presteje, kolikokrat bi se predpomnjeni odgovor razlikoval od pravega — s tem je velikost
 * spremembe obnasanja izmerjena, ne ocenjena.
 *
 * <ul>
 *   <li>0 = original (vsako iskanje tece)</li>
 *   <li>1 = po neuspelem iskanju se isto tarco {@link #ttl()} tickov ne isce</li>
 *   <li>2 = preverba: iskanje tece, odgovor je original, steje se ujemanje s predpomnjenim</li>
 * </ul>
 *
 * <p>Stanje posameznega NPC-ja je v njegovem navigatorju ({@link RwPathNavigateGround}), tu so samo
 * stikalo in stevci. Bere jih samo strezniska nit.
 */
public final class NegativePathCache {
    public static final int ORIGINAL = 0;
    public static final int ON = 1;
    public static final int VERIFY = 2;
    public static final int MODES = 3;

    /** Privzeto trajanje spomina (ticki). */
    public static final int DEFAULT_TTL = 20;
    /** Privzeta dovoljena razlika ciljnega bloka (Cebiseva razdalja). */
    public static final int DEFAULT_TOLERANCE = 1;

    private static int mode = ORIGINAL;
    private static int ttl = DEFAULT_TTL;
    private static int tolerance = DEFAULT_TOLERANCE;
    private static boolean partialCounts;

    /** Iskanja, ki jih je nacin 1 preskocil. */
    static long skipped;
    /** Zapisani neuspeli izidi (null ali delna pot). */
    static long stored;
    /** Nacin 2: primerjave predpomnjenega in pravega odgovora. */
    static long compared;
    static long mismatches;

    private NegativePathCache() {
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
                return "neuspelo iskanje se " + ttl + " tickov ne ponovi" + (partialCounts ? " (steje tudi delno pot)" : "");
            case VERIFY:
                return "preverba: iskanje tece, steje se ujemanje";
            default:
                return "original";
        }
    }

    public static void setTtl(int ticks) {
        ttl = ticks < 1 ? 1 : ticks;
    }

    public static int ttl() {
        return ttl;
    }

    public static void setTolerance(int blocks) {
        tolerance = blocks < 0 ? 0 : blocks;
    }

    public static int tolerance() {
        return tolerance;
    }

    /**
     * Ali za neuspeh steje tudi **delna** pot (ne le {@code null}). Privzeto ne: M5.6 je 10. 10.
     * izmerila, da bi se predpomnjeni odgovor pri steti delni poti razlikoval od pravega v
     * **66,8 %** primerov (195.402 primerjav), ker vanilla pri nedosegljivi tarci vrne delno pot
     * v 65 % iskanj. Samo {@code null} je veliko blazja sprememba obnasanja.
     */
    public static void setPartialCounts(boolean on) {
        partialCounts = on;
    }

    public static boolean partialCounts() {
        return partialCounts;
    }

    public static void reset() {
        skipped = 0;
        stored = 0;
        compared = 0;
        mismatches = 0;
    }

    /**
     * Ali spomin na neuspelo iskanje velja za ta klic (cista odlocitev, testirana brez sveta).
     *
     * @param has v spominu je neuspelo iskanje
     * @param age ticki od zapisa (sme biti negativen, ce se je svetovni cas zavrtel nazaj)
     * @param distance Cebiseva razdalja med zapisano in zdajsnjo ciljno pozicijo
     */
    public static boolean applies(boolean has, int age, int distance) {
        return has && age >= 0 && age < ttl && distance <= tolerance;
    }

    /** Iskanja, ki jih je nacin 1 preskocil. */
    public static long skipped() {
        return skipped;
    }

    public static long stored() {
        return stored;
    }

    public static long compared() {
        return compared;
    }

    public static long mismatches() {
        return mismatches;
    }
}
