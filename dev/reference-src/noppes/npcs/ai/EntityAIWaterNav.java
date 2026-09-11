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
        ((PathNavigateGround)iNpc.getNavigator()).setCanSwim(true);
    }

    public boolean shouldExecute() {
        if (this.entity.isInWater() || this.entity.isInLava()) {
            if (this.entity.ais.canSwim) {
                return true;
            }
            return this.entity.collidedHorizontally;
        }
        return false;
    }

    public void updateTask() {
        if (this.entity.getRNG().nextFloat() < 0.8f) {
            this.entity.getJumpHelper().setJumping();
        }
    }
}

