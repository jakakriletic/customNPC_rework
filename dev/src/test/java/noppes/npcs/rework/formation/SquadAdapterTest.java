package noppes.npcs.rework.formation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.pathfinding.RwNavigatorAccess;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAI;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * M4.14b: regressions from the controller trace on c6d7fe1c (22 Oct 2026).
 * Calls the real Squad adapter, without ticking a world or simulating a pathfinder.
 * Like MountGuardTest, allocates the entity/navigation shell without constructors.
 * setPath records the handed-off route; getPathToPos returns a supplied route and
 * counts requests without a world/pathfinder. tryMoveToXYZ, clearPath and noPath
 * stay vanilla. The recovery lifecycle cases come from the 9c3650a7 trace.
 * These tests do not certify door traversal or Minecraft's full movement physics.
 */
public class SquadAdapterTest {
    private EntityCustomNpc npc;
    private RecordingNavigator nav;
    private EntityMoveHelper helper;
    private SquadPlanner planner;
    private Squad squad;
    private Object oldManager;
    private boolean managerReplaced;

    /** Its constructor is deliberately never invoked (no world is involved). */
    public static final class RecordingNavigator extends PathNavigateGround {
        public Path searchResult;
        public BlockPos searchedTarget;
        public int requests;

        public RecordingNavigator() {
            super(null, null);
        }

        @Override
        public Path getPathToPos(BlockPos target) {
            requests++;
            searchedTarget = target;
            return searchResult;
        }

        @Override
        public boolean setPath(Path path, double speed) {
            RwNavigatorAccess.restore(this, path, speed);
            return path != null;
        }

        /** Vanilla timeout and 100-tick check, exactly as pathFollow invokes them. */
        public void stuckCheck(Vec3d position) {
            checkForStuck(position);
        }
    }

    @Before
    public void setUp() throws Exception {
        npc = allocate(EntityCustomNpc.class);
        npc.ais = new DataAI(npc);
        npc.onGround = true;
        npc.posX = -10.308229;
        npc.posY = 4;
        npc.posZ = 71.698005;
        helper = new EntityMoveHelper(npc);
        nav = allocate(RecordingNavigator.class);
        field(EntityLiving.class, "navigator").set(npc, nav);
        field(EntityLiving.class, "moveHelper").set(npc, helper);

        planner = new SquadPlanner(new Slot[] {new Slot(0, 0)}, new double[] {0.606375},
                PathTrack.straight(-9.5, 4, 60.5, -9.5, 4, 78.5), true,
                -9.5, 4, 78.5, 0, new Terrain.Flat(4), true, true);
        field(SquadPlanner.class, "phase").set(planner, SquadPlanner.Phase.MARCHING);
        planner.targetX[0] = -9.5;
        planner.targetY[0] = 4;
        planner.targetZ[0] = 69.861600;
        planner.mode[0] = SquadPlanner.STEER;
        planner.speed[0] = 0.02;

        SquadOrder order = new SquadOrder(SquadOrder.Shape.LEGIJA, 1, -9.5, 4, 78.5,
                0, 3, true, true, true);
        Constructor<Squad> constructor = Squad.class.getDeclaredConstructor(int.class, World.class,
                String.class, SquadOrder.class, EntityNPCInterface[].class,
                SquadPlanner.class, double[].class, boolean.class);
        constructor.setAccessible(true);
        squad = constructor.newInstance(1, null, "adapter-regression", order,
                new EntityNPCInterface[] {npc}, planner, new double[] {0.25}, false);
        ((boolean[]) field(Squad.class, "eligible").get(squad))[0] = true;

        // finish schedules removal. Supply a private, unregistered manager so
        // that this unit test neither registers Forge handlers nor leaks state.
        oldManager = field(SquadManager.class, "registered").get(null);
        Constructor<SquadManager> managerConstructor = SquadManager.class.getDeclaredConstructor();
        managerConstructor.setAccessible(true);
        field(SquadManager.class, "registered").set(null, managerConstructor.newInstance());
        managerReplaced = true;
    }

