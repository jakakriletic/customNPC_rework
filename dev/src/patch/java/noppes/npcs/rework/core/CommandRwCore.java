package noppes.npcs.rework.core;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import noppes.npcs.LogWriter;

/**
 * Ukaz {@code /rwcore}: pokaze, ali je coremod (M5.15, D-031) v tem procesu tekel, ali tece v
 * produkciji (imena SRG) ali v dev okolju (MCP) in koliko razredov je slo skozi transformer.
 *
 * <p>Bere sistemske lastnosti {@code rwcore.*} in ne staticnih polj, ker je coremod nalozen v drugem
 * nalagalniku razredov kot koda moda. Brez argumentov in brez stikala: transformer ne spremeni
 * nicesar, ukaz je samo dokaz, da pot do vanilla razredov obstaja. Odgovor gre tudi v log z
 * markerjem {@code RWCORE-STANJE}.
 */
public class CommandRwCore extends CommandBase {
    @Override
    public String getName() {
        return "rwcore";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/rwcore";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        String msg = "RWCORE-STANJE nalozen=" + (RwCoreMod.loaded() ? 1 : 0)
                + " deobf=" + (RwCoreMod.runtimeDeobf() ? 1 : 0)
                + " razredov=" + RwCoreMod.classesSeen()
                + " (objavljeno na vsakih " + RwTransformer.REPORT_EVERY + ")";
        sender.sendMessage(new TextComponentString(msg));
        LogWriter.info(msg);
    }
}
