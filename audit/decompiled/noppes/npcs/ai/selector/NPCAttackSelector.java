/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Predicate
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.init.MobEffects
 */
package noppes.npcs.ai.selector;

import com.google.common.base.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import noppes.npcs.constants.EnumCompanionJobs;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.JobGuard;
import noppes.npcs.roles.RoleCompanion;
import noppes.npcs.roles.companion.CompanionGuard;

public class NPCAttackSelector
implements Predicate<EntityLivingBase> {
    private EntityNPCInterface npc;

    public NPCAttackSelector(EntityNPCInterface npc) {
        this.npc = npc;
    }

    public boolean isEntityApplicable(EntityLivingBase entity) {
        if (!entity.func_70089_S() || entity == this.npc || !this.npc.isInRange((Entity)entity, this.npc.stats.aggroRange) || entity.func_110143_aJ() < 1.0f) {
            return false;
        }
        if (this.npc.ais.directLOS && !this.npc.func_70635_at().func_75522_a((Entity)entity)) {
            return false;
        }
        if (!this.npc.ais.attackInvisible && entity.func_70644_a(MobEffects.field_76441_p) && !this.npc.isInRange((Entity)entity, 3.0)) {
            return false;
        }
        if (!this.npc.isFollower() && this.npc.ais.shouldReturnHome()) {
            int allowedDistance = this.npc.stats.aggroRange * 2;
            if (this.npc.ais.getMovingType() == 1) {
                allowedDistance += this.npc.ais.walkingRange;
            }
            double distance = entity.func_70092_e((double)this.npc.getStartXPos(), this.npc.getStartYPos(), (double)this.npc.getStartZPos());
            if (this.npc.ais.getMovingType() == 2) {
                int[] arr = this.npc.ais.getCurrentMovingPath();
                distance = entity.func_70092_e((double)arr[0], (double)arr[1], (double)arr[2]);
            }
            if (distance > (double)(allowedDistance * allowedDistance)) {
                return false;
            }
        }
        if (this.npc.advanced.job == 3 && ((JobGuard)this.npc.jobInterface).isEntityApplicable((Entity)entity)) {
            return true;
        }
        if (this.npc.advanced.role == 6) {
            RoleCompanion role = (RoleCompanion)this.npc.roleInterface;
            if (role.job == EnumCompanionJobs.GUARD && ((CompanionGuard)role.jobInterface).isEntityApplicable((Entity)entity)) {
                return true;
            }
        }
        if (entity instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP)entity;
            return this.npc.faction.isAggressiveToPlayer((EntityPlayer)player) && !player.field_71075_bZ.field_75102_a;
        }
        if (entity instanceof EntityNPCInterface) {
            if (((EntityNPCInterface)entity).isKilled()) {
                return false;
            }
            if (this.npc.advanced.attackOtherFactions) {
                return this.npc.faction.isAggressiveToNpc((EntityNPCInterface)entity);
            }
        }
        return false;
    }

    public boolean apply(EntityLivingBase ob) {
        return this.isEntityApplicable(ob);
    }
}

