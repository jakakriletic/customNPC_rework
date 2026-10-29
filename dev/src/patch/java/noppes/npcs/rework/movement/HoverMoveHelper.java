package noppes.npcs.rework.movement;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityMoveHelper;
import noppes.npcs.ai.FlyingMoveHelper;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * Opt-in creative flight. Owns velocity, while EntityNPCFlying.travel still performs
 * the actual move and all vanilla collisions. No teleport, gravity or pathfinder changes.
 * Each MOVE_TO is consumed for one tick: clearing a path must also stop its last command.
 */
public final class HoverMoveHelper extends EntityMoveHelper {
    private final EntityNPCInterface npc;

    public HoverMoveHelper(EntityNPCInterface npc) {
        super(npc);
        this.npc = npc;
    }

    /** Called only for movementType=1; swim and ground keep their existing helpers. */
    public static EntityMoveHelper forFlying(EntityNPCInterface npc) {
        return npc.getRwFlightMode() == FlightMode.HOVER
                ? new HoverMoveHelper(npc) : new FlyingMoveHelper(npc);
    }

    @Override
    public void onUpdateMoveHelper() {
        // Do not let stale AI travel inputs add acceleration on top of our velocity.
        npc.setMoveForward(0);
        npc.setMoveStrafing(0);
        npc.setMoveVertical(0);
        npc.motionX = npc.motionY = npc.motionZ = 0;
        if (action != Action.MOVE_TO) {
            return;
        }
        action = Action.WAIT;
        double dx = posX - npc.posX;
        double dy = posY - npc.posY;
        double dz = posZ - npc.posZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double requested = speed * npc.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED)
                .getAttributeValue();
        if (!Double.isFinite(distance) || !Double.isFinite(requested)
                || distance < 1.0e-6 || speed <= 0 || requested <= 0) {
            return;
        }
        // One 3D speed budget. Proportional approach cannot overshoot the waypoint.
        double scale = Math.min(requested, distance * 0.5) / distance;
        npc.motionX = dx * scale;
        npc.motionY = dy * scale;
        npc.motionZ = dz * scale;
        if (dx * dx + dz * dz > 1.0e-12) {
            float yaw = (float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90;
            npc.rotationYaw = limitAngle(npc.rotationYaw, yaw, 20);
            npc.renderYawOffset = npc.rotationYaw;
        }
    }
}
