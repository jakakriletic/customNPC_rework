/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 */
package noppes.npcs.client.gui.advanced;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import noppes.npcs.client.Client;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.ICustomScrollListener;
import noppes.npcs.client.gui.util.IScrollData;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.entity.EntityNPCInterface;

public class GuiNPCAdvancedLinkedNpc
extends GuiNPCInterface2
implements IScrollData,
ICustomScrollListener {
    private GuiCustomScroll scroll;
    private List<String> data = new ArrayList<String>();
    public static GuiScreen Instance;

    public GuiNPCAdvancedLinkedNpc(EntityNPCInterface npc) {
        super(npc);
        Instance = this;
    }

    @Override
    public void initPacket() {
        Client.sendData(EnumPacketServer.LinkedGetAll, new Object[0]);
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        this.addButton(new GuiNpcButton(1, this.guiLeft + 358, this.guiTop + 38, 58, 20, "gui.clear"));
        if (this.scroll == null) {
            this.scroll = new GuiCustomScroll(this, 0);
            this.scroll.setSize(143, 208);
        }
        this.scroll.guiLeft = this.guiLeft + 137;
        this.scroll.guiTop = this.guiTop + 4;
        this.scroll.setSelected(this.npc.linkedName);
        this.scroll.setList(this.data);
        this.addScroll(this.scroll);
    }

    @Override
    public void buttonEvent(GuiButton button) {
        if (button.field_146127_k == 1) {
            Client.sendData(EnumPacketServer.LinkedSet, "");
        }
    }

    @Override
    public void setData(Vector<String> list, HashMap<String, Integer> data) {
        this.data = new ArrayList<String>(list);
        this.func_73866_w_();
    }

    @Override
    public void setSelected(String selected) {
        this.scroll.setSelected(selected);
    }

    @Override
    public void save() {
    }

    @Override
    public void scrollClicked(int i, int j, int k, GuiCustomScroll guiCustomScroll) {
        Client.sendData(EnumPacketServer.LinkedSet, guiCustomScroll.getSelected());
    }

    @Override
    public void scrollDoubleClicked(String selection, GuiCustomScroll scroll) {
    }
}

