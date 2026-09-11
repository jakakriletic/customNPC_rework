/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.util.text.translation.I18n
 */
package noppes.npcs.client.gui.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.client.CustomNpcResourceListener;

public class GuiNpcLabel {
    public String label;
    public int x;
    public int y;
    public int color;
    public boolean enabled = true;
    public int id;

    public GuiNpcLabel(int id, Object label, int x, int y, int color) {
        this.id = id;
        this.label = I18n.func_74838_a((String)label.toString());
        this.x = x;
        this.y = y;
        this.color = color;
    }

    public GuiNpcLabel(int id, Object label, int x, int y) {
        this(id, label, x, y, CustomNpcResourceListener.DefaultTextColor);
    }

    public void drawLabel(GuiScreen gui, FontRenderer fontRenderer) {
        if (this.enabled) {
            fontRenderer.func_78276_b(this.label, this.x, this.y, this.color);
        }
    }

    public void center(int width) {
        int size = Minecraft.func_71410_x().field_71466_p.func_78256_a(this.label);
        this.x += (width - size) / 2;
    }
}

