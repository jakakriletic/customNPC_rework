package noppes.npcs.rework.army;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.server.management.UserListOpsEntry;
import net.minecraft.world.WorldServer;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * M11.1 (Q296, Q297, Q299): server side of armies, shared by {@code /rwarmy} and {@link ArmyApi}. Everything
 * returns a {@link Reply} and changes nothing while RwArmies=0. Provisional API version 0; M11.7a freezes the
 * service for external bridges as version 1. M11.1b: every call takes an {@link ArmyActor} and is checked here on
 * the server. An army the actor may not see answers exactly like one that does not exist.
 */
public final class ArmyCommandService {
    public static final int API_VERSION = 0;
    /** Vanilla level of ordinary operator commands. */
    public static final int OPERATOR_LEVEL = 2;
    public static final String COMMAND_NAME = "rwarmy";
    /** Word for "every army" in grant and revoke, so it cannot be the name of a new army. */
    public static final String ALL = "all";
    private static final int MAX_LINES = 50;

    /** Result line of one call; every line starts with RWARMY so harnesses can find it in the log. */
    public static final class Reply {
        public final boolean ok;
        public final String message;

        Reply(boolean ok, String message) {
            this.ok = ok;
            this.message = "RWARMY " + message;
        }
    }

    /** Copy of CustomNpcs.RwArmies, set at server start, so this class never initializes CustomNpcs. */
    private static volatile int armies;

    /** Copies of RwArmyClusterRadius and RwArmyClusterMin, for the same reason. */
    private static volatile int clusterRadius = ArmyClusters.DEFAULT_RADIUS;
    private static volatile int clusterMin = ArmyClusters.DEFAULT_MIN_MEMBERS;

    private ArmyCommandService() {
    }

    /** Called from CustomNpcs at server start (and by unit tests). */
    public static void setSwitch(int value) {
        armies = value;
    }

    /** Called with {@link #setSwitch}; values are clamped to their limits. */
    public static void setClusters(int radius, int minMembers) {
        clusterRadius = ArmyClusters.clampRadius(radius);
        clusterMin = ArmyClusters.clampMinMembers(minMembers);
    }

    public static boolean enabled() {
        return armies >= 1;
    }

    private static Reply off() {
        return new Reply(false, "off (RwArmies=0)");
    }

    private static ArmyRegistry registry(MinecraftServer server) {
        ArmyWorldData data = ArmyWorldData.get(server);
        return data == null ? null : data.registry();
    }

    private static void dirty(MinecraftServer server) {
        ArmyWorldData data = ArmyWorldData.get(server);
        if (data != null) {
            data.markDirty();
        }
    }

    /** The trusted caller of {@link ArmyApi}. */
    public static Reply create(MinecraftServer server, String name, String ownerSpec) {
        return create(server, ArmyActor.SYSTEM, name, ownerSpec);
    }

    /**
     * Creating stays with operators (Q309): every army costs a place in the bounded register.
     *
     * @param ownerSpec {@code uuid:<uuid>} or {@code faction:<id>}; the faction must exist.
     */
    public static Reply create(MinecraftServer server, ArmyActor actor, String name, String ownerSpec) {
        if (!enabled()) {
            return off();
        }
        if (!actor.operator()) {
            return new Reply(false, "create needs an operator");
        }
        if (!Army.validName(name) || isAll(name)) {
            return new Reply(false, "invalid name '" + name + "' (1-32 letters, digits, _ or -, not all)");
        }
        Object[] owner = ArmyRegistry.parseOwnerSpec(ownerSpec);
        if (owner == null) {
            return new Reply(false, "invalid owner '" + ownerSpec + "' (uuid:<uuid> or faction:<id>)");
        }
        Army.OwnerType type = (Army.OwnerType) owner[0];
        String id = (String) owner[1];
        String ownerName = "";
        if (type == Army.OwnerType.FACTION) {
            Faction f = FactionController.instance.getFaction(Integer.parseInt(id));
            if (f == null) {
                return new Reply(false, "unknown faction " + id);
            }
            ownerName = f.name;
        } else if (server != null && server.getPlayerProfileCache() != null) {
            GameProfile p = server.getPlayerProfileCache().getProfileByUUID(UUID.fromString(id));
            ownerName = p == null || p.getName() == null ? "" : p.getName();
        }
        ArmyRegistry reg = registry(server);
        if (reg == null) {
            return new Reply(false, "no world loaded");
        }
        long now = server.getWorld(0).getTotalWorldTime();
        Army army = new Army(name, type, id, ownerName, now);
        ArmyRegistry.Result r = reg.create(army);
        if (r != ArmyRegistry.Result.CREATED) {
            return new Reply(false, "create " + name + " refused: " + r);
        }
        dirty(server);
        return new Reply(true, "created " + army);
    }

