package noppes.npcs.rework.army;

import org.junit.Test;
import static org.junit.Assert.*;

public class ArmyWaypointsTest {
    @Test public void longAxialGoalRequestsOneBoundedLeg() {
        double[] p = ArmyWaypoints.next(0, 4, 0, 0, 4, 240, 32, 32);
        assertArrayEquals(new double[] {0, 4, 24}, p, 0);
        assertFalse(ArmyWaypoints.finalLeg(p, 0, 4, 240));
    }
    @Test public void nearbyGoalIsExactIncludingFractionalCoordinates() {
        double[] p = ArmyWaypoints.next(-1.2, 4, 0, 5.1, 4.5, -3.2, 32, 32);
        assertArrayEquals(new double[] {5.1, 4.5, -3.2}, p, 0);
        assertTrue(ArmyWaypoints.finalLeg(p, 5.1, 4.5, -3.2));
    }
    @Test public void diagonalAndHeightUseManhattanRatherThanEuclideanBudget() {
        double[] p = ArmyWaypoints.next(0, 4, 0, 100, 104, -100, 32, 32);
        assertArrayEquals(new double[] {8, 12, -8}, p, 0);
        assertEquals(24, ArmyWaypoints.distance(0, 4, 0, p[0], p[1], p[2]), 0);
    }
    @Test public void loweredConfigAndLoweredAttributeBothLimitTheLeg() {
        assertArrayEquals(new double[] {8, 4, 0}, ArmyWaypoints.next(0, 4, 0, 200, 4, 0, 16, 32), 0);
        assertArrayEquals(new double[] {8, 4, 0}, ArmyWaypoints.next(0, 4, 0, 200, 4, 0, 32, 16), 0);
    }
    @Test public void raisedRangeStillBoundsSearchWorkPerLeg() {
        assertEquals(24, ArmyWaypoints.next(0, 4, 0, 200, 4, 0, 512, 512)[0], 0);
    }
    @Test public void invalidRangeOrNonfiniteInputNeverProducesARetry() {
        for (double range : new double[] {0, -32, 8, Double.NaN, Double.NEGATIVE_INFINITY}) {
            assertNull(ArmyWaypoints.next(0, 4, 0, 240, 4, 0, range, 32));
            assertNull(ArmyWaypoints.next(0, 4, 0, 240, 4, 0, 32, range));
        }
        assertNull(ArmyWaypoints.next(Double.NaN, 4, 0, 240, 4, 0, 32, 32));
        assertNull(ArmyWaypoints.next(0, 4, 0, Double.POSITIVE_INFINITY, 4, 0, 32, 32));
        assertFalse(ArmyWaypoints.finalLeg(null, 0, 4, 0));
    }
    @Test public void zeroDistanceAndExactBudgetAreFinalWithoutOvershoot() {
        assertTrue(ArmyWaypoints.finalLeg(ArmyWaypoints.next(3, 4, 5, 3, 4, 5, 32, 32), 3, 4, 5));
        assertTrue(ArmyWaypoints.finalLeg(ArmyWaypoints.next(0, 4, 0, 24, 4, 0, 32, 32), 24, 4, 0));
    }
    @Test public void successiveLegsReachLongNegativeDiagonalGoalAndNeverExceedRange() {
        double[] p = {11.2, 4, 13.7}; int legs = 0;
        while (!ArmyWaypoints.finalLeg(p, -240, 4, -120)) {
            double[] q = ArmyWaypoints.next(p[0], p[1], p[2], -240, 4, -120, 32, 32);
            assertTrue(ArmyWaypoints.distance(p[0], p[1], p[2], q[0], q[1], q[2]) <= 24.000000001);
            assertTrue(ArmyWaypoints.distance(q[0], q[1], q[2], -240, 4, -120)
                    < ArmyWaypoints.distance(p[0], p[1], p[2], -240, 4, -120));
            p = q; assertTrue(++legs < 30);
        }
        assertTrue(legs > 10);
    }
    @Test public void restartNeedsOnlyCurrentPositionAndDurableGoal() {
        double[] p = ArmyWaypoints.next(0, 4, 0, 0, 4, 240, 32, 32);
        p = ArmyWaypoints.next(p[0], p[1], p[2], 0, 4, 240, 32, 32);
        assertArrayEquals(new double[] {0, 4, 48}, p, 0);
        assertArrayEquals(new double[] {0, 4, 71.5}, ArmyWaypoints.next(0, 4, 47.5, 0, 4, 240, 32, 32), 0);
    }
}
