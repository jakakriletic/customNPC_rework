/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.text.translation.I18n
 */
package noppes.npcs.client.gui.global;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.client.Client;
import noppes.npcs.client.gui.SubGuiColorSelector;
import noppes.npcs.client.gui.SubGuiNpcFactionPoints;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.ICustomScrollListener;
import noppes.npcs.client.gui.util.IGuiData;
import noppes.npcs.client.gui.util.IScrollData;
import noppes.npcs.client.gui.util.ISubGuiListener;
import noppes.npcs.client.gui.util.ITextfieldListener;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.entity.EntityNPCInterface;

public class GuiNPCManageFactions
extends GuiNPCInterface2
implements IScrollData,
ICustomScrollListener,
ITextfieldListener,
IGuiData,
ISubGuiListener {
    private GuiCustomScroll scrollFactions;
    private HashMap<String, Integer> data = new HashMap();
    private Faction faction = new Faction();
    private String selected = null;

    public GuiNPCManageFactions(EntityNPCInterface npc) {
        super(npc);
        Client.sendData(EnumPacketServer.FactionsGet, new Object[0]);
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        this.addButton(new GuiNpcButton(0, this.guiLeft + 368, this.guiTop + 8, 45, 20, "gui.add"));
        this.addButton(new GuiNpcButton(1, this.guiLeft + 368, this.guiTop + 32, 45, 20, "gui.remove"));
        if (this.scrollFactions == null) {
            this.scrollFactions = new GuiCustomScroll(this, 0);
            this.scrollFactions.setSize(143, 208);
        }
        this.scrollFactions.guiLeft = this.guiLeft + 220;
        this.scrollFactions.guiTop = this.guiTop + 4;
        this.addScroll(this.scrollFactions);
        if (this.faction.id == -1) {
            return;
        }
        this.addTextField(new GuiNpcTextField(0, this, this.guiLeft + 40, this.guiTop + 4, 136, 20, this.faction.name));
        this.getTextField(0).func_146203_f(20);
        this.addLabel(new GuiNpcLabel(0, "gui.name", this.guiLeft + 8, this.guiTop + 9));
        this.addLabel(new GuiNpcLabel(10, "ID", this.guiLeft + 178, this.guiTop + 4));
        this.addLabel(new GuiNpcLabel(11, this.faction.id + "", this.guiLeft + 178, this.guiTop + 14));
        String color = Integer.toHexString(this.faction.color);
        while (color.length() < 6) {
            color = "0" + color;
        }
        this.addButton(new GuiNpcButton(10, this.guiLeft + 40, this.guiTop + 26, 60, 20, color));
        this.addLabel(new GuiNpcLabel(1, "gui.color", this.guiLeft + 8, this.guiTop + 31));
        this.getButton(10).setTextColor(this.faction.color);
        this.addLabel(new GuiNpcLabel(2, "faction.points", this.guiLeft + 8, this.guiTop + 53));
        this.addButton(new GuiNpcButton(2, this.guiLeft + 100, this.guiTop + 48, 45, 20, "selectServer.edit"));
        this.addLabel(new GuiNpcLabel(3, "faction.hidden", this.guiLeft + 8, this.guiTop + 75));
        this.addButton(new GuiNpcButton(3, this.guiLeft + 100, this.guiTop + 70, 45, 20, new String[]{"gui.no", "gui.yes"}, this.faction.hideFaction ? 1 : 0));
        this.addLabel(new GuiNpcLabel(4, "faction.attacked", this.guiLeft + 8, this.guiTop + 97));
        this.addButton(new GuiNpcButton(4, this.guiLeft + 100, this.guiTop + 92, 45, 20, new String[]{"gui.no", "gui.yes"}, this.faction.getsAttacked ? 1 : 0));
        this.addLabel(new GuiNpcLabel(6, "faction.hostiles", this.guiLeft + 8, this.guiTop + 145));
        ArrayList<String> hostileList = new ArrayList<String>(this.scrollFactions.getList());
        hostileList.remove(this.faction.name);
        HashSet<String> set = new HashSet<String>();
        for (String s : this.data.keySet()) {
            if (s.equals(this.faction.name) || !this.faction.attackFactions.contains(this.data.get(s))) continue;
            set.add(s);
        }
        GuiCustomScroll scrollHostileFactions = new GuiCustomScroll(this, 1, true);
        scrollHostileFactions.setSize(163, 58);
        scrollHostileFactions.guiLeft = this.guiLeft + 4;
        scrollHostileFactions.guiTop = this.guiTop + 154;
        scrollHostileFactions.setList(hostileList);
        scrollHostileFactions.setSelectedList(set);
        this.addScroll(scrollHostileFactions);
    }

    @Override
    protected void func_146284_a(GuiButton guibutton) {
        GuiNpcButton button = (GuiNpcButton)guibutton;
        if (button.field_146127_k == 0) {
            this.save();
            String name = I18n.func_74838_a((String)"gui.new");
            while (this.data.containsKey(name)) {
                name = name + "_";
            }
            Faction faction = new Faction(-1, name, 65280, 1000);
            NBTTagCompound compound = new NBTTagCompound();
            faction.writeNBT(compound);
            Client.sendData(EnumPacketServer.FactionSave, compound);
        }
        if (button.field_146127_k == 1 && this.data.containsKey(this.scrollFactions.getSelected())) {
            Client.sendData(EnumPacketServer.FactionRemove, this.data.get(this.selected));
            this.scrollFactions.clear();
            this.faction = new Faction();
            this.func_73866_w_();
        }
        if (button.field_146127_k == 2) {
            this.setSubGui(new SubGuiNpcFactionPoints(this.faction));
        }
        if (button.field_146127_k == 3) {
            boolean bl = this.faction.hideFaction = button.getValue() == 1;
        }
        if (button.field_146127_k == 4) {
            boolean bl = this.faction.getsAttacked = button.getValue() == 1;
        }
        if (button.field_146127_k == 10) {
            this.setSubGui(new SubGuiColorSelector(this.faction.color));
        }
    }

    @Override
    public void setGuiData(NBTTagCompound compound) {
        this.faction = new Faction();
        this.faction.readNBT(compound);
        this.setSelected(this.faction.name);
        this.func_73866_w_();
    }

    @Override
    public void setData(Vector<String> list, HashMap<String, Integer> data) {
        String name = this.scrollFactions.getSelected();
        this.data = data;
        this.scrollFactions.setList(list);
        if (name != null) {
            this.scrollFactions.setSelected(name);
        }
    }

    @Override
    public void setSelected(String selected) {
        this.selected = selected;
        this.scrollFactions.setSelected(selected);
    }

    @Override
    public void scrollClicked(int i, int j, int k, GuiCustomScroll guiCustomScroll) {
        if (guiCustomScroll.id == 0) {
            this.save();
            this.selected = this.scrollFactions.getSelected();
            Client.sendData(EnumPacketServer.FactionGet, this.data.get(this.selected));
        } else if (guiCustomScroll.id == 1) {
            HashSet<Integer> set = new HashSet<Integer>();
            for (String s : guiCustomScroll.getSelectedList()) {
                if (!this.data.containsKey(s)) continue;
                set.add(this.data.get(s));
            }
            this.faction.attackFactions = set;
            this.save();
        }
    }

    @Override
    public void save() {
        if (this.selected != null && this.data.containsKey(this.selected) && this.faction != null) {
            NBTTagCompound compound = new NBTTagCompound();
            this.faction.writeNBT(compound);
            Client.sendData(EnumPacketServer.FactionSave, compound);
        }
    }

    @Override
    public void unFocused(GuiNpcTextField guiNpcTextField) {
        if (this.faction.id == -1) {
            return;
        }
        if (guiNpcTextField.field_175208_g == 0) {
            String name = guiNpcTextField.func_146179_b();
            if (!name.isEmpty() && !this.data.containsKey(name)) {
                String old = this.faction.name;
                this.data.remove(this.faction.name);
                this.faction.name = name;
                this.data.put(this.faction.name, this.faction.id);
                this.selected = name;
                this.scrollFactions.replace(old, this.faction.name);
            }
        } else if (guiNpcTextField.field_175208_g == 1) {
            int color = 0;
            try {
                color = Integer.parseInt(guiNpcTextField.func_146179_b(), 16);
            }
            catch (NumberFormatException e) {
                color = 0;
            }
            this.faction.color = color;
            guiNpcTextField.func_146193_g(this.faction.color);
        }
    }

    @Override
    public void subGuiClosed(SubGuiInterface subgui) {
        if (subgui instanceof SubGuiColorSelector) {
            this.faction.color = ((SubGuiColorSelector)subgui).color;
            this.func_73866_w_();
        }
    }

    @Override
    public void scrollDoubleClicked(String selection, GuiCustomScroll scroll) {
    }
}

