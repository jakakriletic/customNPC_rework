package noppes.npcs.rework.nav;

/**
 * M4.10 (D-084): nadaljevanje delne poti in cilj izven dometa.
 *
 * <p>Vanilla 1.12 {@code PathFinder} vozlisce sprejme samo, ce je pot do njega (manhattanska vsota
 * korakov, {@code distanceFromOrigin}) krajsa od dometa {@code FOLLOW_RANGE} (pri NPC-ju
 * {@code NpcNavRange}, 32), sosede pa samo, ce so od cilja blizje kot domet
 * ({@code findPathOptions}) [K]. Cilj, ki je od NPC-ja manhattansko {@code >=} domet, zato nikoli ne
 * dobi cele poti; pri 48 blokih po prostem ne dobi nobene ({@code navig0} 0/6 za leteci in kopenski,
 * M4.16 [M]). Pot, ki se ne konca pri cilju (delna: proracun 200 vozlisc ali ovira), NPC prehodi in
 * navigacija se konca (zid M4.16: 0/6, NPC obstane 9 blokov pred ciljem [M]).
 *
 * <p>Pri vklopljenem stikalu navigator (a) ob koncu poti, ki se ni koncala pri cilju, poisce novo
 * pot od tam, kjer je, in (b) pri cilju izven dometa isce do vmesnega cilja na crti proti cilju,
 * manhattansko {@link #MARGIN} manj od dometa. Oboje dokler pot ne konca pri cilju, dokler vsaka
 * nova pot konca vsaj {@link #MIN_PROGRESS} bloka blize cilju kot prejsnja (sicer je cilj s tega
 * mesta dokazano nedosegljiv) in najvec {@link #MAX_RESUMES} obnov na en ukaz. Nadaljevanje
 * prekine: obticanje ali iztek vanilla ({@code clearPath}), nov ukaz, pot do entitete ali tuja pot.
 *
 * <p>To je <b>sprememba obnasanja</b> (D-007): ukaz, ki je v originalu koncal brez poti ali pred
 * ciljem, zdaj pripelje do cilja. Zato stikalo {@code RwPathResume}, privzeto original.
 *
 * <p>Stanje enega NPC-ja je v instanci (v njegovem navigatorju), stikalo in stevci so staticni.
 * Bere jih samo strezniska nit. Razred nima odvisnosti od sveta, da je pravilo preverjeno z JUnit.
 */
public final class PathResume {
    public static final int ORIGINAL = 0;
    public static final int ON = 1;
    public static final int MODES = 2;

    /** Najvec obnov na en ukaz (pri dometu 32 je to ~450 blokov po prostem). */
    public static final int MAX_RESUMES = 16;
    /** Vmesni cilj je manhattansko toliko blokov blize od dometa (M4.0: domet - 4). */
    public static final int MARGIN = 4;
    /** Konec poti blize od tega (bloki) cilju pomeni celo pot (letec cilj je blok visje, M4.0 §2.4). */
    public static final double ARRIVED = 1.5;
    /** Nova pot mora koncati vsaj toliko blokov blize cilju kot prejsnja. */
    public static final double MIN_PROGRESS = 1.0;

    private static int mode = ORIGINAL;

    /** Ukazi, pri katerih se je nadaljevanje zacelo (prva pot ni koncala pri cilju). */
    static long started;
    /** Iskanja do vmesnega cilja (cilj izven dometa). */
    static long intermediates;
    /** Obnove ob koncu poti. */
    static long resumed;
    /** Nadaljevanja, koncana s potjo do cilja. */
    static long arrived;
    /** Ustavljena: nova pot ni blize cilju ali je ni. */
    static long noProgress;
    /** Ustavljena: dosezena meja obnov. */
    static long limit;
    /** Prekinjena: obticanje, nov ukaz, tuja pot. */
    static long cancelled;

    private boolean active;
    private int goalX;
    private int goalY;
    private int goalZ;
    private int resumes;
    private double bestEnd;

    public static boolean isValidMode(int m) {
        return m >= ORIGINAL && m < MODES;
    }

    public static void setMode(int m) {
        mode = isValidMode(m) ? m : ORIGINAL;
    }

    public static int mode() {
        return mode;
    }

    public static boolean on() {
        return mode == ON;
    }

    public static String describe(int m) {
        return m == ON ? "nadaljevanje delne poti in vmesni cilj (meja " + MAX_RESUMES + " obnov)" : "original";
    }

    public static void reset() {
        started = 0L;
        intermediates = 0L;
        resumed = 0L;
        arrived = 0L;
        noProgress = 0L;
        limit = 0L;
        cancelled = 0L;
    }

    public static String counters() {
        return "nadZacetih=" + started + " nadVmesnih=" + intermediates + " nadObnov=" + resumed
                + " nadPrispelih=" + arrived + " nadBrezNapredka=" + noProgress + " nadMeja=" + limit
                + " nadPrekinjenih=" + cancelled;
    }

    /** Manhattanska razdalja blokov. */
    static int manhattan(int ax, int ay, int az, int bx, int by, int bz) {
        return Math.abs(ax - bx) + Math.abs(ay - by) + Math.abs(az - bz);
    }

