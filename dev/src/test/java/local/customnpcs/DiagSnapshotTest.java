package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import noppes.npcs.rework.diag.Diag;
import noppes.npcs.rework.diag.DiagKey;
import noppes.npcs.rework.diag.DiagSnapshot;
import noppes.npcs.rework.diag.Distribution;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Izpis posnetka je tisto, kar bo v M2.6 sel v zapis baseline meritve, in tisto, kar bo
 * skriptiran scenarij grepal iz konzole. Oblika je zato del pogodbe, ne okras.
 */
public class DiagSnapshotTest {

    @Before
    public void cleanSlate() {
        Diag.setEnabled(false);
        Diag.reset();
        Diag.setEnabled(true);
    }

    @After
    public void leaveItOff() {
        Diag.setEnabled(false);
    }

    @Test
    public void perTickNumbersDivideByTheNumberOfTicks() {
        DiagKey key = Diag.key("test.snapshot.perTick", "klic");
        for (int i = 0; i < 400; i++) {
            Diag.count(key);
        }
        for (int i = 0; i < 100; i++) {
            Diag.tick(1000000L);
        }
        DiagSnapshot snapshot = Diag.snapshot();
        DiagSnapshot.Row row = snapshot.row("test.snapshot.perTick");
        assertEquals(400L, row.count);
        assertEquals(4.0, row.perTick(snapshot.ticks()), 0.0001);
        assertEquals(100L, snapshot.ticks());
    }

    @Test
    public void timeColumnsConvertNanosToMillisAndMicros() {
        DiagKey key = Diag.key("test.snapshot.time", "klic");
        key.record(2000000L);
        key.record(4000000L);
        for (int i = 0; i < 2; i++) {
            Diag.tick(1000000L);
        }
        DiagSnapshot snapshot = Diag.snapshot();
        DiagSnapshot.Row row = snapshot.row("test.snapshot.time");
        assertEquals(3.0, row.millisPerTick(snapshot.ticks()), 0.0001);
        assertEquals(3000.0, row.meanMicros(), 0.0001);
    }

    @Test
    public void textReportKeepsQuietAboutCountersThatNeverFired() {
        Diag.key("test.snapshot.silent", "klic");
        DiagKey loud = Diag.key("test.snapshot.loud", "klic");
        Diag.count(loud);
        Diag.tick(1000000L);
        String text = Diag.snapshot().toText();
        assertTrue(text.contains("test.snapshot.loud"));
        assertTrue("prazni stevci samo delajo hrup", !text.contains("test.snapshot.silent"));
        assertTrue(text.contains("RWDIAG posnetek"));
        assertTrue(text.contains("MSPT p95"));
    }

    @Test
    public void jsonReportCarriesCountersAndPercentiles() {
        DiagKey key = Diag.key("test.snapshot.json", "klic");
        key.record(1500000L);
        Distribution distribution = Diag.distribution("test.snapshot.dist", "ns");
        distribution.record(10L);
        distribution.record(20L);
        Diag.tick(2000000L);
        String json = Diag.snapshot().toJson();
        assertTrue(json.startsWith("{\"enabled\":true"));
        assertTrue(json.contains("\"name\":\"test.snapshot.json\",\"count\":1,\"nanos\":1500000"));
        assertTrue(json.contains("\"name\":\"test.snapshot.dist\""));
        assertTrue(json.contains("\"p99\":"));
        assertEquals("oklepaji se morajo izidi", count(json, '{'), count(json, '}'));
    }

    @Test
    public void mixedNanoAndPlainDistributionsAreLabelledDifferently() {
        Diag.distribution("test.snapshot.count", "npc").record(12L);
        Diag.distribution("test.snapshot.nanos", "ns").record(3000000L);
        Diag.tick(1000000L);
        String text = Diag.snapshot().toText();
        assertTrue(text.contains("test.snapshot.nanos (ms)"));
        assertTrue(text.contains("test.snapshot.count "));
    }

    private static int count(String value, char c) {
        int total = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == c) {
                total++;
            }
        }
        return total;
    }
}
