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
 * "Stop": the selected captains drop their orders and go back to patrol.
 *
 * <p>The fleet service never rate-limits a cancel - stopping a fleet is the
 * one order a player may need twice in a second - so this front door only
 * turns away a flood a hand could not have produced.</p>
 */
public final class CancelOrderPacket implements IMessage {

    private static final PacketRate RATE = new PacketRate(100L);

    private List<UUID> captainIds = Collections.emptyList();

    public CancelOrderPacket() {}

    public CancelOrderPacket(List<UUID> captainIds) {
        this.captainIds = new ArrayList<>(captainIds.subList(0,
                Math.min(MoveOrderPacket.MAX_CAPTAINS, captainIds.size())));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(captainIds.size());
        for (UUID id : captainIds) {
            buf.writeLong(id.getMostSignificantBits());
            buf.writeLong(id.getLeastSignificantBits());
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = buf.readUnsignedByte();
        if (count < 1 || count > MoveOrderPacket.MAX_CAPTAINS) {
            throw new IllegalArgumentException("Invalid fleet size");
        }
        LinkedHashSet<UUID> decoded = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) decoded.add(new UUID(buf.readLong(), buf.readLong()));
        captainIds = new ArrayList<>(decoded);
    }

    List<UUID> captainIds() { return Collections.unmodifiableList(captainIds); }

    public static final class Handler implements IMessageHandler<CancelOrderPacket, IMessage> {
        @Override
        public IMessage onMessage(CancelOrderPacket message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> handle(player, message));
            return null;
        }

        private static void handle(EntityPlayerMP player, CancelOrderPacket message) {
            if (!RATE.allow(player) || message.captainIds.isEmpty()) return;
            DavincisFleetBridge bridge = DavincisFleetBridge.discover();
            Object result = bridge.cancelOrder(player, message.captainIds);
            FleetNetwork.CHANNEL.sendTo(new CommandResultPacket(
                    CommandResultPacket.KIND_CANCEL, bridge.copyCommandResult(result)), player);
        }
    }
}
