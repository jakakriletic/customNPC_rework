package si.ladja.xaerofleet.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import si.ladja.xaerofleet.integration.DavincisFleetBridge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.UUID;

public final class MoveOrderPacket implements IMessage {
    public static final int MAX_CAPTAINS = 16;

    /** Far enough out that nothing beyond it is a place a ship can be. */
    private static final double WORLD_BORDER = 29999984.0D;

    /** Half a second of real time, matching the server's own cooldown. */
    private static final long RATE_LIMIT_MILLIS = 500L;
    private static final PacketRate RATE = new PacketRate(RATE_LIMIT_MILLIS);

    private List<UUID> captainIds = Collections.emptyList();
    private int dimension;
    private double centerX;
    private double centerZ;
    private double radius;

    public MoveOrderPacket() {}

    public MoveOrderPacket(List<UUID> captainIds, int dimension,
                           double centerX, double centerZ, double radius) {
        this.captainIds = new ArrayList<>(captainIds.subList(0,
                Math.min(MAX_CAPTAINS, captainIds.size())));
        this.dimension = dimension;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(captainIds.size());
        for (UUID id : captainIds) {
            buf.writeLong(id.getMostSignificantBits());
            buf.writeLong(id.getLeastSignificantBits());
        }
        buf.writeInt(dimension);
        buf.writeDouble(centerX);
        buf.writeDouble(centerZ);
        buf.writeDouble(radius);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = buf.readUnsignedByte();
        if (count < 1 || count > MAX_CAPTAINS) throw new IllegalArgumentException("Invalid fleet size");
        // A set, because sixteen copies of one captain is a fleet of one and
        // the server should never be asked to check the same id sixteen times.
        LinkedHashSet<UUID> decoded = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) decoded.add(new UUID(buf.readLong(), buf.readLong()));
        captainIds = new ArrayList<>(decoded);
        dimension = buf.readInt();
        centerX = buf.readDouble();
        centerZ = buf.readDouble();
        radius = buf.readDouble();
    }

    List<UUID> captainIds() { return Collections.unmodifiableList(captainIds); }
    int dimension() { return dimension; }
    double centerX() { return centerX; }
    double centerZ() { return centerZ; }
    double radius() { return radius; }

    public static final class Handler implements IMessageHandler<MoveOrderPacket, IMessage> {
        @Override
        public IMessage onMessage(MoveOrderPacket message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> handle(player, message));
            return null;
        }

        private static void handle(EntityPlayerMP player, MoveOrderPacket message) {
            if (!RATE.allow(player)) {
                send(player, new CommandFeedback(false, 0, 0, "rate_limited"));
                return;
            }
            // The server repeats every one of these; this is only the front
            // door, and it exists so that a flood never reaches the fleet
            // service at all. Coordinates outside the world border are
            // rejected here rather than turned into a search across a
            // hundred thousand chunks of nothing.
            if (message.dimension != player.dimension || !finite(message.centerX)
                    || !finite(message.centerZ) || !finite(message.radius)
                    || Math.abs(message.centerX) > WORLD_BORDER
                    || Math.abs(message.centerZ) > WORLD_BORDER
                    || message.radius < 4 || message.radius > 1024) {
                send(player, new CommandFeedback(false, 0, 0, "invalid_destination"));
                return;
            }
            if (message.captainIds.isEmpty() || message.captainIds.size() > MAX_CAPTAINS) {
                send(player, new CommandFeedback(false, 0, 0, "invalid_fleet_size"));
                return;
            }
            DavincisFleetBridge bridge = DavincisFleetBridge.discover();
            Object result = bridge.submitMoveOrder(player, message.captainIds,
                    message.centerX, message.centerZ, message.radius, null);
            send(player, bridge.copyCommandResult(result));
        }

        private static void send(EntityPlayerMP player, CommandFeedback feedback) {
            FleetNetwork.CHANNEL.sendTo(new CommandResultPacket(feedback), player);
        }

        private static boolean finite(double value) {
            return !Double.isNaN(value) && !Double.isInfinite(value);
        }
    }
}
