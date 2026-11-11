package si.ladja.xaerofleet.integration;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

/**
 * Resolves a safe surface for an army target. Since v0.1.11 (CNPC M11.11) a target in an unloaded chunk is
 * allowed: the chunk is loaded from disk, or generated, once for this click (the army's own chunk tickets
 * carry the march there). When the clicked column itself is unsafe (water, lava, cactus...), the nearest safe
 * column within {@link #SEARCH} blocks is used instead.
 */
public final class ArmyGround {
    public static final int SEARCH = 8;
    private ArmyGround() {}
    public static BlockPos resolve(WorldServer world,double x,double z) {
        if(world==null || !Double.isFinite(x) || !Double.isFinite(z)
                || Math.abs(x)>29999984 || Math.abs(z)>29999984)return null;
        int bx=(int)Math.floor(x),bz=(int)Math.floor(z);
        for(int r=0;r<=SEARCH;r++){
            BlockPos best=null;double bestD=Double.MAX_VALUE;
            for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
                if(Math.max(Math.abs(dx),Math.abs(dz))!=r)continue;
                BlockPos feet=column(world,bx+dx,bz+dz);
                double d=dx*dx+dz*dz;
                if(feet!=null && d<bestD){best=feet;bestD=d;}
            }
            if(best!=null)return best;
        }
        return null;
    }
    /** Feet position on the topmost safe surface of one column, loading the chunk when needed. */
    static BlockPos column(WorldServer world,int x,int z) {
        BlockPos column=new BlockPos(x,0,z);
        if(!world.getWorldBorder().contains(column))return null;
        // provideChunk loads from disk or generates; at most the few chunks around one move click.
        if(world.getChunkProvider().provideChunk(x>>4,z>>4)==null)return null;
        int y=Math.min(253,world.getHeight(column).getY()-1);
        if(y<1)return null;
        // Topmost non-air surface only: do not silently route beneath water, roofs or lava.
        BlockPos floor=new BlockPos(x,y,z);
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
