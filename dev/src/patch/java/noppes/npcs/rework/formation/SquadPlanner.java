package noppes.npcs.rework.formation;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * Mozgani enote: iz polozajev clanov vsak tick izracuna, kam naj gre vsak clan in kako
 * hitro. Brez odvisnosti od Minecrafta, zato je testiran z navadnim JUnitom.
 *
 * <h2>Model</h2>
 * <ul>
 *   <li>Formacija ima <b>sidro</b>, ki potuje po eni skupni poti ({@link PathTrack}). Pot
 *       izracuna en clan (vodja); ostali poti ne iscejo, ampak sledijo svojemu mestu.</li>
 *   <li>Mesto clana v svetu je sidro plus odmik {@link Slot}, zasukan za trenutni yaw
 *       formacije. Yaw sledi smeri poti z omejeno hitrostjo zasuka.</li>
 *   <li><b>Zaprta zanka:</b> sidro se premika samo tako hitro, kot mu clani sledijo. Ce
 *       najslabsi clan zaostaja vec kot {@link #LAG_SLOW}, sidro upocasni, pri
 *       {@link #LAG_STOP} pocaka. Clan, ki zaostaja, dobi vecjo hitrost, clan pred
 *       mestom manjso. Tega timerji v skripti ne morejo, ker ne vidijo, kje so clani.</li>
 *   <li><b>Ozko grlo:</b> ce na mestu clana ni mogoce stati (zid, luknja), gre clan na
 *       tocko na poti za sidrom, po vrstnem redu vrst. Formacija se tako pred vrati sama
 *       stisne v kolono in se za njimi spet razpre.</li>
 *   <li><b>Faze:</b> FORMING (enota se postroji na zacetku poti), MARCHING (sidro potuje),
 *       SETTLING (clani gredo na koncna, poravnana mesta), DONE.</li>
 * </ul>
 *
 * <p>Hitrosti so v blokih na tick.
 */
public final class SquadPlanner {

    public enum Phase {
        FORMING, MARCHING, SETTLING, DONE
    }

    /** Clan stoji. */
    public static final int HOLD = 0;
    /** Clan gre naravnost proti cilju (en korak poti, brez iskanja). */
    public static final int STEER = 1;
    /** Clan je predalec ali obtical: potrebuje pravo iskanje poti. */
    public static final int PATH = 2;

    /** Blizje od tega je clan na mestu. */
    public static final double SETTLED = 0.4;
    /** Clan na mestu, ki ga odrinejo dlje od tega, se spet premakne (histereza). */
    public static final double UNSETTLE = 1.2;
    /** FORMING se konca, ko so vsi clani blizje od tega. */
    public static final double FORMED = 1.5;
    public static final double LAG_SLOW = 2.0;
    public static final double LAG_STOP = 5.0;
    /** Clan dlje od tega je zaostanek: ne ustavlja enote, sam jo lovi z iskanjem poti. */
    public static final double STRAGGLER = 14.0;
    /** Dlje od tega clan ne gre vec naravnost, ampak z iskanjem poti. */
    public static final double STEER_MAX = 6.0;
    /** Najvecji zasuk formacije v stopinjah na tick. */
    public static final double TURN_RATE = 4.0;
    public static final double TRAIL_SPACING = 1.4;
    /** Pri krajsi poti se enota ne postroji na zacetku, ampak gre vsak naravnost na cilj. */
    public static final double FORM_SKIP = 12.0;
    /** Sidro gre s tem delezem hitrosti najpocasnejsega clana, da imajo clani rezervo. */
    public static final double CRUISE_FRACTION = 0.8;
    public static final double GAIN = 0.12;
    public static final int FORM_TIMEOUT = 200;
    public static final int SETTLE_TIMEOUT = 400;
    public static final int STUCK_WINDOW = 30;
    public static final double STUCK_MOVE = 0.5;
    public static final int STUCK_PENALTY = 60;
    public static final double LOOK_BEHIND = 2.0;
    public static final double LOOK_AHEAD = 4.0;
    public static final int MAX_REPLANS = 30;
    public static final double MIN_SPEED = 0.02;
    /**
     * Toliko tickov zapored sme en clan zaustavljati enoto (napaka nad {@link #HOLDING}),
     * potem je opravicen: enota gre naprej, on jo lovi z iskanjem poti. Brez tega bi en
     * obtican NPC za vedno ustavil vse.
     */
    public static final int WAIT_LIMIT = 80;
    /**
     * Nad tem zaostankom clan steje, da zadrzuje enoto. Namenoma pod {@link #LAG_STOP}: sidro
     * se zaostanku priblizuje asimptotsko in {@code LAG_STOP} nikoli res ne doseze.
     */
    public static final double HOLDING = LAG_STOP - 1.0;

    private final int n;
    private final Slot[] slots;
    private final double[] vmax;
    private final Terrain terrain;
    private final double goalX;
    private final double goalY;
    private final double goalZ;
    private final double finalYaw;
    private final boolean snap;
    private final double cruise;
    private final int[] trailRank;

    private PathTrack track;
    private boolean reachesGoal;
    private double anchorS;
    private double yaw;
    private Phase phase;
    private int tick;
    private int phaseTicks;
    private double anchorSpeed;
    private boolean needsReplan;
    private int replans;
    private int emptyReplans;
    private boolean goalUnreachable;
    private boolean targetsReady;
    private int stuckEvents;
    private int unsettledAtEnd;

    private final double[] finalX;
    private final double[] finalY;
    private final double[] finalZ;
    private final int[] trailUntil;
    private final int[] pathUntil;
    private final double[] checkX;
    private final double[] checkZ;
    private final int[] checkTick;
    private final int[] lagTicks;
    public final boolean[] excused;

    public final double[] targetX;
    public final double[] targetY;
    public final double[] targetZ;
    /** Zelena hitrost v blokih na tick. */
    public final double[] speed;
    /** Smer pogleda za clana, ki stoji; NaN, ce se premika. */
    public final double[] faceYaw;
    public final int[] mode;
    public final boolean[] settled;
    public final boolean[] straggler;
    public final boolean[] trailing;
    public final double[] error;

    /**
     * @param slots       mesto za vsakega clana, ze dodeljeno ({@code slots[i]} je clan i)
     * @param vmax        najvecja hitrost vsakega clana v blokih na tick
     * @param track       skupna pot od zacetka do cilja ali do konca delne poti
     * @param reachesGoal ali se {@code track} konca na cilju
     * @param finalYaw    smer formacije na cilju
     * @param formFirst   ali naj se enota pri daljsi poti najprej postroji
     * @param snap        ali naj bodo koncna mesta na sredini blokov (potrebno za sidranje)
     */
    public SquadPlanner(Slot[] slots, double[] vmax, PathTrack track, boolean reachesGoal,
            double goalX, double goalY, double goalZ, double finalYaw, Terrain terrain,
            boolean formFirst, boolean snap) {
        if (slots.length != vmax.length) {
            throw new IllegalArgumentException("slots in vmax morata imeti enako dolzino");
        }
        this.n = slots.length;
        this.slots = slots.clone();
        this.vmax = vmax.clone();
        this.track = track;
        this.reachesGoal = reachesGoal;
        this.goalX = goalX;
        this.goalY = goalY;
        this.goalZ = goalZ;
        this.finalYaw = FormationMath.wrap(finalYaw);
        this.terrain = terrain;
        this.snap = snap;
        double slowest = Double.MAX_VALUE;
        for (double v : vmax) {
            slowest = Math.min(slowest, v);
        }
        this.cruise = n == 0 ? 0 : Math.max(MIN_SPEED, slowest * CRUISE_FRACTION);
        this.trailRank = computeTrailRank(this.slots);

        finalX = new double[n];
        finalY = new double[n];
        finalZ = new double[n];
        trailUntil = new int[n];
        pathUntil = new int[n];
        checkX = new double[n];
        checkZ = new double[n];
        checkTick = new int[n];
        lagTicks = new int[n];
        excused = new boolean[n];
        Arrays.fill(checkTick, Integer.MIN_VALUE);
        targetX = new double[n];
        targetY = new double[n];
        targetZ = new double[n];
        speed = new double[n];
        faceYaw = new double[n];
        mode = new int[n];
        settled = new boolean[n];
        straggler = new boolean[n];
        trailing = new boolean[n];
        error = new double[n];
        Arrays.fill(faceYaw, Double.NaN);
        Arrays.fill(error, Double.NaN);

        if (!formFirst || track.length() < FORM_SKIP) {
            enterSettling();
        } else {
            phase = Phase.FORMING;
            anchorS = 0;
            yaw = startYaw();
        }
    }

    private double startYaw() {
        double y = track.yawAt(0, 0, LOOK_AHEAD);
        return Double.isNaN(y) ? finalYaw : y;
    }

    private static int[] computeTrailRank(final Slot[] slots) {
        Integer[] order = new Integer[slots.length];
        for (int i = 0; i < slots.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                int c = Double.compare(slots[a].back, slots[b].back);
                return c != 0 ? c : Double.compare(Math.abs(slots[a].side), Math.abs(slots[b].side));
            }
        });
        int[] rank = new int[slots.length];
        for (int r = 0; r < order.length; r++) {
            rank[order[r]] = r;
        }
        return rank;
    }

    /**
     * En tick. {@code eligible[i]} je false za clana, ki je mrtev, neznan ali v boju; tak
     * clan se ne uposteva in dobi {@link #HOLD}.
     */
    public void update(double[] px, double[] py, double[] pz, boolean[] eligible) {
        tick++;
        phaseTicks++;
        if (phase == Phase.DONE) {
            Arrays.fill(mode, HOLD);
            return;
        }
        if (!targetsReady) {
            computeTargets(eligible);
        }

        int count = 0;
        int stragglers = 0;
        double maxErr = 0;
        boolean allFormed = true;
        boolean allSettled = true;
        for (int i = 0; i < n; i++) {
            if (!eligible[i]) {
                error[i] = Double.NaN;
                continue;
            }
            double e = horizontal(targetX[i] - px[i], targetZ[i] - pz[i]);
            error[i] = e;
            count++;
            detectStuck(i, px[i], pz[i], e);
            if (e > STRAGGLER) {
                straggler[i] = true;
                stragglers++;
                settled[i] = false;
                continue;
            }
            straggler[i] = false;
            if (phase == Phase.MARCHING) {
                lagTicks[i] = e > HOLDING ? lagTicks[i] + 1 : 0;
                if (lagTicks[i] > WAIT_LIMIT && !excused[i]) {
                    excused[i] = true;
                    pathUntil[i] = tick + STUCK_PENALTY;
                    trailUntil[i] = tick + STUCK_PENALTY;
                }
            }
            if (excused[i] && e < LAG_SLOW) {
                excused[i] = false;
                lagTicks[i] = 0;
            }
            if (!excused[i]) {
                maxErr = Math.max(maxErr, e);
            }
            if (e > FORMED) {
                allFormed = false;
            }
            if (settled[i]) {
                if (e > UNSETTLE) {
                    settled[i] = false;
                }
            } else if (e < SETTLED) {
                settled[i] = true;
            }
            if (!settled[i]) {
                allSettled = false;
            }
        }
        boolean nobodyNear = count == 0 || stragglers == count;

        double v = 0;
        switch (phase) {
            case FORMING:
                if (!nobodyNear && (allFormed || phaseTicks > FORM_TIMEOUT)) {
                    setPhase(Phase.MARCHING);
                }
                break;
            case MARCHING:
                double ratio;
                if (maxErr <= LAG_SLOW) {
                    ratio = 1.0;
                } else if (maxErr >= LAG_STOP) {
                    ratio = 0.0;
                } else {
                    ratio = (LAG_STOP - maxErr) / (LAG_STOP - LAG_SLOW);
                }
                if (count == 0 || stragglers * 2 > count) {
                    ratio = 0.0;
                }
                v = cruise * ratio;
                anchorS = Math.min(anchorS + v, track.length());
                if (anchorS >= track.length() - 1.0E-9) {
                    if (reachesGoal || replans >= MAX_REPLANS) {
                        goalUnreachable = !reachesGoal;
                        enterSettling();
                        v = 0;
                    } else {
                        needsReplan = true;
                    }
                }
                break;
            case SETTLING:
                if ((!nobodyNear && allSettled && stragglers == 0) || phaseTicks > SETTLE_TIMEOUT) {
                    unsettledAtEnd = 0;
                    for (int i = 0; i < n; i++) {
                        if (eligible[i] && !settled[i]) {
                            unsettledAtEnd++;
                        }
                    }
                    setPhase(Phase.DONE);
                }
                break;
            default:
                break;
        }
        anchorSpeed = v;

        double desired;
        if (phase == Phase.FORMING) {
            desired = startYaw();
        } else if (phase == Phase.MARCHING) {
            desired = track.yawAt(anchorS, LOOK_BEHIND, LOOK_AHEAD);
        } else {
            desired = finalYaw;
        }
        yaw = phase == Phase.MARCHING || phase == Phase.FORMING
                ? FormationMath.turnToward(yaw, desired, TURN_RATE)
                : finalYaw;

        computeTargets(eligible);
        computeCommands(px, pz, eligible);
    }

    private void detectStuck(int i, double x, double z, double e) {
        if (checkTick[i] == Integer.MIN_VALUE) {
            checkX[i] = x;
            checkZ[i] = z;
            checkTick[i] = tick;
            return;
        }
        if (tick - checkTick[i] < STUCK_WINDOW) {
            return;
        }
        double moved = horizontal(x - checkX[i], z - checkZ[i]);
        if (mode[i] == STEER && e > 1.0 && moved < STUCK_MOVE) {
            pathUntil[i] = tick + STUCK_PENALTY;
            trailUntil[i] = tick + STUCK_PENALTY;
            stuckEvents++;
        }
        checkX[i] = x;
        checkZ[i] = z;
        checkTick[i] = tick;
    }

    private void computeTargets(boolean[] eligible) {
        targetsReady = true;
        if (phase == Phase.SETTLING || phase == Phase.DONE) {
            for (int i = 0; i < n; i++) {
                targetX[i] = finalX[i];
                targetY[i] = finalY[i];
                targetZ[i] = finalZ[i];
                trailing[i] = false;
            }
            return;
        }
        double ax = track.x(anchorS);
        double ay = track.y(anchorS);
        double az = track.z(anchorS);
        for (int i = 0; i < n; i++) {
            if (!eligible[i]) {
                continue;
            }
            double sx = FormationMath.worldX(ax, yaw, slots[i]);
            double sz = FormationMath.worldZ(az, yaw, slots[i]);
            double sy = tick < trailUntil[i] ? Double.NaN : terrain.standY(sx, ay, sz);
            if (Double.isNaN(sy)) {
                double s = Math.max(0, anchorS - (trailRank[i] + 1) * TRAIL_SPACING);
                targetX[i] = track.x(s);
                targetY[i] = track.y(s);
                targetZ[i] = track.z(s);
                trailing[i] = true;
            } else {
                targetX[i] = sx;
                targetY[i] = sy;
                targetZ[i] = sz;
                trailing[i] = false;
            }
        }
    }

    private void computeCommands(double[] px, double[] pz, boolean[] eligible) {
        double fx = FormationMath.forwardX(yaw);
        double fz = FormationMath.forwardZ(yaw);
        for (int i = 0; i < n; i++) {
            faceYaw[i] = Double.NaN;
            if (!eligible[i] || phase == Phase.DONE) {
                mode[i] = HOLD;
                speed[i] = 0;
                continue;
            }
            double dx = targetX[i] - px[i];
            double dz = targetZ[i] - pz[i];
            double e = horizontal(dx, dz);
            boolean hold;
            if (phase == Phase.MARCHING) {
                hold = e < SETTLED && anchorSpeed == 0;
            } else {
                hold = settled[i] && e < UNSETTLE;
            }
            if (hold) {
                mode[i] = HOLD;
                speed[i] = 0;
                faceYaw[i] = slots[i].faceOutward ? outwardYaw(i) : yaw;
                continue;
            }
            mode[i] = e > STEER_MAX || tick < pathUntil[i] ? PATH : STEER;
            double v;
            if (phase == Phase.MARCHING) {
                double along = dx * fx + dz * fz;
                v = along < -0.5 ? anchorSpeed * 0.5 : anchorSpeed + GAIN * e;
            } else {
                v = 0.04 + 0.15 * e;
            }
            speed[i] = Math.max(MIN_SPEED, Math.min(vmax[i], v));
        }
    }

    private double outwardYaw(int i) {
        double cx;
        double cz;
        if (phase == Phase.SETTLING || phase == Phase.DONE) {
            cx = settleCenterX;
            cz = settleCenterZ;
        } else {
            cx = track.x(anchorS);
            cz = track.z(anchorS);
        }
        double y = FormationMath.yawOf(targetX[i] - cx, targetZ[i] - cz);
        return Double.isNaN(y) ? yaw : y;
    }

    private double settleCenterX;
    private double settleCenterZ;

    private void enterSettling() {
        setPhase(Phase.SETTLING);
        anchorS = track.length();
        yaw = finalYaw;
        double cx;
        double cy;
        double cz;
        if (reachesGoal) {
            cx = goalX;
            cy = goalY;
            cz = goalZ;
        } else {
            cx = track.endX();
            cy = track.endY();
            cz = track.endZ();
        }
        settleCenterX = cx;
        settleCenterZ = cz;
        Set<Long> taken = new HashSet<Long>();
        for (int i = 0; i < n; i++) {
            double x = FormationMath.worldX(cx, finalYaw, slots[i]);
            double z = FormationMath.worldZ(cz, finalYaw, slots[i]);
            if (snap) {
                x = Math.floor(x) + 0.5;
                z = Math.floor(z) + 0.5;
            }
            double y = terrain.standY(x, cy, z);
            if (Double.isNaN(y) || (snap && taken.contains(key(x, z)))) {
                double[] found = searchNear(x, cy, z, taken);
                if (found != null) {
                    x = found[0];
                    y = found[1];
                    z = found[2];
                } else {
                    double s = Math.max(0, track.length() - (trailRank[i] + 1) * TRAIL_SPACING);
                    x = track.x(s);
                    y = track.y(s);
                    z = track.z(s);
                    if (snap) {
                        x = Math.floor(x) + 0.5;
                        z = Math.floor(z) + 0.5;
                    }
                }
            }
            if (snap) {
                taken.add(key(x, z));
            }
            finalX[i] = x;
            finalY[i] = y;
            finalZ[i] = z;
        }
        targetsReady = false;
    }

    private double[] searchNear(double x, double y, double z, Set<Long> taken) {
        for (int r = 1; r <= 2; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    double cx = x + dx;
                    double cz = z + dz;
                    if (snap && taken.contains(key(cx, cz))) {
                        continue;
                    }
                    double cy = terrain.standY(cx, y, cz);
                    if (!Double.isNaN(cy)) {
                        return new double[] {cx, cy, cz};
                    }
                }
            }
        }
        return null;
    }

    private static long key(double x, double z) {
        long bx = (long) Math.floor(x);
        long bz = (long) Math.floor(z);
        return (bx << 32) ^ (bz & 0xffffffffL);
    }

    private void setPhase(Phase p) {
        phase = p;
        phaseTicks = 0;
    }

    private static double horizontal(double dx, double dz) {
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Ali adapter mora poiskati nadaljevanje delne poti. */
    public boolean needsReplan() {
        return needsReplan;
    }

    /** Nadaljevanje poti od sidra naprej (glej {@link PathTrack#fromAnchor}). */
    public void replaceTrack(PathTrack next, boolean nextReachesGoal) {
        needsReplan = false;
        replans++;
        if (next == null || next.length() < 0.5) {
            emptyReplans++;
            if (emptyReplans >= 3) {
                goalUnreachable = true;
                reachesGoal = false;
                enterSettling();
            }
            return;
        }
        emptyReplans = 0;
        track = next;
        reachesGoal = nextReachesGoal;
        anchorS = 0;
    }

    /** Nadaljevanja ni: enota se postroji na koncu tega, kar ima. */
    public void giveUp() {
        needsReplan = false;
        goalUnreachable = true;
        reachesGoal = false;
        enterSettling();
    }

    public Phase phase() {
        return phase;
    }

    public int size() {
        return n;
    }

    public double anchorX() {
        return track.x(anchorS);
    }

    public double anchorY() {
        return track.y(anchorS);
    }

    public double anchorZ() {
        return track.z(anchorS);
    }

    public double yaw() {
        return yaw;
    }

    public double finalYaw() {
        return finalYaw;
    }

    public double cruise() {
        return cruise;
    }

    public double anchorSpeed() {
        return anchorSpeed;
    }

    public double remaining() {
        return track.length() - anchorS;
    }

    public int replans() {
        return replans;
    }

    public int stuckEvents() {
        return stuckEvents;
    }

    public boolean goalUnreachable() {
        return goalUnreachable;
    }

    public int unsettledAtEnd() {
        return unsettledAtEnd;
    }

    public int tick() {
        return tick;
    }

    public Slot slot(int i) {
        return slots[i];
    }
}
