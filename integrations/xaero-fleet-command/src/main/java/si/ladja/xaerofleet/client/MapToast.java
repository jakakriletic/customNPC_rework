package si.ladja.xaerofleet.client;

import java.util.HashMap;
import java.util.Map;

/**
 * One line of news drawn on the map itself.
 *
 * <p>The chat is hidden behind the world map, so a refusal that only goes to
 * chat is a refusal the player finds after he has closed the map and wondered
 * why nothing moved. The chat line stays - it is the record - and this is what
 * he sees while he is still looking at the fleet.</p>
 */
public final class MapToast {

    public static final int GOOD = 0xFF6BE08A;
    public static final int BAD = 0xFFFF7A6B;
    public static final int INFO = 0xFFFFD54A;

    /** How long a message is shown, fading over the last part. */
    static final long SHOW_MS = 4500L;

    private static final Map<String, String> REASONS = new HashMap<>();

    static {
        REASONS.put("rate_limited", "too fast - wait a moment");
        REASONS.put("invalid_destination", "invalid destination");
        REASONS.put("invalid_request", "invalid order");
        REASONS.put("invalid_fleet_size", "wrong number of ships");
        REASONS.put("invalid_captain_count", "too many or too few captains");
        REASONS.put("captain_not_commandable", "captain does not obey (not an ally or too far away)");
        REASONS.put("fleet_commands_disabled", "map orders are disabled on the server");
        REASONS.put("destination_too_small_for_formation", "the fleet is too big for this destination");
        REASONS.put("no_water_in_destination", "no navigable water at the destination");
        REASONS.put("no_station_in_zone", "not enough room at the destination for some ships");
        REASONS.put("destination_not_navigable", "the destination is not sea");
        REASONS.put("no_route_to_destination", "no water route to the destination");
        REASONS.put("route_budget_exhausted", "the route is too long for the planner");
        REASONS.put("planner_busy", "the planner is busy - try again");
        REASONS.put("no_route_found", "no route could be found");
        REASONS.put("order_not_cancellable", "no order to cancel");
        REASONS.put("order_not_active", "the ship has no active destination");
        REASONS.put("engagement_unsupported", "the server does not know this order yet");
        REASONS.put("integration_unavailable", "the server does not have DaVinci's Vessels");
        REASONS.put("invalid_service_response", "the server sent an unreadable reply");
    }

    private static String text = "";
    private static int colour = INFO;
    private static long shownAt;

    private MapToast() {}

    public static void show(String message, int argb) {
        text = message == null ? "" : message;
        colour = argb;
        shownAt = System.currentTimeMillis();
    }

    /** A reason code from the server, in words. Unknown codes are shown as they are. */
    public static String reason(String code) {
        if (code == null || code.isEmpty()) return "";
        String words = REASONS.get(code);
        return words == null ? code : words;
    }

    static String text() {
        return System.currentTimeMillis() - shownAt < SHOW_MS ? text : "";
    }

    static int colour() {
        return colour;
    }

    /** 1 while fresh, falling to 0 over the last second. */
    static float alpha() {
        long age = System.currentTimeMillis() - shownAt;
        if (age >= SHOW_MS) return 0;
        long left = SHOW_MS - age;
        return left >= 1000 ? 1F : left / 1000F;
    }
}
