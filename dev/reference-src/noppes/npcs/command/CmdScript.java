/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.command.ICommandSender
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentString
 *  net.minecraft.world.WorldServer
 */
package noppes.npcs.command;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import noppes.npcs.EventHooks;
import noppes.npcs.api.CommandNoppesBase;
import noppes.npcs.api.IPos;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.event.WorldEvent;
import noppes.npcs.controllers.ScriptController;

public class CmdScript
extends CommandNoppesBase {
    @CommandNoppesBase.SubCommand(desc="Reload scripts and saved data from disks script folder.")
    public Boolean reload(MinecraftServer server, ICommandSender sender, String[] args) {
        ScriptController.Instance.loadCategories();
        if (ScriptController.Instance.loadPlayerScripts()) {
            sender.sendMessage((ITextComponent)new TextComponentString("Reload player scripts succesfully"));
        } else {
            sender.sendMessage((ITextComponent)new TextComponentString("Failed reloading player scripts"));
        }
        if (ScriptController.Instance.loadForgeScripts()) {
            sender.sendMessage((ITextComponent)new TextComponentString("Reload forge scripts succesfully"));
        } else {
            sender.sendMessage((ITextComponent)new TextComponentString("Failed reloading forge scripts"));
        }
        if (ScriptController.Instance.loadStoredData()) {
            sender.sendMessage((ITextComponent)new TextComponentString("Reload stored data succesfully"));
        } else {
            sender.sendMessage((ITextComponent)new TextComponentString("Failed reloading stored data"));
        }
        return true;
    }

    @CommandNoppesBase.SubCommand(desc="Runs scriptCommand in the players scripts", usage="[args]")
    public Boolean run(MinecraftServer server, ICommandSender sender, String[] args) {
        IWorld world = NpcAPI.Instance().getIWorld((WorldServer)sender.getEntityWorld());
        BlockPos bpos = sender.getPosition();
        IPos pos = NpcAPI.Instance().getIPos(bpos.getX(), bpos.getY(), bpos.getZ());
        WorldEvent.ScriptCommandEvent event = new WorldEvent.ScriptCommandEvent(world, pos, args);
        EventHooks.onWorldScriptEvent(event);
        return true;
    }

    public String getName() {
        return "script";
    }

    @Override
    public String getDescription() {
        return "Commands for scripts";
    }
}

