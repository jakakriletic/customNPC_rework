package noppes.npcs.rework.linked;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.LogWriter;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.controllers.LinkedNpcController;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.items.ItemNpcWand;

/**
 * M13.3 (Q400, Q401): server side of the wand list with multiple selection. A batch reaches many NPCs at once, so every
 * request needs command level 2, the wand in hand and the original permission of that action, checked when it arrives.
 */
final class LinkedWand {
    private LinkedWand() {
    }

    private static CustomNpcsPermissions.Permission permission(LinkedGuiCodec.BatchOp op) {
        switch (op) {
            case LIST: return CustomNpcsPermissions.NPC_GUI;
            case DELETE: return CustomNpcsPermissions.NPC_DELETE;
            case RESET: return CustomNpcsPermissions.NPC_RESET;
            default: return CustomNpcsPermissions.NPC_ADVANCED;
        }
    }

    private static boolean allowed(EntityPlayerMP player, LinkedGuiCodec.Batch b) {
        if (!player.canUseCommand(2, "rwlinked") || !(player.getHeldItemMainhand().getItem() instanceof ItemNpcWand)) {
            return false;
        }
        if (b.op == LinkedGuiCodec.BatchOp.COPY && LinkedCategories.has(b.mask, LinkedCategories.SCRIPTS)
                && !CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.TOOL_SCRIPTER)) {
            return false;
        }
        return CustomNpcsPermissions.hasPermission(player, permission(b.op));
    }

    static byte[] execute(MinecraftServer server, EntityPlayerMP player, LinkedGuiCodec.Batch b) {
        if (!allowed(player, b)) {
            return result(b, LinkedGuiCodec.RESULT_DENIED, 0, "No rights for this action");
        }
        if (b.op == LinkedGuiCodec.BatchOp.LIST) {
            return list(server, player, b).encode();
        }
        List<EntityNPCInterface> npcs = targets(player, b.ids);
        LinkedNpcController linked = LinkedNpcController.Instance;
        int done = 0;
        String text;
        if (b.op == LinkedGuiCodec.BatchOp.LINK) {
            LinkedNpcController.LinkedData data = linked.getData(b.group);
            if (data == null) {
                return result(b, LinkedGuiCodec.RESULT_UNKNOWN, 0, "Group no longer exists");
            }
            for (EntityNPCInterface npc : npcs) {
                if (npc.linkedData != data) {
                    // The members take the data in their next check, so RwLinkedBudget limits this too.
                    npc.linkedName = data.name;
                    npc.linkedData = data;
                    npc.linkedLast = 0L;
                    done++;
                }
            }
            text = "Linked " + done + " to " + data.name;
        } else if (b.op == LinkedGuiCodec.BatchOp.COPY) {
            Entity source = player.world.getEntityByID(b.source);
            if (!(source instanceof EntityCustomNpc) || source.isDead) {
                return result(b, LinkedGuiCodec.RESULT_UNKNOWN, 0, "The source NPC is gone");
            }
            LinkedNpcController.LinkedData once = linked.copyOf((EntityNPCInterface) source, b.mask);
            for (EntityNPCInterface npc : npcs) {
                if (npc != source && npc instanceof EntityCustomNpc) {
                    linked.copyTo(npc, once);
                    done++;
                }
            }
            text = "Copied to " + done + " from " + ((EntityNPCInterface) source).display.getName();
        } else if (b.op == LinkedGuiCodec.BatchOp.DELETE) {
            for (EntityNPCInterface npc : npcs) {
                npc.delete();
                NoppesUtilServer.deleteNpc(npc, (EntityPlayer) player);
                done++;
            }
            text = "Deleted " + done;
        } else {
            for (EntityNPCInterface npc : npcs) {
                npc.reset();
                done++;
            }
            text = "Reset " + done;
        }
        LogWriter.info("RWLINKED wand player=" + player.getName() + " op=" + b.op + " group=" + b.group + " done=" + done + " asked=" + b.ids.length);
        return result(b, LinkedGuiCodec.RESULT_OK, done, text);
    }

    private static byte[] result(LinkedGuiCodec.Batch b, int result, int done, String text) {
        // A forged name with broken UTF-8 grows when it is decoded, so the echo is cut to fit as well.
        return new LinkedGuiCodec.Response(b.id, result, 0, false, done, LinkedGuiCodec.fit(b.group, LinkedGuiCodec.MAX_NAME),
                LinkedGuiCodec.fit(text, LinkedGuiCodec.MAX_TEXT)).encode();
    }

    private static List<EntityNPCInterface> targets(EntityPlayerMP player, int[] ids) {
        Set<Integer> distinct = new LinkedHashSet<Integer>();
        List<EntityNPCInterface> npcs = new ArrayList<EntityNPCInterface>();
        for (int id : ids) {
            Entity entity = distinct.add(id) ? player.world.getEntityByID(id) : null;
            if (entity instanceof EntityNPCInterface && !entity.isDead) {
                npcs.add((EntityNPCInterface) entity);
            }
        }
        return npcs;
    }

    /** The loaded NPCs of the dimension of the player, nearest first, as the original list; at most MAX_ROWS of them. */
    private static LinkedGuiCodec.ListResponse list(MinecraftServer server, EntityPlayerMP player, LinkedGuiCodec.Batch b) {
        Map<String, Integer> members = new HashMap<String, Integer>();
        for (WorldServer world : server.worlds) {
            for (Entity entity : world.loadedEntityList) {
                if (entity instanceof EntityNPCInterface && !((EntityNPCInterface) entity).linkedName.isEmpty()) {
                    members.merge(((EntityNPCInterface) entity).linkedName.toLowerCase(Locale.ROOT), 1, Integer::sum);
                }
            }
        }
        List<LinkedGuiCodec.Group> groups = new ArrayList<LinkedGuiCodec.Group>();
        Map<String, Integer> index = new HashMap<String, Integer>();
        for (LinkedNpcController.LinkedData data : LinkedNpcController.Instance.list) {
            String key = data.name.toLowerCase(Locale.ROOT);
            if (groups.size() < LinkedGuiCodec.MAX_GROUPS && !index.containsKey(key)) {
                index.put(key, groups.size());
                groups.add(new LinkedGuiCodec.Group(LinkedGuiCodec.fit(data.name, LinkedGuiCodec.MAX_NAME), members.getOrDefault(key, 0)));
            }
        }
        List<EntityNPCInterface> npcs = new ArrayList<EntityNPCInterface>();
        for (Entity entity : player.world.loadedEntityList) {
            if (entity instanceof EntityNPCInterface && !entity.isDead) {
                npcs.add((EntityNPCInterface) entity);
            }
        }
        npcs.sort(Comparator.comparingDouble((EntityNPCInterface npc) -> player.getDistanceSq(npc)));
        List<LinkedGuiCodec.Row> rows = new ArrayList<LinkedGuiCodec.Row>();
        for (EntityNPCInterface npc : npcs.subList(0, Math.min(npcs.size(), LinkedGuiCodec.MAX_ROWS))) {
            Integer group = index.get(npc.linkedName.toLowerCase(Locale.ROOT));
            rows.add(new LinkedGuiCodec.Row(npc.getEntityId(), player.getDistance(npc), group == null ? -1 : group,
                    LinkedGuiCodec.fit(npc.display.getName(), LinkedGuiCodec.MAX_NAME)));
        }
        return new LinkedGuiCodec.ListResponse(b.id, npcs.size(), groups, rows);
    }
}
