/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.text.translation.I18n
 */
package noppes.npcs.client.gui.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.IButtonListener;

public class GuiMenuTopButton
extends GuiNpcButton {
    public static final ResourceLocation resource = new ResourceLocation("customnpcs", "textures/gui/menutopbutton.png");
    protected int height;
    public boolean active = false;
    public boolean hover = false;
    public boolean rotated = false;
    public IButtonListener listener;

    public GuiMenuTopButton(int i, int j, int k, String s) {
        super(i, j, k, I18n.translateToLocal((String)s));
        this.width = Minecraft.getMinecraft().fontRenderer.getStringWidth(this.displayString) + 12;
        this.height = 20;
    }

    public GuiMenuTopButton(int i, GuiButton parent, String s) {
        this(i, parent.x + parent.width, parent.y, s);
    }

    public GuiMenuTopButton(int i, GuiButton parent, String s, IButtonListener listener) {
        this(i, parent, s);
        this.listener = listener;
    }

    public int getHoverState(boolean flag) {
        int byte0 = 1;
        if (this.active) {
            byte0 = 0;
        } else if (flag) {
            byte0 = 2;
        }
        return byte0;
    }

    public void drawButton(Minecraft minecraft, int i, int j, float partialTicks) {
        if (!this.getVisible()) {
            return;
        }
        GlStateManager.pushMatrix();
        minecraft.renderEngine.bindTexture(resource);
        GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        int height = this.height - (this.active ? 0 : 2);
        this.hover = i >= this.x && j >= this.y && i < this.x + this.getWidth() && j < this.y + height;
        int k = this.getHoverState(this.hover);
        this.drawTexturedModalRect(this.x, this.y, 0, k * 20, this.getWidth() / 2, height);
        this.drawTexturedModalRect(this.x + this.getWidth() / 2, this.y, 200 - this.getWidth() / 2, k * 20, this.getWidth() / 2, height);
        this.mouseDragged(minecraft, i, j);
        FontRenderer fontrenderer = minecraft.fontRenderer;
        if (this.rotated) {
            GlStateManager.rotate((float)90.0f, (float)1.0f, (float)0.0f, (float)0.0f);
        }
        if (this.active) {
            this.drawCenteredString(fontrenderer, this.displayString, this.x + this.getWidth() / 2, this.y + (height - 8) / 2, 0xFFFFA0);
        } else if (this.hover) {
            this.drawCenteredString(fontrenderer, this.displayString, this.x + this.getWidth() / 2, this.y + (height - 8) / 2, 0xFFFFA0);
        } else {
            this.drawCenteredString(fontrenderer, this.displayString, this.x + this.getWidth() / 2, this.y + (height - 8) / 2, 0xE0E0E0);
        }
        GlStateManager.popMatrix();
    }

    protected void mouseDragged(Minecraft minecraft, int i, int j) {
    }

    public void mouseReleased(int i, int j) {
    }

    @Override
    public boolean mousePressed(Minecraft minecraft, int i, int j) {
        boolean bo;
        boolean bl = bo = !this.active && this.getVisible() && this.hover;
        if (bo && this.listener != null) {
            this.listener.actionPerformed(this);
            return false;
        }
        return bo;
    }
}

