package noppes.npcs.rework.formation;

import java.util.List;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * Enota v svetu: povezava med {@link SquadPlanner} in NPC-ji.
 *
 * <p>Tick enote tece na zacetku ticka sveta ({@link SquadManager}), preden se posodobijo
 * entitete: prebere polozaje, poklice planer in po potrebi poisce nadaljevanje poti. Ukaze
 * nato izvedejo taski clanov v svojem ticku ({@link #drive(int)}).
 *
 * <h2>Kako clan hodi</h2>
 * <ul>
 *   <li>{@code STEER}: navigator dobi pot z <b>eno samo tocko</b> nekoliko pred mestom
 *       clana. Iskanja poti ni; vanilla {@code PathNavigate} in {@code EntityMoveHelper}
 *       poskrbita za hojo, skok na stopnico in animacijo, CustomNPCs pa vidi
 *       {@code Walking = 1}, kot pri vsaki drugi poti.</li>
 *   <li>{@code PATH}: clan je dalec ali obtical, zato dobi pravo iskanje poti
 *       ({@code tryMoveToXYZ}), najvec {@link #PATH_BUDGET} na enoto na tick in najvec
 *       enkrat na {@link #PATH_INTERVAL} tickov na clana.</li>
 * </ul>
 */
public final class Squad {
    /** Najvec pravih iskanj poti na enoto na tick. */
    public static final int PATH_BUDGET = 4;
    public static final int PATH_INTERVAL = 20;
    public static final int REPLAN_INTERVAL = 20;
    /** Clan, ki se toliko tickov ni posodobil, je izven nalozenih chunkov in izpade. */
    public static final int STALE_LIMIT = 40;
    /** Tocka, proti kateri clan hodi med pohodom, je toliko pred njegovim mestom. */
    public static final double LEAD = 2.0;
    /**
     * Terminalna hitrost na tleh je priblizno {@code 2,2 * (atribut * speedIn)^2} blokov na
     * tick: pospesek je kvadraten v hitrosti ({@code EntityMoveHelper} nastavi
     * {@code moveForward} in {@code AIMoveSpeed} na isto vrednost), trenje tal 0,546.
     */
    public static final double GROUND_SPEED_COEFF = 2.2;
    /** Razdalja, do katere se zadnja tocka poti steje za cilj (ista meja kot v D-015). */
    public static final double GOAL_REACHED = 2.0;

    private final int id;
    private final World world;
    private final String label;
    private final SquadOrder order;
    private final EntityNPCInterface[] members;
    private final FormationMoveTask[] tasks;
    private final SquadPlanner planner;
    private final double[] attr;

    private final double[] px;
    private final double[] py;
    private final double[] pz;
    private final boolean[] eligible;
    private final int[] lastTicksExisted;
    private final int[] staleTicks;
    private final int[] lastPathTick;
    private final double[] lastPathX;
    private final double[] lastPathZ;
    private final double[] lastFace;

    private int ticks;
    private int pathBudget;
    private int replanCooldown;
    private int pathRequests;
    private int alive;
    private boolean finished;
    private final boolean pathless;

    private Squad(int id, World world, String label, SquadOrder order, EntityNPCInterface[] members,
            SquadPlanner planner, double[] attr, boolean pathless) {
        this.id = id;
        this.world = world;
        this.label = label;
        this.order = order;
        this.members = members;
        this.planner = planner;
        this.attr = attr;
        this.pathless = pathless;
        int n = members.length;
        this.tasks = new FormationMoveTask[n];
        this.px = new double[n];
        this.py = new double[n];
        this.pz = new double[n];
        this.eligible = new boolean[n];
        this.lastTicksExisted = new int[n];
        this.staleTicks = new int[n];
        this.lastPathTick = new int[n];
        this.lastPathX = new double[n];
        this.lastPathZ = new double[n];
        this.lastFace = new double[n];
        java.util.Arrays.fill(lastPathTick, -PATH_INTERVAL);
        java.util.Arrays.fill(lastFace, Double.NaN);
        for (int i = 0; i < n; i++) {
            tasks[i] = new FormationMoveTask(this, i);
            lastTicksExisted[i] = members[i].ticksExisted;
        }
    }

    /**
     * Sestavi enoto: izbere vodjo, poisce eno pot, dodeli mesta in namesti taske.
     *
     * @return enota ali {@code null}, ce ni nobenega uporabnega clana
     */
    static Squad create(int id, World world, String label, List<EntityNPCInterface> candidates, SquadOrder order) {
        java.util.List<EntityNPCInterface> list = new java.util.ArrayList<EntityNPCInterface>();
        for (EntityNPCInterface npc : candidates) {
            if (npc != null && npc.world == world && !npc.isDead && !npc.isKilled() && !npc.isRiding()) {
                list.add(npc);
            }
        }
        int n = list.size();
        if (n == 0) {
            return null;
        }
        EntityNPCInterface[] members = list.toArray(new EntityNPCInterface[n]);
        double[] xs = new double[n];
        double[] zs = new double[n];
        double cx = 0;
        double cz = 0;
        for (int i = 0; i < n; i++) {
            xs[i] = members[i].posX;
            zs[i] = members[i].posZ;
            cx += xs[i];
            cz += zs[i];
        }
        cx /= n;
        cz /= n;
        int leader = 0;
        double best = Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            double d = (xs[i] - cx) * (xs[i] - cx) + (zs[i] - cz) * (zs[i] - cz);
            if (d < best) {
                best = d;
                leader = i;
            }
        }

        EntityNPCInterface lead = members[leader];
        Path path = lead.getNavigator().getPathToXYZ(order.x, order.y, order.z);
        PathTrack track = toTrack(path);
        boolean pathless = track == null;
        boolean reaches;
        if (pathless) {
            track = PathTrack.straight(lead.posX, lead.posY, lead.posZ, order.x, order.y, order.z);
            reaches = true;
        } else {
            reaches = reachesGoal(track, order);
        }

        double finalYaw = order.yaw;
        if (Double.isNaN(finalYaw)) {
            finalYaw = track.yawAt(track.length(), 6.0, 0.0);
            if (Double.isNaN(finalYaw)) {
                finalYaw = lead.rotationYaw;
            }
        }
        boolean willForm = order.formFirst && track.length() >= SquadPlanner.FORM_SKIP;
        double assignYaw = willForm ? track.yawAt(0, 0, SquadPlanner.LOOK_AHEAD) : finalYaw;
        if (Double.isNaN(assignYaw)) {
            assignYaw = finalYaw;
        }

        Slot[] slots;
        if (order.shape == SquadOrder.Shape.MARCH) {
            slots = FormationShape.keep(xs, zs, assignYaw);
        } else {
            Slot[] shape;
            switch (order.shape) {
                case OBRAMBA:
                    shape = FormationShape.ring(n, order.param);
                    break;
                case KOLONA:
                    shape = FormationShape.column(n, (int) Math.round(order.param));
                    break;
                default:
                    shape = FormationShape.legion(n, (int) Math.round(order.param));
                    break;
            }
            int[] assignment = SlotAssigner.assign(xs, zs, shape, assignYaw);
            slots = new Slot[n];
            for (int i = 0; i < n; i++) {
                slots[i] = shape[assignment[i]];
            }
        }

        double[] attr = new double[n];
        double[] vmax = new double[n];
        double speedIn = order.speed * SquadOrder.SCRIPT_SPEED_FACTOR;
        for (int i = 0; i < n; i++) {
            attr[i] = Math.max(0.01, members[i].getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
            double a = Math.min(1.0, attr[i] * speedIn);
            vmax[i] = Math.min(1.0, GROUND_SPEED_COEFF * a * a);
        }
        SquadPlanner planner = new SquadPlanner(slots, vmax, track, reaches, order.x, order.y, order.z, finalYaw,
                new WorldTerrain(world), order.formFirst, order.anchor);
        // Taski se namestijo sele v ticku enote: ukaz lahko pride iz skripte, ki tece med
        // tickom NPC-ja, in takrat se mnozice taskov ne sme spreminjati.
        return new Squad(id, world, label, order, members, planner, attr, pathless);
    }

    private static PathTrack toTrack(Path path) {
        if (path == null || path.getCurrentPathLength() == 0) {
            return null;
        }
        int len = path.getCurrentPathLength();
        double[] xs = new double[len];
        double[] ys = new double[len];
        double[] zs = new double[len];
        for (int i = 0; i < len; i++) {
            PathPoint p = path.getPathPointFromIndex(i);
            xs[i] = p.x + 0.5;
            ys[i] = p.y;
            zs[i] = p.z + 0.5;
        }
        return new PathTrack(xs, ys, zs);
    }

    private static boolean reachesGoal(PathTrack track, SquadOrder order) {
        double dx = track.endX() - order.x;
        double dz = track.endZ() - order.z;
        double dy = track.endY() - order.y;
        // cilj je lahko blok tal (skripta je ciljala blok, v katerega gleda igralec)
        double dyFeet = Math.min(Math.abs(dy), Math.abs(dy - 1.0));
        return Math.sqrt(dx * dx + dz * dz + dyFeet * dyFeet) <= GOAL_REACHED;
    }

    /** Tick enote; klice ga {@link SquadManager} na zacetku ticka sveta. */
    void tick() {
        if (finished) {
            return;
        }
        ticks++;
        pathBudget = PATH_BUDGET;
        alive = 0;
        for (int i = 0; i < members.length; i++) {
            EntityNPCInterface npc = members[i];
            if (npc == null) {
                eligible[i] = false;
                continue;
            }
            boolean ok = npc.world == world && !npc.isDead && !npc.isKilled();
            if (ok) {
                staleTicks[i] = npc.ticksExisted == lastTicksExisted[i] ? staleTicks[i] + 1 : 0;
                lastTicksExisted[i] = npc.ticksExisted;
                ok = staleTicks[i] <= STALE_LIMIT;
            }
            if (!ok) {
                drop(i);
                eligible[i] = false;
                continue;
            }
            alive++;
            ensureTask(i);
            boolean busy = (order.engage && npc.isAttacking()) || npc.isRiding();
            eligible[i] = !busy;
            px[i] = npc.posX;
            py[i] = npc.posY;
            pz[i] = npc.posZ;
        }
        if (alive == 0) {
            finished = true;
            return;
        }
        planner.update(px, py, pz, eligible);
        for (int i = 0; i < members.length; i++) {
            if (!Double.isNaN(planner.faceYaw[i])) {
                lastFace[i] = planner.faceYaw[i];
            }
        }
        if (replanCooldown > 0) {
            replanCooldown--;
        }
        if (planner.needsReplan() && replanCooldown == 0) {
            replan();
            replanCooldown = REPLAN_INTERVAL;
        }
        if (planner.phase() == SquadPlanner.Phase.DONE) {
            finish();
        }
    }

    /**
     * Nadaljevanje delne poti. Isce clan, ki je sidru najblizji, ker iskanje poti vedno
     * zacne pri entiteti; zacetek poti za sidrom se odreze ({@link PathTrack#fromAnchor}).
     */
    private void replan() {
        double ax = planner.anchorX();
        double ay = planner.anchorY();
        double az = planner.anchorZ();
        int best = -1;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < members.length; i++) {
            if (members[i] == null || !eligible[i]) {
                continue;
            }
            double d = (px[i] - ax) * (px[i] - ax) + (pz[i] - az) * (pz[i] - az);
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        if (best < 0) {
            return;
        }
        Path path = members[best].getNavigator().getPathToXYZ(order.x, order.y, order.z);
        pathRequests++;
        PathTrack raw = toTrack(path);
        if (raw == null) {
            planner.replaceTrack(null, false);
            return;
        }
        double[] xs = new double[raw.size()];
        double[] ys = new double[raw.size()];
        double[] zs = new double[raw.size()];
        for (int i = 0; i < raw.size(); i++) {
            PathPoint p = path.getPathPointFromIndex(i);
            xs[i] = p.x + 0.5;
            ys[i] = p.y;
            zs[i] = p.z + 0.5;
        }
        PathTrack next = PathTrack.fromAnchor(ax, ay, az, xs, ys, zs);
        planner.replaceTrack(next, reachesGoal(next, order));
    }

    /** Ali task clana {@code i} ta tick vodi NPC-ja. */
    boolean controls(int i) {
        return !finished && members[i] != null && eligible[i];
    }

    /** Izvedba ukaza planerja za clana {@code i}; klice ga task v ticku NPC-ja. */
    void drive(int i) {
        EntityNPCInterface npc = members[i];
        if (npc == null) {
            return;
        }
        PathNavigate nav = npc.getNavigator();
        int mode = planner.mode[i];
        if (mode == SquadPlanner.HOLD) {
            if (!nav.noPath()) {
                nav.clearPath();
            }
            double face = planner.faceYaw[i];
            if (!Double.isNaN(face)) {
                face(npc, (float) face, false);
            }
            return;
        }
        double speedIn = speedIn(i, planner.speed[i]);
        double tx = planner.targetX[i];
        double ty = planner.targetY[i];
        double tz = planner.targetZ[i];
        if (mode == SquadPlanner.STEER) {
            if (planner.phase() == SquadPlanner.Phase.MARCHING) {
                tx += FormationMath.forwardX(planner.yaw()) * LEAD;
                tz += FormationMath.forwardZ(planner.yaw()) * LEAD;
            }
            PathPoint point = new PathPoint((int) Math.floor(tx), (int) Math.floor(ty), (int) Math.floor(tz));
            Path current = nav.getPath();
            if (current != null && current.isFinished()) {
                // Pot z isto tocko bi setPath obdrzal kot ze koncano; zato jo najprej pocisti.
                nav.clearPath();
            }
            nav.setPath(new Path(new PathPoint[] {point}), speedIn);
            nav.setSpeed(speedIn);
            return;
        }
        boolean due = ticks - lastPathTick[i] >= PATH_INTERVAL;
        double moved = Math.hypot(tx - lastPathX[i], tz - lastPathZ[i]);
        if ((due || moved > 3.0 || nav.noPath()) && pathBudget > 0 && ticks - lastPathTick[i] >= 5) {
            pathBudget--;
            pathRequests++;
            lastPathTick[i] = ticks;
            lastPathX[i] = tx;
            lastPathZ[i] = tz;
            nav.tryMoveToXYZ(tx, ty, tz, speedIn);
        } else {
            nav.setSpeed(speedIn);
        }
    }

    private double speedIn(int i, double blocksPerTick) {
        double max = order.speed * SquadOrder.SCRIPT_SPEED_FACTOR;
        double s = Math.sqrt(Math.max(0, blocksPerTick) / GROUND_SPEED_COEFF) / attr[i];
        return Math.max(0.05, Math.min(max, s));
    }

    private static void face(EntityNPCInterface npc, float yaw, boolean persist) {
        npc.rotationYaw = yaw;
        npc.rotationYawHead = yaw;
        npc.renderYawOffset = yaw;
        if (persist) {
            int orientation = (int) Math.round(((yaw % 360.0) + 360.0) % 360.0);
            if (npc.ais.orientation != orientation) {
                npc.ais.orientation = orientation;
                npc.updateClient = true;
            }
        }
    }

    /** Namesti task clana, ce ga je CustomNPCs ob {@code updateTasks} pobrisal. */
    void ensureTask(int i) {
        EntityNPCInterface npc = members[i];
        if (npc == null) {
            return;
        }
        EntityAITasks t = npc.tasks;
        for (EntityAITasks.EntityAITaskEntry e : t.taskEntries) {
            if (e.action == tasks[i]) {
                return;
            }
        }
        t.addTask(FormationMoveTask.PRIORITY, tasks[i]);
    }

    /**
     * Odstranitev taska je odlozena na zacetek naslednjega ticka sveta
     * ({@link SquadManager#scheduleRemoval}); do takrat je task neaktiven, ker
     * {@link #controls(int)} vrne false.
     */
    private void removeTask(int i) {
        EntityNPCInterface npc = members[i];
        if (npc != null) {
            SquadManager.scheduleRemoval(npc, tasks[i]);
        }
    }

    private void drop(int i) {
        removeTask(i);
        members[i] = null;
    }

    /** Izpusti enega clana (npr. ker je dobil nov ukaz v drugi enoti). */
    boolean release(EntityNPCInterface npc) {
        for (int i = 0; i < members.length; i++) {
            if (members[i] == npc) {
                removeTask(i);
                npc.getNavigator().clearPath();
                members[i] = null;
                eligible[i] = false;
                return true;
            }
        }
        return false;
    }

    /** Konec: sidranje na koncna mesta, smer pogleda, odstranitev taskov. */
    private void finish() {
        for (int i = 0; i < members.length; i++) {
            EntityNPCInterface npc = members[i];
            if (npc == null) {
                continue;
            }
            removeTask(i);
            npc.getNavigator().clearPath();
            if (!eligible[i]) {
                continue;
            }
            double yaw = Double.isNaN(lastFace[i]) ? planner.finalYaw() : lastFace[i];
            if (order.anchor) {
                npc.ais.setStartPos(new BlockPos(planner.targetX[i], planner.targetY[i], planner.targetZ[i]));
            }
            face(npc, (float) yaw, order.anchor);
        }
        finished = true;
    }

    /** Takojsnja ustavitev brez sidranja. */
    void cancel() {
        for (int i = 0; i < members.length; i++) {
            if (members[i] != null) {
                removeTask(i);
                members[i].getNavigator().clearPath();
                members[i] = null;
            }
        }
        finished = true;
    }

    boolean isFinished() {
        return finished;
    }

    boolean contains(EntityNPCInterface npc) {
        for (EntityNPCInterface m : members) {
            if (m == npc) {
                return true;
            }
        }
        return false;
    }

    World world() {
        return world;
    }

    int id() {
        return id;
    }

    /** Ena vrstica stanja, z markerjem za razclenjevanje iz loga. */
    public String status() {
        int stragglers = 0;
        int excused = 0;
        int trailing = 0;
        int settled = 0;
        for (int i = 0; i < members.length; i++) {
            if (members[i] == null) {
                continue;
            }
            if (planner.straggler[i]) {
                stragglers++;
            }
            if (planner.excused[i]) {
                excused++;
            }
            if (planner.trailing[i]) {
                trailing++;
            }
            if (planner.settled[i]) {
                settled++;
            }
        }
        return String.format(java.util.Locale.ROOT,
                "RWSQUAD enota=%d ime=%s oblika=%s faza=%s clanov=%d/%d ostane=%.1f hitrost=%.3f/%.3f"
                        + " zaostali=%d opraviceni=%d kolona=%d na_mestu=%d iskanj=%d obnov=%d obtical=%d"
                        + " brez_poti=%s cilj_nedosegljiv=%s neporavnani=%d tick=%d",
                id, label, order.shape, planner.phase(), alive, members.length, planner.remaining(),
                planner.anchorSpeed(), planner.cruise(), stragglers, excused, trailing, settled, pathRequests,
                planner.replans(), planner.stuckEvents(), pathless, planner.goalUnreachable(),
                planner.unsettledAtEnd(), ticks);
    }
}
