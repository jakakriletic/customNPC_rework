package noppes.npcs.rework.nav;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * M5-S S14: {@link IBlockAccess} nad svetom, ki chunk za {@link #getBlockState} poisce enkrat
 * na klic namesto za vsak blok.
 *
 * <p>{@code World.getBlockState} (1.12.2, Forge 14.23.5.2847) je: zunaj visine 0..255 zrak,
 * sicer {@code getChunkFromChunkCoords(x >> 4, z >> 4).getBlockState(pos)}, in
 * {@code getChunkFromChunkCoords} je {@code chunkProvider.provideChunk} (hash iskanje, po potrebi
 * nalaganje). Ta razred naredi isto, le da isti chunk v enem klicu ne isce znova
 * ({@link ChunkMemo}). Vse ostale metode gredo nespremenjene v svet.
 *
 * <p>Uporablja se samo za svetove, ki {@code getBlockState} ne prepisejo
 * ({@link #supports(World)}).
 */
public final class MemoBlockAccess implements IBlockAccess {
    private final World world;
    private final ChunkMemo<Chunk> chunks;

    public MemoBlockAccess(World world) {
        this.world = world;
        // Anonimni razred namesto method reference: reobf mora preimenovati klic v SRG ime.
        this.chunks = new ChunkMemo<Chunk>(new ChunkMemo.Loader<Chunk>() {
            @Override
            public Chunk load(int cx, int cz) {
                return world.getChunkFromChunkCoords(cx, cz);
            }
        });
    }

    /**
     * Ali ima svet originalni {@code World.getBlockState}. Vanilla strezniska svetova ga ne
     * prepiseta; svet drugega moda bi ga lahko, zato zanj ostane original.
     */
    public static boolean supports(World world) {
        Class<?> c = world.getClass();
        return c == net.minecraft.world.WorldServer.class || c == net.minecraft.world.WorldServerMulti.class;
    }

    /** Zacetek novega klica: pozabi chunke prejsnjega (chunk se lahko med ticki raztovori). */
    public void reset() {
        this.chunks.reset();
    }

    public long lookups() {
        return this.chunks.lookups();
    }

    public long misses() {
        return this.chunks.misses();
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        if (pos.getY() < 0 || pos.getY() >= 256) {
            return Blocks.AIR.getDefaultState();
        }
        return this.chunks.get(pos.getX() >> 4, pos.getZ() >> 4).getBlockState(pos);
    }

    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        return this.world.getTileEntity(pos);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getCombinedLight(BlockPos pos, int lightValue) {
        return this.world.getCombinedLight(pos, lightValue);
    }

    @Override
    public boolean isAirBlock(BlockPos pos) {
        return this.world.isAirBlock(pos);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Biome getBiome(BlockPos pos) {
        return this.world.getBiome(pos);
    }

    @Override
    public int getStrongPower(BlockPos pos, EnumFacing direction) {
        return this.world.getStrongPower(pos, direction);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public WorldType getWorldType() {
        return this.world.getWorldType();
    }

    @Override
    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
        return this.world.isSideSolid(pos, side, _default);
    }
}
