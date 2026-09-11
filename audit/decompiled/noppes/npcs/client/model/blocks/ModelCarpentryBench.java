/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.model.ModelRenderer
 *  net.minecraft.entity.Entity
 */
package noppes.npcs.client.model.blocks;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

public class ModelCarpentryBench
extends ModelBase {
    ModelRenderer Leg1;
    ModelRenderer Leg2;
    ModelRenderer Leg3;
    ModelRenderer Leg4;
    ModelRenderer Bottom_plate;
    ModelRenderer Desktop;
    ModelRenderer Backboard;
    ModelRenderer Vice_Jaw1;
    ModelRenderer Vice_Jaw2;
    ModelRenderer Vice_Base1;
    ModelRenderer Vice_Base2;
    ModelRenderer Vice_Crank;
    ModelRenderer Vice_Screw;
    ModelRenderer Blueprint;

    public ModelCarpentryBench() {
        this.field_78090_t = 128;
        this.field_78089_u = 64;
        this.Leg1 = new ModelRenderer((ModelBase)this, 0, 0);
        this.Leg1.func_78789_a(0.0f, 0.0f, 0.0f, 2, 14, 2);
        this.Leg1.func_78793_a(6.0f, 10.0f, 5.0f);
        this.Leg2 = new ModelRenderer((ModelBase)this, 0, 0);
        this.Leg2.func_78789_a(0.0f, 0.0f, 0.0f, 2, 14, 2);
        this.Leg2.func_78793_a(6.0f, 10.0f, -5.0f);
        this.Leg3 = new ModelRenderer((ModelBase)this, 0, 0);
        this.Leg3.func_78789_a(0.0f, 0.0f, 0.0f, 2, 14, 2);
        this.Leg3.func_78793_a(-8.0f, 10.0f, 5.0f);
        this.Leg4 = new ModelRenderer((ModelBase)this, 0, 0);
        this.Leg4.func_78789_a(0.0f, 0.0f, 0.0f, 2, 14, 2);
        this.Leg4.func_78793_a(-8.0f, 10.0f, -5.0f);
        this.Bottom_plate = new ModelRenderer((ModelBase)this, 0, 24);
        this.Bottom_plate.func_78789_a(0.0f, 0.0f, 0.0f, 14, 1, 10);
        this.Bottom_plate.func_78793_a(-7.0f, 21.0f, -4.0f);
        this.Bottom_plate.func_78787_b(130, 64);
        this.Desktop = new ModelRenderer((ModelBase)this, 0, 3);
        this.Desktop.func_78789_a(0.0f, 0.0f, 0.0f, 18, 2, 13);
        this.Desktop.func_78793_a(-9.0f, 9.0f, -6.0f);
        this.Backboard = new ModelRenderer((ModelBase)this, 0, 18);
        this.Backboard.func_78789_a(-1.0f, 0.0f, 0.0f, 18, 5, 1);
        this.Backboard.func_78793_a(-8.0f, 7.0f, 7.0f);
        this.Vice_Jaw1 = new ModelRenderer((ModelBase)this, 54, 18);
        this.Vice_Jaw1.func_78789_a(0.0f, 0.0f, 0.0f, 3, 2, 1);
        this.Vice_Jaw1.func_78793_a(3.0f, 6.0f, -8.0f);
        this.Vice_Jaw2 = new ModelRenderer((ModelBase)this, 54, 21);
        this.Vice_Jaw2.func_78789_a(0.0f, 0.0f, 0.0f, 3, 2, 1);
        this.Vice_Jaw2.func_78793_a(3.0f, 6.0f, -6.0f);
        this.Vice_Base1 = new ModelRenderer((ModelBase)this, 38, 30);
        this.Vice_Base1.func_78789_a(0.0f, 0.0f, 0.0f, 3, 1, 3);
        this.Vice_Base1.func_78793_a(3.0f, 8.0f, -5.0f);
        this.Vice_Base2 = new ModelRenderer((ModelBase)this, 38, 25);
        this.Vice_Base2.func_78789_a(0.0f, 0.0f, 0.0f, 1, 2, 2);
        this.Vice_Base2.func_78793_a(4.0f, 7.0f, -5.0f);
        this.Vice_Crank = new ModelRenderer((ModelBase)this, 54, 24);
        this.Vice_Crank.func_78789_a(0.0f, 0.0f, 0.0f, 1, 5, 1);
        this.Vice_Crank.func_78793_a(6.0f, 6.0f, -9.0f);
        this.Vice_Screw = new ModelRenderer((ModelBase)this, 44, 25);
        this.Vice_Screw.func_78789_a(0.0f, 0.0f, 0.0f, 1, 1, 4);
        this.Vice_Screw.func_78793_a(4.0f, 8.0f, -8.0f);
        this.Blueprint = new ModelRenderer((ModelBase)this, 31, 18);
        this.Blueprint.func_78789_a(0.0f, 0.0f, 0.0f, 8, 0, 7);
        this.Blueprint.func_78793_a(0.0f, 9.0f, 1.0f);
        this.setRotation(this.Blueprint, 0.3271718f, 0.1487144f, 0.0f);
    }

    public void func_78088_a(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        super.func_78088_a(entity, f, f1, f2, f3, f4, f5);
        this.func_78087_a(f, f1, f2, f3, f4, f5, entity);
        this.Leg1.func_78785_a(f5);
        this.Leg2.func_78785_a(f5);
        this.Leg3.func_78785_a(f5);
        this.Leg4.func_78785_a(f5);
        this.Bottom_plate.func_78785_a(f5);
        this.Desktop.func_78785_a(f5);
        this.Backboard.func_78785_a(f5);
        this.Vice_Jaw1.func_78785_a(f5);
        this.Vice_Jaw2.func_78785_a(f5);
        this.Vice_Base1.func_78785_a(f5);
        this.Vice_Base2.func_78785_a(f5);
        this.Vice_Crank.func_78785_a(f5);
        this.Vice_Screw.func_78785_a(f5);
        this.Blueprint.func_78785_a(f5);
    }

    private void setRotation(ModelRenderer model, float x, float y, float z) {
        model.field_78795_f = x;
        model.field_78796_g = y;
        model.field_78808_h = z;
    }
}

