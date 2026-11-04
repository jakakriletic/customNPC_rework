/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 */
package noppes.npcs.client.gui.global;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.Client;
import noppes.npcs.client.gui.SubGuiEditText;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.ICustomScrollListener;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.IScrollData;
import noppes.npcs.client.gui.util.ISubGuiListener;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.linked.LinkedGroupPanel;
import noppes.npcs.rework.linked.LinkedGuiCodec;

public class GuiNPCManageLinkedNpc
extends GuiNPCInterface2
implements IScrollData,
ISubGuiListener,
ICustomScrollListener {
    private GuiCustomScroll scroll;
    private List<String> data = new ArrayList<String>();
    public static GuiScreen Instance;
    private final LinkedGroupPanel rwPanel;

    public GuiNPCManageLinkedNpc(EntityNPCInterface npc) {
        super(npc);
        Instance = this;
        Client.sendData(EnumPacketServer.LinkedGetAll, new Object[0]);
        this.rwPanel = CustomNpcs.RwLinkedGroups == 1 ? new LinkedGroupPanel() : null;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.addButton(new GuiNpcButton(1, this.guiLeft + 358, this.guiTop + 38, 58, 20, "gui.add"));
        this.addButton(new GuiNpcButton(2, this.guiLeft + 358, this.guiTop + 61, 58, 20, "gui.remove"));
        if (this.scroll == null) {
            this.scroll = new GuiCustomScroll(this, 0);
            this.scroll.setSize(143, 208);
        }
        this.scroll.guiLeft = this.guiLeft + 214;
        this.scroll.guiTop = this.guiTop + 4;
        this.scroll.setList(this.data);
        this.addScroll(this.scroll);
        if (this.rwPanel != null) {
            this.rwPanel.init(this, this.scroll.getSelected());
        }
    }

    /** M13.2b: an answer on the rwlinked channel while this screen is open. */
    public void rwReply(LinkedGuiCodec.Response reply) {
        if (this.rwPanel != null && this.rwPanel.reply(reply)) {
            this.initGui();
        }
    }

    @Override
    public void scrollClicked(int i, int j, int k, GuiCustomScroll scroll) {
        if (this.rwPanel != null) {
            this.rwPanel.select(scroll.getSelected());
        }
    }

    @Override
    public void scrollDoubleClicked(String selection, GuiCustomScroll scroll) {
    }

    @Override
    public void buttonEvent(GuiButton button) {
        if (this.rwPanel != null && this.rwPanel.button(button.id)) {
            return;
        }
        if (button.id == 1) {
            this.save();
            this.setSubGui(new SubGuiEditText("New"));
        }
        if (button.id == 2 && this.scroll.hasSelected()) {
            Client.sendData(EnumPacketServer.LinkedRemove, this.scroll.getSelected());
        }
    }

    @Override
    public void subGuiClosed(SubGuiInterface subgui) {
        if (!((SubGuiEditText)subgui).cancelled) {
            Client.sendData(EnumPacketServer.LinkedAdd, ((SubGuiEditText)subgui).text);
        }
    }

    @Override
    public void setData(Vector<String> list, HashMap<String, Integer> data) {
        this.data = new ArrayList<String>(list);
        this.initGui();
    }

    @Override
    public void setSelected(String selected) {
    }

    @Override
    public void save() {
    }
}

