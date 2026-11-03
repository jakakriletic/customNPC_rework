package noppes.npcs.rework.army;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * M11.2: client half of {@link ArmyNet}, installed by CustomNpcs through reflection with RwArmies>=1. Asks only when
 * called (the staff of M11.8a while its GUI is open) and keeps the newest answer; nothing draws it yet. A server
 * without the channel simply never answers. Client main thread, except the packet handler.
 */
@SideOnly(Side.CLIENT)
public final class ArmyClient {
    private static int nextRequest;
    private static ArmySnapshot latest;
    private static int replies, malformed;

    private ArmyClient() {
    }

    public static void install() {
        ArmyClient c = new ArmyClient();
        ArmyNet.channel().register(c);
        MinecraftForge.EVENT_BUS.register(c);
    }

    /** Sends one request; the answer later replaces {@link #latest()}. @return the id of the request */
    public static int request() {
        int id = nextRequest++ & Integer.MAX_VALUE;
        send(ArmySnapshot.encodeRequest(id));
        return id;
    }

    /** Any bytes on the channel; the harness also sends malformed ones. */
    public static void send(byte[] data) {
        ArmyNet.channel().sendToServer(new FMLProxyPacket(new PacketBuffer(Unpooled.wrappedBuffer(data)),
                ArmySnapshot.CHANNEL));
    }

    /** The newest answer of this connection or null. */
    public static ArmySnapshot latest() {
        return latest;
    }

    public static int replies() {
        return replies;
    }

    public static int malformed() {
        return malformed;
    }

    @SubscribeEvent
    public void onClientPacket(FMLNetworkEvent.ClientCustomPacketEvent event) {
        ByteBuf payload = event.getPacket().payload();
        int size = payload.readableBytes();
        byte[] data = null;
        if (size <= ArmySnapshot.MAX_MESSAGE) {
            data = new byte[size];
            payload.getBytes(payload.readerIndex(), data);
        }
        final ArmySnapshot snapshot = ArmySnapshot.decode(data);
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if (snapshot == null) {
                malformed++;
            } else {
                latest = snapshot;
                replies++;
            }
        });
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            latest = null;
        });
    }
}