    static double euclid(int ax, int ay, int az, int bx, int by, int bz) {
        double dx = ax - bx;
        double dy = ay - by;
        double dz = az - bz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Ali cilj ne more dobiti cele poti zaradi dometa: vsak korak A* stane vsaj svojo manhattansko
     * razliko, vozlisce pa je sprejeto samo pri {@code distanceFromOrigin < range} [K].
     */
    public static boolean beyondRange(int ex, int ey, int ez, int gx, int gy, int gz, float range) {
        return manhattan(ex, ey, ez, gx, gy, gz) >= range;
    }

    /** Manhattanski proracun do vmesnega cilja: domet - {@link #MARGIN}, pri majhnem dometu polovica. */
    static int budget(float range) {
        int r = (int) range;
        int b = r - MARGIN;
        if (b < r / 2) {
            b = r / 2;
        }
        return b < 1 ? 1 : b;
    }

    /**
     * Vmesni cilj na crti od NPC-ja proti cilju, manhattansko najvec {@link #budget(float)} od NPC-ja.
     * Komponente se zaokrozijo proti NPC-ju, da proracun ni nikoli presezen.
     */
    public static int[] intermediate(int ex, int ey, int ez, int gx, int gy, int gz, float range) {
        int total = manhattan(ex, ey, ez, gx, gy, gz);
        int b = budget(range);
        if (total <= b) {
            return new int[] {gx, gy, gz};
        }
        // Celostevilsko: deljenje v Javi zaokrozi proti nic, torej proti NPC-ju.
        return new int[] {ex + b * (gx - ex) / total, ey + b * (gy - ey) / total, ez + b * (gz - ez) / total};
    }

    public boolean active() {
        return this.active;
    }

    public int goalX() {
        return this.goalX;
    }

    public int goalY() {
        return this.goalY;
    }

    public int goalZ() {
        return this.goalZ;
    }

    public int resumes() {
        return this.resumes;
    }

    /**
     * Nov ukaz (zunanji {@code getPathToPos}). Vrne blok, do katerega naj navigator isce: cilj ali
     * vmesni cilj. Prejsnje nadaljevanje se s tem zamenja.
     */
    public int[] command(int ex, int ey, int ez, int gx, int gy, int gz, float range) {
        this.active = false;
        this.goalX = gx;
        this.goalY = gy;
        this.goalZ = gz;
        this.resumes = 0;
        this.bestEnd = Double.MAX_VALUE;
        return this.searchTarget(ex, ey, ez, range);
    }

    private int[] searchTarget(int ex, int ey, int ez, float range) {
        if (beyondRange(ex, ey, ez, this.goalX, this.goalY, this.goalZ, range)) {
            ++intermediates;
            return intermediate(ex, ey, ez, this.goalX, this.goalY, this.goalZ, range);
        }
        return new int[] {this.goalX, this.goalY, this.goalZ};
    }

    /**
     * Izid iskanja za ukaz. {@code hasPath} false: ni poti, nadaljevanja ni (kot original). Sicer
     * se nadaljevanje vklopi, ce pot ne konca pri cilju.
     */
    public void commandResult(boolean hasPath, int endX, int endY, int endZ) {
        if (!hasPath) {
            this.active = false;
            return;
        }
        this.bestEnd = euclid(endX, endY, endZ, this.goalX, this.goalY, this.goalZ);
        this.active = this.bestEnd > ARRIVED;
        if (this.active) {
            ++started;
        }
    }

    /**
     * Pot je prehojena. Vrne blok za naslednje iskanje ali null, ce je nadaljevanja konec (pot je
     * koncala pri cilju ali je meja obnov dosezena).
     */
    public int[] next(int ex, int ey, int ez, float range) {
        if (!this.active) {
            return null;
        }
        if (this.bestEnd <= ARRIVED) {
            this.active = false;
            ++arrived;
            return null;
        }
        if (this.resumes >= MAX_RESUMES) {
            this.active = false;
            ++limit;
            return null;
        }
        ++this.resumes;
        ++resumed;
        return this.searchTarget(ex, ey, ez, range);
    }

    /**
     * Izid obnove. Vrne true, ce naj navigator novo pot nastavi: obstaja in konca vsaj
     * {@link #MIN_PROGRESS} blize cilju kot vse dosedanje.
     */
    public boolean resumeResult(boolean hasPath, int endX, int endY, int endZ) {
        if (!this.active) {
            return false;
        }
        if (!hasPath) {
            this.active = false;
            ++noProgress;
            return false;
        }
        double end = euclid(endX, endY, endZ, this.goalX, this.goalY, this.goalZ);
        if (end > this.bestEnd - MIN_PROGRESS) {
            this.active = false;
            ++noProgress;
            return false;
        }
        this.bestEnd = end;
        return true;
    }

    /** Prekinitev od zunaj (obticanje, nov ukaz do entitete, tuja pot, brisanje poti). */
    public void cancel() {
        if (this.active) {
            this.active = false;
            ++cancelled;
        }
    }
}
