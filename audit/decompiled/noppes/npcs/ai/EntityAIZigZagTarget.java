/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityCreature
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.ai.EntityAIBase
 *  net.minecraft.entity.ai.RandomPositionGenerator
 *  net.minecraft.pathfinding.Path
 *  net.minecraft.pathfinding.PathPoint
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.util.math.Vec3d
 */
package noppes.npcs.ai;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import noppes.npcs.constants.AiMutex;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityAIZigZagTarget
extends EntityAIBase {
    private EntityNPCInterface npc;
    private EntityLivingBase targetEntity;
    private Path pathentity;
    private double movePosX;
    private double movePosY;
    private double movePosZ;
    private int entityPosX;
    private int entityPosY;
    private int entityPosZ;
    private double speed;
    private int ticks;

    public EntityAIZigZagTarget(EntityNPCInterface par1EntityCreature, double par2) {
        this.npc = par1EntityCreature;
        this.speed = par2;
        this.ticks = 0;
        this.func_75248_a(AiMutex.PASSIVE + AiMutex.LOOK);
    }

    public boolean func_75250_a() {
        this.targetEntity = this.npc.func_70638_az();
        if (this.targetEntity == null || !this.targetEntity.func_70089_S()) {
            return false;
        }
        return !this.npc.isInRange((Entity)this.targetEntity, this.npc.ais.getTacticalRange());
    }

    public void func_75251_c() {
        this.npc.func_70661_as().func_75499_g();
        this.ticks = 0;
    }

    public void func_75246_d() {
        this.npc.func_70671_ap().func_75651_a((Entity)this.targetEntity, 30.0f, 30.0f);
        if (this.ticks-- <= 0) {
            Path pathentity = this.npc.func_70661_as().func_75494_a((Entity)this.targetEntity);
            if (pathentity != null && pathentity.func_75874_d() >= this.npc.ais.getTacticalRange()) {
                PathPoint pathpoint = pathentity.func_75877_a(MathHelper.func_76128_c((double)((double)this.npc.ais.getTacticalRange() / 2.0)));
                this.entityPosX = pathpoint.field_75839_a;
                this.entityPosY = pathpoint.field_75837_b;
                this.entityPosZ = pathpoint.field_75838_c;
                Vec3d vec3 = RandomPositionGenerator.func_75464_a((EntityCreature)this.npc, (int)this.npc.ais.getTacticalRange(), (int)3, (Vec3d)new Vec3d((double)this.entityPosX, (double)this.entityPosY, (double)this.entityPosZ));
                if (vec3 != null) {
                    if (this.targetEntity.func_70092_e(vec3.field_72450_a, vec3.field_72448_b, vec3.field_72449_c) < this.targetEntity.func_70092_e((double)this.entityPosX, (double)this.entityPosY, (double)this.entityPosZ)) {
                        this.movePosX = vec3.field_72450_a;
                        this.movePosY = vec3.field_72448_b;
                        this.movePosZ = vec3.field_72449_c;
                    }
                } else {
                    this.movePosX = pathpoint.field_75839_a;
                    this.movePosY = pathpoint.field_75837_b;
                    this.movePosZ = pathpoint.field_75838_c;
                }
                this.npc.func_70661_as().func_75492_a(this.movePosX, this.movePosY, this.movePosZ, this.speed);
            } else {
                this.ticks = 10;
            }
        }
    }
}