    public static Reply remove(MinecraftServer server, String name) {
        return remove(server, ArmyActor.SYSTEM, name);
    }

    /**
     * Removes the army and its selected grants and clears RwArmy on its loaded members; unloaded members keep the
     * name (orphan). Owner or operator only: a command grant does not include removing.
     */
    public static Reply remove(MinecraftServer server, ArmyActor actor, String name) {
        if (!enabled()) {
            return off();
        }
        ArmyRegistry reg = registry(server);
        Army army = reg == null ? null : reg.get(name);
        if (army == null || !reg.access(army, actor).canView()) {
            return unknown(name);
        }
        if (!actor.operator() && !actor.owns(army)) {
            return new Reply(false, "only the owner or an operator can remove " + army.name());
        }
        reg.remove(name);
        int cleared = 0;
        for (EntityNPCInterface npc : members(server, army.name())) {
            npc.setRwArmy("");
            cleared++;
        }
        dirty(server);
        return new Reply(true, "removed " + army.name() + " cleared=" + cleared);
    }

    public static Reply assign(MinecraftServer server, List<EntityNPCInterface> npcs, String army) {
        return assign(server, ArmyActor.SYSTEM, npcs, army);
    }

    /**
     * Assigns the NPCs to the army (stored with its registered spelling); {@code army} "" unassigns. Needs command
     * over the army. An NPC is taken from its present army only with command over that one too, and an NPC without
     * an army only by an operator (Q310), so a grant cannot capture shopkeepers or other players' NPCs. Refused NPCs
     * stay unchanged and are counted.
     */
    public static Reply assign(MinecraftServer server, ArmyActor actor, List<EntityNPCInterface> npcs, String army) {
        if (!enabled()) {
            return off();
        }
        ArmyRegistry reg = registry(server);
        String stored = "";
        if (army != null && !army.isEmpty()) {
            Army a = reg == null ? null : reg.get(army);
            if (a == null || !reg.access(a, actor).canView()) {
                return unknown(army);
            }
            if (!reg.access(a, actor).canCommand()) {
                return new Reply(false, "no command permission for " + a.name());
            }
            stored = a.name();
        }
        int changed = 0;
        int refused = 0;
        for (EntityNPCInterface npc : npcs) {
            if (npc.world.isRemote || npc.isDead) {
                continue;
            }
            if (stored.equals(npc.getRwArmy())) {
                continue;
            }
            if (!mayTake(reg, actor, npc.getRwArmy())) {
                refused++;
                continue;
            }
            npc.setRwArmy(stored);
            changed++;
        }
        return new Reply(true, (stored.isEmpty() ? "unassigned" : "assigned " + stored) + " npcs=" + npcs.size()
                + " changed=" + changed + (refused > 0 ? " refused=" + refused : ""));
    }

    /** An orphan name (army gone) counts as no army. */
    private static boolean mayTake(ArmyRegistry reg, ArmyActor actor, String present) {
        if (actor.operator()) {
            return true;
        }
        Army from = reg == null || present.isEmpty() ? null : reg.get(present);
        return from != null && reg.access(from, actor).canCommand();
    }

