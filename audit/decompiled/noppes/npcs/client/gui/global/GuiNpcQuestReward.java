/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.util.ResourceLocation
 */
package noppes.npcs.client.gui.global;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.global.GuiNPCManageQuest;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.ITextfieldListener;
import noppes.npcs.containers.ContainerNpcQuestReward;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.entity.EntityNPCInterface;

public class GuiNpcQuestReward
extends GuiContainerNPCInterface
implements ITextfieldListener {
    private Quest quest;
    private ResourceLocation resource;

    public GuiNpcQuestReward(EntityNPCInterface npc, ContainerNpcQuestReward container) {
        super(npc, container);
        this.quest = NoppesUtilServer.getEditingQuest((EntityPlayer)this.player);
        this.resource = this.getResource("questreward.png");
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        this.addLabel(new GuiNpcLabel(0, "quest.randomitem", this.field_147003_i + 4, this.field_147009_r + 4));
        this.addButton(new GuiNpcButton(0, this.field_147003_i + 4, this.field_147009_r + 14, 60, 20, new String[]{"gui.no", "gui.yes"}, this.quest.randomReward ? 1 : 0));
        this.addButton(new GuiNpcButton(5, this.field_147003_i, this.field_147009_r + this.field_147000_g, 98, 20, "gui.back"));
        this.addLabel(new GuiNpcLabel(1, "quest.exp", this.field_147003_i + 4, this.field_147009_r + 45));
        this.addTextField(new GuiNpcTextField(0, (GuiScreen)this, this.field_146289_q, this.field_147003_i + 4, this.field_147009_r + 55, 60, 20, this.quest.rewardExp + ""));
        this.getTextField((int)0).numbersOnly = true;
        this.getTextField(0).setMinMaxDefault(0, 99999, 0);
    }

    @Override
    public void func_146284_a(GuiButton guibutton) {
        int id = guibutton.field_146127_k;
        if (id == 5) {
            NoppesUtil.openGUI((EntityPlayer)this.player, GuiNPCManageQuest.Instance);
        }
        if (id == 0) {
            this.quest.randomReward = ((GuiNpcButton)guibutton).getValue() == 1;
        }
    }

    @Override
    protected void func_146976_a(float f, int i, int j) {
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.field_146297_k.field_71446_o.func_110577_a(this.resource);
        int l = (this.field_146294_l - this.field_146999_f) / 2;
        int i1 = (this.field_146295_m - this.field_147000_g) / 2;
        this.func_73729_b(l, i1, 0, 0, this.field_146999_f, this.field_147000_g);
        super.func_146976_a(f, i, j);
    }

    @Override
    public void save() {
    }

    @Override
    public void unFocused(GuiNpcTextField textfield) {
        this.quest.rewardExp = textfield.getInteger();
    }
}

