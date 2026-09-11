/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.renderer.GlStateManager
 */
package noppes.npcs.client.renderer;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import noppes.npcs.client.renderer.RenderNPCInterface;
import noppes.npcs.entity.EntityNPCInterface;

public class RenderNpcDragon<T extends EntityNPCInterface>
extends RenderNPCInterface<T> {
    public RenderNpcDragon(ModelBase model, float f) {
        super(model, f);
    }

    @Override
    protected void preRenderCallback(T npc, float f) {
        GlStateManager.func_179109_b((float)0.0f, (float)0.0f, (float)(0.120000005f * (float)((EntityNPCInterface)((Object)npc)).display.getSize()));
        super.preRenderCallback(npc, f);
    }
}

