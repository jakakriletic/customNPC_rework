package noppes.npcs.rework.formation;

/**
 * Vprasanje planerja svetu: ali se na tem mestu da stati. Vmesnik obstaja zato, da je
 * planer mogoce testirati brez Minecrafta.
 */
public interface Terrain {
    /**
     * Visina stopal, na kateri se da stati v stolpcu bloka, ki vsebuje (x, z), iskano okoli
     * {@code yHint}. {@link Double#NaN}, ce takega mesta ni ali chunk ni nalozen.
     */
    double standY(double x, double yHint, double z);

    /** Ravna tla na visini {@code y}, brez ovir. */
    final class Flat implements Terrain {
        private final double y;

        public Flat(double y) {
            this.y = y;
        }

        @Override
        public double standY(double x, double yHint, double z) {
            return y;
        }
    }
}
