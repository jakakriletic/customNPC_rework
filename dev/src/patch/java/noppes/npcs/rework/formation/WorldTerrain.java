package noppes.npcs.rework.formation;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * {@link Terrain} nad pravim svetom. Mesto je stojno, ce sta blok stopal in blok glave
 * prehodna (in ne lava) ter blok pod stopali blokira gibanje. Nenalozenih chunkov ne bere,
 * da planer nikoli ne sprozi nalaganja chunka.
 */
final class WorldTerrain implements Terrain {
    /** Iskanje od enega bloka nad namigom do treh pod njim: stopnica gor ali tri dol. */
    private static final int UP = 1;
    private static final int DOWN = 3;

    private final World world;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    WorldTerrain(World world) {
        this.world = world;
    }

    @Override
    public double standY(double x, double yHint, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        int y0 = (int) Math.floor(yHint + 0.01);
        pos.setPos(bx, y0, bz);
        if (!world.isBlockLoaded(pos)) {
            return Double.NaN;
        }
        for (int dy = UP; dy >= -DOWN; dy--) {
            int y = y0 + dy;
            if (y < 1 || y > 254) {
                continue;
            }
            if (passable(bx, y, bz) && passable(bx, y + 1, bz) && solid(bx, y - 1, bz)) {
                return y;
            }
        }
        return Double.NaN;
    }

    private boolean passable(int x, int y, int z) {
        IBlockState state = world.getBlockState(pos.setPos(x, y, z));
        Material m = state.getMaterial();
        return !m.blocksMovement() && m != Material.LAVA && m != Material.FIRE;
    }

    private boolean solid(int x, int y, int z) {
        return world.getBlockState(pos.setPos(x, y, z)).getMaterial().blocksMovement();
    }
}
