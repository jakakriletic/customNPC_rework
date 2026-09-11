/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 */
package noppes.npcs.client.gui;

import net.minecraft.client.gui.GuiButton;
import noppes.npcs.NoppesStringUtils;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiTextArea;
import noppes.npcs.client.gui.util.ITextChangeListener;
import noppes.npcs.client.gui.util.SubGuiInterface;

public class SubGuiNpcTextArea
extends SubGuiInterface
implements ITextChangeListener {
    public String text;
    public String originalText;
    private GuiTextArea textarea;
    private boolean highlighting = false;

    public SubGuiNpcTextArea(String text) {
        this.text = text;
        this.originalText = text;
        this.setBackground("bgfilled.png");
        this.xSize = 256;
        this.ySize = 256;
        this.closeOnEsc = true;
    }

    public SubGuiNpcTextArea(String originalText, String text) {
        this(text);
        this.originalText = originalText;
    }

    @Override
    public void func_73866_w_() {
        this.xSize = (int)((double)this.field_146294_l * 0.88);
        this.ySize = (int)((double)this.xSize * 0.56);
        if ((double)this.ySize > (double)this.field_146295_m * 0.95) {
            this.ySize = (int)((double)this.field_146295_m * 0.95);
            this.xSize = (int)((double)this.ySize / 0.56);
        }
        this.bgScale = (float)this.xSize / 440.0f;
        super.func_73866_w_();
        if (this.textarea != null) {
            this.text = this.textarea.getText();
        }
        int yoffset = (int)((double)this.ySize * 0.02);
        this.textarea = new GuiTextArea(2, this.guiLeft + 1 + yoffset, this.guiTop + yoffset, this.xSize - 100 - yoffset, this.ySize - yoffset * 2, this.text);
        this.textarea.setListener(this);
        if (this.highlighting) {
            this.textarea.enableCodeHighlighting();
        }
        this.add(this.textarea);
        this.field_146292_n.add(new GuiNpcButton(102, this.guiLeft + this.xSize - 90 - yoffset, this.guiTop + 20, 56, 20, "gui.clear"));
        this.field_146292_n.add(new GuiNpcButton(101, this.guiLeft + this.xSize - 90 - yoffset, this.guiTop + 43, 56, 20, "gui.paste"));
        this.field_146292_n.add(new GuiNpcButton(100, this.guiLeft + this.xSize - 90 - yoffset, this.guiTop + 66, 56, 20, "gui.copy"));
        this.field_146292_n.add(new GuiNpcButton(103, this.guiLeft + this.xSize - 90 - yoffset, this.guiTop + 89, 56, 20, "remote.reset"));
        this.field_146292_n.add(new GuiNpcButton(0, this.guiLeft + this.xSize - 90 - yoffset, this.guiTop + 160, 56, 20, "gui.close"));
        this.xSize = 420;
        this.ySize = 256;
    }

    public SubGuiNpcTextArea enableHighlighting() {
        this.highlighting = true;
        return this;
    }

    @Override
    public void buttonEvent(GuiButton guibutton) {
        int id = guibutton.field_146127_k;
        if (id == 100) {
            NoppesStringUtils.setClipboardContents(this.textarea.getText());
        }
        if (id == 101) {
            this.textarea.setText(NoppesStringUtils.getClipboardContents());
        }
        if (id == 102) {
            this.textarea.setText("");
        }
        if (id == 103) {
            this.textarea.setText(this.originalText);
        }
        if (id == 0) {
            this.close();
        }
    }

    @Override
    public void textUpdate(String text) {
        this.text = text;
    }
}

