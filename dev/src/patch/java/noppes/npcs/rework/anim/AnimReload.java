package noppes.npcs.rework.anim;

import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/** P7c (D-098, Q275): the published library snapshots, one immutable table per logical side, because an
 * integrated client and server share one classloader. The coremod passes the return value of
 * {@code EntityAnimatedNpc.animations()} through {@link #library}; with no reload that is two field reads. */
public final class AnimReload {
    private static volatile Map<Object, Object> server, client;
    private static volatile int clientRevision;
    private static volatile byte[] clientHash;
    private static volatile int clientOverrides;

    private AnimReload() { }

    /** Called by the patched {@code animations()}, also during entity construction. Never throws. */
    public static Object library(Object jar, Object npc) {
        Map<Object, Object> s = server, c = client;
        if (s == null && c == null) { return jar; }
        World world = ((Entity) npc).world;
        if (world == null) { return jar; }
        Map<Object, Object> table = world.isRemote ? c : s;
        Object snapshot = table == null ? null : table.get(jar);
        return snapshot == null ? jar : snapshot;
    }

    static void publishServer(Map<Object, Object> table) { server = table.isEmpty() ? null : table; }

    static void publishClient(int revision, byte[] hash, Map<Object, Object> table, int overrides) {
        client = table.isEmpty() ? null : table;
        clientHash = hash; clientOverrides = overrides; clientRevision = revision;
    }

    /** Committed revision on this client; 0 until the first commit of a connection. */
    public static int clientRevision() { return clientRevision; }

    public static String clientHash() { return ReloadCodec.hex(clientHash); }

    public static int clientOverrides() { return clientOverrides; }
}
