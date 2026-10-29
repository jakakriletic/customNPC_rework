package noppes.npcs.rework.movement;

import net.minecraft.nbt.NBTTagCompound;

/**
 * M4.2: persistent flight selection, independent of DataAI.movementType.
 * IDs are a save-format contract, never enum ordinals. HOVER and MOMENTUM are
 * selections only until their helpers are implemented in M4.3 and M4.6.
 */
public enum FlightMode {
    ORIGINAL(0),
    HOVER(1),
    MOMENTUM(2);

    public static final String KEY = "RwFlightMode";
    private final int id;

    FlightMode(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static FlightMode fromId(int id) {
        if (id == 1) return HOVER;
        if (id == 2) return MOMENTUM;
        return ORIGINAL;
    }

    /** Missing, unknown or wrongly typed values never opt an old NPC into new movement. */
    public static FlightMode read(NBTTagCompound compound) {
        return compound.hasKey(KEY, 3) ? fromId(compound.getInteger(KEY)) : ORIGINAL;
    }

    /** Default NPCs retain the original NBT shape, including when reusing a compound. */
    public void write(NBTTagCompound compound) {
        if (this == ORIGINAL) {
            compound.removeTag(KEY);
        } else {
            compound.setInteger(KEY, id);
        }
    }
}
