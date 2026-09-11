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
        this.title = titleComponent.func_150260_c();
        this.subtitle = subtitleComponent == null ? null : subtitleComponent.func_150260_c();
        this.type = type;
    }

    public IToast.Visibility func_193653_a(GuiToast toastGui, long delta) {
        if (this.newDisplay) {
            this.firstDrawTime = delta;
            this.newDisplay = false;
        }
        toastGui.func_192989_b().func_110434_K().func_110577_a(field_193654_a);
        GlStateManager.func_179124_c((float)1.0f, (float)1.0f, (float)1.0f);
        toastGui.func_73729_b(0, 0, 0, 32 * this.type, 160, 32);
        int color1 = -256;
        int color2 = -1;
        if (this.type == 1 || this.type == 3) {
            color1 = -11534256;
            color2 = -16777216;
        }
        toastGui.func_192989_b().field_71466_p.func_78276_b(this.title, 18, 7, color1);
        toastGui.func_192989_b().field_71466_p.func_78276_b(this.subtitle, 18, 18, color2);
        return delta - this.firstDrawTime < 5000L ? IToast.Visibility.SHOW : IToast.Visibility.HIDE;
    }

    public void setDisplayedText(ITextComponent titleComponent, @Nullable ITextComponent subtitleComponent) {
        this.title = titleComponent.func_150260_c();
        this.subtitle = subtitleComponent == null ? null : subtitleComponent.func_150260_c();
        this.newDisplay = true;
    }
}

