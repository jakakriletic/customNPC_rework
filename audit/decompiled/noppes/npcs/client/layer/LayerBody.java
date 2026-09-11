/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.model.ModelRenderer
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.entity.RenderLiving
 *  net.minecraft.util.math.MathHelper
 */
package noppes.npcs.client.layer;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.ModelPartData;
import noppes.npcs.client.layer.LayerInterface;
import noppes.npcs.client.model.Model2DRenderer;
import noppes.npcs.client.model.ModelPlaneRenderer;
import noppes.npcs.constants.EnumParts;

public class LayerBody
extends LayerInterface {
    private Model2DRenderer lWing;
    private Model2DRenderer rWing;
    private Model2DRenderer breasts;
    private ModelRenderer breasts2;
    private ModelRenderer breasts3;
    private ModelPlaneRenderer skirt;
    private Model2DRenderer fin;

    public LayerBody(RenderLiving render) {
        super(render);
        this.createParts();
    }

    private void createParts() {
        this.lWing = new Model2DRenderer((ModelBase)this.model, 56.0f, 16.0f, 8, 16);
        this.lWing.field_78809_i = true;
        this.lWing.func_78793_a(2.0f, 2.5f, 1.0f);
        this.lWing.setRotationOffset(8.0f, 14.0f, 0.0f);
        this.setRotation(this.lWing, 0.7141593f, -0.5235988f, -0.5090659f);
        this.rWing = new Model2DRenderer((ModelBase)this.model, 56.0f, 16.0f, 8, 16);
        this.rWing.func_78793_a(-2.0f, 2.5f, 1.0f);
        this.rWing.setRotationOffset(-8.0f, 14.0f, 0.0f);
        this.setRotation(this.rWing, 0.7141593f, 0.5235988f, 0.5090659f);
        this.breasts = new Model2DRenderer((ModelBase)this.model, 20.0f, 22.0f, 8, 3);
        this.breasts.func_78793_a(-3.6f, 5.2f, -3.0f);
        this.breasts.setScale(0.17f, 0.19f);
        this.breasts.setThickness(1.0f);
        this.breasts2 = new ModelRenderer((ModelBase)this.model);
        Model2DRenderer bottom = new Model2DRenderer((ModelBase)this.model, 20.0f, 22.0f, 8, 4);
        bottom.func_78793_a(-3.6f, 5.0f, -3.1f);
        bottom.setScale(0.225f, 0.2f);
        bottom.setThickness(2.0f);
        bottom.field_78795_f = -0.31415927f;
        this.breasts2.func_78792_a((ModelRenderer)bottom);
        this.breasts3 = new ModelRenderer((ModelBase)this.model);
        Model2DRenderer right = new Model2DRenderer((ModelBase)this.model, 20.0f, 23.0f, 3, 2);
        right.func_78793_a(-3.8f, 5.3f, -3.6f);
        right.setScale(0.12f, 0.14f);
        right.setThickness(1.75f);
        this.breasts3.func_78792_a((ModelRenderer)right);
        Model2DRenderer right2 = new Model2DRenderer((ModelBase)this.model, 20.0f, 22.0f, 3, 1);
        right2.func_78793_a(-3.79f, 4.1f, -3.14f);
        right2.setScale(0.06f, 0.07f);
        right2.setThickness(1.75f);
        right2.field_78795_f = 0.34906584f;
        this.breasts3.func_78792_a((ModelRenderer)right2);
        Model2DRenderer right3 = new Model2DRenderer((ModelBase)this.model, 20.0f, 24.0f, 3, 1);
        right3.func_78793_a(-3.79f, 5.3f, -3.6f);
        right3.setScale(0.06f, 0.07f);
        right3.setThickness(1.75f);
        right3.field_78795_f = -0.34906584f;
        this.breasts3.func_78792_a((ModelRenderer)right3);
        Model2DRenderer right4 = new Model2DRenderer((ModelBase)this.model, 21.0f, 23.0f, 1, 2);
        right4.func_78793_a(-1.8f, 5.3f, -3.14f);
        right4.setScale(0.12f, 0.14f);
        right4.setThickness(1.75f);
        right4.field_78796_g = 0.34906584f;
        this.breasts3.func_78792_a((ModelRenderer)right4);
        Model2DRenderer left = new Model2DRenderer((ModelBase)this.model, 25.0f, 23.0f, 3, 2);
        left.func_78793_a(0.8f, 5.3f, -3.6f);
        left.setScale(0.12f, 0.14f);
        left.setThickness(1.75f);
        this.breasts3.func_78792_a((ModelRenderer)left);
        Model2DRenderer left2 = new Model2DRenderer((ModelBase)this.model, 25.0f, 22.0f, 3, 1);
        left2.func_78793_a(0.81f, 4.1f, -3.18f);
        left2.setScale(0.06f, 0.07f);
        left2.setThickness(1.75f);
        left2.field_78795_f = 0.34906584f;
        this.breasts3.func_78792_a((ModelRenderer)left2);
        Model2DRenderer left3 = new Model2DRenderer((ModelBase)this.model, 25.0f, 24.0f, 3, 1);
        left3.func_78793_a(0.81f, 5.3f, -3.6f);
        left3.setScale(0.06f, 0.07f);
        left3.setThickness(1.75f);
        left3.field_78795_f = -0.34906584f;
        this.breasts3.func_78792_a((ModelRenderer)left3);
        Model2DRenderer left4 = new Model2DRenderer((ModelBase)this.model, 24.0f, 23.0f, 1, 2);
        left4.func_78793_a(0.8f, 5.3f, -3.6f);
        left4.setScale(0.12f, 0.14f);
        left4.setThickness(1.75f);
        left4.field_78796_g = -0.34906584f;
        this.breasts3.func_78792_a((ModelRenderer)left4);
        this.skirt = new ModelPlaneRenderer((ModelBase)this.model, 58, 18);
        this.skirt.addSidePlane(0.0f, 0.0f, 0.0f, 9, 2);
        ModelPlaneRenderer part1 = new ModelPlaneRenderer((ModelBase)this.model, 58, 18);
        part1.addSidePlane(2.0f, 0.0f, 0.0f, 9, 2);
        part1.field_78796_g = -1.5707964f;
        this.skirt.func_78792_a(part1);
        this.skirt.func_78793_a(2.4f, 8.8f, 0.0f);
        this.setRotation(this.skirt, 0.3f, -0.2f, -0.2f);
        this.fin = new Model2DRenderer((ModelBase)this.model, 56.0f, 20.0f, 8, 12);
        this.fin.func_78793_a(-0.5f, 12.0f, 10.0f);
        this.fin.setScale(0.74f);
        this.fin.field_78796_g = 1.5707964f;
    }

    @Override
    public void render(float par2, float par3, float par4, float par5, float par6, float par7) {
        this.model.field_78115_e.func_78794_c(0.0625f);
        this.renderSkirt(par7);
        this.renderWings(par7);
        this.renderFin(par7);
        this.renderBreasts(par7);
    }

    private void renderWings(float par7) {
        ModelPartData data = this.playerdata.getPartData(EnumParts.WINGS);
        if (data == null) {
            return;
        }
        this.preRender(data);
        this.rWing.func_78785_a(par7);
        this.lWing.func_78785_a(par7);
    }

    private void renderSkirt(float par7) {
        ModelPartData data = this.playerdata.getPartData(EnumParts.SKIRT);
        if (data == null) {
            return;
        }
        this.preRender(data);
        GlStateManager.func_179094_E();
        GlStateManager.func_179152_a((float)1.7f, (float)1.04f, (float)1.6f);
        for (int i = 0; i < 10; ++i) {
            GlStateManager.func_179114_b((float)36.0f, (float)0.0f, (float)1.0f, (float)0.0f);
            this.skirt.func_78785_a(par7);
        }
        GlStateManager.func_179121_F();
    }

    private void renderFin(float par7) {
        ModelPartData data = this.playerdata.getPartData(EnumParts.FIN);
        if (data == null) {
            return;
        }
        this.preRender(data);
        this.fin.func_78785_a(par7);
    }

    private void renderBreasts(float par7) {
        ModelPartData data = this.playerdata.getPartData(EnumParts.BREASTS);
        if (data == null) {
            return;
        }
        data.playerTexture = true;
        this.preRender(data);
        if (data.type == 0) {
            this.breasts.func_78785_a(par7);
        }
        if (data.type == 1) {
            this.breasts2.func_78785_a(par7);
        }
        if (data.type == 2) {
            this.breasts3.func_78785_a(par7);
        }
    }

    @Override
    public void rotate(float par1, float par2, float par3, float par4, float par5, float par6) {
        this.rWing.field_78795_f = 0.7141593f;
        this.rWing.field_78808_h = 0.5090659f;
        this.lWing.field_78795_f = 0.7141593f;
        this.lWing.field_78808_h = -0.5090659f;
        float motion = Math.abs(MathHelper.func_76126_a((float)(par1 * 0.033f + (float)Math.PI)) * 0.4f) * par2;
        if (!this.npc.field_70122_E || (double)motion > 0.01) {
            float speed = 0.55f + 0.5f * motion;
            float y = MathHelper.func_76126_a((float)(par3 * 0.55f));
            this.rWing.field_78808_h += y * 0.5f * speed;
            this.rWing.field_78795_f += y * 0.5f * speed;
            this.lWing.field_78808_h -= y * 0.5f * speed;
            this.lWing.field_78795_f += y * 0.5f * speed;
        } else {
            this.lWing.field_78808_h += MathHelper.func_76134_b((float)(par3 * 0.09f)) * 0.05f + 0.05f;
            this.rWing.field_78808_h -= MathHelper.func_76134_b((float)(par3 * 0.09f)) * 0.05f + 0.05f;
            this.lWing.field_78795_f += MathHelper.func_76126_a((float)(par3 * 0.067f)) * 0.05f;
            this.rWing.field_78795_f += MathHelper.func_76126_a((float)(par3 * 0.067f)) * 0.05f;
        }
        this.setRotation(this.skirt, 0.3f, -0.2f, -0.2f);
        this.skirt.field_78795_f += this.model.field_178724_i.field_78795_f * 0.04f;
        this.skirt.field_78808_h += this.model.field_178724_i.field_78795_f * 0.06f;
        this.skirt.field_78808_h -= MathHelper.func_76134_b((float)(par3 * 0.09f)) * 0.04f - 0.05f;
    }
}

