/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 */
package noppes.npcs.client.gui.util;

import net.minecraft.client.Minecraft;
import noppes.npcs.client.gui.util.GuiNpcButton;

public class GuiColorButton
extends GuiNpcButton {
    public int color;

    public GuiColorButton(int id, int x, int y, int color) {
        super(id, x, y, 50, 20, "");
        this.color = color;
    }

    public void func_191745_a(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.field_146125_m) {
            return;
        }
        GuiColorButton.func_73734_a((int)this.field_146128_h, (int)this.field_146129_i, (int)(this.field_146128_h + 50), (int)(this.field_146129_i + 20), (int)(-16777216 + this.color));
    }
}

