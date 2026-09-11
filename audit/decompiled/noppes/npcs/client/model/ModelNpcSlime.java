/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.model.ModelRenderer
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.entity.Entity
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 */
package noppes.npcs.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(value=Side.CLIENT)
public class ModelNpcSlime
extends ModelBase {
    ModelRenderer outerBody;
    ModelRenderer innerBody;
    ModelRenderer slimeRightEye;
    ModelRenderer slimeLeftEye;
    ModelRenderer slimeMouth;

    public ModelNpcSlime(int par1) {
        this.field_78089_u = 64;
        this.field_78090_t = 64;
        this.outerBody = new ModelRenderer((ModelBase)this, 0, 0);
        this.outerBody = new ModelRenderer((ModelBase)this, 0, 0);
        this.outerBody.func_78789_a(-8.0f, 32.0f, -8.0f, 16, 16, 16);
        if (par1 > 0) {
            this.innerBody = new ModelRenderer((ModelBase)this, 0, 32);
            this.innerBody.func_78789_a(-3.0f, 17.0f, -3.0f, 6, 6, 6);
            this.slimeRightEye = new ModelRenderer((ModelBase)this, 0, 0);
            this.slimeRightEye.func_78789_a(-3.25f, 18.0f, -3.5f, 2, 2, 2);
            this.slimeLeftEye = new ModelRenderer((ModelBase)this, 0, 4);
            this.slimeLeftEye.func_78789_a(1.25f, 18.0f, -3.5f, 2, 2, 2);
            this.slimeMouth = new ModelRenderer((ModelBase)this, 0, 8);
            this.slimeMouth.func_78789_a(0.0f, 21.0f, -3.5f, 1, 1, 1);
        }
    }

    public void func_78088_a(Entity par1Entity, float par2, float par3, float par4, float par5, float par6, float par7) {
        this.func_78087_a(par2, par3, par4, par5, par6, par7, par1Entity);
        if (this.innerBody != null) {
            this.innerBody.func_78785_a(par7);
        } else {
            GlStateManager.func_179094_E();
            GlStateManager.func_179152_a((float)0.5f, (float)0.5f, (float)0.5f);
            this.outerBody.func_78785_a(par7);
            GlStateManager.func_179121_F();
        }
        if (this.slimeRightEye != null) {
            this.slimeRightEye.func_78785_a(par7);
            this.slimeLeftEye.func_78785_a(par7);
            this.slimeMouth.func_78785_a(par7);
        }
    }
}

