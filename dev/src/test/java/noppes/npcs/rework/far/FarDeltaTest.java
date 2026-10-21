package noppes.npcs.rework.far;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

/** M5-V.2: delta posnetkov - posljejo se samo novi, spremenjeni in odstranjeni. */
public class FarDeltaTest {
    private static int[] end(FarDelta d) {
        int[] count = new int[1];
        int[] out = d.end(new int[2], count);
        int[] r = Arrays.copyOf(out, count[0]);
        Arrays.sort(r);
        return r;
    }

    @Test
    public void mirujociNpcPoPrvemPosnetkuNeStaneNic() {
        FarDelta d = new FarDelta();
        d.begin();
        assertTrue(d.offer(1, 100, 32, -200, 10, 0, 0));
        assertTrue(d.offer(2, 0, 0, 0, 0, 0, 0));
        assertEquals(0, end(d).length);
        for (int round = 0; round < 50; ++round) {
            d.begin();
            assertFalse(d.offer(1, 100, 32, -200, 10, 0, 0));
            assertFalse(d.offer(2, 0, 0, 0, 0, 0, 0));
            assertEquals(0, end(d).length);
        }
        assertEquals(2, d.size());
    }

    @Test
    public void pragoviPremikaYawVidezaInZastavic() {
        FarDelta d = new FarDelta();
        d.begin();
        d.offer(1, 0, 0, 0, 0, 0, 0);
        end(d);
        // 0,5 bloka (4/8) ni dovolj, vec je.
        d.begin();
        assertFalse(d.offer(1, 4, -4, 4, 0, 0, 0));
        end(d);
        d.begin();
        assertTrue(d.offer(1, 5, 0, 0, 0, 0, 0));
        end(d);
        // yaw: 8 korakov (11,25 stopinje) ni dovolj, 9 je; tudi cez 0/255.
        d.begin();
        assertFalse(d.offer(1, 5, 0, 0, 248, 0, 0));
        end(d);
        d.begin();
        assertTrue(d.offer(1, 5, 0, 0, 247, 0, 0));
        end(d);
        d.begin();
        assertTrue("videz", d.offer(1, 5, 0, 0, 247, 3, 0));
        end(d);
        d.begin();
        assertTrue("zastavice", d.offer(1, 5, 0, 0, 247, 3, FarCodec.F_WALKING));
        end(d);
        d.begin();
        assertFalse(d.offer(1, 5, 0, 0, 247, 3, FarCodec.F_WALKING));
        end(d);
    }

    @Test
    public void drobniPremikiSeSestevajoDoPraga() {
        FarDelta d = new FarDelta();
        d.begin();
        d.offer(1, 0, 0, 0, 0, 0, 0);
        end(d);
        int sent = 0;
        for (int x = 1; x <= 50; ++x) {
            d.begin();
            if (d.offer(1, x, 0, 0, 0, 0, 0)) {
                ++sent;
            }
            end(d);
        }
        // Po 1/8 na krog: poslje se vsakih 5 (premik > 4 od zadnjega poslanega) -> 50 / 5 = 10.
        assertEquals(10, sent);
    }

    @Test
    public void odstranjeniInPonovnoDodani() {
        FarDelta d = new FarDelta();
        d.begin();
        for (int id = 1; id <= 5; ++id) {
            d.offer(id, id, 0, 0, 0, 0, 0);
        }
        end(d);
        d.begin();
        d.offer(1, 1, 0, 0, 0, 0, 0);
        d.offer(3, 3, 0, 0, 0, 0, 0);
        assertArrayEquals(new int[] {2, 4, 5}, end(d));
        assertEquals(2, d.size());
        d.begin();
        assertTrue("vrnjen v pas je nov", d.offer(4, 4, 0, 0, 0, 0, 0));
        assertArrayEquals(new int[] {1, 3}, end(d));
        d.clear();
        d.begin();
        assertTrue("po clear se poslje vse znova", d.offer(4, 4, 0, 0, 0, 0, 0));
        assertEquals(0, end(d).length);
    }

    @Test
    public void klientovaSlikaJeVednoZadnjiPoslaniPosnetek() {
        // Simulacija: streznik po delti, klient po prejetem; po vsakem krogu se mora klientova shramba
        // ujemati z mnozico NPC-jev v pasu, pozicija pa najvec 0,5 bloka od prave.
        Random r = new Random(7L);
        FarDelta d = new FarDelta();
        java.util.Map<Integer, int[]> client = new java.util.HashMap<Integer, int[]>();
        int[][] pos = new int[400][3];
        for (int round = 0; round < 200; ++round) {
            d.begin();
            java.util.Set<Integer> inBand = new java.util.HashSet<Integer>();
            for (int id = 0; id < pos.length; ++id) {
                pos[id][0] += r.nextInt(5) - 2;
                pos[id][2] += r.nextInt(3) - 1;
                if (r.nextInt(10) == 0) {
                    continue; // zunaj pasu v tem krogu
                }
                inBand.add(id);
                if (d.offer(id, pos[id][0], pos[id][1], pos[id][2], 0, 0, 0)) {
                    client.put(id, pos[id].clone());
                }
            }
            for (int id : end(d)) {
                client.remove(id);
            }
            assertEquals(inBand, client.keySet());
            for (int id : inBand) {
                int[] c = client.get(id);
                assertTrue(Math.abs(c[0] - pos[id][0]) <= FarDelta.MOVE_Q);
                assertTrue(Math.abs(c[2] - pos[id][2]) <= FarDelta.MOVE_Q);
            }
        }
    }
}
