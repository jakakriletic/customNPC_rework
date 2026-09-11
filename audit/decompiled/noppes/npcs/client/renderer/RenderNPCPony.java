/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.resources.IResource
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.ResourceLocation
 */
package noppes.npcs.client.renderer;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.client.model.ModelPony;
import noppes.npcs.client.model.ModelPonyArmor;
import noppes.npcs.client.renderer.RenderNPCInterface;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.EntityNpcPony;

public class RenderNPCPony<T extends EntityNpcPony>
extends RenderNPCInterface<T> {
    private ModelPony modelBipedMain;
    private ModelPonyArmor modelArmorChestplate;
    private ModelPonyArmor modelArmor;

    public RenderNPCPony() {
        super(new ModelPony(0.0f), 0.5f);
        this.modelBipedMain = (ModelPony)this.field_77045_g;
        this.modelArmorChestplate = new ModelPonyArmor(1.0f);
        this.modelArmor = new ModelPonyArmor(0.5f);
    }

    @Override
    public ResourceLocation getEntityTexture(T pony) {
        boolean check = ((EntityNpcPony)((Object)pony)).textureLocation == null || ((EntityNpcPony)((Object)pony)).textureLocation != ((EntityNpcPony)((Object)pony)).checked;
        ResourceLocation loc = super.getEntityTexture(pony);
        if (check) {
            try {
                IResource resource = Minecraft.func_71410_x().func_110442_L().func_110536_a(loc);
                BufferedImage bufferedimage = ImageIO.read(resource.func_110527_b());
                ((EntityNpcPony)((Object)pony)).isPegasus = false;
                ((EntityNpcPony)((Object)pony)).isUnicorn = false;
                Color color = new Color(bufferedimage.getRGB(0, 0), true);
                Color color1 = new Color(249, 177, 49, 255);
                Color color2 = new Color(136, 202, 240, 255);
                Color color3 = new Color(209, 159, 228, 255);
                Color color4 = new Color(254, 249, 252, 255);
                if (color.equals(color1)) {
                    // empty if block
                }
                if (color.equals(color2)) {
                    ((EntityNpcPony)((Object)pony)).isPegasus = true;
                }
                if (color.equals(color3)) {
                    ((EntityNpcPony)((Object)pony)).isUnicorn = true;
                }
                if (color.equals(color4)) {
                    ((EntityNpcPony)((Object)pony)).isPegasus = true;
                    ((EntityNpcPony)((Object)pony)).isUnicorn = true;
                }
                ((EntityNpcPony)((Object)pony)).checked = loc;
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
        return loc;
    }

    @Override
    public void doRender(T pony, double d, double d1, double d2, float f, float f1) {
        ItemStack itemstack = ((EntityNPCInterface)((Object)pony)).func_184614_ca();
        this.modelBipedMain.heldItemRight = itemstack == null ? 0 : 1;
        this.modelArmor.heldItemRight = this.modelBipedMain.heldItemRight;
        this.modelArmorChestplate.heldItemRight = this.modelBipedMain.heldItemRight;
        this.modelArmor.isSneak = this.modelBipedMain.isSneak = ((EntityNPCInterface)((Object)pony)).func_70093_af();
        this.modelArmorChestplate.isSneak = this.modelBipedMain.isSneak;
        this.modelBipedMain.field_78093_q = false;
        this.modelArmor.field_78093_q = false;
        this.modelArmorChestplate.field_78093_q = false;
        this.modelArmor.isSleeping = this.modelBipedMain.isSleeping = ((EntityNPCInterface)((Object)pony)).func_70608_bn();
        this.modelArmorChestplate.isSleeping = this.modelBipedMain.isSleeping;
        this.modelArmor.isUnicorn = this.modelBipedMain.isUnicorn = ((EntityNpcPony)((Object)pony)).isUnicorn;
        this.modelArmorChestplate.isUnicorn = this.modelBipedMain.isUnicorn;
        this.modelArmor.isPegasus = this.modelBipedMain.isPegasus = ((EntityNpcPony)((Object)pony)).isPegasus;
        this.modelArmorChestplate.isPegasus = this.modelBipedMain.isPegasus;
        if (((EntityNPCInterface)((Object)pony)).func_70093_af()) {
            d1 -= 0.125;
        }
        super.doRender(pony, d, d1, d2, f, f1);
        this.modelBipedMain.aimedBow = false;
        this.modelArmor.aimedBow = false;
        this.modelArmorChestplate.aimedBow = false;
        this.modelBipedMain.field_78093_q = false;
        this.modelArmor.field_78093_q = false;
        this.modelArmorChestplate.field_78093_q = false;
        this.modelBipedMain.isSneak = false;
        this.modelArmor.isSneak = false;
        this.modelArmorChestplate.isSneak = false;
        this.modelBipedMain.heldItemRight = 0;
        this.modelArmor.heldItemRight = 0;
        this.modelArmorChestplate.heldItemRight = 0;
    }
}

