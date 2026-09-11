/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.command.CommandBase
 *  net.minecraft.command.CommandException
 *  net.minecraft.command.ICommandSender
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentString
 */
package noppes.npcs.command;

import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import noppes.npcs.Server;
import noppes.npcs.api.CommandNoppesBase;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.controllers.PlayerDataController;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.SyncController;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.util.ValueUtil;

public class CmdQuest
extends CommandNoppesBase {
    public String func_71517_b() {
        return "quest";
    }

    @Override
    public String getDescription() {
        return "Quest operations";
    }

    @CommandNoppesBase.SubCommand(desc="Start a quest", usage="<player> <quest>", permission=2)
    public void start(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        int questid;
        String playername = args[0];
        try {
            questid = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException ex) {
            throw new CommandException("QuestID must be an integer", new Object[0]);
        }
        List<PlayerData> data = PlayerDataController.instance.getPlayersData(sender, playername);
        if (data.isEmpty()) {
            throw new CommandException("Unknow player '%s'", new Object[]{playername});
        }
        Quest quest = QuestController.instance.quests.get(questid);
        if (quest == null) {
            throw new CommandException("Unknown QuestID", new Object[0]);
        }
        for (PlayerData playerdata : data) {
            QuestData questdata = new QuestData(quest);
            playerdata.questData.activeQuests.put(questid, questdata);
            playerdata.save(true);
            Server.sendData((EntityPlayerMP)playerdata.player, EnumPacketClient.MESSAGE, "quest.newquest", quest.title, 2);
            Server.sendData((EntityPlayerMP)playerdata.player, EnumPacketClient.CHAT, "quest.newquest", ": ", quest.title);
        }
    }

    @CommandNoppesBase.SubCommand(desc="Finish a quest", usage="<player> <quest>")
    public void finish(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        int questid;
        String playername = args[0];
        try {
            questid = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException ex) {
            throw new CommandException("QuestID must be an integer", new Object[0]);
        }
        List<PlayerData> data = PlayerDataController.instance.getPlayersData(sender, playername);
        if (data.isEmpty()) {
            throw new CommandException(String.format("Unknow player '%s'", playername), new Object[0]);
        }
        Quest quest = QuestController.instance.quests.get(questid);
        if (quest == null) {
            throw new CommandException("Unknown QuestID", new Object[0]);
        }
        for (PlayerData playerdata : data) {
            playerdata.questData.finishedQuests.put(questid, System.currentTimeMillis());
            playerdata.save(true);
        }
    }

    @CommandNoppesBase.SubCommand(desc="Stop a started quest", usage="<player> <quest>", permission=2)
    public void stop(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        int questid;
        String playername = args[0];
        try {
            questid = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException ex) {
            throw new CommandException("QuestID must be an integer", new Object[0]);
        }
        List<PlayerData> data = PlayerDataController.instance.getPlayersData(sender, playername);
        if (data.isEmpty()) {
            throw new CommandException(String.format("Unknow player '%s'", playername), new Object[0]);
        }
        Quest quest = QuestController.instance.quests.get(questid);
        if (quest == null) {
            throw new CommandException("Unknown QuestID", new Object[0]);
        }
        for (PlayerData playerdata : data) {
            playerdata.questData.activeQuests.remove(questid);
            playerdata.save(true);
        }
    }

    @CommandNoppesBase.SubCommand(desc="Removes a quest from finished and active quests", usage="<player> <quest>", permission=2)
    public void remove(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        int questid;
        String playername = args[0];
        try {
            questid = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException ex) {
            throw new CommandException("QuestID must be an integer", new Object[0]);
        }
        List<PlayerData> data = PlayerDataController.instance.getPlayersData(sender, playername);
        if (data.isEmpty()) {
            throw new CommandException(String.format("Unknow player '%s'", playername), new Object[0]);
        }
        Quest quest = QuestController.instance.quests.get(questid);
        if (quest == null) {
            throw new CommandException("Unknown QuestID", new Object[0]);
        }
        for (PlayerData playerdata : data) {
            playerdata.questData.activeQuests.remove(questid);
            playerdata.questData.finishedQuests.remove(questid);
            playerdata.save(true);
        }
    }

    @CommandNoppesBase.SubCommand(desc="get/set objectives for quests progress", usage="<player> <quest> [objective] [value]", permission=2)
    public void objective(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        int value;
        int objective;
        int questid;
        EntityPlayerMP player = CommandBase.func_184888_a((MinecraftServer)server, (ICommandSender)sender, (String)args[0]);
        try {
            questid = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException ex) {
            throw new CommandException("QuestID must be an integer", new Object[0]);
        }
        Quest quest = QuestController.instance.quests.get(questid);
        if (quest == null) {
            throw new CommandException("Unknown QuestID", new Object[0]);
        }
        PlayerData data = PlayerData.get((EntityPlayer)player);
        if (!data.questData.activeQuests.containsKey(quest.id)) {
            throw new CommandException("Player doesnt have quest active", new Object[0]);
        }
        IQuestObjective[] objectives = quest.questInterface.getObjectives((EntityPlayer)player);
        if (args.length <= 2) {
            for (IQuestObjective ob : objectives) {
                sender.func_145747_a((ITextComponent)new TextComponentString(ob.getText()));
            }
            return;
        }
        try {
            objective = Integer.parseInt(args[2]);
        }
        catch (NumberFormatException ex) {
            throw new CommandException("Objective must be an integer. Most often 0, 1 or 2", new Object[0]);
        }
        if (objective < 0 || objective >= objectives.length) {
            throw new CommandException("Invalid objective number was given", new Object[0]);
        }
        if (args.length <= 3) {
            sender.func_145747_a((ITextComponent)new TextComponentString(objectives[objective].getText()));
            return;
        }
        IQuestObjective object = objectives[objective];
        String s = args[3];
        try {
            value = Integer.parseInt(args[3]);
        }
        catch (NumberFormatException ex) {
            throw new CommandException("Value must be an integer.", new Object[0]);
        }
        if (s.startsWith("-") || s.startsWith("+")) {
            value = ValueUtil.CorrectInt(object.getProgress() + value, 0, object.getMaxProgress());
        }
        object.setProgress(value);
    }

    @CommandNoppesBase.SubCommand(desc="reload quests from disk", permission=4)
    public void reload(MinecraftServer server, ICommandSender sender, String[] args) {
        new QuestController().load();
        SyncController.syncAllQuests(server);
    }
}

