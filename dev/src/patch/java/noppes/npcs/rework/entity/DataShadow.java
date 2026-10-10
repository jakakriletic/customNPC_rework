package noppes.npcs.rework.entity;

/**
 * M5.14 (S16): sencna polja za CNPC-jeve kljuce {@code EntityDataManager}.
 *
 * <p>Profil JFR (8. 10.): {@code EntityDataManager.get} je **11,6 %** CPU strezniske niti v idle-500
 * in 8,3 % v skripte-500; samo {@code HashMap$TreeNode.root} je 9,2 % lastnega casa, ker je vsak
 * {@code get} bralni lock in iskanje po {@code HashMap} **[M]**. Od tega CNPC bere
 * {@code isAttacking} (4,2 %) in {@code isKilled} (1,0 %) vsak tick **[M]**; ostalo so vanilla kljuci
 * ({@code getHealth}, {@code getFlag}), ki jih brez posega v vanilla razrede ni mogoce doseci.
 *
 * <p>Zakaj je sencno polje enako branju iz {@code dataManager} (vanilla 1.12.2) **[K]**: vrednost se
 * spremeni samo prek {@code EntityDataManager.set} (strezniska in klientska stran) ali
 * {@code setEntryValues} (klient, iz paketa); **obe** metodi po zapisu vrednosti poklicta
 * {@code entity.notifyDataManagerChange(key)}
 * ({@code EntityDataManager:161} in {@code :314}), zato je polje, osvezeno v tem kavlju, vedno enako
 * vrednosti v {@code dataManager}. Zacetne vrednosti postavi {@code register}, ki kavlja ne klice,
 * zato jih {@code entityInit} nastavi izrecno.
 *
 * <ul>
 *   <li>0 = original (branje iz {@code dataManager})</li>
 *   <li>1 = branje iz sencnega polja</li>
 *   <li>2 = preverba: bere iz {@code dataManager}, primerja s sencnim poljem in steje neujemanja</li>
 * </ul>
 * Privzeto 1 od 10. 10. 2026 (D-032): 52,0 M primerjav brez neujemanja (24,0 M idle + 27,9 M boj),
 * idle-500 MSPT povp 3,023 -> 2,863 ms (-5,3 %), p95 4,588 -> 4,325, us/NPC 5,92 -> 5,60; boj ugoden,
 * a v sumu. Stanje bere samo strezniska nit (klient bere svojo kopijo).
 */
public final class DataShadow {
    public static final int ORIGINAL = 0;
    public static final int ON = 1;
    public static final int VERIFY = 2;
    public static final int MODES = 3;

    private static int mode = ORIGINAL;

    static long reads;
    static long compared;
    static long mismatches;

    private DataShadow() {
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

    /** Branje naj gre iz sencnega polja. */
    public static boolean useShadow() {
        return mode == ON;
    }

    /** Branje naj gre iz {@code dataManager} in se primerja s sencnim poljem. */
    public static boolean verifying() {
        return mode == VERIFY;
    }

    public static String describe(int m) {
        switch (m) {
            case ON:
                return "branje iz sencnih polj";
            case VERIFY:
                return "preverba: branje iz dataManager in primerjava";
            default:
                return "original";
        }
    }

    public static void reset() {
        reads = 0;
        compared = 0;
        mismatches = 0;
    }

    /** Eno branje iz sencnega polja (nacin 1). */
    public static void read() {
        ++reads;
    }

    /** Nacin 2: primerjava sencnega polja z vrednostjo iz {@code dataManager}. */
    public static void compare(int shadow, int real) {
        ++compared;
        if (shadow != real) {
            ++mismatches;
        }
    }

    public static void compare(boolean shadow, boolean real) {
        compare(shadow ? 1 : 0, real ? 1 : 0);
    }

    public static long reads() {
        return reads;
    }

    public static long compared() {
        return compared;
    }

    public static long mismatches() {
        return mismatches;
    }
}
