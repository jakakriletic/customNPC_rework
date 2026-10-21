package noppes.npcs.rework.far;

/**
 * M5-V.2: pas oddaljenih NPC-jev za enega igralca (D-034, raziskava M5-V.0 §5.1).
 *
 * <p>NPC je v pasu, ce za Chebysheva razdaljo {@code c = max(|dx|, |dz|)} velja
 * {@code inner < c <= outer}, kjer je
 * <ul>
 *   <li>{@code inner = min(trackRange, entityViewDistance) - OVERLAP}: vanilla sledilnik NPC-ja vidi do
 *       {@code min(range, maxRange)} ({@code EntityTrackerEntry.isVisibleTo}, Chebyshev po x/z);
 *       8 blokov prekrivanja pomeni, da nadomestek pride, preden sledilnik entiteto unici;</li>
 *   <li>{@code outer = min(farRange, entityViewDistance)}, {@code entityViewDistance = (vd - 1) * 16}
 *       ({@code PlayerList.getEntityViewDistance}); dlje klient nima terena.</li>
 * </ul>
 * Brez Minecraft tipov, testljivo brez sveta.
 */
public final class FarBand {
    /** Prekrivanje pasu s sledilnikom v blokih (sav nadomestka in zive entitete). */
    public static final int OVERLAP = 8;
    /**
     * M5-V.5: toliko blokov pred notranjo mejo streznik videz NPC-ja izracuna vnaprej; sicer NPC, ki med hojo
     * sele vstopi v pas, prvi krog dobi videz 0 in ob unicenju zive entitete nima pecenega impostorja (siv).
     */
    public static final int PREFETCH = 16;

    private FarBand() {
    }

    public static int inner(int trackRange, int entityViewDistance) {
        return Math.min(trackRange, entityViewDistance) - OVERLAP;
    }

    public static int outer(int farRange, int entityViewDistance) {
        return Math.min(farRange, entityViewDistance);
    }

    public static double chebyshev(double dx, double dz) {
        return Math.max(Math.abs(dx), Math.abs(dz));
    }

    public static boolean inBand(double dx, double dz, int inner, int outer) {
        double c = chebyshev(dx, dz);
        return c > inner && c <= outer;
    }

    /** M5-V.5: NPC tik pred pasom ({@code inner - PREFETCH < c <= inner}), ki mu streznik ze izracuna videz. */
    public static boolean inPrefetch(double dx, double dz, int inner) {
        double c = chebyshev(dx, dz);
        return c > inner - PREFETCH && c <= inner;
    }
}
