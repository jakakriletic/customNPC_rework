/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 */
package noppes.npcs.controllers.data;

import java.util.HashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.EventHooks;
import noppes.npcs.Server;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.constants.EnumQuestCompletion;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.quests.QuestInterface;

public class PlayerQuestData {
    public HashMap<Integer, QuestData> activeQuests = new HashMap();
    public HashMap<Integer, Long> finishedQuests = new HashMap();

    public void loadNBTData(NBTTagCompound mainCompound) {
        NBTTagList list2;
        if (mainCompound == null) {
            return;
        }
        NBTTagCompound compound = mainCompound.func_74775_l("QuestData");
        NBTTagList list = compound.func_150295_c("CompletedQuests", 10);
        if (list != null) {
            HashMap<Integer, Long> finishedQuests = new HashMap<Integer, Long>();
            for (int i = 0; i < list.func_74745_c(); ++i) {
                NBTTagCompound nbttagcompound = list.func_150305_b(i);
                finishedQuests.put(nbttagcompound.func_74762_e("Quest"), nbttagcompound.func_74763_f("Date"));
            }
            this.finishedQuests = finishedQuests;
        }
        if ((list2 = compound.func_150295_c("ActiveQuests", 10)) != null) {
            HashMap<Integer, QuestData> activeQuests = new HashMap<Integer, QuestData>();
            for (int i = 0; i < list2.func_74745_c(); ++i) {
                NBTTagCompound nbttagcompound = list2.func_150305_b(i);
                int id = nbttagcompound.func_74762_e("Quest");
                Quest quest = QuestController.instance.quests.get(id);
                if (quest == null) continue;
                QuestData data = new QuestData(quest);
                data.readEntityFromNBT(nbttagcompound);
                activeQuests.put(id, data);
            }
            this.activeQuests = activeQuests;
        }
    }

    public void saveNBTData(NBTTagCompound maincompound) {
        NBTTagCompound compound = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (int quest : this.finishedQuests.keySet()) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.func_74768_a("Quest", quest);
            nbttagcompound.func_74772_a("Date", this.finishedQuests.get(quest).longValue());
            list.func_74742_a((NBTBase)nbttagcompound);
        }
        compound.func_74782_a("CompletedQuests", (NBTBase)list);
        NBTTagList list2 = new NBTTagList();
        for (int quest : this.activeQuests.keySet()) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.func_74768_a("Quest", quest);
            this.activeQuests.get(quest).writeEntityToNBT(nbttagcompound);
            list2.func_74742_a((NBTBase)nbttagcompound);
        }
        compound.func_74782_a("ActiveQuests", (NBTBase)list2);
        maincompound.func_74782_a("QuestData", (NBTBase)compound);
    }

    public QuestData getQuestCompletion(EntityPlayer player, EntityNPCInterface npc) {
        for (QuestData data : this.activeQuests.values()) {
            Quest quest = data.quest;
            if (quest == null || quest.completion != EnumQuestCompletion.Npc || !quest.completerNpc.equals(npc.func_70005_c_()) || !quest.questInterface.isCompleted(player)) continue;
            return data;
        }
        return null;
    }

    public boolean checkQuestCompletion(EntityPlayer player, int type) {
        boolean bo = false;
        for (QuestData data : this.activeQuests.values()) {
            if (data.quest.type != type && type >= 0) continue;
            QuestInterface inter = data.quest.questInterface;
            if (inter.isCompleted(player)) {
                if (data.isCompleted) continue;
                if (!data.quest.complete(player, data)) {
                    Server.sendData((EntityPlayerMP)player, EnumPacketClient.MESSAGE, "quest.completed", data.quest.title, 2);
                    Server.sendData((EntityPlayerMP)player, EnumPacketClient.CHAT, "quest.completed", ": ", data.quest.title);
                }
                data.isCompleted = true;
                bo = true;
                EventHooks.onQuestFinished(PlayerData.get((EntityPlayer)player).scriptData, data.quest);
                continue;
            }
            data.isCompleted = false;
        }
        return bo;
    }
}