    @After
    public void restoreManager() throws Exception {
        if (managerReplaced) {
            field(SquadManager.class, "registered").set(null, oldManager);
        }
    }

    private void movingMember() {
        nav.setPath(new Path(new PathPoint[] {new PathPoint(-10, 4, 79)}), 1.9);
        helper.setMoveTo(-9.5, 4, 79.5, 1.9);
        npc.setAIMoveSpeed(0.476068F);
        npc.moveStrafing = 0.1F;
        npc.motionX = 0.012;
        npc.motionY = -0.0784;
        npc.motionZ = 0.146174;
    }

    private void assertStoppedAtSamePosition(double x, double y, double z) {
        assertTrue("the previous path must be gone", nav.noPath());
        assertEquals("HOLD must stop horizontal inertia", 0, npc.motionX, 0);
        assertEquals("DONE must not drift out of its acquired slot", 0, npc.motionZ, 0);
        assertEquals("gravity must remain intact", -0.0784, npc.motionY, 0);
        assertEquals("forward input must not restart movement", 0, npc.moveForward, 0);
        assertEquals("strafe input must not restart movement", 0, npc.moveStrafing, 0);
        assertFalse("a queued MOVE_TO must not restart movement", helper.isUpdating());
        assertEquals("stopping must not teleport", x, npc.posX, 0);
        assertEquals("stopping must not teleport", y, npc.posY, 0);
        assertEquals("stopping must not teleport", z, npc.posZ, 0);
    }

    @Test
    public void holdStopsHorizontalDriftWithoutTeleportingOrCancellingGravity() {
        movingMember();
        planner.mode[0] = SquadPlanner.HOLD;
        squad.drive(0);
        assertStoppedAtSamePosition(-10.308229, 4, 71.698005);
    }

    @Test
    public void completionStopsHorizontalDriftBeforeRemovingTask() throws Exception {
        movingMember();
        // O member 0 at tick 38, immediately before completion: the helper
        // and input have already stopped, but horizontal inertia has not.
        npc.posX = 12.503014;
        npc.posZ = 77.728309;
        npc.motionX = 0;
        npc.moveForward = 0;
        npc.moveStrafing = 0;
        helper.action = EntityMoveHelper.Action.WAIT;
        planner.targetX[0] = 12.5;
        planner.targetZ[0] = 77.5;
        field(SquadPlanner.class, "phase").set(planner, SquadPlanner.Phase.DONE);
        planner.mode[0] = SquadPlanner.HOLD;
        planner.speed[0] = 0;
        Method finish = Squad.class.getDeclaredMethod("finish");
        finish.setAccessible(true);
        finish.invoke(squad);
        assertTrue("completion must release the squad", squad.isFinished());
        assertStoppedAtSamePosition(12.503014, 4, 77.728309);
    }

    @Test
    public void trailingSteerDoesNotAddForwardLeadToReturnTarget() throws Exception {
        // G member 5, tick 26: its column target is behind it. Adding LEAD
        // cancels that correction and is not movement along the shared path.
        planner.trailing[0] = true;
        field(SquadPlanner.class, "anchorSpeed").setDouble(planner, 0.4851);
        squad.drive(0);
        assertEquals("return to the column target, without forward displacement",
                69, nav.getPath().getFinalPathPoint().z);
    }

    @Test
    public void followingSteerUsesTrackAimWithoutLeadOrSearch() throws Exception {
        // P4 (Q258): the straight line to the target is closed; the member walks to the
        // planner's point on the shared track, without forward lead and without a search.
        planner.following[0] = true;
        planner.aimX[0] = -9.5;
        planner.aimY[0] = 4;
        planner.aimZ[0] = 73.5;
        planner.targetZ[0] = 76.5;
        field(SquadPlanner.class, "anchorSpeed").setDouble(planner, 0.4851);
        squad.drive(0);
        assertEquals(73, nav.getPath().getFinalPathPoint().z);
        assertEquals(-10, nav.getPath().getFinalPathPoint().x);
        assertEquals("following is not a real search", 0, nav.requests);
    }

