package noppes.npcs.rework.far;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * M5-V.3: zahteve {@code REQ} enega igralca za opise videzov in njihova omejitev (raziskava M5-V.0 §5.2).
 *
 * <p>Streznik odgovori samo za id-je, ki jih je ta igralec dejansko prejel v vnosu posnetka
 * ({@link #offered}), in za vsak id najvec enkrat do naslednjega {@code HELLO} ({@link #clear}); vrsta je
 * omejena na {@link #QUEUE_CAP}, praznjenje na tick pa klicatelj ({@code FarServer.REPLIES_PER_TICK}).
 * Tako klient ne more zahtevati tujih videzov ali z istim id-jem mnoziti prometa. Brez Minecraft tipov.
 */
public final class FarRequests {
    /** Najvec cakajocih zahtev na igralca; prevec se zavrze (klient po casu zahteva znova). */
    public static final int QUEUE_CAP = 1024;

    private final Set<Integer> offered = new HashSet<Integer>();
    private final Set<Integer> answered = new HashSet<Integer>();
    private final ArrayDeque<Integer> queue = new ArrayDeque<Integer>();

    long requested;
    long rejected;
    long dropped;
    long duplicate;

    /** Igralec je v vnosu prejel id videza. */
    public void offered(int id) {
        if (id != 0) {
            this.offered.add(id);
        }
    }

    /** Zahteva klienta za id. */
    public void request(int id) {
        ++this.requested;
        if (this.queue.size() >= QUEUE_CAP) {
            ++this.dropped;
            return;
        }
        this.queue.add(id);
    }

    /** Naslednji id, na katerega je treba odgovoriti (in ga oznaci kot odgovorjenega), ali 0. */
    public int next() {
        while (!this.queue.isEmpty()) {
            int id = this.queue.poll();
            if (!this.offered.contains(id)) {
                ++this.rejected;
                continue;
            }
            if (!this.answered.add(id)) {
                ++this.duplicate;
                continue;
            }
            return id;
        }
        return 0;
    }

    public boolean hasPending() {
        return !this.queue.isEmpty();
    }

    /** Nov {@code HELLO} ali izklop: klient je vse pozabil. */
    public void clear() {
        this.offered.clear();
        this.answered.clear();
        this.queue.clear();
    }

    public void resetCounters() {
        this.requested = 0;
        this.rejected = 0;
        this.dropped = 0;
        this.duplicate = 0;
    }
}
