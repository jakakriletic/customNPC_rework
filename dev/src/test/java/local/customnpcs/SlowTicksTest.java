package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import noppes.npcs.rework.diag.Diag;
import noppes.npcs.rework.diag.DiagSnapshot;
import noppes.npcs.rework.diag.Distribution;
import noppes.npcs.rework.diag.SlowTicks;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tabela najpocasnejsih tickov je edini del posnetka, ki odgovori na vprasanje "zakaj".
 * Ce zgresi tick ali izgubi kontekst, se prepad med p95 in p99 pripise napacnemu vzroku,
 * kar je slabse od tega, da ga ne bi pripisali.
 */
public class SlowTicksTest {

    private static final long MS = 1000000L;

    @Before
    public void cleanSlate() {
        Diag.setEnabled(false);
        Diag.reset();
    }

    @After
    public void leaveItOff() {
        Diag.setEnabled(false);
    }

    private static SlowTicks small() {
        return new SlowTicks(3, new long[] {10 * MS, 25 * MS, 50 * MS});
    }

    private static void tick(SlowTicks slow, long index, long millis) {
        slow.record(index, index * 50L, millis * MS, 8, 0, 0, 0);
    }

    @Test
    public void keepsOnlyTheSlowestTicksAndOrdersThemFromSlowest() {
        SlowTicks slow = small();
        long[] millis = {1L, 40L, 3L, 90L, 2L, 12L};
        for (int i = 0; i < millis.length; i++) {
            tick(slow, i, millis[i]);
        }
        List<SlowTicks.Entry> entries = slow.entries();
        assertEquals(3, entries.size());
        assertEquals(90.0, entries.get(0).millis(), 0.0001);
        assertEquals(40.0, entries.get(1).millis(), 0.0001);
        assertEquals(12.0, entries.get(2).millis(), 0.0001);
        assertEquals(3L, entries.get(0).tickIndex);
        assertEquals(1L, entries.get(1).tickIndex);
        assertEquals(5L, entries.get(2).tickIndex);
        assertEquals(6L, slow.recorded());
    }

    @Test
    public void slowestNanosMatchesTheSlowestEntry() {
        SlowTicks slow = small();
        tick(slow, 0L, 5L);
        tick(slow, 1L, 77L);
        assertEquals(77L * MS, slow.slowestNanos());
    }

    @Test
    public void emptyTableHasNoSlowestTick() {
        assertEquals(0L, small().slowestNanos());
        assertTrue(small().entries().isEmpty());
    }

    @Test
    public void tieKeepsTheEarlierTick() {
        SlowTicks slow = new SlowTicks(1, new long[] {10 * MS});
        tick(slow, 7L, 30L);
        tick(slow, 8L, 30L);
        assertEquals(1, slow.entries().size());
        assertEquals(7L, slow.entries().get(0).tickIndex);
    }

    /**
     * Prva izvedba je stela ravni od najvisje navzdol in se ustavila pri prvi, ki je tick
     * ni dosegel — 30 ms tick torej ni bil stet niti pri 10 ms. Ta test je tisti, ki je
     * napako ujel.
     */
    @Test
    public void tickOverALevelCountsInEveryLowerLevelToo() {
        SlowTicks slow = small();
        tick(slow, 0L, 30L);
        assertEquals(1L, slow.overCount(0));
        assertEquals(1L, slow.overCount(1));
        assertEquals(0L, slow.overCount(2));
    }

    @Test
    public void levelCountsAreInclusiveAndIndependentOfTheTable() {
        SlowTicks slow = small();
        for (int i = 0; i < 40; i++) {
            tick(slow, i, 60L);
        }
        assertEquals(3, slow.entries().size());
        assertEquals(40L, slow.overCount(0));
        assertEquals(40L, slow.overCount(1));
        assertEquals(40L, slow.overCount(2));
        assertEquals(40L, slow.recorded());
    }

    @Test
    public void levelBoundaryCountsAsReached() {
        SlowTicks slow = small();
        tick(slow, 0L, 10L);
        assertEquals(1L, slow.overCount(0));
        assertEquals(0L, slow.overCount(1));
    }

    @Test
    public void fastTicksAreCountedButNotKept() {
        SlowTicks slow = small();
        for (int i = 0; i < 1000; i++) {
            slow.record(i, i, 500000L, 8, 0, 0, 0);
        }
        assertEquals(1000L, slow.recorded());
        assertEquals(0L, slow.overCount(0));
        assertEquals(3, slow.entries().size());
        assertEquals(0.5, slow.entries().get(0).millis(), 0.0001);
    }

    @Test
    public void negativeDurationBecomesZero() {
        SlowTicks slow = small();
        slow.record(0L, -5L, -1000L, 8, 0, 0, 0);
        assertEquals(0L, slow.entries().get(0).durationNanos);
        assertEquals(0L, slow.entries().get(0).offsetMillis);
    }

    @Test
    public void contextIsKeptPerTick() {
        SlowTicks slow = small();
        slow.record(11L, 2500L, 81 * MS, 8, 12, 3, 1);
        SlowTicks.Entry entry = slow.entries().get(0);
        assertEquals(11L, entry.tickIndex);
        assertEquals(2500L, entry.offsetMillis);
        assertEquals(8, entry.npcs);
        assertEquals(12, entry.chunkLoads);
        assertEquals(3, entry.chunkUnloads);
        assertEquals(1, entry.saves);
    }

