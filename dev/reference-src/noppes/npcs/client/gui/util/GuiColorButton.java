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

    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) {
            return;
        }
        GuiColorButton.drawRect((int)this.x, (int)this.y, (int)(this.x + 50), (int)(this.y + 20), (int)(-16777216 + this.color));
    }
}

