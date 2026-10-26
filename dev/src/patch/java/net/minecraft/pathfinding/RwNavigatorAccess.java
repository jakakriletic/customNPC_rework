package net.minecraft.pathfinding;

import java.lang.reflect.Field;

import net.minecraft.util.math.Vec3d;

/**
 * M3.3: dostop do zasciteneh polj {@code PathNavigate} za
 * {@link noppes.npcs.rework.entity.MountGuard}.
 *
 * <p>Pot se vrne z neposrednim zapisom polja, ne s {@code setPath}: {@code setPath}
 * ponastavi tudi zasebno stanje zaznave zataknitve ({@code ticksAtLastPos},
 * {@code lastPosCheck}), kar bi nosilcu vsak tick pobrisalo zgodovino. Razred je v istem
 * paketu kot {@code PathNavigate} (vzorec {@code RwWorldAccess}, M3.1), zato brez refleksije.
 *
 * <p>M4.14b (D-077): zasebna polja casovne meje vozlisca so dosegljiva le z refleksijo
 * (MCP in SRG ime). Ne {@code ReflectionHelper.findField(cls, mcp, srg)}: ta izven FML
 * razvojnega zagona isce samo SRG ime, zato v JUnit polja ne najde.
 */
public final class RwNavigatorAccess {
    private static final Field TIMEOUT_NODE = field("timeoutCachedNode", "field_188557_k");
    private static final Field TIMEOUT_TIMER = field("timeoutTimer", "field_188558_l");
    private static final Field TIMEOUT_LIMIT = field("timeoutLimit", "field_188560_n");

    private RwNavigatorAccess() {
    }

    private static Field field(String mcp, String srg) {
        for (String name : new String[] {mcp, srg}) {
            try {
                Field f = PathNavigate.class.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                // naslednje ime
            }
        }
        throw new IllegalStateException("PathNavigate." + mcp + "/" + srg + " ni najdeno");
    }

    public static Path currentPath(PathNavigate navigator) {
        return navigator.currentPath;
    }

    public static double speed(PathNavigate navigator) {
        return navigator.speed;
    }

    public static void restore(PathNavigate navigator, Path path, double speed) {
        navigator.currentPath = path;
        navigator.speed = speed;
    }

    /**
     * Zacne novo okno casovne meje vozlisca, enako kot ga vanilla zacne po lastnem izteku
     * ({@code checkForStuck}: vozlisce {@code ZERO}, cas 0, meja 0). Vanilla
     * {@code timeoutTimer} ob menjavi vozlisca ali poti ne ponastavi, zato nova pot podeduje
     * stenski cas prejsnjih poti in jo meja pocisti ob prvi menjavi vozlisca blizu clana
     * (M4.14b navigator sled 4a7a6623: G/7 pri 170 zacne obvoz s 6.748 ms, izgubi ga pri 182).
     * Meja ostane aktivna za novo pot; preverba premika na 100 tickov ostane nespremenjena.
     */
    public static void restartTimeout(PathNavigate navigator) {
        try {
            TIMEOUT_NODE.set(navigator, Vec3d.ZERO);
            TIMEOUT_TIMER.setLong(navigator, 0L);
            TIMEOUT_LIMIT.setDouble(navigator, 0.0D);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
