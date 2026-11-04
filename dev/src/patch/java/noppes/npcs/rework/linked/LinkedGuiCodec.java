package noppes.npcs.rework.linked;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/** M13.2b (Q395): bounded messages of the {@value #CHANNEL} channel between Global, Linked NPCs and the server. */
public final class LinkedGuiCodec {
    public static final String CHANNEL = "rwlinked";
    public static final int REQUEST = 1, RESPONSE = 2;
    public static final int MAX_NAME = 255, MAX_TEXT = 512;
    private static final int REQUEST_HEADER = 12, RESPONSE_HEADER = 19;
    public static final int MAX_REQUEST = REQUEST_HEADER + MAX_NAME, MAX_RESPONSE = RESPONSE_HEADER + MAX_NAME + MAX_TEXT;

    /** GET with an empty group only tells the client that the server has the channel and what the player may do. */
    public enum Op { GET, SET, UNDO }

    public static final int RESULT_OK = 0, RESULT_DENIED = 1, RESULT_UNKNOWN = 2, RESULT_NOTHING = 3;

    public static final class Request {
        public final int id, mask;
        public final Op op;
        public final String group;

        public Request(int id, Op op, int mask, String group) {
            this.id = id;
            this.op = op;
            this.mask = mask;
            this.group = group;
        }

        public byte[] encode() {
            if (id < 0 || op == null || !validMask(mask) || !validGroup(group)) {
                throw new IllegalArgumentException("invalid linked request");
            }
            byte[] name = group.getBytes(StandardCharsets.UTF_8);
            return ByteBuffer.allocate(REQUEST_HEADER + name.length).put((byte) 1).put((byte) REQUEST).putInt(id)
                    .put((byte) op.ordinal()).putInt(mask).put((byte) name.length).put(name).array();
        }
    }

    public static final class Response {
        public final int id, result, mask, loaded;
        public final boolean undo;
        public final String group, text;

        public Response(int id, int result, int mask, boolean undo, int loaded, String group, String text) {
            this.id = id;
            this.result = result;
            this.mask = mask;
            this.undo = undo;
            this.loaded = loaded;
            this.group = group;
            this.text = text;
        }

        public byte[] encode() {
            byte[] name = group.getBytes(StandardCharsets.UTF_8), message = text.getBytes(StandardCharsets.UTF_8);
            if (id < 0 || result < RESULT_OK || result > RESULT_NOTHING || !validMask(mask) || loaded < 0
                    || name.length > MAX_NAME || message.length > MAX_TEXT) {
                throw new IllegalArgumentException("invalid linked response");
            }
            return ByteBuffer.allocate(RESPONSE_HEADER + name.length + message.length).put((byte) 1).put((byte) RESPONSE)
                    .putInt(id).put((byte) result).putInt(mask).put((byte) (undo ? 1 : 0)).putInt(loaded)
                    .put((byte) name.length).put(name).putShort((short) message.length).put(message).array();
        }
    }

    private LinkedGuiCodec() {
    }

    public static boolean validMask(int mask) {
        return (mask & ~LinkedCategories.ALL) == 0;
    }

    public static boolean validGroup(String group) {
        return group != null && group.getBytes(StandardCharsets.UTF_8).length <= MAX_NAME;
    }

    private static int kind(byte[] data) {
        return data != null && data.length >= 2 && data[0] == 1 ? data[1] : -1;
    }

    /** The request or null for anything malformed. */
    public static Request request(byte[] data) {
        if (kind(data) != REQUEST || data.length < REQUEST_HEADER || data.length > MAX_REQUEST) {
            return null;
        }
        ByteBuffer b = ByteBuffer.wrap(data);
        b.position(2);
        int id = b.getInt(), op = b.get() & 255, mask = b.getInt(), length = b.get() & 255;
        if (id < 0 || op >= Op.values().length || !validMask(mask) || length != b.remaining()) {
            return null;
        }
        byte[] name = new byte[length];
        b.get(name);
        return new Request(id, Op.values()[op], mask, new String(name, StandardCharsets.UTF_8));
    }

    /** The response or null for anything malformed. */
    public static Response response(byte[] data) {
        if (kind(data) != RESPONSE || data.length < RESPONSE_HEADER || data.length > MAX_RESPONSE) {
            return null;
        }
        ByteBuffer b = ByteBuffer.wrap(data);
        b.position(2);
        int id = b.getInt(), result = b.get() & 255, mask = b.getInt(), undo = b.get() & 255, loaded = b.getInt();
        int length = b.get() & 255;
        if (id < 0 || result > RESULT_NOTHING || !validMask(mask) || undo > 1 || loaded < 0 || length > b.remaining() - 2) {
            return null;
        }
        byte[] name = new byte[length];
        b.get(name);
        int size = b.getShort() & 65535;
        if (size > MAX_TEXT || size != b.remaining()) {
            return null;
        }
        byte[] message = new byte[size];
        b.get(message);
        return new Response(id, result, mask, undo == 1, loaded, new String(name, StandardCharsets.UTF_8),
                new String(message, StandardCharsets.UTF_8));
    }
}
