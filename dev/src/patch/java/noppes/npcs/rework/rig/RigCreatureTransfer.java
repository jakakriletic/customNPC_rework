package noppes.npcs.rework.rig;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import noppes.npcs.LogWriter;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.rework.anim.ReloadCodec;
import noppes.npcs.rework.anim.ReloadTxn;

/** Server prepares one revision, waits for clients, and commits it in network order. */
public final class RigCreatureTransfer {
    public static final String CHANNEL = "rwcreature";
    private static RigCreatureTransfer instance;
    private final FMLEventChannel channel;
    private int revision, nextRevision = 1;
    private ReloadTxn transaction;
    private RigCreatureAssets.Snapshot proposed;
    private ICommandSender requester;
    private String deployId;
    private Runnable afterCommit;
    private final Map<UUID, ReloadTxn> joining = new HashMap<>();
    private final Set<UUID> prepared = new HashSet<>();

    private RigCreatureTransfer() {
        channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(CHANNEL);
        channel.register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    static void install() {
        if (instance == null) { instance = new RigCreatureTransfer(); }
    }

    public static FMLEventChannel channel() { return instance.channel; }

    public static void start() {
        if (instance == null) { return; }
        try {
            RigCreatureAssets.publishServer(RigCreatureAssets.read(true));
            if (!RigCreatureAssets.server().models.isEmpty()) { instance.revision = instance.nextRevision++; }
        } catch (IOException | RuntimeException failure) {
            LogWriter.error("RWRIG creatures could not load at startup", failure);
        }
    }

    public static void stop() {
        if (instance == null) { return; }
        instance.revision = 0;
        instance.clearTransaction();
        instance.joining.clear();
        RigCreatureAssets.clearServer();
    }

    static void reload(MinecraftServer server, ICommandSender sender) {
        instance.prepare(server, sender, null, null);
    }

    static void deploy(MinecraftServer server, ICommandSender sender, String id, Runnable spawn) {
        instance.prepare(server, sender, id, spawn);
    }

    private void prepare(MinecraftServer server, ICommandSender sender, String id, Runnable spawn) {
        if (transaction != null) { reply(sender, "Creature reload already in progress."); return; }
        try {
            RigCreatureAssets.Snapshot snapshot = RigCreatureAssets.read(false);
            if (id != null && !snapshot.packs.containsKey(id)) {
                reply(sender, "Unknown creature pack " + id);
                return;
            }
            requester = sender;
            proposed = snapshot;
            deployId = id;
            afterCommit = spawn;
            transaction = new ReloadTxn(nextRevision++, snapshot.hash, server.getTickCounter() + 200);
            for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
                transaction.expect(player.getUniqueID(), player.getName());
                prepared.add(player.getUniqueID());
                prepareClient(player, transaction.revision, snapshot.data);
            }
            evaluate(server);
        } catch (IOException | RuntimeException failure) {
            if (transaction != null) { abort(server); }
            reply(sender, "Creature reload refused: " + failure.getMessage());
        }
    }

    private void clearTransaction() {
        transaction = null;
        proposed = null;
        requester = null;
        deployId = null;
        afterCommit = null;
        prepared.clear();
    }

