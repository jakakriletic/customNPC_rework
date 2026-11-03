package noppes.npcs.rework.army;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * M11.1 (Q296, Q297, Q299): server side of armies, shared by {@code /rwarmy} and {@link ArmyApi}. Everything
 * returns a {@link Reply} and changes nothing while RwArmies=0. Provisional API version 0; M11.7a freezes the
 * service for external bridges as version 1. M11.1 has no permissions yet: the command is for operators and
 * the API for trusted script authors (M11.1b adds view/command grants).
 */
public final class ArmyCommandService {
    public static final int API_VERSION = 0;

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

    private ArmyCommandService() {
    }

    /** Called from CustomNpcs at server start (and by unit tests). */
    public static void setSwitch(int value) {
        armies = value;
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

    /** @param ownerSpec {@code uuid:<uuid>} or {@code faction:<id>}; the faction must exist. */
    public static Reply create(MinecraftServer server, String name, String ownerSpec) {
        if (!enabled()) {
            return off();
        }
        if (!Army.validName(name)) {
            return new Reply(false, "invalid name '" + name + "' (1-32 letters, digits, _ or -)");
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

    /** Removes the army and clears RwArmy on its loaded members; unloaded members keep the name (orphan). */
    public static Reply remove(MinecraftServer server, String name) {
        if (!enabled()) {
            return off();
        }
        ArmyRegistry reg = registry(server);
        Army army = reg == null ? null : reg.remove(name);
        if (army == null) {
            return new Reply(false, "unknown army " + name);
        }
        int cleared = 0;
        for (EntityNPCInterface npc : members(server, army.name())) {
            npc.setRwArmy("");
            cleared++;
        }
        dirty(server);
        return new Reply(true, "removed " + army.name() + " cleared=" + cleared);
    }

    /** Assigns the NPCs to the army (stored with its registered spelling); {@code army} "" unassigns. */
    public static Reply assign(MinecraftServer server, List<EntityNPCInterface> npcs, String army) {
        if (!enabled()) {
            return off();
        }
        String stored = "";
        if (army != null && !army.isEmpty()) {
            ArmyRegistry reg = registry(server);
            Army a = reg == null ? null : reg.get(army);
            if (a == null) {
                return new Reply(false, "unknown army " + army);
            }
            stored = a.name();
        }
        int changed = 0;
        for (EntityNPCInterface npc : npcs) {
            if (npc.world.isRemote || npc.isDead) {
                continue;
            }
            if (!stored.equals(npc.getRwArmy())) {
                npc.setRwArmy(stored);
                changed++;
            }
        }
        return new Reply(true, (stored.isEmpty() ? "unassigned" : "assigned " + stored) + " npcs=" + npcs.size()
                + " changed=" + changed);
    }

    public static Army get(MinecraftServer server, String name) {
        if (!enabled()) {
            return null;
        }
        ArmyRegistry reg = registry(server);
        return reg == null ? null : reg.get(name);
    }

    public static List<Army> list(MinecraftServer server) {
        if (!enabled()) {
            return new ArrayList<Army>();
        }
        ArmyRegistry reg = registry(server);
        return reg == null ? new ArrayList<Army>() : reg.list();
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

    /** One line per army with its owner and the number of loaded members. */
    public static List<String> describe(MinecraftServer server) {
        List<String> out = new ArrayList<String>();
        if (!enabled()) {
            out.add(off().message);
            return out;
        }
        List<Army> armies = list(server);
        out.add("RWARMY list armies=" + armies.size());
        for (Army a : armies) {
            out.add("RWARMY army " + a + " loaded=" + members(server, a.name()).size());
        }
        return out;
    }
}
