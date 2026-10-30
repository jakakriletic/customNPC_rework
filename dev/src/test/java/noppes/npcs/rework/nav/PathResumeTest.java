package noppes.npcs.rework.nav;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

/**
 * M4.10 (D-084): pravilo nadaljevanja delne poti in vmesnega cilja, preverjeno brez sveta.
 * Reprodukcije uporabljajo geometrijo prog M4.16 (daleč: 48 blokov po prostem, domet 32).
 */
public class PathResumeTest {
    private static final float RANGE = 32.0F;

    @After
    public void pocisti() {
        PathResume.setMode(PathResume.ORIGINAL);
        PathResume.reset();
    }

    @Test
    public void stikaloSprejmeSamoVeljavneNacine() {
        assertEquals(PathResume.ORIGINAL, PathResume.mode());
        assertFalse(PathResume.on());
        PathResume.setMode(PathResume.ON);
        assertTrue(PathResume.on());
        PathResume.setMode(7);
        assertEquals("neveljaven nacin = original", PathResume.ORIGINAL, PathResume.mode());
        PathResume.setMode(-1);
        assertEquals(PathResume.ORIGINAL, PathResume.mode());
    }

    @Test
    public void izvenDometaJeManhattanskiPogojIskalnika() {
        // Proga DF M4.16: start (50, 4, -48), cilj (50, 4, -96): 48 > 32, original navig0 0/6.
        assertTrue(PathResume.beyondRange(50, 4, -48, 50, 4, -96, RANGE));
        assertFalse("31 blokov po osi je v dometu", PathResume.beyondRange(0, 4, 0, 0, 4, -31, RANGE));
        assertTrue("tocno domet: distanceFromOrigin < 32 ne velja", PathResume.beyondRange(0, 4, 0, 0, 4, -32, RANGE));
        // Diagonala: evklidsko 28,3 (sosedje prestanejo filter cilja), manhattansko 40 (cele poti ni).
        assertTrue(PathResume.beyondRange(0, 4, 0, 20, 4, 20, RANGE));
    }

    @Test
    public void vmesniCiljJeNaCrtiInVProracunu() {
        assertArrayEquals(new int[] {50, 4, -76}, PathResume.intermediate(50, 4, -48, 50, 4, -96, RANGE));
        int[] d = PathResume.intermediate(0, 4, 0, 30, 4, -40, RANGE);
        int m = Math.abs(d[0]) + Math.abs(d[1] - 4) + Math.abs(d[2]);
        assertTrue("manhattansko <= domet - 4: " + m, m <= 28);
        assertTrue("v smeri cilja", d[0] > 0 && d[2] < 0);
        assertEquals("na crti: razmerje komponent", 30.0 / 40.0, d[0] / (double) -d[2], 0.06);
        assertArrayEquals("cilj v proracunu ostane cilj", new int[] {3, 4, 5},
                PathResume.intermediate(0, 4, 0, 3, 4, 5, RANGE));
    }

    @Test
    public void proracunPriMajhnemDometu() {
        assertEquals(28, PathResume.budget(32.0F));
        assertEquals(3, PathResume.budget(6.0F));
        assertEquals(1, PathResume.budget(1.0F));
        assertEquals(1, PathResume.budget(0.0F));
    }

    @Test
    public void ukazVDometuIsceDoCiljaInCelaPotNeNadaljuje() {
        PathResume r = new PathResume();
        assertArrayEquals(new int[] {100, 4, -64}, r.command(100, 4, -48, 100, 4, -64, RANGE));
        r.commandResult(true, 100, 5, -64);
        assertFalse("pot do cilja (leteci konec blok visje) ni delna", r.active());
        assertEquals(0L, PathResume.started);
        assertEquals(0L, PathResume.intermediates);
    }

    @Test
    public void ukazBrezPotiOstaneKotOriginal() {
        PathResume r = new PathResume();
        r.command(0, 4, 0, 0, 4, -10, RANGE);
        r.commandResult(false, 0, 0, 0);
        assertFalse(r.active());
        assertNull(r.next(0, 4, 0, RANGE));
    }

    /** Reprodukcija proge DF/DW: 48 blokov po prostem pride z eno obnovo. */
    @Test
    public void dalecPoProstemPrideZEnoObnovo() {
        PathResume r = new PathResume();
        int[] t = r.command(50, 4, -48, 50, 4, -96, RANGE);
        assertArrayEquals(new int[] {50, 4, -76}, t);
        r.commandResult(true, t[0], t[1], t[2]);
        assertTrue(r.active());
        // NPC prehodi pot do vmesnega cilja.
        int[] n = r.next(50, 4, -76, RANGE);
        assertArrayEquals("20 blokov: v dometu, isce do cilja", new int[] {50, 4, -96}, n);
        assertTrue(r.resumeResult(true, 50, 4, -96));
        assertNull("pot je koncala pri cilju", r.next(50, 4, -96, RANGE));
        assertFalse(r.active());
        assertEquals(1, r.resumes());
        assertEquals(1L, PathResume.started);
        assertEquals(1L, PathResume.intermediates);
        assertEquals(1L, PathResume.resumed);
        assertEquals(1L, PathResume.arrived);
    }

