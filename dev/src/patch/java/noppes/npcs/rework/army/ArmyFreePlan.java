package noppes.npcs.rework.army;

import noppes.npcs.rework.formation.FormationMath;
import noppes.npcs.rework.formation.FormationShape;
import noppes.npcs.rework.formation.Slot;
import noppes.npcs.rework.formation.SlotAssigner;
import noppes.npcs.rework.formation.SquadOrder;

/** M11.11: pure geometry and per-member progress rules of the free-for-all march (no world access).
 * Members share one route but never wait for each other; the formation is taken only at the goal.
 */
public final class ArmyFreePlan {
    /** A member that moved less than this in {@link #CHECK} ticks is stuck. */
    public static final double MOVE = 1.5;
    public static final int CHECK = 60;
    /** Progress from the first failed recovery that clears the failure count. */
    public static final double PROGRESS = 3.0;
    /** Consecutive recoveries without progress before a member is blocked. */
    public static final int MAX_FAILS = 5;
    /** A blocked member retries one search this often, and gives up for completion after GIVE_UP. */
    public static final int RETRY = 200;
    public static final int GIVE_UP = 600;
    /** Perpendicular offsets of successive recovery targets. */
    static final double[] SIDE = {0, 4, -4, 8, -8};

    private ArmyFreePlan() { }

    /** Destination slots of the whole army; wide enough that 200 members are not 50 rows deep. */
    public static Slot[] slots(SquadOrder.Shape shape, int n) {
        if (n < 1) { return new Slot[0]; }
        switch (shape) {
            case OBRAMBA: return FormationShape.ring(n, 3);
            case KOLONA: return FormationShape.column(n, Math.max(2, (int) Math.round(Math.sqrt(n) / 2)));
            default: return FormationShape.legion(n, Math.max(4, (int) Math.round(Math.sqrt(2.0 * n))));
        }
    }

    /** World x/z of each member's destination, assigned without crossings from the current positions. */
    public static double[][] destinations(double[] xs, double[] zs, double goalX, double goalZ, double yaw,
            SquadOrder.Shape shape) {
        Slot[] slots = slots(shape, xs.length);
        int[] assignment = SlotAssigner.assign(xs, zs, slots, yaw);
        double[][] out = new double[xs.length][];
        for (int i = 0; i < xs.length; i++) {
            Slot s = slots[assignment[i]];
            double x = FormationMath.worldX(goalX, yaw, s), z = FormationMath.worldZ(goalZ, yaw, s);
            out[i] = new double[] {x, z, s.faceOutward ? outward(goalX, goalZ, x, z, yaw) : yaw};
        }
        return out;
    }

    private static double outward(double cx, double cz, double x, double z, double fallback) {
        double dx = x - cx, dz = z - cz;
        return Math.hypot(dx, dz) < 1e-6 ? fallback : FormationMath.yawOf(dx, dz);
    }

    /** Recovery target for attempt {@code attempt}: the target shifted sideways, perpendicular to the approach. */
    public static double[] side(double fromX, double fromZ, double tx, double tz, int attempt) {
        double offset = SIDE[Math.floorMod(attempt, SIDE.length)];
        double dx = tx - fromX, dz = tz - fromZ, len = Math.hypot(dx, dz);
        if (offset == 0 || len < 1e-6) { return new double[] {tx, tz}; }
        return new double[] {tx - dz / len * offset, tz + dx / len * offset};
    }

    /** Per-member progress watch: stuck detection, recovery count and the blocked state. */
    public static final class Walker {
        private double checkX, checkZ, failX, failZ;
        private int since, fails, blockedAt, lastTry;
        private boolean blocked, started;

        public void reset(int tick, double x, double z) {
            checkX = x; checkZ = z; since = tick; started = true;
        }

        /** True when the member has not moved {@link #MOVE} in {@link #CHECK} ticks. */
        public boolean stuck(int tick, double x, double z) {
            if (!started) { reset(tick, x, z); return false; }
            if (fails > 0 && Math.hypot(x - failX, z - failZ) >= PROGRESS) { fails = 0; blocked = false; }
            if (Math.hypot(x - checkX, z - checkZ) >= MOVE) { reset(tick, x, z); return false; }
            return tick - since >= CHECK;
        }

        /** A recovery search was issued at this position. Returns the attempt number (0 first). */
        public int recovery(int tick, double x, double z) {
            if (fails == 0) { failX = x; failZ = z; }
            int attempt = fails++;
            lastTry = tick;
            reset(tick, x, z);
            if (fails > MAX_FAILS && !blocked) { blocked = true; blockedAt = tick; }
            return attempt;
        }

        public boolean blocked() { return blocked; }
        public boolean retryDue(int tick) { return blocked && tick - lastTry >= RETRY; }
        public boolean gaveUp(int tick) { return blocked && tick - blockedAt >= GIVE_UP; }
        public int fails() { return fails; }
    }
}
