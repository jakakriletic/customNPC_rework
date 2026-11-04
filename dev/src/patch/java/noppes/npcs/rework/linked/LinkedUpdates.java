package noppes.npcs.rework.linked;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import noppes.npcs.LogWriter;
import noppes.npcs.entity.EntityNPCInterface;

/** Reports actual completion of loaded recipients; never invents a count of unloaded members. */
public final class LinkedUpdates {
    private static final Map<UUID, Report> pending = new HashMap<UUID, Report>();
    private static final class Report {
        EntityPlayerMP player;
        String group;
        long revision;
        int ticks;
        final List<EntityNPCInterface> members = new ArrayList<EntityNPCInterface>();
    }

    public static void saved(EntityPlayerMP player, EntityNPCInterface source) {
        Report report = new Report();
        report.player = player;
        report.group = source.linkedName;
        report.revision = source.linkedData.time;
        for (WorldServer world : player.getServer().worlds) {
            for (Entity entity : world.loadedEntityList) {
                if (entity instanceof EntityNPCInterface && entity != source && !entity.isDead
                        && source.linkedName.equalsIgnoreCase(((EntityNPCInterface)entity).linkedName)) {
                    report.members.add((EntityNPCInterface)entity);
                }
            }
        }
        // Bound memory to the latest edit per connected player; no permanent entity index or tick scan.
        pending.put(player.getUniqueID(), report);
    }

    public static String message(String group, int updated, int pending) {
        return "Group " + group + ": updated " + updated + " loaded NPCs; pending " + pending
                + ". Unloaded members update when loaded (total unknown).";
    }

    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pending.isEmpty()) return;
        Iterator<Report> it = pending.values().iterator();
        while (it.hasNext()) {
            Report r = it.next();
            if (r.player.hasDisconnected()) { it.remove(); continue; }
            int updated = 0;
            for (EntityNPCInterface npc : r.members) {
                if (!npc.isDead && r.group.equalsIgnoreCase(npc.linkedName) && npc.linkedLast >= r.revision) updated++;
            }
            if (updated == r.members.size() || ++r.ticks >= 200) {
                String text = message(r.group, updated, r.members.size() - updated);
                r.player.sendMessage(new TextComponentString(text));
                LogWriter.info("RWLINKED update player=" + r.player.getName() + " " + text);
                it.remove();
            }
        }
    }

    public static void clear() { pending.clear(); }
}
