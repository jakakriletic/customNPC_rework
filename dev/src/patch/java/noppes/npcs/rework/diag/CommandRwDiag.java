package noppes.npcs.rework.diag;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import noppes.npcs.LogWriter;

/**
 * Ukaz {@code /rwdiag}.
 *
 * <p>Ukaz je namenoma locen od {@code /noppes}: instrumentacija ni funkcija moda, ampak
 * orodje reworka, in mora biti uporabna tudi, ce je z mod ukazi kaj narobe. Vsi odgovori
 * gredo tudi v log z markerjem {@code RWDIAG}, da jih skriptiran scenarij lahko prebere
 * iz izpisa dedicated serverja.
 */
public class CommandRwDiag extends CommandBase {
    private static final List<String> SUBCOMMANDS =
            Arrays.asList("on", "off", "status", "reset", "dump");

    @Override
    public String getName() {
        return "rwdiag";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/rwdiag <on|off|status|reset|dump [ime]>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        String action = args.length == 0 ? "status" : args[0].toLowerCase(java.util.Locale.ROOT);
        if (action.equals("on")) {
            DiagEventCollector.enable();
            reply(sender, "RWDIAG vklopljen, stevci pocisceni");
            return;
        }
        if (action.equals("off")) {
            DiagEventCollector.disable();
            reply(sender, "RWDIAG izklopljen");
            return;
        }
        if (action.equals("reset")) {
            Diag.reset();
            reply(sender, "RWDIAG stevci pocisceni");
            return;
        }
        if (action.equals("status")) {
            DiagSnapshot snapshot = Diag.snapshot();
            reply(sender, "RWDIAG stanje=" + (DiagEventCollector.isEnabled() ? "on" : "off")
                    + " ticki=" + snapshot.ticks() + " trajanje=" + snapshot.elapsedMillis() + "ms");
            return;
        }
        if (action.equals("dump")) {
            DiagSnapshot snapshot = Diag.snapshot();
            String label = args.length > 1 ? args[1] : "dump";
            File file = DiagDump.write(snapshot, label);
            if (file == null) {
                reply(sender, "RWDIAG-NAPAKA posnetka ni bilo mogoce zapisati");
                return;
            }
            LogWriter.info(System.lineSeparator() + snapshot.toText());
            reply(sender, "RWDIAG-DUMP " + file.getPath());
            reply(sender, "RWDIAG-OK ticki=" + snapshot.ticks()
                    + " npc=" + countOf(snapshot, "npc.update"));
            return;
        }
        reply(sender, "RWDIAG neznan podukaz: " + action + "; " + getUsage(sender));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
            String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return CommandBase.getListOfStringsMatchingLastWord(args, SUBCOMMANDS);
        }
        return java.util.Collections.emptyList();
    }

    private static long countOf(DiagSnapshot snapshot, String name) {
        DiagSnapshot.Row row = snapshot.row(name);
        return row == null ? 0L : row.count;
    }

    private static void reply(ICommandSender sender, String message) {
        sender.sendMessage(new TextComponentString(message));
        LogWriter.info(message);
    }
}
