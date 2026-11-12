package noppes.npcs.rework.army;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.material.Material;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import noppes.npcs.CustomNpcs;
import noppes.npcs.LogWriter;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.formation.FormationMath;
import noppes.npcs.rework.formation.FormationMoveTask;
import noppes.npcs.rework.formation.SquadOrder;
import noppes.npcs.rework.formation.SquadPlanner;
import noppes.npcs.rework.formation.WorldTerrain;

/**
 * M11.11 free-for-all march. One shared route (one search per leg, extended by the front runner), but
 * every member walks it at its own pace: no anchor, no companies, no waiting for the slowest. Only a
 * member that gets stuck, or starts away from the route, runs its own bounded search. The formation is
 * taken only at the goal; a member that cannot get there is reported blocked and never holds the others.
 * Runs on the server thread from {@link ArmyMarches}; never teleports.
 */
public final class ArmyFreeMarch {
    /** Real path searches per army per tick (route legs and member recoveries together). */
    public static final int PATH_BUDGET = 4;
    static final int DECIDE = 10, MEMBER_SEARCH = 20, WINDOW = 32, EXT_RETRY = 40, EXT_TRIES = 5, STALE = 40;
    static final double ON_ROUTE = 3.0, REJOIN = 10.0, SETTLE = 10.0, ARRIVE = 0.5, LEAVE = 1.5, REFINE = 1.5, SNAP = 0.3;
    /** Blocks around the goal that must be loaded before slots are placed on real ground. */
    static final int GOAL_AREA = 24;
    /** Widest side offset of a lane; members farther out walk the outermost lane. */
    static final double LANE = 32;
    /** Route points walked centrally after a failed lane, and points to ramp back out to the lane. */
    static final int CENTRE = 24, RAMP = 16;
    private static final double[] TURN = {0, 45, -45, 90, -90};

    private static final List<EntityNPCInterface> removeFrom = new ArrayList<EntityNPCInterface>();
    private static final List<EntityAIBase> removeTask = new ArrayList<EntityAIBase>();
    private static final Set<ArmyFreeMarch> live = new HashSet<ArmyFreeMarch>();

    enum Phase { ROUTE, OWN, SLOT, HOLD }

    private final class Member {
        final EntityNPCInterface npc;
        final Task task;
        final ArmyFreePlan.Walker walk = new ArmyFreePlan.Walker();
        Phase phase = Phase.ROUTE;
        int idx = -1, windowEnd = -1, lastSearch = -1000, lastTicks, stale;
        double sx, sy, sz, face;
        /** M11.11e: own lane, the shared route shifted sideways by where the member stood (lat along the side). */
        double ox, oz, lat;
        /** Up to this route index the member walks the shared route (its lane failed: a wall, a gap); ramps out after. */
        int centre = -1000;
        boolean busy, ticking = true;
        Member(EntityNPCInterface npc) { this.npc = npc; this.task = new Task(this); lastTicks = npc.ticksExisted; }
        boolean controls() { return !finished && ticking && !busy && !npc.isDead; }
    }

    /** Same priority and mutex as a formation member: wander, return home and moving path do not run. */
    private static final class Task extends EntityAIBase {
        private final Member m;
        Task(Member m) { this.m = m; setMutexBits(FormationMoveTask.MUTEX); }
        @Override public boolean shouldExecute() { return m.controls(); }
        @Override public boolean shouldContinueExecuting() { return m.controls(); }
        @Override public void updateTask() {
            if (m.phase != Phase.HOLD) { return; }
            // Members walking to rear slots push the holders: step straight back onto the slot.
            if (Math.hypot(m.npc.posX - m.sx, m.npc.posZ - m.sz) > SNAP) {
                m.npc.getNavigator().clearPath();
                m.npc.getMoveHelper().setMoveTo(m.sx, m.sy, m.sz, 0.6);
            } else {
                stopHorizontal(m.npc);
                // Settle onto the slot centre: a held member pressed by a neighbour drifts back, it does not stay
                // up to SNAP off (two such neighbours would stand 0.4 apart). A small step, no turning.
                m.npc.motionX = settle(m.sx - m.npc.posX);
                m.npc.motionZ = settle(m.sz - m.npc.posZ);
                face(m.npc, m.face, false);
            }
        }
    }

