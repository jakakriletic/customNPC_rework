package noppes.npcs.rework.entity;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import noppes.npcs.LogWriter;

/**
 * Ukaz {@code /rwmount [0|1|2]}: pokaze ali med tekom preklopi nacin krmiljenja nosilca
 * ({@link RiderState}). Ne zapise v config — trajna nastavitev je {@code RwMountSteering}.
 * Odgovor gre tudi v log z markerjem {@code RWMOUNT}, da ga prebere {@code r1-run.ps1}.
 */
public class CommandRwMount extends CommandBase {
    @Override
    public String getName() {
        return "rwmount";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/rwmount [0|1|2]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
            throws WrongUsageException {
        if (args.length > 0) {
            int requested;
            try {
                requested = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                throw new WrongUsageException(getUsage(sender));
            }
            if (!RiderState.isValidMode(requested)) {
                throw new WrongUsageException(getUsage(sender));
            }
            RiderState.setMode(requested);
        }
        int m = RiderState.mode();
        String msg = "RWMOUNT nacin=" + m + " (" + RiderState.describe(m) + ")";
        sender.sendMessage(new TextComponentString(msg));
        LogWriter.info(msg);
    }
}
