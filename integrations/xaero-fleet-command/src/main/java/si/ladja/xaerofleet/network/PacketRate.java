package si.ladja.xaerofleet.network;

import net.minecraft.entity.player.EntityPlayerMP;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * How often one player may ask for something, counted on the server.
 *
 * <p>The client sends a snapshot request whenever the map is open and a move
 * order whenever the player right-clicks it, and neither of those is a promise
 * about what a client actually sends: a modified one can ask sixty times a
 * second, and the snapshot it asks for walks every loaded entity in the
 * dimension. So the count is kept here, against the player's id, and the
 * server answers the ones that arrive too quickly with nothing.</p>
 *
 * <p>Bounded and keyed by id, so it holds no player object and cannot grow
 * past a few hundred entries however many people pass through the server.</p>
 */
final class PacketRate {

    private static final int REMEMBERED_PLAYERS = 256;

    private final long intervalMillis;
    private final Map<UUID, Long> last = new LinkedHashMap<UUID, Long>(32, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, Long> eldest) {
            return size() > REMEMBERED_PLAYERS;
        }
    };

    PacketRate(long intervalMillis) {
        this.intervalMillis = Math.max(1L, intervalMillis);
    }

    /**
     * Whether this player may be served now, recording it if he may.
     *
     * <p>Measured against the wall clock rather than the world's. In single
     * player the integrated server stops ticking while the map is open, so a
     * world-tick interval never elapses while the player is doing the very
     * thing it is meant to pace - and every request after the first comes
     * back refused until he closes the map.</p>
     */
    boolean allow(EntityPlayerMP player) {
        return player != null && allow(player.getUniqueID(), System.currentTimeMillis());
    }

    /**
     * The same decision without a player, which is the whole of the policy.
     *
     * <p>Kept separate so that it can be tested: a rate limit that has never
     * been run against a clock that jumps is a rate limit nobody has
     * checked. A world whose time has been set backwards lets everybody
     * through once rather than locking them out until it catches up.</p>
     */
    boolean allow(UUID playerId, long now) {
        if (playerId == null) {
            return false;
        }
        Long previous = last.get(playerId);
        if (previous != null && now >= previous && now - previous < intervalMillis) {
            return false;
        }
        last.put(playerId, now);
        return true;
    }

    int remembered() {
        return last.size();
    }

    void forget(EntityPlayerMP player) {
        if (player != null) {
            last.remove(player.getUniqueID());
        }
    }
}
