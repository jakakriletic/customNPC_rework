package noppes.npcs.rework.nav;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import noppes.npcs.LogWriter;

/**
 * Ukaz {@code /rwpath [0|1|2|reset]}: pokaze ali med tekom preklopi nacin
 * {@link PathFollowCache} in izpise stevce. Ne zapise v config — trajna nastavitev je
 * {@code RwPathFollowCache}.
 *
 * <p>Nacin se prebere ob vsakem klicu {@code isDirectPathBetweenPoints}, zato preklop velja
 * takoj. Odgovor gre tudi v log z markerjem {@code RWPATH}, da ga prebere skripta meritve.
 */
public class CommandRwPath extends CommandBase {
    @Override
    public String getName() {
        return "rwpath";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/rwpath [0|1|2|reset]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
            throws WrongUsageException {
        if (args.length > 0) {
            if ("reset".equals(args[0])) {
                PathFollowCache.reset();
            } else {
                int requested;
                try {
                    requested = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    throw new WrongUsageException(getUsage(sender));
                }
                if (!PathFollowCache.isValidMode(requested)) {
                    throw new WrongUsageException(getUsage(sender));
                }
                PathFollowCache.setMode(requested);
            }
        }
        int m = PathFollowCache.mode();
        String msg = "RWPATH nacin=" + m + " (" + PathFollowCache.describe(m) + ") klicev=" + PathFollowCache.calls()
                + " branj=" + PathFollowCache.lookups() + " iskanjChunka=" + PathFollowCache.misses()
                + " primerjav=" + PathFollowCache.compared() + " neujemanj=" + PathFollowCache.mismatches();
        sender.sendMessage(new TextComponentString(msg));
        LogWriter.info(msg);
    }
}
