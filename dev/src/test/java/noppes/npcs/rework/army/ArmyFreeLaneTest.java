package noppes.npcs.rework.army;

import org.junit.Test;
import static org.junit.Assert.*;

/** M11.11e: a member whose lane failed walks the shared route, then ramps back out to its lane; no world. */
public class ArmyFreeLaneTest {
    @Test public void untouchedMemberWalksItsFullLane() {
        assertEquals(1.0, ArmyFreeMarch.laneScale(-1000, 0), 0);
        assertEquals(1.0, ArmyFreeMarch.laneScale(-1000, 500), 0);
    }

    @Test public void failedLaneIsCentralThenRampsBackWithoutAJump() {
        int centre = 40;
        assertEquals(0.0, ArmyFreeMarch.laneScale(centre, 10), 0);
        assertEquals(0.0, ArmyFreeMarch.laneScale(centre, centre), 0);
        double prev = 0;
        for (int k = centre; k <= centre + ArmyFreeMarch.RAMP + 4; k++) {
            double s = ArmyFreeMarch.laneScale(centre, k);
            // Widest lane 32 blocks: at most 2 blocks sideways per route point while ramping.
            assertTrue("step at " + k, (s - prev) * ArmyFreeMarch.LANE <= 2.0 + 1e-9);
            assertTrue(s >= prev);
            prev = s;
        }
        assertEquals(1.0, prev, 0);
    }

    @Test public void heldMemberSettlesBackOntoItsSlotWithoutOvershoot() {
        assertEquals(0.0, ArmyFreeMarch.settle(0.01), 0);
        double off = 0.28;
        for (int t = 0; t < 40; t++) {
            double step = ArmyFreeMarch.settle(off);
            assertTrue(Math.abs(step) <= 0.05);
            double next = off - step;
            assertTrue("no overshoot", next >= -1e-9 || Math.abs(off) < 0.02);
            off = next;
        }
        assertTrue("settled: " + off, Math.abs(off) < 0.03);
        assertTrue(ArmyFreeMarch.settle(-0.2) > 0 == false);
    }
}
