package noppes.npcs.rework.far;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

/** M5-V.3: strezniska tabela videzov (deljenje, stetje referenc, LRU, id-ji se ne ponovijo). */
public class FarLooksTest {
    private static final byte[] D = {1, 2, 3};

    @Test
    public void kloniSiDelijoId() {
        FarLooks t = new FarLooks(4);
        Object a = new Object();
        Object b = new Object();
        Object c = new Object();
        FarLooks.Look la = t.put(a, 42L, D);
        FarLooks.Look lb = t.put(b, 42L, D);
        FarLooks.Look lc = t.put(c, 43L, D);
        assertSame(la, lb);
        assertNotEquals(la.id, lc.id);
        assertEquals(2, t.inUse());
        assertEquals(3, t.keys());
        assertSame(la, t.byId(la.id));
        assertSame(la, t.get(b));
    }

    @Test
    public void razveljavitevInNovVidez() {
        FarLooks t = new FarLooks(4);
        Object a = new Object();
        Object b = new Object();
        FarLooks.Look old = t.put(a, 1L, D);
        t.put(b, 1L, D);
        t.forget(a);
        assertNull(t.get(a));
        assertEquals(1, t.inUse());
        // Isti hash ob ponovnem izracunu: isti id (klient ne zahteva znova).
        assertSame(old, t.put(a, 1L, D));
        // Spremenjen videz: nov id, star ostane, dokler ga uporablja b.
        FarLooks.Look changed = t.put(a, 2L, D);
        assertNotEquals(old.id, changed.id);
        assertSame(old, t.byId(old.id));
        assertEquals(2, t.inUse());
    }

    @Test
    public void nedejavniVLruInIdSeNePonovi() {
        FarLooks t = new FarLooks(2);
        Object k = new Object();
        int[] ids = new int[5];
        for (int i = 0; i < 5; ++i) {
            ids[i] = t.put(k, 100L + i, D).id;
        }
        // k zdaj kaze na zadnjega; nedejavni so 100..103, v LRU ostaneta 2 (102, 103).
        assertEquals(1, t.inUse());
        assertEquals(3, t.size());
        assertNull(t.byId(ids[0]));
        assertNull(t.byId(ids[1]));
        assertEquals(ids[2], t.byId(ids[2]).id);
        // Vrnitev nedejavnega videza ga obudi z istim id-jem.
        assertEquals(ids[2], t.put(new Object(), 102L, D).id);
        // Izpadel videz dobi nov id, nikoli starega.
        int again = t.put(new Object(), 100L, D).id;
        for (int id : ids) {
            assertNotEquals(id, again);
        }
        t.clear();
        assertEquals(0, t.size());
        int afterClear = t.put(new Object(), 100L, D).id;
        assertNotEquals(again, afterClear);
    }
}
