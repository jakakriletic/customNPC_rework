package si.ladja.xaerofleet.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import si.ladja.xaerofleet.integration.DavincisFleetBridge;

import java.util.ArrayList;
import java.util.List;

/** Client request; the server ignores the requested dimension and uses the player world. */
public final class RequestCaptains implements IMessage {

    /**
     * Shortest gap between snapshots for one player, in real milliseconds.
     *
     * <p>The map refreshes every two seconds by design, so a second turns
     * nobody's honest request away. What it does stop is a client asking on
     * every frame: each answer walks the dimension's loaded entities, which is
     * a cost the server pays and the client does not.</p>
     *
     * <p>Real time, because in single player the world clock stops while the
     * map is open - which is precisely when these requests are made.</p>
     */
    private static final long RATE_LIMIT_MILLIS = 1000L;
    private static final PacketRate RATE = new PacketRate(RATE_LIMIT_MILLIS);

    @Override public void fromBytes(ByteBuf buf) {}
    @Override public void toBytes(ByteBuf buf) {}

    public static final class Handler implements IMessageHandler<RequestCaptains, IMessage> {
        @Override
        public IMessage onMessage(RequestCaptains message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> sendSnapshot(player));
            return null;
        }

        private static void sendSnapshot(EntityPlayerMP player) {
            if (!RATE.allow(player)) {
                return;
            }
            DavincisFleetBridge bridge = DavincisFleetBridge.discover();
            List<CaptainView> views = new ArrayList<>();
            if (bridge.isAvailable()) {
                for (Object source : bridge.captainsVisibleTo(player, player.dimension)) {
                    CaptainView view = bridge.copySnapshot(source);
                    if (view != null && views.size() < CaptainSnapshotPacket.MAX_CAPTAINS) {
                        views.add(view);
                    }
                }
            }
            FleetNetwork.CHANNEL.sendTo(new CaptainSnapshotPacket(views), player);
        }
    }
}