    @Test
    public void stationaryAnchorSteersTowardsSlotBehindMember() {
        // G after tick 26: the anchor is stopped; a lead point beyond the
        // assigned slot drives a member towards the wall instead of back.
        planner.targetZ[0] = 70.261600;
        squad.drive(0);
        assertEquals("a stopped anchor needs correction towards the slot",
                70, nav.getPath().getFinalPathPoint().z);
    }

    @Test
    public void movingAnchorKeepsForwardLeadForOrdinarySlot() throws Exception {
        npc.posZ = 67;
        field(SquadPlanner.class, "anchorSpeed").setDouble(planner, 0.4851);
        squad.drive(0);
        assertEquals(71, nav.getPath().getFinalPathPoint().z);
    }

    @Test
    public void settlingSteerDoesNotUseMarchingLead() throws Exception {
        field(SquadPlanner.class, "phase").set(planner, SquadPlanner.Phase.SETTLING);
        squad.drive(0);
        assertEquals(69, nav.getPath().getFinalPathPoint().z);
    }

    private void adapterTick(int tick) throws Exception {
        // Exercise drive independently of world ticking, with the same fresh
        // per-tick request budget that Squad.tick supplies.
        field(Squad.class, "ticks").setInt(squad, tick);
        field(Squad.class, "pathBudget").setInt(squad, Squad.PATH_BUDGET);
    }

    private Path beginRecovery(int tick, double x, double z) throws Exception {
        planner.mode[0] = SquadPlanner.PATH;
        planner.targetX[0] = x;
        planner.targetZ[0] = z;
        // A supplied detour, not a reconstruction or simulation of A*. Its
        // identity and progress are the contract under test.
        Path route = new Path(new PathPoint[] {
                new PathPoint(-8, 4, 71), new PathPoint(-10, 4, 70),
                new PathPoint((int) Math.floor(x), 4, (int) Math.floor(z))});
        nav.searchResult = route;
        adapterTick(tick);
        squad.drive(0);
        assertEquals("fixture must issue the initial recovery request", 1, nav.requests);
        assertSame("fixture must install the supplied route", route, nav.getPath());
        route.setCurrentPathIndex(1);
        return route;
    }

    @Test
    public void activeRecoveryDoesNotRepeatRequestJustBecauseIntervalElapsed() throws Exception {
        // G/7: ticks 31 -> 51, same target, unfinished 10-point route, no
        // new stuck event. The adapter must update speed without requerying.
        Path route = beginRecovery(31, -9.5, 65.661600);
        planner.speed[0] = 0.1;
        adapterTick(31 + Squad.PATH_INTERVAL);
        squad.drive(0);
        assertEquals("an active unchanged route needs no periodic request", 1, nav.requests);
        assertSame(route, nav.getPath());
        assertEquals("keep progress through the detour", 1, route.getCurrentPathIndex());
        double expectedSpeed = Math.sqrt(0.1 / Squad.GROUND_SPEED_COEFF) / 0.25;
        assertEquals("retained routes still receive the current speed",
                expectedSpeed, RwNavigatorAccess.speed(nav), 1.0E-9);
    }

    @Test
    public void steerNearTargetKeepsUnfinishedRecoveryDetour() throws Exception {
        // G/2: ticks 120 -> 140, target unchanged. Error falls from >6 to
        // 5.86, so the planner asks for STEER while the detour is unfinished.
        Path route = beginRecovery(120, -10.293306, 65.5);
        npc.posX = -9.573776;
        npc.posZ = 71.294884;
        planner.mode[0] = SquadPlanner.STEER;
        planner.trailing[0] = true;
        adapterTick(121);
        squad.drive(0);
        assertSame("approaching the target must not cut across the detour", route, nav.getPath());
        assertEquals(1, route.getCurrentPathIndex());
        assertEquals("STEER must not issue a new search", 1, nav.requests);
    }

