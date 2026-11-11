package noppes.npcs.rework.army;

import java.util.HashSet;
import java.util.Set;
import noppes.npcs.rework.formation.Slot;
import noppes.npcs.rework.formation.SquadOrder;
import org.junit.Test;
import static org.junit.Assert.*;

/** M11.11: free-for-all destinations, recovery offsets and the blocked rule; no world. */
public class ArmyFreePlanTest {
    @Test public void twoHundredMembersGetDistinctCompactDestinations() {
        for (SquadOrder.Shape shape : new SquadOrder.Shape[] {SquadOrder.Shape.LEGIJA, SquadOrder.Shape.KOLONA,
                SquadOrder.Shape.OBRAMBA}) {
            int n = 200;
            double[] xs = new double[n], zs = new double[n];
            for (int i = 0; i < n; i++) { xs[i] = (i * 37) % 90 - 45; zs[i] = -100 - (i * 53) % 70; }
            double[][] d = ArmyFreePlan.destinations(xs, zs, 0, 0, 0, shape);
            Set<String> blocks = new HashSet<String>();
            double far = 0;
            for (double[] p : d) {
                assertTrue(Double.isFinite(p[0]) && Double.isFinite(p[1]) && Double.isFinite(p[2]));
                blocks.add((int) Math.floor(p[0]) + "," + (int) Math.floor(p[1]));
                far = Math.max(far, Math.hypot(p[0], p[1]));
            }
            assertEquals(shape + " slots on distinct blocks", n, blocks.size());
            assertTrue(shape + " compact: " + far, far < 60);
        }
    }

    @Test public void legionIsWideNotFiftyRowsDeep() {
        Slot[] s = ArmyFreePlan.slots(SquadOrder.Shape.LEGIJA, 200);
        double maxBack = 0;
        for (Slot slot : s) { maxBack = Math.max(maxBack, Math.abs(slot.back)); }
        assertEquals(200, s.length);
        assertTrue("depth " + maxBack, maxBack < 15);
        assertEquals(0, ArmyFreePlan.slots(SquadOrder.Shape.LEGIJA, 0).length);
    }

    @Test public void ringMembersFaceOutward() {
        double[][] d = ArmyFreePlan.destinations(new double[] {5, -5, 0}, new double[] {0, 0, 5}, 0, 0, 0,
                SquadOrder.Shape.OBRAMBA);
        for (double[] p : d) {
            double fx = -Math.sin(Math.toRadians(p[2])), fz = Math.cos(Math.toRadians(p[2]));
            assertTrue("outward", fx * p[0] + fz * p[1] > 0);
        }
    }

    @Test public void recoveryTargetsShiftSidewaysAndCycle() {
        assertArrayEquals(new double[] {0, 10}, ArmyFreePlan.side(0, 0, 0, 10, 0), 1e-9);
        double[] a = ArmyFreePlan.side(0, 0, 0, 10, 1), b = ArmyFreePlan.side(0, 0, 0, 10, 2);
        assertEquals(10, a[1], 1e-9); assertEquals(10, b[1], 1e-9);
        assertEquals(-a[0], b[0], 1e-9); assertEquals(4, Math.abs(a[0]), 1e-9);
        assertArrayEquals(ArmyFreePlan.side(0, 0, 0, 10, 0), ArmyFreePlan.side(0, 0, 0, 10, 5), 1e-9);
        assertArrayEquals(new double[] {3, 3}, ArmyFreePlan.side(3, 3, 3, 3, 2), 1e-9);
    }

    @Test public void walkerDetectsStuckOnlyWithoutProgress() {
        ArmyFreePlan.Walker w = new ArmyFreePlan.Walker();
        assertFalse(w.stuck(0, 0, 0));
        for (int t = 10; t <= 200; t += 10) { assertFalse(w.stuck(t, t / 10.0, 0)); }
        assertFalse(w.stuck(250, 20, 0));
        assertTrue(w.stuck(260, 20.5, 0));
    }

    @Test public void repeatedFailuresBlockButProgressClearsThem() {
        ArmyFreePlan.Walker w = new ArmyFreePlan.Walker();
        int t = 0;
        for (int k = 0; k < ArmyFreePlan.MAX_FAILS; k++) { assertEquals(k, w.recovery(t += 60, 0, 0)); }
        assertFalse(w.blocked());
        w.recovery(t += 60, 0, 0);
        assertTrue(w.blocked());
        assertFalse(w.gaveUp(t + ArmyFreePlan.GIVE_UP - 1)); assertTrue(w.gaveUp(t + ArmyFreePlan.GIVE_UP));
        assertFalse(w.retryDue(t + ArmyFreePlan.RETRY - 1)); assertTrue(w.retryDue(t + ArmyFreePlan.RETRY));
        w.stuck(t + 10, ArmyFreePlan.PROGRESS, 0);
        assertFalse(w.blocked()); assertEquals(0, w.fails());
    }

    @Test public void freeForAllShapeValues() {
        assertEquals("legion", ArmyCommandService.freeShape("freeforall"));
        assertEquals("legion", ArmyCommandService.freeShape("FFA"));
        assertEquals("ring", ArmyCommandService.freeShape("freeforall:ring"));
        assertEquals("column", ArmyCommandService.freeShape("ffa:column"));
        assertNull(ArmyCommandService.freeShape("freeforall:"));
        assertNull(ArmyCommandService.freeShape("legion"));
        assertNull(ArmyCommandService.freeShape("swarm"));
    }

    @Test public void freeForAllOrderSurvivesNbt() {
        ArmyOrder o = new ArmyOrder(0, 10, 64, -20, SquadOrder.Shape.KOLONA, 3, 8, ArmyOrder.State.MOVING,
                ArmyOrder.Mode.FREEFORALL, ArmyOrder.Stance.PASSIVE);
        ArmyOrder copy = ArmyOrder.read(o.write());
        assertEquals(ArmyOrder.Mode.FREEFORALL, copy.mode);
        assertEquals(SquadOrder.Shape.KOLONA, copy.shape);
        assertEquals(ArmyOrder.Stance.PASSIVE, copy.stance);
    }
}
