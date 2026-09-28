package noppes.npcs.rework.nav;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.BlockPos;

/** M7.7: enaka geometrijska meritev dejanske poti obeh navigatorjev. */
public final class NavPathSample {
    private static final double COMPLETE_TOLERANCE = 2.0;
    private final List<Double> ratios = new ArrayList<>();
    private int matched;
    private int available;
    private int complete;

    public void add(Path path, BlockPos goal) {
        matched++;
        if (path == null || path.getCurrentPathLength() < 2) {
            return; // tudi Baritonov enotočkovni placeholder med iskanjem
        }
        available++;
        PathPoint start = path.getPathPointFromIndex(0);
        PathPoint end = path.getFinalPathPoint();
        if (end == null || distance(end, goal) >= COMPLETE_TOLERANCE) {
            return;
        }
        complete++;
        double straight = distance(start, goal);
        if (straight < 1.0) {
            return;
        }
        double length = 0.0;
        PathPoint previous = start;
        for (int i = 1; i < path.getCurrentPathLength(); i++) {
            PathPoint next = path.getPathPointFromIndex(i);
            length += distance(previous, next);
            previous = next;
        }
        ratios.add(length / straight);
    }

    public String marker(String prefix) {
        Collections.sort(ratios);
        return String.format(Locale.ROOT,
                "RWNAV-POT ime=%s npc=%d poti=%d celih=%d razmerjeN=%d razmerjeP50=%.3f razmerjeP95=%.3f",
                prefix, matched, available, complete, ratios.size(), percentile(50), percentile(95));
    }

    private double percentile(int p) {
        if (ratios.isEmpty()) {
            return 0.0;
        }
        int index = Math.max(0, Math.min(ratios.size() - 1,
                (int) Math.ceil(p / 100.0 * ratios.size()) - 1));
        return ratios.get(index);
    }

    private static double distance(PathPoint a, PathPoint b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double distance(PathPoint a, BlockPos b) {
        double dx = a.x - b.getX();
        double dy = a.y - b.getY();
        double dz = a.z - b.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