    @Test
    public void changedSteerTargetDoesNotCutAcrossUnfinishedRecovery() throws Exception {
        // Recovery trace 1f792335, G/7 at 165 -> 166: the column penalty
        // expires, so the target jumps from (-9.5, 65.6616) to
        // (-6.5, 74.106626). The member is still on the near side of the
        // wall. Nearness to the NEW slot is not proof of a clear segment.
        Path route = beginRecovery(30, -9.5, 65.661600);
        npc.posX = -8.007444;
        npc.posZ = 70.275764;
        planner.targetX[0] = -6.5;
        planner.targetZ[0] = 74.106626;
        planner.mode[0] = SquadPlanner.STEER;
        planner.trailing[0] = false;
        adapterTick(166);
        squad.drive(0);
        assertSame("STEER must not replace an unfinished detour with a straight wall crossing",
                route, nav.getPath());
        assertEquals("retain progress until completion or an explicit PATH replacement",
                1, route.getCurrentPathIndex());
        assertEquals("STEER must not silently search for a replacement", 1, nav.requests);
    }

    @Test
    public void changedSteerTargetDoesNotResurrectClearedRecovery() throws Exception {
        // Retention applies only while the navigator still owns the route.
        // A cleared route may have been invalidated by Minecraft or another
        // task; this test does not assume a cause for navId=0 in the trace.
        Path route = beginRecovery(30, -9.5, 65.661600);
        nav.clearPath();
        planner.targetX[0] = -6.5;
        planner.targetZ[0] = 74.106626;
        planner.mode[0] = SquadPlanner.STEER;
        adapterTick(166);
        squad.drive(0);
        assertNotSame("an invalidated route must not be restored", route, nav.getPath());
        assertEquals(1, nav.getPath().getCurrentPathLength());
        assertEquals(1, nav.requests);
    }

    @Test
    public void completedRecoveryAllowsSteerToAcquireSlot() throws Exception {
        Path route = beginRecovery(120, -10.293306, 65.5);
        route.setCurrentPathIndex(route.getCurrentPathLength());
        planner.mode[0] = SquadPlanner.STEER;
        adapterTick(121);
        squad.drive(0);
        assertNotSame("a completed detour must not block final correction", route, nav.getPath());
        assertEquals(1, nav.getPath().getCurrentPathLength());
        assertFalse(nav.noPath());
        assertEquals(1, nav.requests);
    }

    @Test
    public void substantiallyChangedRecoveryTargetCanRequestNewRoute() throws Exception {
        Path route = beginRecovery(31, -9.5, 65.661600);
        planner.targetZ[0] += 4;
        Path replacement = new Path(new PathPoint[] {new PathPoint(-10, 4, 69)});
        nav.searchResult = replacement;
        adapterTick(31 + Squad.PATH_INTERVAL);
        squad.drive(0);
        assertEquals("retaining a route must not freeze a substantially changed target", 2, nav.requests);
        assertNotSame(route, nav.getPath());
        assertSame(replacement, nav.getPath());
        assertEquals(new BlockPos(-9.5, 4, planner.targetZ[0]), nav.searchedTarget);
    }

    @Test
    public void lostRecoveryRouteCanBeRequestedAgainAfterCooldown() throws Exception {
        Path route = beginRecovery(31, -9.5, 65.661600);
        nav.clearPath();
        adapterTick(31 + Squad.PATH_INTERVAL);
        squad.drive(0);
        assertEquals("cleared navigation must not stay suppressed", 2, nav.requests);
        assertSame(route, nav.getPath());
        assertFalse(nav.noPath());
    }

    @Test
    public void failedRecoveryRequestRetainsCooldownAndCanRetry() throws Exception {
        planner.mode[0] = SquadPlanner.PATH;
        nav.searchResult = null;
        adapterTick(31);
        squad.drive(0);
        assertEquals(1, nav.requests);
        assertTrue(nav.noPath());
        adapterTick(32);
        squad.drive(0);
        assertEquals("failure must not trigger a request each tick", 1, nav.requests);
        adapterTick(31 + Squad.PATH_INTERVAL);
        squad.drive(0);
        assertEquals("failed recovery must remain retryable", 2, nav.requests);
    }

