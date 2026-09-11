/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.entity.RenderLiving
 */
package noppes.npcs.client.layer;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.ClientProxy;
import noppes.npcs.client.layer.LayerInterface;
import noppes.npcs.client.layer.LayerPreRender;
import noppes.npcs.client.model.part.head.ModelHeadwear;
import noppes.npcs.entity.EntityCustomNpc;

public class LayerHeadwear
extends LayerInterface
implements LayerPreRender {
    private ModelHeadwear headwear;

    public LayerHeadwear(RenderLiving render) {
        super(render);
        this.headwear = new ModelHeadwear((ModelBase)this.model);
    }

    @Override
    public void render(float par2, float par3, float par4, float par5, float par6, float par7) {
        if (CustomNpcs.HeadWearType != 1) {
            return;
        }
        if (this.npc.field_70737_aN <= 0 && this.npc.field_70725_aQ <= 0) {
            int color = this.npc.display.getTint();
            float red = (float)(color >> 16 & 0xFF) / 255.0f;
            float green = (float)(color >> 8 & 0xFF) / 255.0f;
            float blue = (float)(color & 0xFF) / 255.0f;
            GlStateManager.func_179131_c((float)red, (float)green, (float)blue, (float)1.0f);
        }
        ClientProxy.bindTexture(this.npc.textureLocation);
        this.model.field_78116_c.func_78794_c(par7);
        this.headwear.func_78785_a(par7);
    }

    @Override
    public void rotate(float par2, float par3, float par4, float par5, float par6, float par7) {
    }

    @Override
    public void preRender(EntityCustomNpc player) {
        this.model.field_178720_f.field_78807_k = CustomNpcs.HeadWearType == 1;
        this.headwear.config = null;
    }
}

