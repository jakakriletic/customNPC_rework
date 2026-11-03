package noppes.npcs.rework.army;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

/**
 * M11.1b (research 3.5a): who may see and command armies besides their owner and operators. One record per
 * grantor and grantee (both stored as UUID): selected armies or all, separately for view and command. The grantor
 * {@link #SERVER} is an operator's grant and its "all" covers every army of the server; a player's grant covers
 * only the armies of that player, also ones created later. Without game state, so the rights matrix is unit tested.
 */
public final class ArmyGrants {
    /** Grantor of operator grants: survives the deop of whoever made it. */
    public static final String SERVER = "server";
    /** Upper bound that keeps the saved file small; owners who are not operators can add records. */
    public static final int MAX_GRANTS = 4096;

    /** Effective right; COMMAND includes VIEW. */
    public enum Access {
        NONE, VIEW, COMMAND;

        public boolean canView() {
            return this != NONE;
        }

        public boolean canCommand() {
            return this == COMMAND;
        }
    }

    public enum Result { GRANTED, ALREADY, FULL }

    /** One grantor's grants to one player. */
    public static final class Grant {
        private final String grantor;
        private final String grantee;
        private boolean viewAll;
        private boolean commandAll;
        private final Set<String> view = new TreeSet<String>();
        private final Set<String> command = new TreeSet<String>();

        Grant(String grantor, String grantee) {
            this.grantor = grantor;
            this.grantee = grantee;
        }

        public String grantor() {
            return grantor;
        }

        public String grantee() {
            return grantee;
        }

        /** "all", the selected army keys joined with commas, or "-". */
        public String describe(Access kind) {
            boolean all = kind == Access.COMMAND ? commandAll : viewAll;
            Set<String> set = kind == Access.COMMAND ? command : view;
            return all ? "all" : set.isEmpty() ? "-" : String.join(",", set);
        }

        Access access(String armyKey) {
            if (commandAll || command.contains(armyKey)) {
                return Access.COMMAND;
            }
            return viewAll || view.contains(armyKey) ? Access.VIEW : Access.NONE;
        }

        boolean empty() {
            return !viewAll && !commandAll && view.isEmpty() && command.isEmpty();
        }
    }

    private final Map<String, Map<String, Grant>> byGrantee = new TreeMap<String, Map<String, Grant>>();
    private int count;

    /** Right from grants alone; owner and operator are handled by {@link ArmyRegistry#access}. */
    public Access access(Army army, UUID player) {
        Map<String, Grant> grants = player == null ? null : byGrantee.get(player.toString());
        if (grants == null) {
            return Access.NONE;
        }
        Access best = access(grants.get(SERVER), army);
        if (best != Access.COMMAND && army.ownerType() == Army.OwnerType.PLAYER) {
            Access own = access(grants.get(army.owner()), army);
            best = own.ordinal() > best.ordinal() ? own : best;
        }
        return best;
    }

    private static Access access(Grant grant, Army army) {
        return grant == null ? Access.NONE : grant.access(army.key());
    }

    /**
     * @param kind VIEW or COMMAND
     * @param armyKey key of a selected army, null for all
     */
    public Result grant(String grantor, String grantee, Access kind, String armyKey) {
        Map<String, Grant> grants = byGrantee.get(grantee);
        Grant grant = grants == null ? null : grants.get(grantor);
        if (grant == null) {
            if (count >= MAX_GRANTS) {
                return Result.FULL;
            }
            if (grants == null) {
                grants = new TreeMap<String, Grant>();
                byGrantee.put(grantee, grants);
            }
            grant = new Grant(grantor, grantee);
            grants.put(grantor, grant);
            count++;
        }
        return set(grant, kind, armyKey) ? Result.GRANTED : Result.ALREADY;
    }

    private static boolean set(Grant grant, Access kind, String armyKey) {
        if (armyKey != null) {
            return (kind == Access.COMMAND ? grant.command : grant.view).add(armyKey);
        }
        boolean was = kind == Access.COMMAND ? grant.commandAll : grant.viewAll;
        if (kind == Access.COMMAND) {
            grant.commandAll = true;
        } else {
            grant.viewAll = true;
        }
        return !was;
    }

