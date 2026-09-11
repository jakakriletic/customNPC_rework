/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.ItemStackHelper
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.nbt.NBTTagString
 *  net.minecraft.util.NonNullList
 *  net.minecraft.util.text.ITextComponent
 */
package noppes.npcs.controllers.data;

import java.util.ArrayList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.IContainer;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.data.IPlayerMail;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.data.Quest;

public class PlayerMail
implements IInventory,
IPlayerMail {
    public String subject = "";
    public String sender = "";
    public NBTTagCompound message = new NBTTagCompound();
    public long time = 0L;
    public boolean beenRead = false;
    public int questId = -1;
    public NonNullList<ItemStack> items = NonNullList.func_191197_a((int)4, (Object)ItemStack.field_190927_a);
    public long timePast;

    public void readNBT(NBTTagCompound compound) {
        this.subject = compound.func_74779_i("Subject");
        this.sender = compound.func_74779_i("Sender");
        this.time = compound.func_74763_f("Time");
        this.beenRead = compound.func_74767_n("BeenRead");
        this.message = compound.func_74775_l("Message");
        this.timePast = compound.func_74763_f("TimePast");
        if (compound.func_74764_b("MailQuest")) {
            this.questId = compound.func_74762_e("MailQuest");
        }
        this.items.clear();
        NBTTagList nbttaglist = compound.func_150295_c("MailItems", 10);
        for (int i = 0; i < nbttaglist.func_74745_c(); ++i) {
            NBTTagCompound nbttagcompound1 = nbttaglist.func_150305_b(i);
            int j = nbttagcompound1.func_74771_c("Slot") & 0xFF;
            if (j < 0 || j >= this.items.size()) continue;
            this.items.set(j, (Object)new ItemStack(nbttagcompound1));
        }
    }

    public NBTTagCompound writeNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.func_74778_a("Subject", this.subject);
        compound.func_74778_a("Sender", this.sender);
        compound.func_74772_a("Time", this.time);
        compound.func_74757_a("BeenRead", this.beenRead);
        compound.func_74782_a("Message", (NBTBase)this.message);
        compound.func_74772_a("TimePast", System.currentTimeMillis() - this.time);
        compound.func_74768_a("MailQuest", this.questId);
        if (this.hasQuest()) {
            compound.func_74778_a("MailQuestTitle", this.getQuest().title);
        }
        NBTTagList nbttaglist = new NBTTagList();
        for (int i = 0; i < this.items.size(); ++i) {
            if (((ItemStack)this.items.get(i)).func_190926_b()) continue;
            NBTTagCompound nbttagcompound1 = new NBTTagCompound();
            nbttagcompound1.func_74774_a("Slot", (byte)i);
            ((ItemStack)this.items.get(i)).func_77955_b(nbttagcompound1);
            nbttaglist.func_74742_a((NBTBase)nbttagcompound1);
        }
        compound.func_74782_a("MailItems", (NBTBase)nbttaglist);
        return compound;
    }

    public boolean isValid() {
        return !this.subject.isEmpty() && !this.message.func_82582_d() && !this.sender.isEmpty();
    }

    public boolean hasQuest() {
        return this.getQuest() != null;
    }

    @Override
    public Quest getQuest() {
        return QuestController.instance != null ? QuestController.instance.quests.get(this.questId) : null;
    }

    public int func_70302_i_() {
        return 4;
    }

    public int func_70297_j_() {
        return 64;
    }

    public ItemStack func_70301_a(int i) {
        return (ItemStack)this.items.get(i);
    }

    public ItemStack func_70298_a(int index, int count) {
        ItemStack itemstack = ItemStackHelper.func_188382_a(this.items, (int)index, (int)count);
        if (!itemstack.func_190926_b()) {
            this.func_70296_d();
        }
        return itemstack;
    }

    public ItemStack func_70304_b(int var1) {
        return (ItemStack)this.items.set(var1, (Object)ItemStack.field_190927_a);
    }

    public void func_70299_a(int index, ItemStack stack) {
        this.items.set(index, (Object)stack);
        if (stack.func_190916_E() > this.func_70297_j_()) {
            stack.func_190920_e(this.func_70297_j_());
        }
        this.func_70296_d();
    }

    public ITextComponent func_145748_c_() {
        return null;
    }

    public boolean func_145818_k_() {
        return false;
    }

    public void func_70296_d() {
    }

    public boolean func_70300_a(EntityPlayer var1) {
        return true;
    }

    public void func_174889_b(EntityPlayer player) {
    }

    public void func_174886_c(EntityPlayer player) {
    }

    public boolean func_94041_b(int var1, ItemStack var2) {
        return true;
    }

    public PlayerMail copy() {
        PlayerMail mail = new PlayerMail();
        mail.readNBT(this.writeNBT());
        return mail;
    }

    public String func_70005_c_() {
        return null;
    }

    public int func_174887_a_(int id) {
        return 0;
    }

    public void func_174885_b(int id, int value) {
    }

    public int func_174890_g() {
        return 0;
    }

    public void func_174888_l() {
    }

    public boolean func_191420_l() {
        for (int slot = 0; slot < this.func_70302_i_(); ++slot) {
            ItemStack item = this.func_70301_a(slot);
            if (NoppesUtilServer.IsItemStackNull(item) || item.func_190926_b()) continue;
            return false;
        }
        return true;
    }

    @Override
    public String getSender() {
        return this.sender;
    }

    @Override
    public void setSender(String sender) {
        this.sender = sender;
    }

    @Override
    public String getSubject() {
        return this.subject;
    }

    @Override
    public void setSubject(String subject) {
        this.subject = subject;
    }

    @Override
    public String[] getText() {
        ArrayList<String> list = new ArrayList<String>();
        NBTTagList pages = this.message.func_150295_c("pages", 8);
        for (int i = 0; i < pages.func_74745_c(); ++i) {
            list.add(pages.func_150307_f(i));
        }
        return list.toArray(new String[list.size()]);
    }

    @Override
    public void setText(String[] pages) {
        NBTTagList list = new NBTTagList();
        if (pages != null && pages.length > 0) {
            for (String page : pages) {
                list.func_74742_a((NBTBase)new NBTTagString(page));
            }
        }
        this.message.func_74782_a("pages", (NBTBase)list);
    }

    @Override
    public void setQuest(int id) {
        this.questId = id;
    }

    @Override
    public IContainer getContainer() {
        return NpcAPI.Instance().getIContainer(this);
    }
}

