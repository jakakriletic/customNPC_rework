package noppes.npcs.rework.army;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import noppes.npcs.rework.diag.DiagChunkPlan;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArmyTicketPlanTest {
    @After public void reset() { ArmyTickets.setMaxMarches(ArmyTicketPlan.DEFAULT_MAX_MARCHES); }

    private static long c(int x, int z) { return DiagChunkPlan.pack(x, z); }

    @Test public void defaultIsTwoAndTheSwitchIsClamped() {
        assertEquals(2, ArmyTickets.maxMarches());
        ArmyTickets.setMaxMarches(-5); assertEquals(0, ArmyTickets.maxMarches());
        ArmyTickets.setMaxMarches(99); assertEquals(ArmyTicketPlan.MAX_MARCHES, ArmyTickets.maxMarches());
        ArmyTickets.setMaxMarches(3); assertEquals(3, ArmyTickets.maxMarches());
    }

    @Test public void zeroSlotsGrantNothing() {
        assertTrue(ArmyTicketPlan.slots(Arrays.asList("a"), Arrays.asList("a", "b"), 0).isEmpty());
    }

    @Test public void freeSlotsGoToWaitingArmiesInOrder() {
        assertEquals(Arrays.asList("a", "b"),
                ArmyTicketPlan.slots(Collections.<String>emptyList(), Arrays.asList("a", "b", "c"), 2));
    }

    @Test public void aHolderKeepsItsSlotBeforeEarlierNamedArmies() {
        assertEquals(Arrays.asList("c", "a"),
                ArmyTicketPlan.slots(Arrays.asList("c"), Arrays.asList("a", "b", "c"), 2));
    }

    @Test public void finishedHolderFreesItsSlotForTheQueue() {
        assertEquals(Arrays.asList("b", "c"),
                ArmyTicketPlan.slots(Arrays.asList("a", "b"), Arrays.asList("b", "c"), 2));
    }

    @Test public void aLoweredLimitKeepsTheOldestHolders() {
        assertEquals(Arrays.asList("x"), ArmyTicketPlan.slots(Arrays.asList("x", "y"), Arrays.asList("x", "y"), 1));
    }

    @Test public void oneMemberHoldsItsChunkAndOneRing() {
        LinkedHashSet<Long> held = ArmyTicketPlan.members(new double[] {8}, new double[] {8}, ArmyTicketPlan.RADIUS,
                ArmyTicketPlan.MAX_CHUNKS_PER_MARCH);
        assertEquals(9, held.size());
        assertEquals(Long.valueOf(c(0, 0)), held.iterator().next());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) { assertTrue(held.contains(c(dx, dz))); }
        }
    }

    @Test public void negativeCoordinatesUseFloorChunks() {
        LinkedHashSet<Long> held = ArmyTicketPlan.members(new double[] {-0.5}, new double[] {-16.0}, 0, 49);
        assertEquals(Collections.singleton(c(-1, -1)), held);
    }

    @Test public void companyInOneChunkSharesTheArea() {
        double[] xs = new double[8], zs = new double[8];
        for (int i = 0; i < 8; i++) { xs[i] = 1 + i; zs[i] = 3 + (i % 2); }
        assertEquals(9, ArmyTicketPlan.members(xs, zs, 1, 49).size());
    }

    @Test public void spreadArmyKeepsEveryMemberChunkWhenTheCapCutsRings() {
        List<Double> x = new ArrayList<Double>(), z = new ArrayList<Double>();
        for (int i = 0; i < 40; i++) { x.add(i * 32.0 + 1); z.add(1.0); }
        double[] xs = new double[x.size()], zs = new double[z.size()];
        for (int i = 0; i < xs.length; i++) { xs[i] = x.get(i); zs[i] = z.get(i); }
        LinkedHashSet<Long> held = ArmyTicketPlan.members(xs, zs, 1, ArmyTicketPlan.MAX_CHUNKS_PER_MARCH);
        assertEquals(ArmyTicketPlan.MAX_CHUNKS_PER_MARCH, held.size());
        for (int i = 0; i < 40; i++) { assertTrue(held.contains(c(i * 2, 0))); }
    }

    @Test public void resumeAreaIsFiveByFiveAroundTheSavedPosition() {
        LinkedHashSet<Long> held = ArmyTicketPlan.resume(1000, -20);
        assertEquals(25, held.size());
        assertEquals(Long.valueOf(c(62, -2)), held.iterator().next());
        assertTrue(held.contains(c(60, -4))); assertTrue(held.contains(c(64, 0)));
        assertFalse(held.contains(c(65, -2)));
    }

    @Test public void staleChunksWaitThreeScansBeforeRelease() {
        java.util.Map<Long, Integer> misses = new java.util.HashMap<Long, Integer>();
        List<Long> forced = Arrays.asList(c(0, 0), c(1, 0));
        LinkedHashSet<Long> want = new LinkedHashSet<Long>(Collections.singleton(c(1, 0)));
        for (int scan = 1; scan < ArmyTicketPlan.GRACE_SCANS; scan++) {
            assertTrue(DiagChunkPlan.stale(forced, want, misses, ArmyTicketPlan.GRACE_SCANS).isEmpty());
        }
        assertEquals(Collections.singletonList(c(0, 0)),
                DiagChunkPlan.stale(forced, want, misses, ArmyTicketPlan.GRACE_SCANS));
    }
}
