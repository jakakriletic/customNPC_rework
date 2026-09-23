package noppes.npcs.rework.diag;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.List;

/**
 * Pomnilnik in GC v oknu meritve (M2.6).
 *
 * <p><b>Zakaj.</b> Protokol meritev ({@code docs/01-ARHITEKTURA.md} §7) zahteva alokacije,
 * GC premore in heap po GC. Prvi zagon M2.4 je pokazal rep, ki ga MSPT sam ne razlozi
 * (skripte pri 500 NPC-jih: max 518 ms v tretjem ticku meritve). Brez teh stevilk je
 * "to je GC" ugibanje.
 *
 * <p><b>Kako.</b> Samo {@code java.lang.management}, brez Minecraft tipov, zato je razred
 * testljiv brez sveta. Ob {@link #mark()} (ob vklopu in resetu) si zapomni izhodisce,
 * ob {@link #apply()} (ob posnetku) zapise razliko v stevce:
 *
 * <ul>
 *   <li>{@code jvm.gc} - vse zbirke: count = stevilo, nanos = cas zbiranja</li>
 *   <li>{@code jvm.gc.old} - samo zbiralniki stare generacije (MarkSweep, Old, Tenured)</li>
 *   <li>{@code jvm.alloc.server} - bajti, ki jih je alocirala server nit</li>
 *   <li>{@code jvm.heap.old.poGc} - zasedenost stare generacije ob zadnji zbirki (bajti);
 *       0, ce v oknu ni bilo nobene zbirke stare generacije</li>
 *   <li>{@code jvm.heap.max} - najvecji heap (bajti), da je zasedenost berljiva</li>
 * </ul>
 *
 * <p>Cas zbiranja je vsota trajanj zbirk po porocilu JVM-a (milisekundna locljivost), ne
 * cas ustavljenega sveta; pri vzporednih zbiralnikih je spodnja meja premorov.
 *
 * <p>Alokacije: {@code com.sun.management.ThreadMXBean} ni del standarda. Ce ga JVM nima
 * ali je izklopljen, ostane stevec 0 in {@code jvm.alloc.podprto} je 0 - nicla takrat ne
 * pomeni "nic alokacij".
 */
public final class JvmProbe {
    public static final String SERVER_THREAD = "Server thread";

    private static long gcCount0;
    private static long gcMillis0;
    private static long oldCount0;
    private static long oldMillis0;
    private static long serverThreadId = -1L;
    private static long serverAlloc0;

    private JvmProbe() {
    }

    /** Izhodisce okna. Poklice ga {@link Diag#reset()}. */
    public static synchronized void mark() {
        long[] gc = gcTotals(false);
        long[] old = gcTotals(true);
        gcCount0 = gc[0];
        gcMillis0 = gc[1];
        oldCount0 = old[0];
        oldMillis0 = old[1];
        serverThreadId = findThread(SERVER_THREAD);
        serverAlloc0 = allocatedBytes(serverThreadId);
    }

    /** Zapise razliko od izhodisca v stevce. Idempotentno: vsak klic prepise vrednosti. */
    public static synchronized void apply() {
        long[] gc = gcTotals(false);
        long[] old = gcTotals(true);
        set(DiagKeys.JVM_GC, delta(gc[0], gcCount0), delta(gc[1], gcMillis0) * 1000000L);
        set(DiagKeys.JVM_GC_OLD, delta(old[0], oldCount0), delta(old[1], oldMillis0) * 1000000L);
        long now = allocatedBytes(serverThreadId);
        boolean supported = serverThreadId >= 0L && now >= 0L && serverAlloc0 >= 0L;
        set(DiagKeys.JVM_ALLOC_SERVER, supported ? delta(now, serverAlloc0) : 0L, 0L);
        set(DiagKeys.JVM_ALLOC_SUPPORTED, supported ? 1L : 0L, 0L);
        set(DiagKeys.JVM_HEAP_OLD_AFTER_GC, delta(old[0], oldCount0) > 0L ? oldAfterGc() : 0L, 0L);
        set(DiagKeys.JVM_HEAP_MAX, Math.max(0L, Runtime.getRuntime().maxMemory()), 0L);
    }

    public static long delta(long now, long then) {
        return now >= then ? now - then : 0L;
    }

    /** Stara generacija po imenu zbiralnika; imena so iz HotSpot 8 (Serial, Parallel, CMS, G1). */
    public static boolean isOldCollector(String name) {
        if (name == null) {
            return false;
        }
        return name.contains("MarkSweep") || name.contains("Old") || name.contains("Tenured");
    }

    public static boolean isOldPool(String name) {
        if (name == null) {
            return false;
        }
        return name.contains("Old Gen") || name.contains("Tenured");
    }

    private static void set(DiagKey key, long count, long nanos) {
        key.reset();
        key.add(count);
        key.addNanos(nanos);
    }

    /** [stevilo, milisekunde] cez vse (ali samo stare) zbiralnike; neznano (-1) se preskoci. */
    static long[] gcTotals(boolean oldOnly) {
        long count = 0L;
        long millis = 0L;
        List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans();
        for (int i = 0; i < beans.size(); i++) {
            GarbageCollectorMXBean bean = beans.get(i);
            if (oldOnly && !isOldCollector(bean.getName())) {
                continue;
            }
            long c = bean.getCollectionCount();
            long t = bean.getCollectionTime();
            if (c > 0L) {
                count += c;
            }
            if (t > 0L) {
                millis += t;
            }
        }
        return new long[] {count, millis};
    }

    private static long oldAfterGc() {
        List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
        for (int i = 0; i < pools.size(); i++) {
            MemoryPoolMXBean pool = pools.get(i);
            if (pool.getType() != MemoryType.HEAP || !isOldPool(pool.getName())) {
                continue;
            }
            MemoryUsage usage = pool.getCollectionUsage();
            if (usage != null) {
                return usage.getUsed();
            }
        }
        return 0L;
    }

    static long findThread(String name) {
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (name.equals(thread.getName())) {
                return thread.getId();
            }
        }
        return -1L;
    }

    /** Bajti, ki jih je nit alocirala od zagona; -1, ce JVM tega ne podpira. */
    static long allocatedBytes(long threadId) {
        if (threadId < 0L) {
            return -1L;
        }
        try {
            java.lang.management.ThreadMXBean base = ManagementFactory.getThreadMXBean();
            if (!(base instanceof com.sun.management.ThreadMXBean)) {
                return -1L;
            }
            com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) base;
            if (!bean.isThreadAllocatedMemorySupported() || !bean.isThreadAllocatedMemoryEnabled()) {
                return -1L;
            }
            return bean.getThreadAllocatedBytes(threadId);
        } catch (RuntimeException | LinkageError unavailable) {
            return -1L;
        }
    }
}