    @Test
    public void completedRecoveryAtTargetSteersInsteadOfRepeatingSearch() throws Exception {
        // FA5 G on 637ad98f: an excused or stuck member stays in PATH for the
        // penalty; once its route is walked, noPath() alone re-searched the same
        // target every 5 ticks. The residual is covered straight.
        Path route = beginRecovery(200, -9.5, 65.661600);
        route.setCurrentPathIndex(route.getCurrentPathLength());
        npc.posX = -9.8;
        npc.posZ = 66.2;
        adapterTick(205);
        squad.drive(0);
        assertEquals("a successful, completed search must not be repeated", 1, nav.requests);
        assertNotSame(route, nav.getPath());
        assertEquals(1, nav.getPath().getCurrentPathLength());
        assertEquals(65, nav.getPath().getFinalPathPoint().z);
        assertFalse(nav.noPath());
    }

    @Test
    public void completedPartialRecoveryRetriesOnlyAfterInterval() throws Exception {
        planner.mode[0] = SquadPlanner.PATH;
        planner.targetX[0] = -9.5;
        planner.targetZ[0] = 78.5;
        // The search ended at the wall, far from the target.
        Path partial = new Path(new PathPoint[] {new PathPoint(-10, 4, 70), new PathPoint(-10, 4, 71)});
        nav.searchResult = partial;
        adapterTick(200);
        squad.drive(0);
        assertEquals(1, nav.requests);
        partial.setCurrentPathIndex(partial.getCurrentPathLength());
        adapterTick(205);
        squad.drive(0);
        assertEquals("a partial route is not arrival, and retry waits for the interval", 1, nav.requests);
        adapterTick(200 + Squad.PATH_INTERVAL);
        squad.drive(0);
        assertEquals("a partial route remains retryable", 2, nav.requests);
    }

    /**
     * Navigator trace 4a7a6623, G/7 at 170: the detour starts while the vanilla
     * timeout clock still holds 6,748 ms of wall time from earlier STEER paths
     * (timeoutTimer is reset only when it fires). The member then stands near
     * its current node, as when pathFollow advances to a close node.
     */
    private Path recoveryWithInheritedTimeout() throws Exception {
        field(PathNavigate.class, "entity").set(nav, npc);
        npc.setAIMoveSpeed(0.606375F);
        field(PathNavigate.class, "lastPosCheck").set(nav, Vec3d.ZERO);
        field(PathNavigate.class, "timeoutCachedNode").set(nav, new Vec3d(-10, 4, 74));
        field(PathNavigate.class, "timeoutTimer").setLong(nav, 6748L);
        field(PathNavigate.class, "timeoutLimit").setDouble(nav, 10787.002508);
        field(PathNavigate.class, "lastTimeoutCheck").setLong(nav, System.currentTimeMillis());
        Path route = beginRecovery(170, -9.5, 65.661600);
        npc.posX = -9.9;
        npc.posZ = 70.1;
        return route;
    }

    @Test
    public void newRecoveryDoesNotInheritTimeoutClockOfEarlierPaths() throws Exception {
        Path route = recoveryWithInheritedTimeout();
        nav.stuckCheck(new Vec3d(npc.posX, npc.posY, npc.posZ));
        assertSame("a fresh detour must not time out on its first close node", route, nav.getPath());
        assertEquals(1, nav.requests);
    }

    @Test
    public void restartedTimeoutStillClearsStalledRecovery() throws Exception {
        // Vanilla stuck protection stays: the same node held past 3 x limit
        // (limit about 233 ms here) still clears the route.
        Path route = recoveryWithInheritedTimeout();
        Vec3d position = new Vec3d(npc.posX, npc.posY, npc.posZ);
        nav.stuckCheck(position);
        assertSame(route, nav.getPath());
        field(PathNavigate.class, "lastTimeoutCheck").setLong(nav, System.currentTimeMillis() - 5000L);
        nav.stuckCheck(position);
        assertTrue("a stalled route must still time out", nav.noPath());
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Object unsafe = field(unsafeClass, "theUnsafe").get(null);
        return type.cast(unsafeClass.getMethod("allocateInstance", Class.class).invoke(unsafe, type));
    }
}
