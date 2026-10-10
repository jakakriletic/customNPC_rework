package noppes.npcs.rework.net;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import noppes.npcs.LogWriter;

/**
 * Ukaz {@code /rwblink [0|1|2|reset|cas 0|cas 1|poskus [n]]}: pokaze ali med tekom preklopi nacin
 * {@link AssociatedPlayers} (prejemniki utripa oci) in izpise stevce. Ne zapise v config —
 * trajna nastavitev je {@code RwBlinkRecipients}.
 *
 * <p>{@code cas 1} vklopi merjenje ns okoli iskanja prejemnikov; {@code poNacinu=} so ticki in
 * merjeni stevci po nacinu od zadnjega {@code reset}, da scenarij primerja nacina, med katerima
 * preklaplja v istem zagonu (A/B kot M5.11). Nacin se prebere ob vsakem utripu, zato preklop
 * velja takoj. Odgovor gre tudi v log z markerjem {@code RWBLINK}.
 *
 * <p>{@code poskus [n]} je preverba enakosti v svetu brez posiljanja paketov: za n NPC-jev
 * primerja vanilla poizvedbo in obhod seznama entitet na petih kvadrih
 * ({@link AssociatedPlayers#probe}). Odgovor ima marker {@code RWBLINK-POSKUS}.
 */
public class CommandRwBlink extends CommandBase {
    /** Tick streznika ob zadnjem {@code reset}; razlika da tickov v merilnem oknu. */
    private static int resetTick;

    @Override
    public String getName() {
        return "rwblink";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/rwblink [0|1|2|reset|cas 0|cas 1|poskus [n]]";
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
                resetTick = server.getTickCounter();
                AssociatedPlayers.reset(resetTick);
            } else if ("poskus".equals(args[0])) {
                int samples = 64;
                if (args.length > 1) {
                    try {
                        samples = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        throw new WrongUsageException(getUsage(sender));
                    }
                }
                if (samples < 1) {
                    throw new WrongUsageException(getUsage(sender));
                }
                long done = AssociatedPlayers.probe(sender.getEntityWorld(), samples);
                String probeMsg = "RWBLINK-POSKUS primerjav=" + done + " skupaj=" + AssociatedPlayers.compared()
                        + " neujemanj=" + AssociatedPlayers.mismatches();
                sender.sendMessage(new TextComponentString(probeMsg));
                LogWriter.info(probeMsg);
            } else if ("cas".equals(args[0])) {
                if (args.length < 2 || !("0".equals(args[1]) || "1".equals(args[1]))) {
                    throw new WrongUsageException(getUsage(sender));
                }
                AssociatedPlayers.setTiming("1".equals(args[1]));
            } else {
                int requested;
                try {
                    requested = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    throw new WrongUsageException(getUsage(sender));
                }
                if (!AssociatedPlayers.isValidMode(requested)) {
                    throw new WrongUsageException(getUsage(sender));
                }
                AssociatedPlayers.switchMode(requested, server.getTickCounter());
            }
        }
        int m = AssociatedPlayers.mode();
        String msg = "RWBLINK nacin=" + m + " (" + AssociatedPlayers.describe(m) + ") iskanj="
                + AssociatedPlayers.calls() + " igralcev=" + AssociatedPlayers.players()
                + " prejemnikov=" + AssociatedPlayers.recipients() + " chunkov=" + AssociatedPlayers.cells()
                + " primerjav=" + AssociatedPlayers.compared() + " neujemanj=" + AssociatedPlayers.mismatches()
                + " cas=" + (AssociatedPlayers.timing() ? 1 : 0)
                + " tickov=" + (server.getTickCounter() - resetTick)
                + " poNacinu=" + AssociatedPlayers.perMode(server.getTickCounter());
        sender.sendMessage(new TextComponentString(msg));
        LogWriter.info(msg);
    }
}
