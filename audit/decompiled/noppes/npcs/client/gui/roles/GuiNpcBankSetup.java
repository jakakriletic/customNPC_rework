/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.nbt.NBTTagCompound
 */
package noppes.npcs.client.gui.roles;

import java.util.HashMap;
import java.util.Vector;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.client.Client;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.client.gui.util.ICustomScrollListener;
import noppes.npcs.client.gui.util.IScrollData;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.controllers.data.Bank;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleBank;

public class GuiNpcBankSetup
extends GuiNPCInterface2
implements IScrollData,
ICustomScrollListener {
    private GuiCustomScroll scroll;
    private HashMap<String, Integer> data = new HashMap();
    private RoleBank role;

    public GuiNpcBankSetup(EntityNPCInterface npc) {
        super(npc);
        this.role = (RoleBank)npc.roleInterface;
    }

    @Override
    public void initPacket() {
        Client.sendData(EnumPacketServer.BanksGet, new Object[0]);
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        if (this.scroll == null) {
            this.scroll = new GuiCustomScroll(this, 0);
        }
        this.scroll.setSize(200, 152);
        this.scroll.guiLeft = this.guiLeft + 85;
        this.scroll.guiTop = this.guiTop + 20;
        this.addScroll(this.scroll);
    }

    @Override
    protected void func_146284_a(GuiButton guibutton) {
    }

    @Override
    public void setData(Vector<String> list, HashMap<String, Integer> data) {
        String name = null;
        Bank bank = this.role.getBank();
        if (bank != null) {
            name = bank.name;
        }
        this.data = data;
        this.scroll.setList(list);
        if (name != null) {
            this.setSelected(name);
        }
    }

    @Override
    public void func_73864_a(int i, int j, int k) {
        super.func_73864_a(i, j, k);
        if (k == 0 && this.scroll != null) {
            this.scroll.func_73864_a(i, j, k);
        }
    }

    @Override
    public void setSelected(String selected) {
        this.scroll.setSelected(selected);
    }

    @Override
    public void scrollClicked(int i, int j, int k, GuiCustomScroll guiCustomScroll) {
        if (guiCustomScroll.id == 0) {
            this.role.bankId = this.data.get(this.scroll.getSelected());
            this.save();
        }
    }

    @Override
    public void save() {
        Client.sendData(EnumPacketServer.RoleSave, this.role.writeToNBT(new NBTTagCompound()));
    }

    @Override
    public void scrollDoubleClicked(String selection, GuiCustomScroll scroll) {
    }
}

