/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.command.CommandException
 *  net.minecraft.command.EntitySelector
 *  net.minecraft.command.ICommandSender
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.server.MinecraftServer
 */
package noppes.npcs.controllers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.command.CommandException;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import noppes.npcs.CustomNpcs;
import noppes.npcs.LogWriter;
import noppes.npcs.controllers.BankController;
import noppes.npcs.controllers.data.Bank;
import noppes.npcs.controllers.data.PlayerBankData;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerMail;
import noppes.npcs.util.NBTJsonUtil;

public class PlayerDataController {
    public static PlayerDataController instance;
    public Map<String, String> nameUUIDs;

    public PlayerDataController() {
        instance = this;
        File dir = CustomNpcs.getWorldSaveDirectory("playerdata");
        HashMap<String, String> map = new HashMap<String, String>();
        for (File file : dir.listFiles()) {
            if (file.isDirectory() || !file.getName().endsWith(".json")) continue;
            try {
                NBTTagCompound compound = NBTJsonUtil.LoadFile(file);
                if (!compound.func_74764_b("PlayerName")) continue;
                map.put(compound.func_74779_i("PlayerName"), file.getName().substring(0, file.getName().length() - 5));
            }
            catch (Exception e) {
                LogWriter.error("Error loading: " + file.getAbsolutePath(), e);
            }
        }
        this.nameUUIDs = map;
    }

    public PlayerBankData getBankData(EntityPlayer player, int bankId) {
        Bank bank = BankController.getInstance().getBank(bankId);
        PlayerBankData data = PlayerData.get((EntityPlayer)player).bankData;
        if (!data.hasBank(bank.id)) {
            data.loadNew(bank.id);
        }
        return data;
    }

    public String hasPlayer(String username) {
        for (String name : this.nameUUIDs.keySet()) {
            if (!name.equalsIgnoreCase(username)) continue;
            return name;
        }
        return "";
    }

    public PlayerData getDataFromUsername(MinecraftServer server, String username) {
        EntityPlayerMP player = server.func_184103_al().func_152612_a(username);
        PlayerData data = null;
        if (player == null) {
            for (String name : this.nameUUIDs.keySet()) {
                if (!name.equalsIgnoreCase(username)) continue;
                data = new PlayerData();
                data.setNBT(PlayerData.loadPlayerData(this.nameUUIDs.get(name)));
                break;
            }
        } else {
            data = PlayerData.get((EntityPlayer)player);
        }
        return data;
    }

    public void addPlayerMessage(MinecraftServer server, String username, PlayerMail mail) {
        mail.time = System.currentTimeMillis();
        PlayerData data = this.getDataFromUsername(server, username);
        data.mailData.playermail.add(mail.copy());
        data.save(false);
    }

    public List<PlayerData> getPlayersData(ICommandSender sender, String username) throws CommandException {
        ArrayList<PlayerData> list = new ArrayList<PlayerData>();
        List players = EntitySelector.func_179656_b((ICommandSender)sender, (String)username, EntityPlayerMP.class);
        if (players.isEmpty()) {
            PlayerData data = this.getDataFromUsername(sender.func_184102_h(), username);
            if (data != null) {
                list.add(data);
            }
        } else {
            for (EntityPlayer player : players) {
                list.add(PlayerData.get(player));
            }
        }
        return list;
    }
}

