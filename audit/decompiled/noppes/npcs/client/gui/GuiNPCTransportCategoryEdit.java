/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.entity.player.EntityPlayer
 */
package noppes.npcs.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import noppes.npcs.client.Client;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.entity.EntityNPCInterface;

public class GuiNPCTransportCategoryEdit
extends GuiNPCInterface {
    private GuiScreen parent;
    private String name;
    private int id;

    public GuiNPCTransportCategoryEdit(EntityNPCInterface npc, GuiScreen parent, String name, int id) {
        super(npc);
        this.parent = parent;
        this.name = name;
        this.id = id;
        this.title = "Npc Transport Category";
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        this.addTextField(new GuiNpcTextField(1, this, this.field_146289_q, this.field_146294_l / 2 - 40, 100, 140, 20, this.name));
        this.addLabel(new GuiNpcLabel(1, "Title:", this.field_146294_l / 2 - 100 + 4, 105, 0xFFFFFF));
        this.addButton(new GuiNpcButton(2, this.field_146294_l / 2 - 100, 210, 98, 20, "gui.back"));
        this.addButton(new GuiNpcButton(3, this.field_146294_l / 2 + 2, 210, 98, 20, "Save"));
    }

    @Override
    public void func_73863_a(int i, int j, float f) {
        super.func_73863_a(i, j, f);
    }

    @Override
    protected void func_146284_a(GuiButton guibutton) {
        int id = guibutton.field_146127_k;
        if (id == 2) {
            NoppesUtil.openGUI((EntityPlayer)this.player, this.parent);
            Client.sendData(EnumPacketServer.TransportCategoriesGet, new Object[0]);
        }
        if (id == 3) {
            this.save();
            NoppesUtil.openGUI((EntityPlayer)this.player, this.parent);
            Client.sendData(EnumPacketServer.TransportCategoriesGet, new Object[0]);
        }
    }

    @Override
    public void save() {
        String name = this.getTextField(1).func_146179_b();
        if (name.trim().isEmpty()) {
            return;
        }
        Client.sendData(EnumPacketServer.TransportCategorySave, name, this.id);
    }
}

