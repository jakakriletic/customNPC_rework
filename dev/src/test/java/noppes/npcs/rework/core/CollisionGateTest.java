package noppes.npcs.rework.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

/**
 * M5.16: poizvedba entitet za trke se sme preskociti samo v nacinu 1, kadar v svetu ni nobene
 * entitete, ki bi lahko prispevala trkalno skatlo, in premikajoca se entiteta ne prepisuje
 * {@code getCollisionBox(Entity)}. Vsaka druga kombinacija mora teci kot original.
 */
public class CollisionGateTest {
    @After
    public void pocisti() {
        CollisionGate.setMode(CollisionGate.ORIGINAL);
        CollisionGate.reset();
    }

    @Test
    public void preskokSamoBrezPrispevajocihInBrezPrepisaneSkatle() {
        int[] prispevajoci = { -1, 0, 1, 7 };
        for (int m = CollisionGate.ORIGINAL; m <= CollisionGate.VERIFY; ++m) {
            for (int i = 0; i < prispevajoci.length; ++i) {
                for (int b = 0; b < 2; ++b) {
                    boolean prepisanaSkatla = b == 1;
                    int c = prispevajoci[i];
                    int d = CollisionGate.decide(m, c, prepisanaSkatla);
                    String opis = "nacin=" + m + " prispevajocih=" + c + " prepisanaSkatla=" + prepisanaSkatla;
                    boolean dokazanoPrazno = c == 0 && !prepisanaSkatla;
                    if (m == CollisionGate.ON && dokazanoPrazno) {
                        assertEquals(opis, CollisionGate.SKIP_QUERY, d);
                    } else if (m == CollisionGate.VERIFY && dokazanoPrazno) {
                        assertEquals(opis, CollisionGate.RUN_VERIFY, d);
                    } else {
                        assertEquals(opis, CollisionGate.RUN, d);
                    }
                }
            }
        }
    }

    /** Nespremljan svet (-1) ni nikoli preskocen, tudi ce bi -1 kdo bral kot "nic prispevajocih". */
    @Test
    public void nespremljanSvetNiPreskocen() {
        assertEquals(CollisionGate.RUN, CollisionGate.decide(CollisionGate.ON, -1, false));
        assertEquals(CollisionGate.RUN, CollisionGate.decide(CollisionGate.VERIFY, -1, false));
    }

    @Test
    public void privzetoOriginalInNeveljavniNacini() {
        assertEquals(CollisionGate.ORIGINAL, CollisionGate.mode());
        assertTrue(CollisionGate.isValidMode(CollisionGate.VERIFY));
        assertFalse(CollisionGate.isValidMode(-1));
        assertFalse(CollisionGate.isValidMode(CollisionGate.MODES));
        CollisionGate.setMode(CollisionGate.MODES);
        assertEquals(CollisionGate.ORIGINAL, CollisionGate.mode());
        CollisionGate.setMode(CollisionGate.ON);
        assertEquals(CollisionGate.ON, CollisionGate.mode());
        // Neveljaven nacin tudi v decide pomeni original.
        assertEquals(CollisionGate.RUN, CollisionGate.decide(-1, 0, false));
        assertEquals(CollisionGate.RUN, CollisionGate.decide(CollisionGate.MODES, 0, false));
    }

    @Test
    public void resetPobriseStevce() {
        CollisionGate.calls = 3;
        CollisionGate.skipped = 2;
        CollisionGate.compared = 5;
        CollisionGate.mismatches = 1;
        CollisionGate.reset();
        assertEquals(0L, CollisionGate.calls());
        assertEquals(0L, CollisionGate.skipped());
        assertEquals(0L, CollisionGate.compared());
        assertEquals(0L, CollisionGate.mismatches());
    }

    @Test
    public void nespremljanSvetDaMinusEno() {
        assertEquals(-1, CollisionGate.contributors(null));
    }
}
