package noppes.npcs.rework.army;

import java.util.Map;
import java.util.WeakHashMap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import noppes.npcs.LogWriter;

/**
 * M11.2 (Q299, Q317): server side of the {@value ArmySnapshot#CHANNEL} channel, which exists only with RwArmies>=1
 * (registered in pre-init on both sides). The server only ever answers a request: no tick, nothing is pushed. A
 * player gets at most one snapshot per {@value #MIN_INTERVAL} ticks, built with the rights of that moment.
 */
public final class ArmyNet {
    public static final int MIN_INTERVAL = 20;
    private static FMLEventChannel channel;
    private static int served, limited, rejected;

    /** Server thread only; a player who reconnects is a new object and may ask at once. */
    private final Map<EntityPlayerMP, Integer> lastAnswer = new WeakHashMap<EntityPlayerMP, Integer>();
    private final Map<EntityPlayerMP, Integer> lastAction = new WeakHashMap<EntityPlayerMP, Integer>();

    private ArmyNet() {
    }

    /** Pre-init, only with RwArmies>=1. */
    public static synchronized void init() {
        if (channel == null) {
            channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(ArmySnapshot.CHANNEL);
            channel.register(new ArmyNet());
            LogWriter.info("RWARMY channel " + ArmySnapshot.CHANNEL + " registered");
        }
    }

    static FMLEventChannel channel() {
        return channel;
    }

    /** Counters for {@code /rwarmy clusters}: answers, requests over the limit, malformed requests. */
    static String counters() {
        return "served=" + served + " limited=" + limited + " rejected=" + rejected;
    }

    @SubscribeEvent
    public void onServerPacket(FMLNetworkEvent.ServerCustomPacketEvent event) {
        if (!(event.getHandler() instanceof NetHandlerPlayServer)) {
            return;
        }
        final EntityPlayerMP player = ((NetHandlerPlayServer) event.getHandler()).player;
        final MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ByteBuf payload = event.getPacket().payload();
        byte[] data = null;
        if (payload.readableBytes() <= ArmyStaffCodec.MAX_PERM_REQUEST) {
            data = new byte[payload.readableBytes()];
            payload.getBytes(payload.readerIndex(), data);
        }
        int kind = ArmyStaffCodec.kind(data);
        if (kind == ArmyStaffCodec.REQUEST || kind == ArmyStaffCodec.PERM_REQUEST) {
            final ArmyStaffCodec.Request request = kind == ArmyStaffCodec.REQUEST ? ArmyStaffCodec.request(data) : null;
            final ArmyStaffCodec.PermRequest perm = kind == ArmyStaffCodec.PERM_REQUEST ? ArmyStaffCodec.permRequest(data) : null;
            server.addScheduledTask(() -> {
                if (request == null && perm == null) { rejected++; return; }
                int now = server.getTickCounter();
                Integer last = lastAction.get(player);
                if (last != null && now - last < 5) { limited++; return; }
                if (player.hasDisconnected() || !ArmyCommandService.enabled()) { return; }
                lastAction.put(player, now);
                ArmyStaffCodec.Response response = request != null ? ArmyStaffServer.execute(player, request)
                        : ArmyStaffServer.executePerm(player, perm);
                channel.sendTo(new FMLProxyPacket(new PacketBuffer(Unpooled.wrappedBuffer(response.encode())),
                        ArmySnapshot.CHANNEL), player);
            });
            return;
        }
        final int requestId = ArmySnapshot.decodeRequest(data);
        server.addScheduledTask(() -> answer(server, player, requestId));
    }

    private void answer(MinecraftServer server, EntityPlayerMP player, int requestId) {
        if (requestId < 0) {
            rejected++;
            return;
        }
        Integer last = lastAnswer.get(player);
        int now = server.getTickCounter();
        if (last != null && now - last < MIN_INTERVAL) {
            limited++;
            return;
        }
        if (player.hasDisconnected()) {
            return;
        }
        ArmySnapshot snapshot = ArmyCommandService.snapshot(server,
                ArmyCommandService.actorFor(server, player.getUniqueID()), requestId);
        if (snapshot == null) {
            return;
        }
        lastAnswer.put(player, now);
        served++;
        channel.sendTo(new FMLProxyPacket(new PacketBuffer(Unpooled.wrappedBuffer(snapshot.encode())),
                ArmySnapshot.CHANNEL), player);
    }
}
