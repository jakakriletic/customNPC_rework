package noppes.npcs.rework.rig;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * M16: built-in server handling of clip events, so an attack can be pure data (as in customnpcentities):
 * <ul>
 * <li>{@code hit}: area damage {@code damage} x the NPC's melee strength; {@code shape} circle|arc,
 *   {@code forward}/{@code side} (blocks, NPC frame), {@code radius}, {@code arc} (degrees), {@code dir},
 *   {@code falloff}, {@code knockback} (blocks), {@code lift}, {@code effect} + {@code effect_seconds} +
 *   {@code effect_level}, {@code fire} (seconds). Only enemies are hit (CustomNPCs factions).</li>
 * <li>{@code sound}: {@code sound} (one id or a list, e.g. {@code minecraft:entity.ravager.roar}),
 *   {@code volume}, {@code pitch}.</li>
 * <li>{@code lunge}: {@code forward} (blocks per tick), {@code up} (jump speed).</li>
 * <li>{@code heal}: {@code amount} (health points) to the NPC itself.</li>
 * </ul>
 * Any other type does nothing here; scripts get every event through {@code animationEvent}.
 */
public final class RigEvents {
    private RigEvents() {
    }

    static void server(EntityNPCInterface npc, AnimEvent e) {
        switch (e.type) {
            case "hit": hit(npc, e); break;
            case "sound": for (String s : e.getStrings("sound")) { sound(npc, s, e.getFloat("volume", 1), e.getFloat("pitch", 1)); } break;
            case "lunge": lunge(npc, e); break;
            case "heal": npc.heal(e.getFloat("amount", 0)); break;
            default: break;
        }
    }

    /** World X/Z of a point in the NPC's frame (blocks forward, blocks to its right). */
    static double[] local(EntityNPCInterface npc, double forward, double side) {
        double yaw = npc.rotationYaw * 0.017453292F;
        double fx = -Math.sin(yaw), fz = Math.cos(yaw), rx = -Math.cos(yaw), rz = -Math.sin(yaw);
        return new double[] {npc.posX + fx * forward + rx * side, npc.posZ + fz * forward + rz * side};
    }

