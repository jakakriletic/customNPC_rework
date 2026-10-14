package noppes.npcs.rework.core;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import noppes.npcs.LogWriter;

/**
 * Ukaz {@code /rwmove [0|1|2|reset]}: pokaze ali med tekom preklopi nacin {@link CollisionGate}
 * (preskok poizvedbe entitet v {@code World.getCollisionBoxes}) in izpise stevce. Ne zapise v
 * config — trajna nastavitev je {@code RwMoveBoxes}.
 *
 * <p>V izpisu je tudi, ali je poseg v vanillo uspel ({@code poseg=1}) in koliko entitet v tem svetu
 * lahko prispeva trkalno skatlo ({@code prispevajocih}); brez posega stikalo ne naredi nicesar.
 * Odgovor gre tudi v log z markerjem {@code RWMOVE-STANJE}.
 */
public class CommandRwMove extends CommandBase {
    @Override
    public String getName() {
        return "rwmove";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/rwmove [0|1|2|reset]";
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
                CollisionGate.reset();
            } else {
                int requested;
                try {
                    requested = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    throw new WrongUsageException(getUsage(sender));
                }
                if (!CollisionGate.isValidMode(requested)) {
                    throw new WrongUsageException(getUsage(sender));
                }
                CollisionGate.setMode(requested);
            }
        }
        int m = CollisionGate.mode();
        String msg = "RWMOVE-STANJE nacin=" + m + " (" + CollisionGate.describe(m) + ") poseg="
                + (CollisionBoxesPatch.patched() ? 1 : 0) + " klicev=" + CollisionGate.calls()
                + " preskocenih=" + CollisionGate.skipped() + " primerjav=" + CollisionGate.compared()
                + " neujemanj=" + CollisionGate.mismatches() + " "
                + CollisionWatcher.summary(sender.getEntityWorld());
        sender.sendMessage(new TextComponentString(msg));
        LogWriter.info(msg);
    }
}
