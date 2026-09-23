package net.minecraft.world;

/**
 * M3.1: dostop do {@code World.pathListener} za preneseni {@code EntityNPCInterface}.
 *
 * <p>Original do polja dostopa neposredno, ker ga {@code META-INF/cnpcs_at.cfg}
 * ({@code public net.minecraft.world.World *}) ob zagonu naredi javnega. Prevajanje v
 * {@code dev} tece proti netransformiranemu razredu, kjer je polje {@code protected}.
 * Razred v istem paketu ima do njega dostop brez refleksije; v igri je klic enakovreden
 * neposrednemu branju polja.
 */
public final class RwWorldAccess {
    private RwWorldAccess() {
    }

    public static net.minecraft.pathfinding.PathWorldListener pathListener(World world) {
        return world.pathListener;
    }
}
