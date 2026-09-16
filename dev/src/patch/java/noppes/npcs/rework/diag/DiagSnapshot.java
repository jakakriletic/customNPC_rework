package noppes.npcs.rework.diag;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Nespremenljiv posnetek stevcev ob dolocenem trenutku.
 *
 * <p>Posnetek se naredi enkrat in se nato izpise; med izpisom se stevci lahko se naprej
 * spreminjajo, kar bi sicer dalo notranje neskladno tabelo.
 */
public final class DiagSnapshot {
    /** Proracun enega server ticka pri 20 TPS. */
    public static final double TICK_BUDGET_MS = 50.0;

    private final boolean enabled;
    private final long startedMillis;
    private final long elapsedMillis;
    private final long ticks;
    private final List<Row> rows;
    private final List<Distribution> distributions;
    private final SlowTicks slowTicks;

    private DiagSnapshot(boolean enabled, long startedMillis, long elapsedMillis, long ticks,
            List<Row> rows, List<Distribution> distributions, SlowTicks slowTicks) {
        this.enabled = enabled;
        this.startedMillis = startedMillis;
        this.elapsedMillis = elapsedMillis;
        this.ticks = ticks;
        this.rows = Collections.unmodifiableList(rows);
        this.distributions = Collections.unmodifiableList(distributions);
        this.slowTicks = slowTicks;
    }

    static DiagSnapshot of(boolean enabled, long startedMillis, long elapsedMillis, long ticks,
            List<DiagKey> keys, List<Distribution> distributions, SlowTicks slowTicks) {
        List<Row> rows = new ArrayList<Row>(keys.size());
        for (DiagKey key : keys) {
            rows.add(new Row(key.name(), key.unit(), key.count(), key.nanos()));
        }
        return new DiagSnapshot(enabled, startedMillis, elapsedMillis, ticks, rows,
                new ArrayList<Distribution>(distributions), slowTicks);
    }

    public boolean enabled() {
        return this.enabled;
    }

    public long ticks() {
        return this.ticks;
    }

    public long elapsedMillis() {
        return this.elapsedMillis;
    }

    public List<Row> rows() {
        return this.rows;
    }

    public List<Distribution> distributions() {
        return this.distributions;
    }

    /** Tabela najpocasnejsih tickov te meritve. */
    public SlowTicks slowTicks() {
        return this.slowTicks;
    }

    public Row row(String name) {
        for (Row row : this.rows) {
            if (row.name.equals(name)) {
                return row;
            }
        }
        return null;
    }

    public Distribution distribution(String name) {
        for (Distribution d : this.distributions) {
            if (d.name().equals(name)) {
                return d;
            }
        }
        return null;
    }

    /** Ena vrstica tabele: stevec in, kjer je merjen, tudi cas. */
    public static final class Row {
        public final String name;
        public final String unit;
        public final long count;
        public final long nanos;

        Row(String name, String unit, long count, long nanos) {
            this.name = name;
            this.unit = unit;
            this.count = count;
            this.nanos = nanos;
        }

        public double perTick(long ticks) {
            return ticks <= 0L ? 0.0 : (double) this.count / (double) ticks;
        }

        public double millisPerTick(long ticks) {
            return ticks <= 0L ? 0.0 : (double) this.nanos / (double) ticks / 1000000.0;
        }

        public double meanMicros() {
            return this.count <= 0L ? 0.0 : (double) this.nanos / (double) this.count / 1000.0;
        }
    }

