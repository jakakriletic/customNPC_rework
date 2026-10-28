package noppes.npcs.rework.nav;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigateFlying;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Navigator letecega NPC-ja ({@code movementType == 1}). Od {@link PathNavigateFlying} se razlikuje
 * samo pri {@code RwPathResume=1} (M4.10, D-084): nadaljevanje delne poti in vmesni cilj izven
 * dometa, isto pravilo kot {@link RwPathNavigateGround} ({@link PathResume}). Leteci navigator
 * nima negativnega predpomnilnika (S5) ne merjenja iskanja.
 *
 * <p>Razred je navigator vsakega letecega NPC-ja ne glede na stikalo; pri nacinu 0 (privzeto) se
 * vsaka metoda konca v {@code super}, torej original. {@code instanceof PathNavigateFlying} ostane
 * resnicen.
 *
 * <p>M4.17 (D-085): pri {@code RwFlyLineOfSight=1} {@link #isDirectPathBetweenPoints} preveri vidno
 * crto s premikom hitboxa in preskok omeji na {@link FlightFollow#MAX_SKIP} blokov
 * ({@link FlightFollow}); pri 0 vanilla, ki vedno vrne {@code true}.
 */
public class RwPathNavigateFlying extends PathNavigateFlying {
    private final PathResume resume = new PathResume();
    /** Pot, ki jo je vrnil zadnji ukaz; {@link #setPath} z drugo potjo nadaljevanje prekine. */
    private Path commandPath;
    /** Klic, ki ni ukaz: obnova, pot do entitete, vanilla {@code updatePath}. */
    private boolean passThrough;
    /** M4.17: trk kvadra z bloki ({@code World.collidesWithAnyBlock}). */
    private final FlightFollow.BoxTest blocks = (minX, minY, minZ, maxX, maxY, maxZ) ->
            this.world.collidesWithAnyBlock(new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ));

    public RwPathNavigateFlying(EntityLiving entity, World world) {
        super(entity, world);
    }

    /** Pri {@link PathResume#ON} je zunanji klic ukaz; v nacinu 0 natanko {@code super}. */
    @Override
    public Path getPathToPos(BlockPos pos) {
        if (!PathResume.on() || this.passThrough) {
            return super.getPathToPos(pos);
        }
        int[] t = this.resume.command(MathHelper.floor(this.entity.posX), MathHelper.floor(this.entity.posY),
                MathHelper.floor(this.entity.posZ), pos.getX(), pos.getY(), pos.getZ(), this.getPathSearchRange());
        BlockPos search = t[0] == pos.getX() && t[1] == pos.getY() && t[2] == pos.getZ() ? pos : new BlockPos(t[0], t[1], t[2]);
        Path path = super.getPathToPos(search);
        PathPoint end = path == null ? null : path.getFinalPathPoint();
        this.resume.commandResult(end != null, end == null ? 0 : end.x, end == null ? 0 : end.y, end == null ? 0 : end.z);
        this.commandPath = path;
        return path;
    }

    /** Vanilla leteci gre do entitete prek {@code getPathToPos}; to ni ukaz do bloka. */
    @Override
    public Path getPathToEntityLiving(Entity entityIn) {
        this.resume.cancel();
        boolean outer = this.passThrough;
        this.passThrough = true;
        try {
            return super.getPathToEntityLiving(entityIn);
        } finally {
            this.passThrough = outer;
        }
    }

    @Override
    public void updatePath() {
        boolean outer = this.passThrough;
        this.passThrough = true;
        try {
            super.updatePath();
        } finally {
            this.passThrough = outer;
        }
    }

    @Override
    public boolean setPath(Path pathentityIn, double speedIn) {
        if (!this.passThrough && pathentityIn != this.commandPath) {
            this.resume.cancel();
        }
        return super.setPath(pathentityIn, speedIn);
    }

    /**
     * M4.17: {@code pathFollow} klice to vsak tick za tocke poti iste visine, od najdaljse proti
     * trenutni; prva prosta postane trenutna [K]. {@code from} je polozaj NPC-ja
     * ({@code getEntityPosition}). V nacinu 0 natanko {@code super}.
     */
    @Override
    protected boolean isDirectPathBetweenPoints(Vec3d from, Vec3d to, int sizeX, int sizeY, int sizeZ) {
        if (!FlightFollow.on()) {
            return super.isDirectPathBetweenPoints(from, to, sizeX, sizeY, sizeZ);
        }
        AxisAlignedBB box = this.entity.getEntityBoundingBox();
        return FlightFollow.direct(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                to.x - from.x, to.y - from.y, to.z - from.z, this.blocks);
    }

    @Override
    public void onUpdateNavigation() {
        super.onUpdateNavigation();
        if (this.resume.active()) {
            this.continuePath();
        }
    }

    private void continuePath() {
        if (!PathResume.on() || this.currentPath == null) {
            this.resume.cancel();
            return;
        }
        if (!this.currentPath.isFinished() || !this.canNavigate()) {
            return;
        }
        int[] t = this.resume.next(MathHelper.floor(this.entity.posX), MathHelper.floor(this.entity.posY),
                MathHelper.floor(this.entity.posZ), this.getPathSearchRange());
        if (t == null) {
            return;
        }
        boolean outer = this.passThrough;
        this.passThrough = true;
        try {
            Path path = super.getPathToPos(new BlockPos(t[0], t[1], t[2]));
            PathPoint end = path == null ? null : path.getFinalPathPoint();
            if (this.resume.resumeResult(end != null, end == null ? 0 : end.x, end == null ? 0 : end.y, end == null ? 0 : end.z)) {
                this.commandPath = path;
                super.setPath(path, this.speed);
            }
        } finally {
            this.passThrough = outer;
        }
    }
}
