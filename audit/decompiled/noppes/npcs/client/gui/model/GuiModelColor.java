/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.resources.IResource
 *  net.minecraft.util.ResourceLocation
 */
package noppes.npcs.client.gui.model;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.ITextfieldListener;
import noppes.npcs.client.gui.util.SubGuiInterface;

public class GuiModelColor
extends SubGuiInterface
implements ITextfieldListener {
    private GuiScreen parent;
    private static final ResourceLocation colorPicker = new ResourceLocation("moreplayermodels:textures/gui/color.png");
    private static final ResourceLocation colorgui = new ResourceLocation("moreplayermodels:textures/gui/color_gui.png");
    private int colorX;
    private int colorY;
    private GuiNpcTextField textfield;
    public int color;
    private ColorCallback callback;

    public GuiModelColor(GuiScreen parent, int color, ColorCallback callback) {
        this.parent = parent;
        this.callback = callback;
        this.ySize = 230;
        this.closeOnEsc = false;
        this.background = colorgui;
        this.color = color;
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        this.colorX = this.guiLeft + 4;
        this.colorY = this.guiTop + 50;
        this.textfield = new GuiNpcTextField(0, this, this.guiLeft + 35, this.guiTop + 25, 60, 20, this.getColor());
        this.addTextField(this.textfield);
        this.addButton(new GuiNpcButton(66, this.guiLeft + 107, this.guiTop + 8, 20, 20, "X"));
        this.textfield.func_146193_g(this.color);
    }

    @Override
    protected void func_146284_a(GuiButton guibutton) {
        if (guibutton.field_146127_k == 66) {
            this.close();
        }
    }

    @Override
    public void func_73869_a(char c, int i) {
        String prev = this.textfield.func_146179_b();
        super.func_73869_a(c, i);
        String newText = this.textfield.func_146179_b();
        if (newText.equals(prev)) {
            return;
        }
        try {
            this.color = Integer.parseInt(this.textfield.func_146179_b(), 16);
            this.callback.color(this.color);
            this.textfield.func_146193_g(this.color);
        }
        catch (NumberFormatException e) {
            this.textfield.func_146180_a(prev);
        }
    }

    @Override
    public void func_73863_a(int par1, int par2, float par3) {
        super.func_73863_a(par1, par2, par3);
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.field_146297_k.func_110434_K().func_110577_a(colorPicker);
        this.func_73729_b(this.colorX, this.colorY, 0, 0, 120, 120);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void func_73864_a(int i, int j, int k) {
        super.func_73864_a(i, j, k);
        if (i < this.colorX || i > this.colorX + 120 || j < this.colorY || j > this.colorY + 120) {
            return;
        }
        InputStream stream = null;
        try {
            IResource resource = this.field_146297_k.func_110442_L().func_110536_a(colorPicker);
            stream = resource.func_110527_b();
            BufferedImage bufferedimage = ImageIO.read(stream);
            int color = bufferedimage.getRGB((i - this.guiLeft - 4) * 4, (j - this.guiTop - 50) * 4) & 0xFFFFFF;
            if (color != 0) {
                this.color = color;
                this.callback.color(color);
                this.textfield.func_146193_g(color);
                this.textfield.func_146180_a(this.getColor());
            }
        }
        catch (IOException iOException) {
        }
        finally {
            if (stream != null) {
                try {
                    stream.close();
                }
                catch (IOException iOException) {}
            }
        }
    }

    @Override
    public void unFocused(GuiNpcTextField textfield) {
        try {
            this.color = Integer.parseInt(textfield.func_146179_b(), 16);
        }
        catch (NumberFormatException e) {
            this.color = 0;
        }
        this.callback.color(this.color);
        textfield.func_146193_g(this.color);
    }

    public String getColor() {
        String str = Integer.toHexString(this.color);
        while (str.length() < 6) {
            str = "0" + str;
        }
        return str;
    }

    @Override
    public void save() {
    }

    public static interface ColorCallback {
        public void color(int var1);
    }
}