    /** The trusted caller of {@link ArmyApi}. */
    public static Army get(MinecraftServer server, String name) {
        return get(server, ArmyActor.SYSTEM, name);
    }

    /** The army with this name when the actor may see it, otherwise null. */
    public static Army get(MinecraftServer server, ArmyActor actor, String name) {
        if (!enabled()) {
            return null;
        }
        ArmyRegistry reg = registry(server);
        Army army = reg == null ? null : reg.get(name);
        return army != null && reg.access(army, actor).canView() ? army : null;
    }

    public static List<Army> list(MinecraftServer server) {
        return list(server, ArmyActor.SYSTEM);
    }

    /** The armies this actor may see; M11.2 builds its snapshot from the same list. */
    public static List<Army> list(MinecraftServer server, ArmyActor actor) {
        if (!enabled()) {
            return new ArrayList<Army>();
        }
        ArmyRegistry reg = registry(server);
        return reg == null ? new ArrayList<Army>() : reg.visible(actor);
    }

    /** Effective right of the actor over a named army at this moment; NONE also for an unknown army. */
    public static ArmyGrants.Access access(MinecraftServer server, ArmyActor actor, String name) {
        if (!enabled()) {
            return ArmyGrants.Access.NONE;
        }
        ArmyRegistry reg = registry(server);
        Army army = reg == null ? null : reg.get(name);
        return army == null ? ArmyGrants.Access.NONE : reg.access(army, actor);
    }

    /**
     * Actor for a player who need not be online, with the same operator rule as vanilla: the entry level of the
     * operator list, otherwise the server's default level. The online player's own check covers singleplayer.
     */
    public static ArmyActor actorFor(MinecraftServer server, UUID player) {
        PlayerList players = server == null ? null : server.getPlayerList();
        if (players == null || player == null) {
            return ArmyActor.player(player, false);
        }
        EntityPlayerMP online = players.getPlayerByUUID(player);
        if (online != null) {
            return ArmyActor.player(player, online.canUseCommand(OPERATOR_LEVEL, COMMAND_NAME));
        }
        GameProfile profile = new GameProfile(player, null);
        if (!players.canSendCommands(profile)) {
            return ArmyActor.player(player, false);
        }
        UserListOpsEntry entry = players.getOppedPlayers().getEntry(profile);
        int level = entry != null ? entry.getPermissionLevel() : server.getOpPermissionLevel();
        return ArmyActor.player(player, level >= OPERATOR_LEVEL);
    }

    /**
     * A player by {@code uuid:<uuid>}, online name or a name the server has already seen (usercache, any case).
     * Null when unknown: a name the server has never seen is not looked up at Mojang on the server thread.
     */
    public static UUID resolvePlayer(MinecraftServer server, String spec) {
        if (spec == null || spec.isEmpty()) {
            return null;
        }
        if (spec.indexOf(':') >= 0) {
            Object[] parsed = ArmyRegistry.parseOwnerSpec(spec);
            return parsed == null || parsed[0] != Army.OwnerType.PLAYER ? null : UUID.fromString((String) parsed[1]);
        }
        if (server == null) {
            return null;
        }
        EntityPlayerMP online = server.getPlayerList().getPlayerByUsername(spec);
        if (online != null) {
            return online.getUniqueID();
        }
        PlayerProfileCache cache = server.getPlayerProfileCache();
        for (String seen : cache.getUsernames()) {
            if (seen.equalsIgnoreCase(spec)) {
                GameProfile profile = cache.getGameProfileForUsername(seen);
                return profile == null ? null : profile.getId();
            }
        }
        return null;
    }

    /** Name for messages: online player, then usercache, otherwise the UUID; a rename never breaks a grant. */
    public static String playerName(MinecraftServer server, String uuid) {
        if (server == null) {
            return uuid;
        }
        UUID id = UUID.fromString(uuid);
        EntityPlayerMP online = server.getPlayerList().getPlayerByUUID(id);
        if (online != null) {
            return online.getName();
        }
        GameProfile profile = server.getPlayerProfileCache().getProfileByUUID(id);
        return profile == null || profile.getName() == null ? uuid : profile.getName();
    }

