package noppes.npcs.rework.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

/**
 * M5.14 (S16): stikalo in stevci sencnih polj. Samo osvezevanje polj je v
 * {@code EntityNPCInterface.notifyDataManagerChange} in se preveri v svetu (nacin 2).
 */
public class DataShadowTest {
    @After
    public void pocisti() {
        DataShadow.setMode(DataShadow.ORIGINAL);
        DataShadow.reset();
    }

    @Test
    public void privzetoOriginalInNeveljavniNacini() {
        assertEquals(DataShadow.ORIGINAL, DataShadow.mode());
        assertFalse(DataShadow.useShadow());
        assertFalse(DataShadow.verifying());
        assertTrue(DataShadow.isValidMode(DataShadow.VERIFY));
        assertFalse(DataShadow.isValidMode(-1));
        assertFalse(DataShadow.isValidMode(DataShadow.MODES));
        DataShadow.setMode(DataShadow.MODES);
        assertEquals(DataShadow.ORIGINAL, DataShadow.mode());
    }

    @Test
    public void nacinaSeIzkljucujeta() {
        DataShadow.setMode(DataShadow.ON);
        assertTrue(DataShadow.useShadow());
        assertFalse(DataShadow.verifying());
        DataShadow.setMode(DataShadow.VERIFY);
        assertFalse(DataShadow.useShadow());
        assertTrue(DataShadow.verifying());
    }

    @Test
    public void primerjavaStejeNeujemanja() {
        DataShadow.compare(true, true);
        DataShadow.compare(false, false);
        DataShadow.compare(true, false);
        DataShadow.compare(3, 3);
        DataShadow.compare(3, 4);
        assertEquals(5, DataShadow.compared());
        assertEquals(2, DataShadow.mismatches());
    }

    @Test
    public void resetPobriseStevce() {
        DataShadow.read();
        DataShadow.compare(1, 2);
        assertEquals(1, DataShadow.reads());
        DataShadow.reset();
        assertEquals(0, DataShadow.reads());
        assertEquals(0, DataShadow.compared());
        assertEquals(0, DataShadow.mismatches());
    }
}
