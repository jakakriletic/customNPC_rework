package noppes.npcs.rework.nav;

import net.minecraft.nbt.NBTTagCompound;

/** Transient client editor state, independent of the client entity and its local config. */
public final class NavGuiState {
    private boolean ready;
    private int choice;
    private int savedChoice;
    private int actual;
    private String reason = "";

    public void load(NBTTagCompound data) {
        boolean keepEdit = pending();
        ready = false;
        if (!NavGuiData.validChoice(data) || !data.hasKey(NavGuiData.STATUS, 10)) { return; }
        NBTTagCompound status = data.getCompoundTag(NavGuiData.STATUS);
        if (!status.hasKey("Actual", 3) || !status.hasKey("Reason", 8)) { return; }
        int nextActual = status.getInteger("Actual");
        if (nextActual != 0 && nextActual != 1) { return; }
        actual = nextActual;
        reason = status.getString("Reason");
        savedChoice = data.getInteger(NavGuiData.CHOICE);
        if (!keepEdit) { choice = savedChoice; }
        ready = true;
    }

    public boolean ready() { return ready; }
    public int choice() { return choice; }
    public boolean pending() { return ready && choice != savedChoice; }
    public void choose(int value) { if (ready && (value == 0 || value == 1)) { choice = value; } }

    public void writeChoice(NBTTagCompound data) {
        if (ready) { data.setInteger(NavGuiData.CHOICE, choice); }
    }

    public String actualName() { return !ready ? "Unknown" : actual == 1 ? "Baritone" : "Vanilla"; }

    public String reasonText() {
        if (!ready) { return "No server status"; }
        switch (reason) {
            case "active": return "Active";
            case "selected_vanilla": return "Selected vanilla";
            case "global_off": return "Global setting disabled";
            case "missing_mod": return "npcbaritone missing";
            case "movement": return "Flying/swimming unsupported";
            case "riding": return "Riding another entity";
            case "carrying": return "Carrying passengers";
            case "squad": return "Formation member (U7)";
            case "killed": return "NPC killed";
            case "unavailable": return "Library unavailable";
            case "unsupported": return "Entity/size unsupported";
            case "incompatible": return "Library API incompatible";
            case "not_attached": return "Navigator not attached";
            case "not_server": return "Server state unavailable";
            default: return "Unknown server reason";
        }
    }
}
