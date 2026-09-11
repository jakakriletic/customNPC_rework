/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.nbt.NBTTagCompound
 */
package noppes.npcs.client.gui.roles;

import java.util.ArrayList;
import java.util.HashMap;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.client.Client;
import noppes.npcs.client.gui.util.GuiButtonBiDirectional;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcSlider;
import noppes.npcs.client.gui.util.ICustomScrollListener;
import noppes.npcs.client.gui.util.ISliderListener;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.roles.JobPuppet;

public class GuiNpcPuppet
extends GuiNPCInterface
implements ISliderListener,
ICustomScrollListener {
    private GuiScreen parent;
    private JobPuppet job;
    private String selectedName;
    private boolean isStart = true;
    public HashMap<String, JobPuppet.PartConfig> data = new HashMap();
    private GuiCustomScroll scroll;

    public GuiNpcPuppet(GuiScreen parent, EntityCustomNpc npc) {
        super(npc);
        this.parent = parent;
        this.ySize = 230;
        this.xSize = 400;
        this.job = (JobPuppet)npc.jobInterface;
        this.closeOnEsc = true;
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        int y = this.guiTop;
        this.addButton(new GuiNpcButton(30, this.guiLeft + 110, y += 14, 60, 20, new String[]{"gui.yes", "gui.no"}, this.job.whileStanding ? 0 : 1));
        this.addLabel(new GuiNpcLabel(30, "puppet.standing", this.guiLeft + 10, y + 5, 0xFFFFFF));
        this.addButton(new GuiNpcButton(31, this.guiLeft + 110, y += 22, 60, 20, new String[]{"gui.yes", "gui.no"}, this.job.whileMoving ? 0 : 1));
        this.addLabel(new GuiNpcLabel(31, "puppet.walking", this.guiLeft + 10, y + 5, 0xFFFFFF));
        this.addButton(new GuiNpcButton(32, this.guiLeft + 110, y += 22, 60, 20, new String[]{"gui.yes", "gui.no"}, this.job.whileAttacking ? 0 : 1));
        this.addLabel(new GuiNpcLabel(32, "puppet.attacking", this.guiLeft + 10, y + 5, 0xFFFFFF));
        this.addButton(new GuiNpcButton(33, this.guiLeft + 110, y += 22, 60, 20, new String[]{"gui.yes", "gui.no"}, this.job.animate ? 0 : 1));
        this.addLabel(new GuiNpcLabel(33, "puppet.animation", this.guiLeft + 10, y + 5, 0xFFFFFF));
        if (this.job.animate) {
            this.addButton(new GuiButtonBiDirectional(34, this.guiLeft + 240, y, 60, 20, new String[]{"1", "2", "3", "4", "5", "6", "7", "8"}, this.job.animationSpeed));
            this.addLabel(new GuiNpcLabel(34, "stats.speed", this.guiLeft + 190, y + 5, 0xFFFFFF));
        }
        y += 34;
        HashMap<String, JobPuppet.PartConfig> data = new HashMap<String, JobPuppet.PartConfig>();
        if (this.isStart) {
            data.put("model.head", this.job.head);
            data.put("model.body", this.job.body);
            data.put("model.larm", this.job.larm);
            data.put("model.rarm", this.job.rarm);
            data.put("model.lleg", this.job.lleg);
            data.put("model.rleg", this.job.rleg);
        } else {
            data.put("model.head", this.job.head2);
            data.put("model.body", this.job.body2);
            data.put("model.larm", this.job.larm2);
            data.put("model.rarm", this.job.rarm2);
            data.put("model.lleg", this.job.lleg2);
            data.put("model.rleg", this.job.rleg2);
        }
        this.data = data;
        if (this.scroll == null) {
            this.scroll = new GuiCustomScroll(this, 0);
        }
        this.scroll.setList(new ArrayList<String>(data.keySet()));
        this.scroll.guiLeft = this.guiLeft + 10;
        this.scroll.guiTop = y;
        this.scroll.setSize(80, 100);
        this.addScroll(this.scroll);
        if (this.selectedName != null) {
            this.scroll.setSelected(this.selectedName);
            this.drawSlider(y, (JobPuppet.PartConfig)data.get(this.selectedName));
        }
        this.addButton(new GuiNpcButton(66, this.guiLeft + this.xSize - 22, this.guiTop, 20, 20, "X"));
        if (this.job.animate) {
            this.addButton(new GuiNpcButton(67, this.guiLeft + 10, y + 110, 70, 20, "gui.start"));
            this.addButton(new GuiNpcButton(68, this.guiLeft + 90, y + 110, 70, 20, "gui.end"));
            this.getButton((int)67).field_146124_l = !this.isStart;
            this.getButton((int)68).field_146124_l = this.isStart;
        }
    }

    private void drawSlider(int y, JobPuppet.PartConfig config) {
        this.addButton(new GuiNpcButton(29, this.guiLeft + 140, y, 80, 20, new String[]{"gui.enabled", "gui.disabled"}, config.disabled ? 1 : 0));
        this.addLabel(new GuiNpcLabel(10, "X", this.guiLeft + 100, (y += 22) + 5, 0xFFFFFF));
        this.addSlider(new GuiNpcSlider(this, 10, this.guiLeft + 120, y, (config.rotationX + 1.0f) / 2.0f));
        this.addLabel(new GuiNpcLabel(11, "Y", this.guiLeft + 100, (y += 22) + 5, 0xFFFFFF));
        this.addSlider(new GuiNpcSlider(this, 11, this.guiLeft + 120, y, (config.rotationY + 1.0f) / 2.0f));
        this.addLabel(new GuiNpcLabel(12, "Z", this.guiLeft + 100, (y += 22) + 5, 0xFFFFFF));
        this.addSlider(new GuiNpcSlider(this, 12, this.guiLeft + 120, y, (config.rotationZ + 1.0f) / 2.0f));
    }

    @Override
    public void func_73863_a(int i, int j, float f) {
        this.drawNpc(320, 200);
        super.func_73863_a(i, j, f);
    }

    @Override
    protected void func_146284_a(GuiButton btn) {
        super.func_146284_a(btn);
        if (!(btn instanceof GuiNpcButton)) {
            return;
        }
        GuiNpcButton button = (GuiNpcButton)btn;
        if (btn.field_146127_k == 29) {
            boolean bl = this.data.get((Object)this.selectedName).disabled = button.getValue() == 1;
        }
        if (btn.field_146127_k == 30) {
            boolean bl = this.job.whileStanding = button.getValue() == 0;
        }
        if (btn.field_146127_k == 31) {
            boolean bl = this.job.whileMoving = button.getValue() == 0;
        }
        if (btn.field_146127_k == 32) {
            boolean bl = this.job.whileAttacking = button.getValue() == 0;
        }
        if (btn.field_146127_k == 33) {
            this.job.animate = button.getValue() == 0;
            this.isStart = true;
            this.func_73866_w_();
        }
        if (btn.field_146127_k == 34) {
            this.job.animationSpeed = button.getValue();
        }
        if (btn.field_146127_k == 66) {
            this.close();
        }
        if (btn.field_146127_k == 67) {
            this.isStart = true;
            this.func_73866_w_();
        }
        if (btn.field_146127_k == 68) {
            this.isStart = false;
            this.func_73866_w_();
        }
    }

    @Override
    public void close() {
        this.field_146297_k.func_147108_a(this.parent);
        Client.sendData(EnumPacketServer.JobSave, this.job.writeToNBT(new NBTTagCompound()));
    }

    @Override
    public void mouseDragged(GuiNpcSlider slider) {
        int percent = (int)(slider.sliderValue * 360.0f);
        slider.setString(percent + "%");
        JobPuppet.PartConfig part = this.data.get(this.selectedName);
        if (slider.field_146127_k == 10) {
            part.rotationX = (slider.sliderValue - 0.5f) * 2.0f;
        }
        if (slider.field_146127_k == 11) {
            part.rotationY = (slider.sliderValue - 0.5f) * 2.0f;
        }
        if (slider.field_146127_k == 12) {
            part.rotationZ = (slider.sliderValue - 0.5f) * 2.0f;
        }
        this.npc.updateHitbox();
    }

    @Override
    public void mousePressed(GuiNpcSlider slider) {
    }

    @Override
    public void mouseReleased(GuiNpcSlider slider) {
    }

    @Override
    public void save() {
    }

    @Override
    public void scrollClicked(int i, int j, int k, GuiCustomScroll guiCustomScroll) {
        this.selectedName = guiCustomScroll.getSelected();
        this.func_73866_w_();
    }

    @Override
    public void scrollDoubleClicked(String selection, GuiCustomScroll scroll) {
    }
}

