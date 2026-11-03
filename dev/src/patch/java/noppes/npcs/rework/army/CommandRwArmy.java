package noppes.npcs.rework.army;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import noppes.npcs.LogWriter;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * M11.1: {@code /rwarmy}, registered only with RwArmies>=1 (operators, level 2).
 *
 * <pre>
 * /rwarmy create &lt;army&gt; [&lt;online player&gt;|uuid:&lt;uuid&gt;|faction:&lt;id&gt;]   (default owner: the sender)
 * /rwarmy assign &lt;army&gt; &lt;npc name|selector&gt; [range]
 * /rwarmy unassign &lt;npc name|selector&gt; [range]
 * /rwarmy list
 * /rwarmy info &lt;army&gt;
 * /rwarmy remove &lt;army&gt;
 * </pre>
 *
 * An NPC name selects loaded NPCs with exactly that name within the range (default 100, at most 220 blocks)
 * around the sender, as {@code /rwsquad}; an argument starting with {@code @} or a UUID is an entity selector.
 * Every reply also goes to the log with the marker RWARMY.
 */
public class CommandRwArmy extends CommandBase {
    public static final double DEFAULT_RANGE = 100;
    public static final double MAX_RANGE = 220;
    private static final List<String> SUBCOMMANDS =
            Arrays.asList("create", "assign", "unassign", "list", "info", "remove");

    @Override
    public String getName() {
        return "rwarmy";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/rwarmy create <army> [player|uuid:<uuid>|faction:<id>] | assign <army> <npc name|selector> [range]"
                + " | unassign <npc name|selector> [range] | list | info <army> | remove <army>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        String action = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);
        if (action.equals("list")) {
            for (String line : ArmyCommandService.describe(server)) {
                reply(sender, line);
            }
            return;
        }
        if (action.equals("create") && (args.length == 2 || args.length == 3)) {
            String owner = args.length == 3 ? ownerSpec(server, args[2]) : senderSpec(sender);
            if (owner == null) {
                reply(sender, "RWARMY create needs an owner: an online player, uuid:<uuid> or faction:<id>");
                return;
            }
            reply(sender, ArmyCommandService.create(server, args[1], owner).message);
            return;
        }
        if (action.equals("assign") && (args.length == 3 || args.length == 4)) {
            double range = args.length == 4 ? parseDouble(args[3], 1, MAX_RANGE) : DEFAULT_RANGE;
            List<EntityNPCInterface> npcs = select(server, sender, args[2], range);
            if (npcs.isEmpty()) {
                reply(sender, "RWARMY no NPC matches " + args[2]);
                return;
            }
            reply(sender, ArmyCommandService.assign(server, npcs, args[1]).message);
            return;
        }
        if (action.equals("unassign") && (args.length == 2 || args.length == 3)) {
            double range = args.length == 3 ? parseDouble(args[2], 1, MAX_RANGE) : DEFAULT_RANGE;
            List<EntityNPCInterface> npcs = select(server, sender, args[1], range);
            if (npcs.isEmpty()) {
                reply(sender, "RWARMY no NPC matches " + args[1]);
                return;
            }
            reply(sender, ArmyCommandService.assign(server, npcs, "").message);
            return;
        }
        if (action.equals("info") && args.length == 2) {
            Army army = ArmyCommandService.get(server, args[1]);
            if (army == null) {
                reply(sender, "RWARMY unknown army " + args[1]);
                return;
            }
            List<EntityNPCInterface> members = ArmyCommandService.members(server, army.name());
            reply(sender, "RWARMY info " + army + " created=" + army.created() + " loaded=" + members.size());
            for (int i = 0; i < members.size() && i < 8; i++) {
                EntityNPCInterface npc = members.get(i);
                reply(sender, String.format(Locale.ROOT, "RWARMY member %s dim=%d at %.1f %.1f %.1f", npc.getName(),
                        npc.world.provider.getDimension(), npc.posX, npc.posY, npc.posZ));
            }
            return;
        }
        if (action.equals("remove") && args.length == 2) {
            reply(sender, ArmyCommandService.remove(server, args[1]).message);
            return;
        }
        throw new WrongUsageException(getUsage(sender));
    }

    /** {@code uuid:}/{@code faction:} as given, otherwise the UUID of an online player with that name. */
    private static String ownerSpec(MinecraftServer server, String arg) {
        if (arg.indexOf(':') > 0) {
            return arg;
        }
        EntityPlayerMP player = server.getPlayerList().getPlayerByUsername(arg);
        return player == null ? null : "uuid:" + player.getUniqueID();
    }

    private static String senderSpec(ICommandSender sender) {
        Entity e = sender.getCommandSenderEntity();
        return e instanceof EntityPlayerMP ? "uuid:" + e.getUniqueID() : null;
    }

    private static List<EntityNPCInterface> select(MinecraftServer server, ICommandSender sender, String target,
            double range) throws CommandException {
        List<EntityNPCInterface> out = new ArrayList<EntityNPCInterface>();
        if (target.startsWith("@") || target.split("-").length == 5) {
            for (Entity e : getEntityList(server, sender, target)) {
                if (e instanceof EntityNPCInterface) {
                    out.add((EntityNPCInterface) e);
                }
            }
            return out;
        }
        World world = sender.getEntityWorld();
        Vec3d c = sender.getPositionVector();
        AxisAlignedBB box = new AxisAlignedBB(c.x - range, c.y - range, c.z - range, c.x + range, c.y + range,
                c.z + range);
        for (EntityNPCInterface npc : world.getEntitiesWithinAABB(EntityNPCInterface.class, box)) {
            if (target.equals(npc.getName())) {
                out.add(npc);
            }
        }
        return out;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
            @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUBCOMMANDS);
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && (action.equals("assign") || action.equals("info") || action.equals("remove"))) {
            List<String> names = new ArrayList<String>();
            for (Army a : ArmyCommandService.list(server)) {
                names.add(a.name());
            }
            return getListOfStringsMatchingLastWord(args, names);
        }
        if (args.length == 3 && action.equals("create")) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        return new ArrayList<String>();
    }

    private static void reply(ICommandSender sender, String message) {
        sender.sendMessage(new TextComponentString(message));
        LogWriter.info(message);
    }
}
