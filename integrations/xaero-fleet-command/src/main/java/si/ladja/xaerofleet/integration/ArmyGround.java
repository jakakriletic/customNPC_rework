package si.ladja.xaerofleet.integration;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

/** Resolves the clicked surface only in an already loaded column. Never generates chunks. */
public final class ArmyGround {
    private ArmyGround() {}
    public static BlockPos resolve(WorldServer world,double x,double z) {
        if(world==null || !Double.isFinite(x) || !Double.isFinite(z)
                || Math.abs(x)>29999984 || Math.abs(z)>29999984)return null;
        BlockPos column=new BlockPos(x,0,z);
        if(world.getChunkProvider().getLoadedChunk(column.getX()>>4,column.getZ()>>4)==null)return null;
        int y=Math.min(253,world.getHeight(column).getY()-1);
        // Topmost non-air surface only: do not silently route beneath water, roofs or lava.
        BlockPos floor=new BlockPos(column.getX(),y,column.getZ());
        IBlockState ground=world.getBlockState(floor);
        if(!ground.isTopSolid() || ground.getMaterial().isLiquid() || ground.getBlock()==Blocks.MAGMA
                || ground.getBlock()==Blocks.CACTUS)return null;
        BlockPos feet=floor.up();
        if(!world.getWorldBorder().contains(feet) || !clear(world,feet) || !clear(world,feet.up()))return null;
        return feet;
    }
    private static boolean clear(WorldServer w,BlockPos p) {
        IBlockState s=w.getBlockState(p);
        return s.getCollisionBoundingBox(w,p)==null && !s.getMaterial().isLiquid()
                && s.getBlock()!=Blocks.FIRE && s.getBlock()!=Blocks.CACTUS;
    }
}