    /**
     * Candidates for a grant, online and seen players, sorted without case; for operators and army owners. Seen
     * names come in lower case: asking the cache for each profile would rewrite usercache.json per name.
     */
    public static List<String> candidates(MinecraftServer server, ArmyActor actor, String prefix) {
        List<String> out = new ArrayList<String>();
        if (!mayGrant(server, actor)) {
            return out;
        }
        Set<String> names = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        names.addAll(Arrays.asList(server.getOnlinePlayerNames()));
        names.addAll(Arrays.asList(server.getPlayerProfileCache().getUsernames()));
        String start = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        for (String name : names) {
            if (name.toLowerCase(Locale.ROOT).startsWith(start) && out.size() < MAX_LINES) {
                out.add(name);
            }
        }
        return out;
    }

    private static boolean mayGrant(MinecraftServer server, ArmyActor actor) {
        if (!enabled() || server == null) {
            return false;
        }
        ArmyRegistry reg = registry(server);
        return reg != null && (actor.operator() || reg.ownsAny(actor));
    }

    /** VIEW or COMMAND from command text, otherwise null. */
    private static ArmyGrants.Access parseKind(String kind) {
        return "view".equals(kind) ? ArmyGrants.Access.VIEW : "command".equals(kind) ? ArmyGrants.Access.COMMAND : null;
    }

    private static String kindText(ArmyGrants.Access kind) {
        return kind == ArmyGrants.Access.COMMAND ? "command" : "view";
    }

    /**
     * Owner for own armies, operator for all, no chain (research 3.5a). A named army goes to the owner's record when
     * the owner grants it, otherwise to the server record; {@code all} is the server record for an operator (every
     * army) and the owner's own record otherwise (the owner's armies, also later ones).
     *
     * @param kind {@code view} or {@code command}
     * @param army an army name or {@code all}
     */
    public static Reply grant(MinecraftServer server, ArmyActor actor, String player, String kind, String army) {
        if (!enabled()) {
            return off();
        }
        ArmyGrants.Access access = parseKind(kind);
        if (access == null) {
            return new Reply(false, "invalid right '" + kind + "' (view or command)");
        }
        ArmyRegistry reg = registry(server);
        if (reg == null) {
            return new Reply(false, "no world loaded");
        }
        UUID grantee = resolvePlayer(server, player);
        if (grantee == null) {
            return new Reply(false, "unknown player " + player);
        }
        if (grantee.equals(actor.player())) {
            return new Reply(false, "cannot grant to yourself");
        }
        String grantor;
        String key = null;
        if (isAll(army)) {
            if (actor.operator()) {
                grantor = ArmyGrants.SERVER;
            } else if (reg.ownsAny(actor)) {
                grantor = actor.player().toString();
            } else {
                return new Reply(false, "only the owner or an operator can grant");
            }
        } else {
            Army a = reg.get(army);
            if (a == null || !reg.access(a, actor).canView()) {
                return unknown(army);
            }
            if (actor.owns(a)) {
                grantor = actor.player().toString();
            } else if (actor.operator()) {
                grantor = ArmyGrants.SERVER;
            } else {
                return new Reply(false, "only the owner or an operator can grant " + a.name());
            }
            key = a.key();
        }
        ArmyGrants.Result r = reg.grants().grant(grantor, grantee.toString(), access, key);
        if (r == ArmyGrants.Result.FULL) {
            return new Reply(false, "grant refused: FULL");
        }
        if (r == ArmyGrants.Result.GRANTED) {
            dirty(server);
        }
        return new Reply(true, (r == ArmyGrants.Result.GRANTED ? "granted " : "already granted ") + kindText(access)
                + " " + army + " to " + playerName(server, grantee.toString()) + " scope="
                + (ArmyGrants.SERVER.equals(grantor) ? "server" : "owner"));
    }

