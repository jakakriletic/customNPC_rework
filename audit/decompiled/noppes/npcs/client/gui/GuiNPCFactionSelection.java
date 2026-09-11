/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.entity.player.EntityPlayer
 */
package noppes.npcs.client.gui;

import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import noppes.npcs.client.Client;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.GuiNPCStringSlot;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiSelectionListener;
import noppes.npcs.client.gui.util.IScrollData;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.entity.EntityNPCInterface;

public class GuiNPCFactionSelection
extends GuiNPCInterface
implements IScrollData {
    private GuiNPCStringSlot slot;
    private GuiScreen parent;
    private HashMap<String, Integer> data = new HashMap();
    private int factionId;
    public GuiSelectionListener listener;

    public GuiNPCFactionSelection(EntityNPCInterface npc, GuiScreen parent, int dialog) {
        super(npc);
        this.drawDefaultBackground = false;
        this.title = "Select Dialog Category";
        this.parent = parent;
        this.factionId = dialog;
        if (parent instanceof GuiSelectionListener) {
            this.listener = (GuiSelectionListener)parent;
        }
    }

    @Override
    public void initPacket() {
        Client.sendData(EnumPacketServer.FactionsGet, new Object[0]);
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        Vector<String> list = new Vector<String>();
        this.slot = new GuiNPCStringSlot(list, this, false, 18);
        this.slot.func_148134_d(4, 5);
        this.addButton(new GuiNpcButton(2, this.field_146294_l / 2 - 100, this.field_146295_m - 41, 98, 20, "gui.back"));
        this.addButton(new GuiNpcButton(4, this.field_146294_l / 2 + 2, this.field_146295_m - 41, 98, 20, "mco.template.button.select"));
    }

    @Override
    public void func_73863_a(int i, int j, float f) {
        this.slot.func_148128_a(i, j, f);
        super.func_73863_a(i, j, f);
    }

    public void func_146274_d() throws IOException {
        this.slot.func_178039_p();
        super.func_146274_d();
    }

    @Override
    protected void func_146284_a(GuiButton guibutton) {
        int id = guibutton.field_146127_k;
        if (id == 2) {
            this.close();
            NoppesUtil.openGUI((EntityPlayer)this.player, this.parent);
        }
        if (id == 4) {
            this.doubleClicked();
        }
    }

    @Override
    public void doubleClicked() {
        if (this.slot.selected == null || this.slot.selected.isEmpty()) {
            return;
        }
        this.factionId = this.data.get(this.slot.selected);
        this.close();
        NoppesUtil.openGUI((EntityPlayer)this.player, this.parent);
    }

    @Override
    public void save() {
        if (this.factionId >= 0 && this.listener != null) {
            this.listener.selected(this.factionId, this.slot.selected);
        }
    }

    @Override
    public void setData(Vector<String> list, HashMap<String, Integer> data) {
        this.data = data;
        this.slot.setList(list);
        if (this.factionId >= 0) {
            for (String name : data.keySet()) {
                if (data.get(name) != this.factionId) continue;
                this.slot.selected = name;
            }
        }
    }

    @Override
    public void setSelected(String selected) {
    }
}

