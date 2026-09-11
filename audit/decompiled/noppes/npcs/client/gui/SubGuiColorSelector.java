/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.resources.IResource
 *  net.minecraft.util.ResourceLocation
 */
package noppes.npcs.client.gui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.ITextfieldListener;
import noppes.npcs.client.gui.util.SubGuiInterface;

public class SubGuiColorSelector
extends SubGuiInterface
implements ITextfieldListener {
    private static final ResourceLocation resource = new ResourceLocation("customnpcs:textures/gui/color.png");
    private int colorX;
    private int colorY;
    private GuiNpcTextField textfield;
    public int color;

    public SubGuiColorSelector(int color) {
        this.xSize = 176;
        this.ySize = 222;
        this.color = color;
        this.setBackground("smallbg.png");
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        this.colorX = this.guiLeft + 30;
        this.colorY = this.guiTop + 50;
        this.textfield = new GuiNpcTextField(0, this, this.guiLeft + 53, this.guiTop + 20, 70, 20, this.getColor());
        this.addTextField(this.textfield);
        this.textfield.func_146193_g(this.color);
        this.addButton(new GuiNpcButton(66, this.guiLeft + 112, this.guiTop + 198, 60, 20, "gui.done"));
    }

    public String getColor() {
        String str = Integer.toHexString(this.color);
        while (str.length() < 6) {
            str = "0" + str;
        }
        return str;
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
            this.textfield.func_146193_g(this.color);
        }
        catch (NumberFormatException e) {
            this.textfield.func_146180_a(prev);
        }
    }

    @Override
    protected void func_146284_a(GuiButton btn) {
        super.func_146284_a(btn);
        if (btn.field_146127_k == 66) {
            this.close();
        }
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    public void func_73863_a(int par1, int par2, float par3) {
        super.func_73863_a(par1, par2, par3);
        this.field_146297_k.func_110434_K().func_110577_a(resource);
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.func_73729_b(this.colorX, this.colorY, 0, 0, 120, 120);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void func_73864_a(int i, int j, int k) {
        super.func_73864_a(i, j, k);
        if (i < this.colorX || i > this.colorX + 117 || j < this.colorY || j > this.colorY + 117) {
            return;
        }
        InputStream stream = null;
        try {
            IResource iresource = this.field_146297_k.func_110442_L().func_110536_a(resource);
            stream = iresource.func_110527_b();
            BufferedImage bufferedimage = ImageIO.read(stream);
            this.color = bufferedimage.getRGB((i - this.guiLeft - 30) * 4, (j - this.guiTop - 50) * 4) & 0xFFFFFF;
            this.textfield.func_146193_g(this.color);
            this.textfield.func_146180_a(this.getColor());
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
        int color = 0;
        try {
            color = Integer.parseInt(textfield.func_146179_b(), 16);
        }
        catch (NumberFormatException e) {
            color = 0;
        }
        this.color = color;
        textfield.func_146193_g(color);
    }
}