    /** Reprodukcija proge Z (zid): delna pot obstane pred zidom; obnova najde pot cez. */
    @Test
    public void delnaPotSeObnoviOdKonca() {
        PathResume r = new PathResume();
        r.command(-70, 4, -48, -70, 4, -64, RANGE);
        r.commandResult(true, -70, 4, -55);
        assertTrue("konec 9 blokov pred ciljem", r.active());
        assertArrayEquals(new int[] {-70, 4, -64}, r.next(-70, 4, -55, RANGE));
        assertTrue(r.resumeResult(true, -70, 4, -64));
    }

    /** Cilj zaprt: nova pot ne konca blize, nadaljevanje se ustavi (dokazano nedosegljiv s tod). */
    @Test
    public void brezNapredkaSeUstavi() {
        PathResume r = new PathResume();
        r.command(0, 4, 0, 0, 4, -20, RANGE);
        r.commandResult(true, 0, 4, -10);
        assertTrue(r.next(0, 4, -10, RANGE) != null);
        assertFalse("isti konec", r.resumeResult(true, 0, 4, -10));
        assertFalse(r.active());
        assertEquals(1L, PathResume.noProgress);
        r.command(0, 4, 0, 0, 4, -20, RANGE);
        r.commandResult(true, 0, 4, -10);
        r.next(0, 4, -10, RANGE);
        assertFalse("manj kot blok napredka (9,06 proti 10)", r.resumeResult(true, 1, 4, -11));
        r.command(0, 4, 0, 0, 4, -20, RANGE);
        r.commandResult(true, 0, 4, -10);
        r.next(0, 4, -10, RANGE);
        assertFalse("brez poti", r.resumeResult(false, 0, 0, 0));
        assertEquals(3L, PathResume.noProgress);
    }

    @Test
    public void mejaObnov() {
        PathResume r = new PathResume();
        // Cilj 1000 blokov dalec; vsaka pot napreduje 28 blokov.
        int[] t = r.command(0, 4, 0, 0, 4, -1000, RANGE);
        r.commandResult(true, t[0], t[1], t[2]);
        int z = t[2];
        int obnov = 0;
        while (true) {
            int[] n = r.next(0, 4, z, RANGE);
            if (n == null) {
                break;
            }
            assertTrue(r.resumeResult(true, n[0], n[1], n[2]));
            z = n[2];
            obnov++;
        }
        assertEquals(PathResume.MAX_RESUMES, obnov);
        assertEquals(-28 * (PathResume.MAX_RESUMES + 1), z);
        assertFalse(r.active());
        assertEquals(1L, PathResume.limit);
    }

    @Test
    public void prekinitevInNovUkaz() {
        PathResume r = new PathResume();
        int[] t = r.command(0, 4, 0, 0, 4, -48, RANGE);
        r.commandResult(true, t[0], t[1], t[2]);
        r.cancel();
        assertFalse(r.active());
        assertNull(r.next(0, 4, -28, RANGE));
        assertEquals(1L, PathResume.cancelled);
        r.cancel();
        assertEquals("neaktivno se ne steje", 1L, PathResume.cancelled);
        t = r.command(0, 4, 0, 0, 4, -48, RANGE);
        r.commandResult(true, t[0], t[1], t[2]);
        assertTrue(r.active());
        r.command(0, 4, 0, 0, 4, -5, RANGE);
        assertFalse("nov ukaz zamenja prejsnjega", r.active());
        assertEquals(0, r.resumes());
    }

    @Test
    public void stevciVOdgovoru() {
        assertEquals("nadZacetih=0 nadVmesnih=0 nadObnov=0 nadPrispelih=0 nadBrezNapredka=0 nadMeja=0 nadPrekinjenih=0"
                + " nadSirokih=0 nadSirokihNapredek=0 nadSirokihUs=0",
                PathResume.counters());
    }

    @Test
    public void zastojNaVogluSprozRazsirjenoIskanjeBrezSpremembeStanja() {
        // P3 (Q256), proga V M4.16: ukaz (-14, 4, -48) -> (-14, 4, -60), pot konca ob steni (-14, 4, -53).
        PathResume r = new PathResume();
        assertFalse("neaktivno: brez razsirjenega iskanja", r.stalls(false, 0, 0, 0));
        assertArrayEquals(new int[] {-14, 4, -60}, r.command(-14, 4, -48, -14, 4, -60, RANGE));
        r.commandResult(true, -14, 4, -53);
        assertTrue(r.active());
        assertArrayEquals(new int[] {-14, 4, -60}, r.next(-14, 4, -53, RANGE));
        assertTrue("vanilla obnova najde isto tocko", r.stalls(true, -14, 4, -53));
        assertTrue("brez poti", r.stalls(false, 0, 0, 0));
        assertTrue("visje ob steni, ne blize", r.stalls(true, -14, 10, -55));
        assertFalse("razsirjeno iskanje do cilja napreduje", r.stalls(true, -14, 4, -60));
        assertTrue("stalls ne spremeni stanja", r.active());
        assertEquals(1, r.resumes());
        assertTrue(r.resumeResult(true, -14, 4, -60));
        assertNull("pot do cilja: konec nadaljevanja", r.next(-14, 4, -60, RANGE));
        assertEquals(1L, PathResume.arrived);
        assertEquals(0L, PathResume.noProgress);
        PathResume.recordWide(true, 2500L);
        PathResume.recordWide(false, 1500L);
        assertTrue(PathResume.counters().endsWith("nadSirokih=2 nadSirokihNapredek=1 nadSirokihUs=4"));
    }
}
