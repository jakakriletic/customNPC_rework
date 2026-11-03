package noppes.npcs.rework.army;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * M11.1 (Q297, Q300): the army register without game state, so its NBT round trip is unit tested.
 * {@link ArmyWorldData} saves it with the world. Later packages add the stored army orders here (Q300).
 */
public final class ArmyRegistry {
    public static final int VERSION = 1;
    /** Upper bound that keeps the saved file and the list command small. */
    public static final int MAX_ARMIES = 1024;

    public enum Result { CREATED, INVALID_NAME, EXISTS, FULL }

    private final Map<String, Army> armies = new LinkedHashMap<String, Army>();
    private int skippedOnRead;

    public Result create(Army army) {
        if (army == null) {
            return Result.INVALID_NAME;
        }
        if (armies.containsKey(army.key())) {
            return Result.EXISTS;
        }
        if (armies.size() >= MAX_ARMIES) {
            return Result.FULL;
        }
        armies.put(army.key(), army);
        return Result.CREATED;
    }

    /** The army with this name (any case) or null. */
    public Army get(String name) {
        return armies.get(Army.key(name));
    }

    public Army remove(String name) {
        return armies.remove(Army.key(name));
    }

    public int size() {
        return armies.size();
    }

    /** Armies sorted by name without case. */
    public List<Army> list() {
        List<Army> out = new ArrayList<Army>(armies.values());
        Collections.sort(out, new Comparator<Army>() {
            @Override
            public int compare(Army a, Army b) {
                return a.key().compareTo(b.key());
            }
        });
        return out;
    }

    /** Entries the last {@link #read} left out because they were invalid or duplicate. */
    public int skippedOnRead() {
        return skippedOnRead;
    }

    public NBTTagCompound write(NBTTagCompound c) {
        c.setInteger("Version", VERSION);
        NBTTagList list = new NBTTagList();
        for (Army a : list()) {
            list.appendTag(a.write());
        }
        c.setTag("Armies", list);
        return c;
    }

    /** Replaces the content. Invalid and duplicate entries are skipped and counted, never thrown. */
    public void read(NBTTagCompound c) {
        armies.clear();
        skippedOnRead = 0;
        NBTTagList list = c.getTagList("Armies", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            Army a = Army.read(list.getCompoundTagAt(i));
            if (a == null || create(a) != Result.CREATED) {
                skippedOnRead++;
            }
        }
    }

    /**
     * Owner text of commands and scripts: {@code uuid:<uuid>} or {@code faction:<id>}. Returns
     * {type, canonical owner} or null.
     */
    public static Object[] parseOwnerSpec(String spec) {
        if (spec == null) {
            return null;
        }
        int at = spec.indexOf(':');
        if (at <= 0) {
            return null;
        }
        String prefix = spec.substring(0, at);
        Army.OwnerType type = prefix.equals("uuid") ? Army.OwnerType.PLAYER
                : prefix.equals("faction") ? Army.OwnerType.FACTION : null;
        String owner = Army.canonicalOwner(type, spec.substring(at + 1));
        return owner == null ? null : new Object[] {type, owner};
    }

    /** RwArmy of an NPC compound: the stored string, or "" when the key is missing or not a string. */
    public static String readNpc(NBTTagCompound compound) {
        return compound.hasKey(Army.NPC_KEY, 8) ? compound.getString(Army.NPC_KEY) : "";
    }

    /** An NPC without an army keeps the original NBT shape (no key), also when reusing a compound. */
    public static void writeNpc(NBTTagCompound compound, String army) {
        if (army == null || army.isEmpty()) {
            compound.removeTag(Army.NPC_KEY);
        } else {
            compound.setString(Army.NPC_KEY, army);
        }
    }
}
