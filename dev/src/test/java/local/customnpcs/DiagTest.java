package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import noppes.npcs.rework.diag.Diag;
import noppes.npcs.rework.diag.DiagKey;
import noppes.npcs.rework.diag.DiagSnapshot;
import noppes.npcs.rework.diag.Distribution;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Prvo pravilo instrumentacije: izklopljena ne sme narediti nicesar.
 * Drugo: vklopljena mora steti tocno, tudi pod socasnostjo.
 */
public class DiagTest {

    @Before
    public void cleanSlate() {
        Diag.setEnabled(false);
        Diag.reset();
    }

    @After
    public void leaveItOff() {
        Diag.setEnabled(false);
    }

    @Test
    public void disabledInstrumentationCountsNothing() {
        DiagKey key = Diag.key("test.disabled", "klic");
        assertFalse(Diag.isEnabled());
        assertEquals(0L, Diag.start());
        for (int i = 0; i < 100; i++) {
            long t = Diag.start();
            Diag.count(key);
            Diag.stop(key, t);
        }
        Diag.tick(1000000L);
        assertEquals(0L, key.count());
        assertEquals(0L, key.nanos());
        assertEquals(0L, Diag.ticks());
    }

    @Test
    public void enabledInstrumentationCountsAndTimes() {
        DiagKey key = Diag.key("test.enabled", "klic");
        Diag.setEnabled(true);
        long t = Diag.start();
        assertTrue(t != 0L);
        busyWork();
        Diag.stop(key, t);
        assertEquals(1L, key.count());
        assertTrue("izmerjen cas mora biti pozitiven", key.nanos() > 0L);
    }

    @Test
    public void turningItOnClearsWhatThePreviousRunLeftBehind() {
        DiagKey key = Diag.key("test.rearm", "klic");
        Diag.setEnabled(true);
        Diag.count(key);
        Diag.count(key);
        assertEquals(2L, key.count());
        Diag.setEnabled(true);
        assertEquals("vklop mora zaceti pri nic", 0L, key.count());
    }

    @Test
    public void measurementStartedWhileDisabledIsDiscarded() {
        DiagKey key = Diag.key("test.straddle", "klic");
        long t = Diag.start();
        Diag.setEnabled(true);
        Diag.stop(key, t);
        assertEquals("meritev brez izhodisca se ne sme steti", 0L, key.count());
    }

    @Test
    public void measurementRunningWhenItIsTurnedOffIsDiscarded() {
        DiagKey key = Diag.key("test.straddle.off", "klic");
        Diag.setEnabled(true);
        long t = Diag.start();
        Diag.setEnabled(false);
        Diag.stop(key, t);
        assertEquals(0L, key.count());
    }

    @Test
    public void keysAreCreatedOnceAndFoundByName() {
        DiagKey first = Diag.key("test.identity", "klic");
        DiagKey second = Diag.key("test.identity", "nekaj drugega");
        assertSame(first, second);
        assertEquals("klic", first.unit());
    }

    @Test
    public void taskKeysAreCachedPerClassAndNamedAfterIt() {
        DiagKey first = Diag.taskKey(String.class);
        DiagKey second = Diag.taskKey(String.class);
        assertSame(first, second);
        assertEquals("ai.task.String", first.name());
    }

    @Test
    public void tickRecordsBothTheCounterAndTheDistribution() {
        Diag.setEnabled(true);
        Diag.tick(5000000L);
        Diag.tick(15000000L);
        assertEquals(2L, Diag.ticks());
        DiagSnapshot snapshot = Diag.snapshot();
        Distribution tickNanos = snapshot.distribution("server.tick.ns");
        assertNotNull(tickNanos);
        assertEquals(2L, tickNanos.count());
        assertTrue(tickNanos.max() >= 15000000L);
    }

    @Test
    public void countingIsExactUnderConcurrency() throws Exception {
        final DiagKey key = Diag.key("test.concurrent", "klic");
        Diag.setEnabled(true);
        final int threads = 8;
        final int perThread = 20000;
        List<Thread> workers = new ArrayList<Thread>();
        for (int i = 0; i < threads; i++) {
            Thread worker = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < perThread; j++) {
                        Diag.count(key);
                    }
                }
            });
            workers.add(worker);
            worker.start();
        }
        for (Thread worker : workers) {
            worker.join();
        }
        assertEquals((long) threads * perThread, key.count());
    }

    @Test
    public void snapshotDoesNotMoveWhileItIsBeingRead() {
        DiagKey key = Diag.key("test.frozen", "klic");
        Diag.setEnabled(true);
        Diag.count(key);
        DiagSnapshot snapshot = Diag.snapshot();
        Diag.count(key);
        Diag.count(key);
        assertEquals(1L, snapshot.row("test.frozen").count);
        assertEquals(3L, key.count());
    }

    private static void busyWork() {
        long sum = 0L;
        for (int i = 0; i < 20000; i++) {
            sum += i * 31L;
        }
        if (sum == Long.MIN_VALUE) {
            throw new IllegalStateException("nedosegljivo; prepreci, da bi JIT zanko odstranil");
        }
    }
}