    private void abort(MinecraftServer server) {
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            if (prepared.contains(player.getUniqueID())) { send(player, ReloadCodec.abort(transaction.revision)); }
        }
        clearTransaction();
    }

    private void evaluate(MinecraftServer server) {
        if (transaction == null) { return; }
        switch (transaction.state(server.getTickCounter())) {
            case ABORT:
                ICommandSender failed = requester;
                String reason = transaction.failure();
                abort(server);
                reply(failed, "Creature reload refused: " + reason);
                break;
            case COMMIT: commit(server); break;
            default: break;
        }
    }

    private void commit(MinecraftServer server) {
        int committed = transaction.revision;
        RigCreatureAssets.Snapshot snapshot = proposed;
        ICommandSender sender = requester;
        String id = deployId;
        Runnable spawn = afterCommit;
        Set<UUID> clients = new HashSet<>(prepared);
        clearTransaction();
        for (WorldServer world : server.worlds) {
            for (Entity entity : new ArrayList<>(world.loadedEntityList)) {
                if (entity instanceof EntityNPCInterface && ((EntityNPCInterface)entity).rwRig.active()) {
                    EntityNPCInterface npc = (EntityNPCInterface)entity;
                    if (RigCreatureAssets.server().models.containsKey(npc.rwRig.model) || snapshot.models.containsKey(npc.rwRig.model)) {
                        npc.rwRigPlayer.stop(npc);
                    }
                }
            }
        }
        RigCreatureAssets.publishServer(snapshot);
        revision = committed;
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            if (clients.contains(player.getUniqueID())) {
                send(player, ReloadCodec.commit(revision, snapshot.hash));
                joining.remove(player.getUniqueID());
            } else {
                synchronize(server, player);
            }
        }
        reply(sender, "Creature reload complete: " + snapshot.packs.size() + " packs, revision " + revision + ".");
        if (spawn != null && sender.canUseCommand(2, "rwcreature")
                && (!(sender instanceof EntityPlayerMP) || !((EntityPlayerMP)sender).hasDisconnected())) {
            spawn.run();
        } else if (id != null) {
            reply(sender, "Deployment could not spawn " + id + " after permissions or connection changed.");
        }
    }

    private void synchronize(MinecraftServer server, EntityPlayerMP player) {
        RigCreatureAssets.Snapshot current = RigCreatureAssets.server();
        ReloadTxn confirmation = new ReloadTxn(revision, current.hash, server.getTickCounter() + 1200);
        confirmation.expect(player.getUniqueID(), player.getName());
        joining.put(player.getUniqueID(), confirmation);
        prepareClient(player, revision, current.data);
        send(player, ReloadCodec.commit(revision, current.hash));
    }

    @SubscribeEvent public void joined(PlayerEvent.PlayerLoggedInEvent event) {
        if (revision != 0 && event.player instanceof EntityPlayerMP) {
            synchronize(event.player.getServer(), (EntityPlayerMP)event.player);
        }
    }

    @SubscribeEvent public void left(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.player.getUniqueID();
        joining.remove(id);
        if (transaction != null) {
            transaction.left(id);
            prepared.remove(id);
        }
    }

    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || (transaction == null && joining.isEmpty())) { return; }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) { return; }
        evaluate(server);
        Iterator<Map.Entry<UUID, ReloadTxn>> pending = joining.entrySet().iterator();
        while (pending.hasNext()) {
            Map.Entry<UUID, ReloadTxn> entry = pending.next();
            ReloadTxn.State state = entry.getValue().state(server.getTickCounter());
            if (state == ReloadTxn.State.WAITING) { continue; }
            pending.remove();
            if (state == ReloadTxn.State.ABORT) {
                EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(entry.getKey());
                if (player != null) { player.connection.disconnect(new TextComponentString("Could not confirm CustomNPCs creature assets.")); }
            }
        }
    }

    @SubscribeEvent public void packet(FMLNetworkEvent.ServerCustomPacketEvent event) {
        if (!(event.getHandler() instanceof NetHandlerPlayServer)) { return; }
        ByteBuf payload = event.getPacket().payload();
        if (payload.readableBytes() != 6 + ReloadCodec.HASH_SIZE) { return; }
        byte[] bytes = new byte[payload.readableBytes()];
        payload.getBytes(payload.readerIndex(), bytes);
        ReloadCodec.Message ack = ReloadCodec.decode(bytes);
        if (ack == null || ack.kind != ReloadCodec.ACK) { return; }
        EntityPlayerMP player = ((NetHandlerPlayServer)event.getHandler()).player;
        MinecraftServer server = player.getServer();
        if (server == null) { return; }
        server.addScheduledTask(() -> {
            if (player.hasDisconnected()) { return; }
            UUID id = player.getUniqueID();
            ReloadTxn confirmation = joining.get(id);
            if (confirmation != null) { confirmation.ack(id, ack.revision, ack.ok, ack.hash); }
            if (transaction != null) { transaction.ack(id, ack.revision, ack.ok, ack.hash); }
        });
    }

    private void prepareClient(EntityPlayerMP player, int rev, byte[] data) {
        for (byte[] part : ReloadCodec.parts(rev, data)) { send(player, part); }
    }

    private void send(EntityPlayerMP player, byte[] bytes) {
        channel.sendTo(new FMLProxyPacket(new PacketBuffer(Unpooled.wrappedBuffer(bytes)), CHANNEL), player);
    }

    private static void reply(ICommandSender sender, String message) {
        LogWriter.info("RWRIG " + message);
        sender.sendMessage(new TextComponentString("RWRIG " + message));
    }
}
