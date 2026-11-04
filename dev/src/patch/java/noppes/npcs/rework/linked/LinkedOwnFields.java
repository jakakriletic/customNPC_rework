package noppes.npcs.rework.linked;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

/**
 * M13.1 (Q380): fields a linked NPC keeps as its own when its group data is merged over it.
 *
 * <p>The original {@code LinkedNpcController.loadNpcData} merges the group's data (written by the
 * NPC edited last) over every member, so all members took that NPC's orientation and current path
 * index. Its path itself was already kept, and {@code DataAI.setMovingPath} then moved the home of a
 * member with a path to its own first path point, so that member lost its own home (Q384, Q388). With
 * {@code RwLinkedOwnFields=1} these keys are restored from the member's own data after the merge,
 * and the own home is set again after {@code setMovingPath}.
 * World position, rotation, {@code RwArmy}, {@code RwNavBackend} and {@code RwFlightMode} are
 * entity tags and never part of group data, so they stay the member's in both modes.
 */
public final class LinkedOwnFields {
    /** Home, orientation, path and the member's place on that path. */
    public static final String[] KEYS = {"StartPosNew", "Orientation", "MovingPathNew", "MovingPos"};

    private LinkedOwnFields() {
    }

    /** Puts the member's own value of every {@link #KEYS} key back into the merged compound. */
    public static void restore(NBTTagCompound own, NBTTagCompound merged) {
        for (String key : KEYS) {
            if (own.hasKey(key)) {
                merged.setTag(key, own.getTag(key).copy());
            } else {
                merged.removeTag(key);
            }
        }
    }

    /** True when the two versions of group data differ at most in {@link #KEYS}. */
    public static boolean sameShared(NBTTagCompound a, NBTTagCompound b) {
        NBTTagCompound left = a.copy();
        NBTTagCompound right = b.copy();
        for (String key : KEYS) {
            left.removeTag(key);
            right.removeTag(key);
        }
        return left.equals(right);
    }

    /** The member's own home from its data, or null when it has none. */
    public static BlockPos home(NBTTagCompound own) {
        if (!own.hasKey("StartPosNew", 11)) {
            return null;
        }
        int[] home = own.getIntArray("StartPosNew");
        return home.length < 3 ? null : new BlockPos(home[0], home[1], home[2]);
    }
}
