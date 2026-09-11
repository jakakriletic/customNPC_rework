/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  io.netty.buffer.Unpooled
 *  net.minecraft.command.ICommandSender
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.init.SoundEvents
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.network.PacketBuffer
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentString
 *  net.minecraft.world.Teleporter
 *  net.minecraft.world.WorldServer
 *  net.minecraftforge.common.util.FakePlayer
 *  net.minecraftforge.fml.common.network.internal.FMLProxyPacket
 *  net.minecraftforge.oredict.OreDictionary
 */
package noppes.npcs;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.oredict.OreDictionary;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomTeleporter;
import noppes.npcs.EventHooks;
import noppes.npcs.NoppesStringUtils;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.Server;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.event.QuestEvent;
import noppes.npcs.api.event.RoleEvent;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.constants.EnumPlayerPacket;
import noppes.npcs.containers.ContainerNPCBankInterface;
import noppes.npcs.containers.ContainerNPCFollower;
import noppes.npcs.containers.ContainerNPCFollowerHire;
import noppes.npcs.controllers.BankController;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.PlayerDataController;
import noppes.npcs.controllers.PlayerQuestController;
import noppes.npcs.controllers.data.Bank;
import noppes.npcs.controllers.data.BankData;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.DialogOption;
import noppes.npcs.controllers.data.Line;
import noppes.npcs.controllers.data.PlayerBankData;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerQuestData;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleCompanion;
import noppes.npcs.roles.RoleDialog;
import noppes.npcs.roles.RoleFollower;

public class NoppesUtilPlayer {
    public static void changeFollowerState(EntityPlayerMP player, EntityNPCInterface npc) {
        if (npc.advanced.role != 2) {
            return;
        }
        RoleFollower role = (RoleFollower)npc.roleInterface;
        EntityPlayer owner = role.owner;
        if (owner == null || !owner.func_70005_c_().equals(player.func_70005_c_())) {
            return;
        }
        role.isFollowing = !role.isFollowing;
    }

    public static void hireFollower(EntityPlayerMP player, EntityNPCInterface npc) {
        if (npc.advanced.role != 2) {
            return;
        }
        Container con = player.field_71070_bA;
        if (con == null || !(con instanceof ContainerNPCFollowerHire)) {
            return;
        }
        ContainerNPCFollowerHire container = (ContainerNPCFollowerHire)con;
        RoleFollower role = (RoleFollower)npc.roleInterface;
        NoppesUtilPlayer.followerBuy(role, (IInventory)container.currencyMatrix, player, npc);
    }

    public static void extendFollower(EntityPlayerMP player, EntityNPCInterface npc) {
        if (npc.advanced.role != 2) {
            return;
        }
        Container con = player.field_71070_bA;
        if (con == null || !(con instanceof ContainerNPCFollower)) {
            return;
        }
        ContainerNPCFollower container = (ContainerNPCFollower)con;
        RoleFollower role = (RoleFollower)npc.roleInterface;
        NoppesUtilPlayer.followerBuy(role, container.currencyMatrix, player, npc);
    }

    public static void teleportPlayer(EntityPlayerMP player, double x, double y, double z, int dimension) {
        if (player.field_71093_bK != dimension) {
            int dim = player.field_71093_bK;
            MinecraftServer server = player.func_184102_h();
            WorldServer wor = server.func_71218_a(dimension);
            if (wor == null) {
                player.func_145747_a((ITextComponent)new TextComponentString("Broken transporter. Dimenion does not exist"));
                return;
            }
            player.func_70012_b(x, y, z, player.field_70177_z, player.field_70125_A);
            server.func_184103_al().transferPlayerToDimension(player, dimension, (Teleporter)new CustomTeleporter(wor));
            player.field_71135_a.func_147364_a(x, y, z, player.field_70177_z, player.field_70125_A);
            if (!wor.field_73010_i.contains(player)) {
                wor.func_72838_d((Entity)player);
            }
        } else {
            player.field_71135_a.func_147364_a(x, y, z, player.field_70177_z, player.field_70125_A);
        }
        player.field_70170_p.func_72866_a((Entity)player, false);
    }

