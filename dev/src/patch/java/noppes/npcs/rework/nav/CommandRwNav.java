package noppes.npcs.rework.nav;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import noppes.npcs.LogWriter;
import noppes.npcs.entity.EntityNPCInterface;

/** M7 diagnostic switch for loaded NPCs, including scripted A/B fixtures. */
public final class CommandRwNav extends CommandBase {
    @Override
    public String getName() {
        return "rwnav";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/rwnav <on|off|status> [name-prefix] | paths <name-prefix> <x> <y> <z>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        String action = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        if (!action.equals("on") && !action.equals("off") && !action.equals("status") && !action.equals("paths")) {
            throw new WrongUsageException(getUsage(sender));
        }
        String prefix = args.length > 1 ? args[1] : "";
        if (!action.equals("status") && prefix.isEmpty()) {
            throw new WrongUsageException(getUsage(sender));
        }
        if (action.equals("paths")) {
            if (args.length != 5) {
                throw new WrongUsageException(getUsage(sender));
            }
            BlockPos goal = new BlockPos(parseInt(args[2]), parseInt(args[3]), parseInt(args[4]));
            NavPathSample sample = new NavPathSample();
            for (Entity entity : sender.getEntityWorld().loadedEntityList) {
                if (entity instanceof EntityNPCInterface && entity.getName().startsWith(prefix)) {
                    sample.add(((EntityNPCInterface) entity).getNavigator().getPath(), goal);
                }
            }
            String line = sample.marker(prefix);
            sender.sendMessage(new TextComponentString(line));
            LogWriter.info(line);
            return;
        }
        int matched = 0;
        int selected = 0;
        int attached = 0;
        boolean modLoaded = Loader.isModLoaded("npcbaritone");
        World world = sender.getEntityWorld();
        for (Entity entity : world.loadedEntityList) {
            if (!(entity instanceof EntityNPCInterface)) {
                continue;
            }
            EntityNPCInterface npc = (EntityNPCInterface) entity;
            if (!npc.getName().startsWith(prefix)) {
                continue;
            }
            matched++;
            if (action.equals("on")) {
                npc.setRwNavBackend(1);
            } else if (action.equals("off")) {
                npc.setRwNavBackend(0);
            }
            selected += npc.getRwNavBackend();
            // M7.6: scenarij (nav-run -Ozadje baritone) mora videti, da je knjiznica res pripeta,
            // ne samo, da je NBT stikalo vklopljeno (globalno stikalo, movementType, jahanje).
            if (modLoaded && RwNavBackend.attached(npc)) {
                attached++;
            }
        }
        String line = "RWNAV global=" + RwNavBackend.globalMode()
                + " mod=" + modLoaded + " ime=" + prefix
                + " ujemanj=" + matched + " izbranih=" + selected + " pripetih=" + attached;
        sender.sendMessage(new TextComponentString(line));
        LogWriter.info(line);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
                                          String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("on", "off", "status", "paths"));
        }
        return super.getTabCompletions(server, sender, args, targetPos);
    }
}
