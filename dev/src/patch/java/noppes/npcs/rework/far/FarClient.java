package noppes.npcs.rework.far;

import java.util.HashMap;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;

/**
 * M5-V.2: klientska stran kanala {@code rwfar} - rokovanje in shramba nadomestkov.
 *
 * <p>Z {@code RwFarView >= 1} klient ob vsakem novem svetu (vstop, menjava dimenzije) poslje
 * {@code HELLO} z zelenim dometom {@code renderDistanceChunks * 16}; ob izklopu stikala poslje
 * {@code BYE}. Prejeti posnetki se na glavni niti klienta zapisejo v shrambo
 * {@code entityId -> (x, y, z v 1/8 bloka, yaw, videz, zastavice)}; ob novem svetu ali {@code CLEAR}
 * se shramba izprazni. Izris (L3) je M5-V.5; ta paket shrambo samo vodi in steje (harness
 * {@code v-run.ps1 -Far 1}). Z {@code RwFarView=0} klient ne poslje nicesar in streznik mu ne poslje
 * nicesar.
 */
@SideOnly(Side.CLIENT)
public final class FarClient {
    static final Map<Integer, int[]> STORE = new HashMap<Integer, int[]>();

    public static long messages;
    public static long payloadBytes;
    public static long upserts;
    public static long removes;
    public static long clears;
    public static long hellos;

    private WorldClient helloWorld;

    private FarClient() {
    }

    /** Klice {@code CustomNpcs} prek refleksije, samo na klientu. */
    public static void install() {
        FarClient c = new FarClient();
        FarServer.channel().register(c);
        MinecraftForge.EVENT_BUS.register(c);
    }

    public static int size() {
        return STORE.size();
    }

    @SubscribeEvent
    public void onClientPacket(FMLNetworkEvent.ClientCustomPacketEvent event) {
        ByteBuf payload = event.getPacket().payload();
        final byte[] data = new byte[payload.readableBytes()];
        payload.getBytes(payload.readerIndex(), data);
        Minecraft.getMinecraft().addScheduledTask(() -> apply(data));
    }

    private static void apply(byte[] data) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return;
        }
        ++messages;
        payloadBytes += data.length;
        final int rx = FarCodec.quantize(mc.player.posX);
        final int ry = FarCodec.quantize(mc.player.posY);
        final int rz = FarCodec.quantize(mc.player.posZ);
        FarCodec.readServerMessage(Unpooled.wrappedBuffer(data), new FarCodec.Sink() {
            @Override
            public void clear() {
                ++clears;
                STORE.clear();
            }

            @Override
            public void remove(int id) {
                ++removes;
                STORE.remove(id);
            }

            @Override
            public void entry(int id, int qx24, int qy24, int qz24, int yaw, int look, int flags) {
                ++upserts;
                STORE.put(id, new int[] {FarCodec.unwrap24(qx24, rx), FarCodec.unwrap24(qy24, ry),
                        FarCodec.unwrap24(qz24, rz), yaw, look, flags});
            }
        });
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        WorldClient world = mc.world;
        boolean connected = world != null && mc.player != null && mc.getConnection() != null;
        if (!connected) {
            if (this.helloWorld != null) {
                this.helloWorld = null;
                STORE.clear();
            }
            return;
        }
        if (CustomNpcs.RwFarView >= 1) {
            if (world != this.helloWorld) {
                STORE.clear();
                this.helloWorld = world;
                ++hellos;
                send(FarCodec.hello(mc.gameSettings.renderDistanceChunks * 16));
            }
        } else if (this.helloWorld != null) {
            this.helloWorld = null;
            STORE.clear();
            send(FarCodec.bye());
        }
    }

    private static void send(ByteBuf buf) {
        FarServer.channel().sendToServer(new FMLProxyPacket(new PacketBuffer(buf), FarCodec.CHANNEL));
    }
}
