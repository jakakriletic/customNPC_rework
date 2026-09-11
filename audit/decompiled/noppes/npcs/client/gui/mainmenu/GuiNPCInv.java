/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.inventory.Slot
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.text.translation.I18n
 */
package noppes.npcs.client.gui.mainmenu;

import java.util.HashMap;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.client.Client;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface2;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcSlider;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.IGuiData;
import noppes.npcs.client.gui.util.ISliderListener;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.containers.ContainerNPCInv;
import noppes.npcs.entity.EntityNPCInterface;

public class GuiNPCInv
extends GuiContainerNPCInterface2
implements ISliderListener,
IGuiData {
    private HashMap<Integer, Integer> chances = new HashMap();
    private ContainerNPCInv container;
    private ResourceLocation slot;

    public GuiNPCInv(EntityNPCInterface npc, ContainerNPCInv container) {
        super(npc, container, 3);
        this.setBackground("npcinv.png");
        this.container = container;
        this.field_147000_g = 200;
        this.slot = this.getResource("slot.png");
        Client.sendData(EnumPacketServer.MainmenuInvGet, new Object[0]);
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        this.addLabel(new GuiNpcLabel(0, "inv.minExp", this.field_147003_i + 118, this.field_147009_r + 18));
        this.addTextField(new GuiNpcTextField(0, (GuiScreen)this, this.field_146289_q, this.field_147003_i + 108, this.field_147009_r + 29, 60, 20, this.npc.inventory.getExpMin() + ""));
        this.getTextField((int)0).numbersOnly = true;
        this.getTextField(0).setMinMaxDefault(0, Short.MAX_VALUE, 0);
        this.addLabel(new GuiNpcLabel(1, "inv.maxExp", this.field_147003_i + 118, this.field_147009_r + 52));
        this.addTextField(new GuiNpcTextField(1, (GuiScreen)this, this.field_146289_q, this.field_147003_i + 108, this.field_147009_r + 63, 60, 20, this.npc.inventory.getExpMax() + ""));
        this.getTextField((int)1).numbersOnly = true;
        this.getTextField(1).setMinMaxDefault(0, Short.MAX_VALUE, 0);
        this.addButton(new GuiNpcButton(10, this.field_147003_i + 88, this.field_147009_r + 88, 80, 20, new String[]{"stats.normal", "inv.auto"}, this.npc.inventory.lootMode));
        this.addLabel(new GuiNpcLabel(2, "inv.npcInventory", this.field_147003_i + 191, this.field_147009_r + 5));
        this.addLabel(new GuiNpcLabel(3, "inv.inventory", this.field_147003_i + 8, this.field_147009_r + 101));
        for (int i = 0; i < 9; ++i) {
            int chance = 100;
            if (this.npc.inventory.dropchance.containsKey(i)) {
                chance = this.npc.inventory.dropchance.get(i);
            }
            if (chance <= 0 || chance > 100) {
                chance = 100;
            }
            this.chances.put(i, chance);
            GuiNpcSlider slider = new GuiNpcSlider((GuiScreen)this, i, this.field_147003_i + 211, this.field_147009_r + 14 + i * 21, (float)chance / 100.0f);
            this.addSlider(slider);
        }
    }

    @Override
    protected void func_146284_a(GuiButton guibutton) {
        if (guibutton.field_146127_k == 10) {
            this.npc.inventory.lootMode = ((GuiNpcButton)guibutton).getValue();
        }
    }

    @Override
    protected void func_146976_a(float f, int i, int j) {
        super.func_146976_a(f, i, j);
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.field_146297_k.field_71446_o.func_110577_a(this.slot);
        for (int id = 4; id <= 6; ++id) {
            Slot slot = this.container.func_75139_a(id);
            if (!slot.func_75216_d()) continue;
            this.func_73729_b(this.field_147003_i + slot.field_75223_e - 1, this.field_147009_r + slot.field_75221_f - 1, 0, 0, 18, 18);
        }
    }

    @Override
    public void func_73863_a(int i, int j, float f) {
        int showname = this.npc.display.getShowName();
        this.npc.display.setShowName(1);
        this.drawNpc(50, 84);
        this.npc.display.setShowName(showname);
        super.func_73863_a(i, j, f);
    }

    @Override
    public void save() {
        this.npc.inventory.dropchance = this.chances;
        this.npc.inventory.setExp(this.getTextField(0).getInteger(), this.getTextField(1).getInteger());
        Client.sendData(EnumPacketServer.MainmenuInvSave, this.npc.inventory.writeEntityToNBT(new NBTTagCompound()));
    }

    @Override
    public void setGuiData(NBTTagCompound compound) {
        this.npc.inventory.readEntityFromNBT(compound);
        this.func_73866_w_();
    }

    @Override
    public void mouseDragged(GuiNpcSlider guiNpcSlider) {
        guiNpcSlider.field_146126_j = I18n.func_74838_a((String)"inv.dropChance") + ": " + (int)(guiNpcSlider.sliderValue * 100.0f) + "%";
    }

    @Override
    public void mousePressed(GuiNpcSlider guiNpcSlider) {
    }

    @Override
    public void mouseReleased(GuiNpcSlider guiNpcSlider) {
        this.chances.put(guiNpcSlider.field_146127_k, (int)(guiNpcSlider.sliderValue * 100.0f));
    }
}

