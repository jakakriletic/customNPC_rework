package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import noppes.npcs.rework.diag.Diag;
import noppes.npcs.rework.diag.DiagSnapshot;
import noppes.npcs.rework.diag.JvmProbe;

import org.junit.After;
import org.junit.Test;

/**
 * M2.6: GC in alokacije v oknu meritve. Merijo se prave JVM stevilke, zato testi
 * preverjajo spodnje meje in razmerja, ne tocnih vrednosti.
 */
public class JvmProbeTest {

    private static final long MB = 1024L * 1024L;

    /** Da JIT alokacije ne odstrani. */
    private static volatile Object sink;

    @After
    public void leaveItOff() {
        Diag.setEnabled(false);
    }

    @Test
    public void oldCollectorsAreRecognisedByHotSpotNames() {
        assertTrue(JvmProbe.isOldCollector("PS MarkSweep"));
        assertTrue(JvmProbe.isOldCollector("G1 Old Generation"));
        assertTrue(JvmProbe.isOldCollector("ConcurrentMarkSweep"));
        assertTrue(JvmProbe.isOldCollector("MarkSweepCompact"));
        assertFalse(JvmProbe.isOldCollector("PS Scavenge"));
        assertFalse(JvmProbe.isOldCollector("G1 Young Generation"));
        assertFalse(JvmProbe.isOldCollector("ParNew"));
        assertFalse(JvmProbe.isOldCollector("Copy"));
        assertFalse(JvmProbe.isOldCollector(null));
    }

    @Test
    public void oldPoolsAreRecognisedByHotSpotNames() {
        assertTrue(JvmProbe.isOldPool("PS Old Gen"));
        assertTrue(JvmProbe.isOldPool("G1 Old Gen"));
        assertTrue(JvmProbe.isOldPool("Tenured Gen"));
        assertTrue(JvmProbe.isOldPool("CMS Old Gen"));
        assertFalse(JvmProbe.isOldPool("PS Eden Space"));
        assertFalse(JvmProbe.isOldPool("Metaspace"));
    }

    @Test
    public void counterThatWentBackwardsIsZeroNotNegative() {
        assertEquals(0L, JvmProbe.delta(5L, 9L));
        assertEquals(4L, JvmProbe.delta(9L, 5L));
    }

    /** Posnetek brez vklopa ne sme izmisliti nicesar. */
    @Test
    public void snapshotWithoutMeasurementReportsNoCollections() {
        Diag.setEnabled(false);
        Diag.reset();
        DiagSnapshot snapshot = Diag.snapshot();
        assertEquals(0L, count(snapshot, "jvm.gc"));
        assertEquals(0L, count(snapshot, "jvm.alloc.server"));
    }

    /**
     * Alokacije server niti se stejejo, GC tudi, in po izklopu se ne spreminjajo vec
     * (isto pravilo kot D7b za stevce moda).
     */
    @Test
    public void allocationsAndCollectionsOfTheServerThreadAreCountedAndFrozenAtOff() throws Exception {
        final long[] seen = new long[6];
        final Throwable[] failure = new Throwable[1];
        Thread server = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Diag.setEnabled(true);
                    for (int i = 0; i < 64; i++) {
                        sink = new byte[(int) MB];
                    }
                    System.gc();
                    DiagSnapshot during = Diag.snapshot();
                    seen[0] = count(during, "jvm.alloc.server");
                    seen[1] = count(during, "jvm.alloc.podprto");
                    seen[2] = count(during, "jvm.gc");
                    seen[3] = count(during, "jvm.heap.max");
                    Diag.setEnabled(false);
                    long atOff = count(Diag.snapshot(), "jvm.alloc.server");
                    for (int i = 0; i < 64; i++) {
                        sink = new byte[(int) MB];
                    }
                    System.gc();
                    DiagSnapshot after = Diag.snapshot();
                    seen[4] = atOff;
                    seen[5] = count(after, "jvm.alloc.server");
                } catch (Throwable t) {
                    failure[0] = t;
                }
            }
        }, JvmProbe.SERVER_THREAD);
        server.start();
        server.join(60000L);
        if (failure[0] != null) {
            throw new AssertionError(failure[0]);
        }
        if (seen[1] == 1L) {
            assertTrue("alociranih vsaj 64 MB, izmerjeno " + seen[0], seen[0] >= 64L * MB);
        } else {
            assertEquals("brez podpore mora biti stevec 0", 0L, seen[0]);
        }
        assertTrue("System.gc() mora biti vsaj ena zbirka", seen[2] >= 1L);
        assertTrue("heap max je znan", seen[3] > 0L);
        assertEquals("po izklopu se alokacije ne stejejo vec", seen[4], seen[5]);
    }

    private static long count(DiagSnapshot snapshot, String name) {
        DiagSnapshot.Row row = snapshot.row(name);
        return row == null ? 0L : row.count;
    }
}