    /**
     * A player clears their own record, an operator every record of the player. Applies to the next call; the
     * reply names what is left, because the grant of another grantor can still give the right.
     *
     * @param kind {@code view}, {@code command} or {@code all} (both)
     * @param army an army name or {@code all}
     */
    public static Reply revoke(MinecraftServer server, ArmyActor actor, String player, String kind, String army) {
        if (!enabled()) {
            return off();
        }
        ArmyGrants.Access access = parseKind(kind);
        if (access == null && !ALL.equals(kind)) {
            return new Reply(false, "invalid right '" + kind + "' (view, command or all)");
        }
        ArmyRegistry reg = registry(server);
        if (reg == null) {
            return new Reply(false, "no world loaded");
        }
        UUID grantee = resolvePlayer(server, player);
        if (grantee == null) {
            return new Reply(false, "unknown player " + player);
        }
        if (!actor.operator() && (actor.player() == null || !reg.ownsAny(actor))) {
            return new Reply(false, "only the owner or an operator can revoke");
        }
        Army named = null;
        if (!isAll(army)) {
            named = reg.get(army);
            if (named == null || !reg.access(named, actor).canView()) {
                return unknown(army);
            }
        }
        String grantor = actor.operator() ? null : actor.player().toString();
        int changed = reg.grants().revoke(grantor, grantee.toString(), access, named == null ? null : named.key());
        if (changed > 0) {
            dirty(server);
        }
        String left = named == null ? "" : " left="
                + reg.access(named, ArmyActor.player(grantee, false)).name().toLowerCase(Locale.ROOT);
        return new Reply(true, "revoked " + kind + " " + army + " from " + playerName(server, grantee.toString())
                + " changed=" + changed + left);
    }

    /** Grant lines: everything for an operator, otherwise what the player gave or received. Bounded. */
    public static List<String> describeGrants(MinecraftServer server, ArmyActor actor, String player) {
        List<String> out = new ArrayList<String>();
        if (!enabled()) {
            out.add(off().message);
            return out;
        }
        ArmyRegistry reg = registry(server);
        UUID filter = player == null ? null : resolvePlayer(server, player);
        if (reg == null || (player != null && filter == null)) {
            out.add("RWARMY unknown player " + player);
            return out;
        }
        String self = actor.player() == null ? null : actor.player().toString();
        List<String> lines = new ArrayList<String>();
        for (ArmyGrants.Grant g : reg.grants().list()) {
            boolean mine = actor.operator() || g.grantor().equals(self) || g.grantee().equals(self);
            if (mine && (filter == null || g.grantee().equals(filter.toString()))) {
                lines.add("RWARMY grant " + playerName(server, g.grantee()) + " by "
                        + (ArmyGrants.SERVER.equals(g.grantor()) ? "server" : playerName(server, g.grantor()))
                        + " view=" + g.describe(ArmyGrants.Access.VIEW)
                        + " command=" + g.describe(ArmyGrants.Access.COMMAND));
            }
        }
        out.add("RWARMY grants count=" + lines.size());
        out.addAll(lines.subList(0, Math.min(lines.size(), MAX_LINES)));
        return out;
    }

    /** Loaded, living server NPCs whose RwArmy names this army (without case), in every dimension. */
    public static List<EntityNPCInterface> members(MinecraftServer server, String name) {
        List<EntityNPCInterface> out = new ArrayList<EntityNPCInterface>();
        if (!enabled() || server == null || name == null || name.isEmpty()) {
            return out;
        }
        String key = Army.key(name);
        for (WorldServer world : server.worlds) {
            for (Entity e : world.loadedEntityList) {
                if (e instanceof EntityNPCInterface && !e.isDead
                        && key.equals(Army.key(((EntityNPCInterface) e).getRwArmy()))) {
                    out.add((EntityNPCInterface) e);
                }
            }
        }
        return out;
    }

