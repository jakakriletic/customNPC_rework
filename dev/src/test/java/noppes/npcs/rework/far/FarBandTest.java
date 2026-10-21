package noppes.npcs.rework.far;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** M5-V.2: pas Chebyshev {@code (min(sledilnik, meja) - 8, min(RwFarRange, meja)]}. */
public class FarBandTest {
    @Test
    public void mejePasuPriRazdalji24InSledilniku64() {
        int evd = (24 - 1) * 16;
        int inner = FarBand.inner(64, evd);
        int outer = FarBand.outer(384, evd);
        assertEquals(56, inner);
        assertEquals(368, outer);
        assertFalse("tocno na notranji meji je se v sledilniku", FarBand.inBand(56.0, 0.0, inner, outer));
        assertTrue(FarBand.inBand(56.001, 0.0, inner, outer));
        assertTrue("prekrivanje s sledilnikom (56..64]", FarBand.inBand(-60.0, 10.0, inner, outer));
        assertTrue("zunanja meja je vkljucena", FarBand.inBand(0.0, -368.0, inner, outer));
        assertFalse(FarBand.inBand(368.001, 0.0, inner, outer));
    }

    @Test
    public void chebyshevKotVanillaSledilnik() {
        // Diagonala 300 x 300 je Chebyshev 300 (ne evklidsko 424): v pasu.
        assertTrue(FarBand.inBand(300.0, 300.0, 56, 368));
        // Blizu po eni osi, dalec po drugi: odloci vecja.
        assertTrue(FarBand.inBand(5.0, -200.0, 56, 368));
        assertFalse(FarBand.inBand(40.0, -40.0, 56, 368));
    }

    @Test
    public void manjsaRazdaljaStreznikaOmejiDomet() {
        int evd = (10 - 1) * 16;
        assertEquals(144, FarBand.outer(384, evd));
        assertEquals(56, FarBand.inner(64, evd));
        assertTrue(FarBand.inBand(144.0, 0.0, 56, 144));
        assertFalse(FarBand.inBand(145.0, 0.0, 56, 144));
    }

    @Test
    public void premajhenDometDaPrazenPas() {
        // vd 4: meja 48 < sledilnik 64 -> notranja 40, zunanja min(384, 48) = 48; manjsi RwFarRange zapre pas.
        assertEquals(40, FarBand.inner(64, 48));
        assertEquals(48, FarBand.outer(384, 48));
        int outer = FarBand.outer(32, 48);
        for (double d = 0.0; d <= 100.0; d += 0.5) {
            assertFalse(FarBand.inBand(d, 0.0, 40, outer));
        }
    }
}
