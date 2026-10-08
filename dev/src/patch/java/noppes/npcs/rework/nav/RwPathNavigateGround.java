package noppes.npcs.rework.nav;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import noppes.npcs.LogWriter;

/**
 * M5-S S14: kopenski navigator NPC-ja. Od {@link PathNavigateGround} se razlikuje samo v
 * {@link #isDirectPathBetweenPoints}, ki ga {@code pathFollow} klice vsak tick sledenja poti.
 *
 * <p>V nacinu {@link PathFollowCache#MEMO} je metoda prepis vanilla kode (Forge
 * 14.23.5.2847, {@code PathNavigateGround.isDirectPathBetweenPoints}, {@code isSafeToStandAt},
 * {@code isPositionClear}) z eno razliko: bloke bere prek {@link MemoBlockAccess} namesto
 * {@code this.world}. Vrstni red in koordinate branj so enaki; enakost izida preveri nacin
 * {@link PathFollowCache#VERIFY} v svetu.
 *
 * <p>Razred je navigator vsakega kopenskega NPC-ja ne glede na stikalo; pri nacinu 0 (privzeto)
 * se klice {@code super}, torej original. {@code instanceof PathNavigateGround} ostane resnicen.
 */
public class RwPathNavigateGround extends PathNavigateGround {
    private final MemoBlockAccess access;

    public RwPathNavigateGround(EntityLiving entity, World world) {
        super(entity, world);
        this.access = MemoBlockAccess.supports(world) ? new MemoBlockAccess(world) : null;
    }

    @Override
    protected boolean isDirectPathBetweenPoints(Vec3d posVec31, Vec3d posVec32, int sizeX, int sizeY, int sizeZ) {
        int mode = PathFollowCache.mode();
        if (mode == PathFollowCache.ORIGINAL || this.access == null) {
            return super.isDirectPathBetweenPoints(posVec31, posVec32, sizeX, sizeY, sizeZ);
        }
        if (mode == PathFollowCache.VERIFY) {
            boolean original = super.isDirectPathBetweenPoints(posVec31, posVec32, sizeX, sizeY, sizeZ);
            boolean memo = this.memoDirectPath(posVec31, posVec32, sizeX, sizeY, sizeZ);
            ++PathFollowCache.compared;
            if (original != memo) {
                if (++PathFollowCache.mismatches <= 5) {
                    LogWriter.info("RWPATH neujemanje: original=" + original + " predpomnilnik=" + memo + " od=" + posVec31
                            + " do=" + posVec32 + " velikost=" + sizeX + "/" + sizeY + "/" + sizeZ);
                }
            }
            return original;
        }
        return this.memoDirectPath(posVec31, posVec32, sizeX, sizeY, sizeZ);
    }

    private boolean memoDirectPath(Vec3d posVec31, Vec3d posVec32, int sizeX, int sizeY, int sizeZ) {
        ++PathFollowCache.calls;
        this.access.reset();
        long l0 = this.access.lookups();
        long m0 = this.access.misses();
        try {
            return this.directPath(this.access, posVec31, posVec32, sizeX, sizeY, sizeZ);
        } finally {
            PathFollowCache.lookups += this.access.lookups() - l0;
            PathFollowCache.misses += this.access.misses() - m0;
            // Brez referenc na chunke med ticki (chunk se lahko raztovori).
            this.access.reset();
        }
    }

    // --- prepis vanilla PathNavigateGround (2847); edina sprememba: this.world -> blocks ---

