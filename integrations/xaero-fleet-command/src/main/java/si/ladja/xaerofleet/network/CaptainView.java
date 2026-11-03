package si.ladja.xaerofleet.network;

import java.util.UUID;

/** Immutable client-facing copy of the versioned DaVinci snapshot. */
public final class CaptainView {
    public final UUID captainId;
    public final UUID shipId;
    public final int dimension;
    public final double x;
    public final double y;
    public final double z;
    public final float heading;
    public final String faction;
    public final String orderState;
    public final String label;
    public final boolean commandable;
    /** {@code ENGAGE} or {@code PASSIVE} while she has an order; empty otherwise. */
    public final String orderEngagement;

    public CaptainView(UUID captainId, UUID shipId, int dimension,
                       double x, double y, double z, float heading,
                       String faction, String orderState, String label,
                       boolean commandable) {
        this(captainId, shipId, dimension, x, y, z, heading, faction, orderState,
                label, commandable, "");
    }

    public CaptainView(UUID captainId, UUID shipId, int dimension,
                       double x, double y, double z, float heading,
                       String faction, String orderState, String label,
                       boolean commandable, String orderEngagement) {
        this.captainId = captainId;
        this.shipId = shipId;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.heading = heading;
        this.faction = safe(faction, 16);
        this.orderState = safe(orderState, 24);
        this.label = safe(label, 64);
        this.commandable = commandable;
        this.orderEngagement = safe(orderEngagement, 16);
    }

    public boolean isEngaging() {
        return "ENGAGE".equals(orderEngagement);
    }

    private static String safe(String value, int maximum) {
        if (value == null) return "";
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }
}
