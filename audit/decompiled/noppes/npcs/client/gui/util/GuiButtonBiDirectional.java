/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.util.ResourceLocation
 */
package noppes.npcs.client.gui.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.client.gui.util.GuiNpcButton;

public class GuiButtonBiDirectional
extends GuiNpcButton {
    public static final ResourceLocation resource = new ResourceLocation("customnpcs:textures/gui/arrowbuttons.png");
    private int color = 0xFFFFFF;

    public GuiButtonBiDirectional(int id, int x, int y, int width, int height, String[] arr, int current) {
        super(id, x, y, width, height, arr, current);
    }

    public void func_191745_a(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.field_146125_m) {
            return;
        }
        boolean hover = mouseX >= this.field_146128_h && mouseY >= this.field_146129_i && mouseX < this.field_146128_h + this.field_146120_f && mouseY < this.field_146129_i + this.field_146121_g;
        boolean hoverL = mouseX >= this.field_146128_h && mouseY >= this.field_146129_i && mouseX < this.field_146128_h + 14 && mouseY < this.field_146129_i + this.field_146121_g;
        boolean hoverR = !hoverL && mouseX >= this.field_146128_h + this.field_146120_f - 14 && mouseY >= this.field_146129_i && mouseX < this.field_146128_h + this.field_146120_f && mouseY < this.field_146129_i + this.field_146121_g;
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        mc.func_110434_K().func_110577_a(resource);
        this.func_73729_b(this.field_146128_h, this.field_146129_i, 0, hoverL ? 40 : 20, 11, 20);
        this.func_73729_b(this.field_146128_h + this.field_146120_f - 11, this.field_146129_i, 11, hover && !hoverL || hoverR ? 40 : 20, 11, 20);
        int l = this.color;
        if (this.packedFGColour != 0) {
            l = this.packedFGColour;
        } else if (!this.field_146124_l) {
            l = 0xA0A0A0;
        } else if (hover) {
            l = 0xFFFFA0;
        }
        String text = "";
        float maxWidth = this.field_146120_f - 36;
        if ((float)mc.field_71466_p.func_78256_a(this.field_146126_j) > maxWidth) {
            for (int h = 0; h < this.field_146126_j.length(); ++h) {
                char c = this.field_146126_j.charAt(h);
                if ((float)mc.field_71466_p.func_78256_a(text = text + c) > maxWidth) break;
            }
            text = text + "...";
        } else {
            text = this.field_146126_j;
        }
        if (hover) {
            text = "\u00a7n" + text;
        }
        this.func_73732_a(mc.field_71466_p, text, this.field_146128_h + this.field_146120_f / 2, this.field_146129_i + (this.field_146121_g - 8) / 2, l);
    }

    @Override
    public boolean func_146116_c(Minecraft minecraft, int mouseX, int mouseY) {
        int value = this.getValue();
        boolean bo = super.func_146116_c(minecraft, mouseX, mouseY);
        if (bo && this.display != null && this.display.length != 0) {
            boolean hoverR;
            boolean hoverL = mouseX >= this.field_146128_h && mouseY >= this.field_146129_i && mouseX < this.field_146128_h + 14 && mouseY < this.field_146129_i + this.field_146121_g;
            boolean bl = hoverR = !hoverL && mouseX >= this.field_146128_h + 14 && mouseY >= this.field_146129_i && mouseX < this.field_146128_h + this.field_146120_f && mouseY < this.field_146129_i + this.field_146121_g;
            if (hoverR) {
                value = (value + 1) % this.display.length;
            }
            if (hoverL) {
                if (value <= 0) {
                    value = this.display.length;
                }
                --value;
            }
            this.setDisplay(value);
        }
        return bo;
    }
}

