package noppes.npcs.rework.nav;

import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class NavPathSampleTest {
    @Test
    public void countsOnlyCompleteMultiPointPathsAndUsesGeometricLength() {
        BlockPos goal = new BlockPos(2, 0, 0);
        NavPathSample sample = new NavPathSample();
        sample.add(null, goal);
        sample.add(path(point(2, 0, 0)), goal); // placeholder med asinhronim iskanjem
        sample.add(path(point(0, 0, 0), point(0, 0, 1)), goal); // delna pot
        sample.add(path(point(0, 0, 0), point(0, 0, 1), point(2, 0, 1), point(2, 0, 0)), goal);
        String marker = sample.marker("NAV_WalkG");
        assertTrue(marker, marker.contains("npc=4 poti=2 celih=1 razmerjeN=1"));
        assertTrue(marker, marker.contains("razmerjeP50=2.000 razmerjeP95=2.000"));
    }

    private static Path path(PathPoint... points) {
        return new Path(points);
    }

    private static PathPoint point(int x, int y, int z) {
        return new PathPoint(x, y, z);
    }
}
