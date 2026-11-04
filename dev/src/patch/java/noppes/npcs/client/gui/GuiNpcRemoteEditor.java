/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.gui.GuiYesNo
 *  net.minecraft.client.gui.GuiYesNoCallback
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.util.text.translation.I18n
 */
package noppes.npcs.client.gui;

import java.util.HashMap;
import java.util.Vector;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.Client;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.ICustomScrollListener;
import noppes.npcs.client.gui.util.IScrollData;
import noppes.npcs.client.gui.util.ISubGuiListener;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.linked.LinkedGuiCodec;
import noppes.npcs.rework.linked.LinkedWandPanel;

public class GuiNpcRemoteEditor
extends GuiNPCInterface
implements IScrollData,
GuiYesNoCallback,
ICustomScrollListener,
ISubGuiListener {
    private GuiCustomScroll scroll;
    private HashMap<String, Integer> data = new HashMap();
    private final LinkedWandPanel rwPanel = CustomNpcs.RwLinkedGroups == 1 ? new LinkedWandPanel() : null;

    public GuiNpcRemoteEditor() {
        this.xSize = 256;
        this.setBackground("menubg.png");
    }

    @Override
    public void initPacket() {
        Client.sendData(EnumPacketServer.RemoteNpcsGet, new Object[0]);
        if (this.rwPanel != null) {
            this.rwPanel.refresh();
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        if (this.scroll == null) {
            this.scroll = new GuiCustomScroll(this, 0);
            this.scroll.setSize(165, 208);
        }
        this.scroll.guiLeft = this.guiLeft + 4;
        this.scroll.guiTop = this.guiTop + 4;
        this.addScroll(this.scroll);
        String title = I18n.translateToLocal((String)"remote.title");
        int x = (this.xSize - this.fontRenderer.getStringWidth(title)) / 2;
        this.addLabel(new GuiNpcLabel(0, title, this.guiLeft + x, this.guiTop - 8));
        this.addButton(new GuiNpcButton(0, this.guiLeft + 170, this.guiTop + 6, 82, 20, "selectServer.edit"));
        this.addButton(new GuiNpcButton(1, this.guiLeft + 170, this.guiTop + 28, 82, 20, "selectWorld.deleteButton"));
        this.addButton(new GuiNpcButton(2, this.guiLeft + 170, this.guiTop + 50, 82, 20, "remote.reset"));
        this.addButton(new GuiNpcButton(4, this.guiLeft + 170, this.guiTop + 72, 82, 20, "remote.tp"));
        this.addButton(new GuiNpcButton(5, this.guiLeft + 170, this.guiTop + 110, 82, 20, "remote.resetall"));
        this.addButton(new GuiNpcButton(3, this.guiLeft + 170, this.guiTop + 132, 82, 20, "remote.freeze"));
        if (this.rwPanel != null) {
            this.rwPanel.init(this, this.scroll);
        }
    }

    /** M13.3: the rows from the rwlinked channel; the first ones turn on the panel. */
    public void rwRows(LinkedGuiCodec.ListResponse rows) {
        if (this.rwPanel != null && this.rwPanel.rows(rows)) {
            this.initGui();
        }
    }

    public void rwReply(LinkedGuiCodec.Response reply) {
        if (this.rwPanel != null) {
            this.rwPanel.reply(reply);
        }
    }

    /** For the controller harness; null with RwLinkedGroups=0. */
    public LinkedWandPanel rwPanel() {
        return this.rwPanel;
    }

    private boolean rwActive() {
        return this.rwPanel != null && this.rwPanel.active();
    }

    @Override
    public void scrollClicked(int i, int j, int k, GuiCustomScroll scroll) {
        // The original also hands clicks on an open dialog to the list below it.
        if (this.rwActive() && !this.hasSubGui()) {
            this.rwPanel.clicked();
        }
    }

    @Override
    public void scrollDoubleClicked(String selection, GuiCustomScroll scroll) {
    }

    @Override
    public void subGuiClosed(SubGuiInterface subgui) {
        if (this.rwActive()) {
            this.rwPanel.closed(subgui);
        }
    }

    public void confirmClicked(boolean flag, int i) {
        if (flag && this.rwActive()) {
            this.rwPanel.confirmed();
        } else if (flag) {
            Client.sendData(EnumPacketServer.RemoteDelete, this.data.get(this.scroll.getSelected()));
        }
        NoppesUtil.openGUI((EntityPlayer)this.player, this);
    }

    @Override
    protected void actionPerformed(GuiButton guibutton) {
        int id = guibutton.id;
        if (id == 3) {
            Client.sendData(EnumPacketServer.RemoteFreeze, new Object[0]);
        }
        if (id == 5) {
            for (int ids : this.data.values()) {
                Client.sendData(EnumPacketServer.RemoteReset, ids);
                Entity entity = this.player.world.getEntityByID(ids);
                if (entity == null || !(entity instanceof EntityNPCInterface)) continue;
                ((EntityNPCInterface)entity).reset();
            }
        }
        if (this.rwActive()) {
            this.rwPanel.button(id, this);
            return;
        }
        if (!this.data.containsKey(this.scroll.getSelected())) {
            return;
        }
        if (id == 0) {
            Client.sendData(EnumPacketServer.RemoteMainMenu, this.data.get(this.scroll.getSelected()));
        }
        if (id == 1) {
            GuiYesNo guiyesno = new GuiYesNo((GuiYesNoCallback)this, "", I18n.translateToLocal((String)"gui.deleteMessage"), 0);
            this.displayGuiScreen((GuiScreen)guiyesno);
        }
        if (id == 2) {
            Client.sendData(EnumPacketServer.RemoteReset, this.data.get(this.scroll.getSelected()));
            Entity entity = this.player.world.getEntityByID(this.data.get(this.scroll.getSelected()).intValue());
            if (entity != null && entity instanceof EntityNPCInterface) {
                ((EntityNPCInterface)entity).reset();
            }
        }
        if (id == 4) {
            Client.sendData(EnumPacketServer.RemoteTpToNpc, this.data.get(this.scroll.getSelected()));
            this.close();
        }
    }

    @Override
    public void mouseClicked(int i, int j, int k) {
        super.mouseClicked(i, j, k);
        this.scroll.mouseClicked(i, j, k);
    }

    @Override
    public void keyTyped(char c, int i) {
        if (this.rwActive()) {
            // The inventory key is only a letter while typing in the filter.
            if (this.hasSubGui()) {
                super.keyTyped(c, i);
            } else if (i == 1 || !GuiNpcTextField.isActive() && this.isInventoryKey(i)) {
                this.close();
            } else {
                super.keyTyped(c, i);
                this.rwPanel.typed();
            }
            return;
        }
        if (i == 1 || this.isInventoryKey(i)) {
            this.close();
        }
    }

    @Override
    public void save() {
    }

    @Override
    public void setData(Vector<String> list, HashMap<String, Integer> data) {
        if (!this.rwActive()) {
            this.scroll.setList(list);
        }
        this.data = data;
    }

    @Override
    public void setSelected(String selected) {
        this.getButton(3).setDisplayText(selected);
    }
}

