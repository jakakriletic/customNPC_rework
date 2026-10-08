package noppes.npcs.rework.nav;

/**
 * M5-S S14: majhen predpomnilnik "koordinata chunka -> chunk" za cas enega klica.
 *
 * <p>Stiri reze, izbrane po parnosti (cx, cz): sosednji chunki nikoli ne pristanejo v isti
 * rezi, zato sprehod po crti in pregled sosedov ({@code checkNeighborBlocks}) ne izrivata drug
 * drugega. Ob zgresitvi se poklice nalagalnik (v igri {@code World.getChunkFromChunkCoords},
 * torej isti klic kot v originalu); {@link #reset()} izprazni vse reze brez brisanja tabel.
 *
 * <p>Razred ne pozna Minecrafta, da je lahko testiran brez sveta ({@code ChunkMemoTest}).
 */
public final class ChunkMemo<T> {
    /** Nalagalnik chunka; vrne isti objekt kot original za iste koordinate. */
    public interface Loader<T> {
        T load(int cx, int cz);
    }

    private final Loader<T> loader;
    private final int[] keyX = new int[4];
    private final int[] keyZ = new int[4];
    private final int[] gen = new int[4];
    private final Object[] value = new Object[4];
    private int generation = 1;
    private long lookups;
    private long misses;

    public ChunkMemo(Loader<T> loader) {
        this.loader = loader;
    }

    /** Pozabi vse; naslednji {@link #get} za vsak chunk spet poklice nalagalnik. */
    public void reset() {
        if (++this.generation == 0) {
            // Prelom stevca: reze z gen 0 bi sicer veljale za sveze.
            java.util.Arrays.fill(this.gen, 0);
            this.generation = 1;
        }
        java.util.Arrays.fill(this.value, null);
    }

    @SuppressWarnings("unchecked")
    public T get(int cx, int cz) {
        ++this.lookups;
        int slot = ((cx & 1) << 1) | (cz & 1);
        if (this.gen[slot] == this.generation && this.keyX[slot] == cx && this.keyZ[slot] == cz) {
            return (T) this.value[slot];
        }
        ++this.misses;
        T v = this.loader.load(cx, cz);
        this.keyX[slot] = cx;
        this.keyZ[slot] = cz;
        this.gen[slot] = this.generation;
        this.value[slot] = v;
        return v;
    }

    public long lookups() {
        return this.lookups;
    }

    public long misses() {
        return this.misses;
    }
}
