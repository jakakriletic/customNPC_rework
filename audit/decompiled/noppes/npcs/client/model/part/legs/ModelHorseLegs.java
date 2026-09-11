/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.model.ModelBiped
 *  net.minecraft.client.model.ModelRenderer
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.math.MathHelper
 */
package noppes.npcs.client.model.part.legs;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.ModelData;

public class ModelHorseLegs
extends ModelRenderer {
    private ModelRenderer backLeftLeg;
    private ModelRenderer backLeftShin;
    private ModelRenderer backLeftHoof;
    private ModelRenderer backRightLeg;
    private ModelRenderer backRightShin;
    private ModelRenderer backRightHoof;
    private ModelRenderer frontLeftLeg;
    private ModelRenderer frontLeftShin;
    private ModelRenderer frontLeftHoof;
    private ModelRenderer frontRightLeg;
    private ModelRenderer frontRightShin;
    private ModelRenderer frontRightHoof;
    private ModelBiped base;

    public ModelHorseLegs(ModelBiped model) {
        super((ModelBase)model);
        this.base = model;
        float var1 = 0.0f;
        int var2 = 15;
        int zOffset = 10;
        float yOffset = 7.0f;
        ModelRenderer body = new ModelRenderer((ModelBase)model, 0, 34);
        body.func_78787_b(128, 128);
        body.func_78789_a(-5.0f, -8.0f, -19.0f, 10, 10, 24);
        body.func_78793_a(0.0f, 11.0f + yOffset, 9.0f + (float)zOffset);
        this.func_78792_a(body);
        this.backLeftLeg = new ModelRenderer((ModelBase)model, 78, 29);
        this.backLeftLeg.func_78787_b(128, 128);
        this.backLeftLeg.func_78789_a(-2.0f, -2.0f, -2.5f, 4, 9, 5);
        this.backLeftLeg.func_78793_a(4.0f, 9.0f + yOffset, 11.0f + (float)zOffset);
        this.func_78792_a(this.backLeftLeg);
        this.backLeftShin = new ModelRenderer((ModelBase)model, 78, 43);
        this.backLeftShin.func_78787_b(128, 128);
        this.backLeftShin.func_78789_a(-1.5f, 0.0f, -1.5f, 3, 5, 3);
        this.backLeftShin.func_78793_a(0.0f, 7.0f, 0.0f);
        this.backLeftLeg.func_78792_a(this.backLeftShin);
        this.backLeftHoof = new ModelRenderer((ModelBase)model, 78, 51);
        this.backLeftHoof.func_78787_b(128, 128);
        this.backLeftHoof.func_78789_a(-2.0f, 5.0f, -2.0f, 4, 3, 4);
        this.backLeftHoof.func_78793_a(0.0f, 7.0f, 0.0f);
        this.backLeftLeg.func_78792_a(this.backLeftHoof);
        this.backRightLeg = new ModelRenderer((ModelBase)model, 96, 29);
        this.backRightLeg.func_78787_b(128, 128);
        this.backRightLeg.func_78789_a(-2.0f, -2.0f, -2.5f, 4, 9, 5);
        this.backRightLeg.func_78793_a(-4.0f, 9.0f + yOffset, 11.0f + (float)zOffset);
        this.func_78792_a(this.backRightLeg);
        this.backRightShin = new ModelRenderer((ModelBase)model, 96, 43);
        this.backRightShin.func_78787_b(128, 128);
        this.backRightShin.func_78789_a(-1.5f, 0.0f, -1.5f, 3, 5, 3);
        this.backRightShin.func_78793_a(0.0f, 7.0f, 0.0f);
        this.backRightLeg.func_78792_a(this.backRightShin);
        this.backRightHoof = new ModelRenderer((ModelBase)model, 96, 51);
        this.backRightHoof.func_78787_b(128, 128);
        this.backRightHoof.func_78789_a(-2.0f, 5.0f, -2.0f, 4, 3, 4);
        this.backRightHoof.func_78793_a(0.0f, 7.0f, 0.0f);
        this.backRightLeg.func_78792_a(this.backRightHoof);
        this.frontLeftLeg = new ModelRenderer((ModelBase)model, 44, 29);
        this.frontLeftLeg.func_78787_b(128, 128);
        this.frontLeftLeg.func_78789_a(-1.4f, -1.0f, -2.1f, 3, 8, 4);
        this.frontLeftLeg.func_78793_a(4.0f, 9.0f + yOffset, -8.0f + (float)zOffset);
        this.func_78792_a(this.frontLeftLeg);
        this.frontLeftShin = new ModelRenderer((ModelBase)model, 44, 41);
        this.frontLeftShin.func_78787_b(128, 128);
        this.frontLeftShin.func_78789_a(-1.4f, 0.0f, -1.6f, 3, 5, 3);
        this.frontLeftShin.func_78793_a(0.0f, 7.0f, 0.0f);
        this.frontLeftLeg.func_78792_a(this.frontLeftShin);
        this.frontLeftHoof = new ModelRenderer((ModelBase)model, 44, 51);
        this.frontLeftHoof.func_78787_b(128, 128);
        this.frontLeftHoof.func_78789_a(-1.9f, 5.0f, -2.1f, 4, 3, 4);
        this.frontLeftHoof.func_78793_a(0.0f, 7.0f, 0.0f);
        this.frontLeftLeg.func_78792_a(this.frontLeftHoof);
        this.frontRightLeg = new ModelRenderer((ModelBase)model, 60, 29);
        this.frontRightLeg.func_78787_b(128, 128);
        this.frontRightLeg.func_78789_a(-1.6f, -1.0f, -2.1f, 3, 8, 4);
        this.frontRightLeg.func_78793_a(-4.0f, 9.0f + yOffset, -8.0f + (float)zOffset);
        this.func_78792_a(this.frontRightLeg);
        this.frontRightShin = new ModelRenderer((ModelBase)model, 60, 41);
        this.frontRightShin.func_78787_b(128, 128);
        this.frontRightShin.func_78789_a(-1.6f, 0.0f, -1.6f, 3, 5, 3);
        this.frontRightShin.func_78793_a(0.0f, 7.0f, 0.0f);
        this.frontRightLeg.func_78792_a(this.frontRightShin);
        this.frontRightHoof = new ModelRenderer((ModelBase)model, 60, 51);
        this.frontRightHoof.func_78787_b(128, 128);
        this.frontRightHoof.func_78789_a(-2.1f, 5.0f, -2.1f, 4, 3, 4);
        this.frontRightHoof.func_78793_a(0.0f, 7.0f, 0.0f);
        this.frontRightLeg.func_78792_a(this.frontRightHoof);
    }

    public void setRotationAngles(ModelData data, float par1, float par2, float par3, float par4, float par5, float par6, Entity entity) {
        this.frontLeftLeg.field_78795_f = MathHelper.func_76134_b((float)(par1 * 0.6662f)) * 0.4f * par2;
        this.frontRightLeg.field_78795_f = MathHelper.func_76134_b((float)(par1 * 0.6662f + (float)Math.PI)) * 0.4f * par2;
        this.backLeftLeg.field_78795_f = MathHelper.func_76134_b((float)(par1 * 0.6662f + (float)Math.PI)) * 0.4f * par2;
        this.backRightLeg.field_78795_f = MathHelper.func_76134_b((float)(par1 * 0.6662f)) * 0.4f * par2;
    }

    private void setRotation(ModelRenderer model, float x, float y, float z) {
        model.field_78795_f = x;
        model.field_78796_g = y;
        model.field_78808_h = z;
    }
}

