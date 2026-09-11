/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.text.translation.I18n
 */
package noppes.npcs.quests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.NBTTags;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerQuestData;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.quests.QuestInterface;

public class QuestKill
extends QuestInterface {
    public TreeMap<String, Integer> targets = new TreeMap();

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        this.targets = new TreeMap<String, Integer>(NBTTags.getStringIntegerMap(compound.func_150295_c("QuestDialogs", 10)));
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        compound.func_74782_a("QuestDialogs", (NBTBase)NBTTags.nbtStringIntegerMap(this.targets));
    }

    @Override
    public boolean isCompleted(EntityPlayer player) {
        PlayerQuestData playerdata = PlayerData.get((EntityPlayer)player).questData;
        QuestData data = playerdata.activeQuests.get(this.questId);
        if (data == null) {
            return false;
        }
        HashMap<String, Integer> killed = this.getKilled(data);
        if (killed.size() != this.targets.size()) {
            return false;
        }
        for (String entity : killed.keySet()) {
            if (this.targets.containsKey(entity) && this.targets.get(entity) <= killed.get(entity)) continue;
            return false;
        }
        return true;
    }

    @Override
    public void handleComplete(EntityPlayer player) {
    }

    public HashMap<String, Integer> getKilled(QuestData data) {
        return NBTTags.getStringIntegerMap(data.extraData.func_150295_c("Killed", 10));
    }

    public void setKilled(QuestData data, HashMap<String, Integer> killed) {
        data.extraData.func_74782_a("Killed", (NBTBase)NBTTags.nbtStringIntegerMap(killed));
    }

    @Override
    public IQuestObjective[] getObjectives(EntityPlayer player) {
        ArrayList<QuestKillObjective> list = new ArrayList<QuestKillObjective>();
        for (Map.Entry<String, Integer> entry : this.targets.entrySet()) {
            list.add(new QuestKillObjective(player, entry.getKey(), entry.getValue()));
        }
        return list.toArray(new IQuestObjective[list.size()]);
    }

    class QuestKillObjective
    implements IQuestObjective {
        private final EntityPlayer player;
        private final String entity;
        private final int amount;

        public QuestKillObjective(EntityPlayer player, String entity, int amount) {
            this.player = player;
            this.entity = entity;
            this.amount = amount;
        }

        @Override
        public int getProgress() {
            PlayerData data = PlayerData.get(this.player);
            PlayerQuestData playerdata = data.questData;
            QuestData questdata = playerdata.activeQuests.get(QuestKill.this.questId);
            HashMap<String, Integer> killed = QuestKill.this.getKilled(questdata);
            if (!killed.containsKey(this.entity)) {
                return 0;
            }
            return killed.get(this.entity);
        }

        @Override
        public void setProgress(int progress) {
            if (progress < 0 || progress > this.amount) {
                throw new CustomNPCsException("Progress has to be between 0 and " + this.amount, new Object[0]);
            }
            PlayerData data = PlayerData.get(this.player);
            PlayerQuestData playerdata = data.questData;
            QuestData questdata = playerdata.activeQuests.get(QuestKill.this.questId);
            HashMap<String, Integer> killed = QuestKill.this.getKilled(questdata);
            if (killed.containsKey(this.entity) && killed.get(this.entity) == progress) {
                return;
            }
            killed.put(this.entity, progress);
            QuestKill.this.setKilled(questdata, killed);
            data.questData.checkQuestCompletion(this.player, 2);
            data.questData.checkQuestCompletion(this.player, 4);
            data.updateClient = true;
        }

        @Override
        public int getMaxProgress() {
            return this.amount;
        }

        @Override
        public boolean isCompleted() {
            return this.getProgress() >= this.amount;
        }

        @Override
        public String getText() {
            String transName;
            String name = "entity." + this.entity + ".name";
            if (name.equals(transName = I18n.func_74838_a((String)name))) {
                transName = this.entity;
            }
            return transName + ": " + this.getProgress() + "/" + this.getMaxProgress();
        }
    }
}

