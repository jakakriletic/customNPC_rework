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
 * {@link ArmyWorldData} saves it with the world. M11.3 keeps durable orders here as well (Q300).
 */
public final class ArmyRegistry {
    public static final int VERSION = 1;
    /** Upper bound that keeps the saved file and the list command small. */
    public static final int MAX_ARMIES = 1024;

    public enum Result { CREATED, INVALID_NAME, EXISTS, FULL }

    private final Map<String, Army> armies = new LinkedHashMap<String, Army>();
    private final ArmyGrants grants = new ArmyGrants();
    private final Map<String, ArmyOrder> orders = new LinkedHashMap<String, ArmyOrder>();
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
        Army army = armies.remove(Army.key(name));
        if (army != null) {
            grants.removeArmy(army.key());
            orders.remove(army.key());
        }
        return army;
    }

    /**
     * M11.2: the main cluster of this army is here now. The position changes only after a move of at least
     * {@link ArmySnapshot#LAST_MOVE} blocks or to another dimension, so a standing army never makes the file dirty.
     *
     * @return true when the position changed and the register has to be saved
     */
    boolean noteSeen(String key, int dimension, int x, int y, int z, long time) {
        Army a = armies.get(key);
        if (a == null) {
            return false;
        }
        boolean moved = a.lastSeen() < 0 || a.lastDimension() != dimension
                || Math.abs(a.lastX() - x) >= ArmySnapshot.LAST_MOVE || Math.abs(a.lastY() - y) >= ArmySnapshot.LAST_MOVE
                || Math.abs(a.lastZ() - z) >= ArmySnapshot.LAST_MOVE;
        armies.put(key, moved ? a.withLast(dimension, x, y, z, time)
                : a.withLast(a.lastDimension(), a.lastX(), a.lastY(), a.lastZ(), time));
        return moved;
    }

    public ArmyGrants grants() {
        return grants;
    }

    public ArmyOrder order(String name) { return orders.get(Army.key(name)); }

    public boolean order(String name, ArmyOrder order) {
        if (get(name) == null || order == null) { return false; }
        orders.put(Army.key(name), order);
        return true;
    }

    /** M11.1b: operator and owner have every right, others what the grants say. Checked on every call. */
    public ArmyGrants.Access access(Army army, ArmyActor actor) {
        if (actor.operator() || actor.owns(army)) {
            return ArmyGrants.Access.COMMAND;
        }
        return grants.access(army, actor.player());
    }

    /** Armies this actor may see, sorted by name without case. */
    public List<Army> visible(ArmyActor actor) {
        List<Army> out = new ArrayList<Army>();
        for (Army a : list()) {
            if (access(a, actor).canView()) {
                out.add(a);
            }
        }
        return out;
    }

    /** True when the actor is a player who owns at least one army. */
    public boolean ownsAny(ArmyActor actor) {
        for (Army a : armies.values()) {
            if (actor.owns(a)) {
                return true;
            }
        }
        return false;
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
        NBTTagList savedOrders = new NBTTagList();
        for (Army a : list()) {
            ArmyOrder order = order(a.name());
            if (order != null) {
                NBTTagCompound tag = order.write();
                tag.setString("Army", a.key());
                savedOrders.appendTag(tag);
            }
        }
        if (savedOrders.tagCount() > 0) { c.setTag("Orders", savedOrders); }
        else { c.removeTag("Orders"); }
        if (grants.size() > 0) {
            c.setTag("Grants", grants.write());
        }
        return c;
    }

    /** Replaces the content. Invalid and duplicate entries are skipped and counted, never thrown. */
    public void read(NBTTagCompound c) {
        armies.clear();
        orders.clear();
        skippedOnRead = 0;
        NBTTagList list = c.getTagList("Armies", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            Army a = Army.read(list.getCompoundTagAt(i));
            if (a == null || create(a) != Result.CREATED) {
                skippedOnRead++;
            }
        }
        skippedOnRead += grants.read(c.getTagList("Grants", 10), this);
        NBTTagList savedOrders = c.getTagList("Orders", 10);
        for (int i = 0; i < savedOrders.tagCount(); i++) {
            NBTTagCompound tag = savedOrders.getCompoundTagAt(i);
            ArmyOrder order = ArmyOrder.read(tag);
            if (order == null || order(tag.getString("Army")) != null || !order(tag.getString("Army"), order)) {
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
