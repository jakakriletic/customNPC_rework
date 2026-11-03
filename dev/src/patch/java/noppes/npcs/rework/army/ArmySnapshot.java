package noppes.npcs.rework.army;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * M11.2: what one player may see of the armies at one moment, and its two messages on the {@value #CHANNEL} channel
 * (Q317): a request from the client and the snapshot as the answer. Without game state, so the codec and the build
 * are unit tested. Built from {@link ArmyRegistry#visible} only (Q313), so a name on an NPC without a visible
 * registered army (Q305) never reaches a client.
 */
public final class ArmySnapshot {
    public static final String CHANNEL = "rwarmy";
    static final byte VERSION = 1;
    static final byte KIND_REQUEST = 1;
    static final byte KIND_SNAPSHOT = 2;
    /** version, kind, request id. */
    public static final int REQUEST_SIZE = 1 + 1 + 4;
    /** Largest clusters per army; the rest is only counted. */
    public static final int MAX_CLUSTERS = 16;
    public static final int MAX_OWNER_NAME = 32;
    /** 1024 armies with 16 clusters each stay well below this; a larger message is refused on both sides. */
    public static final int MAX_MESSAGE = 1000000;
    /** Blocks the main cluster has to move before the last known position changes and the register is saved. */
    public static final int LAST_MOVE = 16;

    /** One army of the snapshot. Without a last known position {@link #lastSeen} is negative. */
    public static final class Entry {
        public final String name, ownerName;
        public final boolean command;
        public final int loaded, omittedClusters, omittedMembers;
        public final List<ArmyClusters.Cluster> clusters;
        public final int lastDimension, lastX, lastY, lastZ;
        public final long lastSeen;

        Entry(String name, String ownerName, boolean command, int loaded, int omittedClusters, int omittedMembers,
                List<ArmyClusters.Cluster> clusters, int lastDimension, int lastX, int lastY, int lastZ,
                long lastSeen) {
            this.name = name;
            this.ownerName = ownerName;
            this.command = command;
            this.loaded = loaded;
            this.omittedClusters = omittedClusters;
            this.omittedMembers = omittedMembers;
            this.clusters = clusters;
            this.lastDimension = lastDimension;
            this.lastX = lastX;
            this.lastY = lastY;
            this.lastZ = lastZ;
            this.lastSeen = lastSeen;
        }

        public boolean hasLast() {
            return lastSeen >= 0;
        }
    }

    public final int requestId, radius, minMembers;
    public final long worldTime;
    public final List<Entry> armies;
    /** True when building moved a last known position, so the register has to be saved. */
    final boolean lastMoved;

    ArmySnapshot(int requestId, long worldTime, int radius, int minMembers, List<Entry> armies, boolean lastMoved) {
        this.requestId = requestId;
        this.worldTime = worldTime;
        this.radius = radius;
        this.minMembers = minMembers;
        this.armies = armies;
        this.lastMoved = lastMoved;
    }

    /** The entry of this army (any case) or null. */
    public Entry entry(String name) {
        for (Entry e : armies) {
            if (e.name.equalsIgnoreCase(name)) {
                return e;
            }
        }
        return null;
    }

    /**
     * @param byKey member positions by {@link Army#key}; other keys, as orphan names, are ignored
     */
    static ArmySnapshot build(int requestId, long worldTime, int radius, int minMembers, ArmyRegistry registry,
            ArmyActor actor, Map<String, List<ArmyClusters.Point>> byKey) {
        List<Entry> entries = new ArrayList<Entry>();
        boolean moved = false;
        for (Army listed : registry.visible(actor)) {
            List<ArmyClusters.Point> points = byKey.get(listed.key());
            List<ArmyClusters.Cluster> all = points == null ? new ArrayList<ArmyClusters.Cluster>()
                    : ArmyClusters.cluster(points, radius, minMembers);
            if (!all.isEmpty()) {
                ArmyClusters.Cluster main = all.get(0);
                moved |= registry.noteSeen(listed.key(), main.dimension, main.x, main.y, main.z, worldTime);
            }
            Army a = registry.get(listed.key());
            int kept = Math.min(all.size(), MAX_CLUSTERS);
            int omittedMembers = 0;
            for (ArmyClusters.Cluster c : all.subList(kept, all.size())) {
                omittedMembers += c.size;
            }
            String owner = a.ownerName();
            entries.add(new Entry(a.name(), owner.length() > MAX_OWNER_NAME ? owner.substring(0, MAX_OWNER_NAME) : owner,
                    registry.access(a, actor).canCommand(), points == null ? 0 : points.size(), all.size() - kept,
                    omittedMembers, new ArrayList<ArmyClusters.Cluster>(all.subList(0, kept)), a.lastDimension(),
                    a.lastX(), a.lastY(), a.lastZ(), a.lastSeen()));
        }
        return new ArmySnapshot(requestId, worldTime, radius, minMembers, entries, moved);
    }

    public static byte[] encodeRequest(int requestId) {
        return ByteBuffer.allocate(REQUEST_SIZE).put(VERSION).put(KIND_REQUEST).putInt(requestId).array();
    }

    /** The request id, or -1 for a wrong size, version, kind or a negative id. */
    public static int decodeRequest(byte[] data) {
        if (data == null || data.length != REQUEST_SIZE) {
            return -1;
        }
        ByteBuffer in = ByteBuffer.wrap(data);
        if (in.get() != VERSION || in.get() != KIND_REQUEST) {
            return -1;
        }
        int id = in.getInt();
        return id >= 0 ? id : -1;
    }

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(256);
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(VERSION);
            out.writeByte(KIND_SNAPSHOT);
            out.writeInt(requestId);
            out.writeLong(worldTime);
            out.writeShort(radius);
            out.writeShort(minMembers);
            out.writeShort(armies.size());
            for (Entry e : armies) {
                writeText(out, e.name);
                writeText(out, e.ownerName);
                out.writeBoolean(e.command);
                out.writeInt(e.loaded);
                out.writeInt(e.omittedClusters);
                out.writeInt(e.omittedMembers);
                out.writeLong(e.lastSeen);
                if (e.hasLast()) {
                    out.writeInt(e.lastDimension);
                    out.writeInt(e.lastX);
                    out.writeInt(e.lastY);
                    out.writeInt(e.lastZ);
                }
                out.writeByte(e.clusters.size());
                for (ArmyClusters.Cluster c : e.clusters) {
                    out.writeByte(c.kind.ordinal());
                    out.writeInt(c.dimension);
                    out.writeInt(c.x);
                    out.writeInt(c.y);
                    out.writeInt(c.z);
                    out.writeInt(c.size);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        if (bytes.size() > MAX_MESSAGE) {
            throw new IllegalStateException("army snapshot of " + bytes.size() + " bytes");
        }
        return bytes.toByteArray();
    }

    /** The snapshot, or null for anything malformed: size, version, counts, names or leftover bytes. */
    public static ArmySnapshot decode(byte[] data) {
        if (data == null || data.length < 2 || data.length > MAX_MESSAGE) {
            return null;
        }
        try {
            ByteBuffer in = ByteBuffer.wrap(data);
            if (in.get() != VERSION || in.get() != KIND_SNAPSHOT) {
                return null;
            }
            int requestId = in.getInt();
            long worldTime = in.getLong();
            int radius = in.getShort();
            int minMembers = in.getShort();
            int count = in.getShort();
            if (requestId < 0 || radius < 1 || minMembers < 1 || count < 0 || count > ArmyRegistry.MAX_ARMIES) {
                return null;
            }
            List<Entry> armies = new ArrayList<Entry>(count);
            for (int i = 0; i < count; i++) {
                Entry e = readEntry(in);
                if (e == null) {
                    return null;
                }
                armies.add(e);
            }
            return in.hasRemaining() ? null : new ArmySnapshot(requestId, worldTime, radius, minMembers, armies, false);
        } catch (BufferUnderflowException | IllegalArgumentException e) {
            return null;
        }
    }

    private static Entry readEntry(ByteBuffer in) {
        String name = readText(in);
        String owner = readText(in);
        boolean command = in.get() != 0;
        int loaded = in.getInt();
        int omittedClusters = in.getInt();
        int omittedMembers = in.getInt();
        long lastSeen = in.getLong();
        int dimension = 0, x = 0, y = 0, z = 0;
        if (lastSeen >= 0) {
            dimension = in.getInt();
            x = in.getInt();
            y = in.getInt();
            z = in.getInt();
        }
        int clusters = in.get();
        if (!Army.validName(name) || owner.length() > MAX_OWNER_NAME || loaded < 0 || omittedClusters < 0
                || omittedMembers < 0 || clusters < 0 || clusters > MAX_CLUSTERS) {
            return null;
        }
        List<ArmyClusters.Cluster> list = new ArrayList<ArmyClusters.Cluster>(clusters);
        long members = omittedMembers;
        for (int i = 0; i < clusters; i++) {
            ArmyClusters.Kind kind = ArmyClusters.Kind.of(in.get());
            int d = in.getInt(), cx = in.getInt(), cy = in.getInt(), cz = in.getInt(), size = in.getInt();
            if (kind == null || size < 1 || (kind == ArmyClusters.Kind.MAIN) != (i == 0)) {
                return null;
            }
            members += size;
            list.add(new ArmyClusters.Cluster(kind, d, cx, cy, cz, size));
        }
        return members == loaded ? new Entry(name, owner, command, loaded, omittedClusters, omittedMembers, list,
                dimension, x, y, z, lastSeen) : null;
    }

    private static void writeText(DataOutputStream out, String text) throws IOException {
        byte[] utf = text.getBytes(StandardCharsets.UTF_8);
        out.writeShort(utf.length);
        out.write(utf);
    }

    private static String readText(ByteBuffer in) {
        int length = in.getShort();
        if (length < 0 || length > in.remaining()) {
            throw new IllegalArgumentException("text length");
        }
        byte[] utf = new byte[length];
        in.get(utf);
        return new String(utf, StandardCharsets.UTF_8);
    }
}
