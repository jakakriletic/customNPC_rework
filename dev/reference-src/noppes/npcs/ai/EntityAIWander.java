/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Predicate
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityCreature
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.ai.EntityAIBase
 *  net.minecraft.entity.ai.RandomPositionGenerator
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.util.math.Vec3d
 */
package noppes.npcs.ai;

import com.google.common.base.Predicate;
import java.util.Iterator;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import noppes.npcs.CustomNpcs;
import noppes.npcs.ai.selector.NPCInteractSelector;
import noppes.npcs.constants.AiMutex;
import noppes.npcs.controllers.data.Line;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityAIWander
extends EntityAIBase {
    private EntityNPCInterface entity;
    public final NPCInteractSelector selector;
    private double x;
    private double y;
    private double zPosition;
    private EntityNPCInterface nearbyNPC;

    public EntityAIWander(EntityNPCInterface npc) {
        this.entity = npc;
        this.setMutexBits(AiMutex.PASSIVE);
        this.selector = new NPCInteractSelector(npc);
    }

    public boolean shouldExecute() {
        if (this.entity.getIdleTime() >= 100 || !this.entity.getNavigator().noPath() || this.entity.isInteracting() || this.entity.isRiding() || this.entity.ais.movingPause && this.entity.getRNG().nextInt(80) != 0) {
            return false;
        }
        if (this.entity.ais.npcInteracting && this.entity.getRNG().nextInt(this.entity.ais.movingPause ? 6 : 16) == 1) {
            this.nearbyNPC = this.getNearbyNPC();
        }
        if (this.nearbyNPC != null) {
            this.x = MathHelper.floor((double)this.nearbyNPC.posX);
            this.y = MathHelper.floor((double)this.nearbyNPC.posY);
            this.zPosition = MathHelper.floor((double)this.nearbyNPC.posZ);
            this.nearbyNPC.addInteract((EntityLivingBase)this.entity);
        } else {
            Vec3d vec = this.getVec();
            if (vec == null) {
                return false;
            }
            this.x = vec.x;
            this.y = vec.y;
            if (this.entity.ais.movementType == 1) {
                this.y = this.entity.getStartYPos() + (double)this.entity.getRNG().nextFloat() * 0.75 * (double)this.entity.ais.walkingRange;
            }
            this.zPosition = vec.z;
        }
        return true;
    }

    public void updateTask() {
        if (this.nearbyNPC != null) {
            this.nearbyNPC.getNavigator().clearPath();
        }
    }

    private EntityNPCInterface getNearbyNPC() {
        List list = this.entity.world.getEntitiesInAABBexcluding((Entity)this.entity, this.entity.getEntityBoundingBox().grow((double)this.entity.ais.walkingRange, this.entity.ais.walkingRange > 7 ? 7.0 : (double)this.entity.ais.walkingRange, (double)this.entity.ais.walkingRange), (Predicate)this.selector);
        Iterator ita = list.iterator();
        while (ita.hasNext()) {
            EntityNPCInterface npc = (EntityNPCInterface)((Object)ita.next());
            if (npc.ais.stopAndInteract && !npc.isAttacking() && npc.isEntityAlive() && !this.entity.faction.isAggressiveToNpc(npc)) continue;
            ita.remove();
        }
        if (list.isEmpty()) {
            return null;
        }
        return (EntityNPCInterface)((Object)list.get(this.entity.getRNG().nextInt(list.size())));
    }

    private Vec3d getVec() {
        if (this.entity.ais.walkingRange > 0) {
            BlockPos start = new BlockPos((double)this.entity.getStartXPos(), this.entity.getStartYPos(), (double)this.entity.getStartZPos());
            int distance = (int)MathHelper.sqrt((double)this.entity.getDistanceSq(start));
            int range = this.entity.ais.walkingRange - distance;
            if (range > CustomNpcs.NpcNavRange) {
                range = CustomNpcs.NpcNavRange;
            }
            if (range < 3) {
                range = this.entity.ais.walkingRange;
                if (range > CustomNpcs.NpcNavRange) {
                    range = CustomNpcs.NpcNavRange;
                }
                Vec3d pos2 = new Vec3d((this.entity.posX + (double)start.getX()) / 2.0, (this.entity.posY + (double)start.getY()) / 2.0, (this.entity.posZ + (double)start.getZ()) / 2.0);
                return RandomPositionGenerator.findRandomTargetBlockTowards((EntityCreature)this.entity, (int)(distance / 2), (int)(distance / 2 > 7 ? 7 : distance / 2), (Vec3d)pos2);
            }
            return RandomPositionGenerator.findRandomTarget((EntityCreature)this.entity, (int)(range / 2), (int)(range / 2 > 7 ? 7 : range / 2));
        }
        return RandomPositionGenerator.findRandomTarget((EntityCreature)this.entity, (int)CustomNpcs.NpcNavRange, (int)7);
    }

    public boolean shouldContinueExecuting() {
        if (this.nearbyNPC != null && (!this.selector.apply((Object)this.nearbyNPC) || this.entity.isInRange((Entity)this.nearbyNPC, this.entity.width))) {
            return false;
        }
        return !this.entity.getNavigator().noPath() && this.entity.isEntityAlive() && !this.entity.isInteracting();
    }

    public void startExecuting() {
        this.entity.getNavigator().tryMoveToXYZ(this.x, this.y, this.zPosition, 1.0);
    }

    public void resetTask() {
        if (this.nearbyNPC != null && this.entity.isInRange((Entity)this.nearbyNPC, 3.5)) {
            Line line;
            EntityNPCInterface talk = this.entity;
            if (this.entity.getRNG().nextBoolean()) {
                talk = this.nearbyNPC;
            }
            if ((line = talk.advanced.getNPCInteractLine()) == null) {
                line = new Line(".........");
            }
            line.hideText = true;
            talk.saySurrounding(line);
            this.entity.addInteract((EntityLivingBase)this.nearbyNPC);
            this.nearbyNPC.addInteract((EntityLivingBase)this.entity);
        }
        this.nearbyNPC = null;
    }
}

