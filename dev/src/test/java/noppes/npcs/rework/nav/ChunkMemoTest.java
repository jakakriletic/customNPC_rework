package noppes.npcs.rework.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

/**
 * M5-S S14: predpomnilnik chunkov mora za vsako koordinato vrniti natanko objekt, ki bi ga vrnil
 * nalagalnik (v igri {@code World.getChunkFromChunkCoords}), tudi pri negativnih koordinatah,
 * trkih rez in po {@code reset}. Nalagalnik tu vsakic ustvari nov objekt, zato bi napacna reza
 * ali zastarel vnos takoj vrnila napacen (ne isti) objekt.
 */
public class ChunkMemoTest {
    /** Nalagalnik, ki steje klice in hrani "trenutno nalozen" objekt po koordinati. */
    private static final class Svet implements ChunkMemo.Loader<Object> {
        final Map<Long, Object> nalozeni = new HashMap<Long, Object>();
        int klicev;

        @Override
        public Object load(int cx, int cz) {
            ++this.klicev;
            Long k = ((long) cx << 32) ^ (cz & 0xffffffffL);
            Object o = this.nalozeni.get(k);
            if (o == null) {
                o = new Object();
                this.nalozeni.put(k, o);
            }
            return o;
        }

        /** "Raztovori" chunk: naslednji load vrne nov objekt. */
        void raztovori(int cx, int cz) {
            this.nalozeni.remove(((long) cx << 32) ^ (cz & 0xffffffffL));
        }
    }

    @Test
    public void nakljucnaZaporedjaVracajoObjektNalagalnika() {
        Random r = new Random(14);
        Svet svet = new Svet();
        ChunkMemo<Object> memo = new ChunkMemo<Object>(svet);
        for (int klic = 0; klic < 2000; klic++) {
            memo.reset();
            int ox = r.nextInt(8) - 4;  // majhno okno: isti chunki se vracajo po raztovoru
            int oz = r.nextInt(8) - 4;
            for (int i = 0; i < 300; i++) {
                int cx = ox + r.nextInt(5) - 2;
                int cz = oz + r.nextInt(5) - 2;
                Object dobljen = memo.get(cx, cz);
                assertSame(svet.nalozeni.get(((long) cx << 32) ^ (cz & 0xffffffffL)), dobljen);
            }
            // Med klici se chunki raztovarjajo; po reset() mora memo vrniti novega.
            svet.raztovori(ox, oz);
        }
    }

    @Test
    public void sosednjiChunkiSeNeIzrivajo() {
        Svet svet = new Svet();
        ChunkMemo<Object> memo = new ChunkMemo<Object>(svet);
        int[][] kvadrat = {{-1, -1}, {-1, 0}, {0, -1}, {0, 0}};
        for (int ponovi = 0; ponovi < 50; ponovi++) {
            for (int[] c : kvadrat) {
                memo.get(c[0], c[1]);
            }
        }
        assertEquals("vsak od stirih sosednjih chunkov se isce enkrat", 4, svet.klicev);
        assertEquals(200, memo.lookups());
        assertEquals(4, memo.misses());
    }

    @Test
    public void resetPozabiInNeDrziStarihObjektov() {
        Svet svet = new Svet();
        ChunkMemo<Object> memo = new ChunkMemo<Object>(svet);
        Object a = memo.get(3, -7);
        memo.reset();
        svet.raztovori(3, -7);
        Object b = memo.get(3, -7);
        assertTrue("po reset in raztovoru mora priti nov objekt", a != b);
        assertEquals(2, svet.klicev);
    }

    @Test
    public void trkRezeNeVrneTujegaChunka() {
        Svet svet = new Svet();
        ChunkMemo<Object> memo = new ChunkMemo<Object>(svet);
        // (0,0) in (2,0) imata isto parnost -> ista reza.
        Object a = memo.get(0, 0);
        Object b = memo.get(2, 0);
        assertTrue(a != b);
        assertSame(a, memo.get(0, 0));
        assertSame(b, memo.get(2, 0));
        assertEquals(4, svet.klicev);
    }
}
