package noppes.npcs.rework.army;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import noppes.npcs.LogWriter;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.formation.FormationMath;
import noppes.npcs.rework.formation.PathTrack;
import noppes.npcs.rework.formation.Slot;
import noppes.npcs.rework.formation.SlotAssigner;
import noppes.npcs.rework.formation.Squad;
import noppes.npcs.rework.formation.SquadManager;
import noppes.npcs.rework.formation.SquadOrder;

/** M11.3: server-thread adapter. Durable intent lives in ArmyRegistry; all runtime references die at stop.
 * M11.6: chunk tickets travel with up to RwArmyMaxMarches armies (ArmyTickets); no long-path continuation
 * (M11.5, v2). Only enabled servers register a listener.
 */
public final class ArmyMarches {
    private static ArmyMarches installed;
    private final MinecraftServer server;
    private final Map<String, Run> runs = new HashMap<String, Run>();
    private final Set<EntityNPCInterface> passive = new HashSet<EntityNPCInterface>();
    private final ArmyTickets tickets = new ArmyTickets();
    private final Set<String> queued = new HashSet<String>();
    /** M11.6: members a run lost because they stopped ticking, with their ticksExisted at the loss. */
    private final Map<String, Map<EntityNPCInterface, Integer>> stale = new HashMap<String, Map<EntityNPCInterface, Integer>>();
    private int ticks;

    private static final class Run {
        final List<EntityNPCInterface> members;
        final List<Squad> squads = new ArrayList<Squad>();
        boolean joining;
        int age, paths;
        Run(List<EntityNPCInterface> members) { this.members = members; }
        void cancel() {
            if (installed != null) { installed.passive.removeAll(members); }
            for (Squad s : squads) { s.cancelArmy(); }
        }
    }

    private ArmyMarches(MinecraftServer server) { this.server = server; }

    public static void install(MinecraftServer server) {
        clear();
        if (ArmyCommandService.enabled()) {
            installed = new ArmyMarches(server);
            MinecraftForge.EVENT_BUS.register(installed);
        }
    }

    public static void clear() {
        if (installed != null) {
            for (Run r : installed.runs.values()) { r.cancel(); }
            installed.runs.clear();
            installed.stale.clear();
            installed.tickets.releaseAll("released");
            MinecraftForge.EVENT_BUS.unregister(installed);
            installed = null;
        }
    }

    /** M11.6: before the last save at server stopping, so no army ticket reaches forcedchunks.dat. */
    public static void releaseTickets() {
        if (installed != null) { installed.tickets.releaseAll("released-at-stop"); }
    }

    public static void cancel(String army) {
        if (installed != null) {
            Run r = installed.runs.remove(Army.key(army));
            if (r != null) { r.cancel(); }
        }
    }

    /** Only members controlled by an active passive army order. No world lookup at switch 0. */
    public static boolean isPassive(EntityNPCInterface npc) {
        return ArmyCommandService.enabled() && installed != null && !npc.world.isRemote
                && installed.passive.contains(npc);
    }

    /** Membership changes invalidate only army-controlled squads, never an unrelated /rwsquad. */
    public static void detach(EntityNPCInterface npc) {
        if (installed == null) { return; }
        for (Map.Entry<String, Run> e : new ArrayList<Map.Entry<String, Run>>(installed.runs.entrySet())) {
            if (e.getValue().members.contains(npc)) { cancel(e.getKey()); }
        }
    }

    public static String status(String army) {
        Run r = installed == null ? null : installed.runs.get(Army.key(army));
        String held = installed == null || ArmyTickets.maxMarches() == 0 ? "" : " " + installed.tickets.status(army);
        return (r == null ? "companies=0 members=0 paths=0" : "companies=" + r.squads.size()
                + " members=" + r.members.size() + " paths=" + r.paths + " age=" + r.age) + held;
    }

