package net.minecraft.pathfinding;

/**
 * M3.3: dostop do zasciteneh polj {@code PathNavigate} za
 * {@link noppes.npcs.rework.entity.MountGuard}.
 *
 * <p>Pot se vrne z neposrednim zapisom polja, ne s {@code setPath}: {@code setPath}
 * ponastavi tudi zasebno stanje zaznave zataknitve ({@code ticksAtLastPos},
 * {@code lastPosCheck}), kar bi nosilcu vsak tick pobrisalo zgodovino. Razred je v istem
 * paketu kot {@code PathNavigate} (vzorec {@code RwWorldAccess}, M3.1), zato brez refleksije.
 */
public final class RwNavigatorAccess {
    private RwNavigatorAccess() {
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
}
