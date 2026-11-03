package noppes.npcs.rework.army;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** M11.7a: immutable v1 value for reflection bridges; only JDK types in its public getters. */
public final class ArmyMapMarker {
    private final String name, ownerName, kind, orderState, mode, shape, stance;
    private final boolean commandable;
    private final int dimension, x, y, z, members, loaded, omittedClusters, omittedMembers;
    private final long lastSeen;

    private ArmyMapMarker(ArmySnapshot.Entry entry, ArmyClusters.Cluster cluster, ArmyOrder order) {
        name = entry.name; ownerName = entry.ownerName; commandable = entry.command;
        kind = cluster == null ? "last_known" : cluster.kind.id();
        dimension = cluster == null ? entry.lastDimension : cluster.dimension;
        x = cluster == null ? entry.lastX : cluster.x;
        y = cluster == null ? entry.lastY : cluster.y;
        z = cluster == null ? entry.lastZ : cluster.z;
        members = cluster == null ? 0 : cluster.size;
        loaded = entry.loaded; omittedClusters = entry.omittedClusters; omittedMembers = entry.omittedMembers;
        lastSeen = entry.lastSeen;
        orderState = order == null ? "NONE" : order.state.name();
        mode = order == null ? "" : order.mode.name();
        shape = order == null ? "" : order.shape.name();
        stance = order == null ? "" : order.stance.name();
    }

    /** Copies only an already-authorized snapshot; dimension filtering never promotes a group to main. */
    static List<ArmyMapMarker> copy(ArmySnapshot snapshot, ArmyRegistry registry, int dimension) {
        List<ArmyMapMarker> out = new ArrayList<ArmyMapMarker>();
        for (ArmySnapshot.Entry entry : snapshot.armies) {
            ArmyOrder order = registry.order(entry.name);
            for (ArmyClusters.Cluster cluster : entry.clusters) {
                if (cluster.dimension == dimension) { out.add(new ArmyMapMarker(entry, cluster, order)); }
            }
            if (entry.clusters.isEmpty() && entry.hasLast() && entry.lastDimension == dimension) {
                out.add(new ArmyMapMarker(entry, null, order));
            }
        }
        return Collections.unmodifiableList(out);
    }

    public String getArmyName() { return name; }
    public String getOwnerName() { return ownerName; }
    public String getKind() { return kind; }
    public boolean isCommandable() { return commandable; }
    public int getDimension() { return dimension; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public int getMemberCount() { return members; }
    public int getLoadedCount() { return loaded; }
    public int getOmittedClusters() { return omittedClusters; }
    public int getOmittedMembers() { return omittedMembers; }
    public long getLastSeen() { return lastSeen; }
    public String getOrderState() { return orderState; }
    public String getOrderMode() { return mode; }
    public String getOrderShape() { return shape; }
    public String getOrderEngagement() { return stance; }
}
