package noppes.npcs.rework.army;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import noppes.npcs.rework.diag.DiagChunkPlan;

/** M11.6 (Q301, Q349-Q351): pure planning of the chunks a marching army holds. No world or Forge types. */
public final class ArmyTicketPlan {
    public static final int DEFAULT_MAX_MARCHES = 2;
    public static final int MAX_MARCHES = 16;
    /** One ring around every member chunk: a member never stands at the edge of the held area. */
    public static final int RADIUS = 1;
    /** After a restart only the saved last known position exists; it may be up to 16 blocks stale. */
    public static final int RESUME_RADIUS = 2;
    /** Upper bound per march; member chunks come first, so a cut drops outer rings, never a member chunk. */
    public static final int MAX_CHUNKS_PER_MARCH = 49;
    /** A chunk is released after this many one-second scans without need (no flapping at a chunk border). */
    public static final int GRACE_SCANS = 3;

    private ArmyTicketPlan() { }

    public static int clampMarches(int value) { return Math.max(0, Math.min(MAX_MARCHES, value)); }

    /**
     * Which armies hold tickets now. A holder keeps its slot while its order stays active (first come, first
     * served); free slots go to the waiting armies in the given deterministic order.
     */
    public static List<String> slots(Collection<String> holders, List<String> active, int max) {
        List<String> out = new ArrayList<String>();
        for (String key : holders) {
            if (active.contains(key) && out.size() < max) { out.add(key); }
        }
        for (String key : active) {
            if (out.size() >= max) { break; }
            if (!out.contains(key)) { out.add(key); }
        }
        return out;
    }

    /** Member chunks in the given (company) order, then rings around them, at most {@code cap} chunks. */
    public static LinkedHashSet<Long> members(double[] xs, double[] zs, int radius, int cap) {
        LinkedHashSet<Long> centres = new LinkedHashSet<Long>();
        for (int i = 0; i < xs.length; i++) {
            centres.add(DiagChunkPlan.pack(DiagChunkPlan.chunkOf(xs[i]), DiagChunkPlan.chunkOf(zs[i])));
        }
        return DiagChunkPlan.expand(centres, radius, cap);
    }

    /** The area around a saved last known position, used until the members are loaded again. */
    public static LinkedHashSet<Long> resume(int x, int z) {
        List<Long> centre = new ArrayList<Long>();
        centre.add(DiagChunkPlan.pack(DiagChunkPlan.chunkOf(x), DiagChunkPlan.chunkOf(z)));
        return DiagChunkPlan.expand(centre, RESUME_RADIUS, MAX_CHUNKS_PER_MARCH);
    }
}
