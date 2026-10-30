package net.minecraft.pathfinding;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.junit.Test;

/**
 * P3 (Q256): {@link RwPathFinder} pri meji 200 = vanilla {@link PathFinder}, z mejo 1024 pa z mesta
 * ob steni najde pot okoli vogala M4.16 (Q82). Mreza posnema {@link FlyingNodeProcessor} v prostem
 * zraku (6 sosedov, diagonale samo ob vozliscih s kaznijo, ki jih tu ni), brez sveta.
 */
public class RwPathFinderTest {
    private static final float RANGE = 32.0F;
    private static final BlockPos GOAL = new BlockPos(-14, 4, -60);

    /** Proga V: tla y <= 3, L-zid y 4..19 (krak z = -54, x -27..-8; krak x = -27, z -53..-44). */
    private static boolean solid(int x, int y, int z) {
        if (y <= 3) {
            return true;
        }
        if (y <= 19 && z == -54 && x >= -27 && x <= -8) {
            return true;
        }
        return y <= 19 && x == -27 && z >= -53 && z <= -44;
    }

    private static final class Grid extends NodeProcessor {
        private final int sx;
        private final int sy;
        private final int sz;
        private final boolean wall;

        Grid(int sx, int sy, int sz, boolean wall) {
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
            this.wall = wall;
        }

        private boolean open(int x, int y, int z) {
            if (y > 40) {
                return false;
            }
            if (this.wall) {
                return !solid(x, y, z) && !solid(x, y + 1, z);
            }
            return y > 3;
        }

        @Override
        public void init(IBlockAccess sourceIn, EntityLiving mob) {
            this.pointMap.clearMap();
        }

        @Override
        public void postProcess() {
        }

        @Override
        public PathPoint getStart() {
            return this.openPoint(this.sx, this.sy, this.sz);
        }

        @Override
        public PathPoint getPathPointToCoords(double x, double y, double z) {
            return this.openPoint((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        }

        private PathPoint at(int x, int y, int z) {
            return this.open(x, y, z) ? this.openPoint(x, y, z) : null;
        }

        @Override
        public int findPathOptions(PathPoint[] options, PathPoint c, PathPoint target, float maxDistance) {
            int i = 0;
            PathPoint[] n = {at(c.x, c.y, c.z + 1), at(c.x - 1, c.y, c.z), at(c.x + 1, c.y, c.z),
                at(c.x, c.y, c.z - 1), at(c.x, c.y + 1, c.z), at(c.x, c.y - 1, c.z)};
            for (PathPoint p : n) {
                if (p != null && !p.visited && p.distanceTo(target) < maxDistance) {
                    options[i++] = p;
                }
            }
            return i;
        }

        @Override
        public PathNodeType getPathNodeType(IBlockAccess blockaccessIn, int x, int y, int z, EntityLiving entitylivingIn,
                int xSize, int ySize, int zSize, boolean canBreakDoorsIn, boolean canEnterDoorsIn) {
            return this.open(x, y, z) ? PathNodeType.OPEN : PathNodeType.BLOCKED;
        }

        @Override
        public PathNodeType getPathNodeType(IBlockAccess blockaccessIn, int x, int y, int z) {
            return this.open(x, y, z) ? PathNodeType.OPEN : PathNodeType.BLOCKED;
        }
    }

    private static int[] points(Path path) {
        if (path == null) {
            return new int[0];
        }
        int[] out = new int[path.getCurrentPathLength() * 3];
        for (int i = 0; i < path.getCurrentPathLength(); ++i) {
            PathPoint p = path.getPathPointFromIndex(i);
            out[3 * i] = p.x;
            out[3 * i + 1] = p.y;
            out[3 * i + 2] = p.z;
        }
        return out;
    }

    private static Path vanilla(Grid g) {
        return new PathFinder(g).findPath(null, null, GOAL, RANGE);
    }

    private static Path rw(Grid g, int nodes) {
        return new RwPathFinder(g, nodes).findPath(null, null, GOAL, RANGE);
    }

    @Test
    public void meja200JeEnakaVanilliNaVogalu() {
        for (int x = -20; x <= -10; x += 2) {
            assertArrayEquals("start x=" + x, points(vanilla(new Grid(x, 4, -48, true))),
                    points(rw(new Grid(x, 4, -48, true), RwPathFinder.VANILLA_NODES)));
        }
        assertArrayEquals("mesto ob steni", points(vanilla(new Grid(-14, 4, -53, true))),
                points(rw(new Grid(-14, 4, -53, true), RwPathFinder.VANILLA_NODES)));
    }

    @Test
    public void meja200JeEnakaVanilliNaProstem() {
        assertArrayEquals(points(vanilla(new Grid(-14, 4, -44, false))),
                points(rw(new Grid(-14, 4, -44, false), RwPathFinder.VANILLA_NODES)));
        assertArrayEquals("cilj izven dometa", points(vanilla(new Grid(-14, 4, -12, false))),
                points(rw(new Grid(-14, 4, -12, false), RwPathFinder.VANILLA_NODES)));
    }

    @Test
    public void vanillaSeNaVoglaKoncaObSteniInObnovaNeNapreduje() {
        // Q82: vseh 6 zacetkov konca na isti tocki ob steni, z nje vanilla najde isto tocko.
        for (int x = -20; x <= -10; x += 2) {
            PathPoint end = vanilla(new Grid(x, 4, -48, true)).getFinalPathPoint();
            assertEquals("x=" + x, new PathPoint(-14, 4, -53), end);
        }
        Path again = vanilla(new Grid(-14, 4, -53, true));
        assertTrue("obnova brez napredka", again == null || again.getFinalPathPoint().equals(new PathPoint(-14, 4, -53)));
    }

    @Test
    public void razsirjenoIskanjeObSteniNajdePotOkoliVogala() {
        Path path = rw(new Grid(-14, 4, -53, true), 1024);
        assertNotNull(path);
        assertEquals(new PathPoint(-14, 4, -60), path.getFinalPathPoint());
        for (int i = 0; i < path.getCurrentPathLength(); ++i) {
            PathPoint p = path.getPathPointFromIndex(i);
            assertTrue("tocka v zidu: " + p, !solid(p.x, p.y, p.z));
        }
    }
}
