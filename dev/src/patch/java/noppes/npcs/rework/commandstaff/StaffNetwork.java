package noppes.npcs.rework.commandstaff;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.army.ArmyActor;
import noppes.npcs.rework.army.ArmyCommandService;
import noppes.npcs.rework.army.ArmyMarches;
import noppes.npcs.rework.army.ArmyStaffServer;
import noppes.npcs.rework.formation.FormationMath;
import noppes.npcs.rework.formation.FormationShape;
import noppes.npcs.rework.formation.Slot;
import noppes.npcs.rework.formation.SlotAssigner;
import noppes.npcs.rework.formation.SquadManager;
import noppes.npcs.rework.formation.WorldTerrain;

/** The server owns selection and resolves both ray traces from the player's current look. */
public final class StaffNetwork {
    static final String CHANNEL = "rwcmdstaff";
    static final int SELECT = 1, MENU = 2, MODES = 3, MOVE = 4, RESTORE = 5;
    static final int MAX_SELECTION = 256, MAX_GROW_TICKS = 37, MAX_RANGE = 256;
    private static FMLEventChannel channel;
    private final WeakHashMap<EntityPlayerMP, Selection> selections = new WeakHashMap<>();

    private static final class Selection {
        final int dimension;
        final List<EntityNPCInterface> npcs = new ArrayList<>();
        int lastAction = -10;
        Selection(int dimension) { this.dimension = dimension; }
    }

    public static void install() {
        if (channel != null) { return; }
        channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(CHANNEL);
        StaffNetwork handler = new StaffNetwork();
        channel.register(handler);
        MinecraftForge.EVENT_BUS.register(handler);
    }

    static FMLEventChannel channel() { return channel; }
    static float radius(int ticks) { return Math.min(24F, 2F + Math.max(0, ticks) * 0.6F); }

