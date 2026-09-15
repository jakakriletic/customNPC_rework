package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import noppes.npcs.rework.diag.DiagChunkPlan;

import org.junit.Test;

/**
 * M2.1d: nacrt prisilno nalozenih chunkov.
 *
 * <p>Testira se tisti del, kjer je napaka tiha in draga: napacna pretvorba blok/chunk pri
 * negativnih koordinatah bi pokrila napacen chunk, NPC pa bi izgledal, kot da normalno
 * tika, dokler se ne bi cudili neveljavnim stevilkam. Ostalo ({@code ForgeChunkManager})
 * je zunaj dosega enotskega testa in se preverja v svetu (merili C1-C4).
 */
public class DiagChunkPlanTest {

    @Test
    public void packRoundTripsIncludingNegativeCoordinates() {
        int[] samples = {0, 1, -1, 31, -31, 1 << 20, -(1 << 20), Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (int x : samples) {
            for (int z : samples) {
                long packed = DiagChunkPlan.pack(x, z);
                assertEquals("x pri " + x + "/" + z, x, DiagChunkPlan.unpackX(packed));
                assertEquals("z pri " + x + "/" + z, z, DiagChunkPlan.unpackZ(packed));
            }
        }
    }

    @Test
    public void chunkOfMatchesVanillaFloorShift() {
        // Vanilla racuna MathHelper.floor(x) >> 4. Deljenje s 16 bi pri negativnih
        // koordinatah dalo 0 namesto -1 in bi pokrilo sosednji chunk.
        assertEquals(0, DiagChunkPlan.chunkOf(0.0));
        assertEquals(0, DiagChunkPlan.chunkOf(15.9));
        assertEquals(1, DiagChunkPlan.chunkOf(16.0));
        assertEquals(-1, DiagChunkPlan.chunkOf(-0.5));
        assertEquals(-1, DiagChunkPlan.chunkOf(-1.0));
        assertEquals(-1, DiagChunkPlan.chunkOf(-16.0));
        assertEquals(-2, DiagChunkPlan.chunkOf(-16.5));
        assertEquals(-2, DiagChunkPlan.chunkOf(-32.0));
    }

    @Test
    public void radiusZeroKeepsOnlyTheCenters() {
        List<Long> centers = Arrays.asList(DiagChunkPlan.pack(0, 0), DiagChunkPlan.pack(5, -3));
        LinkedHashSet<Long> plan = DiagChunkPlan.expand(centers, 0, 100);
        assertEquals(2, plan.size());
        assertTrue(plan.contains(DiagChunkPlan.pack(0, 0)));
        assertTrue(plan.contains(DiagChunkPlan.pack(5, -3)));
    }

    @Test
    public void radiusOneCoversNineChunksAroundASingleCenter() {
        LinkedHashSet<Long> plan =
                DiagChunkPlan.expand(Arrays.asList(DiagChunkPlan.pack(10, 10)), 1, 100);
        assertEquals(9, plan.size());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                assertTrue("manjka " + dx + "/" + dz,
                        plan.contains(DiagChunkPlan.pack(10 + dx, 10 + dz)));
            }
        }
    }

    @Test
    public void overlappingCentersAreCountedOnce() {
        // Dva NPC-ja v sosednjih chunkih: 9 + 9 = 18 s podvojitvami, brez njih 12.
        List<Long> centers = Arrays.asList(DiagChunkPlan.pack(0, 0), DiagChunkPlan.pack(1, 0));
        LinkedHashSet<Long> plan = DiagChunkPlan.expand(centers, 1, 100);
        assertEquals(12, plan.size());
    }

    @Test
    public void everyCenterIsCoveredBeforeAnyRingChunk() {
        // Bistvo vrstnega reda: ce proracun zmanjka, mora manjkati rob, ne NPC.
        List<Long> centers = new ArrayList<Long>();
        for (int i = 0; i < 20; i++) {
            centers.add(DiagChunkPlan.pack(i * 10, 0));
        }
        LinkedHashSet<Long> plan = DiagChunkPlan.expand(centers, 3, 20);
        assertEquals(20, plan.size());
        for (Long center : centers) {
            assertTrue("sredisce ni pokrito: " + center, plan.contains(center));
        }
    }

    @Test
    public void budgetIsNeverExceeded() {
        List<Long> centers = new ArrayList<Long>();
        for (int i = 0; i < 50; i++) {
            centers.add(DiagChunkPlan.pack(i, i));
        }
        for (int budget = 1; budget <= 200; budget += 7) {
            LinkedHashSet<Long> plan = DiagChunkPlan.expand(centers, 4, budget);
            assertTrue("proracun " + budget + " presezen: " + plan.size(), plan.size() <= budget);
        }
    }

    @Test
    public void emptyOrImpossibleInputGivesEmptyPlan() {
        assertTrue(DiagChunkPlan.expand(null, 2, 100).isEmpty());
        assertTrue(DiagChunkPlan.expand(new ArrayList<Long>(), 2, 100).isEmpty());
        assertTrue(DiagChunkPlan.expand(
                Arrays.asList(DiagChunkPlan.pack(0, 0)), 2, 0).isEmpty());
    }

    @Test
    public void radiusIsClampedInsteadOfBlowingUpTheBudget() {
        // 1000 bi pomenilo 2001x2001 chunkov na NPC-ja; omejitev je MAX_RADIUS.
        int max = DiagChunkPlan.MAX_RADIUS;
        int expected = (2 * max + 1) * (2 * max + 1);
        LinkedHashSet<Long> plan =
                DiagChunkPlan.expand(Arrays.asList(DiagChunkPlan.pack(0, 0)), 1000, 1000000);
        assertEquals(expected, plan.size());
        assertFalse(plan.contains(DiagChunkPlan.pack(max + 1, 0)));
    }

    @Test
    public void negativeRadiusBehavesLikeZero() {
        LinkedHashSet<Long> plan =
                DiagChunkPlan.expand(Arrays.asList(DiagChunkPlan.pack(-7, -7)), -3, 100);
        assertEquals(1, plan.size());
        assertTrue(plan.contains(DiagChunkPlan.pack(-7, -7)));
    }
}
