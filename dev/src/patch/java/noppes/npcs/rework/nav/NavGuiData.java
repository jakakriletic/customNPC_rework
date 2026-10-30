package noppes.npcs.rework.nav;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Loader;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.formation.SquadManager;
import noppes.npcs.rework.movement.FlightGui;

/** AI editor wire extension only. Does not add fields to DataAI or entity persistence. */
public final class NavGuiData {
    public static final String CHOICE = "RwNavBackend";
    public static final String STATUS = "RwNavGuiStatus";

    private NavGuiData() { }

    /** Server thread; reading a status must never install or detach a navigator. */
    public static NBTTagCompound reply(EntityNPCInterface npc) {
        NBTTagCompound data = npc.ais.writeToNBT(new NBTTagCompound());
        data.setInteger(CHOICE, npc.getRwNavBackend());
        String reason = reason(npc);
        NBTTagCompound status = new NBTTagCompound();
        status.setInteger("Actual", "active".equals(reason) ? 1 : 0);
        status.setString("Reason", reason);
        data.setTag(STATUS, status);
        FlightGui.writeReply(data, npc);
        return data;
    }

    /** Same AI-save effects as the original handler, plus an explicitly supplied choice. */
    public static void save(EntityNPCInterface npc, NBTTagCompound data) {
        npc.ais.readToNBT(data);
        applySelection(npc, data);
        FlightGui.applySelection(npc, data);
        npc.setHealth(npc.getMaxHealth());
        npc.updateAI = true;
        npc.updateClient = true;
    }

    /** Legacy clients omit the key. Invalid IDs/types and echoed status cannot change it. */
    public static void applySelection(EntityNPCInterface npc, NBTTagCompound data) {
        if (validChoice(data)) {
            npc.setRwNavBackend(data.getInteger(CHOICE));
        }
    }

    static boolean validChoice(NBTTagCompound data) {
        return data.hasKey(CHOICE, 3)
                && (data.getInteger(CHOICE) == 0 || data.getInteger(CHOICE) == 1);
    }

    private static String reason(EntityNPCInterface npc) {
        if (npc.world == null || npc.world.isRemote) { return "not_server"; }
        // Report a real attachment first, even during a pending task rebuild.
        if (RwNavBackend.attached(npc) && libraryNavigator(npc.getNavigator().getClass())) { return "active"; }
        if (npc.getRwNavBackend() == 0) { return "selected_vanilla"; }
        if (RwNavBackend.globalMode() == 0) { return "global_off"; }
        if (!Loader.isModLoaded("npcbaritone")) { return "missing_mod"; }
        if (npc.ais.movementType != 0) { return "movement"; }
        if (npc.isRiding()) { return "riding"; }
        if (npc.isBeingRidden()) { return "carrying"; }
        if (SquadManager.contains(npc)) { return "squad"; }
        if (npc.isKilled()) { return "killed"; }
        try {
            return NpcBaritoneBridge.fallbackReason(npc);
        } catch (LinkageError incompatibleApi) {
            return "incompatible";
        }
    }

    // The API's get() can retain a handle while CNPC replaces the navigator during a rebuild.
    // Check the actual runtime type without linking to the optional implementation class.
    private static boolean libraryNavigator(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if ("si.ladja.npcbaritone.forge.BaritonePathNavigate".equals(current.getName())) { return true; }
        }
        return false;
    }
}
