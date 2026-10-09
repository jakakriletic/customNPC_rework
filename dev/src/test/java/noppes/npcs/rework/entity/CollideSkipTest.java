package noppes.npcs.rework.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

/**
 * M5.12 (S4): poizvedba se sme preskociti samo v nacinu 1, ko NPC nima vklopljenih skript in na
 * vodilu ni poslusalca. Vsaka druga kombinacija mora teci kot original.
 */
public class CollideSkipTest {
    @After
    public void pocisti() {
        CollideSkip.setMode(CollideSkip.ORIGINAL);
        CollideSkip.reset();
    }

    @Test
    public void preskokSamoBrezSkriptInBrezPoslusalca() {
        for (int m = CollideSkip.ORIGINAL; m <= CollideSkip.VERIFY; ++m) {
            for (int s = 0; s < 2; ++s) {
                for (int l = 0; l < 2; ++l) {
                    boolean skripte = s == 1;
                    boolean poslusalec = l == 1;
                    int d = CollideSkip.decide(m, skripte, poslusalec);
                    boolean brezOpazovalca = !skripte && !poslusalec;
                    String opis = "nacin=" + m + " skripte=" + skripte + " poslusalec=" + poslusalec;
                    if (m == CollideSkip.SKIP && brezOpazovalca) {
                        assertEquals(opis, CollideSkip.SKIP_QUERY, d);
                    } else if (m == CollideSkip.VERIFY && brezOpazovalca) {
                        assertEquals(opis, CollideSkip.RUN_UNOBSERVED, d);
                    } else {
                        assertEquals(opis, CollideSkip.RUN, d);
                    }
                }
            }
        }
    }

    @Test
    public void privzetoOriginalInNeveljavniNacini() {
        assertEquals(CollideSkip.ORIGINAL, CollideSkip.mode());
        assertTrue(CollideSkip.isValidMode(CollideSkip.VERIFY));
        assertFalse(CollideSkip.isValidMode(-1));
        assertFalse(CollideSkip.isValidMode(3));
        CollideSkip.setMode(3);
        assertEquals(CollideSkip.ORIGINAL, CollideSkip.mode());
    }

    @Test
    public void resetPobriseStevce() {
        CollideSkip.unobservedEvent();
        assertEquals(1, CollideSkip.unobservedEvents());
        CollideSkip.reset();
        assertEquals(0, CollideSkip.unobservedEvents());
        assertEquals(0, CollideSkip.calls());
        assertEquals(0, CollideSkip.skipped());
    }
}
