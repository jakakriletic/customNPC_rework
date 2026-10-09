package noppes.npcs.rework.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

/**
 * M5.11 (S14b): pomnilnik izida po poziciji za cas enega klica. Mora vrniti natanko shranjeno
 * vrednost za isto (x, y, z), nikoli za drugo pozicijo, po {@code reset} pa nicesar; rast tabele
 * ne sme izgubiti vnosov.
 */
public class NodeTypeMemoTest {
    @Test
    public void vrneShranjenoInSamoZaIstoPozicijo() {
        NodeTypeMemo<String> m = new NodeTypeMemo<String>(8);
        assertNull(m.get(1, 2, 3));
        m.put(1, 2, 3, "a");
        m.put(3, 2, 1, "b");
        m.put(1, -1, 3, "c");
        assertEquals("a", m.get(1, 2, 3));
        assertEquals("b", m.get(3, 2, 1));
        assertEquals("c", m.get(1, -1, 3));
        assertNull(m.get(1, 3, 2));
        m.put(1, 2, 3, "d");
        assertEquals("d", m.get(1, 2, 3));
        assertEquals(3, m.size());
    }

    @Test
    public void resetPozabiVse() {
        NodeTypeMemo<String> m = new NodeTypeMemo<String>(4);
        m.put(0, 0, 0, "x");
        m.reset();
        assertNull(m.get(0, 0, 0));
        assertEquals(0, m.size());
        m.put(0, 0, 0, "y");
        assertEquals("y", m.get(0, 0, 0));
    }

    @Test
    public void nullSeNeShrani() {
        NodeTypeMemo<String> m = new NodeTypeMemo<String>(4);
        m.put(5, 5, 5, null);
        assertNull(m.get(5, 5, 5));
        assertEquals(0, m.size());
    }

    @Test
    public void stevcaIskanjInZgresitev() {
        NodeTypeMemo<String> m = new NodeTypeMemo<String>(4);
        m.get(0, 0, 0);
        m.put(0, 0, 0, "x");
        m.get(0, 0, 0);
        m.get(0, 0, 0);
        assertEquals(3, m.lookups());
        assertEquals(1, m.misses());
    }

    /** Nakljucna zaporedja z rastjo in reseti, primerjana s HashMap. */
    @Test
    public void ujemanjeSHashMapZRastjoInReseti() {
        Random r = new Random(511);
        NodeTypeMemo<Integer> m = new NodeTypeMemo<Integer>(2);
        Map<String, Integer> ref = new HashMap<String, Integer>();
        for (int klic = 0; klic < 300; ++klic) {
            m.reset();
            ref.clear();
            int ops = r.nextInt(400);
            int obseg = 1 + r.nextInt(12);
            for (int i = 0; i < ops; ++i) {
                int x = r.nextInt(obseg) - obseg / 2 + (klic % 3 == 0 ? 30000000 : 0);
                int y = r.nextInt(obseg) - 1;
                int z = r.nextInt(obseg) - obseg / 2;
                String k = x + "," + y + "," + z;
                if (r.nextBoolean()) {
                    Integer v = r.nextInt(20);
                    m.put(x, y, z, v);
                    ref.put(k, v);
                } else {
                    assertEquals(k, ref.get(k), m.get(x, y, z));
                }
            }
            assertEquals(ref.size(), m.size());
            for (Map.Entry<String, Integer> e : ref.entrySet()) {
                String[] p = e.getKey().split(",");
                assertEquals(e.getValue(), m.get(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])));
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void velikostMoraBitiPotenca2() {
        new NodeTypeMemo<String>(6);
    }
}
