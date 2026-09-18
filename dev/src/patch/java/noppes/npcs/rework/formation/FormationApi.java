package noppes.npcs.rework.formation;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.World;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * Vstop za skripte CustomNPCs (Nashorn). Obstojeci script API se ne spremeni; skripta do
 * razreda pride z {@code Java.type}:
 *
 * <pre>
 * var F = Java.type("noppes.npcs.rework.formation.FormationApi");
 * var npcs = event.player.world.getNearbyEntities(x, y, z, 100, 2);
 * event.player.message(F.legija(npcs, lb.x + 0.5, lb.y + 1, lb.z + 0.5, 5, 90, 3, ""));
 * </pre>
 *
 * Tabela {@code npcs} je lahko karkoli, kar Nashorn pretvori v {@code Object[]}: tabela
 * {@code IEntity} iz {@code getNearbyEntities} ali JS tabela. Elementi, ki niso NPC-ji, se
 * preskocijo. Vse metode vrnejo vrstico stanja, primerno za {@code player.message}.
 *
 * <p>Hitrost je v enotah skripte ({@code nav_speed}; 3 za razpored, 2 za march). Zastavice
 * so niz z besedami {@code drzi}, {@code brezsidra}, {@code takoj} (lahko prazen).
 */
public final class FormationApi {
    private FormationApi() {
    }

    public static String legija(Object[] npcs, double x, double y, double z, int width, double yaw, double speed,
            String flags) {
        return move(npcs, "legija", x, y, z, width, yaw, speed, flags);
    }

    public static String obramba(Object[] npcs, double x, double y, double z, double radius, double speed,
            String flags) {
        return move(npcs, "obramba", x, y, z, radius, Double.NaN, speed, flags);
    }

    public static String kolona(Object[] npcs, double x, double y, double z, int files, double yaw, double speed,
            String flags) {
        return move(npcs, "kolona", x, y, z, files, yaw, speed, flags);
    }

    public static String march(Object[] npcs, double x, double y, double z, double speed, String flags) {
        return move(npcs, "march", x, y, z, 0, Double.NaN, speed, flags);
    }

    /** Splosna oblika; {@code yaw} NaN pomeni smer zadnjega dela poti. */
    public static String move(Object[] npcs, String shapeName, double x, double y, double z, double param,
            double yaw, double speed, String flags) {
        if (!SquadManager.enabled()) {
            return "RWSQUAD izklopljen";
        }
        SquadOrder.Shape shape = SquadOrder.Shape.parse(shapeName);
        if (shape == null) {
            return "RWSQUAD neznana oblika: " + shapeName;
        }
        List<EntityNPCInterface> list = unwrap(npcs);
        if (list.isEmpty()) {
            return "RWSQUAD ni NPC-jev";
        }
        World world = list.get(0).world;
        List<EntityNPCInterface> same = new ArrayList<EntityNPCInterface>();
        for (EntityNPCInterface npc : list) {
            if (npc.world == world) {
                same.add(npc);
            }
        }
        boolean[] f = SquadOrder.parseFlags(flags);
        double p = param > 0 ? param : SquadOrder.defaultParam(shape);
        SquadOrder order = new SquadOrder(shape, p, x, y, z, yaw, speed > 0 ? speed : SquadOrder.defaultSpeed(shape),
                f[0], f[1], f[2]);
        Squad squad = SquadManager.start(world, same.get(0).getName(), same, order);
        if (squad == null) {
            return "RWSQUAD enote ni bilo mogoce sestaviti";
        }
        return squad.status();
    }

    /** Ustavi enote, v katerih so ti NPC-ji; brez sidranja. */
    public static String stop(Object[] npcs) {
        return "RWSQUAD stop izpusceni=" + SquadManager.stop(unwrap(npcs));
    }

    public static String status() {
        StringBuilder sb = new StringBuilder();
        for (String line : SquadManager.status()) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(line);
        }
        return sb.toString();
    }

    static List<EntityNPCInterface> unwrap(Object[] npcs) {
        List<EntityNPCInterface> out = new ArrayList<EntityNPCInterface>();
        if (npcs == null) {
            return out;
        }
        for (Object o : npcs) {
            Object e = o;
            if (o instanceof IEntity) {
                e = ((IEntity<?>) o).getMCEntity();
            }
            if (e instanceof EntityNPCInterface && !out.contains(e)) {
                out.add((EntityNPCInterface) e);
            }
        }
        return out;
    }
}
