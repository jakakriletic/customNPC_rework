package noppes.npcs.rework.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.minecraft.util.math.MathHelper;
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
        assertEquals("zavrnjena zaradi trka, ne razdalje", "vidKlicev=1 vidDalec=0 vidZaprtih=1 vidProstih=0 vidUnija=0 vidCelic=0",
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
        assertEquals("vidKlicev=2 vidDalec=1 vidZaprtih=0 vidProstih=1 vidUnija=1 vidCelic=0", FlightFollow.counters());
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

    // M4.17 poskus 2 (Q83): unija pred koraki mora dati isti izid kot sami koraki (poskus 1).

    /** Izid poskusa 1: zmanjsan hitbox in samo koraki, brez unije. */
    private static boolean steps(Grid g, double px, double py, double pz, double tx, double ty, double tz) {
        double s = FlightFollow.SHRINK;
        return FlightFollow.steps(px - W / 2 + s, py + s, pz - W / 2 + s, px + W / 2 - s, py + H - s, pz + W / 2 - s,
                W, tx - px, ty - py, tz - pz, g);
    }

    private static boolean sweep(Grid g, double px, double py, double pz, double tx, double ty, double tz) {
        return FlightFollow.sweep(px - W / 2, py, pz - W / 2, px + W / 2, py + H, pz + W / 2, tx - px, ty - py, tz - pz, g);
    }

    @Test
    public void prostaUnijaJeEnaPreverba() {
        Grid g = corner();
        int pred = g.tests;
        assertTrue(direct(g, 100.5, 4, -48.5, 104.5, 4.2, -52.5));
        assertEquals("po prostem samo unija", pred + 1, g.tests);
        assertEquals("vidKlicev=1 vidDalec=0 vidZaprtih=0 vidProstih=1 vidUnija=1 vidCelic=0", FlightFollow.counters());
    }

    @Test
    public void zaprtaUnijaPreideNaKorakeInNeZavrnePreuranjeno() {
        // NPC je 0,1 v tleh (zmanjsan hitbox se jih se dotika): unija z zacetkom je zaprta,
        // koraki navzgor pa ne, zato je izid prost kot pri poskusu 1.
        Grid g = corner();
        assertTrue(steps(g, 30.5, 3.9, -48.5, 30.5, 4.9, -48.5));
        int pred = g.tests;
        assertTrue(direct(g, 30.5, 3.9, -48.5, 30.5, 4.9, -48.5));
        assertTrue("unija in vsaj en korak", g.tests >= pred + 2);
        assertEquals("vidKlicev=1 vidDalec=0 vidZaprtih=0 vidProstih=1 vidUnija=0 vidCelic=0", FlightFollow.counters());
    }

    @Test
    public void unijaDaIstiIzidKotKorakiNaProgahM416() {
        Grid g = corner();
        g.fill(13, 3, -61, 27, 7, -61); // labirint: lupina 13..27 x 3..7 x -61..-49 (tla so ze v corner())
        g.fill(13, 3, -49, 27, 7, -49);
        g.fill(13, 7, -61, 27, 7, -49);
        g.fill(13, 4, -61, 13, 6, -49);
        g.fill(27, 4, -61, 27, 6, -49);
        g.fill(14, 4, -52, 23, 6, -52);
        g.fill(17, 4, -55, 26, 6, -55);
        g.fill(14, 4, -58, 23, 6, -58);
        g.fill(-106, 4, -56, -34, 7, -56); // zid
        Random rnd = new Random(41700L);
        double[][] obmocja = { { -30, -5, 4, 9, -58, -45 }, { 14, 26, 4, 5, -60, -50 }, { -60, -40, 4, 9, -59, -53 } };
        int prostih = 0;
        int zaprtih = 0;
        for (int i = 0; i < 6000; i++) {
            double[] o = obmocja[i % obmocja.length];
            double px = o[0] + rnd.nextDouble() * (o[1] - o[0]);
            double py = o[2] + rnd.nextDouble() * (o[3] - o[2]);
            double pz = o[4] + rnd.nextDouble() * (o[5] - o[4]);
            double tx = px + (rnd.nextDouble() * 2 - 1) * 4.5;
            double ty = py + (rnd.nextInt(3) - 1) * rnd.nextDouble();
            double tz = pz + (rnd.nextDouble() * 2 - 1) * 4.5;
            boolean pricakovano = steps(g, px, py, pz, tx, ty, tz);
            assertEquals("i=" + i, pricakovano, sweep(g, px, py, pz, tx, ty, tz));
            if (pricakovano) {
                prostih++;
            } else {
                zaprtih++;
            }
        }
        assertTrue("vzorec ima proste in zaprte: " + prostih + "/" + zaprtih, prostih > 200 && zaprtih > 200);
    }

    // M4.17 poskus 3 (Q85): FlightFollow.scan = zanka World.getCollisionBoxes brez dogodka v celici.

    /** Celice, ki si zapomnijo vrstni red branja; zaprta je samo celica {@code stop}. */
    private static final class Trace implements FlightFollow.Cells {
        final List<String> read = new ArrayList<String>();
        final Set<Long> unloaded = new HashSet<Long>();
        String stop;

        @Override
        public boolean loaded(int x, int z) {
            return !this.unloaded.contains(((long) x << 32) ^ (z & 0xFFFFFFFFL));
        }

        @Override
        public boolean blocked(int x, int y, int z) {
            String c = x + "," + y + "," + z;
            this.read.add(c);
            return c.equals(this.stop);
        }
    }

    /** Prepis Forge 14.23.5 World:1413-1486 pri entity = null in p_191504_3_ = true, brez dogodka. */
    private static boolean original(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Trace c) {
        int i = MathHelper.floor(minX) - 1;
        int j = MathHelper.ceil(maxX) + 1;
        int k = MathHelper.floor(minY) - 1;
        int l = MathHelper.ceil(maxY) + 1;
        int i1 = MathHelper.floor(minZ) - 1;
        int j1 = MathHelper.ceil(maxZ) + 1;
        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = i1; l1 < j1; ++l1) {
                boolean flag2 = k1 == i || k1 == j - 1;
                boolean flag3 = l1 == i1 || l1 == j1 - 1;
                if ((!flag2 || !flag3) && c.loaded(k1, l1)) {
                    for (int i2 = k; i2 < l; ++i2) {
                        if (!flag2 && !flag3 || i2 != l - 1) {
                            if (k1 < -30000000 || k1 >= 30000000 || l1 < -30000000 || l1 >= 30000000) {
                                return true;
                            }
                            if (c.blocked(k1, i2, l1)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Test
    public void scanPrebereIsteCeliceVIstemVrstnemReduKotOriginal() {
        Random rnd = new Random(41730L);
        for (int n = 0; n < 3000; n++) {
            double x = (rnd.nextInt(41) - 20) + (n % 3 == 0 ? 0 : rnd.nextDouble());
            double y = rnd.nextInt(10) + (n % 5 == 0 ? 0 : rnd.nextDouble());
            double z = (rnd.nextInt(41) - 20) + (n % 7 == 0 ? 0 : rnd.nextDouble());
            double sx = n % 11 == 0 ? 0 : rnd.nextDouble() * 7;
            double sy = rnd.nextDouble() * 3;
            double sz = rnd.nextDouble() * 7;
            Trace a = new Trace();
            Trace b = new Trace();
            if (n % 4 == 0) {
                int ux = (int) Math.floor(x) + rnd.nextInt(3);
                int uz = (int) Math.floor(z) + rnd.nextInt(3);
                a.unloaded.add(((long) ux << 32) ^ (uz & 0xFFFFFFFFL));
                b.unloaded.addAll(a.unloaded);
            }
            assertEquals(original(x, y, z, x + sx, y + sy, z + sz, a), FlightFollow.scan(x, y, z, x + sx, y + sy, z + sz, b));
            assertEquals("n=" + n, a.read, b.read);
            if (!a.read.isEmpty()) {
                // Zgodnji izhod: zaprta celica iz sredine branja.
                String stop = a.read.get(rnd.nextInt(a.read.size()));
                Trace c = new Trace();
                Trace d = new Trace();
                c.stop = stop;
                d.stop = stop;
                c.unloaded.addAll(a.unloaded);
                d.unloaded.addAll(a.unloaded);
                assertTrue(original(x, y, z, x + sx, y + sy, z + sz, c));
                assertTrue(FlightFollow.scan(x, y, z, x + sx, y + sy, z + sz, d));
                assertEquals("n=" + n, c.read, d.read);
            }
        }
    }

    @Test
    public void scanZaMejoSvetaJeZaprtBrezBranja() {
        Trace t = new Trace();
        assertTrue(FlightFollow.scan(29999999.2, 4, 0.2, 29999999.8, 5.9, 0.8, t));
        assertTrue(original(29999999.2, 4, 0.2, 29999999.8, 5.9, 0.8, new Trace()));
        assertFalse(t.read.isEmpty());
        assertTrue(t.read.get(t.read.size() - 1).startsWith("29999999,"));
    }

    /**
     * M4.17 poskus 4 (Q86): navigator si v {@code loaded} zapomni chunk stolpca in ga uporabi v
     * {@code blocked}; vsaka prebrana celica mora biti v stolpcu zadnjega potrjenega {@code loaded}.
     */
    @Test
    public void scanBereCeliceSamoIzZadnjegaPotrjenegaStolpca() {
        Random rnd = new Random(41740L);
        for (int n = 0; n < 3000; n++) {
            final double x = (rnd.nextInt(41) - 20) + rnd.nextDouble();
            final double y = rnd.nextInt(10) + rnd.nextDouble();
            final double z = (rnd.nextInt(41) - 20) + rnd.nextDouble();
            final Set<Long> unloaded = new HashSet<Long>();
            unloaded.add(((long) ((int) Math.floor(x) + rnd.nextInt(3)) << 32) ^ (((int) Math.floor(z) + rnd.nextInt(3)) & 0xFFFFFFFFL));
            final int[] last = new int[2];
            final boolean[] any = new boolean[1];
            final int[] cells = new int[1];
            FlightFollow.Cells c = new FlightFollow.Cells() {
                @Override
                public boolean loaded(int cx, int cz) {
                    if (unloaded.contains(((long) cx << 32) ^ (cz & 0xFFFFFFFFL))) {
                        any[0] = false;
                        return false;
                    }
                    last[0] = cx;
                    last[1] = cz;
                    any[0] = true;
                    return true;
                }

                @Override
                public boolean blocked(int cx, int cy, int cz) {
                    assertTrue(any[0]);
                    assertEquals(last[0], cx);
                    assertEquals(last[1], cz);
                    cells[0]++;
                    return false;
                }
            };
            assertFalse(FlightFollow.scan(x, y, z, x + rnd.nextDouble() * 7, y + rnd.nextDouble() * 3, z + rnd.nextDouble() * 7, c));
            assertTrue(cells[0] > 0);
        }
    }

    @Test
    public void scanSamoPrebereCeliceInJihPresteje() {
        Trace t = new Trace();
        // Brez blokov ni skatle: scan prebere vse celice in vrne prosto.
        assertFalse(FlightFollow.scan(0.2, 10.1, 0.2, 0.8, 11.9, 0.8, t));
        // 3 x 3 stolpci brez 4 kotov = 5 stolpcev; y od 9 do 12 (4 plasti), na robu brez zgornje.
        assertEquals("vidKlicev=0 vidDalec=0 vidZaprtih=0 vidProstih=0 vidUnija=0 vidCelic=" + (1 * 4 + 4 * 3),
                FlightFollow.counters());
    }
}
