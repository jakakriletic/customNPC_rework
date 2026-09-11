/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.server.MinecraftServer
 */
package noppes.npcs.controllers;

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NBTTags;
import noppes.npcs.Server;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.RecipeController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.DialogCategory;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.controllers.data.QuestCategory;
import noppes.npcs.controllers.data.RecipeCarpentry;
import noppes.npcs.items.ItemScripted;

public class SyncController {
    public static void syncPlayer(EntityPlayerMP player) {
        NBTTagList list = new NBTTagList();
        NBTTagCompound compound = new NBTTagCompound();
        for (Faction faction : FactionController.instance.factions.values()) {
            list.appendTag((NBTBase)faction.writeNBT(new NBTTagCompound()));
            if (list.tagCount() <= 20) continue;
            compound = new NBTTagCompound();
            compound.setTag("Data", (NBTBase)list);
            Server.sendData(player, EnumPacketClient.SYNC_ADD, 1, compound);
            list = new NBTTagList();
        }
        compound = new NBTTagCompound();
        compound.setTag("Data", (NBTBase)list);
        Server.sendData(player, EnumPacketClient.SYNC_END, 1, compound);
        for (QuestCategory questCategory : QuestController.instance.categories.values()) {
            Server.sendData(player, EnumPacketClient.SYNC_ADD, 3, questCategory.writeNBT(new NBTTagCompound()));
        }
        Server.sendData(player, EnumPacketClient.SYNC_END, 3, new NBTTagCompound());
        for (DialogCategory dialogCategory : DialogController.instance.categories.values()) {
            Server.sendData(player, EnumPacketClient.SYNC_ADD, 5, dialogCategory.writeNBT(new NBTTagCompound()));
        }
        Server.sendData(player, EnumPacketClient.SYNC_END, 5, new NBTTagCompound());
        list = new NBTTagList();
        for (RecipeCarpentry recipeCarpentry : RecipeController.instance.globalRecipes.values()) {
            list.appendTag((NBTBase)recipeCarpentry.writeNBT());
            if (list.tagCount() <= 10) continue;
            compound = new NBTTagCompound();
            compound.setTag("Data", (NBTBase)list);
            Server.sendData(player, EnumPacketClient.SYNC_ADD, 6, compound);
            list = new NBTTagList();
        }
        compound = new NBTTagCompound();
        compound.setTag("Data", (NBTBase)list);
        Server.sendData(player, EnumPacketClient.SYNC_END, 6, compound);
        list = new NBTTagList();
        for (RecipeCarpentry recipeCarpentry : RecipeController.instance.anvilRecipes.values()) {
            list.appendTag((NBTBase)recipeCarpentry.writeNBT());
            if (list.tagCount() <= 10) continue;
            compound = new NBTTagCompound();
            compound.setTag("Data", (NBTBase)list);
            Server.sendData(player, EnumPacketClient.SYNC_ADD, 7, compound);
            list = new NBTTagList();
        }
        compound = new NBTTagCompound();
        compound.setTag("Data", (NBTBase)list);
        Server.sendData(player, EnumPacketClient.SYNC_END, 7, compound);
        PlayerData data = PlayerData.get((EntityPlayer)player);
        Server.sendData(player, EnumPacketClient.SYNC_END, 8, data.getNBT());
        SyncController.syncScriptItems(player);
    }

    public static void syncAllDialogs(MinecraftServer server) {
        for (DialogCategory category : DialogController.instance.categories.values()) {
            Server.sendToAll(server, EnumPacketClient.SYNC_ADD, 5, category.writeNBT(new NBTTagCompound()));
        }
        Server.sendToAll(server, EnumPacketClient.SYNC_END, 5, new NBTTagCompound());
    }

    public static void syncAllQuests(MinecraftServer server) {
        for (QuestCategory category : QuestController.instance.categories.values()) {
            Server.sendToAll(server, EnumPacketClient.SYNC_ADD, 3, category.writeNBT(new NBTTagCompound()));
        }
        Server.sendToAll(server, EnumPacketClient.SYNC_END, 3, new NBTTagCompound());
    }

    public static void syncScriptItems(EntityPlayerMP player) {
        NBTTagCompound comp = new NBTTagCompound();
        comp.setTag("List", NBTTags.nbtIntegerStringMap(ItemScripted.Resources));
        Server.sendData(player, EnumPacketClient.SYNC_END, 9, comp);
    }

