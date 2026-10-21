package noppes.npcs.rework.far;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * M5-V.4: dodelitev trakov atlasa impostorjev videzom (hash opisa {@link FarLook}) z LRU (D-046).
 * Ko je atlas poln, se prepise najdlje neuporabljen videz ({@link #lastEvicted} pove, kateri). Brez GL,
 * testljivo z JUnit; uporablja jo samo render nit.
 */
public final class ImpostorSlots {
    private final int capacity;
    private final LinkedHashMap<Long, Integer> byHash = new LinkedHashMap<Long, Integer>(16, 0.75f, true);
    private final ArrayDeque<Integer> free = new ArrayDeque<Integer>();
    public long evictions;
    /** Hash videza, ki ga je zadnji {@link #allocate} izrinil, ali 0. */
    public long lastEvicted;

    public ImpostorSlots(int capacity) {
        this.capacity = Math.max(1, capacity);
        for (int i = 0; i < this.capacity; ++i) {
            this.free.add(i);
        }
    }

    public int capacity() {
        return this.capacity;
    }

    public int size() {
        return this.byHash.size();
    }

    /** Ali ima videz trak (ne steje kot uporaba). */
    public boolean contains(long hash) {
        return this.byHash.containsKey(hash);
    }

    /** Trak videza ali -1; steje kot uporaba (LRU). */
    public int touch(long hash) {
        Integer s = this.byHash.get(hash);
        return s == null ? -1 : s;
    }

    /** Trak za videz: obstojec, prost ali izrinjen najdlje neuporabljen. */
    public int allocate(long hash) {
        this.lastEvicted = 0L;
        Integer s = this.byHash.get(hash);
        if (s != null) {
            return s;
        }
        int slot;
        if (!this.free.isEmpty()) {
            slot = this.free.poll();
        } else {
            Iterator<Map.Entry<Long, Integer>> it = this.byHash.entrySet().iterator();
            Map.Entry<Long, Integer> eldest = it.next();
            slot = eldest.getValue();
            this.lastEvicted = eldest.getKey();
            it.remove();
            ++this.evictions;
        }
        this.byHash.put(hash, slot);
        return slot;
    }

    /** Sprosti trak videza (npr. neuspela peka). */
    public void release(long hash) {
        Integer s = this.byHash.remove(hash);
        if (s != null) {
            this.free.add(s);
        }
    }

    public void clear() {
        this.byHash.clear();
        this.free.clear();
        for (int i = 0; i < this.capacity; ++i) {
            this.free.add(i);
        }
    }
}
