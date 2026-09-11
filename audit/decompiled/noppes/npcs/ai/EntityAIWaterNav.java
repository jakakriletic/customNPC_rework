/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.ai.EntityAIBase
 *  net.minecraft.pathfinding.PathNavigateGround
 */
package noppes.npcs.ai;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.pathfinding.PathNavigateGround;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityAIWaterNav
extends EntityAIBase {
    private EntityNPCInterface entity;

    public EntityAIWaterNav(EntityNPCInterface iNpc) {
        this.entity = iNpc;
        ((PathNavigateGround)iNpc.func_70661_as()).func_179693_d(true);
    }

    public boolean func_75250_a() {
        if (this.entity.func_70090_H() || this.entity.func_180799_ab()) {
            if (this.entity.ais.canSwim) {
                return true;
            }
            return this.entity.field_70123_F;
        }
        return false;
    }

    public void func_75246_d() {
        if (this.entity.func_70681_au().nextFloat() < 0.8f) {
            this.entity.func_70683_ar().func_75660_a();
        }
    }
}