    public static void syncScriptItemsEverybody() {
        NBTTagCompound comp = new NBTTagCompound();
        comp.setTag("List", NBTTags.nbtIntegerStringMap(ItemScripted.Resources));
        for (EntityPlayerMP player : CustomNpcs.Server.getPlayerList().getPlayers()) {
            Server.sendData(player, EnumPacketClient.SYNC_END, 9, comp);
        }
    }

    public static void clientSync(int synctype, NBTTagCompound compound, boolean syncEnd) {
        if (synctype == 1) {
            NBTTagList list = compound.getTagList("Data", 10);
            for (int i = 0; i < list.tagCount(); ++i) {
                Faction faction = new Faction();
                faction.readNBT(list.getCompoundTagAt(i));
                FactionController.instance.factionsSync.put(faction.id, faction);
            }
            if (syncEnd) {
                FactionController.instance.factions = FactionController.instance.factionsSync;
                FactionController.instance.factionsSync = new HashMap();
            }
        } else if (synctype == 3) {
            if (!compound.hasNoTags()) {
                QuestCategory category = new QuestCategory();
                category.readNBT(compound);
                QuestController.instance.categoriesSync.put(category.id, category);
            }
            if (syncEnd) {
                HashMap<Integer, Quest> quests = new HashMap<Integer, Quest>();
                for (QuestCategory category : QuestController.instance.categoriesSync.values()) {
                    for (Quest quest : category.quests.values()) {
                        quests.put(quest.id, quest);
                    }
                }
                QuestController.instance.categories = QuestController.instance.categoriesSync;
                QuestController.instance.quests = quests;
                QuestController.instance.categoriesSync = new HashMap();
            }
        } else if (synctype == 5) {
            if (!compound.hasNoTags()) {
                DialogCategory category = new DialogCategory();
                category.readNBT(compound);
                DialogController.instance.categoriesSync.put(category.id, category);
            }
            if (syncEnd) {
                HashMap<Integer, Dialog> dialogs = new HashMap<Integer, Dialog>();
                for (DialogCategory category : DialogController.instance.categoriesSync.values()) {
                    for (Dialog dialog : category.dialogs.values()) {
                        dialogs.put(dialog.id, dialog);
                    }
                }
                DialogController.instance.categories = DialogController.instance.categoriesSync;
                DialogController.instance.dialogs = dialogs;
                DialogController.instance.categoriesSync = new HashMap();
            }
        }
    }

    public static void clientSyncUpdate(int synctype, NBTTagCompound compound, ByteBuf buffer) {
        if (synctype == 1) {
            Faction faction = new Faction();
            faction.readNBT(compound);
            FactionController.instance.factions.put(faction.id, faction);
        } else if (synctype == 4) {
            DialogCategory category = DialogController.instance.categories.get(buffer.readInt());
            Dialog dialog = new Dialog(category);
            dialog.readNBT(compound);
            DialogController.instance.dialogs.put(dialog.id, dialog);
            category.dialogs.put(dialog.id, dialog);
        } else if (synctype == 5) {
            DialogCategory category = new DialogCategory();
            category.readNBT(compound);
            DialogController.instance.categories.put(category.id, category);
        } else if (synctype == 2) {
            QuestCategory category = QuestController.instance.categories.get(buffer.readInt());
            Quest quest = new Quest(category);
            quest.readNBT(compound);
            QuestController.instance.quests.put(quest.id, quest);
            category.quests.put(quest.id, quest);
        } else if (synctype == 3) {
            QuestCategory category = new QuestCategory();
            category.readNBT(compound);
            QuestController.instance.categories.put(category.id, category);
        }
    }

    public static void clientSyncRemove(int synctype, int id) {
        QuestCategory category;
        if (synctype == 1) {
            FactionController.instance.factions.remove(id);
        } else if (synctype == 4) {
            Dialog dialog = DialogController.instance.dialogs.remove(id);
            if (dialog != null) {
                dialog.category.dialogs.remove(id);
            }
        } else if (synctype == 5) {
            DialogCategory category2 = DialogController.instance.categories.remove(id);
            if (category2 != null) {
                DialogController.instance.dialogs.keySet().removeAll(category2.dialogs.keySet());
            }
        } else if (synctype == 2) {
            Quest quest = QuestController.instance.quests.remove(id);
            if (quest != null) {
                quest.category.quests.remove(id);
            }
        } else if (synctype == 3 && (category = QuestController.instance.categories.remove(id)) != null) {
            QuestController.instance.quests.keySet().removeAll(category.quests.keySet());
        }
    }
}

