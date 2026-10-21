package noppes.npcs.rework.far;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.nbt.NBTTagCompound;
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
 *
 * <p>M5-V.3: za id videza, ki ga klient se ne pozna, poslje {@code REQ} (ponovno sele po
 * {@link #REQ_RETRY_MS}); odgovor {@code LOOK} razpakira, preveri, da se hash opisa ujema s poslanim, in
 * ga shrani: id -&gt; hash za ta svet, hash -&gt; opis v LRU ({@link #DESC_CAP}) cez svetove (hash je
 * vsebinski).
 *
 * <p>M5-V.4: vsak nov opis (in vsak vnos z znanim, a se nepecenim videzom) gre v vrsto peke
 * {@link ImpostorBaker} (en videz na slicico); ob izklopu stikala se atlas sprosti.
 */
@SideOnly(Side.CLIENT)
public final class FarClient {
    static final Map<Integer, int[]> STORE = new HashMap<Integer, int[]>();
    /** Najvec opisov videza v LRU. */
    public static final int DESC_CAP = 512;
    /** Po tolikem casu brez odgovora klient id zahteva znova. */
    public static final long REQ_RETRY_MS = 10000L;
    static final Map<Integer, Long> LOOK_IDS = new HashMap<Integer, Long>();
    static final Map<Integer, Long> LOOK_PENDING = new HashMap<Integer, Long>();
    static final LinkedHashMap<Long, NBTTagCompound> DESCS = new LinkedHashMap<Long, NBTTagCompound>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, NBTTagCompound> eldest) {
            return size() > DESC_CAP;
        }
    };

    public static long messages;
    public static long payloadBytes;
    public static long upserts;
    public static long removes;
    public static long clears;
    public static long hellos;
    public static long lookReqIds;
    public static long lookReplies;
    public static long lookErrors;

    private WorldClient helloWorld;

    private FarClient() {
    }

    /** Klice {@code CustomNpcs} prek refleksije, samo na klientu. */
    public static void install() {
        FarClient c = new FarClient();
        FarServer.channel().register(c);
        MinecraftForge.EVENT_BUS.register(c);
        ImpostorBaker.install();
    }

    public static int size() {
        return STORE.size();
    }

    /** Stevilo opisov videza v LRU. */
    public static int looks() {
        return DESCS.size();
    }

    /** Nadomestki brez znanega opisa videza (id 0 ali odgovor se ni prisel). */
    public static int missingLooks() {
        int n = 0;
        for (int[] e : STORE.values()) {
            Long hash = e[4] == 0 ? null : LOOK_IDS.get(e[4]);
            if (hash == null || !DESCS.containsKey(hash)) {
                ++n;
            }
        }
        return n;
    }

    /** Opis videza za id iz vnosa ali {@code null} (M5-V.4). */
    public static NBTTagCompound description(int lookId) {
        Long hash = LOOK_IDS.get(lookId);
        return hash == null ? null : DESCS.get(hash);
    }

    /** Kopija opisa videza prvega nadomestka z znanim opisom ali {@code null} (harness, M5-V.4). */
    public static NBTTagCompound anyDescription() {
        for (int[] e : STORE.values()) {
            NBTTagCompound d = e[4] == 0 ? null : description(e[4]);
            if (d != null) {
                return d.copy();
            }
        }
        return null;
    }

    private static void forgetWorld() {
        STORE.clear();
        LOOK_IDS.clear();
        LOOK_PENDING.clear();
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
        final long now = System.currentTimeMillis();
        final int[] want = new int[FarCodec.MAX_ITEMS];
        final int[] nWant = new int[1];
        FarCodec.readServerMessage(Unpooled.wrappedBuffer(data), new FarCodec.Sink() {
            @Override
            public void clear() {
                ++clears;
                STORE.clear();
                LOOK_PENDING.clear();
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
                Long known = look == 0 ? null : LOOK_IDS.get(look);
                if (known != null) {
                    ImpostorBaker.want(known);
                } else if (look != 0) {
                    Long since = LOOK_PENDING.get(look);
                    if ((since == null || now - since > REQ_RETRY_MS) && nWant[0] < want.length) {
                        LOOK_PENDING.put(look, now);
                        want[nWant[0]++] = look;
                    }
                }
            }

            @Override
            public void look(int id, long hash, byte[] desc) {
                LOOK_PENDING.remove(id);
                NBTTagCompound d;
                try {
                    d = FarLook.decode(desc);
                } catch (Exception e) {
                    ++lookErrors;
                    return;
                }
                if (FarLook.hash(d) != hash) {
                    ++lookErrors;
                    return;
                }
                ++lookReplies;
                DESCS.put(hash, d);
                LOOK_IDS.put(id, hash);
                ImpostorBaker.want(hash);
            }
        });
        for (int i = 0; i < nWant[0]; i += FarCodec.MAX_REQ) {
            int n = Math.min(FarCodec.MAX_REQ, nWant[0] - i);
            lookReqIds += n;
            send(FarCodec.req(want, i, n));
        }
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
                forgetWorld();
            }
            return;
        }
        if (CustomNpcs.RwFarView >= 1) {
            if (world != this.helloWorld) {
                forgetWorld();
                this.helloWorld = world;
                ++hellos;
                send(FarCodec.hello(mc.gameSettings.renderDistanceChunks * 16));
            }
        } else if (this.helloWorld != null) {
            this.helloWorld = null;
            forgetWorld();
            ImpostorBaker.release();
            send(FarCodec.bye());
        }
    }

    private static void send(ByteBuf buf) {
        FarServer.channel().sendToServer(new FMLProxyPacket(new PacketBuffer(buf), FarCodec.CHANNEL));
    }
}
