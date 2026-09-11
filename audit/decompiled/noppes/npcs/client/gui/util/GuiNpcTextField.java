/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.gui.GuiTextField
 */
package noppes.npcs.client.gui.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import noppes.npcs.client.gui.util.ITextfieldListener;

public class GuiNpcTextField
extends GuiTextField {
    public boolean enabled = true;
    public boolean inMenu = true;
    public boolean numbersOnly = false;
    private ITextfieldListener listener;
    public int min = 0;
    public int max = Integer.MAX_VALUE;
    public int def = 0;
    private static GuiNpcTextField activeTextfield = null;
    public boolean canEdit = true;
    private final int[] allowedSpecialChars = new int[]{14, 211, 203, 205};

    public GuiNpcTextField(int id, GuiScreen parent, FontRenderer fontRenderer, int i, int j, int k, int l, String s) {
        super(id, fontRenderer, i, j, k, l);
        this.func_146203_f(500);
        this.func_146180_a(s == null ? "" : s);
        if (parent instanceof ITextfieldListener) {
            this.listener = (ITextfieldListener)parent;
        }
    }

    public static boolean isActive() {
        return activeTextfield != null;
    }

    public GuiNpcTextField(int id, GuiScreen parent, int i, int j, int k, int l, String s) {
        this(id, parent, Minecraft.func_71410_x().field_71466_p, i, j, k, l, s);
    }

    private boolean charAllowed(char c, int i) {
        if (!this.numbersOnly || Character.isDigit(c) || c == '-' && this.func_146179_b().length() == 0) {
            return true;
        }
        for (int j : this.allowedSpecialChars) {
            if (j != i) continue;
            return true;
        }
        return false;
    }

    public boolean func_146201_a(char c, int i) {
        if (!this.charAllowed(c, i) || !this.canEdit) {
            return false;
        }
        return super.func_146201_a(c, i);
    }

    public boolean isEmpty() {
        return this.func_146179_b().trim().length() == 0;
    }

    public int getInteger() {
        return Integer.parseInt(this.func_146179_b());
    }

    public boolean isInteger() {
        try {
            Integer.parseInt(this.func_146179_b());
            return true;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean func_146192_a(int i, int j, int k) {
        if (!this.canEdit) {
            return false;
        }
        boolean wasFocused = this.func_146206_l();
        boolean clicked = super.func_146192_a(i, j, k);
        if (wasFocused != this.func_146206_l() && wasFocused) {
            this.unFocused();
        }
        if (this.func_146206_l()) {
            activeTextfield = this;
        }
        return clicked;
    }

    public void unFocused() {
        if (this.numbersOnly) {
            if (this.isEmpty() || !this.isInteger()) {
                this.func_146180_a(this.def + "");
            } else if (this.getInteger() < this.min) {
                this.func_146180_a(this.min + "");
            } else if (this.getInteger() > this.max) {
                this.func_146180_a(this.max + "");
            }
        }
        if (this.listener != null) {
            this.listener.unFocused(this);
        }
        if (this == activeTextfield) {
            activeTextfield = null;
        }
    }

    public void func_146194_f() {
        if (this.enabled) {
            super.func_146194_f();
        }
    }

    public void setMinMaxDefault(int i, int j, int k) {
        this.min = i;
        this.max = j;
        this.def = k;
    }

    public static void unfocus() {
        GuiNpcTextField prev = activeTextfield;
        activeTextfield = null;
        if (prev != null) {
            prev.unFocused();
        }
    }

    public void drawTextBox(int mousX, int mousY) {
        this.func_146194_f();
    }

    public GuiNpcTextField setNumbersOnly() {
        this.numbersOnly = true;
        return this;
    }
}

