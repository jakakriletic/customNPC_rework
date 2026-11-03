package noppes.npcs.rework.army;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import net.minecraft.nbt.NBTTagCompound;

/**
 * M11.1 (Q297): one army of the register. The name is the identity (compared without case); the owner
 * is a player UUID or a CustomNPCs faction id. Immutable; the register replaces an entry to change it.
 */
public final class Army {
    public enum OwnerType {
        PLAYER("player"), FACTION("faction");

        private final String id;

        OwnerType(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        static OwnerType parse(String id) {
            for (OwnerType t : values()) {
                if (t.id.equals(id)) {
                    return t;
                }
            }
            return null;
        }
    }

    /** NBT key of the army name on an NPC. */
    public static final String NPC_KEY = "RwArmy";
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private final String name;
    private final OwnerType ownerType;
    private final String owner;
    private final String ownerName;
    private final long created;

    public Army(String name, OwnerType ownerType, String owner, String ownerName, long created) {
        if (!validName(name)) {
            throw new IllegalArgumentException("invalid army name: " + name);
        }
        if (ownerType == null || canonicalOwner(ownerType, owner) == null) {
            throw new IllegalArgumentException("invalid army owner: " + ownerType + " " + owner);
        }
        this.name = name;
        this.ownerType = ownerType;
        this.owner = canonicalOwner(ownerType, owner);
        this.ownerName = ownerName == null ? "" : ownerName;
        this.created = created;
    }

    /** 1-32 letters, digits, '_' or '-'; the name is a command argument and an NBT string. */
    public static boolean validName(String name) {
        return name != null && NAME.matcher(name).matches();
    }

    /** Register key: the name without case, so "Alpha" and "alpha" are one army. */
    public static String key(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    /** Canonical owner text or null: a UUID for a player, a non-negative integer for a faction. */
    static String canonicalOwner(OwnerType type, String owner) {
        if (type == null || owner == null) {
            return null;
        }
        try {
            if (type == OwnerType.PLAYER) {
                return UUID.fromString(owner).toString();
            }
            int id = Integer.parseInt(owner);
            return id >= 0 ? Integer.toString(id) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String name() {
        return name;
    }

    public String key() {
        return key(name);
    }

    public OwnerType ownerType() {
        return ownerType;
    }

    /** Player UUID text or faction id text. */
    public String owner() {
        return owner;
    }

    /** Name for messages only (player name or faction name when created); may be empty. */
    public String ownerName() {
        return ownerName;
    }

    /** Total world time when the army was created. */
    public long created() {
        return created;
    }

    /** Owner as written in commands and the script API: {@code uuid:<uuid>} or {@code faction:<id>}. */
    public String ownerSpec() {
        return (ownerType == OwnerType.PLAYER ? "uuid:" : "faction:") + owner;
    }

    NBTTagCompound write() {
        NBTTagCompound c = new NBTTagCompound();
        c.setString("Name", name);
        c.setString("OwnerType", ownerType.id());
        c.setString("Owner", owner);
        if (!ownerName.isEmpty()) {
            c.setString("OwnerName", ownerName);
        }
        c.setLong("Created", created);
        return c;
    }

    /** Null when the entry is not a valid army (it is then left out of the register and counted). */
    static Army read(NBTTagCompound c) {
        if (!c.hasKey("Name", 8) || !c.hasKey("OwnerType", 8) || !c.hasKey("Owner", 8)) {
            return null;
        }
        OwnerType type = OwnerType.parse(c.getString("OwnerType"));
        String name = c.getString("Name");
        if (type == null || !validName(name) || canonicalOwner(type, c.getString("Owner")) == null) {
            return null;
        }
        return new Army(name, type, c.getString("Owner"), c.getString("OwnerName"), c.getLong("Created"));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Army)) {
            return false;
        }
        Army a = (Army) o;
        return name.equals(a.name) && ownerType == a.ownerType && owner.equals(a.owner)
                && ownerName.equals(a.ownerName) && created == a.created;
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 31 + owner.hashCode();
    }

    @Override
    public String toString() {
        return name + " owner=" + ownerSpec() + (ownerName.isEmpty() ? "" : " (" + ownerName + ")");
    }
}
