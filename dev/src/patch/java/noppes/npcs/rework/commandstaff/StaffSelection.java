package noppes.npcs.rework.commandstaff;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.army.ArmyStaffServer;

/** Same circle center for the preview and authoritative selection. */
final class StaffSelection {
    private static final double ENTITY_REACH = 64;

    private StaffSelection() { }

    static Vec3d center(EntityPlayer player) {
        Vec3d eyes = player.getPositionEyes(1);
        Vec3d look = player.getLook(1);
        RayTraceResult ground = ArmyStaffServer.rayHit(player);
        double reach = ground == null ? ENTITY_REACH : Math.min(ENTITY_REACH, eyes.distanceTo(ground.hitVec));
        Vec3d ray = look.scale(reach);
        Vec3d end = eyes.add(ray);
        Entity nearest = null;
        double best = Double.POSITIVE_INFINITY;
        for (EntityNPCInterface npc : player.world.getEntitiesWithinAABB(EntityNPCInterface.class,
                player.getEntityBoundingBox().expand(ray.x, ray.y, ray.z).grow(1), Entity::isEntityAlive)) {
            RayTraceResult hit = npc.getEntityBoundingBox().grow(0.3).calculateIntercept(eyes, end);
            if (hit == null) { continue; }
            double distance = eyes.squareDistanceTo(hit.hitVec);
            if (distance < best) { best = distance; nearest = npc; }
        }
        if (nearest != null) { return nearest.getPositionVector(); }
        return ground == null ? player.getPositionVector() : ground.hitVec;
    }
}
