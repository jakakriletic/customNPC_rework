/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.renderer.BufferBuilder
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.Tessellator
 *  net.minecraft.client.renderer.vertex.DefaultVertexFormats
 *  net.minecraft.util.math.MathHelper
 *  org.lwjgl.input.Mouse
 */
package noppes.npcs.client.gui.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import org.lwjgl.input.Mouse;

public class GuiNpcTextArea
extends GuiNpcTextField {
    public boolean inMenu = true;
    public boolean numbersOnly = false;
    private int posX;
    private int posY;
    private int width;
    private int height;
    private int cursorCounter;
    private ClientProxy.FontContainer font;
    private int cursorPosition = 0;
    private int listHeight;
    private float scrolledY = 0.0f;
    private int startClick = -1;
    private boolean clickVerticalBar = false;
    private boolean wrapLine = true;

    public GuiNpcTextArea(int id, GuiScreen guiscreen, int i, int j, int k, int l, String s) {
        super(id, guiscreen, i, j, k, l, s);
        this.posX = i;
        this.posY = j;
        this.width = k;
        this.listHeight = this.height = l;
        this.font = ClientProxy.Font.copy();
        this.font.useCustomFont = true;
        this.func_146203_f(Integer.MAX_VALUE);
        this.func_146180_a(s.replace("\r\n", "\n"));
    }

    public void func_146178_a() {
        ++this.cursorCounter;
    }

    @Override
    public boolean func_146201_a(char c, int i) {
        if (this.func_146206_l() && this.canEdit) {
            String originalText = this.func_146179_b();
            this.func_146180_a(originalText);
            if (c == '\r' || c == '\n') {
                this.func_146180_a(originalText.substring(0, this.cursorPosition) + c + originalText.substring(this.cursorPosition));
            }
            this.func_146196_d();
            this.func_146182_d(this.cursorPosition);
            boolean bo = super.func_146201_a(c, i);
            String newText = this.func_146179_b();
            if (i != 211) {
                this.cursorPosition += newText.length() - originalText.length();
            }
            if (i == 203 && this.cursorPosition > 0) {
                --this.cursorPosition;
            }
            if (i == 205 && this.cursorPosition < newText.length()) {
                ++this.cursorPosition;
            }
            return bo;
        }
        return false;
    }

    @Override
    public boolean func_146192_a(int i, int j, int k) {
        boolean wasFocused = this.func_146206_l();
        super.func_146192_a(i, j, k);
        if (this.hoverVerticalScrollBar(i, j)) {
            this.clickVerticalBar = true;
            this.startClick = -1;
            return false;
        }
        if (k != 0 || !this.canEdit) {
            return false;
        }
        int x = i - this.posX;
        int y = (j - this.posY - 4) / this.font.height(this.func_146179_b()) + this.getStartLineY();
        this.cursorPosition = 0;
        List<String> lines = this.getLines();
        int charCount = 0;
        int lineCount = 0;
        int maxSize = this.width - (this.isScrolling() ? 14 : 4);
        for (int g = 0; g < lines.size(); ++g) {
            String wholeLine = lines.get(g);
            String line = "";
            for (char c : wholeLine.toCharArray()) {
                this.cursorPosition = charCount;
                if (this.font.width(line + c) > maxSize && this.wrapLine) {
                    line = "";
                    if (y < ++lineCount) break;
                }
                if (lineCount == y && x <= this.font.width(line + c)) {
                    return true;
                }
                ++charCount;
                line = line + c;
            }
            this.cursorPosition = charCount++;
            if (y < ++lineCount) break;
        }
        if (y >= lineCount) {
            this.cursorPosition = this.func_146179_b().length();
        }
        return true;
    }

    private List<String> getLines() {
        ArrayList<String> list = new ArrayList<String>();
        String line = "";
        for (char c : this.func_146179_b().toCharArray()) {
            if (c == '\r' || c == '\n') {
                list.add(line);
                line = "";
                continue;
            }
            line = line + c;
        }
        list.add(line);
        return list;
    }

    private int getStartLineY() {
        if (!this.isScrolling()) {
            this.scrolledY = 0.0f;
        }
        return MathHelper.func_76123_f((float)(this.scrolledY * (float)this.listHeight / (float)this.font.height(this.func_146179_b())));
    }

    @Override
    public void drawTextBox(int mouseX, int mouseY) {
        GuiNpcTextArea.func_73734_a((int)(this.posX - 1), (int)(this.posY - 1), (int)(this.posX + this.width + 1), (int)(this.posY + this.height + 1), (int)-6250336);
        GuiNpcTextArea.func_73734_a((int)this.posX, (int)this.posY, (int)(this.posX + this.width), (int)(this.posY + this.height), (int)-16777216);
        int color = 0xE0E0E0;
        boolean flag = this.func_146206_l() && this.cursorCounter / 6 % 2 == 0;
        int startLine = this.getStartLineY();
        int maxLine = this.height / this.font.height(this.func_146179_b()) + startLine;
        List<String> lines = this.getLines();
        int charCount = 0;
        int lineCount = 0;
        int maxSize = this.width - (this.isScrolling() ? 14 : 4);
        for (int i = 0; i < lines.size(); ++i) {
            String wholeLine = lines.get(i);
            String line = "";
            for (char c : wholeLine.toCharArray()) {
                if (this.font.width(line + c) > maxSize && this.wrapLine) {
                    if (lineCount >= startLine && lineCount < maxLine) {
                        this.func_73731_b(null, line, this.posX + 4, this.posY + 4 + (lineCount - startLine) * this.font.height(line), color);
                    }
                    line = "";
                    ++lineCount;
                }
                if (flag && charCount == this.cursorPosition && lineCount >= startLine && lineCount < maxLine && this.canEdit) {
                    int xx = this.posX + this.font.width(line) + 4;
                    int yy = this.posY + (lineCount - startLine) * this.font.height(line) + 4;
                    if (this.func_146179_b().length() == this.cursorPosition) {
                        this.font.drawString("_", xx, yy, color);
                    } else {
                        this.drawCursorVertical(xx, yy, xx + 1, yy + this.font.height(line));
                    }
                }
                ++charCount;
                line = line + c;
            }
            if (lineCount >= startLine && lineCount < maxLine) {
                this.func_73731_b(null, line, this.posX + 4, this.posY + 4 + (lineCount - startLine) * this.font.height(line), color);
                if (flag && charCount == this.cursorPosition && this.canEdit) {
                    int xx = this.posX + this.font.width(line) + 4;
                    int yy = this.posY + (lineCount - startLine) * this.font.height(line) + 4;
                    if (this.func_146179_b().length() == this.cursorPosition) {
                        this.font.drawString("_", xx, yy, color);
                    } else {
                        this.drawCursorVertical(xx, yy, xx + 1, yy + this.font.height(line));
                    }
                }
            }
            ++lineCount;
            ++charCount;
        }
        int k2 = Mouse.getDWheel();
        if (k2 != 0 && this.func_146206_l()) {
            this.addScrollY(k2 < 0 ? -10 : 10);
        }
        if (Mouse.isButtonDown((int)0)) {
            if (this.clickVerticalBar) {
                if (this.startClick >= 0) {
                    this.addScrollY(this.startClick - (mouseY - this.posY));
                }
                if (this.hoverVerticalScrollBar(mouseX, mouseY)) {
                    this.startClick = mouseY - this.posY;
                }
                this.startClick = mouseY - this.posY;
            }
        } else {
            this.clickVerticalBar = false;
        }
        this.listHeight = lineCount * this.font.height(this.func_146179_b());
        this.drawVerticalScrollBar();
    }

    public void func_73731_b(FontRenderer fontRendererIn, String text, int x, int y, int color) {
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.font.drawString(text, x, y, color);
    }

    private boolean isScrolling() {
        return this.listHeight > this.height - 4;
    }

    private void addScrollY(int scrolled) {
        float max;
        this.scrolledY -= 1.0f * (float)scrolled / (float)this.height;
        if (this.scrolledY < 0.0f) {
            this.scrolledY = 0.0f;
        }
        if (this.scrolledY > (max = 1.0f - 1.0f * (float)(this.height + 2) / (float)this.listHeight)) {
            this.scrolledY = max;
        }
    }

    private boolean hoverVerticalScrollBar(int x, int y) {
        if (this.listHeight <= this.height - 4) {
            return false;
        }
        return this.posY < y && this.posY + this.height > y && x < this.posX + this.width && x > this.posX + (this.width - 8);
    }

    private void drawCursorVertical(int p_146188_1_, int p_146188_2_, int p_146188_3_, int p_146188_4_) {
        int i1;
        if (p_146188_1_ < p_146188_3_) {
            i1 = p_146188_1_;
            p_146188_1_ = p_146188_3_;
            p_146188_3_ = i1;
        }
        if (p_146188_2_ < p_146188_4_) {
            i1 = p_146188_2_;
            p_146188_2_ = p_146188_4_;
            p_146188_4_ = i1;
        }
        if (p_146188_3_ > this.posX + this.width) {
            p_146188_3_ = this.posX + this.width;
        }
        if (p_146188_1_ > this.posX + this.width) {
            p_146188_1_ = this.posX + this.width;
        }
        BufferBuilder tessellator = Tessellator.func_178181_a().func_178180_c();
        GlStateManager.func_179131_c((float)0.0f, (float)0.0f, (float)255.0f, (float)255.0f);
        GlStateManager.func_179090_x();
        GlStateManager.func_179115_u();
        GlStateManager.func_179116_f((int)5387);
        tessellator.func_181668_a(7, DefaultVertexFormats.field_181705_e);
        tessellator.func_181662_b((double)p_146188_1_, (double)p_146188_4_, 0.0).func_181675_d();
        tessellator.func_181662_b((double)p_146188_3_, (double)p_146188_4_, 0.0).func_181675_d();
        tessellator.func_181662_b((double)p_146188_3_, (double)p_146188_2_, 0.0).func_181675_d();
        tessellator.func_181662_b((double)p_146188_1_, (double)p_146188_2_, 0.0).func_181675_d();
        Tessellator.func_178181_a().func_78381_a();
        GlStateManager.func_179134_v();
        GlStateManager.func_179098_w();
    }

    private int getVerticalBarSize() {
        return (int)(1.0f * (float)this.height / (float)this.listHeight * (float)(this.height - 4));
    }

    private void drawVerticalScrollBar() {
        if (this.listHeight <= this.height - 4) {
            return;
        }
        Minecraft.func_71410_x().field_71446_o.func_110577_a(GuiCustomScroll.resource);
        int x = this.posX + this.width - 6;
        int y = (int)((float)this.posY + this.scrolledY * (float)this.height) + 2;
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        int sbSize = this.getVerticalBarSize();
        this.func_73729_b(x, y, this.width, 9, 5, 1);
        for (int k = 0; k < sbSize; ++k) {
            this.func_73729_b(x, y + k, this.width, 10, 5, 1);
        }
        this.func_73729_b(x, y, this.width, 11, 5, 1);
    }
}

