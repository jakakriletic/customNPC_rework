package noppes.npcs.rework.army;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/** M11.8a: bounded, independent messages on rwarmy; snapshot v1 remains unchanged. */
public final class ArmyStaffCodec {
    public static final int REQUEST = 3, RESPONSE = 4, MAX_REQUEST = 46, MAX_RESPONSE = 2094;
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
    private ArmyStaffCodec() { }
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
