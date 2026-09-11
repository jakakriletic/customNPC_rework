/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.model.ModelRenderer
 *  net.minecraft.client.renderer.entity.RenderLiving
 */
package noppes.npcs.client.layer;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderLiving;
import noppes.npcs.ModelPartData;
import noppes.npcs.client.layer.LayerInterface;
import noppes.npcs.client.model.Model2DRenderer;
import noppes.npcs.client.model.part.head.ModelDuckBeak;
import noppes.npcs.client.model.part.horns.ModelAntennasBack;
import noppes.npcs.client.model.part.horns.ModelAntennasFront;
import noppes.npcs.client.model.part.horns.ModelAntlerHorns;
import noppes.npcs.client.model.part.horns.ModelBullHorns;
import noppes.npcs.constants.EnumParts;

public class LayerHead
extends LayerInterface {
    private ModelRenderer small;
    private ModelRenderer medium;
    private ModelRenderer large;
    private ModelRenderer bunnySnout;
    private ModelRenderer beak;
    private Model2DRenderer beard;
    private Model2DRenderer hair;
    private Model2DRenderer mohawk;
    private ModelRenderer bull;
    private ModelRenderer antlers;
    private ModelRenderer antennasBack;
    private ModelRenderer antennasFront;
    private ModelRenderer ears;
    private ModelRenderer bunnyEars;

    public LayerHead(RenderLiving render) {
        super(render);
        this.createParts();
    }

    private void createParts() {
        this.small = new ModelRenderer((ModelBase)this.model, 24, 0);
        this.small.func_78789_a(0.0f, 0.0f, 0.0f, 4, 3, 1);
        this.small.func_78793_a(-2.0f, -3.0f, -5.0f);
        this.medium = new ModelRenderer((ModelBase)this.model, 24, 0);
        this.medium.func_78789_a(0.0f, 0.0f, 0.0f, 4, 3, 2);
        this.medium.func_78793_a(-2.0f, -3.0f, -6.0f);
        this.large = new ModelRenderer((ModelBase)this.model, 24, 0);
        this.large.func_78789_a(0.0f, 0.0f, 0.0f, 4, 3, 3);
        this.large.func_78793_a(-2.0f, -3.0f, -7.0f);
        this.bunnySnout = new ModelRenderer((ModelBase)this.model, 24, 0);
        this.bunnySnout.func_78789_a(1.0f, 1.0f, 0.0f, 4, 2, 1);
        this.bunnySnout.func_78793_a(-3.0f, -4.0f, -5.0f);
        ModelRenderer tooth = new ModelRenderer((ModelBase)this.model, 24, 3);
        tooth.func_78789_a(2.0f, 3.0f, 0.0f, 2, 1, 1);
        tooth.func_78793_a(0.0f, 0.0f, 0.0f);
        this.bunnySnout.func_78792_a(tooth);
        this.beak = new ModelDuckBeak(this.model);
        this.beak.func_78793_a(0.0f, 0.0f, -4.0f);
        this.beard = new Model2DRenderer((ModelBase)this.model, 56.0f, 20.0f, 8, 12);
        this.beard.setRotationOffset(-3.99f, 11.8f, -4.0f);
        this.beard.setScale(0.74f);
        this.hair = new Model2DRenderer((ModelBase)this.model, 56.0f, 20.0f, 8, 12);
        this.hair.setRotationOffset(-3.99f, 11.8f, 3.0f);
        this.hair.setScale(0.75f);
        this.mohawk = new Model2DRenderer((ModelBase)this.model, 0.0f, 0.0f, 64, 64);
        this.mohawk.setRotationOffset(-9.0f, 0.1f, -0.5f);
        this.setRotation(this.mohawk, 0.0f, 1.5707964f, 0.0f);
        this.mohawk.setScale(0.825f);
        this.bull = new ModelBullHorns(this.model);
        this.antlers = new ModelAntlerHorns(this.model);
        this.antennasBack = new ModelAntennasBack(this.model);
        this.antennasFront = new ModelAntennasFront(this.model);
        this.ears = new ModelRenderer((ModelBase)this.model);
        Model2DRenderer right = new Model2DRenderer((ModelBase)this.model, 56.0f, 0.0f, 8, 4);
        right.func_78793_a(-7.44f, -7.3f, -0.0f);
        right.setScale(0.234f, 0.234f);
        right.setThickness(1.16f);
        this.ears.func_78792_a((ModelRenderer)right);
        Model2DRenderer left = new Model2DRenderer((ModelBase)this.model, 56.0f, 0.0f, 8, 4);
        left.func_78793_a(7.44f, -7.3f, 1.15f);
        left.setScale(0.234f, 0.234f);
        this.setRotation(left, 0.0f, (float)Math.PI, 0.0f);
        left.setThickness(1.16f);
        this.ears.func_78792_a((ModelRenderer)left);
        Model2DRenderer right2 = new Model2DRenderer((ModelBase)this.model, 56.0f, 4.0f, 8, 4);
        right2.func_78793_a(-7.44f, -7.3f, 1.14f);
        right2.setScale(0.234f, 0.234f);
        right2.setThickness(1.16f);
        this.ears.func_78792_a((ModelRenderer)right2);
        Model2DRenderer left2 = new Model2DRenderer((ModelBase)this.model, 56.0f, 4.0f, 8, 4);
        left2.func_78793_a(7.44f, -7.3f, 2.31f);
        left2.setScale(0.234f, 0.234f);
        this.setRotation(left2, 0.0f, (float)Math.PI, 0.0f);
        left2.setThickness(1.16f);
        this.ears.func_78792_a((ModelRenderer)left2);
        this.bunnyEars = new ModelRenderer((ModelBase)this.model);
        ModelRenderer earleft = new ModelRenderer((ModelBase)this.model, 56, 0);
        earleft.field_78809_i = true;
        earleft.func_78789_a(-1.466667f, -4.0f, 0.0f, 3, 7, 1);
        earleft.func_78793_a(2.533333f, -11.0f, 0.0f);
        this.bunnyEars.func_78792_a(earleft);
        ModelRenderer earright = new ModelRenderer((ModelBase)this.model, 56, 0);
        earright.func_78789_a(-1.5f, -4.0f, 0.0f, 3, 7, 1);
        earright.func_78793_a(-2.466667f, -11.0f, 0.0f);
        this.bunnyEars.func_78792_a(earright);
    }

    @Override
    public void render(float par2, float par3, float par4, float par5, float par6, float par7) {
        this.model.field_78116_c.func_78794_c(0.0625f);
        this.renderSnout(par7);
        this.renderBeard(par7);
        this.renderHair(par7);
        this.renderMohawk(par7);
        this.renderHorns(par7);
        this.renderEars(par7);
    }

    private void renderSnout(float par7) {
        ModelPartData data = this.playerdata.getPartData(EnumParts.SNOUT);
        if (data == null) {
            return;
        }
        this.preRender(data);
        if (data.type == 0) {
            this.small.func_78785_a(par7);
        } else if (data.type == 1) {
            this.medium.func_78785_a(par7);
        } else if (data.type == 2) {
            this.large.func_78785_a(par7);
        } else if (data.type == 3) {
            this.bunnySnout.func_78785_a(par7);
        } else if (data.type == 4) {
            this.beak.func_78785_a(par7);
        }
    }

    private void renderBeard(float par7) {
        ModelPartData data = this.playerdata.getPartData(EnumParts.BEARD);
        if (data == null) {
            return;
        }
        this.preRender(data);
        this.beard.func_78785_a(par7);
    }

    private void renderHair(float par7) {
        ModelPartData data = this.playerdata.getPartData(EnumParts.HAIR);
        if (data == null) {
            return;
        }
        this.preRender(data);
        this.hair.func_78785_a(par7);
    }

    private void renderMohawk(float par7) {
        ModelPartData data = this.playerdata.getPartData(EnumParts.MOHAWK);
        if (data == null) {
            return;
        }
        this.preRender(data);
        this.mohawk.func_78785_a(par7);
    }

    private void renderHorns(float par7) {
        ModelPartData data = this.playerdata.getPartData(EnumParts.HORNS);
        if (data == null) {
            return;
        }
        this.preRender(data);
        if (data.type == 0) {
            this.bull.func_78785_a(par7);
        } else if (data.type == 1) {
            this.antlers.func_78785_a(par7);
        } else if (data.type == 2 && data.pattern == 0) {
            this.antennasBack.func_78785_a(par7);
        } else if (data.type == 2 && data.pattern == 1) {
            this.antennasFront.func_78785_a(par7);
        }
    }

    private void renderEars(float par7) {
        ModelPartData data = this.playerdata.getPartData(EnumParts.EARS);
        if (data == null) {
            return;
        }
        this.preRender(data);
        if (data.type == 0) {
            this.ears.func_78785_a(par7);
        } else if (data.type == 1) {
            this.bunnyEars.func_78785_a(par7);
        }
    }

    @Override
    public void rotate(float par2, float par3, float par4, float par5, float par6, float par7) {
        ModelRenderer head = this.model.field_78116_c;
        if (head.field_78795_f < 0.0f) {
            this.beard.field_78795_f = 0.0f;
            this.hair.field_78795_f = -head.field_78795_f * 1.2f;
            if (head.field_78795_f > -1.0f) {
                this.hair.field_78797_d = -head.field_78795_f * 1.5f;
                this.hair.field_78798_e = -head.field_78795_f * 1.5f;
            }
        } else {
            this.hair.field_78795_f = 0.0f;
            this.hair.field_78797_d = 0.0f;
            this.hair.field_78798_e = 0.0f;
            this.beard.field_78795_f = -head.field_78795_f;
        }
    }
}

