/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.command.CommandException
 *  net.minecraft.command.ICommandSender
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.server.MinecraftServer
 */
package noppes.npcs.command;

import java.util.List;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.server.MinecraftServer;
import noppes.npcs.api.CommandNoppesBase;
import noppes.npcs.controllers.data.MarkData;

public class CmdMark
extends CommandNoppesBase {
    public String getName() {
        return "mark";
    }

    @Override
    public String getDescription() {
        return "Mark operations";
    }

    @CommandNoppesBase.SubCommand(desc="Set mark (warning overrides existing marks)", usage="<@e> <type> [color]")
    public void set(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        List list = CmdMark.getEntityList((MinecraftServer)server, (ICommandSender)sender, (String)args[0]);
        int type = 0;
        try {
            type = Integer.parseInt(args[1]);
        }
        catch (Exception exception) {
            // empty catch block
        }
        int color = 0xFFFFFF;
        if (args.length > 2) {
            try {
                color = Integer.parseInt(args[2], 16);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        for (Entity e : list) {
            if (!(e instanceof EntityLivingBase)) continue;
            MarkData data = MarkData.get((EntityLivingBase)e);
            data.marks.clear();
            data.addMark(type, color);
        }
    }

    @CommandNoppesBase.SubCommand(desc="Clear mark", usage="<@e>")
    public void clear(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        List list = CmdMark.getEntityList((MinecraftServer)server, (ICommandSender)sender, (String)args[0]);
        for (Entity e : list) {
            if (!(e instanceof EntityLivingBase)) continue;
            MarkData data = MarkData.get((EntityLivingBase)e);
            data.marks.clear();
            data.syncClients();
        }
    }
}

