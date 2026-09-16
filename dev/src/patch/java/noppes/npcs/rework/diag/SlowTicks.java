package noppes.npcs.rework.diag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Pripis najpocasnejsih server tickov.
 *
 * <p>Zakaj obstaja: prva veljavna meritev (15. 9.) ima p95 = 1,6 ms in p99 = 81,8 ms.
 * Porazdelitev tega prepada ne more razloziti, ker ne ve, <b>kateri</b> ticki so bili
 * pocasni in kaj se je v njih dogajalo. Ta razred hrani najpocasnejse ticke skupaj s
 * kontekstom, zato je iz posnetka razvidno, ali so pocasni ticki autosave, nalaganje
 * chunkov ali delo NPC-jev.
 *
 * <p>Hranjenje vseh tickov ni mogoce (20 na sekundo, ure merjenja), zato se hrani samo
 * {@code capacity} najpocasnejsih. Na vroci poti to pomeni <b>eno primerjavo</b>, dokler
 * tick ni hitrejsi od najpocasnejsega ze shranjenega — alokacije ni.
 *
 * <p>Poleg tabele se steje, koliko tickov je preseglo posamezno raven proracuna. Tabela
 * pove, kaj se je zgodilo v skrajnih primerih; stevci povedo, kako pogosto je to.
 *
 * <p>Razred je namenoma brez Minecraft tipov, da je enotsko testljiv. Metode so
 * {@code synchronized}: merjenje tece na server niti, izpis pa lahko pride z druge.
 */
public final class SlowTicks {
    /** Koliko najpocasnejsih tickov se hrani, ce ni podano drugace. */
    public static final int DEFAULT_CAPACITY = 16;

    /** Privzete ravni proracuna: 10, 25, 50 (cel tick) in 100 ms. */
    public static final long[] DEFAULT_LEVELS_NANOS = {
        10000000L, 25000000L, 50000000L, 100000000L
    };

    /** Vrednost, s katero se oznaci neznan kontekst (klic brez podatka). */
    public static final int UNKNOWN = -1;

    private final int capacity;
    private final long[] levels;
    private final long[] overCounts;
    private final Entry[] entries;

    private int size;
    private long recorded;

    public SlowTicks() {
        this(DEFAULT_CAPACITY, DEFAULT_LEVELS_NANOS);
    }

