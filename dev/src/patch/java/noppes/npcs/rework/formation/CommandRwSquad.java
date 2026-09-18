package noppes.npcs.rework.formation;

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
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import noppes.npcs.LogWriter;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * Ukaz {@code /rwsquad}: formacije za skupino NPC-jev z istim imenom.
 *
 * <pre>
 * /rwsquad legija  &lt;ime&gt; &lt;x&gt; &lt;y&gt; &lt;z&gt; [sirina] [yaw|~] [hitrost] [doseg] [zastavice]
 * /rwsquad obramba &lt;ime&gt; &lt;x&gt; &lt;y&gt; &lt;z&gt; [polmer] [hitrost] [doseg] [zastavice]
 * /rwsquad kolona  &lt;ime&gt; &lt;x&gt; &lt;y&gt; &lt;z&gt; [vrst] [yaw|~] [hitrost] [doseg] [zastavice]
 * /rwsquad march   &lt;ime&gt; &lt;x&gt; &lt;y&gt; &lt;z&gt; [hitrost] [doseg] [zastavice]
 * /rwsquad stop [ime] [doseg]
 * /rwsquad status
 * </pre>
 *
 * Zastavice: {@code drzi} (ne prekini pohoda za boj), {@code brezsidra} (ne premakni doma),
 * {@code takoj} (ne postroji se na zacetku). Hitrost je v enotah skripte ({@code nav_speed}).
 * Yaw {@code ~} pomeni smer posiljatelja, zaokrozeno na 90 stopinj, kot v skripti.
 * Vsak odgovor gre tudi v log z markerjem {@code RWSQUAD}.
 */
public class CommandRwSquad extends CommandBase {
    public static final double DEFAULT_RANGE = 100;
    public static final double MAX_RANGE = 220;

    private static final List<String> SUBCOMMANDS =
            Arrays.asList("legija", "obramba", "kolona", "march", "stop", "status");

    @Override
    public String getName() {
        return "rwsquad";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/rwsquad <legija|obramba|kolona|march> <ime> <x> <y> <z> [param] [yaw|~] [hitrost] [doseg]"
                + " [drzi|brezsidra|takoj] | stop [ime] [doseg] | status";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!SquadManager.enabled()) {
            reply(sender, "RWSQUAD izklopljen (-D" + SquadManager.PROPERTY + "=off)");
            return;
        }
        String action = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        if (action.equals("status")) {
            for (String line : SquadManager.status()) {
                reply(sender, line);
            }
            return;
        }
        if (action.equals("stop")) {
            if (args.length < 2) {
                reply(sender, "RWSQUAD stop vse=" + SquadManager.stopAll());
                return;
            }
            double range = args.length > 2 ? parseDouble(args[2], 1, MAX_RANGE) : DEFAULT_RANGE;
            List<EntityNPCInterface> npcs = find(sender, args[1], range);
            reply(sender, "RWSQUAD stop ime=" + args[1] + " izpusceni=" + SquadManager.stop(npcs));
            return;
        }
        SquadOrder.Shape shape = SquadOrder.Shape.parse(action);
        if (shape == null || args.length < 5) {
            throw new WrongUsageException(getUsage(sender));
        }
        String name = args[1];
        BlockPos pos = parseBlockPos(sender, args, 2, false);

        List<String> numbers = new ArrayList<String>();
        StringBuilder flags = new StringBuilder();
        for (int i = 5; i < args.length; i++) {
            if (SquadOrder.isFlag(args[i])) {
                flags.append(args[i]).append(' ');
            } else {
                numbers.add(args[i]);
            }
        }
        int k = 0;
        double param = SquadOrder.defaultParam(shape);
        if (shape != SquadOrder.Shape.MARCH && k < numbers.size()) {
            param = parseDouble(numbers.get(k++), 1, 80);
        }
        double yaw = Double.NaN;
        if ((shape == SquadOrder.Shape.LEGIJA || shape == SquadOrder.Shape.KOLONA)) {
            yaw = senderYaw(sender);
            if (k < numbers.size()) {
                String y = numbers.get(k++);
                if (!y.equals("~")) {
                    yaw = parseDouble(y);
                }
            }
        }
        double speed = SquadOrder.defaultSpeed(shape);
        if (k < numbers.size()) {
            speed = parseDouble(numbers.get(k++), SquadOrder.MIN_SPEED, SquadOrder.MAX_SPEED);
        }
        double range = DEFAULT_RANGE;
        if (k < numbers.size()) {
            range = parseDouble(numbers.get(k++), 1, MAX_RANGE);
        }
        boolean[] f = SquadOrder.parseFlags(flags.toString());

        List<EntityNPCInterface> npcs = find(sender, name, range);
        if (npcs.isEmpty()) {
            reply(sender, "RWSQUAD ni NPC-jev z imenom " + name + " v dosegu " + range);
            return;
        }
        SquadOrder order = new SquadOrder(shape, param, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, speed,
                f[0], f[1], f[2]);
        Squad squad = SquadManager.start(sender.getEntityWorld(), name, npcs, order);
        if (squad == null) {
            reply(sender, "RWSQUAD enote ni bilo mogoce sestaviti (" + npcs.size() + " kandidatov)");
            return;
        }
        reply(sender, SquadManager.describe(npcs.size(), squad));
        reply(sender, squad.status());
    }

    /** NPC-ji z danim imenom v kvadru {@code doseg} okoli posiljatelja. */
    static List<EntityNPCInterface> find(ICommandSender sender, String name, double range) {
        World world = sender.getEntityWorld();
        Vec3d c = sender.getPositionVector();
        AxisAlignedBB box = new AxisAlignedBB(c.x - range, c.y - range, c.z - range, c.x + range, c.y + range,
                c.z + range);
        List<EntityNPCInterface> out = new ArrayList<EntityNPCInterface>();
        for (EntityNPCInterface npc : world.getEntitiesWithinAABB(EntityNPCInterface.class, box)) {
            if (name.equals(npc.getName())) {
                out.add(npc);
            }
        }
        return out;
    }

    private static double senderYaw(ICommandSender sender) {
        Entity e = sender.getCommandSenderEntity();
        if (e == null) {
            return 0;
        }
        return Math.round(e.rotationYaw / 90.0) * 90.0;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
            @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUBCOMMANDS);
        }
        if (args.length >= 3 && args.length <= 5 && SquadOrder.Shape.parse(args[0]) != null) {
            return getTabCompletionCoordinate(args, 2, targetPos);
        }
        return super.getTabCompletions(server, sender, args, targetPos);
    }

    private static void reply(ICommandSender sender, String message) {
        sender.sendMessage(new TextComponentString(message));
        LogWriter.info(message);
    }
}
