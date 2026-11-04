package noppes.npcs.rework.formation;

import java.lang.reflect.Field;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.pathfinding.*;
import net.minecraft.util.math.Vec3d;
import noppes.npcs.entity.EntityCustomNpc;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/** Real vanilla pathFollow boundary on an entity shell; no Minecraft process or fake world physics. */
public class ArmyArrivalTest {
    public static class Npc extends EntityCustomNpc {
        EntityMoveHelper helper;
        public Npc() { super(null); }
        @Override public EntityMoveHelper getMoveHelper() { return helper; }
    }
    private static class Nav extends PathNavigate {
        Nav(Npc npc) { super(npc, null); }
        @Override protected PathFinder getPathFinder() { return new PathFinder(new WalkNodeProcessor()); }
        @Override protected Vec3d getEntityPosition() { return new Vec3d(entity.posX, entity.posY, entity.posZ); }
        @Override protected boolean canNavigate() { return true; }
        @Override protected boolean isDirectPathBetweenPoints(Vec3d a, Vec3d b, int x, int y, int z) { return false; }
        void nativeStep() {
            currentPath = new Path(new PathPoint[] {new PathPoint(0, 4, 0)});
            pathFollow();
        }
    }
    private Npc npc;
    private Nav nav;
    private SquadPlanner planner;
    private final Terrain flat = (x, y, z) -> 4;

    @Before public void setup() throws Exception {
        Class<?> type = Class.forName("sun.misc.Unsafe");
        Field f = type.getDeclaredField("theUnsafe"); f.setAccessible(true);
        npc = (Npc) type.getMethod("allocateInstance", Class.class).invoke(f.get(null), Npc.class);
        npc.width = 0.6f; npc.height = 1.8f;
        npc.posX = 0.08; npc.posY = 4; npc.posZ = 0.08;
        npc.getAttributeMap().registerAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32);
        npc.helper = new EntityMoveHelper(npc);
        nav = new Nav(npc);
        planner = new SquadPlanner(new Slot[] {new Slot(0, 0)}, new double[] {0.2},
                PathTrack.straight(0, 4, 0, 0.5, 4, 0.5), true, 0.5, 4, 0.5, 0, flat, false, true);
    }
    private void update() {
        planner.update(new double[] {npc.posX}, new double[] {4}, new double[] {npc.posZ}, new boolean[] {true});
    }
    @Test public void nativeBoxStopsOutsideStrictPlannerRadius() {
        nav.nativeStep();
        assertTrue("Actual vanilla pathFollow finishes the single node", nav.noPath());
        assertTrue(Math.hypot(0.5 - npc.posX, 0.5 - npc.posZ) > SquadPlanner.SETTLED);
        for (int i = 0; i < 410; i++) { update(); }
        assertEquals(SquadPlanner.Phase.DONE, planner.phase());
        assertEquals("Timeout is NOT arrival", 1, planner.unsettledAtEnd());
    }
    @Test public void refinementDrivesSameHelperAndAcquiresOriginalRadius() {
        nav.nativeStep(); assertTrue(nav.noPath()); update();
        assertTrue(Squad.refineArmyArrival(true, false, planner, 0, npc, flat, 0.7));
        assertTrue(npc.helper.isUpdating());
        assertEquals(0.5, npc.helper.getX(), 0);
        assertEquals(0.5, npc.helper.getZ(), 0);
        assertEquals("Issuing a helper action never teleports", 0.08, npc.posX, 0);
        // Simulate a short ordinary physical step; keep the ORIGINAL strict acquisition radius.
        npc.posX += 0.16; npc.posZ += 0.16; update();
        assertEquals(SquadPlanner.Phase.DONE, planner.phase());
        assertEquals(0, planner.unsettledAtEnd());
    }
    @Test public void blockedTerrainDetoursAndOrdinarySquadsKeepNativeBehaviour() {
        update();
        assertFalse(Squad.refineArmyArrival(false, false, planner, 0, npc, flat, 0.7));
        assertFalse(Squad.refineArmyArrival(true, true, planner, 0, npc, flat, 0.7));
        assertFalse(Squad.refineArmyArrival(true, false, planner, 0, npc, (x, y, z) -> Double.NaN, 0.7));
        assertFalse(npc.helper.isUpdating());
    }
    @Test public void neverRefinesBeyondOneMetreOrVerticalGap() {
        update(); npc.posX = -2;
        assertFalse(Squad.refineArmyArrival(true, false, planner, 0, npc, flat, 0.7));
        npc.posX = 0.08; npc.posY = 2;
        assertFalse(Squad.refineArmyArrival(true, false, planner, 0, npc, flat, 0.7));
    }
}
