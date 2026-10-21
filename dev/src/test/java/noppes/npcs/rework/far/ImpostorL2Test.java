package noppes.npcs.rework.far;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** M5-V.6: prag L2 s histerezo ({@link ImpostorL2}). */
public class ImpostorL2Test {
    @Test
    public void vstopSeleOnkrajPraga() {
        assertFalse("tocno na pragu je se model", ImpostorL2.next(false, 56.0 * 56.0, 56));
        assertTrue(ImpostorL2.next(false, 56.01 * 56.01, 56));
        assertFalse(ImpostorL2.next(false, 40.0 * 40.0, 56));
    }

    @Test
    public void histerezaZadrziImpostorDoPragaMinusStiri() {
        // Ze v L2: med 52 in 56 bloki ostane impostor, NPC na meji ne utripa.
        assertTrue(ImpostorL2.next(true, 54.0 * 54.0, 56));
        assertTrue(ImpostorL2.next(true, 52.01 * 52.01, 56));
        assertFalse("na pragu - 4 nazaj na model", ImpostorL2.next(true, 52.0 * 52.0, 56));
        // Isti pas brez prejsnjega L2 ostane model.
        assertFalse(ImpostorL2.next(false, 54.0 * 54.0, 56));
    }

    @Test
    public void zaporedjeHojeCezMejoPreklopiDvakrat() {
        boolean in = false;
        int switches = 0;
        // Naprej 40 -> 70 in nazaj po 0,25 bloka, z drobnim tresenjem +-1 blok okoli vsake tocke.
        for (int pass = 0; pass < 2; ++pass) {
            for (int i = 0; i <= 120; ++i) {
                double d = pass == 0 ? 40.0 + i * 0.25 : 70.0 - i * 0.25;
                for (double j = -1.0; j <= 1.0; j += 1.0) {
                    boolean n = ImpostorL2.next(in, (d + j) * (d + j), 56);
                    if (n != in) {
                        ++switches;
                    }
                    in = n;
                }
            }
        }
        assertEquals("tresenje manjse od histereze ne povzroci utripanja", 2, switches);
    }

    @Test
    public void pragOmejen() {
        assertEquals(ImpostorL2.MIN_START, ImpostorL2.start(0));
        assertEquals(ImpostorL2.MIN_START, ImpostorL2.start(-5));
        assertEquals(56, ImpostorL2.start(56));
        assertEquals(ImpostorL2.MAX_START, ImpostorL2.start(9999));
    }
}
