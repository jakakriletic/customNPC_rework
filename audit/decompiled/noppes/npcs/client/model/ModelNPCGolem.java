/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.model.ModelBiped$ArmPose
 *  net.minecraft.client.model.ModelRenderer
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.math.MathHelper
 */
package noppes.npcs.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.client.model.ModelBipedAlt;
import noppes.npcs.entity.EntityNPCInterface;

public class ModelNPCGolem
extends ModelBipedAlt {
    private ModelRenderer bipedLowerBody;

    public ModelNPCGolem(float scale) {
        super(scale);
        this.init(0.0f, 0.0f);
    }

    public void init(float f, float f1) {
        int short1 = 128;
        int short2 = 128;
        float f2 = -7.0f;
        this.field_78116_c = new ModelRenderer((ModelBase)this).func_78787_b(short1, short2);
        this.field_78116_c.func_78793_a(0.0f, f2, -2.0f);
        this.field_78116_c.func_78784_a(0, 0).func_78790_a(-4.0f, -12.0f, -5.5f, 8, 10, 8, f);
        this.field_78116_c.func_78784_a(24, 0).func_78790_a(-1.0f, -5.0f, -7.5f, 2, 4, 2, f);
        this.field_178720_f = new ModelRenderer((ModelBase)this).func_78787_b(short1, short2);
        this.field_178720_f.func_78793_a(0.0f, f2, -2.0f);
        this.field_178720_f.func_78784_a(0, 85).func_78790_a(-4.0f, -12.0f, -5.5f, 8, 10, 8, f + 0.5f);
        this.field_78115_e = new ModelRenderer((ModelBase)this).func_78787_b(short1, short2);
        this.field_78115_e.func_78793_a(0.0f, 0.0f + f2, 0.0f);
        this.field_78115_e.func_78784_a(0, 40).func_78790_a(-9.0f, -2.0f, -6.0f, 18, 12, 11, f + 0.2f);
        this.field_78115_e.func_78784_a(0, 21).func_78790_a(-9.0f, -2.0f, -6.0f, 18, 8, 11, f);
        this.bipedLowerBody = new ModelRenderer((ModelBase)this).func_78787_b(short1, short2);
        this.bipedLowerBody.func_78793_a(0.0f, 0.0f + f2, 0.0f);
        this.bipedLowerBody.func_78784_a(0, 70).func_78790_a(-4.5f, 10.0f, -3.0f, 9, 5, 6, f + 0.5f);
        this.bipedLowerBody.func_78784_a(30, 70).func_78790_a(-4.5f, 6.0f, -3.0f, 9, 9, 6, f + 0.4f);
        this.field_178723_h = new ModelRenderer((ModelBase)this).func_78787_b(short1, short2);
        this.field_178723_h.func_78793_a(0.0f, f2, 0.0f);
        this.field_178723_h.func_78784_a(60, 21).func_78790_a(-13.0f, -2.5f, -3.0f, 4, 30, 6, f + 0.2f);
        this.field_178723_h.func_78784_a(80, 21).func_78790_a(-13.0f, -2.5f, -3.0f, 4, 20, 6, f);
        this.field_178723_h.func_78784_a(100, 21).func_78790_a(-13.0f, -2.5f, -3.0f, 4, 20, 6, f + 1.0f);
        this.field_178724_i = new ModelRenderer((ModelBase)this).func_78787_b(short1, short2);
        this.field_178724_i.func_78793_a(0.0f, f2, 0.0f);
        this.field_178724_i.func_78784_a(60, 58).func_78790_a(9.0f, -2.5f, -3.0f, 4, 30, 6, f + 0.2f);
        this.field_178724_i.func_78784_a(80, 58).func_78790_a(9.0f, -2.5f, -3.0f, 4, 20, 6, f);
        this.field_178724_i.func_78784_a(100, 58).func_78790_a(9.0f, -2.5f, -3.0f, 4, 20, 6, f + 1.0f);
        this.field_178722_k = new ModelRenderer((ModelBase)this, 0, 22).func_78787_b(short1, short2);
        this.field_178722_k.func_78793_a(-4.0f, 18.0f + f2, 0.0f);
        this.field_178722_k.func_78784_a(37, 0).func_78790_a(-3.5f, -3.0f, -3.0f, 6, 16, 5, f);
        this.field_178721_j = new ModelRenderer((ModelBase)this, 0, 22).func_78787_b(short1, short2);
        this.field_178721_j.field_78809_i = true;
        this.field_178721_j.func_78784_a(60, 0).func_78790_a(-3.5f, -3.0f, -3.0f, 6, 16, 5, f);
        this.field_178721_j.func_78793_a(5.0f, 18.0f + f2, 0.0f);
    }

    public void func_78088_a(Entity par1Entity, float par2, float par3, float par4, float par5, float par6, float par7) {
        super.func_78088_a(par1Entity, par2, par3, par4, par5, par6, par7);
        this.bipedLowerBody.func_78785_a(par7);
    }

    @Override
    public void func_78087_a(float par1, float par2, float par3, float par4, float par5, float par6, Entity entity) {
        EntityNPCInterface npc = (EntityNPCInterface)entity;
        this.field_78093_q = npc.func_184218_aH();
        if (this.field_78117_n && (npc.currentAnimation == 7 || npc.currentAnimation == 2)) {
            this.field_78117_n = false;
        }
        this.field_78116_c.field_78796_g = par4 / 57.295776f;
        this.field_78116_c.field_78795_f = par5 / 57.295776f;
        this.field_178720_f.field_78796_g = this.field_78116_c.field_78796_g;
        this.field_178720_f.field_78795_f = this.field_78116_c.field_78795_f;
        this.field_178722_k.field_78795_f = -1.5f * this.func_78172_a(par1, 13.0f) * par2;
        this.field_178721_j.field_78795_f = 1.5f * this.func_78172_a(par1, 13.0f) * par2;
        this.field_178722_k.field_78796_g = 0.0f;
        this.field_178721_j.field_78796_g = 0.0f;
        float f6 = MathHelper.func_76126_a((float)(this.field_78095_p * (float)Math.PI));
        float f7 = MathHelper.func_76126_a((float)((16.0f - (1.0f - this.field_78095_p) * (1.0f - this.field_78095_p)) * (float)Math.PI));
        if ((double)this.field_78095_p > 0.0) {
            this.field_178723_h.field_78808_h = 0.0f;
            this.field_178724_i.field_78808_h = 0.0f;
            this.field_178723_h.field_78796_g = -(0.1f - f6 * 0.6f);
            this.field_178724_i.field_78796_g = 0.1f - f6 * 0.6f;
            this.field_178723_h.field_78795_f = 0.0f;
            this.field_178724_i.field_78795_f = 0.0f;
            this.field_178723_h.field_78795_f = -1.5707964f;
            this.field_178724_i.field_78795_f = -1.5707964f;
            this.field_178723_h.field_78795_f -= f6 * 1.2f - f7 * 0.4f;
            this.field_178724_i.field_78795_f -= f6 * 1.2f - f7 * 0.4f;
        } else if (this.field_187076_m == ModelBiped.ArmPose.BOW_AND_ARROW) {
            float f1 = 0.0f;
            float f3 = 0.0f;
            this.field_178723_h.field_78808_h = 0.0f;
            this.field_178723_h.field_78795_f = -1.5707964f + this.field_78116_c.field_78795_f;
            this.field_178723_h.field_78795_f -= f1 * 1.2f - f3 * 0.4f;
            this.field_178723_h.field_78808_h += MathHelper.func_76134_b((float)(par3 * 0.09f)) * 0.05f + 0.05f;
            this.field_178723_h.field_78795_f += MathHelper.func_76126_a((float)(par3 * 0.067f)) * 0.05f;
            this.field_178724_i.field_78795_f = (-0.2f - 1.5f * this.func_78172_a(par1, 13.0f)) * par2;
            this.field_78115_e.field_78796_g = -(0.1f - f1 * 0.6f) + this.field_78116_c.field_78796_g;
            this.field_178723_h.field_78796_g = -(0.1f - f1 * 0.6f) + this.field_78116_c.field_78796_g;
            this.field_178724_i.field_78796_g = 0.1f - f1 * 0.6f + this.field_78116_c.field_78796_g;
        } else {
            this.field_178723_h.field_78795_f = (-0.2f + 1.5f * this.func_78172_a(par1, 13.0f)) * par2;
            this.field_178724_i.field_78795_f = (-0.2f - 1.5f * this.func_78172_a(par1, 13.0f)) * par2;
            this.field_78115_e.field_78796_g = 0.0f;
            this.field_178723_h.field_78796_g = 0.0f;
            this.field_178724_i.field_78796_g = 0.0f;
            this.field_178723_h.field_78808_h = 0.0f;
            this.field_178724_i.field_78808_h = 0.0f;
        }
        if (this.field_78093_q) {
            this.field_178723_h.field_78795_f += -0.62831855f;
            this.field_178724_i.field_78795_f += -0.62831855f;
            this.field_178722_k.field_78795_f = -1.2566371f;
            this.field_178721_j.field_78795_f = -1.2566371f;
            this.field_178722_k.field_78796_g = 0.31415927f;
            this.field_178721_j.field_78796_g = -0.31415927f;
        }
    }

    private float func_78172_a(float par1, float par2) {
        return (Math.abs(par1 % par2 - par2 * 0.5f) - par2 * 0.25f) / (par2 * 0.25f);
    }
}

