package noppes.npcs.rework.far;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** M5-V.4: dodelitev trakov atlasa z LRU. */
public class ImpostorSlotsTest {
    @Test
    public void prostiTrakiNatoIzrinjenNajdljeNeuporabljen() {
        ImpostorSlots s = new ImpostorSlots(2);
        int a = s.allocate(11L);
        int b = s.allocate(22L);
        assertTrue(a != b);
        assertEquals(0L, s.lastEvicted);
        assertEquals(a, s.allocate(11L));
        // 11 je bil uporabljen nazadnje, zato gre 22.
        assertEquals(a, s.touch(11L));
        int c = s.allocate(33L);
        assertEquals(b, c);
        assertEquals(22L, s.lastEvicted);
        assertEquals(1L, s.evictions);
        assertFalse(s.contains(22L));
        assertEquals(-1, s.touch(22L));
        assertEquals(2, s.size());
    }

    @Test
    public void containsNeSteje_kotUporaba() {
        ImpostorSlots s = new ImpostorSlots(2);
        s.allocate(1L);
        s.allocate(2L);
        assertTrue(s.contains(1L));
        s.allocate(3L);
        assertEquals("contains ne premakne 1 na konec LRU", 1L, s.lastEvicted);
    }

    @Test
    public void sprostitevInPraznjenje() {
        ImpostorSlots s = new ImpostorSlots(1);
        int a = s.allocate(5L);
        s.release(5L);
        assertEquals(0, s.size());
        assertEquals(a, s.allocate(6L));
        assertEquals(0L, s.lastEvicted);
        s.clear();
        assertEquals(0, s.size());
        assertEquals(1, s.capacity());
        s.allocate(7L);
        assertEquals(0L, s.lastEvicted);
    }
}
