package noppes.npcs.rework.entity;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import noppes.npcs.LogWriter;

/**
 * Ukaz {@code /rwdata [0|1|2|reset]}: pokaze ali med tekom preklopi nacin {@link DataShadow}
 * (sencna polja CNPC kljucev {@code EntityDataManager}) in izpise stevce. Ne zapise v config —
 * trajna nastavitev je {@code RwDataShadow}.
 *
 * <p>Nacin se prebere ob vsakem branju, zato preklop velja takoj; sencna polja so osvezena ne glede
 * na nacin, zato preklop na 1 ne potrebuje ogrevanja. Odgovor gre tudi v log z markerjem
 * {@code RWDATA}.
 */
public class CommandRwData extends CommandBase {
    @Override
    public String getName() {
        return "rwdata";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/rwdata [0|1|2|reset]";
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
                DataShadow.reset();
            } else {
                int requested;
                try {
                    requested = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    throw new WrongUsageException(getUsage(sender));
                }
                if (!DataShadow.isValidMode(requested)) {
                    throw new WrongUsageException(getUsage(sender));
                }
                DataShadow.setMode(requested);
            }
        }
        int m = DataShadow.mode();
        String msg = "RWDATA nacin=" + m + " (" + DataShadow.describe(m) + ") branj=" + DataShadow.reads()
                + " primerjav=" + DataShadow.compared() + " neujemanj=" + DataShadow.mismatches();
        sender.sendMessage(new TextComponentString(msg));
        LogWriter.info(msg);
    }
}
