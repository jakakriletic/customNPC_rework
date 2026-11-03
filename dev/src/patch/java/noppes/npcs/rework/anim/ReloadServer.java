package noppes.npcs.rework.anim;

import io.netty.buffer.ByteBuf;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import noppes.npcs.LogWriter;

/** P7c (D-098, Q275, Q287): {@code /npcanim reload} as a transaction. All target files are read and fully
 * checked first; any fault leaves everything as it was. Every online player then prepares the whole
 * override state and confirms it; only after all have, the server stops the changed NPCs' actions and
 * queues, swaps its own table and sends the commit, so a player gets every later action index after the
 * commit on the same ordered connection. A refusal or the deadline aborts with nothing changed. A player
 * who joins later gets the current state before any entity and is disconnected without a confirmation,
 * as its clip indices would be wrong. Kept for this server session only. */
public final class ReloadServer {
    static final int CONFIRM_TICKS = 200, JOIN_CONFIRM_TICKS = 1200;
    private static ReloadServer installed;

    private TreeMap<String, byte[]> current = new TreeMap<>();
    private int revision, nextRevision = 1;
    private byte[] hash;
    private ReloadTxn txn;
    private TreeMap<String, byte[]> proposed;
    private Map<Object, Object> proposedTable;
    private ICommandSender requester;
    private final Set<UUID> prepared = new HashSet<>();
    private final Map<UUID, long[]> joining = new HashMap<>();

    private ReloadServer() { }

    /** Pre-init, only with RwAnimHooks>=1, after {@link BlendServer#init}. */
    public static synchronized void install() {
        if (installed != null) { return; }
        installed = new ReloadServer();
        BlendServer.channel().register(installed);
        MinecraftForge.EVENT_BUS.register(installed);
    }

    /** Server stopped: the state lived for this session only. */
    public static synchronized void stopped() {
        if (installed == null) { return; }
        AnimReload.publishServer(Collections.emptyMap());
        installed.current = new TreeMap<>();
        installed.revision = 0;
        installed.hash = null;
        installed.endTxn();
        installed.prepared.clear();
        installed.joining.clear();
    }

    static ReloadServer instance() { return installed; }

    private static File directory() { return new File(Loader.instance().getConfigDir(), "customnpcs/animations"); }

    private static void reply(ICommandSender to, String line) {
        LogWriter.info(line);
        if (to != null) { to.sendMessage(new TextComponentString(line)); }
    }

    private static long now(MinecraftServer server) { return server.getTickCounter(); }

    private static String names(Set<String> ids) { return ids.isEmpty() ? "none" : String.join(",", new TreeSet<>(ids)); }

    private void endTxn() { txn = null; proposed = null; proposedTable = null; requester = null; }

    void status(ICommandSender sender) {
        reply(sender, "NPCANIM STATUS rev=" + revision + " hash=" + ReloadCodec.hex(hash) + " overrides=" + names(current.keySet())
                + " busy=" + (txn == null ? 0 : 1));
    }

    /** Server thread. @param id one entity id, or null for every file plus every current override */
    void reload(MinecraftServer server, ICommandSender sender, String id) {
        if (txn != null) { reply(sender, "NPCANIM ERROR reload already in progress"); return; }
        try {
            if (!AnimLibraries.available()) { reply(sender, "NPCANIM ERROR customnpcentities is not loaded"); return; }
            File dir = directory();
            Set<String> targets = new HashSet<>();
            if (id != null) {
                if (!AnimLibraries.known(id)) { reply(sender, "NPCANIM ERROR unknown entity id " + id); return; }
                targets.add(id);
            } else {
                targets.addAll(current.keySet());
                File[] files = dir.listFiles();
                for (File f : files == null ? new File[0] : files) {
                    if (!f.isFile() || !f.getName().endsWith(".animation.json")) { continue; }
                    String name = f.getName().substring(0, f.getName().length() - ".animation.json".length());
                    if (!AnimLibraries.known(name)) { reply(sender, "NPCANIM ERROR file=" + f.getName() + " unknown entity id"); return; }
                    targets.add(name);
                }
            }
            TreeMap<String, byte[]> next = new TreeMap<>(current);
            for (String target : targets) {
                File f = new File(dir, target + ".animation.json");
                if (!f.isFile()) { next.remove(target); continue; }
                if (f.length() > ReloadCodec.MAX_FILE) { reply(sender, "NPCANIM ERROR file=" + f.getName() + " larger than " + ReloadCodec.MAX_FILE + " bytes"); return; }
                next.put(target, Files.readAllBytes(f.toPath()));
            }
            String problem = AnimLibraries.check(next);
            if (problem != null) {
                String bad = problem.substring(0, problem.indexOf(':'));
                reply(sender, "NPCANIM ERROR file=" + bad + ".animation.json path=" + problem.substring(bad.length() + 2));
                return;
            }
            byte[] bundle = ReloadCodec.encodeBundle(next);
            if (bundle.length > ReloadCodec.MAX_BUNDLE) { reply(sender, "NPCANIM ERROR all override files together are too large"); return; }
            Map<Object, Object> table = AnimLibraries.build(next);
            int rev = nextRevision++;
            byte[] h = ReloadCodec.hash(bundle);
            txn = new ReloadTxn(rev, h, now(server) + CONFIRM_TICKS);
            proposed = next; proposedTable = table; requester = sender;
            prepared.clear();
            for (EntityPlayerMP player : new ArrayList<>(server.getPlayerList().getPlayers())) {
                txn.expect(player.getUniqueID(), player.getName());
                prepared.add(player.getUniqueID());
                sendParts(player, rev, bundle);
            }
            LogWriter.info("NPCANIM PREPARE rev=" + rev + " hash=" + ReloadCodec.hex(h) + " players=" + prepared.size());
            evaluate(server);
        } catch (IOException | ReflectiveOperationException | RuntimeException failure) {
            LogWriter.error("NPCANIM reload failed", failure);
            reply(sender, "NPCANIM ERROR " + failure);
            endTxn();
        }
    }