    /** @return entities hit */
    static int hit(EntityNPCInterface npc, AnimEvent e) {
        String shape = e.getString("shape", "circle");
        double radius = e.getFloat("radius", 2);
        double[] c = local(npc, e.getFloat("forward", 1), e.getFloat("side", 0));
        float damage = e.getFloat("damage", 1) * Math.max(1, npc.stats.melee.getStrength());
        float falloff = e.getFloat("falloff", 1);
        float knock = e.getFloat("knockback", 1);
        float lift = e.getFloat("lift", 0.3F);
        double yaw = npc.rotationYaw * 0.017453292F, dir = Math.toRadians(e.getFloat("dir", 0));
        double fx = -Math.sin(yaw), fz = Math.cos(yaw), rx = -Math.cos(yaw), rz = -Math.sin(yaw);
        double ax = fx * Math.cos(dir) + rx * Math.sin(dir), az = fz * Math.cos(dir) + rz * Math.sin(dir);
        double halfArc = Math.toRadians(e.getFloat("arc", 120)) / 2;
        float below = e.getFloat("below", 1.5F), above = e.getFloat("above", npc.height);
        AxisAlignedBB box = new AxisAlignedBB(c[0] - radius - 2, npc.posY - below, c[1] - radius - 2,
                c[0] + radius + 2, npc.posY + above, c[1] + radius + 2);
        List<EntityLivingBase> victims = new ArrayList<EntityLivingBase>(npc.world.getEntitiesWithinAABB(EntityLivingBase.class, box));
        int count = 0;
        for (EntityLivingBase victim : victims) {
            if (!canHit(npc, victim)) { continue; }
            double dx = victim.posX - c[0], dz = victim.posZ - c[1], dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > radius + victim.width / 2) { continue; }
            if (shape.equals("arc") && dist > 0.5) {
                double cos = (dx * ax + dz * az) / dist;
                if (Math.acos(MathHelper.clamp(cos, -1, 1)) > halfArc + Math.atan2(victim.width / 2, dist)) { continue; }
            }
            boolean landed = true;
            if (damage > 0) {
                float scale = 1 - (1 - falloff) * (float) MathHelper.clamp(dist / radius, 0, 1);
                landed = victim.attackEntityFrom(DamageSource.causeMobDamage(npc), damage * scale);
            }
            if (!landed) { continue; }
            count++;
            if (knock > 0) { launch(npc, victim, c, dist, knock, lift); }
            String effect = e.getString("effect", null);
            if (effect != null) {
                Potion p = Potion.REGISTRY.getObject(new ResourceLocation(effect));
                if (p != null) { victim.addPotionEffect(new PotionEffect(p, (int) (e.getFloat("effect_seconds", 3) * 20), Math.max(0, (int) e.getFloat("effect_level", 1) - 1))); }
            }
            int fire = (int) e.getFloat("fire", 0);
            if (fire > 0) { victim.setFire(fire); }
        }
        return count;
    }

    static void launch(EntityNPCInterface npc, EntityLivingBase victim, double[] from, double dist, float blocks, float lift) {
        double dx = victim.posX - from[0], dz = victim.posZ - from[1];
        if (dist < 0.3) { dx = victim.posX - npc.posX; dz = victim.posZ - npc.posZ; }
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-3) { dx = -MathHelper.sin(npc.rotationYaw * 0.017453292F); dz = MathHelper.cos(npc.rotationYaw * 0.017453292F); len = 1; }
        double resist = victim.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).getAttributeValue();
        double speed = blocks / slideFactor(lift) * (1 - MathHelper.clamp(resist, 0, 1));
        victim.motionX = dx / len * speed;
        victim.motionZ = dz / len * speed;
        victim.motionY = Math.max(victim.motionY, lift);
        victim.isAirBorne = true;
        victim.velocityChanged = true;
    }

    /** Blocks travelled per 1.0 horizontal speed for a hop of initial vertical speed lift. */
    static double slideFactor(double lift) {
        double vy = lift, y = 0, total = 0, keep = 1;
        for (int ticks = 0; ticks < 60; ticks++) {
            total += keep; keep *= 0.91; y += vy; vy = (vy - 0.08) * 0.98;
            if (y <= 0) { break; }
        }
        return total + keep / (1 - 0.546);
    }

    /** Enemies only: CustomNPCs factions, the NPC's target, hostile mobs. */
    static boolean canHit(EntityNPCInterface npc, EntityLivingBase victim) {
        if (victim == npc || !victim.isEntityAlive() || victim == npc.getRidingEntity() || npc.isPassenger(victim)) { return false; }
        if (victim instanceof EntityNPCInterface) {
            EntityNPCInterface other = (EntityNPCInterface) victim;
            return npc.faction != null && other.faction != null && npc.faction.id != other.faction.id
                    && (npc.faction.isAggressiveToNpc(other) || victim == npc.getAttackTarget());
        }
        if (victim instanceof EntityPlayer) {
            EntityPlayer p = (EntityPlayer) victim;
            if (p.isCreative() || p.isSpectator()) { return false; }
            return victim == npc.getAttackTarget() || npc.faction != null && npc.faction.isAggressiveToPlayer(p);
        }
        return victim == npc.getAttackTarget() || victim instanceof IMob;
    }

    static void sound(EntityNPCInterface npc, String name, float volume, float pitch) {
        SoundEvent s = RigSounds.get(name);
        if (s == null) { return; }
        npc.world.playSound(null, npc.posX, npc.posY, npc.posZ, s, SoundCategory.HOSTILE, volume,
                pitch * (0.92F + npc.getRNG().nextFloat() * 0.16F));
    }

    static void lunge(EntityNPCInterface npc, AnimEvent e) {
        float speed = e.getFloat("forward", 0.5F);
        double yaw = npc.rotationYaw * 0.017453292F;
        npc.motionX = -Math.sin(yaw) * speed;
        npc.motionZ = Math.cos(yaw) * speed;
        float up = e.getFloat("up", 0);
        if (up > 0) { npc.motionY = up; npc.isAirBorne = true; }
        npc.velocityChanged = true;
    }

}
