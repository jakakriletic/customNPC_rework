package noppes.npcs.rework.army;

import java.util.Random;
import noppes.npcs.rework.formation.Slot;

/** M11.4: unordered, stable scatter in a disk; no nearest-slot assignment or rows.
 * Randomness belongs to the order's members, never to a world tick. Bounded rejection
 * keeps destinations apart; a failed packing is explicit rather than overlapping.
 */
public final class ArmySwarm {
    public static final double SEPARATION = 1.8;
    private ArmySwarm() { }

    public static Slot[] scatter(long[] seeds, double gap) {
        if (seeds.length > 2048 || !Double.isFinite(gap) || gap <= 0) {
            throw new IllegalArgumentException("invalid swarm");
        }
        Slot[] out = new Slot[seeds.length];
        double radius = radius(seeds.length, gap);
        for (int i = 0; i < seeds.length; i++) {
            Random random = new Random(seeds[i]);
            for (int attempt = 0; attempt < 10000; attempt++) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double r = radius * Math.sqrt(random.nextDouble());
                Slot p = new Slot(r * Math.cos(angle), r * Math.sin(angle));
                boolean clear = true;
                for (int j = 0; j < i; j++) {
                    if (Math.hypot(p.side - out[j].side, p.back - out[j].back) < gap) { clear = false; break; }
                }
                if (clear) { out[i] = p; break; }
            }
            if (out[i] == null) { throw new IllegalArgumentException("swarm packing exhausted"); }
        }
        return out;
    }

    public static double radius(int count, double gap) { return Math.sqrt(count) * gap; }
}
