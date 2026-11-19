package noppes.npcs.rework.rig;

import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import java.nio.file.Path;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import noppes.npcs.controllers.ServerCloneController;
import noppes.npcs.LogWriter;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.CustomNpcs;
import noppes.npcs.rework.data.CloneWorkspace;
import noppes.npcs.util.NBTJsonUtil;

/** Place a complete server-side creature pack in one operation. */
public final class CommandRwCreature extends CommandBase {
    @Override public String getName() { return "rwcreature"; }
    @Override public int getRequiredPermissionLevel() { return 2; }
    @Override public String getUsage(ICommandSender sender) { return "/rwcreature list | reload | spawn/deploy <pack> [x y z] | unpack clone <tab> <id> <name...> | unpack npc <target> <id> | pack <id> <tab> <name...>"; }

    @Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) { throw new WrongUsageException(getUsage(sender)); }
        if ("unpack".equals(args[0]) || "pack".equals(args[0])) {
            workspace(server, sender, args);
            return;
        }
        if (args.length == 1 && "list".equals(args[0])) {
            List<String> packs = RigCreatureAssets.ids();
            sender.sendMessage(new TextComponentString(packs.isEmpty() ? "No creature packs loaded." : String.join(", ", packs)));
            return;
        }
        if (args.length == 1 && "reload".equals(args[0])) {
            RigCreatureTransfer.reload(server, sender);
            return;
        }
        boolean deploy = "deploy".equals(args[0]);
        if ((args.length != 2 && args.length != 5) || (!deploy && !"spawn".equals(args[0]))) {
            throw new WrongUsageException(getUsage(sender));
        }
        String id = args[1];
        Vec3d location = sender.getPositionVector();
        if (args.length == 5) {
            location = new Vec3d(parseDouble(location.x, args[2], true),
                    parseDouble(location.y, args[3], false), parseDouble(location.z, args[4], true));
        }
        World world = sender.getEntityWorld();
        if (deploy) {
            checkPosition(world, location);
            Vec3d target = location;
            RigCreatureTransfer.deploy(server, sender, id, () -> {
                try {
                    spawn(sender, world, id, target);
                } catch (CommandException failure) {
                    sender.sendMessage(new TextComponentString(failure.getMessage()));
                }
            });
        } else {
            spawn(sender, world, id, location);
        }
    }

    private void workspace(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!sender.canUseCommand(4, getName())) { throw new CommandException("commands.generic.permission"); }
        Path creatures = CustomNpcs.Dir.toPath().resolve("creatures");
        Path clones = ServerCloneController.Instance.getDir().toPath();
        try {
            Path result;
            if ("pack".equals(args[0]) && args.length >= 4) {
                result = CloneWorkspace.pack(creatures, args[1], clones, parseInt(args[2], 1, 9),
                        String.join(" ", Arrays.copyOfRange(args, 3, args.length)), CustomNpcs.RwReadableClones == 1);
            } else if ("unpack".equals(args[0]) && args.length >= 5 && "clone".equals(args[1])) {
                NBTTagCompound clone = CloneWorkspace.loadClone(clones, parseInt(args[2], 1, 9),
                        String.join(" ", Arrays.copyOfRange(args, 4, args.length)));
                result = CloneWorkspace.unpack(creatures, args[3], clone);
            } else if ("unpack".equals(args[0]) && args.length == 4 && "npc".equals(args[1])) {
                EntityNPCInterface npc = getEntity(server, sender, args[2], EntityNPCInterface.class);
                NBTTagCompound clone = new NBTTagCompound();
                if (!npc.isEntityAlive() || !npc.writeToNBTAtomically(clone)) { throw new CommandException("Cannot capture a dead NPC"); }
                ServerCloneController.Instance.cleanTags(clone);
                result = CloneWorkspace.unpack(creatures, args[3], clone);
            } else { throw new WrongUsageException(getUsage(sender)); }
            String message = "RWWORKSPACE " + args[0] + " complete: " + result.getFileName();
            LogWriter.info(message);
            sender.sendMessage(new TextComponentString(message));
        } catch (java.io.IOException | IllegalArgumentException failure) {
            throw new CommandException("Clone workspace refused: " + failure.getMessage());
        }
    }

    private void spawn(ICommandSender sender, World world, String id, Vec3d location) throws CommandException {
        RigCreatureAssets.Pack pack = RigCreatureAssets.server().packs.get(id);
        if (pack == null) { throw new CommandException("Unknown creature pack " + id); }
        BlockPos pos = checkPosition(world, location);
        EntityCustomNpc npc = new EntityCustomNpc(world);
        try {
            if (!pack.clone.isEmpty()) {
                NBTTagCompound clone = noppes.npcs.rework.data.ReadableCloneFiles.parse(pack.clone);
                ServerCloneController.Instance.cleanTags(clone);
                npc.readFromNBT(clone);
                npc.rwRig = new RigData();
            }
            if (pack.clone.isEmpty() && !pack.config.has("name")) { npc.display.setName(id); }
            String[] problems = RigApi.applyPackage(npc.wrappedNPC, pack.config.toString());
            if (problems.length > 0) { throw new IllegalArgumentException(String.join(", ", problems)); }
        } catch (Exception failure) {
            throw new CommandException("Invalid creature " + id + ": " + failure.getMessage());
        }
        npc.setPositionAndRotation(location.x, location.y, location.z, 0, 0);
        npc.ais.setStartPos(pos);
        if (!world.spawnEntity(npc)) { throw new CommandException("Could not spawn creature " + id); }
        String message = "RWRIG spawned " + npc.getName() + " (" + id + ").";
        LogWriter.info(message);
        sender.sendMessage(new TextComponentString(message));
    }

    private BlockPos checkPosition(World world, Vec3d location) throws CommandException {
        BlockPos pos = new BlockPos(location);
        if (pos.getY() < 0 || pos.getY() >= world.getHeight() || !world.isBlockLoaded(pos) || !world.getWorldBorder().contains(pos)) {
            throw new CommandException("Choose a loaded position inside the world.");
        }
        return pos;
    }

    @Override public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) { return getListOfStringsMatchingLastWord(args, "list", "reload", "spawn", "deploy", "unpack", "pack"); }
        if (args.length == 2 && "unpack".equals(args[0])) { return getListOfStringsMatchingLastWord(args, "clone", "npc"); }
        if (args.length == 2 && ("spawn".equals(args[0]) || "deploy".equals(args[0]))) {
            return getListOfStringsMatchingLastWord(args, RigCreatureAssets.ids());
        }
        return Collections.emptyList();
    }
}
