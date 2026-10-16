package local.customnpcs;

import static org.junit.Assert.assertEquals;

import noppes.npcs.rework.diag.FrameStats;

import org.junit.Test;

/** M5-K.0: definicije casa slicice, FPS in "x % low" so zaklenjene na rocno izracunane primere. */
public class FrameStatsTest {
    private static final long MS = 1000000L;

    @Test
    public void constantFramesGiveSameValueEverywhere() {
        long[] f = new long[1000];
        java.util.Arrays.fill(f, 10 * MS);
        FrameStats s = FrameStats.of(f, f.length);
        assertEquals(1000, s.count);
        assertEquals(10 * MS, s.p50);
        assertEquals(10 * MS, s.p95);
        assertEquals(10 * MS, s.p99);
        assertEquals(10 * MS, s.max);
        assertEquals(100.0, s.fps, 1e-9);
        assertEquals(100.0, s.low1, 1e-9);
        assertEquals(100.0, s.low01, 1e-9);
    }

    @Test
    public void percentilesUseNearestRank() {
        long[] f = new long[100];
        for (int i = 0; i < 100; i++) {
            f[i] = (100 - i) * MS; // nerazvrsceno: 100 .. 1 ms
        }
        FrameStats s = FrameStats.of(f, 100);
        assertEquals(50 * MS, s.p50);
        assertEquals(95 * MS, s.p95);
        assertEquals(99 * MS, s.p99);
        assertEquals(100 * MS, s.max);
    }

    @Test
    public void lowIsAverageOfSlowestShare() {
        // 990 slicic po 10 ms, 10 po 50 ms: 1 % = 10 najpocasnejsih = 50 ms → 20 FPS;
        // 0,1 % = ceil(1) = 1 slicica = 50 ms → 20 FPS; FPS = 1000 / 10,4 s.
        long[] f = new long[1000];
        for (int i = 0; i < 1000; i++) {
            f[i] = (i % 100 == 7 ? 50 : 10) * MS;
        }
        FrameStats s = FrameStats.of(f, 1000);
        assertEquals(20.0, s.low1, 1e-9);
        assertEquals(20.0, s.low01, 1e-9);
        assertEquals(1000.0 / 10.4, s.fps, 1e-9);
        assertEquals(10 * MS, s.p95);
        assertEquals(10 * MS, s.p99); // rang ceil(0,99 * 1000) = 990, se 10 ms
        assertEquals(50 * MS, s.max);
    }

    @Test
    public void onlyFirstNSamplesCountAndInputIsNotModified() {
        long[] f = {5 * MS, 1 * MS, 999 * MS};
        FrameStats s = FrameStats.of(f, 2);
        assertEquals(2, s.count);
        assertEquals(5 * MS, s.max);
        assertEquals(5 * MS, f[0]);
        assertEquals(1 * MS, f[1]);
    }

    @Test
    public void emptyIsZero() {
        FrameStats s = FrameStats.of(new long[0], 0);
        assertEquals(0, s.count);
        assertEquals(0L, s.p95);
        assertEquals(0.0, s.fps, 0.0);
        assertEquals(0.0, s.low1, 0.0);
    }

    @Test
    public void parsesRenderedEntities() {
        assertEquals(12, FrameStats.parseRendered("E: 12/50, B: 0"));
        assertEquals(0, FrameStats.parseRendered("E: 0/0, B: 3"));
        assertEquals(-1, FrameStats.parseRendered("C: 1/2"));
        assertEquals(-1, FrameStats.parseRendered(null));
        assertEquals(-1, FrameStats.parseRendered("E: x/2"));
    }
}
