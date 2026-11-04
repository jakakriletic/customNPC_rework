package noppes.npcs.rework.army;

import java.lang.reflect.Field;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.pathfinding.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.rework.formation.PathTrack;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/** Native same-target reuse on a navigator shell. Only the fresh terrain search is supplied.
 * The controller ticket scenario certifies actual unload/reload and terrain pathfinding.
 */
public class ArmyAnchorPathTest {
    private static final BlockPos GOAL = new BlockPos(420, 4, 100);
    private static final double[] TARGET = {420, 4, 100};
    private static class Nav extends PathNavigate {
        Path fresh;
        int searches;
        Nav(EntityCustomNpc npc) { super(npc, null); }
        @Override protected PathFinder getPathFinder() { return new PathFinder(new WalkNodeProcessor()); }
        @Override protected Vec3d getEntityPosition() { return new Vec3d(entity.posX, entity.posY, entity.posZ); }
        @Override protected boolean canNavigate() { return true; }
        @Override protected boolean isDirectPathBetweenPoints(Vec3d a, Vec3d b, int x, int y, int z) { return false; }
        @Override public Path getPathToPos(BlockPos target) {
            if (currentPath != null && !currentPath.isFinished()) {
                // Actual vanilla cache branch: targetPos below is the same requested target.
                return super.getPathToPos(target);
            }
            searches++;
            assertEquals(GOAL, target);
            return fresh;
        }
        void retain(Path stale) throws Exception {
            currentPath = stale;
            Field f = PathNavigate.class.getDeclaredField("targetPos");
            f.setAccessible(true); f.set(this, GOAL);
        }
    }
    private Nav nav;
    private Path partial;
    private static Path path(int endX) {
        return new Path(new PathPoint[] {new PathPoint(404, 4, 101), new PathPoint(endX, 4, 100)});
    }
    @Before public void setup() throws Exception {
        Class<?> type = Class.forName("sun.misc.Unsafe");
        Field f = type.getDeclaredField("theUnsafe"); f.setAccessible(true);
        EntityCustomNpc npc = (EntityCustomNpc) type.getMethod("allocateInstance", Class.class)
                .invoke(f.get(null), EntityCustomNpc.class);
        npc.getAttributeMap().registerAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32);
        nav = new Nav(npc);
        partial = path(405);
        nav.retain(partial);
        nav.fresh = path(420);
    }
    @Test public void nativeSameTargetReusesAnUnfinishedPartialPathWithoutSearching() {
        assertSame(partial, nav.getPathToXYZ(420, 4, 100));
        assertEquals(0, nav.searches);
        assertEquals(405, nav.getPath().getFinalPathPoint().x);
    }
    @Test public void newArmyLegInvalidatesCachedPathBeforeItsOnlySearch() {
        PathTrack result = ArmyMarches.anchorPath(nav, TARGET);
        assertEquals(1, nav.searches);
        assertNull("Old member path is no longer installed", nav.getPath());
        assertEquals(420.5, result.endX(), 0);
        assertFalse(partial.isFinished());
    }
    @Test public void freshNullIsNotRetriedOrReplacedByCachedPartialPath() {
        nav.fresh = null;
        assertNull(ArmyMarches.anchorPath(nav, TARGET));
        assertEquals(1, nav.searches);
    }
    @Test public void freshIncompletePathKeepsItsRealEndpointForTheArrivalCheck() {
        nav.fresh = path(406);
        assertEquals(406.5, ArmyMarches.anchorPath(nav, TARGET).endX(), 0);
        assertEquals(1, nav.searches);
    }
}
