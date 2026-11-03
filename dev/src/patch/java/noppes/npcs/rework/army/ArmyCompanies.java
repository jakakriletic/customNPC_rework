package noppes.npcs.rework.army;

import java.util.ArrayList;
import java.util.List;
import noppes.npcs.rework.formation.FormationMath;
import noppes.npcs.rework.formation.FormationShape;
import noppes.npcs.rework.formation.PathTrack;
import noppes.npcs.rework.formation.Slot;
import noppes.npcs.rework.formation.SquadOrder;

/** Pure geometry of M11.3: bounded companies and parallel lanes of ONE army path. */
public final class ArmyCompanies {
    public static final int DEFAULT_SIZE = 8;
    /** Maximum lead of a company over the slowest company, in blocks along the common path. */
    public static final double MAX_LEAD = 4;
    private ArmyCompanies() { }

    public static int clampSize(int size) { return Math.max(1, Math.min(24, size)); }

    /** A join can begin outside the navigator's range. Request a real path to a bounded
     * waypoint, then re-evaluate the clusters after arrival. This is not the army's march route.
     */
    public static double[] joinTarget(double x, double y, double z, double tx, double ty, double tz) {
        double dx = tx - x, dy = ty - y, dz = tz - z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double scale = distance <= 24 ? 1 : 24 / distance;
        return new double[] {x + dx * scale, y + dy * scale, z + dz * scale};
    }

    /** Exact main-component membership, not distance from its centroid (a curved column can surround a straggler).
     * Same largest/mean-x/mean-z ordering as ArmyClusters, for one dimension. O(n^2), no entity dependencies.
     */
    public static boolean[] mainMembers(double[] xs, double[] zs, int radius) {
        boolean[] seen = new boolean[xs.length], best = new boolean[xs.length];
        int bestSize = 0; double bestX = 0, bestZ = 0;
        for (int start = 0; start < xs.length; start++) {
            if (seen[start]) { continue; }
            int[] queue = new int[xs.length]; int count = 1; queue[0] = start; seen[start] = true;
            double x = 0, z = 0;
            for (int at = 0; at < count; at++) {
                int i = queue[at]; x += xs[i]; z += zs[i];
                for (int j = 0; j < xs.length; j++) {
                    if (!seen[j] && Math.hypot(xs[i] - xs[j], zs[i] - zs[j]) <= radius) {
                        seen[j] = true; queue[count++] = j;
                    }
                }
            }
            x = Math.round(x / count); z = Math.round(z / count);
            if (count > bestSize || count == bestSize && (x < bestX || x == bestX && z < bestZ)) {
                best = new boolean[xs.length];
                for (int i = 0; i < count; i++) { best[queue[i]] = true; }
                bestSize = count; bestX = x; bestZ = z;
            }
        }
        return best;
    }

    /** The input is already in deterministic spatial order; no member is dropped or duplicated. */
    public static <T> List<List<T>> split(List<T> members, int size) {
        size = clampSize(size);
        List<List<T>> out = new ArrayList<List<T>>();
        for (int i = 0; i < members.size(); i += size) {
            out.add(new ArrayList<T>(members.subList(i, Math.min(i + size, members.size()))));
        }
        return out;
    }

    public static Slot[] macro(int count, int companySize, SquadOrder.Shape shape) {
        // A company's diameter plus a two-block lane gap; also works for the unproven opt-in size 24.
        double gap = 2 * Math.ceil(Math.sqrt(companySize)) + 2;
        if (shape == SquadOrder.Shape.OBRAMBA) {
            return FormationShape.ring(count, Math.max(gap, count * gap / (2 * Math.PI)));
        }
        return FormationShape.block(count, shape == SquadOrder.Shape.KOLONA ? 1 : (int) Math.ceil(Math.sqrt(count)),
                gap, 0, 0);
    }

    /** Constant offsets preserve the original path's arc length, so companies can share a progress barrier.
     * Obstacles on a lane are handled by the existing per-member recovery. Turning/narrow terrain is M11.9.
     */
    public static PathTrack lane(PathTrack path, Slot slot, double yaw) {
        double dx = FormationMath.worldX(0, yaw, slot), dz = FormationMath.worldZ(0, yaw, slot);
        return path.translated(dx, dz);
    }
}
