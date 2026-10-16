package noppes.npcs.rework.diag;

import java.util.Arrays;

/**
 * Statistika casov slicic klienta (M5-K.0, harness M2.4r).
 *
 * <p><b>Zakaj tocno, ne {@link Distribution}.</b> Za "1 % low" in "0,1 % low" je treba
 * povprecje najpocasnejsih slicic, kar kosi ne dajo. V 60 s je slicic najvec nekaj deset
 * tisoc, zato se hranijo vse (long na slicico).
 *
 * <p>Definicije (enake kot v orodjih za merjenje iger, npr. CapFrameX/PresentMon):
 * <ul>
 *   <li>percentil casa slicice: najblizji rang, {@code ceil(p * n)}-ta najmanjsa vrednost</li>
 *   <li>FPS: stevilo slicic / vsota casov</li>
 *   <li>{@code x % low}: 1e9 / povprecje najpocasnejsih {@code ceil(x * n)} slicic</li>
 * </ul>
 *
 * <p>Brez Minecraft tipov, testljivo brez klienta.
 */
public final class FrameStats {
    public final int count;
    public final long sumNanos;
    public final long p50;
    public final long p95;
    public final long p99;
    public final long max;
    public final double fps;
    public final double low1;
    public final double low01;

    private FrameStats(long[] sorted) {
        this.count = sorted.length;
        long s = 0L;
        for (long v : sorted) {
            s += v;
        }
        this.sumNanos = s;
        this.p50 = percentile(sorted, 0.50);
        this.p95 = percentile(sorted, 0.95);
        this.p99 = percentile(sorted, 0.99);
        this.max = sorted.length == 0 ? 0L : sorted[sorted.length - 1];
        this.fps = s <= 0L ? 0.0 : sorted.length * 1.0e9 / s;
        this.low1 = low(sorted, 0.01);
        this.low01 = low(sorted, 0.001);
    }

    /** Izracun iz prvih {@code n} vzorcev; tabela se ne spremeni. */
    public static FrameStats of(long[] frames, int n) {
        if (n < 0 || n > frames.length) {
            throw new IllegalArgumentException("n=" + n);
        }
        long[] sorted = Arrays.copyOf(frames, n);
        Arrays.sort(sorted);
        return new FrameStats(sorted);
    }

    static long percentile(long[] sorted, double p) {
        if (sorted.length == 0) {
            return 0L;
        }
        int rank = (int) Math.ceil(p * sorted.length);
        if (rank < 1) {
            rank = 1;
        }
        return sorted[rank - 1];
    }

    /** Stevilo upodobljenih entitet iz {@code RenderGlobal.getDebugInfoEntities()}: "E: 12/50, B: 0" → 12; -1, ce oblika ni znana. */
    public static int parseRendered(String s) {
        if (s == null || !s.startsWith("E: ")) {
            return -1;
        }
        int slash = s.indexOf('/');
        if (slash < 0) {
            return -1;
        }
        try {
            return Integer.parseInt(s.substring(3, slash).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    static double low(long[] sorted, double share) {
        if (sorted.length == 0) {
            return 0.0;
        }
        int k = (int) Math.ceil(share * sorted.length);
        if (k < 1) {
            k = 1;
        }
        long s = 0L;
        for (int i = sorted.length - k; i < sorted.length; i++) {
            s += sorted[i];
        }
        return s <= 0L ? 0.0 : k * 1.0e9 / s;
    }
}
