package noppes.npcs.rework.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

/**
 * M5.17 (S14c): stikalo in stevci pomnjenja med iskanjem poti. Sam pomnilnik je preverjen v
 * {@code NodeTypeMemoTest}, zgornja meja rez pa tukaj, ker od nje odvisi poraba pomnilnika pri
 * 500 NPC-jih.
 */
public class PathSearchMemoTest {
    @After
    public void pocisti() {
        PathSearchMemo.setMode(PathSearchMemo.ORIGINAL);
        PathSearchMemo.reset();
    }

    @Test
    public void privzetoOriginalInNeveljavniNacini() {
        assertEquals(PathSearchMemo.ORIGINAL, PathSearchMemo.mode());
        assertTrue(PathSearchMemo.isValidMode(PathSearchMemo.VERIFY));
        assertFalse(PathSearchMemo.isValidMode(-1));
        assertFalse(PathSearchMemo.isValidMode(PathSearchMemo.MODES));
        PathSearchMemo.setMode(PathSearchMemo.MODES);
        assertEquals(PathSearchMemo.ORIGINAL, PathSearchMemo.mode());
    }

    @Test
    public void delezZadetkovInReset() {
        assertEquals(0.0, PathSearchMemo.hitRatio(), 1e-9);
        PathSearchMemo.searches = 2;
        PathSearchMemo.lookups = 100;
        PathSearchMemo.misses = 40;
        assertEquals(0.6, PathSearchMemo.hitRatio(), 1e-9);
        assertEquals(2, PathSearchMemo.searches());
        assertEquals(100, PathSearchMemo.lookups());
        assertEquals(40, PathSearchMemo.misses());
        PathSearchMemo.reset();
        assertEquals(0, PathSearchMemo.lookups());
        assertEquals(0.0, PathSearchMemo.hitRatio(), 1e-9);
    }

    @Test
    public void zgornjaMejaRezUstaviRast() {
        NodeTypeMemo<String> memo = new NodeTypeMemo<String>(4);
        memo.setMaxCapacity(16);
        for (int i = 0; i < 1000; ++i) {
            memo.put(i, 0, 0, "v" + i);
        }
        assertTrue("tabela se ne sme podvojiti nad mejo: " + memo.capacity(), memo.capacity() <= 16);
        // Kar je se slo v tabelo, se mora najti; kar ne, se izracuna na novo (null).
        assertEquals("v0", memo.get(0, 0, 0));
        assertEquals(null, memo.get(999, 0, 0));
    }

    @Test
    public void mejaNiManjsaOdDveh() {
        NodeTypeMemo<String> memo = new NodeTypeMemo<String>(2);
        memo.setMaxCapacity(0);
        memo.put(1, 1, 1, "a");
        assertTrue(memo.capacity() >= 2);
    }
}