    static boolean mayCommand(EntityPlayerMP player, EntityNPCInterface npc) {
        if (player == null || player.world != npc.world || !npc.isEntityAlive() || !StaffOrder.canMove(npc)) { return false; }
        if (player.canUseCommand(2, "rwarmy") && CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.NPC_GUI)) {
            return true;
        }
        if (!npc.getRwArmy().isEmpty() && ArmyCommandService.enabled()) {
            ArmyActor actor = ArmyCommandService.actorFor(player.getServer(), player.getUniqueID());
            return ArmyCommandService.access(player.getServer(), actor, npc.getRwArmy()).canCommand();
        }
        return npc.getOwner() == player;
    }

    @SubscribeEvent public void packet(FMLNetworkEvent.ServerCustomPacketEvent event) {
        if (!(event.getHandler() instanceof NetHandlerPlayServer)) { return; }
        ByteBuf data = event.getPacket().payload();
        if (data.readableBytes() != 3) { return; }
        int operation = data.getUnsignedByte(data.readerIndex());
        int mask = data.getUnsignedByte(data.readerIndex() + 1);
        int values = data.getUnsignedByte(data.readerIndex() + 2);
        if (operation < SELECT || operation > RESTORE) { return; }
        if (operation == SELECT ? mask > MAX_GROW_TICKS || values != 0
                : operation == MODES ? (mask & ~StaffOrder.MODES) != 0 || (values & ~mask) != 0
                : mask != 0 || values != 0) { return; }
        EntityPlayerMP player = ((NetHandlerPlayServer)event.getHandler()).player;
        if (player.getServer() != null) {
            player.getServer().addScheduledTask(() -> act(player, operation, mask, values));
        }
    }

    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent event) { selections.remove(event.player); }
    @SubscribeEvent public void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) { selections.remove(event.player); }
    @SubscribeEvent public void respawn(PlayerEvent.PlayerRespawnEvent event) { selections.remove(event.player); }

    private void act(EntityPlayerMP player, int op, int mask, int values) {
        if (player.hasDisconnected() || !player.isEntityAlive() || !CommandStaff.holding(player)) { return; }
        Selection selection = selections.computeIfAbsent(player, p -> new Selection(p.dimension));
        if (selection.dimension != player.dimension) {
            selection = new Selection(player.dimension);
            selections.put(player, selection);
        }
        int now = player.getServer().getTickCounter();
        if (op == SELECT) {
            if (now - selection.lastAction < 4) { return; }
            selection.lastAction = now;
            select(player, selection, radius(mask));
            reply(player, SELECT, selection.npcs);
            player.sendStatusMessage(new TextComponentString(selection.npcs.size() + " NPCs selected."), true);
            return;
        }
        if (now - selection.lastAction < 4 && op != MENU) { return; }
        if (op != MENU) { selection.lastAction = now; }
        selection.npcs.removeIf(npc -> player.world.getEntityByID(npc.getEntityId()) != npc
                || !mayCommand(player, npc) || player.getDistanceSq(npc) > MAX_RANGE * MAX_RANGE);
        if (op == MENU) {
            reply(player, MENU, selection.npcs);
            return;
        }
        if (selection.npcs.isEmpty()) {
            player.sendStatusMessage(new TextComponentString("No NPCs selected."), true);
            reply(player, SELECT, selection.npcs);
            return;
        }
        List<EntityNPCInterface> available = new ArrayList<>(selection.npcs);
        available.removeIf(ArmyMarches::hasActiveOrder);
        if (available.isEmpty()) {
            player.sendStatusMessage(new TextComponentString("The selected NPCs are marching under an army order."), true);
            if (op != MOVE) { reply(player, MODES, selection.npcs); }
            return;
        }
        if (op == MOVE) {
            move(player, available);
        } else {
            SquadManager.stop(available);
            for (EntityNPCInterface npc : available) {
                if (op == RESTORE) { StaffOrder.restore(npc); }
                else { StaffOrder.apply(npc, player, mask, values); }
            }
            reply(player, MODES, selection.npcs);
        }
    }

    private void select(EntityPlayerMP player, Selection selection, double radius) {
        Vec3d center = StaffSelection.center(player);
        selection.npcs.clear();
        AxisAlignedBB box = new AxisAlignedBB(center, center).grow(radius, 8, radius);
        List<EntityNPCInterface> candidates = player.world.getEntitiesWithinAABB(EntityNPCInterface.class, box,
                npc -> mayCommand(player, npc) && player.getDistanceSq(npc) <= MAX_RANGE * MAX_RANGE
                        && horizontalDistance(npc, center) <= radius * radius);
        candidates.sort(Comparator.comparingDouble(npc -> horizontalDistance(npc, center)));
        selection.npcs.addAll(candidates.subList(0, Math.min(MAX_SELECTION, candidates.size())));
    }

    private static double horizontalDistance(Entity npc, Vec3d center) {
        double x = npc.posX - center.x, z = npc.posZ - center.z;
        return x * x + z * z;
    }

    private void move(EntityPlayerMP player, List<EntityNPCInterface> npcs) {
        BlockPos destination = ArmyStaffServer.rayTarget(player);
        if (destination == null) {
            player.sendStatusMessage(new TextComponentString("Look at loaded ground within 256 blocks."), true);
            return;
        }
        World world = player.world;
        WorldTerrain terrain = new WorldTerrain(world);
        int count = npcs.size();
        int width = (int)Math.ceil(Math.sqrt(count));
        double spacing = 2;
        double[] xs = new double[count], zs = new double[count];
        for (int i = 0; i < count; i++) {
            EntityNPCInterface npc = npcs.get(i);
            Entity vehicle = npc.getRidingEntity();
            spacing = Math.max(spacing, (vehicle == null ? npc.width : vehicle.width) + 0.65);
            xs[i] = npc.posX;
            zs[i] = npc.posZ;
        }
        Slot[] slots = FormationShape.block(count, width, spacing, 0, 0);
        int[] assigned = SlotAssigner.assign(xs, zs, slots, player.rotationYaw);
        Vec3d[] positions = new Vec3d[count];
        Set<BlockPos> reserved = new HashSet<>();
        for (int i = 0; i < count; i++) {
            Slot slot = slots[assigned[i]];
            double x = FormationMath.worldX(destination.getX() + 0.5, player.rotationYaw, slot);
            double z = FormationMath.worldZ(destination.getZ() + 0.5, player.rotationYaw, slot);
            double y = terrain.standY(x, destination.getY(), z);
            if (!Double.isFinite(y)) { continue; }
            BlockPos block = new BlockPos(x, y, z);
            if (!world.getWorldBorder().contains(block) || !reserved.add(block)) { continue; }
            positions[i] = new Vec3d(x, y, z);
        }
        List<EntityNPCInterface> moving = new ArrayList<>();
        for (int i = 0; i < count; i++) { if (positions[i] != null) { moving.add(npcs.get(i)); } }
        SquadManager.stop(moving);
        for (int i = 0; i < count; i++) { if (positions[i] != null) { StaffOrder.move(npcs.get(i), player, positions[i]); } }
        player.sendStatusMessage(new TextComponentString("Moving " + moving.size() + "/" + count + " NPCs to formation."), true);
    }

    private static void reply(EntityPlayerMP player, int operation, List<EntityNPCInterface> npcs) {
        PacketBuffer out = new PacketBuffer(Unpooled.buffer(5 + 4 * npcs.size()));
        int all = npcs.isEmpty() ? 0 : StaffOrder.MODES, any = 0;
        for (EntityNPCInterface npc : npcs) {
            int modes = StaffOrder.modes(npc);
            all &= modes;
            any |= modes;
        }
        out.writeByte(operation).writeByte(all).writeByte(any).writeShort(npcs.size());
        for (EntityNPCInterface npc : npcs) { out.writeInt(npc.getEntityId()); }
        channel.sendTo(new FMLProxyPacket(out, CHANNEL), player);
    }
}
