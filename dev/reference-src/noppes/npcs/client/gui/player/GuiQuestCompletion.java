/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.translation.I18n
 */
package noppes.npcs.client.gui.player;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.api.handler.data.IQuest;
import noppes.npcs.client.CustomNpcResourceListener;
import noppes.npcs.client.TextBlockClient;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.ITopButtonListener;
import noppes.npcs.constants.EnumPlayerPacket;

public class GuiQuestCompletion
extends GuiNPCInterface
implements ITopButtonListener {
    private IQuest quest;
    private final ResourceLocation resource = new ResourceLocation("customnpcs", "textures/gui/smallbg.png");

    public GuiQuestCompletion(IQuest quest) {
        this.xSize = 176;
        this.ySize = 222;
        this.quest = quest;
        this.drawDefaultBackground = false;
        this.title = "";
    }

    @Override
    public void initGui() {
        super.initGui();
        String questTitle = I18n.translateToLocal((String)this.quest.getName());
        int left = (this.xSize - this.fontRenderer.getStringWidth(questTitle)) / 2;
        this.addLabel(new GuiNpcLabel(0, questTitle, this.guiLeft + left, this.guiTop + 4));
        this.addButton(new GuiNpcButton(0, this.guiLeft + 38, this.guiTop + this.ySize - 24, 100, 20, I18n.translateToLocal((String)"quest.complete")));
    }

    @Override
    public void drawScreen(int i, int j, float f) {
        this.drawDefaultBackground();
        GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.mc.renderEngine.bindTexture(this.resource);
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        this.drawHorizontalLine(this.guiLeft + 4, this.guiLeft + 170, this.guiTop + 13, -16777216 + CustomNpcResourceListener.DefaultTextColor);
        this.drawQuestText();
        super.drawScreen(i, j, f);
    }

    private void drawQuestText() {
        int xoffset = this.guiLeft + 4;
        TextBlockClient block = new TextBlockClient(this.quest.getCompleteText(), 172, true, this.player);
        int yoffset = this.guiTop + 20;
        for (int i = 0; i < block.lines.size(); ++i) {
            String text = ((ITextComponent)block.lines.get(i)).getFormattedText();
            this.fontRenderer.drawString(text, this.guiLeft + 4, this.guiTop + 16 + i * this.fontRenderer.FONT_HEIGHT, CustomNpcResourceListener.DefaultTextColor);
        }
    }

    @Override
    protected void actionPerformed(GuiButton guibutton) {
        if (guibutton.id == 0) {
            NoppesUtilPlayer.sendData(EnumPlayerPacket.QuestCompletion, this.quest.getId());
            this.close();
        }
    }

    @Override
    public void keyTyped(char c, int i) {
        if (i == 1 || this.isInventoryKey(i)) {
            this.close();
        }
    }

    @Override
    public void save() {
        NoppesUtilPlayer.sendData(EnumPlayerPacket.QuestCompletion, this.quest.getId());
    }
}

