/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.SoundCategory
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.text.ITextComponent
 *  org.lwjgl.input.Mouse
 */
package noppes.npcs.client.gui.player;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import noppes.npcs.NoppesStringUtils;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.TextBlockClient;
import noppes.npcs.client.controllers.MusicController;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.IGuiClose;
import noppes.npcs.constants.EnumPlayerPacket;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.DialogOption;
import noppes.npcs.entity.EntityNPCInterface;
import org.lwjgl.input.Mouse;

public class GuiDialogInteract
extends GuiNPCInterface
implements IGuiClose {
    private Dialog dialog;
    private int selected = 0;
    private List<TextBlockClient> lines = new ArrayList<TextBlockClient>();
    private List<Integer> options = new ArrayList<Integer>();
    private int rowStart = 0;
    private int rowTotal = 0;
    private int dialogHeight = 180;
    private ResourceLocation wheel;
    private ResourceLocation[] wheelparts;
    private ResourceLocation indicator;
    private boolean isGrabbed = false;
    private int selectedX = 0;
    private int selectedY = 0;

    public GuiDialogInteract(EntityNPCInterface npc, Dialog dialog) {
        super(npc);
        this.dialog = dialog;
        this.appendDialog(dialog);
        this.ySize = 238;
        this.wheel = this.getResource("wheel.png");
        this.indicator = this.getResource("indicator.png");
        this.wheelparts = new ResourceLocation[]{this.getResource("wheel1.png"), this.getResource("wheel2.png"), this.getResource("wheel3.png"), this.getResource("wheel4.png"), this.getResource("wheel5.png"), this.getResource("wheel6.png")};
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        this.isGrabbed = false;
        this.grabMouse(this.dialog.showWheel);
        this.guiTop = this.field_146295_m - this.ySize;
        this.calculateRowHeight();
    }

    public void grabMouse(boolean grab) {
        if (grab && !this.isGrabbed) {
            Minecraft.func_71410_x().field_71417_B.func_74372_a();
            this.isGrabbed = true;
        } else if (!grab && this.isGrabbed) {
            Minecraft.func_71410_x().field_71417_B.func_74373_b();
            this.isGrabbed = false;
        }
    }

    @Override
    public void func_73863_a(int i, int j, float f) {
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.func_73733_a(0, 0, this.field_146294_l, this.field_146295_m, -587202560, -587202560);
        if (!this.dialog.hideNPC) {
            int l = -70;
            int i1 = this.ySize;
            this.drawNpc((EntityLivingBase)this.npc, l, i1, 1.4f, 0);
        }
        super.func_73863_a(i, j, f);
        GlStateManager.func_179147_l();
        GlStateManager.func_179120_a((int)770, (int)771, (int)1, (int)0);
        GlStateManager.func_179141_d();
        GlStateManager.func_179094_E();
        GlStateManager.func_179109_b((float)0.0f, (float)0.5f, (float)100.065f);
        int count = 0;
        for (TextBlockClient block : new ArrayList<TextBlockClient>(this.lines)) {
            int size = ClientProxy.Font.width(block.getName() + ": ");
            this.drawString(block.getName() + ": ", -4 - size, block.color, count);
            for (ITextComponent line : block.lines) {
                this.drawString(line.func_150254_d(), 0, block.color, count);
                ++count;
            }
            ++count;
        }
        if (!this.options.isEmpty()) {
            if (!this.dialog.showWheel) {
                this.drawLinedOptions(j);
            } else {
                this.drawWheel();
            }
        }
        GlStateManager.func_179121_F();
    }

    private void drawWheel() {
        int yoffset = this.guiTop + this.dialogHeight + 14;
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.field_146297_k.field_71446_o.func_110577_a(this.wheel);
        this.func_73729_b(this.field_146294_l / 2 - 31, yoffset, 0, 0, 63, 40);
        this.selectedX += Mouse.getDX();
        this.selectedY += Mouse.getDY();
        int limit = 80;
        if (this.selectedX > limit) {
            this.selectedX = limit;
        }
        if (this.selectedX < -limit) {
            this.selectedX = -limit;
        }
        if (this.selectedY > limit) {
            this.selectedY = limit;
        }
        if (this.selectedY < -limit) {
            this.selectedY = -limit;
        }
        this.selected = 1;
        if (this.selectedY < -20) {
            ++this.selected;
        }
        if (this.selectedY > 54) {
            --this.selected;
        }
        if (this.selectedX < 0) {
            this.selected += 3;
        }
        this.field_146297_k.field_71446_o.func_110577_a(this.wheelparts[this.selected]);
        this.func_73729_b(this.field_146294_l / 2 - 31, yoffset, 0, 0, 85, 55);
        for (int slot : this.dialog.options.keySet()) {
            DialogOption option = this.dialog.options.get(slot);
            if (option == null || option.optionType == 2 || option.hasDialog() && !option.getDialog().availability.isAvailable((EntityPlayer)this.player)) continue;
            int color = option.optionColor;
            if (slot == this.selected) {
                color = 8622040;
            }
            int height = ClientProxy.Font.height(option.title);
            if (slot == 0) {
                this.func_73731_b(this.field_146289_q, option.title, this.field_146294_l / 2 + 13, yoffset - height, color);
            }
            if (slot == 1) {
                this.func_73731_b(this.field_146289_q, option.title, this.field_146294_l / 2 + 33, yoffset - height / 2 + 14, color);
            }
            if (slot == 2) {
                this.func_73731_b(this.field_146289_q, option.title, this.field_146294_l / 2 + 27, yoffset + 27, color);
            }
            if (slot == 3) {
                this.func_73731_b(this.field_146289_q, option.title, this.field_146294_l / 2 - 13 - ClientProxy.Font.width(option.title), yoffset - height, color);
            }
            if (slot == 4) {
                this.func_73731_b(this.field_146289_q, option.title, this.field_146294_l / 2 - 33 - ClientProxy.Font.width(option.title), yoffset - height / 2 + 14, color);
            }
            if (slot != 5) continue;
            this.func_73731_b(this.field_146289_q, option.title, this.field_146294_l / 2 - 27 - ClientProxy.Font.width(option.title), yoffset + 27, color);
        }
        this.field_146297_k.field_71446_o.func_110577_a(this.indicator);
        this.func_73729_b(this.field_146294_l / 2 + this.selectedX / 4 - 2, yoffset + 16 - this.selectedY / 6, 0, 0, 8, 8);
    }

    private void drawLinedOptions(int j) {
        int selected;
        this.func_73730_a(this.guiLeft - 60, this.guiLeft + this.xSize + 120, this.guiTop + this.dialogHeight - ClientProxy.Font.height(null) / 3, -1);
        int offset = this.dialogHeight;
        if (j >= this.guiTop + offset && (selected = (j - (this.guiTop + offset)) / ClientProxy.Font.height(null)) < this.options.size()) {
            this.selected = selected;
        }
        if (this.selected >= this.options.size()) {
            this.selected = 0;
        }
        if (this.selected < 0) {
            this.selected = 0;
        }
        for (int k = 0; k < this.options.size(); ++k) {
            int id = this.options.get(k);
            DialogOption option = this.dialog.options.get(id);
            int y = this.guiTop + offset + k * ClientProxy.Font.height(null);
            if (this.selected == k) {
                this.func_73731_b(this.field_146289_q, ">", this.guiLeft - 60, y, 0xE0E0E0);
            }
            this.func_73731_b(this.field_146289_q, NoppesStringUtils.formatText(option.title, new Object[]{this.player, this.npc}), this.guiLeft - 30, y, option.optionColor);
        }
    }

    private void drawString(String text, int left, int color, int count) {
        int height = count - this.rowStart;
        this.func_73731_b(this.field_146289_q, text, this.guiLeft + left, this.guiTop + height * ClientProxy.Font.height(null), color);
    }

    public void func_73731_b(FontRenderer fontRendererIn, String text, int x, int y, int color) {
        ClientProxy.Font.drawString(text, x, y, color);
    }

    private int getSelected() {
        if (this.selected <= 0) {
            return 0;
        }
        if (this.selected < this.options.size()) {
            return this.selected;
        }
        return this.options.size() - 1;
    }

    @Override
    public void func_73869_a(char c, int i) {
        if (i == this.field_146297_k.field_71474_y.field_74351_w.func_151463_i() || i == 200) {
            --this.selected;
        }
        if (i == this.field_146297_k.field_71474_y.field_74368_y.func_151463_i() || i == 208) {
            ++this.selected;
        }
        if (i == 28) {
            this.handleDialogSelection();
        }
        if (this.closeOnEsc && (i == 1 || this.isInventoryKey(i))) {
            NoppesUtilPlayer.sendData(EnumPlayerPacket.Dialog, this.dialog.id, -1);
            this.closed();
            this.close();
        }
        super.func_73869_a(c, i);
    }

    @Override
    public void func_73864_a(int i, int j, int k) {
        if ((this.selected == -1 && this.options.isEmpty() || this.selected >= 0) && k == 0) {
            this.handleDialogSelection();
        }
    }

    private void handleDialogSelection() {
        int optionId = -1;
        if (this.dialog.showWheel) {
            optionId = this.selected;
        } else if (!this.options.isEmpty()) {
            optionId = this.options.get(this.selected);
        }
        NoppesUtilPlayer.sendData(EnumPlayerPacket.Dialog, this.dialog.id, optionId);
        if (this.dialog == null || !this.dialog.hasOtherOptions() || this.options.isEmpty()) {
            this.closed();
            this.close();
            return;
        }
        DialogOption option = this.dialog.options.get(optionId);
        if (option == null || option.optionType != 1) {
            this.closed();
            this.close();
            return;
        }
        this.lines.add(new TextBlockClient(this.player.getDisplayNameString(), option.title, 280, option.optionColor, new Object[]{this.player, this.npc}));
        this.calculateRowHeight();
        NoppesUtil.clickSound();
    }

    private void closed() {
        this.grabMouse(false);
        NoppesUtilPlayer.sendData(EnumPlayerPacket.CheckQuestCompletion, new Object[0]);
    }

    public void appendDialog(Dialog dialog) {
        this.closeOnEsc = !dialog.disableEsc;
        this.dialog = dialog;
        this.options = new ArrayList<Integer>();
        if (dialog.sound != null && !dialog.sound.isEmpty()) {
            MusicController.Instance.stopMusic();
            BlockPos pos = this.npc.func_180425_c();
            MusicController.Instance.playSound(SoundCategory.VOICE, dialog.sound, pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p(), 1.0f, 1.0f);
        }
        this.lines.add(new TextBlockClient(this.npc, dialog.text, 280, 0xE0E0E0, new Object[]{this.player, this.npc}));
        for (int slot : dialog.options.keySet()) {
            DialogOption option = dialog.options.get(slot);
            if (option == null || !option.isAvailable((EntityPlayer)this.player)) continue;
            this.options.add(slot);
        }
        this.calculateRowHeight();
        this.grabMouse(dialog.showWheel);
    }

    private void calculateRowHeight() {
        if (this.dialog.showWheel) {
            this.dialogHeight = this.ySize - 58;
        } else {
            this.dialogHeight = this.ySize - 3 * ClientProxy.Font.height(null) - 4;
            if (this.dialog.options.size() > 3) {
                this.dialogHeight -= (this.dialog.options.size() - 3) * ClientProxy.Font.height(null);
            }
        }
        this.rowTotal = 0;
        for (TextBlockClient block : this.lines) {
            this.rowTotal += block.lines.size() + 1;
        }
        int max = this.dialogHeight / ClientProxy.Font.height(null);
        this.rowStart = this.rowTotal - max;
        if (this.rowStart < 0) {
            this.rowStart = 0;
        }
    }

    @Override
    public void setClose(int i, NBTTagCompound data) {
        this.grabMouse(false);
    }

    @Override
    public void save() {
    }
}

