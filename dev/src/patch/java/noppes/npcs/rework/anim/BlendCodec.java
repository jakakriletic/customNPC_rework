package noppes.npcs.rework.anim;

import java.nio.ByteBuffer;

/** P7b (D-098, Q274): the one message of the separate rework channel {@value #CHANNEL}. Fixed size,
 * checked before parsing; bound to dimension, entity id, entity UUID and the action token
 * ({@code sequence << 8 | action}, the raw value the optional mod syncs). No existing CustomNPCs
 * packet, packet ID or foreign DataParameter is changed. */
public final class BlendCodec {
    public static final String CHANNEL = "rwanim";
    static final byte VERSION = 1;
    /** version, dimension, entity id, UUID (2 longs), token, ticks. */
    public static final int SIZE = 1 + 4 + 4 + 8 + 8 + 4 + 2;

    private BlendCodec() { }

    public static final class Message {
        public final int dimension, entityId, token, ticks;
        public final long most, least;
        Message(int dimension, int entityId, long most, long least, int token, int ticks) {
            this.dimension = dimension; this.entityId = entityId; this.most = most; this.least = least;
            this.token = token; this.ticks = ticks;
        }
    }

    public static byte[] encode(int dimension, int entityId, long most, long least, int token, int ticks) {
        if (!valid(token, ticks)) { throw new IllegalArgumentException("blend token " + token + " ticks " + ticks); }
        return ByteBuffer.allocate(SIZE).put(VERSION).putInt(dimension).putInt(entityId).putLong(most).putLong(least)
                .putInt(token).putShort((short) ticks).array();
    }

    /** @return the message, or null for a wrong size, version, token or tick count */
    public static Message decode(byte[] data) {
        if (data == null || data.length != SIZE) { return null; }
        ByteBuffer in = ByteBuffer.wrap(data);
        if (in.get() != VERSION) { return null; }
        int dimension = in.getInt(), id = in.getInt();
        long most = in.getLong(), least = in.getLong();
        int token = in.getInt(), ticks = in.getShort();
        return valid(token, ticks) ? new Message(dimension, id, most, least, token, ticks) : null;
    }

    /** A blend needs a running action (low byte != 0) and 1..MAX_TICKS ticks; 0 ticks is a plain play. */
    static boolean valid(int token, int ticks) {
        return (token & 0xFF) != 0 && ticks >= 1 && ticks <= PoseBlend.MAX_TICKS;
    }
}
