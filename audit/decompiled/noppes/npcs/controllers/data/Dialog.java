/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 */
package noppes.npcs.controllers.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.ICompatibilty;
import noppes.npcs.VersionCompatibility;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.handler.data.IAvailability;
import noppes.npcs.api.handler.data.IDialog;
import noppes.npcs.api.handler.data.IDialogCategory;
import noppes.npcs.api.handler.data.IDialogOption;
import noppes.npcs.api.handler.data.IQuest;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.data.Availability;
import noppes.npcs.controllers.data.DialogCategory;
import noppes.npcs.controllers.data.DialogOption;
import noppes.npcs.controllers.data.FactionOptions;
import noppes.npcs.controllers.data.PlayerMail;
import noppes.npcs.controllers.data.Quest;

public class Dialog
implements ICompatibilty,
IDialog {
    public int version = VersionCompatibility.ModRev;
    public int id = -1;
    public String title = "";
    public String text = "";
    public int quest = -1;
    public final DialogCategory category;
    public HashMap<Integer, DialogOption> options = new HashMap();
    public Availability availability = new Availability();
    public FactionOptions factionOptions = new FactionOptions();
    public String sound;
    public String command = "";
    public PlayerMail mail = new PlayerMail();
    public boolean hideNPC = false;
    public boolean showWheel = false;
    public boolean disableEsc = false;

    public Dialog(DialogCategory category) {
        this.category = category;
    }

    public boolean hasDialogs(EntityPlayer player) {
        for (DialogOption option : this.options.values()) {
            if (option == null || option.optionType != 1 || !option.hasDialog() || !option.isAvailable(player)) continue;
            return true;
        }
        return false;
    }

    public void readNBT(NBTTagCompound compound) {
        this.id = compound.func_74762_e("DialogId");
        this.readNBTPartial(compound);
    }

    public void readNBTPartial(NBTTagCompound compound) {
        this.version = compound.func_74762_e("ModRev");
        VersionCompatibility.CheckAvailabilityCompatibility(this, compound);
        this.title = compound.func_74779_i("DialogTitle");
        this.text = compound.func_74779_i("DialogText");
        this.quest = compound.func_74762_e("DialogQuest");
        this.sound = compound.func_74779_i("DialogSound");
        this.command = compound.func_74779_i("DialogCommand");
        this.mail.readNBT(compound.func_74775_l("DialogMail"));
        this.hideNPC = compound.func_74767_n("DialogHideNPC");
        this.showWheel = compound.func_74767_n("DialogShowWheel");
        this.disableEsc = compound.func_74767_n("DialogDisableEsc");
        NBTTagList options = compound.func_150295_c("Options", 10);
        HashMap<Integer, DialogOption> newoptions = new HashMap<Integer, DialogOption>();
        for (int iii = 0; iii < options.func_74745_c(); ++iii) {
            NBTTagCompound option = options.func_150305_b(iii);
            int opslot = option.func_74762_e("OptionSlot");
            DialogOption dia = new DialogOption();
            dia.readNBT(option.func_74775_l("Option"));
            newoptions.put(opslot, dia);
            dia.slot = opslot;
        }
        this.options = newoptions;
        this.availability.readFromNBT(compound);
        this.factionOptions.readFromNBT(compound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.func_74768_a("DialogId", this.id);
        return this.writeToNBTPartial(compound);
    }

    public NBTTagCompound writeToNBTPartial(NBTTagCompound compound) {
        compound.func_74778_a("DialogTitle", this.title);
        compound.func_74778_a("DialogText", this.text);
        compound.func_74768_a("DialogQuest", this.quest);
        compound.func_74778_a("DialogCommand", this.command);
        compound.func_74782_a("DialogMail", (NBTBase)this.mail.writeNBT());
        compound.func_74757_a("DialogHideNPC", this.hideNPC);
        compound.func_74757_a("DialogShowWheel", this.showWheel);
        compound.func_74757_a("DialogDisableEsc", this.disableEsc);
        if (this.sound != null && !this.sound.isEmpty()) {
            compound.func_74778_a("DialogSound", this.sound);
        }
        NBTTagList options = new NBTTagList();
        for (int opslot : this.options.keySet()) {
            NBTTagCompound listcompound = new NBTTagCompound();
            listcompound.func_74768_a("OptionSlot", opslot);
            listcompound.func_74782_a("Option", (NBTBase)this.options.get(opslot).writeNBT());
            options.func_74742_a((NBTBase)listcompound);
        }
        compound.func_74782_a("Options", (NBTBase)options);
        this.availability.writeToNBT(compound);
        this.factionOptions.writeToNBT(compound);
        compound.func_74768_a("ModRev", this.version);
        return compound;
    }

    public boolean hasQuest() {
        return this.getQuest() != null;
    }

    @Override
    public Quest getQuest() {
        if (QuestController.instance == null) {
            return null;
        }
        return QuestController.instance.quests.get(this.quest);
    }

    public boolean hasOtherOptions() {
        for (DialogOption option : this.options.values()) {
            if (option == null || option.optionType == 2) continue;
            return true;
        }
        return false;
    }

    public Dialog copy(EntityPlayer player) {
        Dialog dialog = new Dialog(this.category);
        dialog.id = this.id;
        dialog.text = this.text;
        dialog.title = this.title;
        dialog.quest = this.quest;
        dialog.sound = this.sound;
        dialog.mail = this.mail;
        dialog.command = this.command;
        dialog.hideNPC = this.hideNPC;
        dialog.showWheel = this.showWheel;
        dialog.disableEsc = this.disableEsc;
        for (int slot : this.options.keySet()) {
            DialogOption option = this.options.get(slot);
            if (option.optionType == 1 && (!option.hasDialog() || !option.isAvailable(player))) continue;
            dialog.options.put(slot, option);
        }
        return dialog;
    }

    @Override
    public int getVersion() {
        return this.version;
    }

    @Override
    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.title;
    }

    @Override
    public List<IDialogOption> getOptions() {
        return new ArrayList<IDialogOption>(this.options.values());
    }

    @Override
    public IDialogOption getOption(int slot) {
        IDialogOption option = this.options.get(slot);
        if (option == null) {
            throw new CustomNPCsException("There is no DialogOption for slot: " + slot, new Object[0]);
        }
        return option;
    }

    @Override
    public IAvailability getAvailability() {
        return this.availability;
    }

    @Override
    public IDialogCategory getCategory() {
        return this.category;
    }

    @Override
    public void save() {
        DialogController.instance.saveDialog(this.category, this);
    }

    @Override
    public void setName(String name) {
        this.title = name;
    }

    @Override
    public String getText() {
        return this.text;
    }

    @Override
    public void setText(String text) {
        this.text = text;
    }

    @Override
    public void setQuest(IQuest quest) {
        if (quest == null) {
            this.quest = -1;
        } else {
            if (quest.getId() < 0) {
                throw new CustomNPCsException("Quest id is lower than 0", new Object[0]);
            }
            this.quest = quest.getId();
        }
    }

    @Override
    public String getCommand() {
        return this.command;
    }

    @Override
    public void setCommand(String command) {
        this.command = command;
    }
}

