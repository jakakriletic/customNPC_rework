package noppes.npcs.rework.far;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.LogWriter;

/**
 * M5-V.5: klientski ukaz {@code /rwimpostor [reset]} - stanje izrisa L3 ({@link FarRender#status}): atlas,
 * peke, ali tece poseg coremoda ali rezerva, stevci zadnje slicice in cas izrisa od zadnjega {@code reset}.
 * Nicesar ne spreminja razen stevcev.
 */
@SideOnly(Side.CLIENT)
public class CommandRwImpostor extends CommandBase {
    @Override
    public String getName() {
        return "rwimpostor";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/rwimpostor [reset]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length > 0 && "reset".equals(args[0])) {
            FarRender.resetStats();
        }
        String msg = FarRender.status();
        sender.sendMessage(new TextComponentString(msg));
        LogWriter.info(msg);
    }
}
