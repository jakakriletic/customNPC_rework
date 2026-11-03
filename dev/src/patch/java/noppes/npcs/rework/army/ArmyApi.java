package noppes.npcs.rework.army;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.MinecraftServer;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * M11.1: script entry for armies (JS through {@code Java.type}, Java scripts by import). Server side only;
 * with RwArmies=0 every call returns false, "" or an empty array and changes nothing.
 *
 * <pre>
 * var A = Java.type("noppes.npcs.rework.army.ArmyApi");
 * A.create("north", "uuid:" + event.player.getUUID());   // or "faction:2"
 * A.assign(event.npc, "north");
 * var members = A.members("north");                        // loaded NPCs of the army
 * </pre>
 *
 * Names are 1-32 letters, digits, '_' or '-' and compared without case. M11.1 has no permission check: the
 * API is for trusted script authors (M11.1b adds view/command grants).
 */
public final class ArmyApi {
    private ArmyApi() {
    }

    /** Provisional version 0; frozen as 1 in M11.7a. */
    public static int getApiVersion() {
        return ArmyCommandService.API_VERSION;
    }

    public static boolean enabled() {
        return ArmyCommandService.enabled();
    }

    /** @param owner {@code uuid:<player uuid>} or {@code faction:<faction id>} */
    public static boolean create(String name, String owner) {
        return ArmyCommandService.create(server(), name, owner).ok;
    }

    /** Removes the army; its loaded members lose RwArmy. */
    public static boolean remove(String name) {
        return ArmyCommandService.remove(server(), name).ok;
    }

    /** Assigns an NPC to an existing army; false for a non-NPC, an unknown army or RwArmies=0. */
    public static boolean assign(IEntity<?> npc, String army) {
        EntityNPCInterface e = unwrap(npc);
        if (e == null || army == null || army.isEmpty()) {
            return false;
        }
        List<EntityNPCInterface> one = new ArrayList<EntityNPCInterface>();
        one.add(e);
        return ArmyCommandService.assign(server(), one, army).ok;
    }

    public static boolean unassign(IEntity<?> npc) {
        EntityNPCInterface e = unwrap(npc);
        if (e == null) {
            return false;
        }
        List<EntityNPCInterface> one = new ArrayList<EntityNPCInterface>();
        one.add(e);
        return ArmyCommandService.assign(server(), one, "").ok;
    }

    /** Army name of the NPC, "" without one (also with RwArmies=0). */
    public static String armyOf(IEntity<?> npc) {
        EntityNPCInterface e = unwrap(npc);
        return e == null || !enabled() ? "" : e.getRwArmy();
    }

    /** Registered names, sorted. */
    public static String[] list() {
        List<String> out = new ArrayList<String>();
        for (Army a : ArmyCommandService.list(server())) {
            out.add(a.name());
        }
        return out.toArray(new String[0]);
    }

    public static boolean exists(String name) {
        return ArmyCommandService.get(server(), name) != null;
    }

    /** {@code uuid:<uuid>} or {@code faction:<id>}, "" for an unknown army. */
    public static String owner(String name) {
        Army a = ArmyCommandService.get(server(), name);
        return a == null ? "" : a.ownerSpec();
    }

    /** Loaded, living members in every dimension. */
    public static IEntity<?>[] members(String name) {
        List<EntityNPCInterface> list = ArmyCommandService.members(server(), name);
        IEntity<?>[] out = new IEntity<?>[list.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = NpcAPI.Instance().getIEntity(list.get(i));
        }
        return out;
    }

    /** Reads CustomNpcs.Server only with the switch on, so RwArmies=0 never initializes CustomNpcs here. */
    private static MinecraftServer server() {
        return enabled() ? CustomNpcs.Server : null;
    }

    private static EntityNPCInterface unwrap(IEntity<?> entity) {
        Object e = entity == null ? null : entity.getMCEntity();
        return e instanceof EntityNPCInterface ? (EntityNPCInterface) e : null;
    }
}
