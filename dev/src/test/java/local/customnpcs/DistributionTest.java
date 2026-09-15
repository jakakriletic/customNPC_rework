package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import noppes.npcs.rework.diag.Distribution;
import noppes.npcs.rework.diag.Diag;

import org.junit.Test;

/**
 * Porazdelitev je edini del instrumentacije, ki vrednosti izgublja (kosi namesto vzorcev).
 * Ti testi zaklenejo, koliko jih sme izgubiti.
 */
public class DistributionTest {

    @Test
    public void bucketBoundNeverUnderstatesTheValue() {
        Distribution d = Diag.distribution("test.bound", "");
        d.reset();
        for (long value = 0L; value < 5000L; value++) {
            d.reset();
            d.record(value);
            assertTrue("p100 >= vrednost pri " + value, d.percentile(1.0) >= value);
        }
    }

    @Test
    public void bucketErrorStaysUnderFourPercent() {
        Distribution d = Diag.distribution("test.error", "");
        for (long value = 32L; value < 2000000L; value = value + 1L + value / 7L) {
            d.reset();
            d.record(value);
            long reported = d.percentile(0.999);
            double error = (double) (reported - value) / (double) value;
            assertTrue("napaka " + error + " pri " + value, error >= 0.0 && error < 0.04);
        }
    }

    @Test
    public void percentilesFollowAKnownDistribution() {
        Distribution d = Diag.distribution("test.percentiles", "");
        d.reset();
        for (int i = 1; i <= 1000; i++) {
            d.record(i);
        }
        assertEquals(1000L, d.count());
        assertEquals(1L, d.min());
        assertEquals(1000L, d.max());
        assertEquals(500.5, d.mean(), 0.001);
        assertWithin(500L, d.percentile(0.50));
        assertWithin(950L, d.percentile(0.95));
        assertWithin(990L, d.percentile(0.99));
        assertEquals(1000L, d.percentile(1.0));
    }

    @Test
    public void percentileNeverExceedsTheLargestSample() {
        Distribution d = Diag.distribution("test.cap", "");
        d.reset();
        d.record(1000L);
        assertEquals(1000L, d.percentile(0.5));
        assertEquals(1000L, d.percentile(1.0));
    }

    @Test
    public void emptyDistributionReportsZeroInsteadOfThrowing() {
        Distribution d = Diag.distribution("test.empty", "");
        d.reset();
        assertEquals(0L, d.count());
        assertEquals(0L, d.min());
        assertEquals(0L, d.max());
        assertEquals(0L, d.percentile(0.95));
        assertEquals(0.0, d.mean(), 0.0);
    }

    @Test
    public void negativeSamplesAreClampedNotDropped() {
        Distribution d = Diag.distribution("test.negative", "");
        d.reset();
        d.record(-5L);
        assertEquals(1L, d.count());
        assertEquals(0L, d.min());
    }

    @Test
    public void copyIsIndependentOfTheOriginal() {
        Distribution d = Diag.distribution("test.copy", "");
        d.reset();
        d.record(10L);
        Distribution copy = d.copy();
        d.record(1000L);
        assertEquals(1L, copy.count());
        assertEquals(2L, d.count());
    }

    @Test
    public void extremeValuesStayInsideTheBucketArray() {
        Distribution d = Diag.distribution("test.extreme", "");
        d.reset();
        d.record(Long.MAX_VALUE);
        d.record(1L);
        assertEquals(2L, d.count());
        assertEquals(Long.MAX_VALUE, d.max());
        assertEquals(Long.MAX_VALUE, d.percentile(1.0));
    }

    private static void assertWithin(long expected, long reported) {
        assertTrue("pricakovano ~" + expected + ", dobljeno " + reported,
                reported >= expected && reported <= expected * 1.04 + 1);
    }
}
