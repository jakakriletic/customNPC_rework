package noppes.npcs.rework.army;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import noppes.npcs.CustomItems;
import noppes.npcs.entity.EntityNPCInterface;

/** All staff/editor actions run on the server thread using live rights and live world state. */
public final class ArmyStaffServer {
    public static final int REACH = 256, NEAR_RADIUS = 16, MAX_NEAR = 256;
    private ArmyStaffServer() { }
    public static boolean holding(EntityPlayerMP p) { return ArmyStaff.item != null && p.getHeldItemMainhand().getItem() == ArmyStaff.item; }
    public static ArmyStaffCodec.Response execute(EntityPlayerMP p, ArmyStaffCodec.Request r) {
        if (!ArmyCommandService.enabled()) { return reply(r, false, "", "RWARMY off"); }
        ArmyActor actor = ArmyCommandService.actorFor(p.getServer(), p.getUniqueID());
        boolean editor = p.getHeldItemMainhand().getItem() == CustomItems.wand
                && p.canUseCommand(2, "rwarmy");
        if (!holding(p) && !(editor && (r.op == ArmyStaffCodec.Op.INSPECT || r.op == ArmyStaffCodec.Op.ASSIGN))) {
            return reply(r, false, "", "RWARMY hold the command staff");
        }
        EntityNPCInterface npc = null;
        if (r.op == ArmyStaffCodec.Op.INSPECT || r.op == ArmyStaffCodec.Op.ASSIGN) {
            Entity e = p.world.getEntityByID(r.npc);
            if (!(e instanceof EntityNPCInterface) || e.isDead || p.getDistanceSq(e) > 64) {
                return reply(r, false, "", "RWARMY NPC unavailable (reach 8)");
            }
            npc = (EntityNPCInterface)e;
        }
        ArmyCommandService.Reply result;
        switch (r.op) {
            case INSPECT:
                String current = npc.getRwArmy();
                boolean visible = current.isEmpty() || ArmyCommandService.access(p.getServer(), actor, current).canView();
                return reply(r, visible, visible ? current : "", visible ? "RWARMY NPC army: " + (current.isEmpty() ? "none" : current) : "RWARMY NPC army unavailable");
            case ASSIGN:
                result = ArmyCommandService.assign(p.getServer(), actor, Collections.singletonList(npc), r.army); break;
            case NEAR:
                List<EntityNPCInterface> targets = new ArrayList<EntityNPCInterface>();
                for (Entity e : p.world.loadedEntityList) {
                    if (e instanceof EntityNPCInterface && !e.isDead && p.getDistanceSq(e) <= NEAR_RADIUS * NEAR_RADIUS) {
                        targets.add((EntityNPCInterface)e);
                        if (targets.size() > MAX_NEAR) { return reply(r, false, "", "RWARMY more than 256 nearby NPCs; nothing changed"); }
                    }
                }
                result = ArmyCommandService.assign(p.getServer(), actor, targets, r.army); break;
            case STOP: result = ArmyCommandService.cancel(p.getServer(), actor, r.army); break;
            case STATUS: result = ArmyCommandService.orderStatus(p.getServer(), actor, r.army); break;
            case MOVE:
            case RETURN:
                BlockPos target = r.op == ArmyStaffCodec.Op.MOVE ? rayTarget(p) : new BlockPos(p.posX, p.posY, p.posZ);
                if (target == null || !safe(p, target)) { return reply(r, false, "", "RWARMY no loaded, safe ground target (range 256)"); }
                result = ArmyCommandService.submitMove(p.getServer(), actor, r.army, p.dimension,
                        target.getX() + .5, target.getY(), target.getZ() + .5,
                        ArmyStaffCodec.SHAPES[r.shape], 1, ArmyStaffCodec.STANCES[r.stance]); break;
            default: return reply(r, false, "", "RWARMY invalid action");
        }
        String current = npc == null ? "" : npc.getRwArmy();
        if (!current.isEmpty() && !ArmyCommandService.access(p.getServer(), actor, current).canView()) { current = ""; }
        return reply(r, result.ok, current, result.message);
    }
    private static ArmyStaffCodec.Response reply(ArmyStaffCodec.Request r, boolean ok, String army, String text) {
        return new ArmyStaffCodec.Response(r.id, ok, r.npc, army, text);
    }
    /** Stop at the first unloaded chunk. No ray or safety query may load/generate terrain. */
    public static BlockPos rayTarget(EntityPlayerMP p) {
        Vec3d eye = p.getPositionEyes(1), look = p.getLook(1);
        double distance = 0;
        BlockPos previous = new BlockPos(eye);
        for (int d = 1; d <= REACH; d++) {
            Vec3d point = eye.add(look.scale(d));
            BlockPos next = new BlockPos(point);
            // The tiny rectangle also covers a diagonal crossing two chunk boundaries
            // between samples. Checking only the endpoints could miss an unloaded corner.
            if (!p.world.isAreaLoaded(new BlockPos(Math.min(previous.getX(), next.getX()), 64,
                    Math.min(previous.getZ(), next.getZ())), new BlockPos(Math.max(previous.getX(), next.getX()),
                    64, Math.max(previous.getZ(), next.getZ())))) { break; }
            distance = d;
            previous = next;
        }
        if (distance == 0) { return null; }
        RayTraceResult hit = p.world.rayTraceBlocks(eye, eye.add(look.scale(distance)), false, true, false);
        return hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK ? hit.getBlockPos().offset(hit.sideHit) : null;
    }
    private static boolean safe(EntityPlayerMP p, BlockPos pos) {
        return pos.getY() > 0 && pos.getY() < 255 && p.world.isBlockLoaded(pos)
                && p.world.isBlockLoaded(pos.up()) && p.world.isBlockLoaded(pos.down())
                && p.world.getWorldBorder().contains(pos)
                && p.world.getBlockState(pos.down()).isTopSolid()
                && p.world.getBlockState(pos).getCollisionBoundingBox(p.world, pos) == null
                && p.world.getBlockState(pos.up()).getCollisionBoundingBox(p.world, pos.up()) == null;
    }
}
