package noppes.npcs.rework.far;

/**
 * M5-V.6: odlocitev stopnje L2 za sledene NPC-je (raziskava M5-V.0 §5.0, §5.6, D-048).
 *
 * <p>NPC preide v L2, ko je od kamere dlje kot {@code start} blokov, in se vrne na model sele
 * {@link #HYSTERESIS} blokov blize, da NPC na meji ne utripa med modelom in impostorjem. Razdalja je kot pri
 * K5 ({@code Lod}) in imenu v originalu: {@code getDistanceSq} do {@code renderViewEntity}. Brez Minecraft
 * tipov, testljivo z JUnit.
 */
public final class ImpostorL2 {
    /** Bloki histereze na meji L1/L2. */
    public static final int HYSTERESIS = 4;
    public static final int MIN_START = 8;
    public static final int MAX_START = 512;

    private ImpostorL2() {
    }

    /** Prag iz stikala {@code RwImpostorStart}, omejen na {@link #MIN_START}..{@link #MAX_START}. */
    public static int start(int configured) {
        return Math.max(MIN_START, Math.min(MAX_START, configured));
    }

    /** Ali je NPC v tej slicici v L2, ce je bil v prejsnji {@code was}, pri kvadratu razdalje {@code distSq}. */
    public static boolean next(boolean was, double distSq, int start) {
        double enter = start;
        if (distSq > enter * enter) {
            return true;
        }
        double leave = start - HYSTERESIS;
        return was && distSq > leave * leave;
    }
}