    private static void sendParts(EntityPlayerMP player, int rev, byte[] bundle) {
        for (byte[] part : ReloadCodec.parts(rev, bundle)) { BlendServer.sendRaw(player, part); }
    }

    private void evaluate(MinecraftServer server) {
        if (txn == null) { return; }
        ReloadTxn.State state = txn.state(now(server));
        if (state == ReloadTxn.State.COMMIT) { commit(server); }
        else if (state == ReloadTxn.State.ABORT) { abort(server); }
    }

    private void abort(MinecraftServer server) {
        ReloadTxn failed = txn;
        ICommandSender to = requester;
        for (EntityPlayerMP player : new ArrayList<>(server.getPlayerList().getPlayers())) {
            if (prepared.contains(player.getUniqueID())) { BlendServer.sendRaw(player, ReloadCodec.abort(failed.revision)); }
        }
        endTxn();
        reply(to, "NPCANIM ERROR not confirmed: " + failed.failure() + "; nothing changed");
    }

    private void commit(MinecraftServer server) {
        ReloadTxn done = txn;
        TreeMap<String, byte[]> next = proposed;
        Map<Object, Object> table = proposedTable;
        ICommandSender to = requester;
        endTxn();
        Set<String> changed = AnimLibraries.changed(current, next);
        int stopped = 0;
        try {
            // The hooks' journal names the ended clip now, from the old table.
            for (WorldServer world : server.worlds) { stopped += AnimLibraries.stop(world, changed); }
        } catch (ReflectiveOperationException failure) {
            LogWriter.error("NPCANIM could not stop running actions", failure);
        }
        AnimReload.publishServer(table);
        current = next; revision = done.revision; hash = done.hash;
        byte[] commit = ReloadCodec.commit(revision, hash), bundle = ReloadCodec.encodeBundle(current);
        for (EntityPlayerMP player : new ArrayList<>(server.getPlayerList().getPlayers())) {
            if (prepared.contains(player.getUniqueID())) { BlendServer.sendRaw(player, commit); }
            else { sync(server, player, bundle, commit); }
        }
        reply(to, "NPCANIM OK rev=" + revision + " hash=" + ReloadCodec.hex(hash) + " overrides=" + names(current.keySet())
                + " changed=" + names(changed) + " stopped=" + stopped);
    }

    /** A player outside the transaction: the prepare and commit in order, before any later entity packet. */
    private void sync(MinecraftServer server, EntityPlayerMP player, byte[] bundle, byte[] commit) {
        sendParts(player, revision, bundle);
        BlendServer.sendRaw(player, commit);
        joining.put(player.getUniqueID(), new long[]{revision, now(server) + JOIN_CONFIRM_TICKS});
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (revision == 0 || current.isEmpty() || !(event.player instanceof EntityPlayerMP)) { return; }
        MinecraftServer server = event.player.getServer();
        if (server != null) { sync(server, (EntityPlayerMP) event.player, ReloadCodec.encodeBundle(current), ReloadCodec.commit(revision, hash)); }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        joining.remove(event.player.getUniqueID());
        if (txn != null) {
            txn.left(event.player.getUniqueID());
            prepared.remove(event.player.getUniqueID());
            MinecraftServer server = event.player.getServer();
            if (server != null) { evaluate(server); }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || (txn == null && joining.isEmpty())) { return; }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) { return; }
        evaluate(server);
        for (Iterator<Map.Entry<UUID, long[]>> i = joining.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<UUID, long[]> e = i.next();
            if (now(server) < e.getValue()[1]) { continue; }
            i.remove();
            kick(server, e.getKey(), "no confirmation");
        }
    }

    @SubscribeEvent
    public void onServerPacket(FMLNetworkEvent.ServerCustomPacketEvent event) {
        ByteBuf payload = event.getPacket().payload();
        int n = payload.readableBytes();
        if (n < 1 || n > ReloadCodec.MAX_MESSAGE || !(event.getHandler() instanceof NetHandlerPlayServer)) { return; }
        byte[] data = new byte[n];
        payload.getBytes(payload.readerIndex(), data);
        final ReloadCodec.Message m = ReloadCodec.decode(data);
        if (m == null || m.kind != ReloadCodec.ACK) { return; }
        final EntityPlayerMP player = ((NetHandlerPlayServer) event.getHandler()).player;
        final MinecraftServer server = player.getServer();
        if (server != null) { server.addScheduledTask(() -> ack(server, player, m)); }
    }

    private void ack(MinecraftServer server, EntityPlayerMP player, ReloadCodec.Message m) {
        UUID id = player.getUniqueID();
        long[] join = joining.get(id);
        if (join != null && join[0] == m.revision) {
            joining.remove(id);
            if (!m.ok || !Arrays.equals(hash, m.hash)) { kick(server, id, "refused"); return; }
        }
        if (txn != null) {
            txn.ack(id, m.revision, m.ok, m.hash);
            evaluate(server);
        }
    }

    private void kick(MinecraftServer server, UUID id, String why) {
        EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(id);
        if (player == null) { return; }
        LogWriter.error("NPCANIM disconnecting " + player.getName() + ": animation state rev=" + revision + " " + why);
        player.connection.disconnect(new TextComponentString("This server has reloaded animations; your client could not apply them"
                + " (RwAnimHooks=1 is needed on the client)."));
    }
}
