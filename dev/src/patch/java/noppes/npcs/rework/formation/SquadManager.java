package noppes.npcs.rework.formation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * Register aktivnih enot. Na Forge event bus je prijavljen <b>samo, dokler obstaja vsaj
 * ena enota</b>; ko zadnja konca, se odjavi. Brez ukaza je cena v navadnem obratovanju nic
 * in obnasanje obstojecih svetov nespremenjeno (D-007, D-021).
 *
 * <p>Vse tece na server niti: ukaz, skriptni API in tick sveta.
 */
public final class SquadManager {
    /** {@code -Drwformation=off} izklopi ukaz in API. */
    public static final String PROPERTY = "rwformation";

    private static SquadManager registered;
    private static int nextId = 1;
    private final Map<Integer, Squad> squads = new LinkedHashMap<Integer, Squad>();
    private final List<EntityNPCInterface> removeFrom = new ArrayList<EntityNPCInterface>();
    private final List<FormationMoveTask> removeTask = new ArrayList<FormationMoveTask>();

    private SquadManager() {
    }

    public static boolean enabled() {
        return !"off".equalsIgnoreCase(System.getProperty(PROPERTY, "on"));
    }

    /** M7 U7: a squad member uses vanilla navigation while the formation steers it. */
    public static boolean contains(EntityNPCInterface npc) {
        if (registered == null) {
            return false;
        }
        for (Squad squad : registered.squads.values()) {
            if (!squad.isFinished() && squad.contains(npc)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Nova enota. Clani, ki so ze v drugi enoti, se iz nje izpustijo: en NPC je lahko samo
     * v eni enoti.
     */
    public static Squad start(World world, String label, List<EntityNPCInterface> npcs, SquadOrder order) {
        if (!enabled() || world == null || world.isRemote) {
            return null;
        }
        SquadManager m = instance();
        for (Squad s : m.squads.values()) {
            for (EntityNPCInterface npc : npcs) {
                s.release(npc);
            }
        }
        Squad squad = Squad.create(nextId++, world, label, npcs, order);
        if (squad == null) {
            m.unregisterIfIdle();
            return null;
        }
        m.squads.put(squad.id(), squad);
        return squad;
    }

    /** Ustavi enote, v katerih je kateri od NPC-jev; brez sidranja. */
    public static int stop(List<EntityNPCInterface> npcs) {
        if (registered == null) {
            return 0;
        }
        int released = 0;
        for (Squad s : registered.squads.values()) {
            for (EntityNPCInterface npc : npcs) {
                if (s.release(npc)) {
                    released++;
                }
            }
        }
        return released;
    }

    public static int stopAll() {
        if (registered == null) {
            return 0;
        }
        int count = registered.squads.size();
        for (Squad s : registered.squads.values()) {
            s.cancel();
        }
        registered.squads.clear();
        registered.unregisterIfIdle();
        return count;
    }

    public static List<String> status() {
        List<String> out = new ArrayList<String>();
        if (registered == null || registered.squads.isEmpty()) {
            out.add("RWSQUAD ni aktivnih enot");
            return out;
        }
        for (Squad s : registered.squads.values()) {
            out.add(s.status());
        }
        return out;
    }

    /** Ob ustavitvi strezika: enote drzijo reference na svet, zato jih ne smemo obdrzati. */
    public static void clear() {
        if (registered != null) {
            for (Squad s : registered.squads.values()) {
                s.cancel();
            }
            registered.squads.clear();
            registered.flushRemovals();
            registered.unregisterIfIdle();
        }
    }

    private static SquadManager instance() {
        if (registered == null) {
            registered = new SquadManager();
            MinecraftForge.EVENT_BUS.register(registered);
        }
        return registered;
    }

    /**
     * Task se odstrani na zacetku naslednjega ticka sveta, ne takoj: klic lahko pride iz
     * skripte med tickom NPC-ja, ko vanilla {@code EntityAITasks} iterira po svojih taskih.
     */
    static void scheduleRemoval(EntityNPCInterface npc, FormationMoveTask task) {
        SquadManager m = instance();
        m.removeFrom.add(npc);
        m.removeTask.add(task);
    }

    private void flushRemovals() {
        for (int i = 0; i < removeFrom.size(); i++) {
            removeFrom.get(i).tasks.removeTask(removeTask.get(i));
        }
        removeFrom.clear();
        removeTask.clear();
    }

    private void unregisterIfIdle() {
        if (squads.isEmpty() && removeFrom.isEmpty() && registered == this) {
            MinecraftForge.EVENT_BUS.unregister(this);
            registered = null;
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.side != Side.SERVER || event.phase != TickEvent.Phase.START) {
            return;
        }
        flushRemovals();
        Iterator<Squad> it = squads.values().iterator();
        while (it.hasNext()) {
            Squad s = it.next();
            if (s.world() != event.world) {
                continue;
            }
            s.tick();
            if (s.isFinished()) {
                noppes.npcs.LogWriter.info(s.status().replace("RWSQUAD ", "RWSQUAD konec "));
                it.remove();
            }
        }
        unregisterIfIdle();
    }

    static String describe(int count, Squad squad) {
        return String.format(Locale.ROOT, "RWSQUAD start enota=%d clanov=%d", squad.id(), count);
    }
}
