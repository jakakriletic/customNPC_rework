package noppes.npcs.rework.army;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/** M11.8a: bounded, independent messages on rwarmy; snapshot v1 remains unchanged. M11.8b adds kind 5 (rights). */
public final class ArmyStaffCodec {
    public static final int REQUEST = 3, RESPONSE = 4, MAX_REQUEST = 46, MAX_RESPONSE = 2094;
    public static final int PERM_REQUEST = 5, MAX_PERM_REQUEST = 58;
    public enum PermOp { PLAYERS, GRANTS, GRANT, REVOKE }
    /** Right of a grant/revoke; ALL only for revoke (both rights). */
    public static final String[] KINDS = {"view", "command", "all"};
    private static final java.util.regex.Pattern PLAYER = java.util.regex.Pattern.compile("[A-Za-z0-9_]{1,16}");
    public static boolean validPlayer(String name) { return name != null && PLAYER.matcher(name).matches(); }
    public enum Op { INSPECT, ASSIGN, NEAR, MOVE, STOP, RETURN, STATUS }
    public static final String[] SHAPES = {"legion", "column", "ring", "swarm"};
    public static final String[] STANCES = {"passive", "engage"};
    public static final class Request {
        public final int id, npc, shape, stance;
        public final Op op;
        public final String army;
        public Request(int id, Op op, int npc, String army, int shape, int stance) {
            this.id = id; this.op = op; this.npc = npc; this.army = army; this.shape = shape; this.stance = stance;
        }
        public byte[] encode() {
            if (id < 0 || op == null || army == null || (!army.isEmpty() && !Army.validName(army))
                    || shape < 0 || shape >= SHAPES.length || stance < 0 || stance >= STANCES.length) {
                throw new IllegalArgumentException("invalid staff request");
            }
            byte[] name = army.getBytes(StandardCharsets.US_ASCII);
            return ByteBuffer.allocate(14 + name.length).put((byte)1).put((byte)REQUEST).putInt(id)
                    .put((byte)op.ordinal()).putInt(npc).put((byte)shape).put((byte)stance).put((byte)name.length)
                    .put(name).array();
        }
    }
    public static final class Response {
        public final int id, npc;
        public final boolean ok;
        public final String army, text;
        public Response(int id, boolean ok, int npc, String army, String text) {
            this.id = id; this.ok = ok; this.npc = npc; this.army = army; this.text = text;
        }
        public byte[] encode() {
            byte[] name = army.getBytes(StandardCharsets.US_ASCII), message = text.getBytes(StandardCharsets.UTF_8);
            if (id < 0 || name.length > 32 || message.length > 2048) { throw new IllegalArgumentException(); }
            return ByteBuffer.allocate(14 + name.length + message.length).put((byte)1).put((byte)RESPONSE)
                    .putInt(id).put((byte)(ok ? 1 : 0)).putInt(npc).put((byte)name.length).put(name)
                    .putShort((short)message.length).put(message).array();
        }
    }
    /** M11.8b: one rights action for the Permissions tab; the server maps it to /rwarmy grant/revoke/grants/players. */
    public static final class PermRequest {
        public final int id, kind;
        public final PermOp op;
        public final String player, army;
        public PermRequest(int id, PermOp op, int kind, String player, String army) {
            this.id = id; this.op = op; this.kind = kind; this.player = player; this.army = army;
        }
        public byte[] encode() {
            if (id < 0 || op == null || kind < 0 || kind >= KINDS.length || player == null || army == null
                    || (!player.isEmpty() && !validPlayer(player)) || (!army.isEmpty() && !Army.validName(army))) {
                throw new IllegalArgumentException("invalid permission request");
            }
            byte[] who = player.getBytes(StandardCharsets.US_ASCII), name = army.getBytes(StandardCharsets.US_ASCII);
            return ByteBuffer.allocate(10 + who.length + name.length).put((byte)1).put((byte)PERM_REQUEST).putInt(id)
                    .put((byte)op.ordinal()).put((byte)kind).put((byte)who.length).put(who)
                    .put((byte)name.length).put(name).array();
        }
    }
    private ArmyStaffCodec() { }
    public static PermRequest permRequest(byte[] data) {
        if (kind(data) != PERM_REQUEST || data.length < 10 || data.length > MAX_PERM_REQUEST) { return null; }
        try {
            ByteBuffer b = ByteBuffer.wrap(data); b.position(2);
            int id = b.getInt(), op = b.get() & 255, kind = b.get() & 255, length = b.get() & 255;
            if (id < 0 || op >= PermOp.values().length || kind >= KINDS.length || length > 16 || length + 1 > b.remaining()) { return null; }
            String player = ascii(b, length);
            int size = b.get() & 255;
            if (player == null || size > 32 || size != b.remaining()) { return null; }
            String army = ascii(b, size);
            if (army == null || (!player.isEmpty() && !validPlayer(player)) || (!army.isEmpty() && !Army.validName(army))) { return null; }
            return new PermRequest(id, PermOp.values()[op], kind, player, army);
        } catch (RuntimeException invalid) { return null; }
    }
    private static String ascii(ByteBuffer b, int length) {
        byte[] bytes = new byte[length]; b.get(bytes);
        for (byte c : bytes) { if (c < 0) { return null; } }
        return new String(bytes, StandardCharsets.US_ASCII);
    }
    public static int kind(byte[] data) { return data != null && data.length >= 2 && data[0] == 1 ? data[1] : -1; }
    public static Request request(byte[] data) {
        if (kind(data) != REQUEST || data.length < 14 || data.length > MAX_REQUEST) { return null; }
        try {
            ByteBuffer b = ByteBuffer.wrap(data); b.position(2);
            int id = b.getInt(), op = b.get() & 255, npc = b.getInt(), shape = b.get() & 255, stance = b.get() & 255;
            int length = b.get() & 255;
            if (id < 0 || op >= Op.values().length || shape >= SHAPES.length || stance >= STANCES.length
                    || length > 32 || length != b.remaining()) { return null; }
            byte[] name = new byte[length]; b.get(name);
            for (byte c : name) { if (c < 0) { return null; } }
            String army = new String(name, StandardCharsets.US_ASCII);
            if (!army.isEmpty() && !Army.validName(army)) { return null; }
            return new Request(id, Op.values()[op], npc, army, shape, stance);
        } catch (RuntimeException invalid) { return null; }
    }
    public static Response response(byte[] data) {
        if (kind(data) != RESPONSE || data.length < 14 || data.length > MAX_RESPONSE) { return null; }
        try {
            ByteBuffer b = ByteBuffer.wrap(data); b.position(2);
            int id = b.getInt(), ok = b.get() & 255, npc = b.getInt(), length = b.get() & 255;
            if (id < 0 || ok > 1 || length > 32 || length > b.remaining() - 2) { return null; }
            byte[] name = new byte[length]; b.get(name);
            String army = new String(name, StandardCharsets.US_ASCII);
            if (!army.isEmpty() && !Army.validName(army)) { return null; }
            int size = b.getShort() & 65535;
            if (size > 2048 || size != b.remaining()) { return null; }
            byte[] message = new byte[size]; b.get(message);
            return new Response(id, ok == 1, npc, army, new String(message, StandardCharsets.UTF_8));
        } catch (RuntimeException invalid) { return null; }
    }
}
