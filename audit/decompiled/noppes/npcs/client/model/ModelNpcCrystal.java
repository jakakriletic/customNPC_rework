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
        this.field_41058_h.func_78784_a(0, 0).func_78789_a(-4.0f, -4.0f, -4.0f, 8, 8, 8);
        this.field_41057_g = new ModelRenderer((ModelBase)this, "cube");
        this.field_41057_g.func_78784_a(32, 0).func_78789_a(-4.0f, -4.0f, -4.0f, 8, 8, 8);
        this.field_41059_i = new ModelRenderer((ModelBase)this, "base");
        this.field_41059_i.func_78784_a(0, 16).func_78789_a(-6.0f, 16.0f, -6.0f, 12, 4, 12);
    }

    public void func_78086_a(EntityLivingBase par1EntityLiving, float f6, float f5, float par9) {
        this.ticks = par9;
    }

    public void func_78088_a(Entity par1Entity, float par2, float par3, float par4, float par5, float par6, float par7) {
        GlStateManager.func_179094_E();
        GlStateManager.func_179152_a((float)2.0f, (float)2.0f, (float)2.0f);
        GlStateManager.func_179109_b((float)0.0f, (float)-0.5f, (float)0.0f);
        this.field_41059_i.func_78785_a(par7);
        float f = (float)par1Entity.field_70173_aa + this.ticks;
        float f1 = MathHelper.func_76126_a((float)(f * 0.2f)) / 2.0f + 0.5f;
        f1 = f1 * f1 + f1;
        par3 = f * 3.0f;
        par4 = f1 * 0.2f;
        GlStateManager.func_179114_b((float)par3, (float)0.0f, (float)1.0f, (float)0.0f);
        GlStateManager.func_179109_b((float)0.0f, (float)(0.1f + par4), (float)0.0f);
        GlStateManager.func_179114_b((float)60.0f, (float)0.7071f, (float)0.0f, (float)0.7071f);
        this.field_41058_h.func_78785_a(par7);
        float sca = 0.875f;
        GlStateManager.func_179152_a((float)sca, (float)sca, (float)sca);
        GlStateManager.func_179114_b((float)60.0f, (float)0.7071f, (float)0.0f, (float)0.7071f);
        GlStateManager.func_179114_b((float)par3, (float)0.0f, (float)1.0f, (float)0.0f);
        this.field_41058_h.func_78785_a(par7);
        GlStateManager.func_179152_a((float)sca, (float)sca, (float)sca);
        GlStateManager.func_179114_b((float)60.0f, (float)0.7071f, (float)0.0f, (float)0.7071f);
        GlStateManager.func_179114_b((float)par3, (float)0.0f, (float)1.0f, (float)0.0f);
        this.field_41057_g.func_78785_a(par7);
        GlStateManager.func_179121_F();
    }
}