    private void state(ArmyRegistry reg, String name, ArmyOrder.State state) {
        ArmyOrder order = reg.order(name);
        if (order.state != state) {
            reg.order(name, order.withState(state));
            ArmyWorldData.get(server).markDirty();
            LogWriter.info("RWARMY order " + name + " state=" + state + " " + status(name));
        }
    }

    private static boolean emptied(Run r) {
        for (Squad s : r.squads) { if (s.emptied()) { return true; } }
        return false;
    }

    public static boolean eligible(EntityNPCInterface npc) {
        return !npc.isDead && !npc.isKilled() && !npc.isRiding() && npc.ais.movementType != 1;
    }

    @SubscribeEvent
    public void tick(TickEvent.WorldTickEvent e) {
        if (e.world.isRemote || e.phase != TickEvent.Phase.END || e.world != server.getWorld(0)) { return; }
        if (!ArmyCommandService.enabled()) { clear(); return; }
        ticks++;
        // Intent is loaded even before the first NPC appears; active orders resume as chunks load.
        ArmyRegistry reg = ArmyWorldData.get(server).registry();
        for (Army army : reg.list()) {
            ArmyOrder order = reg.order(army.name());
            if (order == null || !order.active()) { stale.remove(army.key()); continue; }
            Run r = runs.get(army.key());
            if (r == null || ticks % 20 == 0) {
                List<EntityNPCInterface> live = ArmyCommandService.members(server, army.name());
                Map<EntityNPCInterface, Integer> frozen = stale.get(army.key());
                if (frozen != null) {
                    // Still listed but not ticking (its chunk is unloading or outside any loaded area) => not live.
                    frozen.keySet().retainAll(new HashSet<EntityNPCInterface>(live));
                    frozen.entrySet().removeIf(f -> f.getKey().ticksExisted != f.getValue().intValue());
                    if (frozen.isEmpty()) { stale.remove(army.key()); frozen = null; }
                }
                final Map<EntityNPCInterface, Integer> skip = frozen;
                live.removeIf(npc -> !eligible(npc) || npc.world.provider.getDimension() != order.dimension
                        || skip != null && skip.containsKey(npc));
                Set<EntityNPCInterface> now = new HashSet<EntityNPCInterface>(live);
                if (r != null && !now.equals(new HashSet<EntityNPCInterface>(r.members))) {
                    r.cancel(); runs.remove(army.key()); r = null;
                }
                if (r == null) {
                    if (live.isEmpty()) { state(reg, army.name(), ArmyOrder.State.WAITING); continue; }
                    if (live.size() > 2048) { state(reg, army.name(), ArmyOrder.State.BLOCKED); continue; }
                    r = build(army.name(), live, order);
                    runs.put(army.key(), r);
                    if (r.squads.isEmpty()) {
                        state(reg, army.name(), ArmyOrder.State.BLOCKED); continue;
                    }
                    if (order.stance == ArmyOrder.Stance.PASSIVE) {
                        passive.addAll(live);
                        for (EntityNPCInterface npc : live) {
                            npc.setAttackTarget(null); npc.setRevengeTarget(null);
                        }
                    }
                    state(reg, army.name(), r.joining ? ArmyOrder.State.JOINING : ArmyOrder.State.MOVING);
                }
            }
            r.age++;
            double slowest = Double.POSITIVE_INFINITY;
            boolean done = true, arrived = true;
            for (Squad s : r.squads) {
                done &= s.isFinished(); arrived &= s.arrived();
                slowest = Math.min(slowest, s.progress());
            }
            if (!r.joining) {
                for (Squad s : r.squads) { s.limitAdvance(slowest + ArmyCompanies.MAX_LEAD); }
            }
            if (done || r.age >= 2400) {
                if (r.joining && done && arrived) {
                    r.cancel(); runs.remove(army.key()); // rebuild with the reunited membership, same durable goal
                } else if (done && !arrived && emptied(r)) {
                    // M11.6: a company lost every member to an unload; that waits for chunks, it is not a blocked
                    // path. The run is rebuilt once they tick again (a ticket or a player loads them).
                    Map<EntityNPCInterface, Integer> frozen = new HashMap<EntityNPCInterface, Integer>();
                    for (EntityNPCInterface npc : r.members) { frozen.put(npc, npc.ticksExisted); }
                    stale.put(army.key(), frozen);
                    r.cancel(); runs.remove(army.key());
                    state(reg, army.name(), ArmyOrder.State.WAITING);
                } else {
                    state(reg, army.name(), done && arrived ? ArmyOrder.State.ARRIVED : ArmyOrder.State.BLOCKED);
                    r.cancel(); // timeout never implies arrival and never teleports
                }
            }
        }
        if (ticks % 20 == 0 && (ArmyTickets.maxMarches() > 0 || !tickets.holders().isEmpty())) { tickets(reg); }
    }

