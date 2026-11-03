package noppes.npcs.rework.anim;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

/** P7c (D-098, Q275): {@code /npcanim reload [entityId]} and {@code /npcanim status}, registered only with
 * RwAnimHooks>=1. Reads animations from {@code config/customnpcs/animations/<entityId>.animation.json}; a
 * missing file restores the jar's clips. The answer comes from {@link ReloadServer}, marker {@code NPCANIM}. */
public class CommandNpcAnim extends CommandBase {
    @Override public String getName() { return "npcanim"; }

    @Override public String getUsage(ICommandSender sender) { return "/npcanim reload [entityId] | status"; }

    @Override public int getRequiredPermissionLevel() { return 2; }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws WrongUsageException {
        ReloadServer reload = ReloadServer.instance();
        if (reload == null) { throw new WrongUsageException(getUsage(sender)); }
        if (args.length == 1 && args[0].equals("status")) { reload.status(sender); }
        else if (args.length >= 1 && args.length <= 2 && args[0].equals("reload")) { reload.reload(server, sender, args.length == 2 ? args[1] : null); }
        else { throw new WrongUsageException(getUsage(sender)); }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, Arrays.asList("reload", "status")) : Collections.<String>emptyList();
    }
}
