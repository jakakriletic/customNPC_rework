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
import noppes.npcs.rework.render.HeadwearSkip;
import noppes.npcs.rework.render.Lod;

public class LayerHeadwear
extends LayerInterface
implements LayerPreRender {
    private ModelHeadwear headwear;
    private boolean rwFar;

    public LayerHeadwear(RenderLiving render) {
        super(render);
        this.headwear = new ModelHeadwear((ModelBase)this.model);
    }

    @Override
    public void render(float par2, float par3, float par4, float par5, float par6, float par7) {
        // M5-K K5: na stopnji L1 je pokrivalo plosko v modelu (preRender).
        if (CustomNpcs.HeadWearType != 1 || this.rwFar) {
            return;
        }
        if (this.npc.hurtTime <= 0 && this.npc.deathTime <= 0) {
            int color = this.npc.display.getTint();
            float red = (float)(color >> 16 & 0xFF) / 255.0f;
            float green = (float)(color >> 8 & 0xFF) / 255.0f;
            float blue = (float)(color & 0xFF) / 255.0f;
            GlStateManager.color((float)red, (float)green, (float)blue, (float)1.0f);
        }
        ClientProxy.bindTexture(this.npc.textureLocation);
        this.model.bipedHead.postRender(par7);
        // M5-K K14b: brez stirikotnikov, ki vzorcijo samo prozorne teksle.
        if (CustomNpcs.RwHeadwearSkip == 0 || !HeadwearSkip.render(this.headwear, this.npc.textureLocation, par7)) {
            this.headwear.render(par7);
        }
    }

    @Override
    public void rotate(float par2, float par3, float par4, float par5, float par6, float par7) {
    }

    @Override
    public void preRender(EntityCustomNpc player) {
        // M5-K K5: dlje od RwLodStart plosko pokrivalo modela (videz HeadWearType=0) namesto 3D.
        this.rwFar = Lod.farCounted(player);
        this.model.bipedHeadwear.isHidden = CustomNpcs.HeadWearType == 1 && !this.rwFar;
        this.headwear.config = null;
    }
}

