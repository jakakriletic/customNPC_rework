package noppes.npcs.rework.diag;

/**
 * Porazdelitev izmerjenih vrednosti z logaritemskimi kosi.
 *
 * <p>Povprecje samo po sebi ne pove nicesar o zatikanju; zanima nas p95 in p99. Hraniti
 * vse vzorce ni mogoce (pri 20 tickih na sekundo in urah merjenja), zato so vrednosti
 * razvrscene v kose: 32 podkosov na potenco dvojke, kar pomeni najvec ~3,2 % napake pri
 * odcitku percentila. Vrednosti pod 32 so shranjene tocno.
 *
 * <p>Razred je majhen (2048 longov, 16 KB) in namenoma brez odvisnosti, da je testljiv brez
 * Minecrafta. Metode so {@code synchronized}: merjenje tece na server niti, izpis pa
 * lahko pride s konzole ali druge niti.
 */
public final class Distribution {
    private static final int SUB_BITS = 5;
    private static final int SUB = 1 << SUB_BITS;
    static final int BUCKETS = 2048;

    private final String name;
    private final String unit;
    private final long[] buckets = new long[BUCKETS];
    private long count;
    private long sum;
    private long min = Long.MAX_VALUE;
    private long max = Long.MIN_VALUE;

    Distribution(String name, String unit) {
        this.name = name;
        this.unit = unit == null ? "" : unit;
    }

    public String name() {
        return this.name;
    }

    public String unit() {
        return this.unit;
    }

    public synchronized void record(long value) {
        long v = value < 0L ? 0L : value;
        this.buckets[bucketOf(v)]++;
        this.count++;
        this.sum += v;
        if (v < this.min) {
            this.min = v;
        }
        if (v > this.max) {
            this.max = v;
        }
    }

    public synchronized long count() {
        return this.count;
    }

    public synchronized long sum() {
        return this.sum;
    }

    public synchronized long min() {
        return this.count == 0L ? 0L : this.min;
    }

    public synchronized long max() {
        return this.count == 0L ? 0L : this.max;
    }

    public synchronized double mean() {
        return this.count == 0L ? 0.0 : (double) this.sum / (double) this.count;
    }

    /**
     * Zgornja meja kosa, v katerem lezi zahtevani percentil. Vrnjena vrednost je zato
     * vedno >= pravi percentil in nikoli ne presega prave vrednosti za vec kot sirino
     * kosa. {@code p} je v razponu 0..1.
     */
    public synchronized long percentile(double p) {
        if (this.count == 0L) {
            return 0L;
        }
        double clamped = p < 0.0 ? 0.0 : (p > 1.0 ? 1.0 : p);
        long target = (long) Math.ceil(clamped * (double) this.count);
        if (target < 1L) {
            target = 1L;
        }
        long seen = 0L;
        for (int i = 0; i < BUCKETS; i++) {
            seen += this.buckets[i];
            if (seen >= target) {
                long high = bucketHigh(i);
                return high > this.max ? this.max : high;
            }
        }
        return this.max;
    }

    public synchronized void reset() {
        java.util.Arrays.fill(this.buckets, 0L);
        this.count = 0L;
        this.sum = 0L;
        this.min = Long.MAX_VALUE;
        this.max = Long.MIN_VALUE;
    }

    public synchronized Distribution copy() {
        Distribution other = new Distribution(this.name, this.unit);
        System.arraycopy(this.buckets, 0, other.buckets, 0, BUCKETS);
        other.count = this.count;
        other.sum = this.sum;
        other.min = this.min;
        other.max = this.max;
        return other;
    }

    static int bucketOf(long value) {
        if (value < SUB) {
            return (int) value;
        }
        int msb = 63 - Long.numberOfLeadingZeros(value);
        int shift = msb - SUB_BITS;
        int sub = (int) ((value >>> shift) & (SUB - 1));
        return ((shift + 1) << SUB_BITS) + sub;
    }

    static long bucketHigh(int index) {
        if (index < SUB) {
            return index;
        }
        int shift = (index >>> SUB_BITS) - 1;
        int sub = index & (SUB - 1);
        long low = ((long) (SUB + sub)) << shift;
        return low + (1L << shift) - 1L;
    }

    @Override
    public String toString() {
        return this.name + "{n=" + this.count() + ", p95=" + this.percentile(0.95) + this.unit + "}";
    }
}
