package net.minecraft.world;

/**
 * Dostop do {@code protected} clanov {@code World} za preneseno kodo CNPC-ja.
 *
 * <p>Original do njih dostopa neposredno, ker jih {@code META-INF/cnpcs_at.cfg}
 * ({@code public net.minecraft.world.World *}) ob zagonu naredi javne. Prevajanje v
 * {@code dev} tece proti netransformiranemu razredu, kjer so {@code protected}.
 * Razred v istem paketu ima do njih dostop brez refleksije; v igri je klic enakovreden
 * neposrednemu dostopu.
 *
 * <ul>
 *   <li>M3.1: {@code World.pathListener} za preneseni {@code EntityNPCInterface}</li>
 *   <li>M5.13: {@code World.isChunkLoaded} za {@code rework/net/AssociatedPlayers}</li>
 * </ul>
 */
public final class RwWorldAccess {
    private RwWorldAccess() {
    }

    public static net.minecraft.pathfinding.PathWorldListener pathListener(World world) {
        return world.pathListener;
    }

    /**
     * M5.13: ali je chunk nalozen, z isto metodo, ki jo uporabi
     * {@code World.getEntitiesWithinAABB} pred pregledom chunka.
     */
    public static boolean isChunkLoaded(World world, int chunkX, int chunkZ, boolean allowEmpty) {
        return world.isChunkLoaded(chunkX, chunkZ, allowEmpty);
    }
}
