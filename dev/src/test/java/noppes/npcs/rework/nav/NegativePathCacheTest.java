package noppes.npcs.rework.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

/**
 * M5.6 (S5): spomin na neuspelo iskanje sme veljati samo znotraj svojega trajanja in samo za
 * (skoraj) isto tarco. Pravilo je tisto, kar doloca velikost spremembe obnasanja, zato je
 * preverjeno brez sveta.
 */
public class NegativePathCacheTest {
    @After
    public void pocisti() {
        NegativePathCache.setMode(NegativePathCache.ORIGINAL);
        NegativePathCache.setTtl(NegativePathCache.DEFAULT_TTL);
        NegativePathCache.setTolerance(NegativePathCache.DEFAULT_TOLERANCE);
        NegativePathCache.setPartialCounts(false);
        NegativePathCache.reset();
    }

    @Test
    public void veljaSamoVTrajanjuInZnotrajTolerance() {
        NegativePathCache.setTtl(20);
        NegativePathCache.setTolerance(1);
        assertTrue("sveze, ista tarca", NegativePathCache.applies(true, 0, 0));
        assertTrue("zadnji tick trajanja", NegativePathCache.applies(true, 19, 0));
        assertFalse("tocno ob izteku", NegativePathCache.applies(true, 20, 0));
        assertFalse("po izteku", NegativePathCache.applies(true, 100, 0));
        assertTrue("tarca premaknjena v toleranci", NegativePathCache.applies(true, 5, 1));
        assertFalse("tarca premaknjena izven tolerance", NegativePathCache.applies(true, 5, 2));
        assertFalse("brez spomina", NegativePathCache.applies(false, 0, 0));
        assertFalse("negativna starost (cas sveta nazaj)", NegativePathCache.applies(true, -1, 0));
    }

    @Test
    public void toleranca0ZahtevaNatanknoIstoTarco() {
        NegativePathCache.setTolerance(0);
        assertTrue(NegativePathCache.applies(true, 1, 0));
        assertFalse(NegativePathCache.applies(true, 1, 1));
    }

    @Test
    public void trajanjeInTolerancaImataSpodnjoMejo() {
        NegativePathCache.setTtl(0);
        assertEquals("trajanje pod 1 ne sme izklopiti preverbe starosti", 1, NegativePathCache.ttl());
        assertTrue(NegativePathCache.applies(true, 0, 0));
        assertFalse(NegativePathCache.applies(true, 1, 0));
        NegativePathCache.setTolerance(-5);
        assertEquals(0, NegativePathCache.tolerance());
    }

    @Test
    public void privzetoOriginalInNeveljavniNacini() {
        assertEquals(NegativePathCache.ORIGINAL, NegativePathCache.mode());
        assertTrue(NegativePathCache.isValidMode(NegativePathCache.VERIFY));
        assertFalse(NegativePathCache.isValidMode(-1));
        assertFalse(NegativePathCache.isValidMode(NegativePathCache.MODES));
        NegativePathCache.setMode(NegativePathCache.MODES);
        assertEquals(NegativePathCache.ORIGINAL, NegativePathCache.mode());
        assertEquals(20, NegativePathCache.DEFAULT_TTL);
        assertEquals(1, NegativePathCache.DEFAULT_TOLERANCE);
        // Privzeto steje za neuspeh samo null: pri steti delni poti je M5.6 izmerila 66,8 %
        // neujemanj s pravim odgovorom.
        assertFalse(NegativePathCache.partialCounts());
        NegativePathCache.setPartialCounts(true);
        assertTrue(NegativePathCache.partialCounts());
    }

    @Test
    public void resetPobriseStevce() {
        ++NegativePathCache.skipped;
        ++NegativePathCache.stored;
        ++NegativePathCache.compared;
        ++NegativePathCache.mismatches;
        assertEquals(1, NegativePathCache.skipped());
        NegativePathCache.reset();
        assertEquals(0, NegativePathCache.skipped());
        assertEquals(0, NegativePathCache.stored());
        assertEquals(0, NegativePathCache.compared());
        assertEquals(0, NegativePathCache.mismatches());
    }
}
