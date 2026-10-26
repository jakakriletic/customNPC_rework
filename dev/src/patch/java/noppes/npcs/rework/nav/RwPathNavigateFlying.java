package noppes.npcs.rework.nav;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigateFlying;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * Navigator letecega NPC-ja ({@code movementType == 1}). Od {@link PathNavigateFlying} se razlikuje
 * samo pri {@code RwPathResume=1} (M4.10, D-084): nadaljevanje delne poti in vmesni cilj izven
 * dometa, isto pravilo kot {@link RwPathNavigateGround} ({@link PathResume}). Leteci navigator
 * nima negativnega predpomnilnika (S5) ne merjenja iskanja.
 *
 * <p>Razred je navigator vsakega letecega NPC-ja ne glede na stikalo; pri nacinu 0 (privzeto) se
 * vsaka metoda konca v {@code super}, torej original. {@code instanceof PathNavigateFlying} ostane
 * resnicen. M4.17 (preverba vidne crte pri sledenju) se doda sem.
 */
public class RwPathNavigateFlying extends PathNavigateFlying {
    private final PathResume resume = new PathResume();
    /** Pot, ki jo je vrnil zadnji ukaz; {@link #setPath} z drugo potjo nadaljevanje prekine. */
    private Path commandPath;
    /** Klic, ki ni ukaz: obnova, pot do entitete, vanilla {@code updatePath}. */
    private boolean passThrough;

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