    /** M11.6: once per second; which armies hold chunks and which chunks. Nothing at RwArmyMaxMarches=0. */
    private void tickets(ArmyRegistry reg) {
        List<String> active = new ArrayList<String>();
        Map<String, String> names = new HashMap<String, String>();
        Map<String, WorldServer> worlds = new HashMap<String, WorldServer>();
        for (Army army : reg.list()) {
            ArmyOrder order = reg.order(army.name());
            WorldServer world = order == null ? null : DimensionManager.getWorld(order.dimension);
            if (order == null || !order.active() || world == null) { continue; }
            active.add(army.key()); names.put(army.key(), army.name()); worlds.put(army.key(), world);
        }
        List<String> granted = ArmyTicketPlan.slots(tickets.holders(), active, ArmyTickets.maxMarches());
        queued.retainAll(active);
        queued.removeAll(granted);
        for (String key : active) {
            if (!granted.contains(key) && queued.add(key)) {
                LogWriter.info("RWARMY tickets " + names.get(key) + " queued slots=" + granted.size() + "/"
                        + ArmyTickets.maxMarches() + " (marches without tickets)");
            }
        }
        Map<String, LinkedHashSet<Long>> wanted = new HashMap<String, LinkedHashSet<Long>>();
        Map<String, Boolean> resume = new HashMap<String, Boolean>();
        Map<String, List<EntityNPCInterface>> lives = new HashMap<String, List<EntityNPCInterface>>();
        for (String key : active) {
            Run r = runs.get(key);
            List<EntityNPCInterface> live = new ArrayList<EntityNPCInterface>();
            if (r != null) {
                for (EntityNPCInterface npc : r.members) {
                    if (!npc.isDead && npc.world == worlds.get(key)) { live.add(npc); }
                }
            }
            lives.put(key, live);
            if (live.isEmpty() || ArmyTickets.maxMarches() == 0) { continue; }
            // Keep the saved last known position current (queued armies too), so a march that lost its
            // chunks or a restart without players resumes here once it holds a slot.
            List<ArmyClusters.Point> points = new ArrayList<ArmyClusters.Point>();
            for (EntityNPCInterface npc : live) {
                points.add(new ArmyClusters.Point(npc.world.provider.getDimension(), npc.posX, npc.posY, npc.posZ));
            }
            ArmyClusters.Cluster main = ArmyClusters.cluster(points, ArmyCommandService.clusterRadius(),
                    ArmyClusters.DEFAULT_MIN_MEMBERS).get(0);
            if (reg.noteSeen(key, main.dimension, main.x, main.y, main.z, server.getWorld(0).getTotalWorldTime())) {
                ArmyWorldData.get(server).markDirty();
            }
        }
        for (String key : granted) {
            Army army = reg.get(names.get(key));
            ArmyOrder order = reg.order(army.name());
            List<EntityNPCInterface> live = lives.get(key);
            if (!live.isEmpty()) {
                double[] xs = new double[live.size()], zs = new double[live.size()];
                for (int i = 0; i < live.size(); i++) { xs[i] = live.get(i).posX; zs[i] = live.get(i).posZ; }
                wanted.put(key, ArmyTicketPlan.members(xs, zs, ArmyTicketPlan.RADIUS, ArmyTicketPlan.MAX_CHUNKS_PER_MARCH));
            } else if (army.lastSeen() >= 0 && army.lastDimension() == order.dimension) {
                wanted.put(key, ArmyTicketPlan.resume(army.lastX(), army.lastZ()));
                resume.put(key, Boolean.TRUE);
            }
        }
        tickets.apply(granted, names, worlds, wanted, resume);
    }

