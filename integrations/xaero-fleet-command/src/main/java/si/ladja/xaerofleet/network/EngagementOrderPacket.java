package si.ladja.xaerofleet.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import si.ladja.xaerofleet.integration.DavincisFleetBridge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * "Attack enemy" / "Passive cruise": what ships already under orders do about
 * enemies they meet on the way.
 *
 * <p>Changes only the rules of engagement - the destination, the route and
 * how far along it each ship is stay as they are. Like a cancel it is a
 * correction to a running order, so the service never rate-limits it and
 * this front door only turns away a flood.</p>
 */
public final class EngagementOrderPacket implements IMessage {

    public static final byte PASSIVE = 0;
    public static final byte ENGAGE = 1;

    private static final PacketRate RATE = new PacketRate(100L);

    private byte mode = PASSIVE;
    private List<UUID> captainIds = Collections.emptyList();

    public EngagementOrderPacket() {}

    public EngagementOrderPacket(List<UUID> captainIds, boolean engage) {
        this.mode = engage ? ENGAGE : PASSIVE;
        this.captainIds = new ArrayList<>(captainIds.subList(0,
                Math.min(MoveOrderPacket.MAX_CAPTAINS, captainIds.size())));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(mode);
        buf.writeByte(captainIds.size());
        for (UUID id : captainIds) {
            buf.writeLong(id.getMostSignificantBits());
            buf.writeLong(id.getLeastSignificantBits());
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        byte decodedMode = buf.readByte();
        if (decodedMode != PASSIVE && decodedMode != ENGAGE) {
            throw new IllegalArgumentException("Invalid engagement mode");
        }
        int count = buf.readUnsignedByte();
        if (count < 1 || count > MoveOrderPacket.MAX_CAPTAINS) {
            throw new IllegalArgumentException("Invalid fleet size");
        }
        LinkedHashSet<UUID> decoded = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) decoded.add(new UUID(buf.readLong(), buf.readLong()));
        mode = decodedMode;
        captainIds = new ArrayList<>(decoded);
    }

    boolean engage() { return mode == ENGAGE; }

    List<UUID> captainIds() { return Collections.unmodifiableList(captainIds); }

    public static final class Handler implements IMessageHandler<EngagementOrderPacket, IMessage> {
        @Override
        public IMessage onMessage(EngagementOrderPacket message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> handle(player, message));
            return null;
        }

        private static void handle(EntityPlayerMP player, EngagementOrderPacket message) {
            if (!RATE.allow(player) || message.captainIds.isEmpty()) return;
            DavincisFleetBridge bridge = DavincisFleetBridge.discover();
            byte kind = message.engage() ? CommandResultPacket.KIND_ENGAGE
                    : CommandResultPacket.KIND_PASSIVE;
            CommandFeedback feedback;
            if (!bridge.supportsEngagement()) {
                feedback = new CommandFeedback(false, 0, message.captainIds.size(),
                        bridge.isAvailable() ? "engagement_unsupported"
                                : "integration_unavailable");
            } else {
                feedback = bridge.copyCommandResult(bridge.setOrderEngagement(player,
                        message.captainIds, message.engage() ? "ENGAGE" : "PASSIVE"));
            }
            FleetNetwork.CHANNEL.sendTo(new CommandResultPacket(kind, feedback), player);
        }
    }
}
