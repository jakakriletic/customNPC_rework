/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 */
package noppes.npcs.client.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.NBTTags;
import noppes.npcs.client.Client;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.IGuiData;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAI;

public class GuiNpcPather
extends GuiNPCInterface
implements IGuiData {
    private GuiCustomScroll scroll;
    private HashMap<String, Integer> data = new HashMap();
    private DataAI ai;

    public GuiNpcPather(EntityNPCInterface npc) {
        this.drawDefaultBackground = false;
        this.xSize = 176;
        this.title = "Npc Pather";
        this.setBackground("smallbg.png");
        this.ai = npc.ais;
    }

    @Override
    public void initPacket() {
        Client.sendData(EnumPacketServer.MovingPathGet, new Object[0]);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.scroll = new GuiCustomScroll(this, 0);
        this.scroll.setSize(160, 164);
        ArrayList<String> list = new ArrayList<String>();
        for (int[] arr : this.ai.getMovingPath()) {
            list.add("x:" + arr[0] + " y:" + arr[1] + " z:" + arr[2]);
        }
        this.scroll.setUnsortedList(list);
        this.scroll.guiLeft = this.guiLeft + 7;
        this.scroll.guiTop = this.guiTop + 12;
        this.addScroll(this.scroll);
        this.addButton(new GuiNpcButton(0, this.guiLeft + 6, this.guiTop + 178, 52, 20, "gui.down"));
        this.addButton(new GuiNpcButton(1, this.guiLeft + 62, this.guiTop + 178, 52, 20, "gui.up"));
        this.addButton(new GuiNpcButton(2, this.guiLeft + 118, this.guiTop + 178, 52, 20, "selectWorld.deleteButton"));
    }

    @Override
    protected void actionPerformed(GuiButton guibutton) {
        int[] b;
        int[] a;
        int selected;
        List<int[]> list;
        if (this.scroll.selected < 0) {
            return;
        }
        int id = guibutton.id;
        if (id == 0) {
            list = this.ai.getMovingPath();
            selected = this.scroll.selected;
            if (list.size() <= selected + 1) {
                return;
            }
            a = list.get(selected);
            b = list.get(selected + 1);
            list.set(selected, b);
            list.set(selected + 1, a);
            this.ai.setMovingPath(list);
            this.initGui();
            this.scroll.selected = selected + 1;
        }
        if (id == 1) {
            if (this.scroll.selected - 1 < 0) {
                return;
            }
            list = this.ai.getMovingPath();
            selected = this.scroll.selected;
            a = list.get(selected);
            b = list.get(selected - 1);
            list.set(selected, b);
            list.set(selected - 1, a);
            this.ai.setMovingPath(list);
            this.initGui();
            this.scroll.selected = selected - 1;
        }
        if (id == 2) {
            list = this.ai.getMovingPath();
            if (list.size() <= 1) {
                return;
            }
            list.remove(this.scroll.selected);
            this.ai.setMovingPath(list);
            this.initGui();
        }
    }

    protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
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
        NBTTagCompound compound = new NBTTagCompound();
        compound.setTag("MovingPathNew", (NBTBase)NBTTags.nbtIntegerArraySet(this.ai.getMovingPath()));
        Client.sendData(EnumPacketServer.MovingPathSave, compound);
    }

    @Override
    public void setGuiData(NBTTagCompound compound) {
        this.ai.readToNBT(compound);
        this.initGui();
    }
}