    /**
     * M11.2: what this actor may see right now, on request only; null with RwArmies=0 or without a world. One pass
     * over the loaded entities. Also keeps the last known positions, the only thing a request writes.
     */
    public static ArmySnapshot snapshot(MinecraftServer server, ArmyActor actor, int requestId) {
        if (!enabled()) {
            return null;
        }
        ArmyRegistry reg = registry(server);
        if (reg == null) {
            return null;
        }
        Map<String, List<ArmyClusters.Point>> byKey = new HashMap<String, List<ArmyClusters.Point>>();
        for (WorldServer world : server.worlds) {
            int dimension = world.provider.getDimension();
            for (Entity e : world.loadedEntityList) {
                if (e instanceof EntityNPCInterface && !e.isDead) {
                    String name = ((EntityNPCInterface) e).getRwArmy();
                    if (!name.isEmpty()) {
                        String key = Army.key(name);
                        List<ArmyClusters.Point> points = byKey.get(key);
                        if (points == null) {
                            byKey.put(key, points = new ArrayList<ArmyClusters.Point>());
                        }
                        points.add(new ArmyClusters.Point(dimension, e.posX, e.posY, e.posZ));
                    }
                }
            }
        }
        ArmySnapshot snapshot = ArmySnapshot.build(requestId, server.getWorld(0).getTotalWorldTime(), clusterRadius,
                clusterMin, reg, actor, byKey);
        if (snapshot.lastMoved) {
            dirty(server);
        }
        return snapshot;
    }

    /** Lines of {@code /rwarmy clusters}: the snapshot of this actor, one army when named. Bounded. */
    public static List<String> describeClusters(MinecraftServer server, ArmyActor actor, String army) {
        List<String> out = new ArrayList<String>();
        ArmySnapshot snapshot = snapshot(server, actor, 0);
        if (snapshot == null) {
            out.add(off().message);
            return out;
        }
        out.add("RWARMY clusters armies=" + snapshot.armies.size() + " radius=" + snapshot.radius + " min="
                + snapshot.minMembers + " " + ArmyNet.counters());
        for (ArmySnapshot.Entry e : snapshot.armies) {
            if (army != null && !e.name.equalsIgnoreCase(army)) {
                continue;
            }
            out.add("RWARMY clusterarmy " + e.name + " access=" + (e.command ? "command" : "view") + " loaded="
                    + e.loaded + " clusters=" + e.clusters.size() + " omitted=" + e.omittedClusters + " last="
                    + (e.hasLast() ? e.lastDimension + "," + e.lastX + "," + e.lastY + "," + e.lastZ + " seen="
                            + e.lastSeen : "none"));
            for (ArmyClusters.Cluster c : e.clusters) {
                out.add("RWARMY cluster " + e.name + " " + c.kind.id() + " dim=" + c.dimension + " at " + c.x + " "
                        + c.y + " " + c.z + " size=" + c.size);
            }
        }
        return out.subList(0, Math.min(out.size(), MAX_LINES));
    }

    /** One line per visible army with its owner, the number of loaded members and, below operator, the right. */
    public static List<String> describe(MinecraftServer server, ArmyActor actor) {
        List<String> out = new ArrayList<String>();
        if (!enabled()) {
            out.add(off().message);
            return out;
        }
        List<Army> armies = list(server, actor);
        out.add("RWARMY list armies=" + armies.size());
        for (Army a : armies) {
            String right = actor.operator() ? ""
                    : " access=" + access(server, actor, a.name()).name().toLowerCase(Locale.ROOT);
            out.add("RWARMY army " + a + " loaded=" + members(server, a.name()).size() + right);
        }
        return out;
    }

    private static boolean isAll(String army) {
        return ALL.equalsIgnoreCase(army);
    }

    private static Reply unknown(String name) {
        return new Reply(false, "unknown army " + name);
    }
}
