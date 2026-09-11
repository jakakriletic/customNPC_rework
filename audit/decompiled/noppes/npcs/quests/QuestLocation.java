/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.text.translation.I18n
 */
package noppes.npcs.quests;

import java.util.ArrayList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerQuestData;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.quests.QuestInterface;

public class QuestLocation
extends QuestInterface {
    public String location = "";
    public String location2 = "";
    public String location3 = "";

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        this.location = compound.func_74779_i("QuestLocation");
        this.location2 = compound.func_74779_i("QuestLocation2");
        this.location3 = compound.func_74779_i("QuestLocation3");
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        compound.func_74778_a("QuestLocation", this.location);
        compound.func_74778_a("QuestLocation2", this.location2);
        compound.func_74778_a("QuestLocation3", this.location3);
    }

    @Override
    public boolean isCompleted(EntityPlayer player) {
        PlayerQuestData playerdata = PlayerData.get((EntityPlayer)player).questData;
        QuestData data = playerdata.activeQuests.get(this.questId);
        if (data == null) {
            return false;
        }
        return this.getFound(data, 0);
    }

    @Override
    public void handleComplete(EntityPlayer player) {
    }

    public boolean getFound(QuestData data, int i) {
        if (i == 1) {
            return data.extraData.func_74767_n("LocationFound");
        }
        if (i == 2) {
            return data.extraData.func_74767_n("Location2Found");
        }
        if (i == 3) {
            return data.extraData.func_74767_n("Location3Found");
        }
        if (!this.location.isEmpty() && !data.extraData.func_74767_n("LocationFound")) {
            return false;
        }
        if (!this.location2.isEmpty() && !data.extraData.func_74767_n("Location2Found")) {
            return false;
        }
        return this.location3.isEmpty() || data.extraData.func_74767_n("Location3Found");
    }

    public boolean setFound(QuestData data, String location) {
        if (location.equalsIgnoreCase(this.location) && !data.extraData.func_74767_n("LocationFound")) {
            data.extraData.func_74757_a("LocationFound", true);
            return true;
        }
        if (location.equalsIgnoreCase(this.location2) && !data.extraData.func_74767_n("LocationFound2")) {
            data.extraData.func_74757_a("Location2Found", true);
            return true;
        }
        if (location.equalsIgnoreCase(this.location3) && !data.extraData.func_74767_n("LocationFound3")) {
            data.extraData.func_74757_a("Location3Found", true);
            return true;
        }
        return false;
    }

    @Override
    public IQuestObjective[] getObjectives(EntityPlayer player) {
        ArrayList<QuestLocationObjective> list = new ArrayList<QuestLocationObjective>();
        if (!this.location.isEmpty()) {
            list.add(new QuestLocationObjective(player, this.location, "LocationFound"));
        }
        if (!this.location2.isEmpty()) {
            list.add(new QuestLocationObjective(player, this.location2, "Location2Found"));
        }
        if (!this.location3.isEmpty()) {
            list.add(new QuestLocationObjective(player, this.location3, "Location3Found"));
        }
        return list.toArray(new IQuestObjective[list.size()]);
    }

    class QuestLocationObjective
    implements IQuestObjective {
        private final EntityPlayer player;
        private final String location;
        private final String nbtName;

        public QuestLocationObjective(EntityPlayer player, String location, String nbtName) {
            this.player = player;
            this.location = location;
            this.nbtName = nbtName;
        }

        @Override
        public int getProgress() {
            return this.isCompleted() ? 1 : 0;
        }

        @Override
        public void setProgress(int progress) {
            if (progress < 0 || progress > 1) {
                throw new CustomNPCsException("Progress has to be 0 or 1", new Object[0]);
            }
            PlayerData data = PlayerData.get(this.player);
            QuestData questData = data.questData.activeQuests.get(QuestLocation.this.questId);
            boolean completed = questData.extraData.func_74767_n(this.nbtName);
            if (completed && progress == 1 || !completed && progress == 0) {
                return;
            }
            questData.extraData.func_74757_a(this.nbtName, progress == 1);
            data.questData.checkQuestCompletion(this.player, 3);
            data.updateClient = true;
        }

        @Override
        public int getMaxProgress() {
            return 1;
        }

        @Override
        public boolean isCompleted() {
            PlayerData data = PlayerData.get(this.player);
            QuestData questData = data.questData.activeQuests.get(QuestLocation.this.questId);
            return questData.extraData.func_74767_n(this.nbtName);
        }

        @Override
        public String getText() {
            String found = I18n.func_74838_a((String)"quest.found");
            String notfound = I18n.func_74838_a((String)"quest.notfound");
            return this.location + ": " + (this.isCompleted() ? found : notfound);
        }
    }
}

