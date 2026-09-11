/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 */
package noppes.npcs.controllers.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.api.handler.data.IQuest;
import noppes.npcs.api.handler.data.IQuestCategory;
import noppes.npcs.controllers.data.Quest;

public class QuestCategory
implements IQuestCategory {
    public HashMap<Integer, Quest> quests = new HashMap();
    public int id = -1;
    public String title = "";

    public void readNBT(NBTTagCompound nbttagcompound) {
        this.id = nbttagcompound.func_74762_e("Slot");
        this.title = nbttagcompound.func_74779_i("Title");
        NBTTagList dialogsList = nbttagcompound.func_150295_c("Dialogs", 10);
        if (dialogsList != null) {
            for (int ii = 0; ii < dialogsList.func_74745_c(); ++ii) {
                NBTTagCompound nbttagcompound2 = dialogsList.func_150305_b(ii);
                Quest quest = new Quest(this);
                quest.readNBT(nbttagcompound2);
                this.quests.put(quest.id, quest);
            }
        }
    }

    public NBTTagCompound writeNBT(NBTTagCompound nbttagcompound) {
        nbttagcompound.func_74768_a("Slot", this.id);
        nbttagcompound.func_74778_a("Title", this.title);
        NBTTagList dialogs = new NBTTagList();
        for (int dialogId : this.quests.keySet()) {
            Quest quest = this.quests.get(dialogId);
            dialogs.func_74742_a((NBTBase)quest.writeToNBT(new NBTTagCompound()));
        }
        nbttagcompound.func_74782_a("Dialogs", (NBTBase)dialogs);
        return nbttagcompound;
    }

    @Override
    public List<IQuest> quests() {
        return new ArrayList<IQuest>(this.quests.values());
    }

    @Override
    public String getName() {
        return this.title;
    }

    @Override
    public IQuest create() {
        return new Quest(this);
    }
}

