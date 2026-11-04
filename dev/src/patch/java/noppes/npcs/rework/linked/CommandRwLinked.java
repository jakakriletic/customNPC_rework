package noppes.npcs.rework.linked;

import java.util.Arrays;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import noppes.npcs.LogWriter;
import noppes.npcs.controllers.LinkedNpcController;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * M13.2: {@code /rwlinked}, registered only with RwLinkedGroups=1, operators (level 2).
 *
 * <pre>
 * /rwlinked list
 * /rwlinked info &lt;group&gt;
 * /rwlinked categories &lt;group&gt; default|none|all|&lt;names&gt;|+name|-name ...
 * /rwlinked undo &lt;group&gt;
 * </pre>
 *
 * Every reply also goes to the log with the marker RWLINKED.
 */
public class CommandRwLinked extends CommandBase {
    @Override
    public String getName() {
        return "rwlinked";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/rwlinked list | info <group> | categories <group> default|none|all|<names>|+name|-name ... | undo <group>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        LinkedNpcController linked = LinkedNpcController.Instance;
        if (args.length == 1 && args[0].equals("list")) {
            reply(sender, "RWLINKED list groups=" + linked.list.size());
            for (LinkedNpcController.LinkedData data : linked.list) {
                reply(sender, describe(server, data));
            }
            return;
        }
        if (args.length < 2) {
            throw new WrongUsageException(getUsage(sender));
        }
        LinkedNpcController.LinkedData data = linked.getData(args[1]);
        if (data == null) {
            reply(sender, "RWLINKED unknown group=" + args[1]);
            return;
        }
        if (args[0].equals("info") && args.length == 2) {
            reply(sender, describe(server, data));
            LinkedBudget budget = linked.budget;
            reply(sender, "RWLINKED budget applied=" + budget.applied + " deferred=" + budget.deferred
                    + " maxPerTick=" + budget.maxPerTick + " microsPerTransfer=" + budget.microsPerTransfer());
        } else if (args[0].equals("categories") && args.length >= 3) {
            int mask = LinkedCategories.apply(data.categories, Arrays.copyOfRange(args, 2, args.length));
            if (mask < 0) {
                reply(sender, "RWLINKED categories rejected group=" + data.name + " kept="
                        + LinkedCategories.toText(data.categories));
                return;
            }
            linked.setCategories(data, mask);
            reply(sender, "RWLINKED categories group=" + data.name + " now=" + LinkedCategories.toText(mask));
        } else if (args[0].equals("undo") && args.length == 2) {
            reply(sender, "RWLINKED undo group=" + data.name + " done=" + (linked.undo(data) ? 1 : 0));
        } else {
            throw new WrongUsageException(getUsage(sender));
        }
    }

    private static String describe(MinecraftServer server, LinkedNpcController.LinkedData data) {
        int members = 0;
        for (WorldServer world : server.worlds) {
            for (Entity entity : world.loadedEntityList) {
                if (entity instanceof EntityNPCInterface && data.name.equalsIgnoreCase(((EntityNPCInterface) entity).linkedName)) {
                    members++;
                }
            }
        }
        return "RWLINKED group=" + data.name + " categories=" + LinkedCategories.toText(data.categories)
                + " loaded=" + members + " undo=" + (LinkedNpcController.Instance.hasPrevious(data) ? 1 : 0);
    }

    private static void reply(ICommandSender sender, String message) {
        sender.sendMessage(new TextComponentString(message));
        LogWriter.info(message);
    }
}