    /**
     * Removes grants and returns how many entries changed.
     *
     * @param grantor one grantor's record, null for every grantor (operator)
     * @param kind VIEW or COMMAND, null for both
     * @param armyKey a selected army, null for all: the "all" flag and every selected army of the kind
     */
    public int revoke(String grantor, String grantee, Access kind, String armyKey) {
        Map<String, Grant> grants = byGrantee.get(grantee);
        if (grants == null) {
            return 0;
        }
        int changed = 0;
        for (Grant grant : new ArrayList<Grant>(grants.values())) {
            if (grantor != null && !grantor.equals(grant.grantor)) {
                continue;
            }
            if (kind != Access.COMMAND) {
                changed += clear(grant, Access.VIEW, armyKey);
            }
            if (kind != Access.VIEW) {
                changed += clear(grant, Access.COMMAND, armyKey);
            }
            if (grant.empty()) {
                grants.remove(grant.grantor);
                count--;
            }
        }
        if (grants.isEmpty()) {
            byGrantee.remove(grantee);
        }
        return changed;
    }

    private static int clear(Grant grant, Access kind, String armyKey) {
        Set<String> set = kind == Access.COMMAND ? grant.command : grant.view;
        if (armyKey != null) {
            return set.remove(armyKey) ? 1 : 0;
        }
        boolean was = kind == Access.COMMAND ? grant.commandAll : grant.viewAll;
        if (kind == Access.COMMAND) {
            grant.commandAll = false;
        } else {
            grant.viewAll = false;
        }
        int changed = set.size() + (was ? 1 : 0);
        set.clear();
        return changed;
    }

    /** A removed army takes its selected grants along, so a new army of that name starts without any. */
    void removeArmy(String armyKey) {
        for (String grantee : new ArrayList<String>(byGrantee.keySet())) {
            revoke(null, grantee, null, armyKey);
        }
    }

    /** All records sorted by grantee, then grantor. */
    public List<Grant> list() {
        List<Grant> out = new ArrayList<Grant>();
        for (Map<String, Grant> grants : byGrantee.values()) {
            out.addAll(grants.values());
        }
        return out;
    }

    public int size() {
        return count;
    }

    NBTTagList write() {
        NBTTagList out = new NBTTagList();
        for (Grant g : list()) {
            NBTTagCompound c = new NBTTagCompound();
            c.setString("Grantor", g.grantor);
            c.setString("Grantee", g.grantee);
            if (g.viewAll) {
                c.setBoolean("ViewAll", true);
            }
            if (g.commandAll) {
                c.setBoolean("CommandAll", true);
            }
            if (!g.view.isEmpty()) {
                c.setTag("View", strings(g.view));
            }
            if (!g.command.isEmpty()) {
                c.setTag("Command", strings(g.command));
            }
            out.appendTag(c);
        }
        return out;
    }

    private static NBTTagList strings(Set<String> values) {
        NBTTagList list = new NBTTagList();
        for (String value : values) {
            list.appendTag(new NBTTagString(value));
        }
        return list;
    }

    /**
     * Replaces the content; the armies of the register must be read first. An invalid record, a record over the
     * limit, or a selected army that is gone or not the grantor's is skipped and counted, never thrown.
     *
     * @return number of skipped entries
     */
    int read(NBTTagList list, ArmyRegistry armies) {
        byGrantee.clear();
        count = 0;
        int skipped = 0;
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound c = list.getCompoundTagAt(i);
            String grantee = Army.canonicalOwner(Army.OwnerType.PLAYER, c.getString("Grantee"));
            String stored = c.getString("Grantor");
            String grantor = SERVER.equals(stored) ? SERVER : Army.canonicalOwner(Army.OwnerType.PLAYER, stored);
            if (grantee == null || grantor == null || count >= MAX_GRANTS) {
                skipped++;
                continue;
            }
            NBTTagList viewTags = c.getTagList("View", 8);
            NBTTagList commandTags = c.getTagList("Command", 8);
            List<String> view = selected(viewTags, grantor, armies);
            List<String> command = selected(commandTags, grantor, armies);
            skipped += viewTags.tagCount() - view.size() + commandTags.tagCount() - command.size();
            if (c.getBoolean("ViewAll")) {
                grant(grantor, grantee, Access.VIEW, null);
            }
            if (c.getBoolean("CommandAll")) {
                grant(grantor, grantee, Access.COMMAND, null);
            }
            for (String key : view) {
                grant(grantor, grantee, Access.VIEW, key);
            }
            for (String key : command) {
                grant(grantor, grantee, Access.COMMAND, key);
            }
        }
        return skipped;
    }

    private static List<String> selected(NBTTagList list, String grantor, ArmyRegistry armies) {
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < list.tagCount(); i++) {
            Army army = armies.get(list.getStringTagAt(i));
            if (army != null && (SERVER.equals(grantor)
                    || (army.ownerType() == Army.OwnerType.PLAYER && army.owner().equals(grantor)))) {
                out.add(army.key());
            }
        }
        return out;
    }
}
