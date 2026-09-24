package noppes.npcs.rework.ai;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import noppes.npcs.LogWriter;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * Ukaz {@code /rwattack [0|1]}: pokaze ali med tekom preklopi nacin
 * {@link AttackPriority}. Ne zapise v config — trajna nastavitev je {@code RwAttackPriority}.
 *
 * <p>Prioritete se dolocijo ob sestavi AI ({@code EntityNPCInterface.updateTasks}), zato ukaz
 * ob spremembi vsem nalozenim NPC-jem nastavi {@code updateAI}; AI se jim sestavi znova ob
 * naslednjem ticku. Odgovor gre tudi v log z markerjem {@code RWATTACK}, da ga prebere
 * {@code m36-run.ps1}.
 */
public class CommandRwAttack extends CommandBase {
    @Override
    public String getName() {
        return "rwattack";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/rwattack [0|1]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
            throws WrongUsageException {
        int rebuilt = 0;
        if (args.length > 0) {
            int requested;
            try {
                requested = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                throw new WrongUsageException(getUsage(sender));
            }
            if (!AttackPriority.isValidMode(requested)) {
                throw new WrongUsageException(getUsage(sender));
            }
            if (AttackPriority.mode() != requested) {
                AttackPriority.setMode(requested);
                rebuilt = requestAiRebuild(server);
            }
        }
        int m = AttackPriority.mode();
        String msg = "RWATTACK nacin=" + m + " (" + AttackPriority.describe(m) + ") preurejenih=" + rebuilt;
        sender.sendMessage(new TextComponentString(msg));
        LogWriter.info(msg);
    }

    private static int requestAiRebuild(MinecraftServer server) {
        int n = 0;
        for (WorldServer world : server.worlds) {
            if (world == null) {
                continue;
            }
            for (Entity e : world.loadedEntityList) {
                if (e instanceof EntityNPCInterface) {
                    ((EntityNPCInterface) e).updateAI = true;
                    n++;
                }
            }
        }
        return n;
    }
}
