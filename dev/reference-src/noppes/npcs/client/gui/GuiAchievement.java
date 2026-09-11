/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.client.gui.toasts.GuiToast
 *  net.minecraft.client.gui.toasts.IToast
 *  net.minecraft.client.gui.toasts.IToast$Visibility
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 */
package noppes.npcs.client.gui;

import javax.annotation.Nullable;
import net.minecraft.client.gui.toasts.GuiToast;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(value=Side.CLIENT)
public class GuiAchievement
implements IToast {
    private String title;
    private String subtitle;
    private int type;
    private long firstDrawTime;
    private boolean newDisplay;

    public GuiAchievement(ITextComponent titleComponent, ITextComponent subtitleComponent, int type) {
        this.title = titleComponent.getUnformattedText();
        this.subtitle = subtitleComponent == null ? null : subtitleComponent.getUnformattedText();
        this.type = type;
    }

    public IToast.Visibility draw(GuiToast toastGui, long delta) {
        if (this.newDisplay) {
            this.firstDrawTime = delta;
            this.newDisplay = false;
        }
        toastGui.getMinecraft().getTextureManager().bindTexture(TEXTURE_TOASTS);
        GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f);
        toastGui.drawTexturedModalRect(0, 0, 0, 32 * this.type, 160, 32);
        int color1 = -256;
        int color2 = -1;
        if (this.type == 1 || this.type == 3) {
            color1 = -11534256;
            color2 = -16777216;
        }
        toastGui.getMinecraft().fontRenderer.drawString(this.title, 18, 7, color1);
        toastGui.getMinecraft().fontRenderer.drawString(this.subtitle, 18, 18, color2);
        return delta - this.firstDrawTime < 5000L ? IToast.Visibility.SHOW : IToast.Visibility.HIDE;
    }

    public void setDisplayedText(ITextComponent titleComponent, @Nullable ITextComponent subtitleComponent) {
        this.title = titleComponent.getUnformattedText();
        this.subtitle = subtitleComponent == null ? null : subtitleComponent.getUnformattedText();
        this.newDisplay = true;
    }
}

