package noppes.npcs.rework.nav;

import java.util.Arrays;

/**
 * M5.11 (S14b): pomnilnik "pozicija bloka -> izid" za cas enega klica
 * {@code isDirectPathBetweenPoints}.
 *
 * <p>Odprto naslavljanje z linearnim preizkusom; kljuc so trije int-i (brez pakiranja, da ni
 * omejitve obsega koordinat). Veljavnost reze doloca stevec generacije, zato {@link #reset()}
 * ne brise tabel. Ko je tabela do polovice polna, se podvoji (model M5.0: 55-110 razlicnih
 * mest na klic, zacetna velikost 256 zadosca za vecino klicev).
 *
 * <p>Razred ne pozna Minecrafta, da je lahko testiran brez sveta ({@code NodeTypeMemoTest}).
 */
public final class NodeTypeMemo<T> {
    private int[] keyX;
    private int[] keyY;
    private int[] keyZ;
    private int[] gen;
    private Object[] value;
    private int mask;
    private int size;
    private int generation = 1;
    private long lookups;
    private long misses;

    public NodeTypeMemo() {
        this(256);
    }

    /** @param capacity zacetno stevilo rez, potenca 2 */
    public NodeTypeMemo(int capacity) {
        if (capacity < 2 || Integer.bitCount(capacity) != 1) {
            throw new IllegalArgumentException("velikost mora biti potenca 2: " + capacity);
        }
        this.allocate(capacity);
    }

    private void allocate(int capacity) {
        this.keyX = new int[capacity];
        this.keyY = new int[capacity];
        this.keyZ = new int[capacity];
        this.gen = new int[capacity];
        this.value = new Object[capacity];
        this.mask = capacity - 1;
        this.size = 0;
    }

    /** Pozabi vse vnose; tabela ostane enako velika. */
    public void reset() {
        this.size = 0;
        if (++this.generation == 0) {
            // Prelom stevca: reze z gen 0 bi sicer veljale za sveze.
            Arrays.fill(this.gen, 0);
            this.generation = 1;
        }
        // Brez referenc med klici (izidi so enum konstante, a pomnilnik je splosen).
        Arrays.fill(this.value, null);
    }

    private static int hash(int x, int y, int z) {
        int h = x * 0x9E3779B1 + y * 0x7FEB352D + z * 0x846CA68B;
        return h ^ (h >>> 15);
    }

    /** Shranjen izid za (x, y, z) ali {@code null}, ce ga v tem klicu se ni. */
    @SuppressWarnings("unchecked")
    public T get(int x, int y, int z) {
        ++this.lookups;
        int i = hash(x, y, z) & this.mask;
        while (this.gen[i] == this.generation) {
            if (this.keyX[i] == x && this.keyY[i] == y && this.keyZ[i] == z) {
                return (T) this.value[i];
            }
            i = (i + 1) & this.mask;
        }
        ++this.misses;
        return null;
    }

    /** Shrani izid; {@code null} se ne shrani. Obstojec kljuc se prepise. */
    public void put(int x, int y, int z, T v) {
        if (v == null) {
            return;
        }
        if ((this.size + 1) * 2 > this.mask + 1) {
            this.grow();
        }
        this.insert(x, y, z, v);
    }

    private void insert(int x, int y, int z, Object v) {
        int i = hash(x, y, z) & this.mask;
        while (this.gen[i] == this.generation) {
            if (this.keyX[i] == x && this.keyY[i] == y && this.keyZ[i] == z) {
                this.value[i] = v;
                return;
            }
            i = (i + 1) & this.mask;
        }
        this.keyX[i] = x;
        this.keyY[i] = y;
        this.keyZ[i] = z;
        this.gen[i] = this.generation;
        this.value[i] = v;
        ++this.size;
    }

    private void grow() {
        int[] ox = this.keyX;
        int[] oy = this.keyY;
        int[] oz = this.keyZ;
        int[] og = this.gen;
        Object[] ov = this.value;
        int g = this.generation;
        this.allocate(ox.length * 2);
        this.generation = 1;
        for (int i = 0; i < ox.length; ++i) {
            if (og[i] == g) {
                this.insert(ox[i], oy[i], oz[i], ov[i]);
            }
        }
    }

    /** Stevilo vnosov v tem klicu. */
    public int size() {
        return this.size;
    }

    public int capacity() {
        return this.mask + 1;
    }

    /** Vsa iskanja (od nastanka). */
    public long lookups() {
        return this.lookups;
    }

    /** Iskanja brez shranjenega izida (od nastanka) = dejanski izracuni. */
    public long misses() {
        return this.misses;
    }
}
