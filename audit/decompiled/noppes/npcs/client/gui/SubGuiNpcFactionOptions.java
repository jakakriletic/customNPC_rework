/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.util.text.translation.I18n
 */
package noppes.npcs.client.gui;

import java.util.HashMap;
import java.util.Vector;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.client.Client;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.ICustomScrollListener;
import noppes.npcs.client.gui.util.IScrollData;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.controllers.data.FactionOptions;

public class SubGuiNpcFactionOptions
extends SubGuiInterface
implements IScrollData,
ICustomScrollListener {
    private FactionOptions options;
    private HashMap<String, Integer> data = new HashMap();
    private GuiCustomScroll scrollFactions;
    private int selected = -1;

    public SubGuiNpcFactionOptions(FactionOptions options) {
        this.options = options;
        this.setBackground("menubg.png");
        this.xSize = 256;
        this.ySize = 216;
        this.closeOnEsc = true;
    }

    @Override
    public void initPacket() {
        Client.sendData(EnumPacketServer.FactionsGet, new Object[0]);
    }

    @Override
    public void func_73866_w_() {
        String label;
        super.func_73866_w_();
        if (this.scrollFactions == null) {
            this.scrollFactions = new GuiCustomScroll(this, 0);
            this.scrollFactions.setSize(120, 208);
        }
        this.scrollFactions.guiLeft = this.guiLeft + 130;
        this.scrollFactions.guiTop = this.guiTop + 4;
        this.addScroll(this.scrollFactions);
        this.addLabel(new GuiNpcLabel(0, "1: ", this.guiLeft + 4, this.guiTop + 12));
        if (this.data.containsValue(this.options.factionId)) {
            this.addLabel(new GuiNpcLabel(1, this.getFactionName(this.options.factionId), this.guiLeft + 12, this.guiTop + 8));
            label = "";
            label = this.options.decreaseFactionPoints ? label + I18n.func_74838_a((String)"gui.decrease") : label + I18n.func_74838_a((String)"gui.increase");
            label = label + " " + this.options.factionPoints + " " + I18n.func_74838_a((String)"faction.points");
            this.addLabel(new GuiNpcLabel(3, label, this.guiLeft + 12, this.guiTop + 16));
            this.addButton(new GuiNpcButton(0, this.guiLeft + 110, this.guiTop + 7, 20, 20, "X"));
        }
        this.addLabel(new GuiNpcLabel(4, "2: ", this.guiLeft + 4, this.guiTop + 40));
        if (this.data.containsValue(this.options.faction2Id)) {
            this.addLabel(new GuiNpcLabel(5, this.getFactionName(this.options.faction2Id), this.guiLeft + 12, this.guiTop + 36));
            label = "";
            label = this.options.decreaseFaction2Points ? label + I18n.func_74838_a((String)"gui.decrease") : label + I18n.func_74838_a((String)"gui.increase");
            label = label + " " + this.options.faction2Points + " " + I18n.func_74838_a((String)"faction.points");
            this.addLabel(new GuiNpcLabel(6, label, this.guiLeft + 12, this.guiTop + 44));
            this.addButton(new GuiNpcButton(1, this.guiLeft + 110, this.guiTop + 35, 20, 20, "X"));
        }
        if (!(this.selected < 0 || this.data.containsValue(this.options.faction2Id) && this.data.containsValue(this.options.factionId) || this.options.hasFaction(this.selected))) {
            this.addButton(new GuiNpcButton(2, this.guiLeft + 4, this.guiTop + 60, 90, 20, new String[]{"gui.increase", "gui.decrease"}, 0));
            this.addTextField(new GuiNpcTextField(1, this, this.field_146289_q, this.guiLeft + 4, this.guiTop + 82, 110, 20, "10"));
            this.getTextField((int)1).numbersOnly = true;
            this.getTextField(1).setMinMaxDefault(1, 100000, 10);
            this.addButton(new GuiNpcButton(3, this.guiLeft + 4, this.guiTop + 104, 60, 20, "gui.add"));
        }
        this.addButton(new GuiNpcButton(66, this.guiLeft + 20, this.guiTop + 192, 90, 20, "gui.done"));
    }

    private String getFactionName(int faction) {
        for (String s : this.data.keySet()) {
            if (this.data.get(s) != faction) continue;
            return s;
        }
        return null;
    }

    @Override
    protected void func_146284_a(GuiButton guibutton) {
        int id = guibutton.field_146127_k;
        if (id == 0) {
            this.options.factionId = -1;
            this.func_73866_w_();
        }
        if (id == 1) {
            this.options.faction2Id = -1;
            this.func_73866_w_();
        }
        if (id == 3) {
            if (!this.data.containsValue(this.options.factionId)) {
                this.options.factionId = this.selected;
                this.options.decreaseFactionPoints = this.getButton(2).getValue() == 1;
                this.options.factionPoints = this.getTextField(1).getInteger();
            } else if (!this.data.containsValue(this.options.faction2Id)) {
                this.options.faction2Id = this.selected;
                this.options.decreaseFaction2Points = this.getButton(2).getValue() == 1;
                this.options.faction2Points = this.getTextField(1).getInteger();
            }
            this.func_73866_w_();
        }
        if (id == 66) {
            this.close();
        }
    }

    @Override
    public void scrollClicked(int i, int j, int k, GuiCustomScroll guiCustomScroll) {
        this.selected = this.data.get(guiCustomScroll.getSelected());
        this.func_73866_w_();
    }

    @Override
    public void setData(Vector<String> list, HashMap<String, Integer> data) {
        GuiCustomScroll scroll = this.getScroll(0);
        String name = scroll.getSelected();
        this.data = data;
        scroll.setList(list);
        if (name != null) {
            scroll.setSelected(name);
        }
        this.func_73866_w_();
    }

    @Override
    public void setSelected(String selected) {
    }

    @Override
    public void scrollDoubleClicked(String selection, GuiCustomScroll scroll) {
    }
}

