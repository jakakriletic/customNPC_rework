/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.nbt.NBTTagCompound
 */
package noppes.npcs.roles;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.NBTTags;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.entity.data.role.IRoleDialog;
import noppes.npcs.controllers.PlayerQuestController;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.DialogOption;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleInterface;

public class RoleDialog
extends RoleInterface
implements IRoleDialog {
    public String dialog = "";
    public int questId = -1;
    public HashMap<Integer, String> options = new HashMap();
    public HashMap<Integer, String> optionsTexts = new HashMap();

    public RoleDialog(EntityNPCInterface npc) {
        super(npc);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setInteger("RoleQuestId", this.questId);
        compound.setString("RoleDialog", this.dialog);
        compound.setTag("RoleOptions", NBTTags.nbtIntegerStringMap(this.options));
        compound.setTag("RoleOptionTexts", NBTTags.nbtIntegerStringMap(this.optionsTexts));
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        this.questId = compound.getInteger("RoleQuestId");
        this.dialog = compound.getString("RoleDialog");
        this.options = NBTTags.getIntegerStringMap(compound.getTagList("RoleOptions", 10));
        this.optionsTexts = NBTTags.getIntegerStringMap(compound.getTagList("RoleOptionTexts", 10));
    }

    @Override
    public void interact(EntityPlayer player) {
        if (this.dialog.isEmpty()) {
            this.npc.say(player, this.npc.advanced.getInteractLine());
        } else {
            Dialog d = new Dialog(null);
            d.text = this.dialog;
            for (Map.Entry<Integer, String> entry : this.options.entrySet()) {
                if (entry.getValue().isEmpty()) continue;
                DialogOption option = new DialogOption();
                String text = this.optionsTexts.get(entry.getKey());
                option.optionType = text != null && !text.isEmpty() ? 3 : 0;
                option.title = entry.getValue();
                d.options.put(entry.getKey(), option);
            }
            NoppesUtilServer.openDialog(player, this.npc, d);
        }
        Quest quest = QuestController.instance.quests.get(this.questId);
        if (quest != null) {
            PlayerQuestController.addActiveQuest(quest, player);
        }
    }

    @Override
    public String getDialog() {
        return this.dialog;
    }

    @Override
    public void setDialog(String text) {
        this.dialog = text;
    }

    @Override
    public String getOption(int option) {
        return this.options.get(option);
    }

    @Override
    public void setOption(int option, String text) {
        if (option < 1 || option > 6) {
            throw new CustomNPCsException("Wrong dialog option slot given: " + option, new Object[0]);
        }
        this.options.put(option, text);
    }

    @Override
    public String getOptionDialog(int option) {
        return this.optionsTexts.get(option);
    }

    @Override
    public void setOptionDialog(int option, String text) {
        if (option < 1 || option > 6) {
            throw new CustomNPCsException("Wrong dialog option slot given: " + option, new Object[0]);
        }
        this.optionsTexts.put(option, text);
    }
}

