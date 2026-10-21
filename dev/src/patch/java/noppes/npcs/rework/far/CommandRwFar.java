package noppes.npcs.rework.far;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import noppes.npcs.LogWriter;

/**
 * Ukaz {@code /rwfar [0|1|reset|domet <bloki>]}: pokaze ali med tekom preklopi posiljanje
 * nadomestkov ({@link FarServer}) in izpise stevce. Ne zapise v config - trajni nastavitvi sta
 * {@code RwFarView} in {@code RwFarRange}.
 *
 * <p>Izpis (marker {@code RWFAR}): {@code registra} (NPC-ji v registru) proti {@code npcVSvetu}
 * (NPC-ji v {@code loadedEntityList}), stevci krogov, vnosov, odstranitev, sporocil, bajtov in ns
 * obdelave od zadnjega {@code reset}, {@code usNaTickNaIgralca} (D-043) in {@code poIgralcih} kot
 * {@code ime:pas:znanih:krogov:vnosov:odstranitev:bajtov:ns:nsMax}.
 */
public class CommandRwFar extends CommandBase {
    @Override
    public String getName() {
        return "rwfar";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/rwfar [0|1|reset|domet <bloki>]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws WrongUsageException {
        if (args.length > 0) {
            if ("reset".equals(args[0])) {
                FarServer.reset(server.getTickCounter());
            } else if ("domet".equals(args[0])) {
                if (args.length < 2) {
                    throw new WrongUsageException(getUsage(sender));
                }
                try {
                    FarServer.setRange(Integer.parseInt(args[1]));
                } catch (NumberFormatException e) {
                    throw new WrongUsageException(getUsage(sender));
                }
            } else if ("0".equals(args[0]) || "1".equals(args[0])) {
                FarServer.setMode(server, Integer.parseInt(args[0]));
            } else {
                throw new WrongUsageException(getUsage(sender));
            }
        }
        String msg = FarServer.status(server);
        sender.sendMessage(new TextComponentString(msg));
        LogWriter.info(msg);
    }
}
