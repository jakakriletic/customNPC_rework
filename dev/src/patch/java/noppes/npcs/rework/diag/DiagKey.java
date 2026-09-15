package noppes.npcs.rework.diag;

import java.util.concurrent.atomic.LongAdder;

/**
 * Ena merjena velicina: stevec dogodkov in vsota porabljenega casa.
 *
 * <p>Klicna mesta hranijo referenco na kljuc v staticnem polju, zato na vroci poti ni
 * niti iskanja po mapi niti alokacije. {@link LongAdder} je izbran namesto
 * {@code AtomicLong}, ker je pod socasnim pristevanjem bistveno cenejsi, branje pa se
 * zgodi samo ob izpisu.
 */
public final class DiagKey {
    private final String name;
    private final String unit;
    private final LongAdder count = new LongAdder();
    private final LongAdder nanos = new LongAdder();

    DiagKey(String name, String unit) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("diag key name must not be empty");
        }
        this.name = name;
        this.unit = unit == null ? "" : unit;
    }

    public String name() {
        return this.name;
    }

    /** Ime enote, ki jo steje stevec (npr. "tick", "klic", "poizvedba"). Samo za izpis. */
    public String unit() {
        return this.unit;
    }

    public void increment() {
        this.count.increment();
    }

    public void add(long amount) {
        this.count.add(amount);
    }

    /** Pristeje en dogodek in njegovo trajanje. */
    public void record(long elapsedNanos) {
        this.count.increment();
        this.nanos.add(elapsedNanos);
    }

    /** Pristeje samo cas, brez dogodka; za primere, ko dogodke steje drug kljuc. */
    public void addNanos(long elapsedNanos) {
        this.nanos.add(elapsedNanos);
    }

    public long count() {
        return this.count.sum();
    }

    public long nanos() {
        return this.nanos.sum();
    }

    public void reset() {
        this.count.reset();
        this.nanos.reset();
    }

    @Override
    public String toString() {
        return this.name + "{count=" + this.count() + ", nanos=" + this.nanos() + "}";
    }
}
