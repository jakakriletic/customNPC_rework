package noppes.npcs.rework.movement;

import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * P6: flight mode in the AI movement editor. Wire extension of the existing MainmenuAIGet/Save
 * packets only (no new packet IDs, no DataAI fields). The server reply carries the stored mode;
 * the client sends a choice only when the user changed it, so legacy clients and untouched
 * editors never change the mode. MOMENTUM (v2) is shown but not selectable.
 */
public final class FlightGui {
    public static final String CHOICE = "RwFlightGui";
    /** Short labels: the button shares the first editor row (texture is 216 px high). */
    private static final String[] LABELS = {"Fly: Orig", "Fly: Hover"};
    private static final String MOMENTUM_LABEL = "Fly: Mom.";

    private boolean ready;
    private int saved;
    private int choice;

    /** Server thread: stored mode for the editor. */
    public static void writeReply(NBTTagCompound data, EntityNPCInterface npc) {
        data.setInteger(CHOICE, npc.getRwFlightMode().id());
    }

    /** Only an explicit integer ORIGINAL or HOVER may change the mode. */
    public static void applySelection(EntityNPCInterface npc, NBTTagCompound data) {
        if (validRequest(data)) {
            npc.setRwFlightMode(data.getInteger(CHOICE));
        }
    }

    static boolean validRequest(NBTTagCompound data) {
        if (!data.hasKey(CHOICE, 3)) {
            return false;
        }
        int value = data.getInteger(CHOICE);
        return value == FlightMode.ORIGINAL.id() || value == FlightMode.HOVER.id();
    }

    public void load(NBTTagCompound data) {
        boolean keepEdit = pending();
        ready = false;
        if (!data.hasKey(CHOICE, 3)) {
            return;
        }
        saved = FlightMode.fromId(data.getInteger(CHOICE)).id();
        if (!keepEdit) {
            choice = saved;
        }
        ready = true;
    }

    public boolean ready() {
        return ready;
    }

    /** A stored MOMENTUM cannot be represented by the button; it stays untouched. */
    public boolean selectable() {
        return ready && saved != FlightMode.MOMENTUM.id();
    }

    public int choice() {
        return choice;
    }

    public boolean pending() {
        return ready && choice != saved;
    }

    public void choose(int value) {
        if (selectable() && (value == FlightMode.ORIGINAL.id() || value == FlightMode.HOVER.id())) {
            choice = value;
        }
    }

    public void writeChoice(NBTTagCompound data) {
        if (pending()) {
            data.setInteger(CHOICE, choice);
        }
    }

    /** Button labels; index = mode id. */
    public String[] labels() {
        return selectable() ? LABELS.clone() : new String[] {ready ? MOMENTUM_LABEL : "Fly: ?"};
    }

    public int buttonValue() {
        return selectable() ? choice : 0;
    }
}
