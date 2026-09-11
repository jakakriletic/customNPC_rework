/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.translation.I18n
 */
package noppes.npcs.client.gui.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import micdoodle8.mods.galacticraft.api.client.tabs.InventoryTabQuests;
import micdoodle8.mods.galacticraft.api.client.tabs.TabRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.client.CustomNpcResourceListener;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.TextBlockClient;
import noppes.npcs.client.gui.util.GuiButtonNextPage;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiMenuSideButton;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.ICustomScrollListener;
import noppes.npcs.client.gui.util.ITopButtonListener;
import noppes.npcs.controllers.PlayerQuestController;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.util.NaturalOrderComparator;

public class GuiQuestLog
extends GuiNPCInterface
implements ITopButtonListener,
ICustomScrollListener {
    private final ResourceLocation resource = new ResourceLocation("customnpcs", "textures/gui/standardbg.png");
    public HashMap<String, List<Quest>> activeQuests = new HashMap();
    private HashMap<String, Quest> categoryQuests = new HashMap();
    public Quest selectedQuest = null;
    public String selectedCategory = "";
    private EntityPlayer player;
    private GuiCustomScroll scroll;
    private HashMap<Integer, GuiMenuSideButton> sideButtons = new HashMap();
    private boolean noQuests = false;
    private final int maxLines = 10;
    private int currentPage = 0;
    private int maxPages = 1;
    TextBlockClient textblock = null;
    private Minecraft mc = Minecraft.func_71410_x();

    public GuiQuestLog(EntityPlayer player) {
        this.player = player;
        this.xSize = 280;
        this.ySize = 180;
        this.drawDefaultBackground = false;
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        for (Quest quest : PlayerQuestController.getActiveQuests(this.player)) {
            String category = quest.category.title;
            if (!this.activeQuests.containsKey(category)) {
                this.activeQuests.put(category, new ArrayList());
            }
            List<Quest> list = this.activeQuests.get(category);
            list.add(quest);
        }
        this.sideButtons.clear();
        this.guiTop += 10;
        TabRegistry.updateTabValues(this.guiLeft, this.guiTop, InventoryTabQuests.class);
        TabRegistry.addTabsToList(this.field_146292_n);
        this.noQuests = false;
        if (this.activeQuests.isEmpty()) {
            this.noQuests = true;
            return;
        }
        ArrayList<String> categories = new ArrayList<String>();
        categories.addAll(this.activeQuests.keySet());
        Collections.sort(categories, new NaturalOrderComparator());
        int i = 0;
        for (String category : categories) {
            if (this.selectedCategory.isEmpty()) {
                this.selectedCategory = category;
            }
            this.sideButtons.put(i, new GuiMenuSideButton(i, this.guiLeft - 69, this.guiTop + 2 + i * 21, 70, 22, category));
            ++i;
        }
        this.sideButtons.get((Object)Integer.valueOf((int)categories.indexOf((Object)this.selectedCategory))).active = true;
        if (this.scroll == null) {
            this.scroll = new GuiCustomScroll(this, 0);
        }
        HashMap<String, Quest> categoryQuests = new HashMap<String, Quest>();
        for (Quest q : this.activeQuests.get(this.selectedCategory)) {
            categoryQuests.put(q.title, q);
        }
        this.categoryQuests = categoryQuests;
        this.scroll.setList(new ArrayList<String>(categoryQuests.keySet()));
        this.scroll.setSize(134, 174);
        this.scroll.guiLeft = this.guiLeft + 5;
        this.scroll.guiTop = this.guiTop + 15;
        this.addScroll(this.scroll);
        this.addButton(new GuiButtonNextPage(1, this.guiLeft + 286, this.guiTop + 114, true));
        this.addButton(new GuiButtonNextPage(2, this.guiLeft + 144, this.guiTop + 114, false));
        this.getButton((int)1).field_146125_m = this.selectedQuest != null && this.currentPage < this.maxPages - 1;
        this.getButton((int)2).field_146125_m = this.selectedQuest != null && this.currentPage > 0;
    }

    @Override
    protected void func_146284_a(GuiButton guibutton) {
        if (!(guibutton instanceof GuiButtonNextPage)) {
            return;
        }
        if (guibutton.field_146127_k == 1) {
            ++this.currentPage;
            this.func_73866_w_();
        }
        if (guibutton.field_146127_k == 2) {
            --this.currentPage;
            this.func_73866_w_();
        }
    }

    @Override
    public void func_73863_a(int i, int j, float f) {
        if (this.scroll != null) {
            this.scroll.visible = !this.noQuests;
        }
        this.func_146276_q_();
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.mc.field_71446_o.func_110577_a(this.resource);
        this.func_73729_b(this.guiLeft, this.guiTop, 0, 0, 252, 195);
        this.func_73729_b(this.guiLeft + 252, this.guiTop, 188, 0, 67, 195);
        super.func_73863_a(i, j, f);
        if (this.noQuests) {
            this.mc.field_71466_p.func_78276_b(I18n.func_74838_a((String)"quest.noquests"), this.guiLeft + 84, this.guiTop + 80, CustomNpcResourceListener.DefaultTextColor);
            return;
        }
        for (GuiMenuSideButton button : this.sideButtons.values().toArray(new GuiMenuSideButton[this.sideButtons.size()])) {
            button.func_191745_a(this.mc, i, j, f);
        }
        this.mc.field_71466_p.func_78276_b(this.selectedCategory, this.guiLeft + 5, this.guiTop + 5, CustomNpcResourceListener.DefaultTextColor);
        if (this.selectedQuest == null) {
            return;
        }
        this.drawProgress();
        this.drawQuestText();
        GlStateManager.func_179094_E();
        GlStateManager.func_179109_b((float)(this.guiLeft + 148), (float)this.guiTop, (float)0.0f);
        GlStateManager.func_179152_a((float)1.24f, (float)1.24f, (float)1.24f);
        String title = I18n.func_74838_a((String)this.selectedQuest.title);
        this.field_146289_q.func_78276_b(title, (130 - this.field_146289_q.func_78256_a(title)) / 2, 4, CustomNpcResourceListener.DefaultTextColor);
        GlStateManager.func_179121_F();
        this.func_73730_a(this.guiLeft + 142, this.guiLeft + 312, this.guiTop + 17, -16777216 + CustomNpcResourceListener.DefaultTextColor);
    }

    private void drawQuestText() {
        if (this.textblock == null) {
            return;
        }
        int yoffset = this.guiTop + 5;
        for (int i = 0; i < 10; ++i) {
            int index = i + this.currentPage * 10;
            if (index >= this.textblock.lines.size()) continue;
            String text = ((ITextComponent)this.textblock.lines.get(index)).func_150254_d();
            this.field_146289_q.func_78276_b(text, this.guiLeft + 142, this.guiTop + 20 + i * this.field_146289_q.field_78288_b, CustomNpcResourceListener.DefaultTextColor);
        }
    }

    private void drawProgress() {
        String title = I18n.func_74838_a((String)"quest.objectives") + ":";
        this.mc.field_71466_p.func_78276_b(title, this.guiLeft + 142, this.guiTop + 130, CustomNpcResourceListener.DefaultTextColor);
        this.func_73730_a(this.guiLeft + 142, this.guiLeft + 312, this.guiTop + 140, -16777216 + CustomNpcResourceListener.DefaultTextColor);
        int yoffset = this.guiTop + 144;
        for (IQuestObjective objective : this.selectedQuest.questInterface.getObjectives(this.player)) {
            this.mc.field_71466_p.func_78276_b("- " + objective.getText(), this.guiLeft + 142, yoffset, CustomNpcResourceListener.DefaultTextColor);
            yoffset += 10;
        }
        this.func_73730_a(this.guiLeft + 142, this.guiLeft + 312, this.guiTop + 178, -16777216 + CustomNpcResourceListener.DefaultTextColor);
        String complete = this.selectedQuest.getNpcName();
        if (complete != null && !complete.isEmpty()) {
            this.mc.field_71466_p.func_78276_b(I18n.func_74837_a((String)"quest.completewith", (Object[])new Object[]{complete}), this.guiLeft + 142, this.guiTop + 182, CustomNpcResourceListener.DefaultTextColor);
        }
    }

    @Override
    public void func_73864_a(int i, int j, int k) {
        super.func_73864_a(i, j, k);
        if (k == 0) {
            if (this.scroll != null) {
                this.scroll.func_73864_a(i, j, k);
            }
            for (GuiMenuSideButton button : new ArrayList<GuiMenuSideButton>(this.sideButtons.values())) {
                if (!button.func_146116_c(this.mc, i, j)) continue;
                this.sideButtonPressed(button);
            }
        }
    }

    private void sideButtonPressed(GuiMenuSideButton button) {
        if (button.active) {
            return;
        }
        NoppesUtil.clickSound();
        this.selectedCategory = button.field_146126_j;
        this.selectedQuest = null;
        this.func_73866_w_();
    }

    @Override
    public void scrollClicked(int i, int j, int k, GuiCustomScroll scroll) {
        if (!scroll.hasSelected()) {
            return;
        }
        this.selectedQuest = this.categoryQuests.get(scroll.getSelected());
        this.textblock = new TextBlockClient(this.selectedQuest.getLogText(), 172, true, this.player);
        if (this.textblock.lines.size() > 10) {
            this.maxPages = MathHelper.func_76123_f((float)(1.0f * (float)this.textblock.lines.size() / 10.0f));
        }
        this.currentPage = 0;
        this.func_73866_w_();
    }

    @Override
    public void func_73869_a(char c, int i) {
        if (i == 1 || i == this.mc.field_71474_y.field_151445_Q.func_151463_i()) {
            this.mc.func_147108_a(null);
            this.mc.func_71381_h();
        }
    }

    @Override
    public boolean func_73868_f() {
        return false;
    }

    @Override
    public void save() {
    }

    @Override
    public void scrollDoubleClicked(String selection, GuiCustomScroll scroll) {
    }
}

