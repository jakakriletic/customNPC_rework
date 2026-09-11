/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.gui.GuiYesNo
 *  net.minecraft.client.gui.GuiYesNoCallback
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.text.translation.I18n
 */
package noppes.npcs.client.gui.global;

import com.google.common.collect.Lists;
import java.util.HashMap;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.client.Client;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.SubGuiEditText;
import noppes.npcs.client.gui.global.GuiDialogEdit;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.ICustomScrollListener;
import noppes.npcs.client.gui.util.ISubGuiListener;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.DialogCategory;
import noppes.npcs.entity.EntityNPCInterface;

public class GuiNPCManageDialogs
extends GuiNPCInterface2
implements ISubGuiListener,
ICustomScrollListener,
GuiYesNoCallback {
    private HashMap<String, DialogCategory> categoryData = new HashMap();
    private HashMap<String, Dialog> dialogData = new HashMap();
    private GuiCustomScroll scrollCategories;
    private GuiCustomScroll scrollDialogs;
    public static GuiScreen Instance;
    private DialogCategory selectedCategory;
    private Dialog selectedDialog;

    public GuiNPCManageDialogs(EntityNPCInterface npc) {
        super(npc);
        Instance = this;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.addLabel(new GuiNpcLabel(0, "gui.categories", this.guiLeft + 8, this.guiTop + 4));
        this.addLabel(new GuiNpcLabel(1, "dialog.dialogs", this.guiLeft + 175, this.guiTop + 4));
        this.addLabel(new GuiNpcLabel(3, "dialog.dialogs", this.guiLeft + 356, this.guiTop + 8));
        this.addButton(new GuiNpcButton(13, this.guiLeft + 356, this.guiTop + 18, 58, 20, "selectServer.edit", this.selectedDialog != null));
        this.addButton(new GuiNpcButton(12, this.guiLeft + 356, this.guiTop + 41, 58, 20, "gui.remove", this.selectedDialog != null));
        this.addButton(new GuiNpcButton(11, this.guiLeft + 356, this.guiTop + 64, 58, 20, "gui.add", this.selectedCategory != null));
        this.addLabel(new GuiNpcLabel(2, "gui.categories", this.guiLeft + 356, this.guiTop + 110));
        this.addButton(new GuiNpcButton(3, this.guiLeft + 356, this.guiTop + 120, 58, 20, "selectServer.edit", this.selectedCategory != null));
        this.addButton(new GuiNpcButton(2, this.guiLeft + 356, this.guiTop + 143, 58, 20, "gui.remove", this.selectedCategory != null));
        this.addButton(new GuiNpcButton(1, this.guiLeft + 356, this.guiTop + 166, 58, 20, "gui.add"));
        HashMap<String, DialogCategory> categoryData = new HashMap<String, DialogCategory>();
        HashMap<String, Dialog> dialogData = new HashMap<String, Dialog>();
        for (DialogCategory category : DialogController.instance.categories.values()) {
            categoryData.put(category.title, category);
        }
        this.categoryData = categoryData;
        if (this.selectedCategory != null) {
            for (Dialog dialog : this.selectedCategory.dialogs.values()) {
                dialogData.put(dialog.title, dialog);
            }
        }
        this.dialogData = dialogData;
        if (this.scrollCategories == null) {
            this.scrollCategories = new GuiCustomScroll(this, 0);
            this.scrollCategories.setSize(170, 200);
        }
        this.scrollCategories.setList(Lists.newArrayList(categoryData.keySet()));
        this.scrollCategories.guiLeft = this.guiLeft + 4;
        this.scrollCategories.guiTop = this.guiTop + 14;
        this.addScroll(this.scrollCategories);
        if (this.scrollDialogs == null) {
            this.scrollDialogs = new GuiCustomScroll(this, 1);
            this.scrollDialogs.setSize(170, 200);
        }
        this.scrollDialogs.setList(Lists.newArrayList(dialogData.keySet()));
        this.scrollDialogs.guiLeft = this.guiLeft + 175;
        this.scrollDialogs.guiTop = this.guiTop + 14;
        this.addScroll(this.scrollDialogs);
    }

    @Override
    public void buttonEvent(GuiButton guibutton) {
        GuiYesNo guiyesno;
        GuiNpcButton button = (GuiNpcButton)guibutton;
        if (button.id == 1) {
            this.setSubGui(new SubGuiEditText(1, I18n.translateToLocal((String)"gui.new")));
        }
        if (button.id == 2) {
            guiyesno = new GuiYesNo((GuiYesNoCallback)this, this.selectedCategory.title, I18n.translateToLocal((String)"gui.deleteMessage"), 2);
            this.displayGuiScreen((GuiScreen)guiyesno);
        }
        if (button.id == 3) {
            this.setSubGui(new SubGuiEditText(3, this.selectedCategory.title));
        }
        if (button.id == 11) {
            this.setSubGui(new SubGuiEditText(11, I18n.translateToLocal((String)"gui.new")));
        }
        if (button.id == 12) {
            guiyesno = new GuiYesNo((GuiYesNoCallback)this, this.selectedDialog.title, I18n.translateToLocal((String)"gui.deleteMessage"), 12);
            this.displayGuiScreen((GuiScreen)guiyesno);
        }
        if (button.id == 13) {
            this.setSubGui(new GuiDialogEdit(this.selectedDialog));
        }
    }

    @Override
    public void subGuiClosed(SubGuiInterface subgui) {
        if (subgui instanceof SubGuiEditText && ((SubGuiEditText)subgui).cancelled) {
            return;
        }
        if (subgui.id == 1) {
            DialogCategory category = new DialogCategory();
            category.title = ((SubGuiEditText)subgui).text;
            while (DialogController.instance.containsCategoryName(category)) {
                category.title = category.title + "_";
            }
            Client.sendData(EnumPacketServer.DialogCategorySave, category.writeNBT(new NBTTagCompound()));
        }
        if (subgui.id == 3) {
            this.selectedCategory.title = ((SubGuiEditText)subgui).text;
            while (DialogController.instance.containsCategoryName(this.selectedCategory)) {
                this.selectedCategory.title = this.selectedCategory.title + "_";
            }
            Client.sendData(EnumPacketServer.DialogCategorySave, this.selectedCategory.writeNBT(new NBTTagCompound()));
        }
        if (subgui.id == 11) {
            Dialog dialog = new Dialog(this.selectedCategory);
            dialog.title = ((SubGuiEditText)subgui).text;
            while (DialogController.instance.containsDialogName(this.selectedCategory, dialog)) {
                dialog.title = dialog.title + "_";
            }
            Client.sendData(EnumPacketServer.DialogSave, this.selectedCategory.id, dialog.writeToNBT(new NBTTagCompound()));
        }
        if (subgui instanceof GuiDialogEdit) {
            this.initGui();
        }
    }

    @Override
    public void scrollClicked(int i, int j, int k, GuiCustomScroll guiCustomScroll) {
        if (guiCustomScroll.id == 0) {
            this.selectedCategory = this.categoryData.get(this.scrollCategories.getSelected());
            this.selectedDialog = null;
            this.scrollDialogs.selected = -1;
        }
        if (guiCustomScroll.id == 1) {
            this.selectedDialog = this.dialogData.get(this.scrollDialogs.getSelected());
        }
        this.initGui();
    }

    @Override
    public void scrollDoubleClicked(String selection, GuiCustomScroll scroll) {
        if (this.selectedDialog != null && scroll.id == 1) {
            this.setSubGui(new GuiDialogEdit(this.selectedDialog));
        }
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    public void save() {
        GuiNpcTextField.unfocus();
    }

    public void confirmClicked(boolean result, int id) {
        NoppesUtil.openGUI((EntityPlayer)this.player, this);
        if (!result) {
            return;
        }
        if (id == 2) {
            Client.sendData(EnumPacketServer.DialogCategoryRemove, this.selectedCategory.id);
        }
        if (id == 12) {
            Client.sendData(EnumPacketServer.DialogRemove, this.selectedDialog.id);
        }
    }
}

