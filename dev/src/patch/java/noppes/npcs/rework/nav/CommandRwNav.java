package noppes.npcs.rework.nav;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import noppes.npcs.LogWriter;
import noppes.npcs.entity.EntityNPCInterface;

/** M7 diagnostic switch for loaded NPCs, including scripted A/B fixtures. */
public final class CommandRwNav extends CommandBase {
    @Override
    public String getName() {
        return "rwnav";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/rwnav <on|off|status> [name-prefix]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        String action = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        if (!action.equals("on") && !action.equals("off") && !action.equals("status")) {
            throw new WrongUsageException(getUsage(sender));
        }
        String prefix = args.length > 1 ? args[1] : "";
        if (!action.equals("status") && prefix.isEmpty()) {
            throw new WrongUsageException(getUsage(sender));
        }
        int matched = 0;
        int selected = 0;
        World world = sender.getEntityWorld();
        for (Entity entity : world.loadedEntityList) {
            if (!(entity instanceof EntityNPCInterface)) {
                continue;
            }
            EntityNPCInterface npc = (EntityNPCInterface) entity;
            if (!npc.getName().startsWith(prefix)) {
                continue;
            }
            matched++;
            if (action.equals("on")) {
                npc.setRwNavBackend(1);
            } else if (action.equals("off")) {
                npc.setRwNavBackend(0);
            }
            selected += npc.getRwNavBackend();
        }
        String line = "RWNAV global=" + RwNavBackend.globalMode()
                + " mod=" + Loader.isModLoaded("npcbaritone") + " ime=" + prefix
                + " ujemanj=" + matched + " izbranih=" + selected;
        sender.sendMessage(new TextComponentString(line));
        LogWriter.info(line);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
                                          String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("on", "off", "status"));
        }
        return super.getTabCompletions(server, sender, args, targetPos);
    }
}
