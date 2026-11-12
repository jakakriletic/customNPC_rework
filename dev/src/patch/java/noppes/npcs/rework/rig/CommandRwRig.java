package noppes.npcs.rework.rig;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import noppes.npcs.LogWriter;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * M16: {@code /rwrig} (operators). The NPC is {@code @near} (nearest NPC within 16 blocks of the sender)
 * or a name prefix (every loaded NPC whose name starts with it).
 * <ul>
 * <li>{@code models} lists the models and their clips; {@code reload} reads the model files again
 *   ({@code customnpcs/rigs/}) on the server and every client;</li>
 * <li>{@code set <npc> <model>} wears a model, {@code clear <npc>} goes back to the original look;</li>
 * <li>{@code play <npc> <clip> [fadeTicks]}, {@code queue <npc> <clip>}, {@code stop <npc>};</li>
 * <li>{@code apply <npc> <file>} applies a rig package from {@code customnpcs/rigs/} (model, clips, JS script; see
 *   {@link RigApi#applyPackage});</li>
 * <li>{@code clips <npc> <file>} loads the NPC's own clips from {@code customnpcs/rigs/<file>} (an
 *   {@code .animation.json}), {@code info <npc>} shows model, clips and the problems found.</li>
 * </ul>
 */
public final class CommandRwRig extends CommandBase {
    @Override public String getName() { return "rwrig"; }

    @Override public String getUsage(ICommandSender sender) {
        return "/rwrig models | reload | set <npc|@near> <model> | clear <npc|@near> | play <npc|@near> <clip> [fadeTicks]"
                + " | queue <npc|@near> <clip> | stop <npc|@near> | apply <npc|@near> <package.json> | clips <npc|@near> <file.animation.json> | info <npc|@near>";
    }

    @Override public int getRequiredPermissionLevel() { return 2; }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws net.minecraft.command.CommandException {
        String action = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        if (action.equals("models")) {
            for (Map.Entry<String, String> e : RigModels.describe().entrySet()) { say(sender, "RWRIG model " + e.getKey() + ": " + e.getValue()); }
            return;
        }
        if (action.equals("reload")) {
            RigModels.reload();
            int npcs = 0;
            for (net.minecraft.world.WorldServer w : server.worlds) {
                for (Entity e : new ArrayList<Entity>(w.loadedEntityList)) {
                    if (e instanceof EntityNPCInterface && ((EntityNPCInterface) e).rwRig.active()) {
                        ((EntityNPCInterface) e).rwRigPlayer.stop((EntityNPCInterface) e);
                        ((EntityNPCInterface) e).updateClient();
                        npcs++;
                    }
                }
            }
            RigNet.sendReload(server);
            say(sender, "RWRIG reload models=" + RigModels.ids().size() + " npcs=" + npcs);
            return;
        }
        if (args.length < 2) { throw new WrongUsageException(getUsage(sender)); }
        List<EntityNPCInterface> npcs = find(sender, args[1]);
        if (npcs.isEmpty()) { throw new WrongUsageException("/rwrig: no NPC '" + args[1] + "' here"); }
        int ok = 0;
        for (EntityNPCInterface npc : npcs) {
            switch (action) {
                case "set": {
                    if (args.length != 3) { throw new WrongUsageException(getUsage(sender)); }
                    if (RigModels.get(args[2]) == null) { throw new WrongUsageException("/rwrig: unknown model '" + args[2] + "' (see /rwrig models)"); }
                    if (RigApi.setModel(npc.wrappedNPC, args[2])) { ok++; }
                    break;
                }
                case "clear": if (RigApi.setModel(npc.wrappedNPC, "")) { ok++; } break;
                case "play": {
                    if (args.length < 3 || args.length > 4) { throw new WrongUsageException(getUsage(sender)); }
                    int fade = args.length == 4 ? parseInt(args[3], 0, 200) : 0;
                    if (npc.rwRigPlayer.play(npc, args[2], fade)) { ok++; }
                    break;
                }
                case "queue": {
                    if (args.length != 3) { throw new WrongUsageException(getUsage(sender)); }
                    if (npc.rwRigPlayer.queue(npc, args[2])) { ok++; }
                    break;
                }
                case "stop": if (npc.rwRigPlayer.stop(npc)) { ok++; } break;
                case "apply": {
                    if (args.length != 3 || !args[2].matches("[A-Za-z0-9_.\\-]+")) { throw new WrongUsageException(getUsage(sender)); }
                    File f = new File(RigModels.folder(), args[2]);
                    String text;
                    try {
                        text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                    } catch (java.io.IOException e) {
                        throw new WrongUsageException("/rwrig apply: cannot read " + f);
                    }
                    String[] problems = RigApi.applyPackage(npc.wrappedNPC, text);
                    for (String p : problems) { say(sender, "RWRIG package problem: " + p); }
                    if (problems.length == 0 || !problems[0].startsWith("unknown model") && !problems[0].startsWith("package is not")) { ok++; }
                    break;
                }
                case "clips": {
                    if (args.length != 3 || !args[2].matches("[A-Za-z0-9_.\\-]+")) { throw new WrongUsageException(getUsage(sender)); }
                    File f = new File(RigModels.folder(), args[2]);
                    String text;
                    try {
                        text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                    } catch (java.io.IOException e) {
                        throw new WrongUsageException("/rwrig clips: cannot read " + f);
                    }
                    String[] errors = RigApi.setClips(npc.wrappedNPC, text);
                    for (String e : errors) { say(sender, "RWRIG clip problem: " + e); }
                    ok++;
                    break;
                }
                case "info": {
                    RigData r = npc.rwRig;
                    r.library();
                    say(sender, "RWRIG info " + npc.getName() + " model=" + (r.active() ? r.model : "-") + " playing=" + npc.rwRigPlayer.clip()
                            + " clips=" + String.join(",", r.library().sortedClipNames()) + " own=" + r.clips.length() + " chars"
                            + (r.errors().isEmpty() ? "" : " problems=" + r.errors()));
                    ok++;
                    break;
                }
                default: throw new WrongUsageException(getUsage(sender));
            }
        }
        say(sender, "RWRIG " + action + " ok=" + ok + "/" + npcs.size());
    }

    static List<EntityNPCInterface> find(ICommandSender sender, String who) {
        List<EntityNPCInterface> out = new ArrayList<EntityNPCInterface>();
        if (who.equals("@near")) {
            BlockPos p = sender.getPosition();
            EntityNPCInterface best = null;
            double bestD = 16 * 16;
            for (Entity e : sender.getEntityWorld().loadedEntityList) {
                if (!(e instanceof EntityNPCInterface)) { continue; }
                double d = e.getDistanceSq(p);
                if (d < bestD) { bestD = d; best = (EntityNPCInterface) e; }
            }
            if (best != null) { out.add(best); }
            return out;
        }
        for (Entity e : sender.getEntityWorld().loadedEntityList) {
            if (e instanceof EntityNPCInterface && e.getName().startsWith(who)) { out.add((EntityNPCInterface) e); }
        }
        return out;
    }

    private static void say(ICommandSender sender, String line) {
        sender.sendMessage(new TextComponentString(line));
        LogWriter.info(line);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "models", "reload", "set", "clear", "play", "queue", "stop", "apply", "clips", "info");
        }
        if (args.length == 2 && !args[0].equals("models") && !args[0].equals("reload")) { return getListOfStringsMatchingLastWord(args, "@near"); }
        if (args.length == 3 && args[0].equals("set")) { return getListOfStringsMatchingLastWord(args, RigModels.ids()); }
        if (args.length == 3 && (args[0].equals("play") || args[0].equals("queue"))) {
            List<EntityNPCInterface> npcs = find(sender, args[1]);
            if (!npcs.isEmpty()) { return getListOfStringsMatchingLastWord(args, npcs.get(0).rwRig.library().sortedClipNames()); }
        }
        return Collections.emptyList();
    }
}
