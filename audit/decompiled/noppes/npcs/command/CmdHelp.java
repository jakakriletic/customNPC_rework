/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.command.CommandException
 *  net.minecraft.command.ICommandSender
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentTranslation
 */
package noppes.npcs.command;

import java.lang.reflect.Method;
import java.util.Map;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import noppes.npcs.api.CommandNoppesBase;
import noppes.npcs.command.CommandNoppes;

public class CmdHelp
extends CommandNoppesBase {
    private CommandNoppes parent;

    public CmdHelp(CommandNoppes parent) {
        this.parent = parent;
    }

    public String func_71517_b() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "help [command]";
    }

    @Override
    public void func_184881_a(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            this.sendMessage(sender, "------Noppes Commands------", new Object[0]);
            for (Map.Entry<String, CommandNoppesBase> entry : this.parent.map.entrySet()) {
                this.sendMessage(sender, entry.getKey() + ": " + entry.getValue().func_71518_a(sender), new Object[0]);
            }
            return;
        }
        CommandNoppesBase command = this.parent.getCommand(args);
        if (command == null) {
            throw new CommandException("Unknown command " + args[0], new Object[0]);
        }
        if (command.subcommands.isEmpty()) {
            sender.func_145747_a((ITextComponent)new TextComponentTranslation(command.func_71518_a(sender), new Object[0]));
            return;
        }
        Method m = null;
        if (args.length > 1) {
            m = command.subcommands.get(args[1].toLowerCase());
        }
        if (m == null) {
            this.sendMessage(sender, "------" + command.func_71517_b() + " SubCommands------", new Object[0]);
            for (Map.Entry<String, Method> entry : command.subcommands.entrySet()) {
                sender.func_145747_a((ITextComponent)new TextComponentTranslation(entry.getKey() + ": " + entry.getValue().getAnnotation(CommandNoppesBase.SubCommand.class).desc(), new Object[0]));
            }
        } else {
            this.sendMessage(sender, "------" + command.func_71517_b() + "." + args[1].toLowerCase() + " Command------", new Object[0]);
            CommandNoppesBase.SubCommand sc = m.getAnnotation(CommandNoppesBase.SubCommand.class);
            sender.func_145747_a((ITextComponent)new TextComponentTranslation(sc.desc(), new Object[0]));
            if (!sc.usage().isEmpty()) {
                sender.func_145747_a((ITextComponent)new TextComponentTranslation("Usage: " + sc.usage(), new Object[0]));
            }
        }
    }
}

