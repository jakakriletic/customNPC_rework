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

    // M5.6 (S5): spomin na zadnje neuspelo iskanje tega NPC-ja (brez alokacije, ena reza).
    private boolean memoHasFail;
    private int memoX;
    private int memoY;
    private int memoZ;
    private int memoTick;

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
     * metodo [K].
     *
     * <p>M5.6 (S5): v nacinu {@link NegativePathCache#ON} se iskanje preskoci in vrne {@code null},
     * ce je isti NPC do (skoraj) iste tarce v zadnjih {@link NegativePathCache#ttl()} tickih ze
     * iskal brez cele poti. Dokler task tece, {@code tryMoveToEntityLiving} ob {@code null} obdrzi
     * obstojeco pot (ne poklice {@code setPath}) [K]; ko task ne tece,
     * {@code EntityAIAttackTarget.shouldExecute} ostane {@code false} - to je sprememba obnasanja,
     * ki jo nacin {@link NegativePathCache#VERIFY} izmeri.
     *
     * <p>V nacinu 0 brez merjenja je to natanko {@code super.getPathToPos(pos)}.
     */
    @Override
    public Path getPathToPos(BlockPos pos) {
        int neg = NegativePathCache.mode();
        if (neg == NegativePathCache.ORIGINAL && !PathSearch.timing()) {
            return super.getPathToPos(pos);
        }
        int now = (int) this.world.getTotalWorldTime();
        boolean memoApplies = NegativePathCache.applies(this.memoHasFail, now - this.memoTick,
                this.memoDistance(pos));
        if (neg == NegativePathCache.ON && memoApplies) {
            ++NegativePathCache.skipped;
            return null;
        }

        Path before = this.currentPath;
        long t0 = PathSearch.timing() ? System.nanoTime() : 0L;
        Path path = super.getPathToPos(pos);
        boolean fresh = path != before;
        int distance = 0;
        if (path != null && fresh) {
            PathPoint end = path.getFinalPathPoint();
            if (end == null) {
                distance = 999;
            } else {
                distance = Math.max(Math.max(Math.abs(end.x - pos.getX()), Math.abs(end.y - pos.getY())),
                        Math.abs(end.z - pos.getZ()));
            }
        }
        if (PathSearch.timing()) {
            PathSearch.record(PathSearch.classify(path != null, path != null && !fresh, distance),
                    System.nanoTime() - t0, distance);
        }
        if (neg == NegativePathCache.VERIFY && memoApplies) {
            // Predpomnjeni odgovor bi bil null; ujemanje pomeni, da iskanje tudi zdaj ne da poti.
            ++NegativePathCache.compared;
            if (path != null) {
                ++NegativePathCache.mismatches;
            }
        }
        // Nacin 2 ob veljavnem spominu spomina ne osvezi, da je zivljenjski cikel enak nacinu 1
        // in sta stevca primerjav in preskokov neposredno primerljiva.
        if (neg != NegativePathCache.ORIGINAL && fresh && !(neg == NegativePathCache.VERIFY && memoApplies)) {
            if (path == null || (distance > 0 && NegativePathCache.partialCounts())) {
                this.memoHasFail = true;
                this.memoX = pos.getX();
                this.memoY = pos.getY();
                this.memoZ = pos.getZ();
                this.memoTick = now;
                ++NegativePathCache.stored;
            } else {
                this.memoHasFail = false;
            }
        }
        return path;
    }

    /** Cebiseva razdalja med zapisano neuspelo tarco in zdajsnjo (velika, ce spomina ni). */
    private int memoDistance(BlockPos pos) {
        if (!this.memoHasFail) {
            return Integer.MAX_VALUE;
        }
        return Math.max(Math.max(Math.abs(this.memoX - pos.getX()), Math.abs(this.memoY - pos.getY())),
                Math.abs(this.memoZ - pos.getZ()));
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
