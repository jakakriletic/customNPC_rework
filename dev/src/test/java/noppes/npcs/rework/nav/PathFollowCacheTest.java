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
        PathFollowCache.reset();
        PathFollowCache.setTiming(false);
        PathFollowCache.setMode(PathFollowCache.ORIGINAL);
    }

    @Test
    public void merjenjeJePrivzetoIzklopljenoInNeodvisnoOdNacina() {
        assertFalse(PathFollowCache.timing());
        PathFollowCache.setTiming(true);
        PathFollowCache.setMode(PathFollowCache.MEMO);
        assertTrue(PathFollowCache.timing());
        PathFollowCache.setMode(PathFollowCache.ORIGINAL);
        assertTrue(PathFollowCache.timing());
    }

    @Test
    public void histogramKandidatovInVsote() {
        PathFollowCache.reset();
        PathFollowCache.recordFollow(100, 0);
        PathFollowCache.recordFollow(200, 1);
        PathFollowCache.recordFollow(300, 1);
        PathFollowCache.recordFollow(400, PathFollowCache.HIST - 1);
        PathFollowCache.recordFollow(500, PathFollowCache.HIST + 7);
        PathFollowCache.recordFollow(600, -3);
        assertEquals(6, PathFollowCache.followCalls());
        assertEquals(2100, PathFollowCache.followNanos());
        assertEquals("2/2/0/0/0/2", PathFollowCache.candidateHistogram());

        PathFollowCache.recordDirect(10, true);
        PathFollowCache.recordDirect(20, false);
        PathFollowCache.recordDirect(30, false);
        assertEquals(3, PathFollowCache.directCalls());
        assertEquals(60, PathFollowCache.directNanos());
        assertEquals(1, PathFollowCache.directTrue());
    }

    @Test
    public void resetPobriseVse() {
        PathFollowCache.recordFollow(100, 2);
        PathFollowCache.recordDirect(10, true);
        PathFollowCache.reset();
        assertEquals(0, PathFollowCache.followCalls());
        assertEquals(0, PathFollowCache.followNanos());
        assertEquals(0, PathFollowCache.directCalls());
        assertEquals(0, PathFollowCache.directNanos());
        assertEquals(0, PathFollowCache.directTrue());
        assertEquals("0/0/0/0/0/0", PathFollowCache.candidateHistogram());
    }
}
