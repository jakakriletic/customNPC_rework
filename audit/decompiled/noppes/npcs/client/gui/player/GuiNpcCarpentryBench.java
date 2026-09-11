/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.text.translation.I18n
 */
package noppes.npcs.client.gui.player;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.client.CustomNpcResourceListener;
import noppes.npcs.client.gui.player.GuiRecipes;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.containers.ContainerCarpentryBench;
import noppes.npcs.controllers.RecipeController;

public class GuiNpcCarpentryBench
extends GuiContainerNPCInterface {
    private final ResourceLocation resource = new ResourceLocation("customnpcs", "textures/gui/carpentry.png");
    private ContainerCarpentryBench container;
    private GuiNpcButton button;

    public GuiNpcCarpentryBench(ContainerCarpentryBench container) {
        super(null, container);
        this.container = container;
        this.title = "";
        this.field_146291_p = false;
        this.closeOnEsc = true;
        this.field_147000_g = 180;
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        this.button = new GuiNpcButton(0, this.field_147003_i + 158, this.field_147009_r + 4, 12, 20, "...");
        this.addButton(this.button);
    }

    @Override
    public void buttonEvent(GuiButton guibutton) {
        this.displayGuiScreen(new GuiRecipes());
    }

    @Override
    protected void func_146976_a(float f, int i, int j) {
        this.button.field_146124_l = RecipeController.instance != null && !RecipeController.instance.anvilRecipes.isEmpty();
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.field_146297_k.field_71446_o.func_110577_a(this.resource);
        int l = (this.field_146294_l - this.field_146999_f) / 2;
        int i1 = (this.field_146295_m - this.field_147000_g) / 2;
        String title = I18n.func_74838_a((String)"tile.npccarpentybench.name");
        this.func_73729_b(l, i1, 0, 0, this.field_146999_f, this.field_147000_g);
        super.func_146976_a(f, i, j);
        this.field_146289_q.func_78276_b(title, this.field_147003_i + 4, this.field_147009_r + 4, CustomNpcResourceListener.DefaultTextColor);
        this.field_146289_q.func_78276_b(I18n.func_74838_a((String)"container.inventory"), this.field_147003_i + 4, this.field_147009_r + 87, CustomNpcResourceListener.DefaultTextColor);
    }

    @Override
    public void save() {
    }
}

