package noppes.npcs.rework.ai;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import noppes.npcs.LogWriter;

/**
 * Ukaz {@code /rwtarget [0|1|reset]}: pokaze ali med tekom preklopi nacin
 * {@link TargetPrefilter} in izpise stevec predzavrnitev. Ne zapise v config — trajna
 * nastavitev je {@code RwTargetPrefilter}.
 *
 * <p>Predikat se oceni ob vsakem iskanju tarce, zato preklop velja takoj, brez ponovne sestave
 * AI. Odgovor gre tudi v log z markerjem {@code RWTARGET}, da ga prebere skripta meritve.
 */
public class CommandRwTarget extends CommandBase {
    @Override
    public String getName() {
        return "rwtarget";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/rwtarget [0|1|reset]";
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
                TargetPrefilter.resetRejected();
            } else {
                int requested;
                try {
                    requested = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    throw new WrongUsageException(getUsage(sender));
                }
                if (!TargetPrefilter.isValidMode(requested)) {
                    throw new WrongUsageException(getUsage(sender));
                }
                TargetPrefilter.setMode(requested);
            }
        }
        int m = TargetPrefilter.mode();
        String msg = "RWTARGET nacin=" + m + " (" + TargetPrefilter.describe(m) + ") predzavrnjenih="
                + TargetPrefilter.rejected();
        sender.sendMessage(new TextComponentString(msg));
        LogWriter.info(msg);
    }
}
