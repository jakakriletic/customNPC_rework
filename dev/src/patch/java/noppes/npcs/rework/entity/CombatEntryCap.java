package noppes.npcs.rework.entity;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.util.CombatTracker;

/**
 * M10.4 (Q449): vanilla {@code CombatTracker} hrani en {@code CombatEntry} (z
 * {@code NpcDamageSource}, skupaj ~80 B) za vsak udarec in seznam pocisti sele 100-300 tickov
 * po zadnjem udarcu. NPC v neprekinjenem boju z obnovo zdravja ga zato nikoli ne pocisti
 * (zaklenjeni heap tek 132104: 81 872 -> 583 875 vnosov, ~24 MiB/h). Z {@code RwCombatEntryCap>0}
 * ostane samo zadnjih N vnosov; starejsi se zavrzejo. Vpliva le na izbiro najboljsega
 * napadalca in smrtnega sporocila iz zgodovine, starejse od zadnjih N udarcev.
 *
 * <p>Polje je zasebno; MCP in SRG ime kot v {@code RwNavigatorAccess}.
 */
public final class CombatEntryCap {
    private static final Field ENTRIES = field("combatEntries", "field_94556_a");

    private CombatEntryCap() {
    }

    private static Field field(String mcp, String srg) {
        for (String name : new String[] {mcp, srg}) {
            try {
                Field f = CombatTracker.class.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                // naslednje ime
            }
        }
        return null;
    }

    /** Odstrani najstarejse vnose nad mejo; vrne stevilo odstranjenih. cap <= 0 = original. */
    public static int trim(List<?> entries, int cap) {
        if (cap <= 0 || entries == null) {
            return 0;
        }
        int over = entries.size() - cap;
        if (over <= 0) {
            return 0;
        }
        entries.subList(0, over).clear();
        return over;
    }

    static boolean available() {
        return ENTRIES != null;
    }

    public static int apply(CombatTracker tracker, int cap) {
        if (cap <= 0 || tracker == null || ENTRIES == null) {
            return 0;
        }
        try {
            return trim((List<?>) ENTRIES.get(tracker), cap);
        } catch (IllegalAccessException e) {
            return 0;
        }
    }
}
