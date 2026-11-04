package noppes.npcs.rework.linked;

import java.nio.charset.StandardCharsets;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.LogWriter;
import noppes.npcs.controllers.LinkedNpcController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.items.ItemNpcCloner;

/** M13.4: opt-in placement; never replace an existing group's data with a clone. */
public final class LinkedCloner {
    private LinkedCloner() { }

    /** A group becomes a filename. Reject traversal, Windows device names and ambiguous trailing dots/spaces. */
    public static boolean validName(String name) {
        if (name == null || name.isEmpty() || name.getBytes(StandardCharsets.UTF_8).length > 120
                || !name.equals(name.trim()) || name.endsWith(".") || name.equals(".") || name.equals("..")) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < 32 || "<>:\"/\\|?*".indexOf(c) >= 0) return false;
        }
        String stem = name.split("\\.", 2)[0].trim().toUpperCase(java.util.Locale.ROOT);
        return !stem.matches("CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]");
    }

    public static boolean allowed(EntityPlayerMP player, NBTTagCompound data, String name, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        boolean ok = CustomNpcs.RwLinkedGroups == 1 && player.canUseCommand(2, "rwlinked")
                && player.getHeldItemMainhand().getItem() instanceof ItemNpcCloner
                && CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.GLOBAL_LINKED)
                && CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.SPAWNER_MOB)
                && validName(name) && "customnpcs:customnpc".equals(data.getString("id"))
                && y >= 0 && y < player.world.getHeight() - 2 && player.world.isBlockLoaded(pos)
                && player.world.getWorldBorder().contains(pos) && player.getDistanceSq(pos) <= 64.0;
        if (!ok) player.sendMessage(new TextComponentString("Linked placement refused: check rights, clone name and nearby loaded target"));
        return ok;
    }

    public static void link(EntityPlayerMP player, EntityNPCInterface npc, String name) {
        LinkedNpcController controller = LinkedNpcController.Instance;
        LinkedNpcController.LinkedData group = controller.getData(name);
        boolean create = group == null;
        if (create) {
            controller.addData(name);
            group = controller.getData(name);
        }
        npc.linkedName = group.name;
        npc.linkedData = group;
        npc.linkedLast = 0L;
        if (create) controller.saveNpcData(npc);
        controller.loadNpcData(npc);
        String message = "Placed linked NPC in " + group.name + (create ? " (new group)" : " (existing group kept)");
        player.sendMessage(new TextComponentString(message));
        LogWriter.info("RWLINKED cloner player=" + player.getName() + " group=" + group.name + " created=" + create);
    }
}
