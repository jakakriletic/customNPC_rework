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
 * {@code RwPathFollowCache}. {@code /rwpath cas 1} vklopi merjenje casa in kandidatov sledenja
 * poti (M5.10), {@code tickov} v odgovoru so ticki od zadnjega {@code reset}. M5.11: odgovor ima
 * {@code poNacinu=} (ticki in merjeni stevci po nacinu od {@code reset}), da scenarij primerja
 * nacina, med katerima preklaplja v istem boju.
 *
 * <p>Nacin se prebere ob vsakem klicu {@code isDirectPathBetweenPoints}, zato preklop velja
 * takoj. Odgovor gre tudi v log z markerjem {@code RWPATH}, da ga prebere skripta meritve.
 */
public class CommandRwPath extends CommandBase {
    /** M5.10: tick streznika ob zadnjem {@code reset}; razlika da tickov v merilnem oknu. */
    private static int resetTick;

    @Override
    public String getName() {
        return "rwpath";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/rwpath [0|1|2|reset|cas 0|cas 1]";
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
                PathFollowCache.reset(resetTick);
            } else if ("cas".equals(args[0])) {
                if (args.length < 2 || !("0".equals(args[1]) || "1".equals(args[1]))) {
                    throw new WrongUsageException(getUsage(sender));
                }
                PathFollowCache.setTiming("1".equals(args[1]));
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
                PathFollowCache.switchMode(requested, server.getTickCounter());
            }
        }
        int m = PathFollowCache.mode();
        String msg = "RWPATH nacin=" + m + " (" + PathFollowCache.describe(m) + ") klicev=" + PathFollowCache.calls()
                + " ocen=" + PathFollowCache.lookups() + " izracunov=" + PathFollowCache.misses()
                + " primerjav=" + PathFollowCache.compared() + " neujemanj=" + PathFollowCache.mismatches()
                + " cas=" + (PathFollowCache.timing() ? 1 : 0) + " tickov=" + (server.getTickCounter() - resetTick)
                + " sledenj=" + PathFollowCache.followCalls() + " sledenjNs=" + PathFollowCache.followNanos()
                + " kandidatov=" + PathFollowCache.directCalls() + " kandidatNs=" + PathFollowCache.directNanos()
                + " prostih=" + PathFollowCache.directTrue() + " histKand=" + PathFollowCache.candidateHistogram()
                + " poNacinu=" + PathFollowCache.perMode(server.getTickCounter());
        sender.sendMessage(new TextComponentString(msg));
        LogWriter.info(msg);
    }
}
