package si.ladja.xaerofleet.client;

import si.ladja.xaerofleet.network.CaptainView;
import si.ladja.xaerofleet.network.MoveOrderPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What the client knows about the captains: the last snapshot, where each one
 * is drawn, the player's selection and the orders he has given.
 *
 * <p>Snapshots arrive every two seconds. Drawn as they come, a sailing ship
 * jumps across the map in steps, and a selection box closed between two steps
 * misses the ship it was drawn around. So a marker glides from where it was
 * drawn to where the new snapshot puts it, and hit-testing uses the same
 * glided position that is on the screen.</p>
 */
public final class ClientCaptainStore {

    /** How long a marker takes to glide to its new position. */
    static final long GLIDE_MS = 2000L;

    /** Further than this in one snapshot is a teleport, not a voyage. */
    private static final double SNAP_DISTANCE = 256.0D;

    /** How long a sent order is drawn before the server has confirmed it. */
    private static final long UNCONFIRMED_ORDER_MS = 6000L;

    private static List<CaptainView> captains = Collections.emptyList();
    private static final Map<UUID, double[]> glideFrom = new HashMap<>();
    private static long receivedAt;
    private static final SelectionModel selection =
            new SelectionModel(MoveOrderPacket.MAX_CAPTAINS);
    /** captain -> {targetX, targetZ, sentAtMillis} */
    private static final Map<UUID, double[]> orders = new HashMap<>();

    private ClientCaptainStore() {}

    public static void replace(List<CaptainView> values) {
        long now = System.currentTimeMillis();
        Map<UUID, CaptainView> old = new HashMap<>();
        for (CaptainView view : captains) old.put(view.captainId, view);
        Map<UUID, double[]> from = new HashMap<>();
        for (CaptainView value : values) {
            CaptainView before = old.get(value.captainId);
            if (before == null || before.dimension != value.dimension) continue;
            double x = displayX(before, now);
            double z = displayZ(before, now);
            if (Math.hypot(value.x - x, value.z - z) < SNAP_DISTANCE) {
                from.put(value.captainId, new double[]{x, z});
            }
        }
        glideFrom.clear();
        glideFrom.putAll(from);
        captains = Collections.unmodifiableList(new ArrayList<>(values));
        receivedAt = now;

        Set<UUID> commandable = new HashSet<>();
        Map<UUID, CaptainView> byId = new HashMap<>();
        for (CaptainView value : values) {
            byId.put(value.captainId, value);
            if (value.commandable) commandable.add(value.captainId);
        }
        selection.retain(commandable);

        // An order line lasts while the server says the ship is on her way,
        // and a moment longer for an order the snapshot has not caught up with.
        Iterator<Map.Entry<UUID, double[]>> it = orders.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, double[]> entry = it.next();
            CaptainView view = byId.get(entry.getKey());
            boolean fresh = now - (long) entry.getValue()[2] < UNCONFIRMED_ORDER_MS;
            if (view == null ? !fresh : !(isUnderway(view.orderState) || fresh)) {
                it.remove();
            }
        }
    }

    public static List<CaptainView> captains() { return captains; }

    public static SelectionModel selection() { return selection; }

    public static boolean isSelected(UUID id) { return selection.contains(id); }

    public static List<UUID> selectedIds() { return selection.ids(); }

    public static Set<UUID> commandableIds() {
        Set<UUID> ids = new HashSet<>();
        for (CaptainView view : captains) if (view.commandable) ids.add(view.captainId);
        return ids;
    }

    public static CaptainView byId(UUID id) {
        for (CaptainView view : captains) if (view.captainId.equals(id)) return view;
        return null;
    }

    /** Where this captain is drawn now, gliding between snapshots. */
    public static double displayX(CaptainView view, long now) {
        double[] from = glideFrom.get(view.captainId);
        return from == null ? view.x : from[0] + (view.x - from[0]) * glideT(now);
    }

    public static double displayZ(CaptainView view, long now) {
        double[] from = glideFrom.get(view.captainId);
        return from == null ? view.z : from[1] + (view.z - from[1]) * glideT(now);
    }

    private static double glideT(long now) {
        double t = (now - receivedAt) / (double) GLIDE_MS;
        if (t <= 0) return 0;
        if (t >= 1) return 1;
        // Ease out: most of the way at once, then settle.
        return 1 - (1 - t) * (1 - t);
    }

    /** Remembers where these captains were sent, for the line to their target. */
    public static void rememberOrder(List<UUID> ids, double x, double z) {
        long now = System.currentTimeMillis();
        for (UUID id : ids) orders.put(id, new double[]{x, z, now});
    }

    public static void forgetOrders(List<UUID> ids) {
        for (UUID id : ids) orders.remove(id);
    }

    /** The target this captain was sent to, or null. */
    public static double[] orderTarget(UUID id) {
        return orders.get(id);
    }

    public static boolean isUnderway(String state) {
        return "QUEUED".equals(state) || "PLANNING".equals(state)
                || "SAILING".equals(state);
    }

    /** Forgets the snapshot when the map closes; the selection and groups stay. */
    public static void clearCaptains() {
        captains = Collections.emptyList();
        glideFrom.clear();
    }

    /** Forgets everything, on leaving a world. */
    public static void clear() {
        clearCaptains();
        selection.clear();
        orders.clear();
    }
}
