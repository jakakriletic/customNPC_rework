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
        this.allowUserInput = false;
        this.closeOnEsc = true;
        this.ySize = 180;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.button = new GuiNpcButton(0, this.guiLeft + 158, this.guiTop + 4, 12, 20, "...");
        this.addButton(this.button);
    }

    @Override
    public void buttonEvent(GuiButton guibutton) {
        this.displayGuiScreen(new GuiRecipes());
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
        this.button.enabled = RecipeController.instance != null && !RecipeController.instance.anvilRecipes.isEmpty();
        GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.mc.renderEngine.bindTexture(this.resource);
        int l = (this.width - this.xSize) / 2;
        int i1 = (this.height - this.ySize) / 2;
        String title = I18n.translateToLocal((String)"tile.npccarpentybench.name");
        this.drawTexturedModalRect(l, i1, 0, 0, this.xSize, this.ySize);
        super.drawGuiContainerBackgroundLayer(f, i, j);
        this.fontRenderer.drawString(title, this.guiLeft + 4, this.guiTop + 4, CustomNpcResourceListener.DefaultTextColor);
        this.fontRenderer.drawString(I18n.translateToLocal((String)"container.inventory"), this.guiLeft + 4, this.guiTop + 87, CustomNpcResourceListener.DefaultTextColor);
    }

    @Override
    public void save() {
    }
}

