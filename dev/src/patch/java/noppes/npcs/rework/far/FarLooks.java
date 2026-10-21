package noppes.npcs.rework.far;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * M5-V.3: strezniska tabela videzov (raziskava M5-V.0 §5.2, D-045).
 *
 * <p>Vsak NPC (kljuc po identiteti) kaze na en {@link Look}; NPC-ji z istim hashem opisa ({@link FarLook})
 * si delijo isti {@code Look} in isti kratek id, ki gre v vnos posnetka ({@code varint videz}, ~1-2 B
 * namesto 8 B hasha). Id-ji se v eni tabeli nikoli ne ponovijo, zato klientova preslikava id -&gt; hash ne
 * more postati napacna. Videz brez NPC-jev gre v LRU nedejavnih ({@link #idleCap}); ko izpade, se pozabi
 * (ponovni videz dobi nov id). Brez Minecraft tipov, testljivo brez sveta; bere jo samo strezniska nit.
 */
public final class FarLooks {
    /** En videz: kratek id, hash opisa in GZIP opis za odgovor {@code LOOK}. */
    public static final class Look {
        public final int id;
        public final long hash;
        public final byte[] data;
        int refs;

        Look(int id, long hash, byte[] data) {
            this.id = id;
            this.hash = hash;
            this.data = data;
        }
    }

    private final Map<Object, Look> perKey = new IdentityHashMap<Object, Look>();
    private final Map<Long, Look> byHash = new HashMap<Long, Look>();
    private final Map<Integer, Look> byId = new HashMap<Integer, Look>();
    private final LinkedHashMap<Long, Look> idle = new LinkedHashMap<Long, Look>(16, 0.75f, true);
    private final int idleCap;
    private int nextId = 1;

    public FarLooks(int idleCap) {
        this.idleCap = Math.max(0, idleCap);
    }

    /** Trenutni videz kljuca ali {@code null} (se ni izracunan ali razveljavljen). */
    public Look get(Object key) {
        return this.perKey.get(key);
    }

    /** Videz po kratkem id-ju (odgovor na {@code REQ}) ali {@code null}. */
    public Look byId(int id) {
        return this.byId.get(id);
    }

    /** Kljucu dodeli videz z danim hashem; obstojec videz z istim hashem se deli, sicer nov id. */
    public Look put(Object key, long hash, byte[] data) {
        Look old = this.perKey.get(key);
        if (old != null && old.hash == hash) {
            return old;
        }
        Look look = this.byHash.get(hash);
        if (look == null) {
            look = new Look(this.nextId++, hash, data);
            this.byHash.put(hash, look);
            this.byId.put(look.id, look);
        } else if (look.refs == 0) {
            this.idle.remove(hash);
        }
        ++look.refs;
        this.perKey.put(key, look);
        if (old != null) {
            release(old);
        }
        return look;
    }

    /** Kljuc nima vec videza (razveljavitev ob {@code UPDATE_NPC} ali odstranitev iz sveta). */
    public void forget(Object key) {
        Look old = this.perKey.remove(key);
        if (old != null) {
            release(old);
        }
    }

    private void release(Look look) {
        if (--look.refs > 0) {
            return;
        }
        this.idle.put(look.hash, look);
        if (this.idle.size() > this.idleCap) {
            Iterator<Look> it = this.idle.values().iterator();
            Look eldest = it.next();
            it.remove();
            this.byHash.remove(eldest.hash);
            this.byId.remove(eldest.id);
        }
    }

    /** Stevilo razlicnih videzov, ki jih uporablja vsaj en kljuc. */
    public int inUse() {
        return this.byHash.size() - this.idle.size();
    }

    /** Stevilo vseh videzov v tabeli (tudi nedejavnih v LRU). */
    public int size() {
        return this.byHash.size();
    }

    /** Stevilo kljucev z videzom. */
    public int keys() {
        return this.perKey.size();
    }

    /** Pozabi vse; id-ji se ne ponovijo (stevec tece naprej). */
    public void clear() {
        this.perKey.clear();
        this.byHash.clear();
        this.byId.clear();
        this.idle.clear();
    }
}
