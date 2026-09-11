/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  net.minecraft.client.gui.GuiButton
 */
package noppes.npcs.client.gui.select;

import com.google.common.collect.Lists;
import java.util.HashMap;
import net.minecraft.client.gui.GuiButton;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiSelectionListener;
import noppes.npcs.client.gui.util.ICustomScrollListener;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.DialogCategory;

public class GuiDialogSelection
extends SubGuiInterface
implements ICustomScrollListener {
    private HashMap<String, DialogCategory> categoryData = new HashMap();
    private HashMap<String, Dialog> dialogData = new HashMap();
    private GuiCustomScroll scrollCategories;
    private GuiCustomScroll scrollDialogs;
    private DialogCategory selectedCategory;
    public Dialog selectedDialog;
    private GuiSelectionListener listener;

    public GuiDialogSelection(int dialog) {
        this.drawDefaultBackground = false;
        this.title = "";
        this.setBackground("menubg.png");
        this.xSize = 366;
        this.ySize = 226;
        this.selectedDialog = DialogController.instance.dialogs.get(dialog);
        if (this.selectedDialog != null) {
            this.selectedCategory = this.selectedDialog.category;
        }
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        if (this.parent instanceof GuiSelectionListener) {
            this.listener = (GuiSelectionListener)this.parent;
        }
        this.addLabel(new GuiNpcLabel(0, "gui.categories", this.guiLeft + 8, this.guiTop + 4));
        this.addLabel(new GuiNpcLabel(1, "dialog.dialogs", this.guiLeft + 175, this.guiTop + 4));
        this.addButton(new GuiNpcButton(2, this.guiLeft + this.xSize - 26, this.guiTop + 4, 20, 20, "X"));
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
        if (this.selectedCategory != null) {
            this.scrollCategories.setSelected(this.selectedCategory.title);
        }
        this.scrollCategories.guiLeft = this.guiLeft + 4;
        this.scrollCategories.guiTop = this.guiTop + 14;
        this.addScroll(this.scrollCategories);
        if (this.scrollDialogs == null) {
            this.scrollDialogs = new GuiCustomScroll(this, 1);
            this.scrollDialogs.setSize(170, 200);
        }
        this.scrollDialogs.setList(Lists.newArrayList(dialogData.keySet()));
        if (this.selectedDialog != null) {
            this.scrollDialogs.setSelected(this.selectedDialog.title);
        }
        this.scrollDialogs.guiLeft = this.guiLeft + 175;
        this.scrollDialogs.guiTop = this.guiTop + 14;
        this.addScroll(this.scrollDialogs);
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
        this.func_73866_w_();
    }

    @Override
    public void scrollDoubleClicked(String selection, GuiCustomScroll scroll) {
        if (this.selectedDialog == null) {
            return;
        }
        if (this.listener != null) {
            this.listener.selected(this.selectedDialog.id, this.selectedDialog.title);
        }
        this.close();
    }

    @Override
    protected void func_146284_a(GuiButton guibutton) {
        int id = guibutton.field_146127_k;
        if (id == 2) {
            if (this.selectedDialog != null) {
                this.scrollDoubleClicked(null, null);
            } else {
                this.close();
            }
        }
    }
}

