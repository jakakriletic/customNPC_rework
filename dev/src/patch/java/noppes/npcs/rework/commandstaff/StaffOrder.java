package noppes.npcs.rework.commandstaff;

import java.util.UUID;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import noppes.npcs.CustomNpcs;
import noppes.npcs.ai.EntityAIWander;
import noppes.npcs.constants.AiMutex;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.army.ArmyMarches;
import noppes.npcs.rework.army.ArmyWaypoints;
import noppes.npcs.rework.formation.SquadManager;
import noppes.npcs.rework.formation.WorldTerrain;

/** One NPC's staff settings and movement. The normal AI settings remain available for restoring. */
public final class StaffOrder {
    public static final int FOLLOW = 1, WANDER = 2, GUARD = 4, MODES = FOLLOW | WANDER | GUARD;
    private final UUID commander;
    int modes;
    private Vec3d post, move;
    private int replans, attempts;
    private double bestDistance;

    StaffOrder(EntityNPCInterface npc, UUID commander) {
        this.commander = commander;
        modes = npc.ais.getMovingType() == 1 ? WANDER : 0;
        post = npc.getPositionVector();
    }

    public static StaffOrder read(EntityNPCInterface npc, NBTTagCompound tag) {
        NBTTagCompound order = tag.getCompoundTag("RwCommandStaff");
        if (!order.hasUniqueId("Commander")) { return null; }
        StaffOrder value = new StaffOrder(npc, order.getUniqueId("Commander"));
        value.modes = order.getInteger("Modes") & MODES;
        if (order.hasKey("PostY", 99)) {
            double x = order.getDouble("PostX"), y = order.getDouble("PostY"), z = order.getDouble("PostZ");
            if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) && Math.abs(x) < 30000000
                    && Math.abs(z) < 30000000 && y >= 0 && y <= 256) { value.post = new Vec3d(x, y, z); }
        }
        return value;
    }

    public void write(NBTTagCompound tag) {
        NBTTagCompound order = new NBTTagCompound();
        order.setUniqueId("Commander", commander);
        order.setInteger("Modes", modes);
        order.setDouble("PostX", post.x);
        order.setDouble("PostY", post.y);
        order.setDouble("PostZ", post.z);
        tag.setTag("RwCommandStaff", order);
    }

    public static boolean active(EntityNPCInterface npc) {
        return CustomNpcs.RwCommandStaff == 1 && npc.rwStaffOrder != null && npc.ais.movementType == 0
                && !npc.isRiding() && !npc.isBeingRidden();
    }

    public static boolean guarding(EntityNPCInterface npc) {
        return active(npc) && (npc.rwStaffOrder.modes & GUARD) != 0 && !ArmyMarches.hasActiveOrder(npc);
    }

    public static boolean canEngage(EntityNPCInterface npc, EntityLivingBase target) {
        if (!active(npc) || ArmyMarches.hasActiveOrder(npc)) { return true; }
        if (target.getUniqueID().equals(npc.rwStaffOrder.commander)) { return false; }
        if ((npc.rwStaffOrder.modes & GUARD) == 0) { return true; }
        double melee = npc.stats.melee.getRange();
        double reach = Math.max(melee * melee + target.width, 4 * npc.width * npc.width + target.width);
        int range = npc.stats.ranged.getRange();
        if (npc.inventory.getProjectile() != null) { reach = Math.max(reach, (double)range * range); }
        return npc.getDistanceSq(target) <= reach;
    }

    static int modes(EntityNPCInterface npc) {
        if (npc.rwStaffOrder != null) { return npc.rwStaffOrder.modes; }
        return npc.ais.getMovingType() == 1 ? WANDER : 0;
    }

    static void apply(EntityNPCInterface npc, EntityPlayerMP player, int mask, int values) {
        StaffOrder next = forPlayer(npc, player);
        next.modes = (next.modes & ~mask) | (values & mask);
        if ((mask & FOLLOW) != 0) {
            next.move = null;
            if ((values & FOLLOW) == 0) { next.post = npc.getPositionVector(); }
        }
        npc.rwStaffOrder = next;
        if (npc.getAttackTarget() != null && !canEngage(npc, npc.getAttackTarget())) {
            npc.setRevengeTarget(null);
            npc.setAttackTarget(null);
        }
        npc.getNavigator().clearPath();
        npc.updateAI = true;
    }

    static void move(EntityNPCInterface npc, EntityPlayerMP player, Vec3d target) {
        StaffOrder next = forPlayer(npc, player);
        next.move = target;
        next.modes &= ~FOLLOW;
        next.replans = 0;
        next.attempts = 0;
        next.bestDistance = Double.POSITIVE_INFINITY;
        boolean rebuild = npc.rwStaffOrder == null || npc.rwStaffOrder != next;
        npc.rwStaffOrder = next;
        npc.getNavigator().clearPath();
        if (rebuild) { npc.updateAI = true; }
    }

    private static StaffOrder forPlayer(EntityNPCInterface npc, EntityPlayerMP player) {
        StaffOrder previous = npc.rwStaffOrder;
        if (previous != null && previous.commander.equals(player.getUniqueID())) { return previous; }
        StaffOrder next = new StaffOrder(npc, player.getUniqueID());
        if (previous != null) {
            next.modes = previous.modes;
            next.post = previous.post;
            if (previous.move != null) {
                next.move = previous.move;
                next.bestDistance = Double.POSITIVE_INFINITY;
            }
        }
        return next;
    }

    static void restore(EntityNPCInterface npc) {
        if (npc.rwStaffOrder == null) { return; }
        npc.rwStaffOrder = null;
        npc.getNavigator().clearPath();
        npc.updateAI = true;
    }

    public static void addTasks(EntityNPCInterface npc, int priority) {
        npc.tasks.addTask(priority, new Move(npc));
        npc.tasks.addTask(priority + 1, new EntityAIWander(npc) {
            @Override public boolean shouldExecute() { return allowed() && super.shouldExecute(); }
            @Override public boolean shouldContinueExecuting() { return allowed() && super.shouldContinueExecuting(); }
            private boolean allowed() {
                StaffOrder order = npc.rwStaffOrder;
                return active(npc) && !busy(npc) && order.move == null && (order.modes & WANDER) != 0
                        && (order.modes & FOLLOW) == 0 && npc.getAttackTarget() == null;
            }
        });
    }

    public static BlockPos wanderCenter(EntityNPCInterface npc, BlockPos original) {
        return active(npc) ? new BlockPos(npc.rwStaffOrder.post) : original;
    }

    private static boolean busy(EntityNPCInterface npc) {
        return SquadManager.contains(npc) || ArmyMarches.hasActiveOrder(npc);
    }

    private static final class Move extends EntityAIBase {
        private final EntityNPCInterface npc;
        private boolean navigating;
        private int followDelay;

        Move(EntityNPCInterface npc) {
            this.npc = npc;
            setMutexBits(AiMutex.PASSIVE);
        }

        @Override public boolean shouldExecute() {
            if (!active(npc) || busy(npc) || npc.isInteracting() || npc.getAttackTarget() != null) { return false; }
            StaffOrder order = npc.rwStaffOrder;
            return order.move != null || (order.modes & FOLLOW) != 0;
        }

        @Override public boolean shouldContinueExecuting() { return shouldExecute(); }

        @Override public void resetTask() {
            if (navigating) { npc.getNavigator().clearPath(); }
            navigating = false;
            followDelay = 0;
        }

        @Override public void updateTask() {
            StaffOrder order = npc.rwStaffOrder;
            if (order.move != null) {
                Vec3d target = order.move;
                double distance = npc.getDistanceSq(target.x, target.y, target.z);
                if (distance <= 0.75 * 0.75) {
                    order.move = null;
                    order.post = target;
                    resetTask();
                } else if (--order.replans <= 0 && npc.getNavigator().noPath()) {
                    order.replans = 12 + (npc.getEntityId() & 7);
                    if (distance + 1 < order.bestDistance) { order.attempts = 0; order.bestDistance = distance; }
                    if (++order.attempts > 6) {
                        order.move = null;
                        return;
                    }
                    navigating = path(target);
                }
                return;
            }
            EntityPlayerMP leader = npc.getServer().getPlayerList().getPlayerByUUID(order.commander);
            if (leader == null || leader.world != npc.world || !StaffNetwork.mayCommand(leader, npc)) {
                resetTask();
            } else if (npc.getDistanceSq(leader) > 4 * 4 && --followDelay <= 0) {
                followDelay = 12 + (npc.getEntityId() & 7);
                navigating = path(leader.getPositionVector());
            } else if (navigating && npc.getDistanceSq(leader) < 3 * 3) {
                resetTask();
            }
        }

        private boolean path(Vec3d target) {
            double[] next = ArmyWaypoints.next(npc.posX, npc.posY, npc.posZ, target.x, target.y, target.z,
                    CustomNpcs.NpcNavRange, npc.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue());
            if (next == null || !npc.world.isBlockLoaded(new BlockPos(next[0], next[1], next[2]))) { return false; }
            double y = next[1];
            if (!ArmyWaypoints.finalLeg(next, target.x, target.y, target.z)) {
                double ground = new WorldTerrain(npc.world).standY(next[0], y, next[2]);
                if (Double.isFinite(ground)) { y = ground; }
            }
            Path p = npc.getNavigator().getPathToXYZ(next[0], y, next[2]);
            return p != null && npc.getNavigator().setPath(p, 1.0);
        }
    }
}