    private boolean directPath(IBlockAccess blocks, Vec3d posVec31, Vec3d posVec32, int sizeX, int sizeY, int sizeZ) {
        int i = MathHelper.floor(posVec31.x);
        int j = MathHelper.floor(posVec31.z);
        double d0 = posVec32.x - posVec31.x;
        double d1 = posVec32.z - posVec31.z;
        double d2 = d0 * d0 + d1 * d1;

        if (d2 < 1.0E-8D) {
            return false;
        }
        double d3 = 1.0D / Math.sqrt(d2);
        d0 = d0 * d3;
        d1 = d1 * d3;
        sizeX = sizeX + 2;
        sizeZ = sizeZ + 2;

        if (!this.isSafeToStandAt(blocks, i, (int) posVec31.y, j, sizeX, sizeY, sizeZ, posVec31, d0, d1)) {
            return false;
        }
        sizeX = sizeX - 2;
        sizeZ = sizeZ - 2;
        double d4 = 1.0D / Math.abs(d0);
        double d5 = 1.0D / Math.abs(d1);
        double d6 = (double) i - posVec31.x;
        double d7 = (double) j - posVec31.z;

        if (d0 >= 0.0D) {
            ++d6;
        }
        if (d1 >= 0.0D) {
            ++d7;
        }

        d6 = d6 / d0;
        d7 = d7 / d1;
        int k = d0 < 0.0D ? -1 : 1;
        int l = d1 < 0.0D ? -1 : 1;
        int i1 = MathHelper.floor(posVec32.x);
        int j1 = MathHelper.floor(posVec32.z);
        int k1 = i1 - i;
        int l1 = j1 - j;

        while (k1 * k > 0 || l1 * l > 0) {
            if (d6 < d7) {
                d6 += d4;
                i += k;
                k1 = i1 - i;
            } else {
                d7 += d5;
                j += l;
                l1 = j1 - j;
            }

            if (!this.isSafeToStandAt(blocks, i, (int) posVec31.y, j, sizeX, sizeY, sizeZ, posVec31, d0, d1)) {
                return false;
            }
        }
        return true;
    }

    private boolean isSafeToStandAt(IBlockAccess blocks, int x, int y, int z, int sizeX, int sizeY, int sizeZ,
                                    Vec3d vec31, double p_179683_8_, double p_179683_10_) {
        int i = x - sizeX / 2;
        int j = z - sizeZ / 2;

        if (!this.isPositionClear(blocks, i, y, j, sizeX, sizeY, sizeZ, vec31, p_179683_8_, p_179683_10_)) {
            return false;
        }
        for (int k = i; k < i + sizeX; ++k) {
            for (int l = j; l < j + sizeZ; ++l) {
                double d0 = (double) k + 0.5D - vec31.x;
                double d1 = (double) l + 0.5D - vec31.z;

                if (d0 * p_179683_8_ + d1 * p_179683_10_ >= 0.0D) {
                    PathNodeType pathnodetype = this.nodeProcessor.getPathNodeType(blocks, k, y - 1, l, this.entity, sizeX, sizeY, sizeZ, true, true);

                    if (pathnodetype == PathNodeType.WATER) {
                        return false;
                    }
                    if (pathnodetype == PathNodeType.LAVA) {
                        return false;
                    }
                    if (pathnodetype == PathNodeType.OPEN) {
                        return false;
                    }

                    pathnodetype = this.nodeProcessor.getPathNodeType(blocks, k, y, l, this.entity, sizeX, sizeY, sizeZ, true, true);
                    float f = this.entity.getPathPriority(pathnodetype);

                    if (f < 0.0F || f >= 8.0F) {
                        return false;
                    }
                    if (pathnodetype == PathNodeType.DAMAGE_FIRE || pathnodetype == PathNodeType.DANGER_FIRE || pathnodetype == PathNodeType.DAMAGE_OTHER) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean isPositionClear(IBlockAccess blocks, int x, int y, int z, int sizeX, int sizeY, int sizeZ,
                                    Vec3d p_179692_7_, double p_179692_8_, double p_179692_10_) {
        for (BlockPos blockpos : BlockPos.getAllInBox(new BlockPos(x, y, z), new BlockPos(x + sizeX - 1, y + sizeY - 1, z + sizeZ - 1))) {
            double d0 = (double) blockpos.getX() + 0.5D - p_179692_7_.x;
            double d1 = (double) blockpos.getZ() + 0.5D - p_179692_7_.z;

            if (d0 * p_179692_8_ + d1 * p_179692_10_ >= 0.0D) {
                Block block = blocks.getBlockState(blockpos).getBlock();

                if (!block.isPassable(blocks, blockpos)) {
                    return false;
                }
            }
        }
        return true;
    }
}
