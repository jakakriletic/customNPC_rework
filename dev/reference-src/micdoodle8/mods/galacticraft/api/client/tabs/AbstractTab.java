/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.RenderHelper
 *  net.minecraft.client.renderer.RenderItem
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.ResourceLocation
 *  net.minecraftforge.fml.client.FMLClientHandler
 */
package micdoodle8.mods.galacticraft.api.client.tabs;

import micdoodle8.mods.galacticraft.api.client.tabs.TabRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;

public abstract class AbstractTab
extends GuiButton {
    ResourceLocation texture = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
    ItemStack renderStack;
    public int potionOffsetLast;
    protected RenderItem itemRender;

    public AbstractTab(int id, int posX, int posY, ItemStack renderStack) {
        super(id, posX, posY, 28, 32, "");
        this.renderStack = renderStack;
        this.itemRender = FMLClientHandler.instance().getClient().getRenderItem();
    }

    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        int newPotionOffset = TabRegistry.getPotionOffsetNEI();
        if (newPotionOffset != this.potionOffsetLast) {
            this.x += newPotionOffset - this.potionOffsetLast;
            this.potionOffsetLast = newPotionOffset;
        }
        if (this.visible) {
            GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            int yTexPos = this.enabled ? 3 : 32;
            int ySize = this.enabled ? 25 : 32;
            int xOffset = this.id == 2 ? 0 : 1;
            int yPos = this.y + (this.enabled ? 3 : 0);
            mc.renderEngine.bindTexture(this.texture);
            this.drawTexturedModalRect(this.x, yPos, xOffset * 28, yTexPos, 28, ySize);
            RenderHelper.enableGUIStandardItemLighting();
            this.zLevel = 100.0f;
            this.itemRender.zLevel = 100.0f;
            GlStateManager.enableLighting();
            GlStateManager.enableRescaleNormal();
            this.itemRender.renderItemAndEffectIntoGUI(this.renderStack, this.x + 6, this.y + 8);
            this.itemRender.renderItemOverlayIntoGUI(mc.fontRenderer, this.renderStack, this.x + 6, this.y + 8, null);
            GlStateManager.disableLighting();
            this.itemRender.zLevel = 0.0f;
            this.zLevel = 0.0f;
            RenderHelper.disableStandardItemLighting();
        }
    }

    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        boolean inWindow;
        boolean bl = inWindow = this.enabled && this.visible && mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        if (inWindow) {
            this.onTabClicked();
        }
        return inWindow;
    }

    public abstract void onTabClicked();

    public abstract boolean shouldAddToList();
}

