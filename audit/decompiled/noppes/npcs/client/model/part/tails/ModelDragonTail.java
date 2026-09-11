/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.model.ModelBiped
 *  net.minecraft.client.model.ModelRenderer
 *  net.minecraft.entity.Entity
 */
package noppes.npcs.client.model.part.tails;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import noppes.npcs.client.model.ModelPlaneRenderer;

public class ModelDragonTail
extends ModelRenderer {
    public ModelDragonTail(ModelBiped base) {
        super((ModelBase)base);
        int x = 52;
        int y = 16;
        ModelRenderer dragon = new ModelRenderer((ModelBase)base, x, y);
        dragon.func_78793_a(0.0f, 0.0f, 3.0f);
        this.func_78792_a(dragon);
        ModelRenderer DragonTail2 = new ModelRenderer((ModelBase)base, x, y);
        DragonTail2.func_78793_a(0.0f, 2.0f, 2.0f);
        ModelRenderer DragonTail3 = new ModelRenderer((ModelBase)base, x, y);
        DragonTail3.func_78793_a(0.0f, 4.5f, 4.0f);
        ModelRenderer DragonTail4 = new ModelRenderer((ModelBase)base, x, y);
        DragonTail4.func_78793_a(0.0f, 7.0f, 5.75f);
        ModelRenderer DragonTail5 = new ModelRenderer((ModelBase)base, x, y);
        DragonTail5.func_78793_a(0.0f, 9.0f, 8.0f);
        ModelPlaneRenderer planeLeft = new ModelPlaneRenderer((ModelBase)base, x, y);
        planeLeft.addSidePlane(-1.5f, -1.5f, -1.5f, 3, 3);
        ModelPlaneRenderer planeRight = new ModelPlaneRenderer((ModelBase)base, x, y);
        planeRight.addSidePlane(-1.5f, -1.5f, -1.5f, 3, 3);
        this.setRotation(planeRight, (float)Math.PI, (float)Math.PI, 0.0f);
        ModelPlaneRenderer planeTop = new ModelPlaneRenderer((ModelBase)base, x, y);
        planeTop.addTopPlane(-1.5f, -1.5f, -1.5f, 3, 3);
        this.setRotation(planeTop, 0.0f, -1.5707964f, 0.0f);
        ModelPlaneRenderer planeBottom = new ModelPlaneRenderer((ModelBase)base, x, y);
        planeBottom.addTopPlane(-1.5f, -1.5f, -1.5f, 3, 3);
        this.setRotation(planeBottom, 0.0f, -1.5707964f, (float)Math.PI);
        ModelPlaneRenderer planeBack = new ModelPlaneRenderer((ModelBase)base, x, y);
        planeBack.addBackPlane(-1.5f, -1.5f, -1.5f, 3, 3);
        this.setRotation(planeBack, 0.0f, 0.0f, 1.5707964f);
        ModelPlaneRenderer planeFront = new ModelPlaneRenderer((ModelBase)base, x, y);
        planeFront.addBackPlane(-1.5f, -1.5f, -1.5f, 3, 3);
        this.setRotation(planeFront, 0.0f, (float)Math.PI, -1.5707964f);
        dragon.func_78792_a((ModelRenderer)planeLeft);
        dragon.func_78792_a((ModelRenderer)planeRight);
        dragon.func_78792_a((ModelRenderer)planeTop);
        dragon.func_78792_a((ModelRenderer)planeBottom);
        dragon.func_78792_a((ModelRenderer)planeFront);
        dragon.func_78792_a((ModelRenderer)planeBack);
        DragonTail2.func_78792_a((ModelRenderer)planeLeft);
        DragonTail2.func_78792_a((ModelRenderer)planeRight);
        DragonTail2.func_78792_a((ModelRenderer)planeTop);
        DragonTail2.func_78792_a((ModelRenderer)planeBottom);
        DragonTail2.func_78792_a((ModelRenderer)planeFront);
        DragonTail2.func_78792_a((ModelRenderer)planeBack);
        DragonTail3.func_78792_a((ModelRenderer)planeLeft);
        DragonTail3.func_78792_a((ModelRenderer)planeRight);
        DragonTail3.func_78792_a((ModelRenderer)planeTop);
        DragonTail3.func_78792_a((ModelRenderer)planeBottom);
        DragonTail3.func_78792_a((ModelRenderer)planeFront);
        DragonTail3.func_78792_a((ModelRenderer)planeBack);
        DragonTail4.func_78792_a((ModelRenderer)planeLeft);
        DragonTail4.func_78792_a((ModelRenderer)planeRight);
        DragonTail4.func_78792_a((ModelRenderer)planeTop);
        DragonTail4.func_78792_a((ModelRenderer)planeBottom);
        DragonTail4.func_78792_a((ModelRenderer)planeFront);
        DragonTail4.func_78792_a((ModelRenderer)planeBack);
        dragon.func_78792_a(DragonTail2);
        dragon.func_78792_a(DragonTail3);
        dragon.func_78792_a(DragonTail4);
    }

    public void setRotationAngles(float par1, float par2, float par3, float par4, float par5, float par6, Entity entity) {
    }

    private void setRotation(ModelRenderer model, float x, float y, float z) {
        model.field_78795_f = x;
        model.field_78796_g = y;
        model.field_78808_h = z;
    }
}

