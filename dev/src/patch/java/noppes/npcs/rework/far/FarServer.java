package noppes.npcs.rework.far;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import noppes.npcs.LogWriter;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * M5-V.2: strezniska stran vidnosti na daljavo (D-034, D-035; raziskava M5-V.0 §5.1).
 *
 * <p>Igralcem, katerih klient je poslal {@code HELLO} (klient z {@code RwFarView >= 1}), streznik
 * priblizno enkrat na sekundo poslje NPC-je v pasu {@link FarBand} kot lahke nadomestke po kanalu
 * {@code rwfar} ({@link FarCodec}), samo spremembe ({@link FarDelta}). Igralec {@code p} je obdelan na
 * ticku {@code t}, ko {@code (t + p.entityId) % 20 == 0} (obremenitev razprsena po tickih). Pogoji za
 * NPC: v pasu, {@code isPlayerWatchingChunk} (klient ima teren), ni neviden ({@code display.visible
 * == 1} se nikoli ne poslje), ni mrtev s skritim truplom.
 *
 * <p>{@code RwFarView=0} (privzeto do M5-V.8) ne poslje nicesar; stari klient ali klient z
 * izklopljenim stikalom ne poslje {@code HELLO} in ne dobi nicesar. AI, frekvenca ticka in vanilla
 * sledilnik so nespremenjeni (R5). Cas obdelave se meri na igralca (D-043: strezniski cilj
 * +&lt;= 0,1 ms na igralca se iz MSPT ne razloci), izpis {@code /rwfar}. Stanje bere in pise samo
 * strezniska nit; {@code HELLO} z Netty niti se prestavi nanjo.
 */
public final class FarServer {
    public static final int OFF = 0;
    public static final int SEND = 1;
    /** Kadenca obdelave igralca v tickih (~1 Hz). */
    public static final int PERIOD = 20;
    /** Domet sledilnika, ce razred NPC-ja nima registracije (CNPC registrira 64). */
    public static final int DEFAULT_TRACK_RANGE = 64;

    private static FMLEventChannel channel;
    private static int mode = OFF;
    private static int range = 384;

    static final Map<UUID, Viewer> VIEWERS = new LinkedHashMap<UUID, Viewer>();

    private static int resetTick;
    static long rounds;
    static long entries;
    static long removals;
    static long messages;
    static long bytes;
    static long nanos;
    static long maxNanos;
    static long hellos;

    /** Stanje enega igralca, ki je poslal HELLO. */
    static final class Viewer {
        final FarDelta delta = new FarDelta();
        String name;
        int wantRange;
        int band;
        long rounds;
        long entries;
        long removals;
        long bytes;
        long nanos;
        long maxNanos;
        int[] removed = new int[64];
        int[] out = new int[64 * FarCodec.ENTRY_INTS];
    }

    private FarServer() {
    }

    /** V predinicializaciji: kanal in prijava na vodila. */
    public static void init() {
        channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(FarCodec.CHANNEL);
        Events events = new Events();
        channel.register(events);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(events);
    }

    public static FMLEventChannel channel() {
        return channel;
    }

    /** Ob zagonu streznika iz configa; pozabi igralce prejsnjega streznika. */
    public static void start(int configMode, int configRange) {
        VIEWERS.clear();
        mode = configMode >= SEND ? SEND : OFF;
        range = clampRange(configRange);
        reset(0);
    }

    public static int clampRange(int r) {
        return Math.max(16, Math.min(1024, r));
    }

    public static int mode() {
        return mode;
    }

    public static int range() {
        return range;
    }

    /** Med tekom ({@code /rwfar}). Izklop poslje CLEAR vsem klientom; ponoven vklop poslje vse znova. */
    public static void setMode(MinecraftServer server, int m) {
        int next = m >= SEND ? SEND : OFF;
        if (next == mode) {
            return;
        }
        mode = next;
        for (Map.Entry<UUID, Viewer> e : VIEWERS.entrySet()) {
            e.getValue().delta.clear();
            if (next == OFF && server != null) {
                EntityPlayerMP p = server.getPlayerList().getPlayerByUUID(e.getKey());
                if (p != null) {
                    send(p, FarCodec.clear());
                }
            }
        }
    }

    public static void setRange(int r) {
        range = clampRange(r);
    }

    public static void reset(int tick) {
        resetTick = tick;
        rounds = 0;
        entries = 0;
        removals = 0;
        messages = 0;
        bytes = 0;
        nanos = 0;
        maxNanos = 0;
        hellos = 0;
        for (Viewer v : VIEWERS.values()) {
            v.rounds = 0;
            v.entries = 0;
            v.removals = 0;
            v.bytes = 0;
            v.nanos = 0;
            v.maxNanos = 0;
        }
    }