    @Test
    public void resetClearsTableAndCounters() {
        SlowTicks slow = small();
        tick(slow, 0L, 60L);
        slow.reset();
        assertTrue(slow.entries().isEmpty());
        assertEquals(0L, slow.recorded());
        assertEquals(0L, slow.overCount(0));
        assertEquals("", slow.toText());
    }

    @Test
    public void copyDoesNotChangeWithTheOriginal() {
        SlowTicks slow = small();
        tick(slow, 0L, 60L);
        SlowTicks copy = slow.copy();
        tick(slow, 1L, 90L);
        assertEquals(1, copy.entries().size());
        assertEquals(60.0, copy.entries().get(0).millis(), 0.0001);
        assertEquals(1L, copy.recorded());
        assertEquals(2, slow.entries().size());
    }

    @Test
    public void emptyTableRendersNothing() {
        assertEquals("", small().toText());
    }

    @Test
    public void textTableNamesTheTickAndItsContext() {
        SlowTicks slow = small();
        slow.record(1183L, 59200L, 269019000L, 8, 25, 0, 1);
        String text = slow.toText();
        assertTrue(text.contains("najpocasnejsi ticki (1 od 1)"));
        assertTrue(text.contains("1183"));
        assertTrue(text.contains("59.2"));
        assertTrue(text.contains("269.019"));
        assertTrue(text.contains("ticki nad ravnijo: 10ms=1 25ms=1 50ms=1"));
    }

    @Test
    public void unknownContextRendersAsDash() {
        SlowTicks slow = small();
        slow.record(SlowTicks.UNKNOWN, 0L, 60 * MS, SlowTicks.UNKNOWN, SlowTicks.UNKNOWN,
                SlowTicks.UNKNOWN, SlowTicks.UNKNOWN);
        assertTrue(slow.toText().contains("-"));
        assertFalse(slow.toText().contains("-1"));
    }

    @Test
    public void jsonCarriesLevelsAndSlowestTicks() {
        SlowTicks slow = small();
        slow.record(5L, 250L, 81789000L, 8, 12, 0, 0);
        String json = slow.toJson();
        assertTrue(json.contains("\"recorded\":1"));
        assertTrue(json.contains("\"levels\":[{\"ns\":10000000,\"count\":1}"));
        assertTrue(json.contains("\"tick\":5"));
        assertTrue(json.contains("\"nanos\":81789000"));
        assertTrue(json.contains("\"chunkLoads\":12"));
    }

    @Test
    public void capacityBelowOneIsRejected() {
        try {
            new SlowTicks(0, new long[] {MS});
            fail("pricakovana IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("capacity"));
        }
    }

    @Test
    public void levelsMustBeAscending() {
        try {
            new SlowTicks(4, new long[] {25 * MS, 10 * MS});
            fail("pricakovana IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("narascajoce"));
        }
    }

    @Test
    public void levelsCannotBeEmpty() {
        try {
            new SlowTicks(4, new long[0]);
            fail("pricakovana IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("proracuna"));
        }
    }

    /**
     * Pripis brez povezave s porazdelitvijo ni dokaz. Najpocasnejsi tick v tabeli mora
     * biti isti tick kot {@code max} porazdelitve {@code server.tick.ns}; ce nista, meri
     * vsak svoje.
     */
    @Test
    public void snapshotTableAgreesWithTheTickDistribution() {
        Diag.setEnabled(true);
        for (int i = 0; i < 50; i++) {
            Diag.tick(400000L, i, 8, 0, 0, 0);
        }
        Diag.tick(81789000L, 50L, 8, 12, 0, 0);
        Diag.tick(1200000L, 51L, 8, 0, 0, 1);

        DiagSnapshot snapshot = Diag.snapshot();
        Distribution tick = snapshot.distribution("server.tick.ns");
        assertEquals(52L, tick.count());
        assertEquals(81789000L, tick.max());
        assertEquals(81789000L, snapshot.slowTicks().slowestNanos());
        assertEquals(50L, snapshot.slowTicks().entries().get(0).tickIndex);
        assertEquals(12, snapshot.slowTicks().entries().get(0).chunkLoads);
        assertEquals(52L, snapshot.slowTicks().recorded());

        String text = snapshot.toText();
        assertTrue(text.contains("najpocasnejsi ticki"));
        assertTrue(text.contains("81.789"));
        assertTrue(snapshot.toJson().contains("\"slowTicks\":"));
    }

    @Test
    public void disabledInstrumentationRecordsNoSlowTicks() {
        Diag.setEnabled(false);
        Diag.tick(500 * MS, 1L, 8, 0, 0, 0);
        assertTrue(Diag.slowTicks().entries().isEmpty());
        assertEquals(0L, Diag.slowTicks().recorded());
    }

    @Test
    public void enablingClearsTheTableOfThePreviousMeasurement() {
        Diag.setEnabled(true);
        Diag.tick(90 * MS, 1L, 8, 0, 0, 0);
        assertEquals(1, Diag.slowTicks().entries().size());
        Diag.setEnabled(false);
        Diag.setEnabled(true);
        assertTrue(Diag.slowTicks().entries().isEmpty());
        assertEquals(0L, Diag.slowTicks().recorded());
    }
}