    /**
     * Berljiva tabela. Namenoma brez barv in brez odvisnosti — gre v log datoteko in v
     * konzolo dedicated serverja, ki jo bere skripta.
     */
    public String toText() {
        StringBuilder out = new StringBuilder(1024);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
        out.append("RWDIAG posnetek\n");
        out.append("  stanje      : ").append(this.enabled ? "vklopljeno" : "izklopljeno").append('\n');
        out.append("  zacetek     : ")
                .append(this.startedMillis == 0L ? "-" : format.format(new Date(this.startedMillis)))
                .append('\n');
        out.append("  trajanje    : ").append(this.elapsedMillis).append(" ms\n");
        out.append("  server ticki: ").append(this.ticks).append('\n');

        out.append('\n');
        out.append(pad("stevec", 34)).append(pad("skupaj", 14)).append(pad("na tick", 12))
                .append(pad("ms/tick", 10)).append(pad("povp. us", 10)).append("\n");
        out.append(line(80)).append('\n');
        for (Row row : this.rows) {
            if (row.count == 0L && row.nanos == 0L) {
                continue;
            }
            out.append(pad(row.name, 34));
            out.append(pad(Long.toString(row.count), 14));
            out.append(pad(number(row.perTick(this.ticks)), 12));
            out.append(pad(row.nanos == 0L ? "-" : number(row.millisPerTick(this.ticks)), 10));
            out.append(pad(row.nanos == 0L ? "-" : number(row.meanMicros()), 10));
            out.append('\n');
        }

        out.append('\n');
        out.append(pad("porazdelitev", 34)).append(pad("n", 10)).append(pad("min", 10))
                .append(pad("povp.", 10)).append(pad("p50", 10)).append(pad("p95", 10))
                .append(pad("p99", 10)).append(pad("max", 10)).append('\n');
        out.append(line(104)).append('\n');
        for (Distribution d : this.distributions) {
            if (d.count() == 0L) {
                continue;
            }
            boolean nanos = "ns".equals(d.unit());
            out.append(pad(d.name() + (nanos ? " (ms)" : ""), 34));
            out.append(pad(Long.toString(d.count()), 10));
            out.append(pad(scale(d.min(), nanos), 10));
            out.append(pad(scale((long) d.mean(), nanos), 10));
            out.append(pad(scale(d.percentile(0.50), nanos), 10));
            out.append(pad(scale(d.percentile(0.95), nanos), 10));
            out.append(pad(scale(d.percentile(0.99), nanos), 10));
            out.append(pad(scale(d.max(), nanos), 10));
            out.append('\n');
        }

        String slow = this.slowTicks == null ? "" : this.slowTicks.toText();
        if (!slow.isEmpty()) {
            out.append('\n').append(slow);
        }

        Distribution tick = this.distribution("server.tick.ns");
        if (tick != null && tick.count() > 0L) {
            double p95ms = tick.percentile(0.95) / 1000000.0;
            out.append('\n');
            out.append("  MSPT p95 = ").append(number(p95ms)).append(" ms od ")
                    .append(number(TICK_BUDGET_MS)).append(" ms proracuna (")
                    .append(number(p95ms / TICK_BUDGET_MS * 100.0)).append(" %)\n");
        }
        return out.toString();
    }

    /** Strojno berljiv izpis za kasnejse primerjave pred/po. */
    public String toJson() {
        StringBuilder out = new StringBuilder(512);
        out.append("{\"enabled\":").append(this.enabled);
        out.append(",\"startedMillis\":").append(this.startedMillis);
        out.append(",\"elapsedMillis\":").append(this.elapsedMillis);
        out.append(",\"ticks\":").append(this.ticks);
        out.append(",\"counters\":[");
        boolean first = true;
        for (Row row : this.rows) {
            if (row.count == 0L && row.nanos == 0L) {
                continue;
            }
            if (!first) {
                out.append(',');
            }
            first = false;
            out.append("{\"name\":\"").append(escape(row.name)).append('"');
            out.append(",\"count\":").append(row.count);
            out.append(",\"nanos\":").append(row.nanos).append('}');
        }
        out.append("],\"distributions\":[");
        first = true;
        for (Distribution d : this.distributions) {
            if (d.count() == 0L) {
                continue;
            }
            if (!first) {
                out.append(',');
            }
            first = false;
            out.append("{\"name\":\"").append(escape(d.name())).append('"');
            out.append(",\"unit\":\"").append(escape(d.unit())).append('"');
            out.append(",\"count\":").append(d.count());
            out.append(",\"min\":").append(d.min());
            out.append(",\"max\":").append(d.max());
            out.append(",\"sum\":").append(d.sum());
            out.append(",\"p50\":").append(d.percentile(0.50));
            out.append(",\"p95\":").append(d.percentile(0.95));
            out.append(",\"p99\":").append(d.percentile(0.99)).append('}');
        }
        out.append(']');
        if (this.slowTicks != null) {
            out.append(",\"slowTicks\":").append(this.slowTicks.toJson());
        }
        out.append('}');
        return out.toString();
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.3f", Double.valueOf(value));
    }

    private static String scale(long value, boolean nanosToMillis) {
        return nanosToMillis ? number(value / 1000000.0) : Long.toString(value);
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

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
