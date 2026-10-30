package noppes.npcs.rework.nav;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigateFlying;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.pathfinding.RwPathFinder;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.ForgeEventFactory;

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
 *
 * <p>P3 (Q256): pri {@code RwPathResume=1} obnova, ki z vanilla iskanjem ne napreduje, enkrat isce
 * z {@link RwPathFinder} in mejo {@link PathResume#WIDE_NODES}; boljsi izid nadomesti vanilla izid.
 */
public class RwPathNavigateFlying extends PathNavigateFlying {
    private final PathResume resume = new PathResume();
    /** Pot, ki jo je vrnil zadnji ukaz; {@link #setPath} z drugo potjo nadaljevanje prekine. */
    private Path commandPath;
    /** Klic, ki ni ukaz: obnova, pot do entitete, vanilla {@code updatePath}. */
    private boolean passThrough;
    /** P3 (Q256): iskalnik z mejo {@link PathResume#WIDE_NODES}; ustvarjen ob prvi obnovi brez napredka. */
    private RwPathFinder wideFinder;
    /** M4.17 poskus 3: seznam skatel in celica za {@link #cells}; samo strezniska nit. */
    private final List<AxisAlignedBB> boxes = new ArrayList<AxisAlignedBB>();
    private final BlockPos.MutableBlockPos cell = new BlockPos.MutableBlockPos();
    private AxisAlignedBB query;
    /** M4.17 poskus 4: chunk stolpca, ki ga je nazadnje potrdil {@code loaded}. */
    private Chunk column;
    /**
     * Poskus 4 (Q86): chunk se poisce enkrat na stolpec namesto v vsaki celici, zrak se preskoci.
     * Enakost [K]: {@code World.getBlockState} = {@code getChunkFromChunkCoords(x >> 4, z >> 4)
     * .getBlockState(x, y, z)} oziroma zrak zunaj y 0-255, kar {@code Chunk.getBlockState} vrne sam;
     * {@link FlightFollow#scan} pred celicami stolpca vedno poklice {@code loaded} istega stolpca, svet
     * se med zanko ne spremeni. {@code BlockAir} ima {@code NULL_AABB}, zato v seznam ne doda nicesar.
     */
    private final FlightFollow.Cells cells = new FlightFollow.Cells() {
        @Override
        public boolean loaded(int x, int z) {
            World w = RwPathNavigateFlying.this.world;
            if (!w.isBlockLoaded(RwPathNavigateFlying.this.cell.setPos(x, 64, z))) {
                return false;
            }
            RwPathNavigateFlying.this.column = w.getChunkFromChunkCoords(x >> 4, z >> 4);
            return true;
        }

        @Override
        public boolean blocked(int x, int y, int z) {
            IBlockState state = RwPathNavigateFlying.this.column.getBlockState(x, y, z);
            if (state.getBlock() == Blocks.AIR) {
                return false;
            }
            BlockPos.MutableBlockPos pos = RwPathNavigateFlying.this.cell.setPos(x, y, z);
            state.addCollisionBoxToList(RwPathNavigateFlying.this.world, pos, RwPathNavigateFlying.this.query,
                    RwPathNavigateFlying.this.boxes, null, false);
            return !RwPathNavigateFlying.this.boxes.isEmpty();
        }
    };
    /**
     * M4.17: trk kvadra z bloki, enak {@code World.collidesWithAnyBlock}. Poskus 3 (Q85): dogodek
     * {@code GetCollisionBoxesEvent} enkrat namesto v vsaki celici ({@link FlightFollow#scan}), seznam
     * se ponovno uporabi.
     */
    private final FlightFollow.BoxTest blocks = (minX, minY, minZ, maxX, maxY, maxZ) -> {
        AxisAlignedBB bb = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
        this.boxes.clear();
        if (!ForgeEventFactory.gatherCollisionBoxes(this.world, null, bb, this.boxes)) {
            return true;
        }
        this.query = bb;
        return FlightFollow.scan(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ, this.cells);
    };

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
            BlockPos target = new BlockPos(t[0], t[1], t[2]);
            Path path = super.getPathToPos(target);
            PathPoint end = path == null ? null : path.getFinalPathPoint();
            if (this.resume.stalls(end != null, end == null ? 0 : end.x, end == null ? 0 : end.y, end == null ? 0 : end.z)) {
                long start = System.nanoTime();
                Path wide = this.widePath(target);
                PathPoint wideEnd = wide == null ? null : wide.getFinalPathPoint();
                boolean progress = wideEnd != null && !this.resume.stalls(true, wideEnd.x, wideEnd.y, wideEnd.z);
                PathResume.recordWide(progress, System.nanoTime() - start);
                if (progress) {
                    path = wide;
                    end = wideEnd;
                }
            }
            if (this.resume.resumeResult(end != null, end == null ? 0 : end.x, end == null ? 0 : end.y, end == null ? 0 : end.z)) {
                this.commandPath = path;
                super.setPath(path, this.speed);
            }
        } finally {
            this.passThrough = outer;
        }
    }

    /**
     * P3: isto kot vanilla {@code getPathToPos} (isti ChunkCache in domet), z mejo
     * {@link PathResume#WIDE_NODES} namesto 200. {@code targetPos} je ze nastavil predhodni klic
     * {@code super.getPathToPos} do istega bloka.
     */
    private Path widePath(BlockPos target) {
        if (this.wideFinder == null) {
            this.wideFinder = new RwPathFinder(this.nodeProcessor, PathResume.WIDE_NODES);
        }
        float range = this.getPathSearchRange();
        this.world.profiler.startSection("pathfind");
        BlockPos at = new BlockPos(this.entity);
        int i = (int) (range + 8.0F);
        ChunkCache cache = new ChunkCache(this.world, at.add(-i, -i, -i), at.add(i, i, i), 0);
        Path path = this.wideFinder.findPath(cache, this.entity, target, range);
        this.world.profiler.endSection();
        return path;
    }
}
