/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.EntityCreature
 *  net.minecraft.entity.ai.EntityAIBase
 *  net.minecraft.entity.ai.RandomPositionGenerator
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Vec3d
 */
package noppes.npcs.ai;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import noppes.npcs.CustomNpcs;
import noppes.npcs.constants.AiMutex;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityAIReturn
extends EntityAIBase {
    public static final int MaxTotalTicks = 600;
    private final EntityNPCInterface npc;
    private int stuckTicks = 0;
    private int totalTicks = 0;
    private double endPosX;
    private double endPosY;
    private double endPosZ;
    private boolean wasAttacked = false;
    private double[] preAttackPos;
    private int stuckCount = 0;

    public EntityAIReturn(EntityNPCInterface npc) {
        this.npc = npc;
        this.setMutexBits(AiMutex.PASSIVE);
    }

    public boolean shouldExecute() {
        BlockPos pos;
        if (this.npc.hasOwner() || this.npc.isRiding() || !this.npc.ais.shouldReturnHome() || this.npc.isKilled() || !this.npc.getNavigator().noPath() || this.npc.isInteracting()) {
            return false;
        }
        if (this.npc.ais.findShelter == 0 && (!this.npc.world.isDaytime() || this.npc.world.isRaining()) && !this.npc.world.provider.hasSkyLight() ? this.npc.world.canSeeSky(pos = new BlockPos((double)this.npc.getStartXPos(), this.npc.getStartYPos(), (double)this.npc.getStartZPos())) || this.npc.world.getLight(pos) <= 8 : this.npc.ais.findShelter == 1 && this.npc.world.isDaytime() && this.npc.world.canSeeSky(pos = new BlockPos((double)this.npc.getStartXPos(), this.npc.getStartYPos(), (double)this.npc.getStartZPos()))) {
            return false;
        }
        if (this.npc.isAttacking()) {
            if (!this.wasAttacked) {
                this.wasAttacked = true;
                this.preAttackPos = new double[]{this.npc.posX, this.npc.posY, this.npc.posZ};
            }
            return false;
        }
        if (!this.npc.isAttacking() && this.wasAttacked) {
            return true;
        }
        if (this.npc.ais.getMovingType() == 2 && this.npc.ais.getDistanceSqToPathPoint() < (double)(CustomNpcs.NpcNavRange * CustomNpcs.NpcNavRange)) {
            return false;
        }
        if (this.npc.ais.getMovingType() == 1) {
            double x = this.npc.posX - (double)this.npc.getStartXPos();
            double z = this.npc.posX - (double)this.npc.getStartZPos();
            return !this.npc.isInRange(this.npc.getStartXPos(), -1.0, this.npc.getStartZPos(), this.npc.ais.walkingRange);
        }
        if (this.npc.ais.getMovingType() == 0) {
            return !this.npc.isVeryNearAssignedPlace();
        }
        return false;
    }

    public boolean shouldContinueExecuting() {
        if (this.npc.isFollower() || this.npc.isKilled() || this.npc.isAttacking() || this.npc.isVeryNearAssignedPlace() || this.npc.isInteracting() || this.npc.isRiding()) {
            return false;
        }
        if (this.npc.getNavigator().noPath() && this.wasAttacked && !this.isTooFar()) {
            return false;
        }
        return this.totalTicks <= 600;
    }

    public void updateTask() {
        ++this.totalTicks;
        if (this.totalTicks > 600) {
            this.npc.setPosition(this.endPosX, this.endPosY, this.endPosZ);
            this.npc.getNavigator().clearPath();
            return;
        }
        if (this.stuckTicks > 0) {
            --this.stuckTicks;
        } else if (this.npc.getNavigator().noPath()) {
            ++this.stuckCount;
            this.stuckTicks = 10;
            if (this.totalTicks > 30 && this.wasAttacked && this.isTooFar() || this.stuckCount > 5) {
                this.npc.setPosition(this.endPosX, this.endPosY, this.endPosZ);
                this.npc.getNavigator().clearPath();
            } else {
                this.navigate(this.stuckCount % 2 == 1);
            }
        } else {
            this.stuckCount = 0;
        }
    }

    private boolean isTooFar() {
        double z;
        double x;
        int allowedDistance = this.npc.stats.aggroRange * 2;
        if (this.npc.ais.getMovingType() == 1) {
            allowedDistance += this.npc.ais.walkingRange;
        }
        return (x = this.npc.posX - this.endPosX) * x + (z = this.npc.posX - this.endPosZ) * z > (double)(allowedDistance * allowedDistance);
    }

    public void startExecuting() {
        this.stuckTicks = 0;
        this.totalTicks = 0;
        this.stuckCount = 0;
        this.navigate(false);
    }

    private void navigate(boolean towards) {
        if (!this.wasAttacked) {
            this.endPosX = this.npc.getStartXPos();
            this.endPosY = this.npc.getStartYPos();
            this.endPosZ = this.npc.getStartZPos();
        } else {
            this.endPosX = this.preAttackPos[0];
            this.endPosY = this.preAttackPos[1];
            this.endPosZ = this.preAttackPos[2];
        }
        double posX = this.endPosX;
        double posY = this.endPosY;
        double posZ = this.endPosZ;
        double range = this.npc.getDistance(posX, posY, posZ);
        if (range > (double)CustomNpcs.NpcNavRange || towards) {
            Vec3d start;
            Vec3d pos;
            int distance = (int)range;
            distance = distance > CustomNpcs.NpcNavRange ? CustomNpcs.NpcNavRange / 2 : (distance /= 2);
            if (distance > 2 && (pos = RandomPositionGenerator.findRandomTargetBlockTowards((EntityCreature)this.npc, (int)distance, (int)(distance / 2 > 7 ? 7 : distance / 2), (Vec3d)(start = new Vec3d(posX, posY, posZ)))) != null) {
                posX = pos.x;
                posY = pos.y;
                posZ = pos.z;
            }
        }
        this.npc.getNavigator().clearPath();
        this.npc.getNavigator().tryMoveToXYZ(posX, posY, posZ, 1.0);
    }

    public void resetTask() {
        this.wasAttacked = false;
        this.npc.getNavigator().clearPath();
    }
}

