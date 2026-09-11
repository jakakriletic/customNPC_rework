/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.text.translation.I18n
 */
package noppes.npcs.client.gui.player;

import java.util.HashMap;
import java.util.Vector;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.IScrollData;
import noppes.npcs.client.gui.util.ITopButtonListener;
import noppes.npcs.constants.EnumPlayerPacket;
import noppes.npcs.entity.EntityNPCInterface;

public class GuiTransportSelection
extends GuiNPCInterface
implements ITopButtonListener,
IScrollData {
    private final ResourceLocation resource = new ResourceLocation("customnpcs", "textures/gui/smallbg.png");
    protected int xSize = 176;
    protected int guiLeft;
    protected int guiTop;
    private GuiCustomScroll scroll;

    public GuiTransportSelection(EntityNPCInterface npc) {
        super(npc);
        this.drawDefaultBackground = false;
        this.title = "";
    }

    @Override
    public void initGui() {
        super.initGui();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - 222) / 2;
        String name = "";
        this.addLabel(new GuiNpcLabel(0, name, this.guiLeft + (this.xSize - this.fontRenderer.getStringWidth(name)) / 2, this.guiTop + 10));
        this.addButton(new GuiNpcButton(0, this.guiLeft + 10, this.guiTop + 192, 156, 20, I18n.translateToLocal((String)"transporter.travel")));
        if (this.scroll == null) {
            this.scroll = new GuiCustomScroll(this, 0);
        }
        this.scroll.setSize(156, 165);
        this.scroll.guiLeft = this.guiLeft + 10;
        this.scroll.guiTop = this.guiTop + 20;
        this.addScroll(this.scroll);
    }

    @Override
    public void drawScreen(int i, int j, float f) {
        this.drawDefaultBackground();
        GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.mc.renderEngine.bindTexture(this.resource);
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, 176, 222);
        super.drawScreen(i, j, f);
    }

    @Override
    protected void actionPerformed(GuiButton guibutton) {
        GuiNpcButton button = (GuiNpcButton)guibutton;
        String sel = this.scroll.getSelected();
        if (button.id == 0 && sel != null) {
            this.close();
            NoppesUtilPlayer.sendData(EnumPlayerPacket.Transport, sel);
        }
    }

    @Override
    public void mouseClicked(int i, int j, int k) {
        super.mouseClicked(i, j, k);
        this.scroll.mouseClicked(i, j, k);
    }

    @Override
    public void keyTyped(char c, int i) {
        if (i == 1 || this.isInventoryKey(i)) {
            this.close();
        }
    }

    @Override
    public void save() {
    }

    @Override
    public void setData(Vector<String> list, HashMap<String, Integer> data) {
        this.scroll.setList(list);
    }

    @Override
    public void setSelected(String selected) {
    }
}

