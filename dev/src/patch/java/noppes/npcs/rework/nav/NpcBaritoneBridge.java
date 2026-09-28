package noppes.npcs.rework.nav;

import noppes.npcs.entity.EntityNPCInterface;
import si.ladja.npcbaritone.api.DoorMode;
import si.ladja.npcbaritone.api.INpcNavigator;
import si.ladja.npcbaritone.api.NpcBaritone;
import si.ladja.npcbaritone.api.SpeedMode;

/** Only this class links to NPC Baritone's optional public API. Server thread only. */
final class NpcBaritoneBridge {
    private NpcBaritoneBridge() {
    }

    static boolean attached(EntityNPCInterface npc) {
        return NpcBaritone.available() && NpcBaritone.get(npc) != null;
    }

    static boolean attach(EntityNPCInterface npc) {
        if (!NpcBaritone.available() || NpcBaritone.apiVersion() < 2
                || !NpcBaritone.supports(npc)) {
            return false;
        }
        // The profile is applied first; instance overrides survive reinstall/setProfile.
        String profile = npc.ais.avoidsWater ? "avoid_water" : "default";
        INpcNavigator nav = NpcBaritone.attach(npc, profile);
        if (nav == null && npc.ais.avoidsWater) {
            nav = NpcBaritone.attach(npc, "default");
        }
        if (nav == null) {
            return false;
        }
        if (!profile.equals(nav.profile()) && !nav.setProfile(profile)) {
            // A server can override the named profile list; keep navigation usable.
            nav.setProfile("default");
        }
        if (nav.speedMode() != SpeedMode.OWN) {
            nav.setSpeedMode(SpeedMode.OWN);
        }
        DoorMode doors = npc.ais.doorInteract == 2 ? DoorMode.NONE : DoorMode.WOODEN;
        if (nav.doorMode() != doors) {
            nav.setDoorMode(doors);
        }
        return true;
    }

    static void detach(EntityNPCInterface npc) {
        if (NpcBaritone.available()) {
            NpcBaritone.detach(npc);
        }
    }
}