    private final String army;
    private final ArmyOrder order;
    private final World world;
    private final WorldTerrain terrain;
    private final List<Member> members = new ArrayList<Member>();
    private int[] rx = new int[256], ry = new int[256], rz = new int[256];
    private int size, extFails, extAt = -1000, ticks, budget, searches, legs;
    private boolean routeFinal, finished;
    /** Slots were placed on real ground; false while the goal area was not loaded (a far target). */
    private boolean grounded;
    private double yaw = Double.NaN;
    /** Origin and side direction of the lanes (fixed at the start of the march). */
    private double laneX, laneZ, sideX, sideZ;

    private ArmyFreeMarch(String army, World world, ArmyOrder order) {
        this.army = army; this.world = world; this.order = order; this.terrain = new WorldTerrain(world);
    }

    /** Builds the march; the first leg is searched by the member nearest the centre. Never null. */
    static ArmyFreeMarch start(String army, List<EntityNPCInterface> npcs, ArmyOrder order) {
        ArmyFreeMarch f = new ArmyFreeMarch(army, npcs.get(0).world, order);
        for (EntityNPCInterface npc : npcs) { f.members.add(f.new Member(npc)); }
        f.budget = PATH_BUDGET;
        double cx = 0, cz = 0;
        for (Member m : f.members) { cx += m.npc.posX; cz += m.npc.posZ; }
        cx /= f.members.size(); cz /= f.members.size();
        Member lead = null;
        for (Member m : f.members) {
            if (lead == null || Math.hypot(m.npc.posX - cx, m.npc.posZ - cz) < Math.hypot(lead.npc.posX - cx, lead.npc.posZ - cz)) { lead = m; }
        }
        f.extend(lead);
        f.yaw = Math.hypot(order.x - cx, order.z - cz) < 1 ? lead.npc.rotationYaw : FormationMath.yawOf(order.x - cx, order.z - cz);
        f.laneX = lead.npc.posX; f.laneZ = lead.npc.posZ;
        double dx = order.x - lead.npc.posX, dz = order.z - lead.npc.posZ, len = Math.hypot(dx, dz);
        if (len < 1) { dx = FormationMath.forwardX(f.yaw); dz = FormationMath.forwardZ(f.yaw); len = 1; }
        f.sideX = -dz / len; f.sideZ = dx / len;
        for (Member m : f.members) { f.lane(m); }
        f.assignSlots();
        live.add(f);
        LogWriter.info("RWARMY freeforall " + army + " members=" + f.members.size() + " route=" + f.size
                + " final=" + f.routeFinal);
        return f;
    }

    /** M7 U7: an army member steered here uses vanilla navigation, like a formation member. */
    public static boolean contains(EntityNPCInterface npc) {
        for (ArmyFreeMarch f : live) {
            if (!f.finished) { for (Member m : f.members) { if (m.npc == npc) { return true; } } }
        }
        return false;
    }

    /** Task removal happens at the army tick (world tick END), never inside an NPC's own task loop. */
    static void flushRemovals() {
        for (int i = 0; i < removeFrom.size(); i++) { removeFrom.get(i).tasks.removeTask(removeTask.get(i)); }
        removeFrom.clear(); removeTask.clear();
    }

    /** Every member gets its own destination slot around the goal, on standable ground where possible.
     * A new member reassigns all slots, so the formation stays complete; nobody restarts its walk. */
    private void assignSlots() {
        grounded = goalLoaded();
        int n = members.size();
        double[] xs = new double[n], zs = new double[n];
        for (int i = 0; i < n; i++) { xs[i] = members.get(i).npc.posX; zs[i] = members.get(i).npc.posZ; }
        double[][] dest = ArmyFreePlan.destinations(xs, zs, order.x, order.z, yaw, order.shape);
        Set<Long> used = new HashSet<Long>();
        for (int i = 0; i < n; i++) {
            Member m = members.get(i);
            double[] spot = standable(dest[i][0], order.y, dest[i][1], used);
            m.sx = spot[0]; m.sy = spot[1]; m.sz = spot[2]; m.face = dest[i][2];
            if (m.phase == Phase.HOLD) { m.phase = Phase.SLOT; }
        }
    }

    /** The whole slot area around the goal is loaded, so every slot can stand on real ground. */
    private boolean goalLoaded() {
        return world.isAreaLoaded(new BlockPos(order.x, order.y, order.z), GOAL_AREA);
    }

