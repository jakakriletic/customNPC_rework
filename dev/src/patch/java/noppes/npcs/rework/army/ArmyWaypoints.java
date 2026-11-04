package noppes.npcs.rework.army;

/** M11.5: bounded requests for ONE army anchor path per leg, not a straight replacement path.
 * Vanilla charges Manhattan steps against FOLLOW_RANGE. Leave room for rounding and a small detour.
 * Terrain routing is still performed by the real navigator, which must reach each requested point.
 */
public final class ArmyWaypoints {
    public static final double MARGIN = 8;
    public static final double MAX_LEG = 24;
    private ArmyWaypoints() { }

    public static double distance(double x, double y, double z, double tx, double ty, double tz) {
        return Math.abs(tx - x) + Math.abs(ty - y) + Math.abs(tz - z);
    }

    /** Null means an unusable range/input, never an invented path or a zero-length retry. */
    public static double[] next(double x, double y, double z, double tx, double ty, double tz,
            double configuredRange, double searchRange) {
        double budget = Math.min(MAX_LEG, Math.min(configuredRange, searchRange) - MARGIN);
        double length = distance(x, y, z, tx, ty, tz);
        if (!Double.isFinite(budget) || budget <= 0 || !Double.isFinite(length)) { return null; }
        if (length <= budget) { return new double[] {tx, ty, tz}; }
        double scale = budget / length;
        return new double[] {x + (tx - x) * scale, y + (ty - y) * scale, z + (tz - z) * scale};
    }

    public static boolean finalLeg(double[] target, double x, double y, double z) {
        return target != null && target[0] == x && target[1] == y && target[2] == z;
    }
}