    private static void followerBuy(RoleFollower role, IInventory currencyInv, EntityPlayerMP player, EntityNPCInterface npc) {
        ItemStack currency = currencyInv.func_70301_a(0);
        if (currency == null || currency.func_190926_b()) {
            return;
        }
        HashMap<ItemStack, Integer> cd = new HashMap<ItemStack, Integer>();
        for (int slot = 0; slot < role.inventory.items.size(); ++slot) {
            ItemStack is = (ItemStack)role.inventory.items.get(slot);
            if (is.func_190926_b() || is.func_77973_b() != currency.func_77973_b() || is.func_77981_g() && is.func_77952_i() != currency.func_77952_i()) continue;
            int days = 1;
            if (role.rates.containsKey(slot)) {
                days = role.rates.get(slot);
            }
            cd.put(is, days);
        }
        if (cd.size() == 0) {
            return;
        }
        int stackSize = currency.func_190916_E();
        int days = 0;
        int possibleDays = 0;
        int possibleSize = stackSize;
        while (true) {
            for (ItemStack item : cd.keySet()) {
                int newStackSize;
                int size;
                int posDays;
                int rDays = (Integer)cd.get(item);
                int rValue = item.func_190916_E();
                if (rValue > stackSize || possibleDays > (posDays = (size = stackSize - (newStackSize = stackSize % rValue)) / rValue * rDays)) continue;
                possibleDays = posDays;
                possibleSize = newStackSize;
            }
            if (stackSize == possibleSize) break;
            stackSize = possibleSize;
            days += possibleDays;
            possibleDays = 0;
        }
        RoleEvent.FollowerHireEvent event = new RoleEvent.FollowerHireEvent((EntityPlayer)player, npc.wrappedNPC, days);
        if (EventHooks.onNPCRole(npc, event)) {
            return;
        }
        if (event.days == 0) {
            return;
        }
        if (stackSize <= 0) {
            currencyInv.func_70299_a(0, ItemStack.field_190927_a);
        } else {
            currency = currency.func_77979_a(stackSize);
        }
        npc.say((EntityPlayer)player, new Line(NoppesStringUtils.formatText(role.dialogHire.replace("{days}", days + ""), new Object[]{player, npc})));
        role.setOwner((EntityPlayer)player);
        role.addDays(days);
    }

    public static void bankUpgrade(EntityPlayerMP player, EntityNPCInterface npc) {
        if (npc.advanced.role != 3) {
            return;
        }
        Container con = player.field_71070_bA;
        if (con == null || !(con instanceof ContainerNPCBankInterface)) {
            return;
        }
        ContainerNPCBankInterface container = (ContainerNPCBankInterface)con;
        Bank bank = BankController.getInstance().getBank(container.bankid);
        ItemStack item = bank.upgradeInventory.func_70301_a(container.slot);
        if (item == null || item.func_190926_b()) {
            return;
        }
        int price = item.func_190916_E();
        ItemStack currency = container.currencyMatrix.func_70301_a(0);
        if (currency == null || currency.func_190926_b() || price > currency.func_190916_E()) {
            return;
        }
        if (currency.func_190916_E() - price == 0) {
            container.currencyMatrix.func_70299_a(0, ItemStack.field_190927_a);
        } else {
            currency = currency.func_77979_a(price);
        }
        player.func_71128_l();
        PlayerBankData data = PlayerDataController.instance.getBankData((EntityPlayer)player, bank.id);
        BankData bankData = data.getBank(bank.id);
        bankData.upgradedSlots.put(container.slot, true);
        RoleEvent.BankUpgradedEvent event = new RoleEvent.BankUpgradedEvent((EntityPlayer)player, npc.wrappedNPC, container.slot);
        EventHooks.onNPCRole(npc, event);
        bankData.openBankGui((EntityPlayer)player, npc, bank.id, container.slot);
    }

