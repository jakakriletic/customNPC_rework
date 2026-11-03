package si.ladja.xaerofleet.client.xaero;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import xaero.map.gui.GuiMap;
import xaero.map.world.MapDimension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Xaero 1.37.8-only view adapter. Any signature mismatch fails closed.
 *
 * <p>Xaero does all of its own map arithmetic in <em>real display pixels</em>,
 * not in the scaled GUI coordinates that {@code drawScreen} works in. Its
 * cursor-to-block conversion is
 *
 * <pre>block = (realMouse - display/2) / scale + camera</pre>
 *
 * with {@code scale} being real pixels per block and the centre taken from
 * {@code Minecraft.displayWidth/Height}. An overlay that reads {@code scale}
 * and then measures from {@code GuiMap.width / 2} is mixing two coordinate
 * systems: at GUI scale 1 they happen to agree, and at any other setting every
 * marker drifts away from its ship in proportion to its distance from the
 * middle of the screen - which looks exactly like markers wandering off across
 * the map as it is panned.</p>
 *
 * <p>So this class keeps Xaero's own units internally and converts once, at
 * the edge: {@link #screenX}/{@link #screenY} hand back GUI pixels to draw in,
 * {@link #worldXAtPixel}/{@link #worldZAtPixel} take the raw mouse position
 * the way Xaero reads it, and {@link #worldX}/{@link #worldZ} take GUI pixels
 * for anything that only has those.</p>
 */
public final class MapViewTransform {
    private static final Field CAMERA_X = field("cameraX");
    private static final Field CAMERA_Z = field("cameraZ");
    private static final Field SCALE = field("scale");
    private static final Field DIMENSION = field("lastViewedDimensionId");
    /** Xaero's own "fly the camera here" target; optional, only for centring. */
    private static final Field CAMERA_DESTINATION = field("cameraDestination");

    /**
     * Xaero's per-dimension coordinate scale, where the build has one.
     *
     * <p>Dimension scaling arrived in a later Xaero than this adapter's
     * oldest target: 1.34 draws every dimension in plain world coordinates and
     * has no such method at all. Calling it directly would compile against the
     * newer jar and throw {@link NoSuchMethodError} on the older one - an
     * {@code Error}, which the catch below would not stop, every frame the map
     * is open. Resolved once instead, and absent simply means a scale of
     * one.</p>
     */
    private static final Method DIM_SCALE = dimScaleMethod();

    public final double cameraX;
    public final double cameraZ;
    /** Real display pixels per block, exactly as Xaero uses it. */
    public final double pixelsPerBlock;
    public final double coordinateScale;
    public final int dimension;
    /** Middle of the map, in real display pixels. */
    public final double centreX;
    public final double centreY;
    /** Real pixels to one GUI pixel: Minecraft's GUI scale factor. */
    public final double guiScale;

    private MapViewTransform(double cameraX, double cameraZ, double pixelsPerBlock,
                             double coordinateScale, int dimension,
                             double centreX, double centreY, double guiScale) {
        this.cameraX = cameraX;
        this.cameraZ = cameraZ;
        this.pixelsPerBlock = pixelsPerBlock;
        this.coordinateScale = coordinateScale;
        this.dimension = dimension;
        this.centreX = centreX;
        this.centreY = centreY;
        this.guiScale = guiScale <= 0 ? 1 : guiScale;
    }

    /**
     * Whether this Xaero build still has the fields the overlay reads.
     *
     * <p>Asked once by the client so that an unsupported Xaero turns the
     * overlay off quietly instead of failing a reflective read sixty times a
     * second behind an open map. Markers are the only thing that goes: the
     * server side of the mod neither knows nor cares what the map is.</p>
     */
    public static boolean isSupported() {
        return CAMERA_X != null && CAMERA_Z != null && SCALE != null && DIMENSION != null;
    }

    /** The fields that could not be found, for a single line in the log. */
    public static String missingFields() {
        StringBuilder missing = new StringBuilder();
        if (CAMERA_X == null) missing.append(" cameraX");
        if (CAMERA_Z == null) missing.append(" cameraZ");
        if (SCALE == null) missing.append(" scale");
        if (DIMENSION == null) missing.append(" lastViewedDimensionId");
        return missing.length() == 0 ? "" : missing.substring(1);
    }

    /**
     * One view built from values already read, for tests of the arithmetic.
     *
     * <p>The projection is the part of this class that can be wrong in a way
     * nobody sees - a marker a few pixels out looks like a marker - and it is
     * also the only part that does not need Xaero to check.</p>
     */
    static MapViewTransform of(double cameraX, double cameraZ, double pixelsPerBlock,
                               double coordinateScale, int dimension,
                               double centreX, double centreY, double guiScale) {
        return new MapViewTransform(cameraX, cameraZ, pixelsPerBlock, coordinateScale,
                dimension, centreX, centreY, guiScale);
    }

    public static MapViewTransform read(GuiMap map) {
        if (!isSupported() || map == null) return null;
        try {
            Integer dimension = (Integer) DIMENSION.get(map);
            if (dimension == null || map.getMapProcessor().getMapWorld() == null) return null;
            MapDimension viewed = map.getMapProcessor().getMapWorld().getCurrentDimension();
            if (viewed == null) return null;
            double coordinateScale = coordinateScaleOf(viewed);
            double pixelsPerBlock = SCALE.getDouble(map);
            if (!finite(coordinateScale) || coordinateScale <= 0
                    || !finite(pixelsPerBlock) || pixelsPerBlock <= 0) return null;
            Minecraft minecraft = Minecraft.getMinecraft();
            int factor = new ScaledResolution(minecraft).getScaleFactor();
            if (minecraft.displayWidth <= 0 || minecraft.displayHeight <= 0 || factor <= 0) {
                return null;
            }
            return new MapViewTransform(CAMERA_X.getDouble(map), CAMERA_Z.getDouble(map),
                    pixelsPerBlock, coordinateScale, dimension,
                    minecraft.displayWidth / 2.0D, minecraft.displayHeight / 2.0D, factor);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    /**
     * Glides Xaero's camera to a world position, the way its own "go to
     * waypoint" does. Quietly does nothing on a Xaero without the field.
     *
     * @return whether the camera was told to move
     */
    public boolean centreOn(GuiMap map, double worldX, double worldZ) {
        if (CAMERA_DESTINATION == null || map == null) return false;
        try {
            CAMERA_DESTINATION.set(map, new int[]{
                    (int) Math.floor(worldX / coordinateScale),
                    (int) Math.floor(worldZ / coordinateScale)});
            return true;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return false;
        }
    }

    /** Where to draw this world position, in the GUI pixels drawScreen uses. */
    public int screenX(double worldX) {
        return (int) Math.round(realX(worldX) / guiScale);
    }

    public int screenY(double worldZ) {
        return (int) Math.round(realY(worldZ) / guiScale);
    }

    /** Where this world position falls on the display, in real pixels. */
    public double realX(double worldX) {
        return centreX + (worldX / coordinateScale - cameraX) * pixelsPerBlock;
    }

    public double realY(double worldZ) {
        return centreY + (worldZ / coordinateScale - cameraZ) * pixelsPerBlock;
    }

    /**
     * The world position under a raw mouse coordinate.
     *
     * <p>The same arithmetic Xaero uses for the coordinates it prints at the
     * top of the map, so an order goes to the block the player was looking
     * at rather than to one a GUI rounding away.</p>
     */
    public double worldXAtPixel(double realPixelX) {
        return (cameraX + (realPixelX - centreX) / pixelsPerBlock) * coordinateScale;
    }

    public double worldZAtPixel(double realPixelY) {
        return (cameraZ + (realPixelY - centreY) / pixelsPerBlock) * coordinateScale;
    }

    public double worldX(int screenX) {
        return worldXAtPixel(screenX * guiScale);
    }

    public double worldX(double screenX) { return worldXAtPixel(screenX * guiScale); }
    public double worldZ(double screenY) { return worldZAtPixel(screenY * guiScale); }

    public double worldZ(int screenY) {
        return worldZAtPixel(screenY * guiScale);
    }

    /** One map coordinate in world blocks, or one where Xaero has no such idea. */
    private static double coordinateScaleOf(MapDimension dimension) {
        if (DIM_SCALE == null) {
            return 1.0D;
        }
        try {
            Object value = DIM_SCALE.invoke(dimension);
            double scale = value instanceof Number ? ((Number) value).doubleValue() : 1.0D;
            return finite(scale) && scale > 0 ? scale : 1.0D;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            return 1.0D;
        }
    }

    private static Method dimScaleMethod() {
        try {
            Method method = MapDimension.class.getMethod("calculateDimScale");
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            return null;
        }
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private static Field field(String name) {
        try {
            Field field = GuiMap.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }
}
