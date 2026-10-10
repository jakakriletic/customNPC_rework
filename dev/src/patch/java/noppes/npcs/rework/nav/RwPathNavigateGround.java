package noppes.npcs.rework.nav;

import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import noppes.npcs.LogWriter;

/**
 * Kopenski navigator NPC-ja. Od {@link PathNavigateGround} se razlikuje samo v node procesorju
 * ({@link RwWalkNodeProcessor}) in v {@link #isDirectPathBetweenPoints}, ki ga {@code pathFollow}
 * klice vsak tick sledenja poti (M5.10: {@link #pathFollow} je prepisan samo za merjenje, vedno
 * klice {@code super}).
 *
 * <p>M5.11 (S14b): v nacinu {@link PathFollowCache#NODE_MEMO} se klice {@code super}, node
 * procesor pa si za cas klica zapomni posamezne ocene tipa vozlisca po poziciji; nacin
 * {@link PathFollowCache#VERIFY_NODE} primerja z originalom. S14 (bloki prek predpomnilnika
 * chunkov, prepis vanilla metode) je bil z A/B 9. 10. v sumu in je odstranjen.
 *
 * <p>Razred je navigator vsakega kopenskega NPC-ja ne glede na stikalo; pri nacinu 0 (privzeto)
 * se klice {@code super}, torej original. {@code instanceof PathNavigateGround} ostane resnicen.
 */
public class RwPathNavigateGround extends PathNavigateGround {
    /** M5.10: kandidati v trenutnem pathFollow (samo pri vklopljenem merjenju). */
    private int candidates;

    public RwPathNavigateGround(EntityLiving entity, World world) {
        super(entity, world);
    }

    /**
     * Enako kot {@code PathNavigateGround.getPathFinder} (2847), le da je node procesor
     * {@link RwWalkNodeProcessor} (podrazred {@code WalkNodeProcessor}, izven pomnjenja enak).
     * Klice ga konstruktor {@code PathNavigate}, preden so polja tega razreda nastavljena.
     */
    @Override
    protected PathFinder getPathFinder() {
        this.nodeProcessor = new RwWalkNodeProcessor();
        this.nodeProcessor.setCanEnterDoors(true);
        return new PathFinder(this.nodeProcessor);
    }

    /**
     * M5.9: pri vklopljenem merjenju ({@link PathSearch#setTiming}) izmeri in razvrsti vsako
     * iskanje poti. Vse poti ({@code getPathToEntityLiving}, {@code getPathToXYZ}) vodijo skozi to
     * metodo [K]. Obnasanje je v obeh vejah {@code super.getPathToPos(pos)}.
     */
    @Override
    public Path getPathToPos(BlockPos pos) {
        if (!PathSearch.timing()) {
            return super.getPathToPos(pos);
        }
        Path before = this.currentPath;
        long t0 = System.nanoTime();
        Path path = super.getPathToPos(pos);
        long dt = System.nanoTime() - t0;
        int distance = 0;
        if (path != null && path != before) {
            PathPoint end = path.getFinalPathPoint();
            if (end == null) {
                distance = 999;
            } else {
                distance = Math.max(Math.max(Math.abs(end.x - pos.getX()), Math.abs(end.y - pos.getY())),
                        Math.abs(end.z - pos.getZ()));
            }
        }
        PathSearch.record(PathSearch.classify(path != null, path != null && path == before, distance), dt, distance);
        return path;
    }

    /**
     * M5.10: pri vklopljenem merjenju ({@link PathFollowCache#setTiming}) izmeri cel vanilla
     * {@code pathFollow} in presteje kandidate (klice {@link #isDirectPathBetweenPoints}) v njem.
     * Obnasanje je v obeh vejah {@code super.pathFollow()}.
     */
    @Override
    protected void pathFollow() {
        if (!PathFollowCache.timing()) {
            super.pathFollow();
            return;
        }
        this.candidates = 0;
        int mode = PathFollowCache.mode();
        long t0 = System.nanoTime();
        super.pathFollow();
        PathFollowCache.recordFollow(mode, System.nanoTime() - t0, this.candidates);
    }

    @Override
    protected boolean isDirectPathBetweenPoints(Vec3d posVec31, Vec3d posVec32, int sizeX, int sizeY, int sizeZ) {
        if (!PathFollowCache.timing()) {
            return this.directPathByMode(posVec31, posVec32, sizeX, sizeY, sizeZ);
        }
        ++this.candidates;
        int mode = PathFollowCache.mode();
        long t0 = System.nanoTime();
        boolean result = this.directPathByMode(posVec31, posVec32, sizeX, sizeY, sizeZ);
        PathFollowCache.recordDirect(mode, System.nanoTime() - t0, result);
        return result;
    }

    private boolean directPathByMode(Vec3d posVec31, Vec3d posVec32, int sizeX, int sizeY, int sizeZ) {
        int mode = PathFollowCache.mode();
        if (mode == PathFollowCache.ORIGINAL || !(this.nodeProcessor instanceof RwWalkNodeProcessor)) {
            return super.isDirectPathBetweenPoints(posVec31, posVec32, sizeX, sizeY, sizeZ);
        }
        if (mode == PathFollowCache.NODE_MEMO) {
            return this.nodeMemoDirectPath(posVec31, posVec32, sizeX, sizeY, sizeZ);
        }
        boolean original = super.isDirectPathBetweenPoints(posVec31, posVec32, sizeX, sizeY, sizeZ);
        boolean memo = this.nodeMemoDirectPath(posVec31, posVec32, sizeX, sizeY, sizeZ);
        ++PathFollowCache.compared;
        if (original != memo) {
            if (++PathFollowCache.mismatches <= 5) {
                LogWriter.info("RWPATH neujemanje: original=" + original + " pomnjenje=" + memo + " od=" + posVec31
                        + " do=" + posVec32 + " velikost=" + sizeX + "/" + sizeY + "/" + sizeZ);
            }
        }
        return original;
    }

    /** M5.11 (S14b): vanilla klic s pomnjenjem posameznih ocen tipa vozlisca za cas klica. */
    private boolean nodeMemoDirectPath(Vec3d posVec31, Vec3d posVec32, int sizeX, int sizeY, int sizeZ) {
        RwWalkNodeProcessor processor = (RwWalkNodeProcessor) this.nodeProcessor;
        ++PathFollowCache.calls;
        long l0 = processor.memoLookups();
        long m0 = processor.memoMisses();
        processor.beginMemo();
        try {
            return super.isDirectPathBetweenPoints(posVec31, posVec32, sizeX, sizeY, sizeZ);
        } finally {
            processor.endMemo();
            PathFollowCache.lookups += processor.memoLookups() - l0;
            PathFollowCache.misses += processor.memoMisses() - m0;
        }
    }
}
