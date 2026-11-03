package noppes.npcs.rework.anim;

import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.LogWriter;

/** P7c (D-098, Q275, Q287): client half of {@link ReloadServer}. Client main thread only, in connection
 * order. A complete bundle is checked and built aside and confirmed; only the matching commit stops the
 * changed NPCs' actions, whose indices belong to the old table, and publishes the new table. */
@SideOnly(Side.CLIENT)
public final class ReloadClient {
    private static ReloadCodec.Assembler assembler = new ReloadCodec.Assembler();
    private static TreeMap<String, byte[]> current = new TreeMap<>();
    private static TreeMap<String, byte[]> pendingFiles;
    private static Map<Object, Object> pendingTable;
    private static int pendingRevision;
    private static byte[] pendingHash;
    private static volatile boolean fault;

    private ReloadClient() { }

    /** Harness only: refuse every following prepare. */
    public static void setFault(boolean on) { fault = on; }

    static void handle(ReloadCodec.Message m) {
        switch (m.kind) {
            case ReloadCodec.PART: {
                byte[] whole = assembler.accept(m);
                if (whole != null) { prepare(m.revision, whole); }
                break;
            }
            case ReloadCodec.COMMIT: commit(m); break;
            case ReloadCodec.ABORT:
                if (m.revision == pendingRevision) { clearPending(); }
                break;
            default: break;
        }
    }

    private static void prepare(int revision, byte[] bundle) {
        byte[] hash = ReloadCodec.hash(bundle);
        clearPending();
        boolean ok = false;
        try {
            TreeMap<String, byte[]> files = fault ? null : ReloadCodec.decodeBundle(bundle);
            String problem = files == null ? "malformed or refused bundle" : AnimLibraries.check(files);
            if (problem != null) {
                LogWriter.error("NPCANIM client refused revision " + revision + ": " + problem);
            } else {
                pendingTable = AnimLibraries.build(files);
                pendingFiles = files; pendingRevision = revision; pendingHash = hash;
                ok = true;
            }
        } catch (ReflectiveOperationException | RuntimeException failure) {
            LogWriter.error("NPCANIM client could not prepare revision " + revision, failure);
            clearPending();
        }
        BlendServer.channel().sendToServer(new FMLProxyPacket(new PacketBuffer(Unpooled.wrappedBuffer(ReloadCodec.ack(revision, ok, hash))), BlendCodec.CHANNEL));
    }

    private static void commit(ReloadCodec.Message m) {
        if (pendingTable == null || m.revision != pendingRevision || !Arrays.equals(m.hash, pendingHash)) { return; }
        Set<String> changed = AnimLibraries.changed(current, pendingFiles);
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (mc.world != null) { AnimLibraries.stop(mc.world, changed); }
        } catch (ReflectiveOperationException failure) {
            LogWriter.error("NPCANIM client could not stop running actions", failure);
        }
        current = pendingFiles;
        AnimReload.publishClient(pendingRevision, pendingHash, pendingTable, current.size());
        clearPending();
    }

    private static void clearPending() { pendingTable = null; pendingFiles = null; pendingRevision = 0; pendingHash = null; }

    /** Disconnect: the state belongs to one connection. */
    static void disconnected() {
        assembler = new ReloadCodec.Assembler();
        current = new TreeMap<>();
        clearPending();
        AnimReload.publishClient(0, null, Collections.emptyMap(), 0);
    }
}
