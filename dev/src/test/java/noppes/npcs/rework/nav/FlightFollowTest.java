package noppes.npcs.rework.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Test;

/**
 * M4.17 (D-085): preverba vidne crte letecega NPC-ja, brez sveta. Bloki so polne kocke v mrezi;
 * geometrija je iz prog M4.16 (vogal: krak z = -54, x -27..-8, y 4..19; labirint: hodnik visine 3,
 * strop y = 7). Hitbox NPC-ja je 0,6 x 1,9 (privzeti NPC).
 */
public class FlightFollowTest {
    private static final double W = 0.6;
    private static final double H = 1.9;

    /** Polni bloki; kvader je v trku, ce seka kocko bloka (strogo, kot AxisAlignedBB.intersects). */
    private static final class Grid implements FlightFollow.BoxTest {
        private final Set<Long> solid = new HashSet<Long>();
        int tests;

        void fill(int x0, int y0, int z0, int x1, int y1, int z1) {
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) {
                        this.solid.add(key(x, y, z));
                    }
                }
            }
        }

        private static long key(int x, int y, int z) {
            return ((long) (x & 0xFFFFF) << 40) | ((long) (y & 0xFFFFF) << 20) | (z & 0xFFFFF);
        }

        @Override
        public boolean blocked(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this.tests++;
            for (int x = (int) Math.floor(minX); x < maxX; x++) {
                for (int y = (int) Math.floor(minY); y < maxY; y++) {
                    for (int z = (int) Math.floor(minZ); z < maxZ; z++) {
                        if (this.solid.contains(key(x, y, z)) && x + 1 > minX && y + 1 > minY && z + 1 > minZ) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    /** Ali je tocka (tx, ty, tz) neposredno dosegljiva za NPC na (px, py, pz) (spodnja sredina hitboxa). */
    private static boolean direct(Grid g, double px, double py, double pz, double tx, double ty, double tz) {
        return FlightFollow.direct(px - W / 2, py, pz - W / 2, px + W / 2, py + H, pz + W / 2, tx - px, ty - py, tz - pz, g);
    }

    private static Grid corner() {
        Grid g = new Grid();
        g.fill(-60, 3, -100, 40, 3, 0); // tla (superflat y = 3)
        g.fill(-27, 4, -54, -8, 19, -54); // krak z = -54
        g.fill(-27, 4, -53, -27, 19, -44); // krak x = -27
        return g;
    }

    @After
    public void pocisti() {
        FlightFollow.setMode(FlightFollow.ORIGINAL);
        FlightFollow.reset();
    }

    @Test
    public void stikaloSprejmeSamoVeljavneNacine() {
        assertEquals(FlightFollow.ORIGINAL, FlightFollow.mode());
        assertFalse(FlightFollow.on());
        FlightFollow.setMode(FlightFollow.ON);
        assertTrue(FlightFollow.on());
        FlightFollow.setMode(2);
        assertEquals("neveljaven nacin = original", FlightFollow.ORIGINAL, FlightFollow.mode());
        FlightFollow.setMode(-1);
        assertEquals(FlightFollow.ORIGINAL, FlightFollow.mode());
    }

    @Test
    public void tockaZaStenoVogalaNiDosegljiva() {
        // Original: vsaka tocka iste visine je "dosegljiva", tudi cilj (-14, 4, -60) za krakom z = -54.
        Grid g = corner();
        assertFalse(direct(g, -12.5, 4, -51.5, -12.5, 4, -56.5));
        assertEquals("zavrnjena zaradi trka, ne razdalje", "vidKlicev=1 vidDalec=0 vidZaprtih=1 vidProstih=0",
                FlightFollow.counters());
    }

    @Test
    public void tockaObSteniPredVogalomJeDosegljiva() {
        Grid g = corner();
        assertTrue(direct(g, -12.5, 4, -51.5, -7.5, 4, -52.5));
    }

    @Test
    public void diagonalaCezRobKrakaJeZaprta() {
        // Krak sega do x = -7 (blok -8); diagonala od pred koncem do za koncem zadane rob bloka.
        Grid g = corner();
        assertFalse(direct(g, -7.5, 4, -52.5, -6.5, 4, -55.5));
        // Okoli konca kraka, ko je NPC ze desno od njega, je prosto.
        assertTrue(direct(g, -6.5, 4, -52.5, -6.5, 4, -55.5));
    }

    @Test
    public void tockaDljeOdSestihBlokovNiKandidatTudiPoProstem() {
        Grid g = corner();
        int pred = g.tests;
        assertFalse(direct(g, 100.5, 4, -48.5, 100.5, 4, -55.0));
        assertEquals("razdalja se preveri pred branjem blokov", pred, g.tests);
        assertTrue(direct(g, 100.5, 4, -48.5, 100.5, 4, -54.5));
        assertEquals("vidKlicev=2 vidDalec=1 vidZaprtih=0 vidProstih=1", FlightFollow.counters());
    }

    @Test
    public void hodnikSStropomPrepustiNPCVisine19() {
        // Labirint: notranjost y 4..6, strop y = 7. NPC visine 1,9 na y = 4 seze do 5,9.
        Grid g = new Grid();
        g.fill(10, 3, -62, 30, 3, -48);
        g.fill(10, 7, -62, 30, 7, -48);
        g.fill(14, 4, -52, 23, 6, -52); // stena z odprtino x 24..25
        assertTrue("vzdolz hodnika", direct(g, 20.5, 4, -50.5, 16.5, 4, -50.5));
        assertFalse("skozi steno hodnika", direct(g, 20.5, 4, -50.5, 20.5, 4, -53.5));
        assertTrue("skozi odprtino", direct(g, 24.5, 4, -50.5, 24.5, 4, -53.5));
        assertFalse("tocka na y = 6: glava v stropu", direct(g, 20.5, 4, -50.5, 18.5, 6, -50.5));
    }

    @Test
    public void malenkostUgreznjenNPCNeIzgubiKandidatov() {
        // Po drsenju ali pristanku je NPC lahko za stotinko v tleh; zmanjsan hitbox tega ne steje.
        Grid g = corner();
        assertTrue(direct(g, 30.5, 3.97, -48.5, 30.5, 4, -52.5));
        assertTrue("nezmanjsan hitbox je v tleh", g.blocked(30.2, 3.97, -48.8, 30.8, 5.87, -48.2));
    }

    @Test
    public void tockaNaMestuNPCJaNiKandidatKotVanilla() {
        Grid g = corner();
        assertFalse(direct(g, 100.5, 4, -48.5, 100.5, 4, -48.5));
    }

    @Test
    public void korakJeOmejenSSirino() {
        assertEquals(0.5, FlightFollow.step(0.6), 1e-9);
        assertEquals(0.3, FlightFollow.step(0.3), 1e-9);
        assertEquals(0.5, FlightFollow.step(3.0), 1e-9);
        assertEquals(0.1, FlightFollow.step(0.00001), 1e-9);
    }

    @Test
    public void tankaStenaNeZdrsneMedKoraki() {
        // Polna stena debeline 1 med koraki 0,5 pri hitboxu 0,5 (zmanjsan): noben polozaj je ne preskoci.
        Grid g = new Grid();
        g.fill(0, 4, -3, 10, 8, -3);
        for (double dz = 1.6; dz <= 5.9; dz += 0.37) {
            assertFalse("dz=" + dz, direct(g, 5.5, 4, -1.5, 5.5, 4, -1.5 - dz));
        }
    }
}
