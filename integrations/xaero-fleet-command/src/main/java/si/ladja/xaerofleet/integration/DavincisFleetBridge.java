package si.ladja.xaerofleet.integration;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import si.ladja.xaerofleet.network.CaptainView;
import si.ladja.xaerofleet.network.CommandFeedback;

/**
 * The only runtime boundary to DaVinci's Vessels.
 *
 * <p>No DaVinci class appears in a field or method signature, so this module
 * remains independently compilable. A missing or incompatible service fails
 * closed and does not affect the rest of the map adapter.</p>
 */
public final class DavincisFleetBridge {

    private static final String SERVICE_CLASS =
            "com.elytradev.davincisvessels.api.fleet.FleetCommandService";
    private static final String ZONE_CLASS =
            "com.elytradev.davincisvessels.api.fleet.DestinationZone";
    private static final int SUPPORTED_API_VERSION = 2;

    private final Object service;
    private final Constructor<?> zoneConstructor;
    private final Method snapshots;
    private final Method submitMove;
    private final Method cancel;
    /** Absent on a DaVinci's Vessels from before rules of engagement; then null. */
    private final Method engagement;
    private final String unavailableReason;

    private DavincisFleetBridge(Object service, Constructor<?> zoneConstructor,
                                Method snapshots, Method submitMove, Method cancel,
                                Method engagement, String unavailableReason) {
        this.service = service;
        this.zoneConstructor = zoneConstructor;
        this.snapshots = snapshots;
        this.submitMove = submitMove;
        this.cancel = cancel;
        this.engagement = engagement;
        this.unavailableReason = unavailableReason;
    }

    public static DavincisFleetBridge discover() {
        if (!Loader.isModLoaded("davincisvessels")) {
            return unavailable("davincisvessels_not_loaded");
        }
        try {
            Class<?> serviceType = Class.forName(SERVICE_CLASS);
            Class<?> zoneType = Class.forName(ZONE_CLASS);
            Object service = serviceType.getMethod("getInstance").invoke(null);
            int version = (Integer) serviceType.getMethod("getApiVersion").invoke(service);
            if (version != SUPPORTED_API_VERSION) {
                return unavailable("unsupported_api_version_" + version);
            }
            Constructor<?> zone = zoneType.getConstructor(
                    double.class, double.class, double.class, Float.class);
            Method snapshots = serviceType.getMethod("captainsVisibleTo",
                    EntityPlayer.class, int.class);
            Method submit = serviceType.getMethod("submitMoveOrder",
                    EntityPlayer.class, List.class, zoneType);
            Method cancel = serviceType.getMethod("cancelOrder",
                    EntityPlayer.class, List.class);
            Method engagement;
            try {
                engagement = serviceType.getMethod("setOrderEngagement",
                        EntityPlayer.class, List.class, String.class);
            } catch (NoSuchMethodException ex) {
                // An older server: move and cancel still work, the attack /
                // passive buttons are answered as unsupported.
                engagement = null;
            }
            return new DavincisFleetBridge(service, zone, snapshots, submit,
                    cancel, engagement, "");
        } catch (ReflectiveOperationException | LinkageError ex) {
            return unavailable("service_contract_unavailable");
        }
    }

    public boolean isAvailable() {
        return service != null;
    }

    public String getUnavailableReason() {
        return unavailableReason;
    }

    public List<?> captainsVisibleTo(EntityPlayer player, int dimension) {
        Object result = invoke(snapshots, player, dimension);
        return result instanceof List ? (List<?>) result : Collections.emptyList();
    }

    /** Copies reflected API objects into this module's wire-safe value type. */
    public CaptainView copySnapshot(Object snapshot) {
        if (snapshot == null) {
            return null;
        }
        try {
            Class<?> type = snapshot.getClass();
            return new CaptainView(
                    (UUID) type.getMethod("getCaptainId").invoke(snapshot),
                    (UUID) type.getMethod("getShipId").invoke(snapshot),
                    (Integer) type.getMethod("getDimension").invoke(snapshot),
                    (Double) type.getMethod("getX").invoke(snapshot),
                    (Double) type.getMethod("getY").invoke(snapshot),
                    (Double) type.getMethod("getZ").invoke(snapshot),
                    (Float) type.getMethod("getHeading").invoke(snapshot),
                    (String) type.getMethod("getFaction").invoke(snapshot),
                    (String) type.getMethod("getOrderState").invoke(snapshot),
                    (String) type.getMethod("getLabel").invoke(snapshot),
                    (Boolean) type.getMethod("isCommandable").invoke(snapshot),
                    optionalString(type, snapshot, "getOrderEngagement"));
        } catch (ReflectiveOperationException | ClassCastException ex) {
            return null;
        }
    }

    public Object submitMoveOrder(EntityPlayer player, List<UUID> captainIds,
                                  double centerX, double centerZ, double radius,
                                  Float preferredHeading) {
        if (!isAvailable()) {
            return null;
        }
        try {
            Object zone = zoneConstructor.newInstance(centerX, centerZ, radius,
                    preferredHeading);
            return invoke(submitMove, player, captainIds, zone);
        } catch (ReflectiveOperationException | IllegalArgumentException ex) {
            return null;
        }
    }

    public Object cancelOrder(EntityPlayer player, List<UUID> captainIds) {
        return invoke(cancel, player, captainIds);
    }

    /**
     * Tells ships under orders whether to fight what they meet on the way.
     *
     * @param mode {@code ENGAGE} or {@code PASSIVE}
     * @return null when the server does not know the order
     */
    public Object setOrderEngagement(EntityPlayer player, List<UUID> captainIds, String mode) {
        return invoke(engagement, player, captainIds, mode);
    }

    public boolean supportsEngagement() {
        return isAvailable() && engagement != null;
    }

    private static String optionalString(Class<?> type, Object target, String getter) {
        try {
            Object value = type.getMethod(getter).invoke(target);
            return value instanceof String ? (String) value : "";
        } catch (ReflectiveOperationException ex) {
            return "";
        }
    }

    public CommandFeedback copyCommandResult(Object result) {
        if (result == null) return new CommandFeedback(false, 0, 0, "integration_unavailable");
        try {
            Class<?> type = result.getClass();
            boolean accepted = (Boolean) type.getMethod("isAccepted").invoke(result);
            List<?> acceptedIds = (List<?>) type.getMethod("getAcceptedCaptainIds").invoke(result);
            List<?> rejectedIds = (List<?>) type.getMethod("getRejectedCaptainIds").invoke(result);
            String reason = (String) type.getMethod("getReason").invoke(result);
            return new CommandFeedback(accepted, acceptedIds.size(), rejectedIds.size(), reason);
        } catch (ReflectiveOperationException | ClassCastException ex) {
            return new CommandFeedback(false, 0, 0, "invalid_service_response");
        }
    }

    private Object invoke(Method method, Object... arguments) {
        if (!isAvailable() || method == null) {
            return null;
        }
        try {
            return method.invoke(service, arguments);
        } catch (IllegalAccessException | InvocationTargetException
                 | IllegalArgumentException ex) {
            return null;
        }
    }

    private static DavincisFleetBridge unavailable(String reason) {
        return new DavincisFleetBridge(null, null, null, null, null, null, reason);
    }
}
