/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.boss.EntityDragon
 *  net.minecraft.entity.passive.EntityChicken
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.EntityEquipmentSlot
 */
package noppes.npcs.client;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityUtil {
    public static void Copy(EntityLivingBase copied, EntityLivingBase entity) {
        EntityNPCInterface npc;
        entity.world = copied.world;
        entity.deathTime = copied.deathTime;
        entity.distanceWalkedModified = copied.distanceWalkedModified;
        entity.prevDistanceWalkedModified = copied.distanceWalkedModified;
        entity.onGround = copied.onGround;
        entity.distanceWalkedOnStepModified = copied.distanceWalkedOnStepModified;
        entity.moveForward = copied.moveForward;
        entity.moveStrafing = copied.moveStrafing;
        entity.setPosition(copied.posX, copied.posY, copied.posZ);
        entity.setEntityBoundingBox(copied.getEntityBoundingBox());
        entity.prevPosX = copied.prevPosX;
        entity.prevPosY = copied.prevPosY;
        entity.prevPosZ = copied.prevPosZ;
        entity.motionX = copied.motionX;
        entity.motionY = copied.motionY;
        entity.motionZ = copied.motionZ;
        entity.rotationYaw = copied.rotationYaw;
        entity.prevRotationYaw = copied.prevRotationYaw;
        entity.rotationPitch = copied.rotationPitch;
        entity.prevRotationPitch = copied.prevRotationPitch;
        entity.rotationYawHead = copied.rotationYawHead;
        entity.prevRotationYawHead = copied.prevRotationYawHead;
        entity.renderYawOffset = copied.renderYawOffset;
        entity.prevRenderYawOffset = copied.prevRenderYawOffset;
        entity.cameraPitch = copied.cameraPitch;
        entity.prevCameraPitch = copied.prevCameraPitch;
        entity.lastTickPosX = copied.lastTickPosX;
        entity.lastTickPosY = copied.lastTickPosY;
        entity.lastTickPosZ = copied.lastTickPosZ;
        entity.limbSwingAmount = copied.limbSwingAmount;
        entity.prevLimbSwingAmount = copied.prevLimbSwingAmount;
        entity.limbSwing = copied.limbSwing;
        entity.swingProgress = copied.swingProgress;
        entity.prevSwingProgress = copied.prevSwingProgress;
        entity.isSwingInProgress = copied.isSwingInProgress;
        entity.swingProgressInt = copied.swingProgressInt;
        entity.setHealth(Math.min(copied.getHealth(), entity.getMaxHealth()));
        entity.isDead = copied.isDead;
        entity.deathTime = copied.deathTime;
        entity.ticksExisted = copied.ticksExisted;
        entity.getEntityData().merge(copied.getEntityData());
        if (entity instanceof EntityPlayer && copied instanceof EntityPlayer) {
            EntityEquipmentSlot[] ePlayer = (EntityEquipmentSlot[])entity;
            EntityPlayer cPlayer = (EntityPlayer)copied;
            ePlayer.cameraYaw = cPlayer.cameraYaw;
            ePlayer.prevCameraYaw = cPlayer.prevCameraYaw;
            ePlayer.prevChasingPosX = cPlayer.prevChasingPosX;
            ePlayer.prevChasingPosY = cPlayer.prevChasingPosY;
            ePlayer.prevChasingPosZ = cPlayer.prevChasingPosZ;
            ePlayer.chasingPosX = cPlayer.chasingPosX;
            ePlayer.chasingPosY = cPlayer.chasingPosY;
            ePlayer.chasingPosZ = cPlayer.chasingPosZ;
        }
        if (entity instanceof EntityDragon) {
            entity.rotationYaw += 180.0f;
        }
        if (entity instanceof EntityChicken) {
            ((EntityChicken)entity).destPos = copied.onGround ? 0.0f : 1.0f;
        }
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            entity.setItemStackToSlot(slot, copied.getItemStackFromSlot(slot));
        }
        if (copied instanceof EntityNPCInterface && entity instanceof EntityNPCInterface) {
            npc = (EntityNPCInterface)copied;
            EntityNPCInterface target = (EntityNPCInterface)entity;
            target.textureLocation = npc.textureLocation;
            target.textureGlowLocation = npc.textureGlowLocation;
            target.textureCloakLocation = npc.textureCloakLocation;
            target.display = npc.display;
            target.inventory = npc.inventory;
            target.currentAnimation = npc.currentAnimation;
            target.setDataWatcher(npc.getDataManager());
        }
        if (entity instanceof EntityCustomNpc && copied instanceof EntityCustomNpc) {
            npc = (EntityCustomNpc)copied;
            EntityCustomNpc target = (EntityCustomNpc)entity;
            target.modelData = ((EntityCustomNpc)npc).modelData.copy();
            target.modelData.setEntityClass(null);
        }
    }

    public static void setRecentlyHit(EntityLivingBase entity) {
        entity.recentlyHit = 100;
    }
}

