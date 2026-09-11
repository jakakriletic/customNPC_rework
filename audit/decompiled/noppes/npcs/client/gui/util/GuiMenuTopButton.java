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
    protected int field_146121_g;
    public boolean active = false;
    public boolean hover = false;
    public boolean rotated = false;
    public IButtonListener listener;

    public GuiMenuTopButton(int i, int j, int k, String s) {
        super(i, j, k, I18n.func_74838_a((String)s));
        this.field_146120_f = Minecraft.func_71410_x().field_71466_p.func_78256_a(this.field_146126_j) + 12;
        this.field_146121_g = 20;
    }

    public GuiMenuTopButton(int i, GuiButton parent, String s) {
        this(i, parent.field_146128_h + parent.field_146120_f, parent.field_146129_i, s);
    }

    public GuiMenuTopButton(int i, GuiButton parent, String s, IButtonListener listener) {
        this(i, parent, s);
        this.listener = listener;
    }

    public int func_146114_a(boolean flag) {
        int byte0 = 1;
        if (this.active) {
            byte0 = 0;
        } else if (flag) {
            byte0 = 2;
        }
        return byte0;
    }

    public void func_191745_a(Minecraft minecraft, int i, int j, float partialTicks) {
        if (!this.getVisible()) {
            return;
        }
        GlStateManager.func_179094_E();
        minecraft.field_71446_o.func_110577_a(resource);
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        int height = this.field_146121_g - (this.active ? 0 : 2);
        this.hover = i >= this.field_146128_h && j >= this.field_146129_i && i < this.field_146128_h + this.getWidth() && j < this.field_146129_i + height;
        int k = this.func_146114_a(this.hover);
        this.func_73729_b(this.field_146128_h, this.field_146129_i, 0, k * 20, this.getWidth() / 2, height);
        this.func_73729_b(this.field_146128_h + this.getWidth() / 2, this.field_146129_i, 200 - this.getWidth() / 2, k * 20, this.getWidth() / 2, height);
        this.func_146119_b(minecraft, i, j);
        FontRenderer fontrenderer = minecraft.field_71466_p;
        if (this.rotated) {
            GlStateManager.func_179114_b((float)90.0f, (float)1.0f, (float)0.0f, (float)0.0f);
        }
        if (this.active) {
            this.func_73732_a(fontrenderer, this.field_146126_j, this.field_146128_h + this.getWidth() / 2, this.field_146129_i + (height - 8) / 2, 0xFFFFA0);
        } else if (this.hover) {
            this.func_73732_a(fontrenderer, this.field_146126_j, this.field_146128_h + this.getWidth() / 2, this.field_146129_i + (height - 8) / 2, 0xFFFFA0);
        } else {
            this.func_73732_a(fontrenderer, this.field_146126_j, this.field_146128_h + this.getWidth() / 2, this.field_146129_i + (height - 8) / 2, 0xE0E0E0);
        }
        GlStateManager.func_179121_F();
    }

    protected void func_146119_b(Minecraft minecraft, int i, int j) {
    }

    public void func_146118_a(int i, int j) {
    }

    @Override
    public boolean func_146116_c(Minecraft minecraft, int i, int j) {
        boolean bo;
        boolean bl = bo = !this.active && this.getVisible() && this.hover;
        if (bo && this.listener != null) {
            this.listener.actionPerformed(this);
            return false;
        }
        return bo;
    }
}

