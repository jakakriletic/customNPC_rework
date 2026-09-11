/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.command.CommandBase
 *  net.minecraft.command.CommandException
 *  net.minecraft.command.ICommandSender
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentTranslation
 */
package noppes.npcs.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public abstract class CommandNoppesBase
extends CommandBase {
    public Map<String, Method> subcommands = new HashMap<String, Method>();

    public CommandNoppesBase() {
        for (Method m : ((Object)((Object)this)).getClass().getDeclaredMethods()) {
            SubCommand sc = m.getAnnotation(SubCommand.class);
            if (sc == null) continue;
            String name = sc.name();
            if (name.equals("")) {
                name = m.getName();
            }
            this.subcommands.put(name.toLowerCase(), m);
        }
    }

    public void func_184881_a(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
    }

    public String func_71518_a(ICommandSender sender) {
        return this.getDescription();
    }

    public abstract String getDescription();

    public String getUsage() {
        return "";
    }

    public boolean runSubCommands() {
        return !this.subcommands.isEmpty();
    }

    protected void sendMessage(ICommandSender sender, String message, Object ... obs) {
        sender.func_145747_a((ITextComponent)new TextComponentTranslation(message, obs));
    }

    public void executeSub(MinecraftServer server, ICommandSender sender, String command, String[] args) throws CommandException {
        Method m = this.subcommands.get(command.toLowerCase());
        if (m == null) {
            throw new CommandException("Unknown subcommand " + command, new Object[0]);
        }
        SubCommand sc = m.getAnnotation(SubCommand.class);
        if (!sender.func_70003_b(sc.permission(), "commands.noppes." + this.func_71517_b().toLowerCase() + "." + command.toLowerCase())) {
            throw new CommandException("You are not allowed to use this command", new Object[0]);
        }
        this.canRun(server, sender, sc.usage(), args);
        try {
            m.invoke((Object)this, server, sender, args);
        }
        catch (Exception e) {
            if (e.getCause() instanceof CommandException) {
                throw (CommandException)e.getCause();
            }
            e.printStackTrace();
        }
    }

    public void canRun(MinecraftServer server, ICommandSender sender, String usage, String[] args) throws CommandException {
        String[] np = usage.split(" ");
        ArrayList<String> required = new ArrayList<String>();
        for (int i = 0; i < np.length; ++i) {
            String command = np[i];
            if (command.startsWith("<")) {
                required.add(command);
            }
            if (!command.equals("<player>") || args.length <= i) continue;
            CommandBase.func_184888_a((MinecraftServer)server, (ICommandSender)sender, (String)args[i]);
        }
        if (args.length < required.size()) {
            throw new CommandException("Missing parameter: " + (String)required.get(args.length), new Object[0]);
        }
    }

    public int func_82362_a() {
        return 2;
    }

    @Retention(value=RetentionPolicy.RUNTIME)
    @Target(value={ElementType.METHOD})
    public static @interface SubCommand {
        public String name() default "";

        public String usage() default "";

        public String desc();

        public int permission() default 4;
    }
}

