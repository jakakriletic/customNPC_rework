/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.inventory.Container
 *  net.minecraft.util.ResourceLocation
 */
package noppes.npcs.client.gui.util;

import java.io.IOException;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.client.gui.util.GuiNpcMenu;
import noppes.npcs.entity.EntityNPCInterface;

public abstract class GuiContainerNPCInterface2
extends GuiContainerNPCInterface {
    private ResourceLocation background = new ResourceLocation("customnpcs", "textures/gui/menubg.png");
    private final ResourceLocation defaultBackground = new ResourceLocation("customnpcs", "textures/gui/menubg.png");
    private GuiNpcMenu menu;
    public int menuYOffset = 0;

    public GuiContainerNPCInterface2(EntityNPCInterface npc, Container cont) {
        this(npc, cont, -1);
    }

    public GuiContainerNPCInterface2(EntityNPCInterface npc, Container cont, int activeMenu) {
        super(npc, cont);
        this.field_146999_f = 420;
        this.menu = new GuiNpcMenu((GuiScreen)this, activeMenu, npc);
        this.title = "";
    }

    public void setBackground(String texture) {
        this.background = new ResourceLocation("customnpcs", "textures/gui/" + texture);
    }

    @Override
    public ResourceLocation getResource(String texture) {
        return new ResourceLocation("customnpcs", "textures/gui/" + texture);
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        this.menu.initGui(this.field_147003_i, this.field_147009_r + this.menuYOffset, this.field_146999_f);
    }

    @Override
    protected void func_73864_a(int i, int j, int k) throws IOException {
        super.func_73864_a(i, j, k);
        if (!this.hasSubGui()) {
            this.menu.mouseClicked(i, j, k);
        }
    }

    public void delete() {
        this.npc.delete();
        this.displayGuiScreen(null);
        this.field_146297_k.func_71381_h();
    }

    @Override
    protected void func_146976_a(float f, int i, int j) {
        this.func_146276_q_();
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.field_146297_k.field_71446_o.func_110577_a(this.background);
        this.func_73729_b(this.field_147003_i, this.field_147009_r, 0, 0, 256, 256);
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.field_146297_k.field_71446_o.func_110577_a(this.defaultBackground);
        this.func_73729_b(this.field_147003_i + this.field_146999_f - 200, this.field_147009_r, 26, 0, 200, 220);
        this.menu.drawElements(this.field_146289_q, i, j, this.field_146297_k, f);
        super.func_146976_a(f, i, j);
    }
}

