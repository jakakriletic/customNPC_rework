package noppes.npcs.rework.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

/**
 * M5.9: razvrstitev izida iskanja poti in stevci. Razvrstitev je tisto, kar loci vprasanje M5.9
 * (ali nedosegljiva tarca pozene ponovljeno iskanje) od primera, kjer vanilla vrne obstojeco pot
 * brez iskanja.
 */
public class PathSearchTest {
    @After
    public void pocisti() {
        PathSearch.setTiming(false);
        PathSearch.reset();
    }

    @Test
    public void razvrstitevIzidov() {
        assertEquals("brez poti", PathSearch.NONE, PathSearch.classify(false, false, 0));
        assertEquals("brez poti tudi, ce bi bila ista instanca", PathSearch.NONE,
                PathSearch.classify(false, true, 0));
        assertEquals("obstojeca pot brez iskanja", PathSearch.CACHED, PathSearch.classify(true, true, 0));
        assertEquals("cela pot", PathSearch.FULL, PathSearch.classify(true, false, 0));
        assertEquals("delna pot", PathSearch.PARTIAL, PathSearch.classify(true, false, 1));
        assertEquals("delna pot dalec od cilja", PathSearch.PARTIAL, PathSearch.classify(true, false, 37));
    }

    @Test
    public void stevciInPovprecjeDelneRazdalje() {
        PathSearch.record(PathSearch.FULL, 1000L, 0);
        PathSearch.record(PathSearch.PARTIAL, 3000L, 4);
        PathSearch.record(PathSearch.PARTIAL, 2000L, 6);
        PathSearch.record(PathSearch.CACHED, 100L, 0);
        PathSearch.record(PathSearch.NONE, 500L, 0);
        assertEquals(5, PathSearch.calls());
        assertEquals(6600L, PathSearch.nanos());
        assertEquals(3000L, PathSearch.maxNanos());
        assertEquals(1, PathSearch.full());
        assertEquals(2, PathSearch.partial());
        assertEquals(1, PathSearch.cached());
        assertEquals(1, PathSearch.none());
        assertEquals(5.0, PathSearch.averagePartialDistance(), 1e-9);
    }

    @Test
    public void resetPobriseVseInMerjenjeJePrivzetoIzklopljeno() {
        assertFalse(PathSearch.timing());
        PathSearch.setTiming(true);
        assertTrue(PathSearch.timing());
        PathSearch.record(PathSearch.PARTIAL, 10L, 2);
        PathSearch.reset();
        assertEquals(0, PathSearch.calls());
        assertEquals(0L, PathSearch.nanos());
        assertEquals(0L, PathSearch.maxNanos());
        assertEquals(0, PathSearch.partial());
        assertEquals(0.0, PathSearch.averagePartialDistance(), 1e-9);
        // reset ne ugasne merjenja: scenarij ga vklopi enkrat in resetira pred vsako celico.
        assertTrue(PathSearch.timing());
    }
}