    private Run build(String army, List<EntityNPCInterface> live, ArmyOrder order) {
        Collections.sort(live, Comparator.comparing(npc -> npc.getUniqueID().toString()));
        Run run = new Run(live);
        double[] xs = new double[live.size()], zs = new double[live.size()];
        for (int i = 0; i < live.size(); i++) { xs[i] = live.get(i).posX; zs[i] = live.get(i).posZ; }
        boolean[] inMain = ArmyCompanies.mainMembers(xs, zs, ArmyCommandService.clusterRadius());
        List<EntityNPCInterface> joined = new ArrayList<EntityNPCInterface>();
        double mainX = 0, mainZ = 0;
        for (int i = 0; i < live.size(); i++) {
            if (inMain[i]) { joined.add(live.get(i)); mainX += xs[i]; mainZ += zs[i]; }
        }
        final double centreX = mainX / joined.size(), centreZ = mainZ / joined.size();
        EntityNPCInterface leader = Collections.min(joined, Comparator.comparingDouble(npc ->
                Math.hypot(npc.posX - centreX, npc.posZ - centreZ)));
        List<EntityNPCInterface> behind = new ArrayList<EntityNPCInterface>(live);
        behind.removeAll(joined);
        World world = leader.world;
        if (!behind.isEmpty()) {
            run.joining = true;
            List<List<EntityNPCInterface>> groups = ArmyCompanies.split(behind, order.companySize);
            Slot[] slots = ArmyCompanies.macro(groups.size(), order.companySize, SquadOrder.Shape.OBRAMBA);
            for (int i = 0; i < groups.size(); i++) {
                double x = FormationMath.worldX(centreX, 0, slots[i]);
                double z = FormationMath.worldZ(centreZ, 0, slots[i]);
                List<EntityNPCInterface> group = groups.get(i);
                double gx = 0, gz = 0;
                for (EntityNPCInterface npc : group) { gx += npc.posX; gz += npc.posZ; }
                final double groupX = gx / group.size(), groupZ = gz / group.size();
                EntityNPCInterface joinLeader = Collections.min(group, Comparator.comparingDouble(npc ->
                        Math.hypot(npc.posX - groupX, npc.posZ - groupZ)));
                double[] target = ArmyCompanies.joinTarget(joinLeader.posX, joinLeader.posY, joinLeader.posZ,
                        x, leader.posY, z);
                PathTrack approach = Squad.toTrack(joinLeader.getNavigator().getPathToXYZ(target[0], target[1], target[2]));
                if (!complete(approach, target[0], target[1], target[2])) {
                    run.cancel(); run.squads.clear();
                    LogWriter.info("RWARMY blocked " + army + " reason=no-complete-join-path");
                    return run;
                }
                SquadOrder join = new SquadOrder(SquadOrder.Shape.LEGIJA, 4, target[0], target[1], target[2], 0,
                        order.speed, order.stance == ArmyOrder.Stance.ENGAGE, false, false);
                Squad s = SquadManager.start(world, army + "-join-" + i, group, join, approach);
                if (s != null) { run.squads.add(s); run.paths++; }
            }
            LogWriter.info("RWARMY join " + army + " stragglers=" + behind.size());
            return run;
        }
        PathTrack path = Squad.toTrack(leader.getNavigator().getPathToXYZ(order.x, order.y, order.z));
        run.paths = 1;
        if (!complete(path, order.x, order.y, order.z)) {
            LogWriter.info("RWARMY blocked " + army + " reason=no-complete-path (long paths in M11.5)");
            return run;
        }
        double yaw = path.yawAt(path.length(), 6, 0);
        if (Double.isNaN(yaw)) { yaw = leader.rotationYaw; }
        // Neighbours go into the same company. A deterministic UUID breaks distance ties.
        List<EntityNPCInterface> spatial = new ArrayList<EntityNPCInterface>();
        List<EntityNPCInterface> remaining = new ArrayList<EntityNPCInterface>(live);
        while (!remaining.isEmpty()) {
            EntityNPCInterface first = remaining.get(0);
            remaining.sort(Comparator.comparingDouble(npc -> npc.getDistanceSq(first)));
            int n = Math.min(order.companySize, remaining.size());
            spatial.addAll(remaining.subList(0, n)); remaining.subList(0, n).clear();
        }
        List<List<EntityNPCInterface>> groups = ArmyCompanies.split(spatial, order.companySize);
        Slot[] slots = ArmyCompanies.macro(groups.size(), order.companySize, order.shape);
        if (order.mode == ArmyOrder.Mode.SWARM && groups.size() > 1) {
            long[] seeds = new long[groups.size()];
            for (int i = 0; i < seeds.length; i++) { seeds[i] = army.hashCode() * 31L + i; }
            slots = ArmySwarm.scatter(seeds, 2 * ArmySwarm.radius(order.companySize, ArmySwarm.SEPARATION) + 2);
        }
        double[] cx = new double[groups.size()], cz = new double[groups.size()];
        for (int i = 0; i < groups.size(); i++) {
            for (EntityNPCInterface npc : groups.get(i)) { cx[i] += npc.posX; cz[i] += npc.posZ; }
            cx[i] = cx[i] / groups.get(i).size() - path.x(0);
            cz[i] = cz[i] / groups.get(i).size() - path.z(0);
        }
        int[] assignment = order.mode == ArmyOrder.Mode.SWARM ? new int[groups.size()]
                : SlotAssigner.assign(cx, cz, slots, yaw);
        if (order.mode == ArmyOrder.Mode.SWARM) {
            if (groups.size() == 1) { slots = new Slot[] {new Slot(0, 0)}; }
            for (int i = 0; i < assignment.length; i++) { assignment[i] = i; }
        }
        for (int i = 0; i < groups.size(); i++) {
            PathTrack lane = ArmyCompanies.lane(path, slots[assignment[i]], yaw);
            Slot[] scatter = null;
            if (order.mode == ArmyOrder.Mode.SWARM) {
                long[] seeds = new long[groups.get(i).size()];
                for (int j = 0; j < seeds.length; j++) {
                    java.util.UUID id = groups.get(i).get(j).getUniqueID();
                    seeds[j] = id.getMostSignificantBits() ^ id.getLeastSignificantBits();
                }
                scatter = ArmySwarm.scatter(seeds, ArmySwarm.SEPARATION);
            }
            SquadOrder company = new SquadOrder(order.shape, order.shape == SquadOrder.Shape.OBRAMBA ? 3 :
                    order.shape == SquadOrder.Shape.KOLONA ? 2 : 4, lane.endX(), lane.endY(), lane.endZ(), yaw,
                    order.speed, order.stance == ArmyOrder.Stance.ENGAGE, true, true, scatter);
            Squad s = SquadManager.start(world, army + "-company-" + i, groups.get(i), company, lane);
            if (s != null) { run.squads.add(s); }
        }
        LogWriter.info("RWARMY march " + army + " companies=" + run.squads.size() + " members=" + live.size()
                + " paths=1 size=" + order.companySize);
        return run;
    }

    private static boolean complete(PathTrack path, double x, double y, double z) {
        return path != null && Math.hypot(path.endX() - x, path.endZ() - z) <= Squad.GOAL_REACHED
                && Math.abs(path.endY() - y) <= Squad.GOAL_REACHED;
    }
}
