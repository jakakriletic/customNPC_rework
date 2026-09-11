/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.command.CommandBase
 *  net.minecraft.command.CommandException
 *  net.minecraft.command.ICommandSender
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.util.math.BlockPos
 */
package noppes.npcs.command;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import noppes.npcs.api.CommandNoppesBase;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.command.CmdClone;
import noppes.npcs.command.CmdConfig;
import noppes.npcs.command.CmdDialog;
import noppes.npcs.command.CmdFaction;
import noppes.npcs.command.CmdHelp;
import noppes.npcs.command.CmdMark;
import noppes.npcs.command.CmdNPC;
import noppes.npcs.command.CmdQuest;
import noppes.npcs.command.CmdScene;
import noppes.npcs.command.CmdSchematics;
import noppes.npcs.command.CmdScript;
import noppes.npcs.command.CmdSlay;

public class CommandNoppes
extends CommandBase {
    public Map<String, CommandNoppesBase> map = new HashMap<String, CommandNoppesBase>();
    public CmdHelp help = new CmdHelp(this);

    public CommandNoppes() {
        this.registerCommand(this.help);
        this.registerCommand(new CmdScript());
        this.registerCommand(new CmdScene());
        this.registerCommand(new CmdSlay());
        this.registerCommand(new CmdQuest());
        this.registerCommand(new CmdDialog());
        this.registerCommand(new CmdSchematics());
        this.registerCommand(new CmdFaction());
        this.registerCommand(new CmdNPC());
        this.registerCommand(new CmdClone());
        this.registerCommand(new CmdConfig());
        this.registerCommand(new CmdMark());
    }

    public void registerCommand(CommandNoppesBase command) {
        String name = command.func_71517_b().toLowerCase();
        if (this.map.containsKey(name)) {
            throw new CustomNPCsException("Already a subcommand with the name: " + name, new Object[0]);
        }
        this.map.put(name, command);
    }

    public String func_71517_b() {
        return "noppes";
    }

    public String func_71518_a(ICommandSender sender) {
        return "Use as /noppes subcommand";
    }

    public void func_184881_a(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            this.help.func_184881_a(server, sender, args);
            return;
        }
        CommandNoppesBase command = this.getCommand(args);
        if (command == null) {
            throw new CommandException("Unknown command " + args[0], new Object[0]);
        }
        args = Arrays.copyOfRange(args, 1, args.length);
        if (command.subcommands.isEmpty() || !command.runSubCommands()) {
            if (!sender.func_70003_b(command.func_82362_a(), "commands.noppes." + command.func_71517_b().toLowerCase())) {
                throw new CommandException("You are not allowed to use this command", new Object[0]);
            }
            command.canRun(server, sender, command.getUsage(), args);
            command.func_184881_a(server, sender, args);
            return;
        }
        if (args.length == 0) {
            this.help.func_184881_a(server, sender, new String[]{command.func_71517_b()});
            return;
        }
        command.executeSub(server, sender, args[0], Arrays.copyOfRange(args, 1, args.length));
    }

    public List func_184883_a(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        String usage;
        Method m;
        if (args.length == 1) {
            return CommandBase.func_175762_a((String[])args, this.map.keySet());
        }
        CommandNoppesBase command = this.getCommand(args);
        if (command == null) {
            return null;
        }
        if (args.length == 2 && command.runSubCommands()) {
            return CommandBase.func_175762_a((String[])args, command.subcommands.keySet());
        }
        String[] useArgs = command.getUsage().split(" ");
        if (command.runSubCommands() && (m = command.subcommands.get(args[1].toLowerCase())) != null) {
            useArgs = m.getAnnotation(CommandNoppesBase.SubCommand.class).usage().split(" ");
        }
        if (args.length <= useArgs.length + 2 && ((usage = useArgs[args.length - 3]).equals("<player>") || usage.equals("[player]"))) {
            return CommandBase.func_71530_a((String[])args, (String[])server.func_71213_z());
        }
        return command.func_184883_a(server, sender, Arrays.copyOfRange(args, 1, args.length), pos);
    }

    public CommandNoppesBase getCommand(String[] args) {
        if (args.length == 0) {
            return null;
        }
        return this.map.get(args[0].toLowerCase());
    }

    public int func_82362_a() {
        return 2;
    }
}