    private double[] standable(double x, double y, double z, Set<Long> used) {
        for (int r = 0; r <= 3; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) { continue; }
                    int bx = (int) Math.floor(x) + dx, bz = (int) Math.floor(z) + dz;
                    double sy = surfaceY(bx + 0.5, y, bz + 0.5);
                    long key = ((long) bx << 32) ^ (bz & 0xffffffffL);
                    if (!Double.isNaN(sy) && Math.abs(sy - y) <= 6 && used.add(key)) {
                        return new double[] {bx + 0.5, sy, bz + 0.5};
                    }
                }
            }
        }
        return new double[] {x, y, z};
    }

    /** Standable feet height nearest the hint within 16 blocks, NaN when none or not loaded. */
    private double surfaceY(double x, double hint, double z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z), y0 = (int) Math.floor(hint + 0.01);
        if (!world.isBlockLoaded(p.setPos(bx, Math.max(1, Math.min(254, y0)), bz))) { return Double.NaN; }
        for (int d = 0; d <= 16; d++) {
            for (int s = 0; s < (d == 0 ? 1 : 2); s++) {
                int y = s == 0 ? y0 + d : y0 - d;
                if (y < 1 || y > 254) { continue; }
                if (passable(p, bx, y, bz) && passable(p, bx, y + 1, bz)
                        && world.getBlockState(p.setPos(bx, y - 1, bz)).getMaterial().blocksMovement()) { return y; }
            }
        }
        return Double.NaN;
    }

    private boolean passable(BlockPos.MutableBlockPos p, int x, int y, int z) {
        Material m = world.getBlockState(p.setPos(x, y, z)).getMaterial();
        return !m.blocksMovement() && m != Material.LAVA && m != Material.FIRE;
    }

    /** Membership changed: drop the gone, add the new; nobody else restarts. */
    void sync(Collection<EntityNPCInterface> now) {
        Set<EntityNPCInterface> want = new HashSet<EntityNPCInterface>(now);
        for (int i = members.size() - 1; i >= 0; i--) {
            Member m = members.get(i);
            if (!want.remove(m.npc)) { release(m); members.remove(i); }
        }
        for (EntityNPCInterface npc : want) { Member m = new Member(npc); lane(m); members.add(m); }
        if (!want.isEmpty()) { assignSlots(); }
    }

    /** Side offset of the member from the leader at the start, perpendicular to the march, clamped. */
    private void lane(Member m) {
        double lat = (m.npc.posX - laneX) * sideX + (m.npc.posZ - laneZ) * sideZ;
        m.lat = Math.max(-LANE, Math.min(LANE, lat));
        m.ox = sideX * m.lat; m.oz = sideZ * m.lat;
    }

    /** Route point k on the member's own lane, or the shared route where the lane is not standable there. */
    private PathPoint lanePoint(Member m, int k) {
        double s = laneScale(m, k);
        if (Math.abs(m.lat) * s < 0.5) { return new PathPoint(rx[k], ry[k], rz[k]); }
        double x = rx[k] + 0.5 + m.ox * s, z = rz[k] + 0.5 + m.oz * s;
        double sy = surfaceY(x, ry[k], z);
        if (Double.isNaN(sy) || Math.abs(sy - ry[k]) > 3) { return new PathPoint(rx[k], ry[k], rz[k]); }
        return new PathPoint((int) Math.floor(x), (int) sy, (int) Math.floor(z));
    }

    /** Distance to route point k: on the member's lane or on the shared route, whichever is nearer. */
    private double laneDistance(Member m, double x, double z, int k) {
        double s = laneScale(m, k);
        return Math.min(Math.hypot(rx[k] + 0.5 + m.ox * s - x, rz[k] + 0.5 + m.oz * s - z),
                Math.hypot(rx[k] + 0.5 - x, rz[k] + 0.5 - z));
    }

    /** 0 on the shared route up to m.centre, then back out to the full lane over RAMP route points. */
    static double laneScale(int centre, int k) {
        return Math.max(0, Math.min(1, (k - centre) / (double) RAMP));
    }

    private double laneScale(Member m, int k) { return laneScale(m.centre, k); }

    /** The lane did not get the member through here: the shared route for the next CENTRE points. */
    private void centre(Member m) {
        m.centre = Math.max(m.centre, Math.max(0, m.idx) + CENTRE);
        m.windowEnd = -1;
    }

    private void release(Member m) {
        removeFrom.add(m.npc); removeTask.add(m.task);
        m.npc.getNavigator().clearPath();
    }

    /** One army tick at world tick END. */
    void tick() {
        if (finished) { return; }
        ticks++;
        budget = PATH_BUDGET;
        int ticking = 0;
        for (Member m : members) {
            EntityNPCInterface npc = m.npc;
            m.stale = npc.ticksExisted == m.lastTicks ? m.stale + 1 : 0;
            m.lastTicks = npc.ticksExisted;
            m.ticking = !npc.isDead && !npc.isKilled() && npc.world == world && m.stale <= STALE;
            if (!m.ticking) { continue; }
            ticking++;
            ensureTask(m);
            boolean was = m.busy;
            m.busy = busy(npc);
            if (was && !m.busy) { m.idx = -1; m.windowEnd = -1; m.walk.reset(ticks, npc.posX, npc.posZ); }
        }
        if (ticking == 0) { return; }
        if (!grounded && ticks % 20 == 0 && goalLoaded()) {
            // A far target: its terrain is known only now that the march (or its tickets) reached it.
            assignSlots();
            LogWriter.info("RWARMY freeforall-slots " + army + " grounded at goal");
        }
        if (!routeFinal && extFails < EXT_TRIES && ticks - extAt >= (extFails > 0 ? EXT_RETRY : DECIDE)) {
            Member front = null, onLane = null;
            for (Member m : members) {
                // Only a member standing at the shared route continues it; one that went its own way elsewhere
                // (e.g. already behind a wall) would leave a gap the others walk into. A member on its own lane
                // never searches the shared route: a lane leg moved back to the centre may cross a wall.
                if (!m.controls() || m.phase != Phase.ROUTE || m.idx < 0 || m.idx < size - 6) { continue; }
                if (Math.hypot(m.npc.posX - (rx[m.idx] + 0.5), m.npc.posZ - (rz[m.idx] + 0.5)) <= ON_ROUTE
                        && Math.abs(m.npc.posY - ry[m.idx]) <= 1.5) {
                    if (front == null || m.idx > front.idx) { front = m; }
                } else if (laneDistance(m, m.npc.posX, m.npc.posZ, m.idx) <= ON_ROUTE
                        && (onLane == null || m.idx > onLane.idx)) { onLane = m; }
            }
            if (size == 0) {
                for (Member m : members) { if (m.controls()) { extend(m); break; } }
            } else if (front != null) {
                extend(front);
            } else if (onLane != null && onLane.centre < onLane.idx) {
                // Only lane members at the front: the foremost walks over to the route end and continues it.
                centre(onLane);
                extAt = ticks;
            }
        }
        for (int i = 0; i < members.size(); i++) {
            Member m = members.get(i);
            if (m.controls() && (ticks + i) % DECIDE == 0) { decide(m); }
        }
    }

    /** Appends one bounded leg searched from the member's position. A leg that gains nothing is a failure. */
    private void extend(Member m) {
        if (budget <= 0) { return; }
        extAt = ticks;
        EntityNPCInterface npc = m.npc;
        PathNavigate nav = npc.getNavigator();
        double[] t = ArmyWaypoints.next(npc.posX, npc.posY, npc.posZ, order.x, order.y, order.z,
                CustomNpcs.NpcNavRange, nav.getPathSearchRange());
        if (t == null) { extFails = EXT_TRIES; return; }
        boolean last = ArmyWaypoints.finalLeg(t, order.x, order.y, order.z);
        double turn = TURN[Math.min(extFails, TURN.length - 1)];
        if (turn != 0) {
            double dx = t[0] - npc.posX, dz = t[2] - npc.posZ, a = Math.toRadians(turn);
            t[0] = npc.posX + dx * Math.cos(a) - dz * Math.sin(a);
            t[2] = npc.posZ + dx * Math.sin(a) + dz * Math.cos(a);
            last = false;
        }
        if (!last) {
            // The search below loads (or generates) the chunks around the searching member anyway;
            // load the leg's end column first so its height is real ground, not an interpolated guess.
            world.getChunkFromChunkCoords((int) Math.floor(t[0]) >> 4, (int) Math.floor(t[2]) >> 4);
            double sy = surfaceY(t[0], t[1], t[2]);
            if (!Double.isNaN(sy)) { t[1] = sy; }
        }
        budget--; searches++;
        nav.clearPath();
        Path p = nav.getPathToXYZ(t[0], t[1], t[2]);
        m.windowEnd = -1;
        double before = size == 0 ? dist(npc.posX, npc.posZ) : dist(rx[size - 1] + 0.5, rz[size - 1] + 0.5);
        if (p == null || p.getCurrentPathLength() == 0) { extFails++; logLeg(npc, t, null); return; }
        PathPoint end = p.getFinalPathPoint();
        boolean reaches = last && Math.hypot(end.x + 0.5 - order.x, end.z + 0.5 - order.z) <= 2.0
                && Math.abs(end.y - order.y) <= 2.0;
        // A leg that reaches the goal is final even when the previous end was already close to it.
        if (!reaches && dist(end.x + 0.5, end.z + 0.5) > before - 2) { extFails++; logLeg(npc, t, end); return; }
        for (int i = 0; i < p.getCurrentPathLength(); i++) {
            PathPoint q = p.getPathPointFromIndex(i);
            if (size > 0 && rx[size - 1] == q.x && ry[size - 1] == q.y && rz[size - 1] == q.z) { continue; }
            add(q.x, q.y, q.z);
        }
        legs++; extFails = 0;
        routeFinal = reaches;
        logLeg(npc, t, end);
    }

    private void logLeg(EntityNPCInterface npc, double[] t, PathPoint end) {
        LogWriter.info("RWARMY freeforall-leg " + army + " from=" + (int) npc.posX + "," + (int) npc.posY + "," + (int) npc.posZ
                + " target=" + (int) t[0] + "," + (int) t[1] + "," + (int) t[2] + " end="
                + (end == null ? "null" : end.x + "," + end.y + "," + end.z) + " route=" + size
                + " final=" + routeFinal + " fails=" + extFails);
    }

    private void add(int x, int y, int z) {
        if (size == rx.length) {
            rx = java.util.Arrays.copyOf(rx, size * 2); ry = java.util.Arrays.copyOf(ry, size * 2);
            rz = java.util.Arrays.copyOf(rz, size * 2);
        }
        rx[size] = x; ry[size] = y; rz[size] = z; size++;
    }

    private double dist(double x, double z) { return Math.hypot(x - order.x, z - order.z); }

    private boolean routeFrozen() { return !routeFinal && extFails >= EXT_TRIES; }

    private void decide(Member m) {
        EntityNPCInterface npc = m.npc;
        double toSlot = Math.hypot(npc.posX - m.sx, npc.posZ - m.sz), dy = Math.abs(npc.posY - m.sy);
        if (m.phase == Phase.HOLD) {
            if (toSlot <= LEAVE && dy <= 1.5) { return; }
            m.phase = Phase.SLOT;
        }
        if (toSlot <= ARRIVE && dy <= 1.5) {
            m.phase = Phase.HOLD; npc.getNavigator().clearPath(); stopHorizontal(npc);
            return;
        }
        boolean stuck = m.walk.stuck(ticks, npc.posX, npc.posZ);
        if (m.walk.blocked()) {
            if (m.walk.retryDue(ticks)) { recover(m); }
            return;
        }
        if (stuck) { recover(m); return; }
        if (m.phase == Phase.OWN && !npc.getNavigator().noPath()) { return; }
        nearest(m);
        boolean nearGoal = toSlot <= SETTLE && (routeFinal || routeFrozen() || size == 0);
        boolean routeDone = routeFinal && m.idx >= 0 && remaining(m.idx) <= SETTLE;
        if (nearGoal || routeDone || routeFrozen() && m.idx >= size - 2 || size == 0 && routeFrozen()) {
            toSlot(m);
            return;
        }
        if (size == 0) { return; } // the first leg is still being searched
        if (!routeFinal && dist(npc.posX, npc.posZ) + 4 < dist(rx[size - 1] + 0.5, rz[size - 1] + 0.5)) {
            toSlot(m); // already ahead of the route (e.g. after a restart): never walk back to it
            return;
        }
        double off = laneDistance(m, npc.posX, npc.posZ, m.idx);
        PathPoint here = lanePoint(m, m.idx);
        // Pushed aside by the crowd: walk straight back onto the lane when the line is clear.
        // Only a member really away from it (or behind a wall) runs its own search.
        if (off > ON_ROUTE && (off > REJOIN || Math.abs(npc.posY - here.y) > 1.5
                || !SquadPlanner.straightWalkable(terrain, npc.posX, npc.posY, npc.posZ, here.x + 0.5, here.z + 0.5))) {
            // Away from the lane or cut off from it: rejoin the shared route, which is known to go through.
            centre(m);
            int k = off > REJOIN ? m.idx : Math.min(size - 1, m.idx + 4);
            own(m, rx[k] + 0.5, ry[k], rz[k] + 0.5, 0);
            return;
        }
        m.phase = Phase.ROUTE;
        if (m.idx >= size - 2) {
            // At the front, the next leg comes from this member; one sent over from its lane walks onto the end.
            if (off > ON_ROUTE) { own(m, rx[m.idx] + 0.5, ry[m.idx], rz[m.idx] + 0.5, 0); }
            return;
        }
        PathNavigate nav = npc.getNavigator();
        if (nav.noPath() || m.windowEnd < 0 || m.idx > m.windowEnd - WINDOW / 2 || m.windowEnd < Math.min(size - 1, m.idx + WINDOW) && m.idx > m.windowEnd - 8) {
            int from = m.idx + 1, to = Math.min(size - 1, m.idx + WINDOW);
            PathPoint[] pts = new PathPoint[to - from + 1];
            for (int k = from; k <= to; k++) { pts[k - from] = lanePoint(m, k); }
            Path current = nav.getPath();
            if (current != null && current.isFinished()) { nav.clearPath(); }
            nav.setPath(new Path(pts), speedIn());
            m.windowEnd = to;
        }
        nav.setSpeed(speedIn());
    }

    private double remaining(int idx) {
        return Math.hypot(rx[size - 1] - rx[idx], rz[size - 1] - rz[idx]) + 0.0;
    }

    /** Nearest route index, searched in a window around the last one and in full when the window misses. */
    private void nearest(Member m) {
        if (size == 0) { m.idx = -1; return; }
        double x = m.npc.posX, z = m.npc.posZ;
        int best = -1; double bestD = Double.MAX_VALUE;
        if (m.idx >= 0) {
            for (int k = Math.max(0, m.idx - 8); k < Math.min(size, m.idx + 48); k++) {
                double d = laneDistance(m, x, z, k);
                if (d <= bestD) { bestD = d; best = k; }
            }
        }
        if (best < 0 || bestD > ON_ROUTE) {
            for (int k = 0; k < size; k++) {
                double d = laneDistance(m, x, z, k);
                if (d <= bestD) { bestD = d; best = k; }
            }
        }
        if (best != m.idx) { m.windowEnd = Math.min(m.windowEnd, best + WINDOW); }
        m.idx = best;
    }

    private void toSlot(Member m) {
        EntityNPCInterface npc = m.npc;
        double near = Math.hypot(npc.posX - m.sx, npc.posZ - m.sz);
        if (near <= REFINE && Math.abs(npc.posY - m.sy) <= 0.5
                && SquadPlanner.straightWalkable(terrain, npc.posX, npc.posY, npc.posZ, m.sx, m.sz)) {
            // The last walkable metre with the same move helper, onto the slot itself (as a company does);
            // path following alone stops anywhere inside its waypoint box.
            m.phase = Phase.SLOT;
            npc.getNavigator().clearPath();
            npc.getMoveHelper().setMoveTo(m.sx, m.sy, m.sz, Math.min(1.0, speedIn()));
            return;
        }
        if (Math.abs(npc.posY - m.sy) <= 1.5 && Math.hypot(npc.posX - m.sx, npc.posZ - m.sz) <= SquadPlanner.STEER_MAX
                && SquadPlanner.straightWalkable(terrain, npc.posX, npc.posY, npc.posZ, m.sx, m.sz)) {
            m.phase = Phase.SLOT;
            PathNavigate nav = npc.getNavigator();
            Path current = nav.getPath();
            if (current != null && current.isFinished()) { nav.clearPath(); }
            nav.setPath(new Path(new PathPoint[] {new PathPoint((int) Math.floor(m.sx), (int) Math.floor(m.sy),
                    (int) Math.floor(m.sz))}), speedIn());
            nav.setSpeed(speedIn());
            return;
        }
        if (m.phase == Phase.OWN && !npc.getNavigator().noPath()) { return; }
        double[] t = ArmyWaypoints.next(npc.posX, npc.posY, npc.posZ, m.sx, m.sy, m.sz,
                CustomNpcs.NpcNavRange, npc.getNavigator().getPathSearchRange());
        if (t == null) { return; }
        if (!ArmyWaypoints.finalLeg(t, m.sx, m.sy, m.sz)) {
            double sy = surfaceY(t[0], t[1], t[2]);
            if (!Double.isNaN(sy)) { t[1] = sy; }
        }
        own(m, t[0], t[1], t[2], 0);
    }

    /** Stuck: an own search toward a point ahead, shifted sideways on repeated failures. */
    private void recover(Member m) {
        EntityNPCInterface npc = m.npc;
        nearest(m);
        double tx, ty, tz;
        boolean toGoal = size == 0 || routeFrozen() && m.idx >= size - 2 || routeFinal && m.idx >= 0 && remaining(m.idx) <= SETTLE
                || Math.hypot(npc.posX - m.sx, npc.posZ - m.sz) <= SETTLE;
        if (!toGoal && m.idx < size - 2) {
            centre(m); // stuck on its lane: the shared route gets through, the lane resumes after it
            int j = Math.min(size - 1, m.idx + 8);
            tx = rx[j] + 0.5; ty = ry[j]; tz = rz[j] + 0.5;
        } else {
            double[] t = ArmyWaypoints.next(npc.posX, npc.posY, npc.posZ, m.sx, m.sy, m.sz,
                    CustomNpcs.NpcNavRange, npc.getNavigator().getPathSearchRange());
            if (t == null) { return; }
            tx = t[0]; ty = t[1]; tz = t[2];
        }
        if (budget <= 0 || ticks - m.lastSearch < MEMBER_SEARCH) { return; }
        int attempt = m.walk.recovery(ticks, npc.posX, npc.posZ);
        own(m, tx, ty, tz, attempt);
        if (m.walk.blocked() && attempt == ArmyFreePlan.MAX_FAILS) {
            LogWriter.info("RWARMY freeforall-blocked " + army + " uuid=" + npc.getUniqueID() + " at="
                    + (int) npc.posX + "," + (int) npc.posY + "," + (int) npc.posZ);
        }
    }

    /** One own search, bounded like a route leg (vanilla charges Manhattan steps against the range). */
    private void own(Member m, double tx, double ty, double tz, int attempt) {
        if (budget <= 0 || ticks - m.lastSearch < MEMBER_SEARCH) { return; }
        EntityNPCInterface npc = m.npc;
        double[] b = ArmyWaypoints.next(npc.posX, npc.posY, npc.posZ, tx, ty, tz,
                CustomNpcs.NpcNavRange, npc.getNavigator().getPathSearchRange());
        if (b == null) { return; }
        boolean shortened = !ArmyWaypoints.finalLeg(b, tx, ty, tz);
        double[] s = ArmyFreePlan.side(npc.posX, npc.posZ, b[0], b[2], attempt);
        double y = b[1];
        if (shortened || s[0] != b[0] || s[1] != b[2]) {
            double sy = surfaceY(s[0], b[1], s[1]);
            if (!Double.isNaN(sy)) { y = sy; }
        }
        budget--; searches++; m.lastSearch = ticks;
        PathNavigate nav = npc.getNavigator();
        nav.clearPath();
        nav.tryMoveToXYZ(s[0], y, s[1], speedIn());
        m.phase = Phase.OWN; m.windowEnd = -1;
    }

    private double speedIn() { return order.speed * SquadOrder.SCRIPT_SPEED_FACTOR; }

    private boolean busy(EntityNPCInterface npc) {
        return npc.isRiding() || order.stance == ArmyOrder.Stance.ENGAGE && (npc.isAttacking()
                || npc.getAttackTarget() != null && npc.getAttackTarget().isEntityAlive()
                || npc.getRevengeTarget() != null && npc.getRevengeTarget().isEntityAlive());
    }

    private void ensureTask(Member m) {
        EntityAITasks t = m.npc.tasks;
        for (EntityAITasks.EntityAITaskEntry e : t.taskEntries) { if (e.action == m.task) { return; } }
        t.addTask(FormationMoveTask.PRIORITY, m.task);
    }

    /** Every ticking member holds its slot or has given up; fighting members are not done yet. */
    boolean done() {
        if (finished) { return true; }
        boolean any = false;
        for (Member m : members) {
            if (!m.ticking) { continue; }
            any = true;
            if (m.busy || m.phase != Phase.HOLD && !m.walk.gaveUp(ticks)) { return false; }
        }
        return any;
    }

    /** No member ticks any more (all unloaded or dead): the march waits for chunks. */
    boolean emptied() {
        for (Member m : members) { if (m.ticking) { return false; } }
        return true;
    }

    int arrived() { int n = 0; for (Member m : members) { if (m.ticking && m.phase == Phase.HOLD) { n++; } } return n; }

    String status() {
        int arrived = 0, blocked = 0, moving = 0, fighting = 0, waiting = 0;
        double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
        for (Member m : members) {
            if (m.ticking && m.phase != Phase.HOLD) {
                // Width of the marching army across its direction (M11.11e: lanes, not a single file).
                double side = (m.npc.posX - laneX) * sideX + (m.npc.posZ - laneZ) * sideZ;
                lo = Math.min(lo, side); hi = Math.max(hi, side);
            }
            if (!m.ticking) { waiting++; }
            else if (m.busy) { fighting++; }
            else if (m.phase == Phase.HOLD) { arrived++; }
            else if (m.walk.blocked()) { blocked++; }
            else { moving++; }
        }
        return "mode=freeforall members=" + members.size() + " arrived=" + arrived + " moving=" + moving
                + " blocked=" + blocked + " fighting=" + fighting + " unloaded=" + waiting + " route=" + size
                + " legs=" + legs + " final=" + routeFinal + " searches=" + searches + " age=" + ticks
                + " spread=" + (hi < lo ? 0 : Math.round(hi - lo));
    }

    /** End: arrived members take their slot as home and face the formation direction; tasks go away. */
    void finish() {
        if (finished) { return; }
        for (Member m : members) {
            if (m.ticking && m.phase == Phase.HOLD) {
                stopHorizontal(m.npc);
                m.npc.ais.setStartPos(new BlockPos(m.sx, m.sy, m.sz));
                face(m.npc, m.face, true);
            } else if (m.ticking && !m.busy) {
                stopHorizontal(m.npc);
            }
        }
        cancel();
    }

    /** Immediate stop without anchoring; safe from a script call (task removal is deferred). */
    void cancel() {
        if (finished) { return; }
        finished = true;
        live.remove(this);
        for (Member m : members) {
            release(m);
            if (!m.busy) { stopHorizontal(m.npc); }
        }
    }

    List<EntityNPCInterface> npcs() {
        List<EntityNPCInterface> out = new ArrayList<EntityNPCInterface>();
        for (Member m : members) { out.add(m.npc); }
        return out;
    }

    static void clearAll() {
        for (ArmyFreeMarch f : new ArrayList<ArmyFreeMarch>(live)) { f.cancel(); }
        live.clear();
        flushRemovals();
    }

    /** Horizontal step toward the slot centre while holding: a fraction of the offset, none within 2 cm. */
    static double settle(double off) {
        return Math.abs(off) < 0.02 ? 0 : Math.max(-0.05, Math.min(0.05, off * 0.3));
    }

    private static void stopHorizontal(EntityNPCInterface npc) {
        npc.getNavigator().clearPath();
        npc.getMoveHelper().action = EntityMoveHelper.Action.WAIT;
        npc.setAIMoveSpeed(0);
        npc.setMoveForward(0);
        npc.setMoveStrafing(0);
        npc.motionX = 0;
        npc.motionZ = 0;
    }

    private static void face(EntityNPCInterface npc, double yaw, boolean persist) {
        float y = (float) yaw;
        npc.rotationYaw = y; npc.rotationYawHead = y; npc.renderYawOffset = y;
        if (persist) {
            int orientation = (int) Math.round(((yaw % 360.0) + 360.0) % 360.0);
            if (npc.ais.orientation != orientation) { npc.ais.orientation = orientation; npc.updateClient = true; }
        }
    }
}