    /**
     * @param capacity koliko najpocasnejsih tickov se hrani; vsaj 1
     * @param levelsNanos ravni proracuna v nanosekundah; morajo biti narascajoce
     */
    public SlowTicks(int capacity, long[] levelsNanos) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity < 1: " + capacity);
        }
        if (levelsNanos == null || levelsNanos.length == 0) {
            throw new IllegalArgumentException("brez ravni proracuna");
        }
        for (int i = 1; i < levelsNanos.length; i++) {
            if (levelsNanos[i] <= levelsNanos[i - 1]) {
                throw new IllegalArgumentException("ravni niso narascajoce");
            }
        }
        this.capacity = capacity;
        this.levels = levelsNanos.clone();
        this.overCounts = new long[this.levels.length];
        this.entries = new Entry[capacity];
    }

    /**
     * Zabelezi tick. Hrani se samo, ce je pocasnejsi od najpocasnejsega ze shranjenega,
     * oziroma vedno, dokler tabela ni polna.
     *
     * <p>Ob enakem trajanju obvelja <b>prej</b> zabelezen tick. Izbira je namerna: tabela
     * mora biti ponovljiva in ne sme biti odvisna od vrstnega reda klicev pri izenacenju.
     *
     * @param tickIndex zaporedna stevilka ticka od zacetka meritve ali {@link #UNKNOWN}
     * @param offsetMillis koliko ms po zacetku meritve se je tick koncal
     * @param durationNanos trajanje ticka
     * @param npcs koliko NPC-jev je v tem ticku tiknilo ali {@link #UNKNOWN}
     * @param chunkLoads koliko chunkov se je v tem ticku nalozilo ali {@link #UNKNOWN}
     * @param chunkUnloads koliko chunkov se je v tem ticku odlozilo ali {@link #UNKNOWN}
     * @param saves koliko svetov se je v tem ticku shranilo ali {@link #UNKNOWN}
     */
    public synchronized void record(long tickIndex, long offsetMillis, long durationNanos,
            int npcs, int chunkLoads, int chunkUnloads, int saves) {
        long duration = durationNanos < 0L ? 0L : durationNanos;
        this.recorded++;
        // Ravni so narascajoce: tick, ki preseze visjo raven, je presegel tudi vse nizje.
        // Zanka zato tece navzgor in se ustavi pri prvi ravni, ki je tick ne doseze.
        for (int i = 0; i < this.levels.length; i++) {
            if (duration >= this.levels[i]) {
                this.overCounts[i]++;
            } else {
                break;
            }
        }
        if (this.size == this.capacity && duration <= this.entries[this.size - 1].durationNanos) {
            return;
        }
        Entry entry = new Entry(tickIndex, offsetMillis < 0L ? 0L : offsetMillis, duration,
                npcs, chunkLoads, chunkUnloads, saves);
        int at = this.size < this.capacity ? this.size++ : this.capacity - 1;
        this.entries[at] = entry;
        // Vstavljanje nazaj po urejenem polju; capacity je majhen, zato je to ceneje od
        // urejanja celotne tabele in ne alocira nicesar.
        for (int i = at; i > 0; i--) {
            if (this.entries[i].durationNanos > this.entries[i - 1].durationNanos) {
                Entry swap = this.entries[i - 1];
                this.entries[i - 1] = this.entries[i];
                this.entries[i] = swap;
            } else {
                break;
            }
        }
    }

    /** Najpocasnejsi ticki, od najpocasnejsega naprej. */
    public synchronized List<Entry> entries() {
        List<Entry> out = new ArrayList<Entry>(this.size);
        for (int i = 0; i < this.size; i++) {
            out.add(this.entries[i]);
        }
        return Collections.unmodifiableList(out);
    }

    /** Koliko tickov je bilo skupaj zabelezenih. */
    public synchronized long recorded() {
        return this.recorded;
    }

    public int capacity() {
        return this.capacity;
    }

    /** Ravni proracuna v nanosekundah. */
    public long[] levelsNanos() {
        return this.levels.clone();
    }

    /** Koliko tickov je doseglo ali preseglo raven z danim indeksom. */
    public synchronized long overCount(int levelIndex) {
        return this.overCounts[levelIndex];
    }

    /** Trajanje najpocasnejsega zabelezenega ticka ali 0, ce tabela je prazna. */
    public synchronized long slowestNanos() {
        return this.size == 0 ? 0L : this.entries[0].durationNanos;
    }

    public synchronized SlowTicks copy() {
        SlowTicks other = new SlowTicks(this.capacity, this.levels);
        System.arraycopy(this.entries, 0, other.entries, 0, this.size);
        System.arraycopy(this.overCounts, 0, other.overCounts, 0, this.overCounts.length);
        other.size = this.size;
        other.recorded = this.recorded;
        return other;
    }

    public synchronized void reset() {
        java.util.Arrays.fill(this.entries, null);
        java.util.Arrays.fill(this.overCounts, 0L);
        this.size = 0;
        this.recorded = 0L;
    }

    /**
     * Berljiva tabela. Prazna meritev vrne prazen niz, da izpis posnetka ne dobi
     * razdelka brez vsebine.
     */
    public synchronized String toText() {
        if (this.size == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(256);
        out.append("najpocasnejsi ticki (").append(this.size).append(" od ")
                .append(this.recorded).append(")\n");
        out.append(pad("#", 4)).append(pad("tick", 10)).append(pad("ob (s)", 10))
                .append(pad("ms", 12)).append(pad("npc", 6)).append(pad("chunk+", 8))
                .append(pad("chunk-", 8)).append(pad("save", 6)).append('\n');
        out.append(line(64)).append('\n');
        for (int i = 0; i < this.size; i++) {
            Entry e = this.entries[i];
            out.append(pad(Integer.toString(i + 1), 4));
            out.append(pad(number(e.tickIndex), 10));
            out.append(pad(String.format(Locale.ROOT, "%.1f", Double.valueOf(e.offsetMillis / 1000.0)), 10));
            out.append(pad(String.format(Locale.ROOT, "%.3f", Double.valueOf(e.durationNanos / 1000000.0)), 12));
            out.append(pad(number(e.npcs), 6));
            out.append(pad(number(e.chunkLoads), 8));
            out.append(pad(number(e.chunkUnloads), 8));
            out.append(pad(number(e.saves), 6));
            out.append('\n');
        }
        out.append("ticki nad ravnijo:");
        for (int i = 0; i < this.levels.length; i++) {
            out.append(' ').append(this.levels[i] / 1000000L).append("ms=").append(this.overCounts[i]);
        }
        out.append('\n');
        return out.toString();
    }

    /** Strojno berljiv izpis; vstavi se v posnetek kot polje objektov. */
    public synchronized String toJson() {
        StringBuilder out = new StringBuilder(256);
        out.append("{\"recorded\":").append(this.recorded);
        out.append(",\"capacity\":").append(this.capacity);
        out.append(",\"levels\":[");
        for (int i = 0; i < this.levels.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append("{\"ns\":").append(this.levels[i]).append(",\"count\":")
                    .append(this.overCounts[i]).append('}');
        }
        out.append("],\"slowest\":[");
        for (int i = 0; i < this.size; i++) {
            Entry e = this.entries[i];
            if (i > 0) {
                out.append(',');
            }
            out.append("{\"tick\":").append(e.tickIndex);
            out.append(",\"offsetMillis\":").append(e.offsetMillis);
            out.append(",\"nanos\":").append(e.durationNanos);
            out.append(",\"npcs\":").append(e.npcs);
            out.append(",\"chunkLoads\":").append(e.chunkLoads);
            out.append(",\"chunkUnloads\":").append(e.chunkUnloads);
            out.append(",\"saves\":").append(e.saves).append('}');
        }
        out.append("]}");
        return out.toString();
    }

    private static String number(long value) {
        return value == UNKNOWN ? "-" : Long.toString(value);
    }

    private static String pad(String value, int width) {
        if (value.length() >= width) {
            return value + " ";
        }
        StringBuilder out = new StringBuilder(width);
        out.append(value);
        while (out.length() < width) {
            out.append(' ');
        }
        return out.toString();
    }

    private static String line(int width) {
        StringBuilder out = new StringBuilder(width);
        for (int i = 0; i < width; i++) {
            out.append('-');
        }
        return out.toString();
    }

    /** En pocasen tick s kontekstom, v katerem se je zgodil. */
    public static final class Entry {
        public final long tickIndex;
        public final long offsetMillis;
        public final long durationNanos;
        public final int npcs;
        public final int chunkLoads;
        public final int chunkUnloads;
        public final int saves;

        Entry(long tickIndex, long offsetMillis, long durationNanos, int npcs, int chunkLoads,
                int chunkUnloads, int saves) {
            this.tickIndex = tickIndex;
            this.offsetMillis = offsetMillis;
            this.durationNanos = durationNanos;
            this.npcs = npcs;
            this.chunkLoads = chunkLoads;
            this.chunkUnloads = chunkUnloads;
            this.saves = saves;
        }

        public double millis() {
            return this.durationNanos / 1000000.0;
        }

        @Override
        public String toString() {
            return "tick " + this.tickIndex + " = " + String.format(Locale.ROOT, "%.3f",
                    Double.valueOf(this.millis())) + " ms";
        }
    }
}