    /** Dogodki (javen razred: Forge ASM klicatelj potrebuje dostop). */
    public static final class Events {
        @SubscribeEvent
        public void onServerPacket(FMLNetworkEvent.ServerCustomPacketEvent event) {
            final EntityPlayerMP player = ((NetHandlerPlayServer) event.getHandler()).player;
            final int[] msg;
            try {
                msg = FarCodec.readClientMessage(event.getPacket().payload());
            } catch (RuntimeException e) {
                LogWriter.warn("RWFAR neveljavno sporocilo od " + player.getName() + ": " + e);
                return;
            }
            if (msg == null || player.getServer() == null) {
                return;
            }
            player.getServer().addScheduledTask(() -> onClientMessage(player, msg));
        }

        @SubscribeEvent
        public void onWorldTick(TickEvent.WorldTickEvent event) {
            if (event.phase != TickEvent.Phase.END || mode == OFF || VIEWERS.isEmpty()
                    || !(event.world instanceof WorldServer)) {
                return;
            }
            tick((WorldServer) event.world);
        }

        @SubscribeEvent
        public void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            VIEWERS.remove(event.player.getUniqueID());
        }

        @SubscribeEvent
        public void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            // Klient ob novem svetu nadomestke pozabi in poslje HELLO znova; posnetek se zacne od zacetka.
            Viewer v = VIEWERS.get(event.player.getUniqueID());
            if (v != null) {
                v.delta.clear();
            }
        }
    }

    static void onClientMessage(EntityPlayerMP player, int[] msg) {
        if (msg[0] == FarCodec.BYE) {
            VIEWERS.remove(player.getUniqueID());
            return;
        }
        if (msg[1] != FarCodec.VERSION) {
            LogWriter.info("RWFAR HELLO " + player.getName() + " z neznano verzijo " + msg[1] + ", prezrt");
            return;
        }
        Viewer v = VIEWERS.get(player.getUniqueID());
        if (v == null) {
            v = new Viewer();
            VIEWERS.put(player.getUniqueID(), v);
        }
        v.name = player.getName();
        v.wantRange = msg[2];
        v.delta.clear();
        ++hellos;
        LogWriter.info("RWFAR HELLO " + v.name + " domet=" + v.wantRange + " nacin=" + mode);
    }

    static void tick(WorldServer world) {
        FarRegistry reg = FarRegistry.of(world);
        if (reg == null) {
            return;
        }
        int t = world.getMinecraftServer().getTickCounter();
        List<EntityPlayer> players = world.playerEntities;
        for (int i = 0; i < players.size(); ++i) {
            EntityPlayer p = players.get(i);
            if (!(p instanceof EntityPlayerMP) || Math.floorMod(t + p.getEntityId(), PERIOD) != 0) {
                continue;
            }
            Viewer v = VIEWERS.get(p.getUniqueID());
            if (v != null) {
                process(world, reg, (EntityPlayerMP) p, v);
            }
        }
    }

    /** En krog za enega igralca: pas, delta, posiljanje. */
    static void process(WorldServer world, FarRegistry reg, EntityPlayerMP player, Viewer v) {
        long t0 = System.nanoTime();
        int evd = world.getMinecraftServer().getPlayerList().getEntityViewDistance();
        int outer = FarBand.outer(Math.min(range, v.wantRange), evd);
        List<EntityNPCInterface> npcs = reg.npcs();
        FarDelta delta = v.delta;
        delta.begin();
        int nOut = 0;
        int band = 0;
        Class<?> lastClass = null;
        int inner = 0;
        double px = player.posX;
        double pz = player.posZ;
        for (int i = 0; i < npcs.size(); ++i) {
            EntityNPCInterface npc = npcs.get(i);
            if (npc.isDead) {
                continue;
            }
            if (npc.getClass() != lastClass) {
                lastClass = npc.getClass();
                inner = FarBand.inner(trackRange(lastClass), evd);
            }
            double dx = npc.posX - px;
            double dz = npc.posZ - pz;
            if (!FarBand.inBand(dx, dz, inner, outer) || npc.display.getVisible() == 1) {
                continue;
            }
            boolean killed = npc.isKilled();
            if (killed && npc.stats.hideKilledBody) {
                continue;
            }
            if (!world.getPlayerChunkMap().isPlayerWatchingChunk(player, npc.chunkCoordX, npc.chunkCoordZ)) {
                continue;
            }
            ++band;
            int qx = FarCodec.quantize(npc.posX);
            int qy = FarCodec.quantize(npc.posY);
            int qz = FarCodec.quantize(npc.posZ);
            int yaw = FarCodec.yawByte(npc.rotationYaw);
            int flags = flags(npc, killed);
            // Videz (M5-V.3) se do takrat ne poslje: 0 = neznan.
            if (delta.offer(npc.getEntityId(), qx, qy, qz, yaw, 0, flags)) {
                int o = nOut * FarCodec.ENTRY_INTS;
                if (o + FarCodec.ENTRY_INTS > v.out.length) {
                    v.out = java.util.Arrays.copyOf(v.out, v.out.length * 2);
                }
                v.out[o] = npc.getEntityId();
                v.out[o + 1] = qx;
                v.out[o + 2] = qy;
                v.out[o + 3] = qz;
                v.out[o + 4] = yaw;
                v.out[o + 5] = 0;
                v.out[o + 6] = flags;
                ++nOut;
            }
        }
        int[] count = new int[1];
        v.removed = delta.end(v.removed, count);
        int nRemoved = count[0];
        long sentBytes = 0L;
        for (ByteBuf buf : FarCodec.snaps(v.removed, nRemoved, v.out, nOut)) {
            sentBytes += buf.readableBytes();
            send(player, buf);
        }
        long dt = System.nanoTime() - t0;
        v.band = band;
        ++v.rounds;
        v.entries += nOut;
        v.removals += nRemoved;
        v.bytes += sentBytes;
        v.nanos += dt;
        v.maxNanos = Math.max(v.maxNanos, dt);
        ++rounds;
        entries += nOut;
        removals += nRemoved;
        bytes += sentBytes;
        nanos += dt;
        maxNanos = Math.max(maxNanos, dt);
    }

    static int flags(EntityNPCInterface npc, boolean killed) {
        int f = 0;
        double mx = npc.posX - npc.prevPosX;
        double mz = npc.posZ - npc.prevPosZ;
        if (mx * mx + mz * mz > 1.0e-4) {
            f |= FarCodec.F_WALKING;
        }
        if (npc.currentAnimation == 1) {
            f |= FarCodec.F_SITTING;
        } else if (npc.currentAnimation == 2) {
            f |= FarCodec.F_LYING;
        }
        if (npc.isRiding()) {
            f |= FarCodec.F_RIDING;
        }
        if (npc.display.getVisible() == 2) {
            f |= FarCodec.F_HALF_VISIBLE;
        }
        if (npc.isAttacking()) {
            f |= FarCodec.F_ATTACKING;
        }
        if (killed) {
            f |= FarCodec.F_DEAD;
        }
        return f;
    }

    private static int trackRange(Class<?> c) {
        @SuppressWarnings("unchecked")
        EntityRegistry.EntityRegistration er =
                EntityRegistry.instance().lookupModSpawn((Class<? extends Entity>) c, true);
        return er == null ? DEFAULT_TRACK_RANGE : er.getTrackingRange();
    }

    private static void send(EntityPlayerMP player, ByteBuf buf) {
        ++messages;
        channel.sendTo(new FMLProxyPacket(new PacketBuffer(buf), FarCodec.CHANNEL), player);
    }

    /** Izpis za {@code /rwfar} (marker {@code RWFAR}). */
    public static String status(MinecraftServer server) {
        int tick = server.getTickCounter();
        int ticks = Math.max(1, tick - resetTick);
        int registered = 0;
        int inWorlds = 0;
        for (WorldServer w : server.worlds) {
            if (w == null) {
                continue;
            }
            FarRegistry r = FarRegistry.of(w);
            registered += r == null ? 0 : r.size();
            for (int i = 0; i < w.loadedEntityList.size(); ++i) {
                if (w.loadedEntityList.get(i) instanceof EntityNPCInterface) {
                    ++inWorlds;
                }
            }
        }
        int viewers = VIEWERS.size();
        StringBuilder per = new StringBuilder();
        for (Viewer v : VIEWERS.values()) {
            if (per.length() > 0) {
                per.append(';');
            }
            per.append(v.name).append(':').append(v.band).append(':').append(v.delta.size()).append(':')
                    .append(v.rounds).append(':').append(v.entries).append(':').append(v.removals).append(':')
                    .append(v.bytes).append(':').append(v.nanos).append(':').append(v.maxNanos);
        }
        double usPerTickPerViewer = viewers == 0 ? 0.0 : nanos / 1000.0 / ticks / viewers;
        return "RWFAR nacin=" + mode + " domet=" + range + " sledilnikMeja=" + server.getPlayerList().getEntityViewDistance()
                + " igralcev=" + viewers + " hello=" + hellos + " registra=" + registered + " npcVSvetu=" + inWorlds
                + " krogov=" + rounds + " vnosov=" + entries + " odstranitev=" + removals + " sporocil=" + messages
                + " bajtov=" + bytes + " ns=" + nanos + " nsMax=" + maxNanos + " tickov=" + ticks
                + " usNaTickNaIgralca=" + String.format(java.util.Locale.ROOT, "%.3f", usPerTickPerViewer)
                + " poIgralcih=" + per;
    }
}