    public static void bankUnlock(EntityPlayerMP player, EntityNPCInterface npc) {
        if (npc.advanced.role != 3) {
            return;
        }
        Container con = player.field_71070_bA;
        if (con == null || !(con instanceof ContainerNPCBankInterface)) {
            return;
        }
        ContainerNPCBankInterface container = (ContainerNPCBankInterface)con;
        Bank bank = BankController.getInstance().getBank(container.bankid);
        ItemStack item = bank.currencyInventory.func_70301_a(container.slot);
        if (item == null || item.func_190926_b()) {
            return;
        }
        int price = item.func_190916_E();
        ItemStack currency = container.currencyMatrix.func_70301_a(0);
        if (currency == null || currency.func_190926_b() || price > currency.func_190916_E()) {
            return;
        }
        if (currency.func_190916_E() - price == 0) {
            container.currencyMatrix.func_70299_a(0, ItemStack.field_190927_a);
        } else {
            currency = currency.func_77979_a(price);
        }
        player.func_71128_l();
        PlayerBankData data = PlayerDataController.instance.getBankData((EntityPlayer)player, bank.id);
        BankData bankData = data.getBank(bank.id);
        if (bankData.unlockedSlots + 1 <= bank.maxSlots) {
            ++bankData.unlockedSlots;
        }
        RoleEvent.BankUnlockedEvent event = new RoleEvent.BankUnlockedEvent((EntityPlayer)player, npc.wrappedNPC, container.slot);
        EventHooks.onNPCRole(npc, event);
        bankData.openBankGui((EntityPlayer)player, npc, bank.id, container.slot);
    }

