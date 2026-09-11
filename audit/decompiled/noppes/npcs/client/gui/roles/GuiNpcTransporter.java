/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.entity.Entity
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.math.BlockPos
 */
package noppes.npcs.client.gui.roles;

import java.util.HashMap;
import java.util.Vector;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import noppes.npcs.client.Client;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.IGuiData;
import noppes.npcs.client.gui.util.IScrollData;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.controllers.data.TransportLocation;
import noppes.npcs.entity.EntityNPCInterface;

public class GuiNpcTransporter
extends GuiNPCInterface2
implements IScrollData,
IGuiData {
    private GuiCustomScroll scroll;
    public TransportLocation location = new TransportLocation();
    private HashMap<String, Integer> data = new HashMap();

    public GuiNpcTransporter(EntityNPCInterface npc) {
        super(npc);
    }

    @Override
    public void initPacket() {
        Client.sendData(EnumPacketServer.TransportCategoriesGet, new Object[0]);
        Client.sendData(EnumPacketServer.TransportGetLocation, new Object[0]);
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        Vector<String> list = new Vector<String>();
        list.addAll(this.data.keySet());
        if (this.scroll == null) {
            this.scroll = new GuiCustomScroll(this, 0);
            this.scroll.setSize(143, 208);
        }
        this.scroll.guiLeft = this.guiLeft + 214;
        this.scroll.guiTop = this.guiTop + 4;
        this.addScroll(this.scroll);
        this.addLabel(new GuiNpcLabel(0, "gui.name", this.guiLeft + 4, this.field_146295_m + 8));
        this.addTextField(new GuiNpcTextField(0, this, this.field_146289_q, this.guiLeft + 60, this.guiTop + 3, 140, 20, this.location.name));
        this.addButton(new GuiNpcButton(0, this.guiLeft + 4, this.guiTop + 31, new String[]{"transporter.discovered", "transporter.start", "transporter.interaction"}, this.location.type));
    }

    @Override
    protected void func_146284_a(GuiButton guibutton) {
        GuiNpcButton button = (GuiNpcButton)guibutton;
        if (button.field_146127_k == 0) {
            this.location.type = button.getValue();
        }
    }

    @Override
    public void save() {
        if (!this.scroll.hasSelected()) {
            return;
        }
        String name = this.getTextField(0).func_146179_b();
        if (!name.isEmpty()) {
            this.location.name = name;
        }
        this.location.pos = new BlockPos((Entity)this.player);
        this.location.dimension = this.player.field_71093_bK;
        int cat = this.data.get(this.scroll.getSelected());
        Client.sendData(EnumPacketServer.TransportSave, cat, this.location.writeNBT());
    }

    @Override
    public void setData(Vector<String> list, HashMap<String, Integer> data) {
        this.data = data;
        this.scroll.setList(list);
    }

    @Override
    public void setSelected(String selected) {
        this.scroll.setSelected(selected);
    }

    @Override
    public void setGuiData(NBTTagCompound compound) {
        TransportLocation loc = new TransportLocation();
        loc.readNBT(compound);
        this.location = loc;
        this.func_73866_w_();
    }
}

