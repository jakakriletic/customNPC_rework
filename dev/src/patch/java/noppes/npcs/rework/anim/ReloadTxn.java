package noppes.npcs.rework.anim;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** P7c (D-098, Q287): one prepared reload on the server. Commit only after every expected player has
 * confirmed this revision and hash; one refusal, a wrong hash or the deadline aborts. A player who
 * leaves is no longer waited for. Plain state, server thread only. */
final class ReloadTxn {
    enum State { WAITING, COMMIT, ABORT }

    final int revision;
    final byte[] hash;
    private final Map<UUID, String> waiting = new HashMap<>();
    private final long deadline;
    private String failure;

    ReloadTxn(int revision, byte[] hash, long deadline) { this.revision = revision; this.hash = hash; this.deadline = deadline; }

    void expect(UUID player, String name) { waiting.put(player, name); }

    void ack(UUID player, int ackRevision, boolean ok, byte[] ackHash) {
        if (ackRevision != revision) { return; }
        String name = waiting.remove(player);
        if (name != null && failure == null && (!ok || !Arrays.equals(hash, ackHash))) { failure = name + " refused the animation files"; }
    }

    void left(UUID player) { waiting.remove(player); }

    State state(long now) {
        if (failure != null) { return State.ABORT; }
        if (waiting.isEmpty()) { return State.COMMIT; }
        if (now >= deadline) {
            failure = "no confirmation from " + waiting.values();
            return State.ABORT;
        }
        return State.WAITING;
    }

    String failure() { return failure; }
}
