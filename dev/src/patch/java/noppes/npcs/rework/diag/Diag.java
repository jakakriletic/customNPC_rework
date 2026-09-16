package noppes.npcs.rework.diag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Vstopna tocka instrumentacije.
 *
 * <p>Pravila, ki veljajo za vsa klicna mesta:
 * <ul>
 *   <li>Kljuci se ustvarijo enkrat, v staticnem polju klicnega razreda. Na vroci poti se
 *       nikoli ne iska po imenu.</li>
 *   <li>Ko je instrumentacija izklopljena, mora klic stati eno branje {@code volatile}
 *       polja in nic vec — brez alokacije, brez {@code System.nanoTime()}.</li>
 *   <li>Merjenje nikoli ne spremeni obnasanja moda. Ce kaj v tem paketu vrze izjemo,
 *       je to napaka instrumentacije, ne moda, zato jo {@link #stop} pozre.</li>
 * </ul>
 *
 * <p>Tipicna uporaba:
 * <pre>
 *   private static final DiagKey PATH = Diag.key("ai.path.request", "poizvedba");
 *   ...
 *   long t = Diag.start();
 *   navigator.tryMoveToXYZ(x, y, z, speed);
 *   Diag.stop(PATH, t);
 * </pre>
 */
public final class Diag {
    /** Ime lastnosti JVM, s katero se instrumentacija vklopi ze ob zagonu. */
    public static final String PROPERTY = "rwdiag";

    private static volatile boolean enabled;
    private static volatile long startedNanos;
    private static volatile long startedMillis;

    private static final Map<String, DiagKey> KEYS = new ConcurrentSkipListMap<String, DiagKey>();
    private static final Map<String, Distribution> DISTRIBUTIONS =
            new ConcurrentSkipListMap<String, Distribution>();

    private static final DiagKey TICKS = key("server.tick", "tick");
    private static final Distribution TICK_NANOS = distribution("server.tick.ns", "ns");
    private static final SlowTicks SLOW_TICKS = new SlowTicks();

    private Diag() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Vklopi ali izklopi zbiranje. Vklop vedno zacne z ociscenimi stevci, da meritev
     * nikoli ne vsebuje ostankov prejsnjega zagona.
     */
    public static void setEnabled(boolean value) {
        if (value) {
            reset();
            enabled = true;
        } else {
            enabled = false;
        }
    }

    /** Ali je bila instrumentacija zahtevana z {@code -Drwdiag=on} ob zagonu JVM. */
    public static boolean requestedByProperty() {
        String value = System.getProperty(PROPERTY);
        if (value == null) {
            return false;
        }
        String v = value.trim().toLowerCase(java.util.Locale.ROOT);
        return v.equals("on") || v.equals("true") || v.equals("1") || v.equals("yes");
    }

    /** Vrne (in ob prvem klicu ustvari) stevec z danim imenom. */
    public static DiagKey key(String name, String unit) {
        DiagKey existing = KEYS.get(name);
        if (existing != null) {
            return existing;
        }
        DiagKey created = new DiagKey(name, unit);
        DiagKey previous = KEYS.putIfAbsent(name, created);
        return previous == null ? created : previous;
    }

    public static DiagKey key(String name) {
        return key(name, "");
    }

    /** Vrne (in ob prvem klicu ustvari) porazdelitev z danim imenom. */
    public static Distribution distribution(String name, String unit) {
        Distribution existing = DISTRIBUTIONS.get(name);
        if (existing != null) {
            return existing;
        }
        Distribution created = new Distribution(name, unit);
        Distribution previous = DISTRIBUTIONS.putIfAbsent(name, created);
        return previous == null ? created : previous;
    }

    /**
     * Kljuc, izpeljan iz razreda AI taska. Ime razreda se prebere enkrat na razred in ne
     * na klic; rezultat je keshiran v {@link ClassValue}, ki ga JVM hrani ob samem razredu.
     */
    public static DiagKey taskKey(Class<?> type) {
        return TASK_KEYS.get(type);
    }

    private static final ClassValue<DiagKey> TASK_KEYS = new ClassValue<DiagKey>() {
        @Override
        protected DiagKey computeValue(Class<?> type) {
            return key("ai.task." + type.getSimpleName(), "izvedba");
        }
    };

    public static void count(DiagKey diagKey) {
        if (enabled) {
            diagKey.increment();
        }
    }

    public static void add(DiagKey diagKey, long amount) {
        if (enabled) {
            diagKey.add(amount);
        }
    }

    public static void record(Distribution target, long value) {
        if (enabled) {
            target.record(value);
        }
    }

    /** Vrne izhodisce za merjenje ali 0, ce je instrumentacija izklopljena. */
    public static long start() {
        return enabled ? System.nanoTime() : 0L;
    }

    /**
     * Zakljuci meritev, ki jo je zacel {@link #start()}. Klic z izhodiscem 0 (torej
     * meritev, ki se je zacela med izklopljeno instrumentacijo) ne naredi nicesar.
     */
    public static void stop(DiagKey diagKey, long startNanos) {
        if (!enabled || startNanos == 0L) {
            return;
        }
        long elapsed = System.nanoTime() - startNanos;
        if (elapsed < 0L) {
            elapsed = 0L;
        }
        diagKey.record(elapsed);
    }

    /**
     * Zabelezi konec server ticka brez konteksta. Za meritev je uporabna samo
     * porazdelitev; tabela najpocasnejsih tickov bo imela stolpce prazne.
     */
    public static void tick(long tickNanos) {
        tick(tickNanos, SlowTicks.UNKNOWN, SlowTicks.UNKNOWN, SlowTicks.UNKNOWN,
                SlowTicks.UNKNOWN, SlowTicks.UNKNOWN);
    }

    /**
     * Zabelezi konec server ticka, njegovo trajanje in kontekst, v katerem je tekel.
     *
     * <p>Kontekst je tu zato, ker porazdelitev pove <i>koliko</i> je pocasnih tickov, ne
     * pa <i>zakaj</i>. Prepad med p95 in p99 v prvi veljavni meritvi se brez pripisa ne da
     * razlociti med autosave, nalaganjem chunkov in delom NPC-jev.
     */
    public static void tick(long tickNanos, long tickIndex, int npcs, int chunkLoads,
            int chunkUnloads, int saves) {
        if (!enabled) {
            return;
        }
        long nanos = tickNanos < 0L ? 0L : tickNanos;
        TICKS.increment();
        TICK_NANOS.record(nanos);
        long offsetMillis = startedNanos == 0L
                ? 0L
                : Math.max(0L, (System.nanoTime() - startedNanos) / 1000000L);
        SLOW_TICKS.record(tickIndex, offsetMillis, nanos, npcs, chunkLoads, chunkUnloads, saves);
    }

    /** Tabela najpocasnejsih tickov trenutne meritve. */
    public static SlowTicks slowTicks() {
        return SLOW_TICKS;
    }

    public static long ticks() {
        return TICKS.count();
    }

    public static DiagSnapshot snapshot() {
        List<DiagKey> keys = new ArrayList<DiagKey>(KEYS.values());
        List<Distribution> distributions = new ArrayList<Distribution>();
        for (Distribution d : DISTRIBUTIONS.values()) {
            distributions.add(d.copy());
        }
        long elapsedMillis = startedMillis == 0L
                ? 0L
                : Math.max(0L, (System.nanoTime() - startedNanos) / 1000000L);
        return DiagSnapshot.of(enabled, startedMillis, elapsedMillis, TICKS.count(), keys,
                distributions, SLOW_TICKS.copy());
    }

    /** Pocisti vse stevce in porazdelitve; registrirani kljuci ostanejo. */
    public static void reset() {
        for (DiagKey diagKey : KEYS.values()) {
            diagKey.reset();
        }
        for (Distribution d : DISTRIBUTIONS.values()) {
            d.reset();
        }
        SLOW_TICKS.reset();
        startedNanos = System.nanoTime();
        startedMillis = System.currentTimeMillis();
    }
}
