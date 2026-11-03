package si.ladja.xaerofleet.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Which map the player is looking at: Xaero's own world map, untouched, or
 * the ship's map - the same map with the fleet on it: captains, selection,
 * orders.
 *
 * <p>The choice is the player's and outlives the session: it is kept in a
 * small file beside the other configs, and a missing or unreadable file means
 * the plain world map.</p>
 */
public final class MapMode {

    public static final String FILE_NAME = "xaerofleetcommand-client.properties";
    private static final String KEY = "map_mode";
    private static final String SHIP = "ship";
    private static final String WORLD = "world";

    private static File file;
    private static boolean ship;
    private static boolean storm;
    private static boolean army;
    private static boolean loaded;

    private MapMode() {
    }

    /** Where the choice is kept; set once from the game directory, or by a test. */
    public static void useFile(File where) {
        file = where;
        loaded = false;
    }

    public static boolean isShipMap() {
        load();
        return ship;
    }

    public static void setShipMap(boolean value) {
        load();
        if (ship == value && !storm && !army) return;
        ship = value;
        storm = false;
        army = false;
        if (file != null) write(file, value);
    }

    /** @return the new mode: true for the ship's map */
    public static boolean toggle() {
        setShipMap(!isShipMap());
        return ship;
    }

    public static boolean isStormMap() { load(); return storm; }
    public static boolean isArmyMap() { load(); return army; }
    public static int current() { load(); return army ? 3 : storm ? 2 : ship ? 1 : 0; }
    public static void setMode(int mode) {
        if (mode == 3) {
            load(); army = true; ship = storm = false;
            if (file != null) writeMode(file, "army");
            return;
        }
        if (mode != 2) { setShipMap(mode == 1); return; }
        load(); ship = false; storm = true; army = false;
        if (file != null) writeMode(file, "storm");
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        ship = file != null && read(file);
        storm = file != null && "storm".equals(readMode(file));
        army = file != null && "army".equals(readMode(file));
    }

    /** The saved choice; false (the world map) for no file or a broken one. */
    static boolean read(File from) {
        return SHIP.equalsIgnoreCase(readMode(from));
    }

    private static String readMode(File from) {
        if (!from.isFile()) return WORLD;
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(from)) {
            props.load(in);
        } catch (IOException | IllegalArgumentException e) {
            return WORLD;
        }
        return props.getProperty(KEY, WORLD).trim().toLowerCase(java.util.Locale.ROOT);
    }

    static boolean write(File to, boolean shipMap) {
        return writeMode(to, shipMap ? SHIP : WORLD);
    }

    private static boolean writeMode(File to, String mode) {
        Properties props = new Properties();
        props.setProperty(KEY, mode);
        File dir = to.getAbsoluteFile().getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs()) return false;
        try (OutputStream out = new FileOutputStream(to)) {
            props.store(out, "Xaero Fleet Command - which map opens: world (plain) or ship (fleet on it)");
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
