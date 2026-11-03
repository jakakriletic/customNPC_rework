package si.ladja.xaerofleet.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Which captains the player has picked on the map, and his saved groups.
 *
 * <p>Client-only and free of Minecraft, so every rule of the gesture set can
 * be tested: click replaces, Shift adds or toggles, Ctrl removes, a box takes
 * everything inside it, and a group saved under a number comes back with that
 * number. Nothing here authorises anything - the server checks every id it is
 * sent - it only decides what the player is pointing at.</p>
 *
 * <p>The selection never holds more than one order can carry. A box drawn
 * around twenty ships takes the sixteen nearest its middle, and says how many
 * it left out, rather than building a selection the server will refuse.</p>
 */
public final class SelectionModel {

    public enum Mode {
        /** Forget the old selection and take these. */
        REPLACE,
        /** Keep the old selection and add these. */
        ADD,
        /** Add the ones not selected, drop the ones that were. */
        TOGGLE,
        /** Drop these, keep the rest. */
        REMOVE
    }

    public static final int GROUPS = 10;

    private final int capacity;
    private final LinkedHashSet<UUID> selected = new LinkedHashSet<>();
    private final Map<Integer, List<UUID>> groups = new HashMap<>();

    public SelectionModel(int capacity) {
        this.capacity = Math.max(1, capacity);
    }

    public int capacity() {
        return capacity;
    }

    /**
     * Applies one gesture.
     *
     * @param ids the captains under the gesture, most wanted first - the
     *            order decides who is left out when the selection is full
     * @return how many were left out because the selection was full
     */
    public int apply(Collection<UUID> ids, Mode mode) {
        if (mode == Mode.REPLACE) {
            selected.clear();
        }
        int dropped = 0;
        if (ids == null) {
            return 0;
        }
        for (UUID id : ids) {
            if (id == null) continue;
            switch (mode) {
                case REMOVE:
                    selected.remove(id);
                    break;
                case TOGGLE:
                    if (selected.remove(id)) break;
                    // fall through: not selected, so this is an add
                default:
                    if (selected.contains(id)) break;
                    if (selected.size() >= capacity) {
                        dropped++;
                    } else {
                        selected.add(id);
                    }
            }
        }
        return dropped;
    }

    public int apply(UUID id, Mode mode) {
        return apply(Collections.singletonList(id), mode);
    }

    public void clear() {
        selected.clear();
    }

    public boolean isEmpty() {
        return selected.isEmpty();
    }

    public int size() {
        return selected.size();
    }

    public boolean contains(UUID id) {
        return selected.contains(id);
    }

    public List<UUID> ids() {
        return Collections.unmodifiableList(new ArrayList<>(selected));
    }

    /** Drops what can no longer be commanded: sunk, gone, or out of range. */
    public void retain(Set<UUID> commandable) {
        selected.retainAll(commandable);
    }

    /** Saves the current selection under a number. An empty one clears it. */
    public void saveGroup(int group) {
        if (group < 0 || group >= GROUPS) return;
        if (selected.isEmpty()) {
            groups.remove(group);
        } else {
            groups.put(group, new ArrayList<>(selected));
        }
    }

    /**
     * Brings a saved group back, keeping only those still on the map.
     *
     * @return how many of the group could be selected
     */
    public int recallGroup(int group, boolean add, Set<UUID> available) {
        List<UUID> saved = groups.get(group);
        if (saved == null) return 0;
        List<UUID> present = new ArrayList<>();
        for (UUID id : saved) {
            if (available == null || available.contains(id)) present.add(id);
        }
        apply(present, add ? Mode.ADD : Mode.REPLACE);
        return present.size();
    }

    public boolean hasGroup(int group) {
        return groups.containsKey(group);
    }

    /** Which saved group this captain belongs to, lowest number first, or -1. */
    public int groupOf(UUID id) {
        for (int group = 1; group <= GROUPS; group++) {
            List<UUID> members = groups.get(group % GROUPS);
            if (members != null && members.contains(id)) return group % GROUPS;
        }
        return -1;
    }

    /** Whether a point lies in the box between two corners, dragged either way. */
    public static boolean inBox(double x, double y, double x0, double y0,
                                double x1, double y1) {
        return x >= Math.min(x0, x1) && x <= Math.max(x0, x1)
                && y >= Math.min(y0, y1) && y <= Math.max(y0, y1);
    }

    /**
     * The zone radius sent with an order for this many ships.
     *
     * <p>Only a bound on where stations may be looked for: the server spaces
     * them from the real hulls, closest to the click first, and widens the
     * zone itself when these hulls need more. Generous on purpose, so a click
     * near a coast still leaves the planner room to find water.</p>
     */
    public static double orderRadius(int count) {
        int ring = 0;
        int capacity = 1;
        while (capacity < count) capacity += 6 * ++ring;
        return Math.min(1024.0D, 16.0D + 64.0D * ring);
    }
}
