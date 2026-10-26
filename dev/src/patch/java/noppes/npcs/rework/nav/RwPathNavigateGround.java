package noppes.npcs.rework.nav;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
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

    // M4.10 (D-084): nadaljevanje delne poti in vmesni cilj; pri RwPathResume=0 neaktivno.
    private final PathResume resume = new PathResume();
    /** Pot, ki jo je vrnil zadnji ukaz; {@link #setPath} z drugo potjo nadaljevanje prekine. */
    private Path commandPath;
    /** Iskanje obnove: mimo ukaza in mimo negativnega predpomnilnika (S5). */
    private boolean resumeSearch;
    /** Klic znotraj vanilla (pot do entitete, {@code updatePath}): ni ukaz, predpomnilnik velja. */
    private boolean passThrough;

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
     * <p>M4.10 (D-084): pri {@link PathResume#ON} je zunanji klic ukaz: cilj se zapomni, pri cilju
     * izven dometa se isce do vmesnega cilja ({@link PathResume}).
     *
     * <p>V nacinu 0 brez merjenja je to natanko {@code super.getPathToPos(pos)}.
     */
    @Override
    public Path getPathToPos(BlockPos pos) {
        if (!PathResume.on() || this.resumeSearch || this.passThrough) {
            return this.searchCounted(pos);
        }
        int[] t = this.resume.command(MathHelper.floor(this.entity.posX), MathHelper.floor(this.entity.posY),
                MathHelper.floor(this.entity.posZ), pos.getX(), pos.getY(), pos.getZ(), this.getPathSearchRange());
        BlockPos search = t[0] == pos.getX() && t[1] == pos.getY() && t[2] == pos.getZ() ? pos : new BlockPos(t[0], t[1], t[2]);
        Path path = this.searchCounted(search);
        PathPoint end = path == null ? null : path.getFinalPathPoint();
        this.resume.commandResult(end != null, end == null ? 0 : end.x, end == null ? 0 : end.y, end == null ? 0 : end.z);
        this.commandPath = path;
        return path;
    }

    /** M4.10: pot do entitete ni ukaz do bloka; nadaljevanje se prekine. */
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

    /** M4.10: vanilla ponovno iskanje do istega bloka ({@code targetPos}) ni nov ukaz. */
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

    /** M4.10: tuja pot (ne tista iz zadnjega ukaza) nadaljevanje prekine. */
    @Override
    public boolean setPath(Path pathentityIn, double speedIn) {
        if (!this.resumeSearch && pathentityIn != this.commandPath) {
            this.resume.cancel();
        }
        return super.setPath(pathentityIn, speedIn);
    }

    /**
     * M4.10: po vanilla posodobitvi, ce je pot ukaza prehojena in ni koncala pri cilju, poisce
     * naslednjo. Ob {@code clearPath} (obticanje, iztek, skripta) se nadaljevanje prekine.
     */
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
        this.resumeSearch = true;
        try {
            Path path = this.getPathToPos(new BlockPos(t[0], t[1], t[2]));
            PathPoint end = path == null ? null : path.getFinalPathPoint();
            if (this.resume.resumeResult(end != null, end == null ? 0 : end.x, end == null ? 0 : end.y, end == null ? 0 : end.z)) {
                this.commandPath = path;
                this.setPath(path, this.speed);
            }
        } finally {
            this.resumeSearch = false;
        }
    }

    /** Iskanje z merjenjem, negativnim predpomnilnikom in pomnjenjem (M5.6, M5.9, M5.17). */
    private Path searchCounted(BlockPos pos) {
        int neg = NegativePathCache.mode();
        if (neg == NegativePathCache.ORIGINAL && !PathSearch.timing()
                && PathSearchMemo.mode() == PathSearchMemo.ORIGINAL) {
            return super.getPathToPos(pos);
        }
        int now = (int) this.world.getTotalWorldTime();
        boolean memoApplies = NegativePathCache.applies(this.memoHasFail, now - this.memoTick,
                this.memoDistance(pos));
        if (neg == NegativePathCache.ON && memoApplies && !this.resumeSearch) {
            ++NegativePathCache.skipped;
            return null;
        }

        Path before = this.currentPath;
        long t0 = PathSearch.timing() ? System.nanoTime() : 0L;
        Path path = this.searchByMode(pos);
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

    /**
     * M5.17 (S14c): iskanje poti s pomnjenjem ocen tipa vozlisca za cas celega klica. V nacinu 0 je
     * to natanko {@code super.getPathToPos(pos)}; pomnjena metoda je ista kot pri S14b, zato velja
     * isti dokaz enakosti ({@link PathSearchMemo}).
     */
    private Path searchByMode(BlockPos pos) {
        int memoMode = PathSearchMemo.mode();
        if (memoMode == PathSearchMemo.ORIGINAL || !(this.nodeProcessor instanceof RwWalkNodeProcessor)) {
            return super.getPathToPos(pos);
        }
        RwWalkNodeProcessor processor = (RwWalkNodeProcessor) this.nodeProcessor;
        long l0 = processor.memoLookups();
        long m0 = processor.memoMisses();
        long x0 = processor.memoMismatches();
        processor.beginMemo(memoMode == PathSearchMemo.VERIFY);
        try {
            return super.getPathToPos(pos);
        } finally {
            processor.endMemo();
            ++PathSearchMemo.searches;
            PathSearchMemo.lookups += processor.memoLookups() - l0;
            PathSearchMemo.misses += processor.memoMisses() - m0;
            PathSearchMemo.mismatches += processor.memoMismatches() - x0;
        }
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
