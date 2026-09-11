/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.gui.GuiYesNo
 *  net.minecraft.client.gui.GuiYesNoCallback
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.util.text.translation.I18n
 *  org.lwjgl.input.Keyboard
 */
package noppes.npcs.client.gui.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.Client;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface2;
import noppes.npcs.client.gui.util.GuiMenuTopButton;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.entity.EntityNPCInterface;
import org.lwjgl.input.Keyboard;

public class GuiNpcMenu
implements GuiYesNoCallback {
    private GuiScreen parent;
    private GuiMenuTopButton[] topButtons = new GuiMenuTopButton[0];
    private int activeMenu;
    private EntityNPCInterface npc;

    public GuiNpcMenu(GuiScreen parent, int activeMenu, EntityNPCInterface npc) {
        this.parent = parent;
        this.activeMenu = activeMenu;
        this.npc = npc;
    }

    public void initGui(int guiLeft, int guiTop, int width) {
        Keyboard.enableRepeatEvents((boolean)true);
        GuiMenuTopButton display = new GuiMenuTopButton(1, guiLeft + 4, guiTop - 17, "menu.display");
        GuiMenuTopButton stats = new GuiMenuTopButton(2, display.field_146128_h + display.getWidth(), guiTop - 17, "menu.stats");
        GuiMenuTopButton ai = new GuiMenuTopButton(6, stats.field_146128_h + stats.getWidth(), guiTop - 17, "menu.ai");
        GuiMenuTopButton inv = new GuiMenuTopButton(3, ai.field_146128_h + ai.getWidth(), guiTop - 17, "menu.inventory");
        GuiMenuTopButton advanced = new GuiMenuTopButton(4, inv.field_146128_h + inv.getWidth(), guiTop - 17, "menu.advanced");
        GuiMenuTopButton global = new GuiMenuTopButton(5, advanced.field_146128_h + advanced.getWidth(), guiTop - 17, "menu.global");
        GuiMenuTopButton close = new GuiMenuTopButton(0, guiLeft + width - 22, guiTop - 17, "X");
        GuiMenuTopButton delete = new GuiMenuTopButton(66, guiLeft + width - 72, guiTop - 17, "selectWorld.deleteButton");
        delete.field_146128_h = close.field_146128_h - delete.getWidth();
        for (GuiMenuTopButton button : this.topButtons = new GuiMenuTopButton[]{display, stats, ai, inv, advanced, global, close, delete}) {
            button.active = button.field_146127_k == this.activeMenu;
        }
    }

    private void topButtonPressed(GuiMenuTopButton button) {
        if (button.field_146126_j.equals(this.activeMenu)) {
            return;
        }
        Minecraft mc = Minecraft.func_71410_x();
        NoppesUtil.clickSound();
        int id = button.field_146127_k;
        if (id == 0) {
            this.close();
            return;
        }
        if (id == 66) {
            GuiYesNo guiyesno = new GuiYesNo((GuiYesNoCallback)this, "", I18n.func_74838_a((String)"gui.deleteMessage"), 0);
            mc.func_147108_a((GuiScreen)guiyesno);
            return;
        }
        this.save();
        if (id == 1) {
            CustomNpcs.proxy.openGui(this.npc, EnumGuiType.MainMenuDisplay);
        } else if (id == 2) {
            CustomNpcs.proxy.openGui(this.npc, EnumGuiType.MainMenuStats);
        } else if (id == 3) {
            NoppesUtil.requestOpenGUI(EnumGuiType.MainMenuInv);
        } else if (id == 4) {
            CustomNpcs.proxy.openGui(this.npc, EnumGuiType.MainMenuAdvanced);
        } else if (id == 5) {
            CustomNpcs.proxy.openGui(this.npc, EnumGuiType.MainMenuGlobal);
        } else if (id == 6) {
            CustomNpcs.proxy.openGui(this.npc, EnumGuiType.MainMenuAI);
        }
        this.activeMenu = id;
    }

    private void save() {
        GuiNpcTextField.unfocus();
        if (this.parent instanceof GuiContainerNPCInterface2) {
            ((GuiContainerNPCInterface2)this.parent).save();
        }
        if (this.parent instanceof GuiNPCInterface2) {
            ((GuiNPCInterface2)this.parent).save();
        }
    }

    private void close() {
        if (this.parent instanceof GuiContainerNPCInterface2) {
            ((GuiContainerNPCInterface2)this.parent).close();
        }
        if (this.parent instanceof GuiNPCInterface2) {
            ((GuiNPCInterface2)this.parent).close();
        }
        if (this.npc != null) {
            this.npc.reset();
            Client.sendData(EnumPacketServer.NpcMenuClose, new Object[0]);
        }
    }

    public void mouseClicked(int i, int j, int k) {
        if (k == 0) {
            Minecraft mc = Minecraft.func_71410_x();
            for (GuiMenuTopButton button : this.topButtons) {
                if (!button.func_146116_c(mc, i, j)) continue;
                this.topButtonPressed(button);
            }
        }
    }

    public void drawElements(FontRenderer fontRenderer, int i, int j, Minecraft mc, float f) {
        for (GuiMenuTopButton button : this.topButtons) {
            button.func_191745_a(mc, i, j, f);
        }
    }

    public void func_73878_a(boolean flag, int i) {
        Minecraft mc = Minecraft.func_71410_x();
        if (flag) {
            Client.sendData(EnumPacketServer.Delete, new Object[0]);
            mc.func_147108_a(null);
            mc.func_71381_h();
        } else {
            NoppesUtil.openGUI((EntityPlayer)mc.field_71439_g, this.parent);
        }
    }
}