    public static void sendData(EnumPlayerPacket enu, Object ... obs) {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        try {
            if (!Server.fillBuffer((ByteBuf)buffer, enu, obs)) {
                return;
            }
            CustomNpcs.ChannelPlayer.sendToServer(new FMLProxyPacket(buffer, "CustomNPCsPlayer"));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void dialogSelected(int diaId, int optionId, EntityPlayerMP player, EntityNPCInterface npc) {
        PlayerData data = PlayerData.get((EntityPlayer)player);
        if (data.dialogId != diaId) {
            return;
        }
        if (data.dialogId < 0 && npc.advanced.role == 7) {
            String text = ((RoleDialog)npc.roleInterface).optionsTexts.get(optionId);
            if (text != null && !text.isEmpty()) {
                Dialog d = new Dialog(null);
                d.text = text;
                NoppesUtilServer.openDialog((EntityPlayer)player, npc, d);
            }
            return;
        }
        Dialog dialog = DialogController.instance.dialogs.get(data.dialogId);
        if (dialog == null) {
            return;
        }
        if (!dialog.hasDialogs((EntityPlayer)player) && !dialog.hasOtherOptions()) {
            NoppesUtilPlayer.closeDialog(player, npc, true);
            return;
        }
        DialogOption option = dialog.options.get(optionId);
        if (option == null || EventHooks.onNPCDialogOption(npc, player, dialog, option) || option.optionType == 1 && (!option.isAvailable((EntityPlayer)player) || !option.hasDialog()) || option.optionType == 2 || option.optionType == 0) {
            NoppesUtilPlayer.closeDialog(player, npc, true);
            return;
        }
        if (option.optionType == 3) {
            NoppesUtilPlayer.closeDialog(player, npc, true);
            if (npc.roleInterface != null) {
                if (npc.advanced.role == 6) {
                    ((RoleCompanion)npc.roleInterface).interact((EntityPlayer)player, true);
                } else {
                    npc.roleInterface.interact((EntityPlayer)player);
                }
            }
        } else if (option.optionType == 1) {
            NoppesUtilPlayer.closeDialog(player, npc, false);
            NoppesUtilServer.openDialog((EntityPlayer)player, npc, option.getDialog());
        } else if (option.optionType == 4) {
            NoppesUtilPlayer.closeDialog(player, npc, true);
            NoppesUtilServer.runCommand(npc, npc.func_70005_c_(), option.command, (EntityPlayer)player);
        } else {
            NoppesUtilPlayer.closeDialog(player, npc, true);
        }
    }

    public static void closeDialog(EntityPlayerMP player, EntityNPCInterface npc, boolean notifyClient) {
        PlayerData data = PlayerData.get((EntityPlayer)player);
        Dialog dialog = DialogController.instance.dialogs.get(data.dialogId);
        EventHooks.onNPCDialogClose(npc, player, dialog);
        if (notifyClient) {
            Server.sendData(player, EnumPacketClient.GUI_CLOSE, -1, new NBTTagCompound());
        }
        data.dialogId = -1;
    }

    public static void questCompletion(EntityPlayerMP player, int questId) {
        PlayerData data = PlayerData.get((EntityPlayer)player);
        PlayerQuestData playerdata = data.questData;
        QuestData questdata = playerdata.activeQuests.get(questId);
        if (questdata == null) {
            return;
        }
        Quest quest = questdata.quest;
        if (!quest.questInterface.isCompleted((EntityPlayer)player)) {
            return;
        }
        QuestEvent.QuestTurnedInEvent event = new QuestEvent.QuestTurnedInEvent(data.scriptData.getPlayer(), quest);
        event.expReward = quest.rewardExp;
        ArrayList<IItemStack> list = new ArrayList<IItemStack>();
        for (ItemStack item : quest.rewardItems.items) {
            if (item.func_190926_b()) continue;
            list.add(NpcAPI.Instance().getIItemStack(item));
        }
        if (!quest.randomReward) {
            event.itemRewards = list.toArray(new IItemStack[list.size()]);
        } else if (!list.isEmpty()) {
            event.itemRewards = new IItemStack[]{(IItemStack)list.get(player.func_70681_au().nextInt(list.size()))};
        }
        EventHooks.onQuestTurnedIn(data.scriptData, event);
        for (IItemStack item : event.itemRewards) {
            if (item == null) continue;
            NoppesUtilServer.GivePlayerItem((Entity)player, (EntityPlayer)player, item.getMCItemStack());
        }
        quest.questInterface.handleComplete((EntityPlayer)player);
        if (event.expReward > 0) {
            NoppesUtilServer.playSound((EntityLivingBase)player, SoundEvents.field_187604_bf, 0.1f, 0.5f * ((player.field_70170_p.field_73012_v.nextFloat() - player.field_70170_p.field_73012_v.nextFloat()) * 0.7f + 1.8f));
            player.func_71023_q(event.expReward);
        }
        quest.factionOptions.addPoints((EntityPlayer)player);
        if (quest.mail.isValid()) {
            PlayerDataController.instance.addPlayerMessage(player.func_184102_h(), player.func_70005_c_(), quest.mail);
        }
        if (!quest.command.isEmpty()) {
            FakePlayer cplayer = EntityNPCInterface.CommandPlayer;
            cplayer.func_70029_a(player.field_70170_p);
            cplayer.func_70107_b(player.field_70165_t, player.field_70163_u, player.field_70161_v);
            NoppesUtilServer.runCommand((ICommandSender)cplayer, "QuestCompletion", quest.command, (EntityPlayer)player);
        }
        PlayerQuestController.setQuestFinished(quest, (EntityPlayer)player);
        if (quest.hasNewQuest()) {
            PlayerQuestController.addActiveQuest(quest.getNextQuest(), (EntityPlayer)player);
        }
    }

    public static boolean compareItems(ItemStack item, ItemStack item2, boolean ignoreDamage, boolean ignoreNBT) {
        if (NoppesUtilServer.IsItemStackNull(item) || NoppesUtilServer.IsItemStackNull(item2)) {
            return false;
        }
        boolean oreMatched = false;
        OreDictionary.itemMatches((ItemStack)item, (ItemStack)item2, (boolean)false);
        int[] ids = OreDictionary.getOreIDs((ItemStack)item);
        if (ids.length > 0) {
            for (int id : ids) {
                boolean match1 = false;
                boolean match2 = false;
                for (ItemStack is : OreDictionary.getOres((String)OreDictionary.getOreName((int)id))) {
                    if (NoppesUtilPlayer.compareItemDetails(item, is, ignoreDamage, ignoreNBT)) {
                        match1 = true;
                    }
                    if (!NoppesUtilPlayer.compareItemDetails(item2, is, ignoreDamage, ignoreNBT)) continue;
                    match2 = true;
                }
                if (!match1 || !match2) continue;
                return true;
            }
        }
        return NoppesUtilPlayer.compareItemDetails(item, item2, ignoreDamage, ignoreNBT);
    }

    private static boolean compareItemDetails(ItemStack item, ItemStack item2, boolean ignoreDamage, boolean ignoreNBT) {
        if (item.func_77973_b() != item2.func_77973_b()) {
            return false;
        }
        if (!ignoreDamage && item.func_77952_i() != -1 && item.func_77952_i() != item2.func_77952_i()) {
            return false;
        }
        if (!(ignoreNBT || item.func_77978_p() == null || item2.func_77978_p() != null && item.func_77978_p().equals((Object)item2.func_77978_p()))) {
            return false;
        }
        return ignoreNBT || item2.func_77978_p() == null || item.func_77978_p() != null;
    }

    public static boolean compareItems(EntityPlayer player, ItemStack item, boolean ignoreDamage, boolean ignoreNBT) {
        int size = 0;
        for (int i = 0; i < player.field_71071_by.func_70302_i_(); ++i) {
            ItemStack is = player.field_71071_by.func_70301_a(i);
            if (NoppesUtilServer.IsItemStackNull(is) || !NoppesUtilPlayer.compareItems(item, is, ignoreDamage, ignoreNBT)) continue;
            size += is.func_190916_E();
        }
        return size >= item.func_190916_E();
    }

    public static void consumeItem(EntityPlayer player, ItemStack item, boolean ignoreDamage, boolean ignoreNBT) {
        if (NoppesUtilServer.IsItemStackNull(item)) {
            return;
        }
        int size = item.func_190916_E();
        for (int i = 0; i < player.field_71071_by.func_70302_i_(); ++i) {
            ItemStack is = player.field_71071_by.func_70301_a(i);
            if (NoppesUtilServer.IsItemStackNull(is) || !NoppesUtilPlayer.compareItems(item, is, ignoreDamage, ignoreNBT)) continue;
            if (size >= is.func_190916_E()) {
                size -= is.func_190916_E();
                player.field_71071_by.func_70299_a(i, ItemStack.field_190927_a);
                continue;
            }
            is.func_77979_a(size);
            break;
        }
    }

    public static List<ItemStack> countStacks(IInventory inv, boolean ignoreDamage, boolean ignoreNBT) {
        ArrayList<ItemStack> list = new ArrayList<ItemStack>();
        for (int i = 0; i < inv.func_70302_i_(); ++i) {
            ItemStack item = inv.func_70301_a(i);
            if (NoppesUtilServer.IsItemStackNull(item)) continue;
            boolean found = false;
            for (ItemStack is : list) {
                if (!NoppesUtilPlayer.compareItems(item, is, ignoreDamage, ignoreNBT)) continue;
                is.func_190920_e(is.func_190916_E() + item.func_190916_E());
                found = true;
                break;
            }
            if (found) continue;
            list.add(item.func_77946_l());
        }
        return list;
    }
}

