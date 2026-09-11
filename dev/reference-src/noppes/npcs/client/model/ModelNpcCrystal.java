/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.model.ModelRenderer
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.util.math.MathHelper
 */
package noppes.npcs.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;

public class ModelNpcCrystal
extends ModelBase {
    private ModelRenderer field_41057_g;
    private ModelRenderer field_41058_h = new ModelRenderer((ModelBase)this, "glass");
    private ModelRenderer field_41059_i;
    float ticks;

    public ModelNpcCrystal(float par1) {
        this.field_41058_h.setTextureOffset(0, 0).addBox(-4.0f, -4.0f, -4.0f, 8, 8, 8);
        this.field_41057_g = new ModelRenderer((ModelBase)this, "cube");
        this.field_41057_g.setTextureOffset(32, 0).addBox(-4.0f, -4.0f, -4.0f, 8, 8, 8);
        this.field_41059_i = new ModelRenderer((ModelBase)this, "base");
        this.field_41059_i.setTextureOffset(0, 16).addBox(-6.0f, 16.0f, -6.0f, 12, 4, 12);
    }

    public void setLivingAnimations(EntityLivingBase par1EntityLiving, float f6, float f5, float par9) {
        this.ticks = par9;
    }

    public void render(Entity par1Entity, float par2, float par3, float par4, float par5, float par6, float par7) {
        GlStateManager.pushMatrix();
        GlStateManager.scale((float)2.0f, (float)2.0f, (float)2.0f);
        GlStateManager.translate((float)0.0f, (float)-0.5f, (float)0.0f);
        this.field_41059_i.render(par7);
        float f = (float)par1Entity.ticksExisted + this.ticks;
        float f1 = MathHelper.sin((float)(f * 0.2f)) / 2.0f + 0.5f;
        f1 = f1 * f1 + f1;
        par3 = f * 3.0f;
        par4 = f1 * 0.2f;
        GlStateManager.rotate((float)par3, (float)0.0f, (float)1.0f, (float)0.0f);
        GlStateManager.translate((float)0.0f, (float)(0.1f + par4), (float)0.0f);
        GlStateManager.rotate((float)60.0f, (float)0.7071f, (float)0.0f, (float)0.7071f);
        this.field_41058_h.render(par7);
        float sca = 0.875f;
        GlStateManager.scale((float)sca, (float)sca, (float)sca);
        GlStateManager.rotate((float)60.0f, (float)0.7071f, (float)0.0f, (float)0.7071f);
        GlStateManager.rotate((float)par3, (float)0.0f, (float)1.0f, (float)0.0f);
        this.field_41058_h.render(par7);
        GlStateManager.scale((float)sca, (float)sca, (float)sca);
        GlStateManager.rotate((float)60.0f, (float)0.7071f, (float)0.0f, (float)0.7071f);
        GlStateManager.rotate((float)par3, (float)0.0f, (float)1.0f, (float)0.0f);
        this.field_41057_g.render(par7);
        GlStateManager.popMatrix();
    }
}

