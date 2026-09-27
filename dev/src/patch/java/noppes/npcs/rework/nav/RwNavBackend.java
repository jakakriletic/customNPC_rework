package noppes.npcs.rework.nav;

import net.minecraftforge.fml.common.Loader;
import noppes.npcs.CustomNpcs;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.formation.SquadManager;

/**
 * M7: opt-in selection of the NPC Baritone navigator. This class has no dependency on
 * the optional API, so an installation without npcbaritone follows the original path.
 */
public final class RwNavBackend {
    private RwNavBackend() {
    }

    public static boolean selected(int global, int npc, int movementType,
                                   boolean riding, boolean inSquad, boolean killed) {
        return global == 1 && npc == 1 && movementType == 0
                && !riding && !inSquad && !killed;
    }

    /** The system property is an explicit test override for scripted A/B runs. */
    public static int globalMode() {
        String override = System.getProperty("rwnavbackend");
        if ("1".equals(override)) {
            return 1;
        }
        if ("0".equals(override)) {
            return 0;
        }
        return CustomNpcs.RwNavBackend == 1 ? 1 : 0;
    }

    /** Called after updateTasks has installed all new vanilla AI tasks. */
    public static void afterTasks(EntityNPCInterface npc) {
        apply(npc, true);
    }

    /** Detect riding and squad membership changes before the entity's navigation tick. */
    public static void beforeTick(EntityNPCInterface npc) {
        if (npc.getRwNavBackend() == 0) {
            return;
        }
        apply(npc, false);
    }

    /** Called when the per-NPC switch changes. */
    public static void changed(EntityNPCInterface npc) {
        apply(npc, true);
    }

    private static void apply(EntityNPCInterface npc, boolean afterRebuild) {
        if (npc.world == null || npc.world.isRemote || !Loader.isModLoaded("npcbaritone")) {
            return;
        }
        try {
            boolean use = selected(globalMode(), npc.getRwNavBackend(),
                    npc.ais.movementType, npc.isRiding(), SquadManager.contains(npc), npc.isKilled());
            if (use) {
                if (afterRebuild || !NpcBaritoneBridge.attached(npc)) {
                    NpcBaritoneBridge.attach(npc);
                }
            } else if (afterRebuild || NpcBaritoneBridge.attached(npc)) {
                NpcBaritoneBridge.detach(npc);
            }
        } catch (LinkageError incompatibleApi) {
            // A missing or older optional API must never prevent CNPC from using vanilla.
        }
    }
}
