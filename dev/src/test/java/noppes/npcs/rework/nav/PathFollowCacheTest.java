package noppes.npcs.rework.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

/**
 * M5.10: stevci merjenja sledenja poti. Histogram kandidatov mora vsak pathFollow steti v
 * natanko en predal (zadnji = HIST-1 ali vec), reset mora pobrisati vse, merjenje pa je
 * privzeto izklopljeno in neodvisno od nacina.
 */
public class PathFollowCacheTest {
    @After
    public void pocisti() {
        PathFollowCache.reset(0);
        PathFollowCache.setTiming(false);
        PathFollowCache.setMode(PathFollowCache.ORIGINAL);
    }

    @Test
    public void merjenjeJePrivzetoIzklopljenoInNeodvisnoOdNacina() {
        assertFalse(PathFollowCache.timing());
        PathFollowCache.setTiming(true);
        PathFollowCache.setMode(PathFollowCache.NODE_MEMO);
        assertTrue(PathFollowCache.timing());
        PathFollowCache.setMode(PathFollowCache.ORIGINAL);
        assertTrue(PathFollowCache.timing());
    }

    @Test
    public void histogramKandidatovInVsote() {
        PathFollowCache.reset();
        PathFollowCache.recordFollow(0, 100, 0);
        PathFollowCache.recordFollow(0, 200, 1);
        PathFollowCache.recordFollow(0, 300, 1);
        PathFollowCache.recordFollow(0, 400, PathFollowCache.HIST - 1);
        PathFollowCache.recordFollow(0, 500, PathFollowCache.HIST + 7);
        PathFollowCache.recordFollow(0, 600, -3);
        assertEquals(6, PathFollowCache.followCalls());
        assertEquals(2100, PathFollowCache.followNanos());
        assertEquals("2/2/0/0/0/2", PathFollowCache.candidateHistogram());

        PathFollowCache.recordDirect(0, 10, true);
        PathFollowCache.recordDirect(0, 20, false);
        PathFollowCache.recordDirect(0, 30, false);
        assertEquals(3, PathFollowCache.directCalls());
        assertEquals(60, PathFollowCache.directNanos());
        assertEquals(1, PathFollowCache.directTrue());
    }

    @Test
    public void resetPobriseVse() {
        PathFollowCache.recordFollow(0, 100, 2);
        PathFollowCache.recordDirect(0, 10, true);
        PathFollowCache.reset();
        assertEquals(0, PathFollowCache.followCalls());
        assertEquals(0, PathFollowCache.followNanos());
        assertEquals(0, PathFollowCache.directCalls());
        assertEquals(0, PathFollowCache.directNanos());
        assertEquals(0, PathFollowCache.directTrue());
        assertEquals("0/0/0/0/0/0", PathFollowCache.candidateHistogram());
    }

    @Test
    public void stevciPoNacinuInTickiVOknih() {
        PathFollowCache.reset(100);
        PathFollowCache.recordFollow(PathFollowCache.ORIGINAL, 1000, 1);
        PathFollowCache.recordDirect(PathFollowCache.ORIGINAL, 800, true);
        PathFollowCache.switchMode(PathFollowCache.NODE_MEMO, 120);
        PathFollowCache.recordFollow(PathFollowCache.NODE_MEMO, 400, 1);
        PathFollowCache.recordDirect(PathFollowCache.NODE_MEMO, 300, true);
        PathFollowCache.switchMode(PathFollowCache.ORIGINAL, 150);
        assertEquals(PathFollowCache.ORIGINAL, PathFollowCache.mode());
        assertEquals(20 + 10, PathFollowCache.ticksIn(PathFollowCache.ORIGINAL, 160));
        assertEquals(30, PathFollowCache.ticksIn(PathFollowCache.NODE_MEMO, 160));
        assertEquals("0:30:1:1000:1:800;1:30:1:400:1:300", PathFollowCache.perMode(160));
        // vsote ostanejo skupne
        assertEquals(2, PathFollowCache.followCalls());
        assertEquals(1100, PathFollowCache.directNanos());
        PathFollowCache.reset(200);
        assertEquals("0:5:0:0:0:0", PathFollowCache.perMode(205));
    }

    @Test
    public void veljavniNacini() {
        for (int m = 0; m < PathFollowCache.MODES; ++m) {
            assertTrue(PathFollowCache.isValidMode(m));
        }
        assertFalse(PathFollowCache.isValidMode(-1));
        assertFalse(PathFollowCache.isValidMode(PathFollowCache.MODES));
        PathFollowCache.setMode(PathFollowCache.MODES);
        assertEquals(PathFollowCache.ORIGINAL, PathFollowCache.mode());
    }
}
