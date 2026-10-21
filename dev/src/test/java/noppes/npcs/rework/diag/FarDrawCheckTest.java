package noppes.npcs.rework.diag;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** M5-V.5: cisti deli preverbe izrisa L3 (primerjava oken, pot kamere, merila). */
public class FarDrawCheckTest {
    @Test
    public void primerjavaStejePikslePragaInNajvecjoRazliko() {
        int[] a = {0xFF102030, 0xFF102030, 0xFF102030};
        int[] b = {0xFF102030, 0xFF122030, 0xFF102090};
        assertArrayEquals(new int[] {1, 0x60}, FarDrawCheck.compare(a, b));
        assertArrayEquals(new int[] {0, 0}, FarDrawCheck.compare(a, a));
        // Razlika 2 je pod pragom DIFF_MIN.
        assertEquals(0, FarDrawCheck.compare(new int[] {0xFF000000}, new int[] {0xFF000002})[0]);
        assertEquals(1, FarDrawCheck.compare(new int[] {0xFF000000}, new int[] {0xFF000003})[0]);
    }

    @Test
    public void potKamereNazajInNaprej() {
        int n = FarDrawCheck.Seam.ticks();
        int half = (n - 1) / 2;
        assertEquals(0.0, FarDrawCheck.Seam.offset(0), 0.0);
        assertEquals(FarDrawCheck.Seam.DISTANCE, FarDrawCheck.Seam.offset(half), 1e-9);
        assertEquals(0.0, FarDrawCheck.Seam.offset(n - 1), 1e-9);
        double prev = 0.0;
        for (int t = 1; t < n; ++t) {
            double o = FarDrawCheck.Seam.offset(t);
            assertTrue("korak ne sme preseci hoje igralca", Math.abs(o - prev) <= FarDrawCheck.Seam.STEP + 1e-9);
            prev = o;
        }
    }

    @Test
    public void meriloPrehodaZahtevaObeSmeriInNicVrzeli() {
        FarDrawCheck.Seam s = new FarDrawCheck.Seam(368.0);
        assertFalse(s.ok());
        s.frames = 100;
        s.liveToImpostor = 3;
        s.impostorToLive = 2;
        assertTrue(s.ok());
        s.gaps = 1;
        assertFalse(s.ok());
        s.gaps = 0;
        s.doubles = 1;
        assertFalse(s.ok());
        s.doubles = 0;
        s.impostorToLive = 0;
        assertFalse(s.ok());
    }

    @Test
    public void meriloVidnostiZahtevaRazlikoInMirnoKontrolo() {
        FarDrawCheck c = new FarDrawCheck();
        assertFalse(c.ok());
        c.done = true;
        c.changed = 2;
        c.control = 0;
        assertTrue(c.ok());
        c.control = 1;
        assertFalse(c.ok());
        c.control = 0;
        c.changed = 0;
        assertFalse(c.ok());
    }
}
