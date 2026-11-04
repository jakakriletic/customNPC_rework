package noppes.npcs.rework.linked;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** M13.2b (Q395): bounded messages of the {@value #CHANNEL} channel between Global, Linked NPCs and the server. */
public final class LinkedGuiCodec {
    public static final String CHANNEL = "rwlinked";
    public static final int REQUEST = 1, RESPONSE = 2, BATCH = 3, LIST = 4;
    public static final int MAX_NAME = 255, MAX_TEXT = 512;
    private static final int REQUEST_HEADER = 12, RESPONSE_HEADER = 19;
    public static final int MAX_REQUEST = REQUEST_HEADER + MAX_NAME, MAX_RESPONSE = RESPONSE_HEADER + MAX_NAME + MAX_TEXT;
    /** M13.3: bounds of the wand list; nearest rows only, so one answer stays well under the 1 MB packet limit. */
    public static final int MAX_ROWS = 1000, MAX_GROUPS = 1000, MAX_IDS = 1000;
    private static final int BATCH_HEADER = 18, LIST_HEADER = 14, GROUP_HEADER = 5, ROW_HEADER = 11;
    public static final int MAX_BATCH = BATCH_HEADER + MAX_NAME + 4 * MAX_IDS;
    public static final int MAX_LIST = LIST_HEADER + MAX_GROUPS * (GROUP_HEADER + MAX_NAME) + MAX_ROWS * (ROW_HEADER + MAX_NAME);

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

    /** M13.3: requests of the wand list. */
    public enum BatchOp { LIST, LINK, COPY, DELETE, RESET }

    public static final class Batch {
        public final int id, mask, source;
        public final BatchOp op;
        public final String group;
        public final int[] ids;

        public Batch(int id, BatchOp op, int mask, int source, String group, int[] ids) {
            this.id = id;
            this.op = op;
            this.mask = mask;
            this.source = source;
            this.group = group;
            this.ids = ids;
        }

        public byte[] encode() {
            if (id < 0 || op == null || !validMask(mask) || !validGroup(group) || ids.length > MAX_IDS) {
                throw new IllegalArgumentException("invalid linked batch");
            }
            byte[] name = group.getBytes(StandardCharsets.UTF_8);
            ByteBuffer b = ByteBuffer.allocate(BATCH_HEADER + name.length + 4 * ids.length).put((byte) 1).put((byte) BATCH).putInt(id)
                    .put((byte) op.ordinal()).putInt(mask).putInt(source).put((byte) name.length).put(name).putShort((short) ids.length);
            for (int each : ids) {
                b.putInt(each);
            }
            return b.array();
        }
    }

    public static final class Group {
        public final String name;
        public final int loaded;

        public Group(String name, int loaded) {
            this.name = name;
            this.loaded = loaded;
        }
    }

    public static final class Row {
        public final int id, group;
        public final float distance;
        public final String name;

        /** @param group index in the groups of the answer, -1 for none */
        public Row(int id, float distance, int group, String name) {
            this.id = id;
            this.distance = distance;
            this.group = group;
            this.name = name;
        }
    }

    public static final class ListResponse {
        public final int id, total;
        public final List<Group> groups;
        public final List<Row> rows;

        public ListResponse(int id, int total, List<Group> groups, List<Row> rows) {
            this.id = id;
            this.total = total;
            this.groups = groups;
            this.rows = rows;
        }

        public byte[] encode() {
            if (id < 0 || total < 0 || groups.size() > MAX_GROUPS || rows.size() > MAX_ROWS) {
                throw new IllegalArgumentException("invalid linked list");
            }
            int size = LIST_HEADER;
            for (Group group : groups) {
                size += GROUP_HEADER + bytes(group.name).length;
            }
            for (Row row : rows) {
                size += ROW_HEADER + bytes(row.name).length;
            }
            ByteBuffer b = ByteBuffer.allocate(size).put((byte) 1).put((byte) LIST).putInt(id).putInt(total)
                    .putShort((short) groups.size()).putShort((short) rows.size());
            for (Group group : groups) {
                byte[] name = bytes(group.name);
                b.put((byte) name.length).put(name).putInt(group.loaded);
            }
            for (Row row : rows) {
                byte[] name = bytes(row.name);
                b.putInt(row.id).putFloat(row.distance).putShort((short) row.group).put((byte) name.length).put(name);
            }
            return b.array();
        }

        private static byte[] bytes(String text) {
            byte[] name = text.getBytes(StandardCharsets.UTF_8);
            if (name.length > MAX_NAME) {
                throw new IllegalArgumentException("linked name too long");
            }
            return name;
        }
    }

    private LinkedGuiCodec() {
    }

    /** The text cut to at most that many UTF-8 bytes, never inside a character. */
    public static String fit(String text, int max) {
        String out = text;
        while (out.getBytes(StandardCharsets.UTF_8).length > max) {
            out = out.substring(0, out.length() - 1);
            if (!out.isEmpty() && Character.isHighSurrogate(out.charAt(out.length() - 1))) {
                out = out.substring(0, out.length() - 1);
            }
        }
        return out;
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

    /** The batch or null for anything malformed. */
    public static Batch batch(byte[] data) {
        if (kind(data) != BATCH || data.length < BATCH_HEADER || data.length > MAX_BATCH) {
            return null;
        }
        ByteBuffer b = ByteBuffer.wrap(data);
        b.position(2);
        int id = b.getInt(), op = b.get() & 255, mask = b.getInt(), source = b.getInt(), length = b.get() & 255;
        if (id < 0 || op >= BatchOp.values().length || !validMask(mask) || length > b.remaining() - 2) {
            return null;
        }
        byte[] name = new byte[length];
        b.get(name);
        int count = b.getShort() & 65535;
        if (count > MAX_IDS || b.remaining() != 4 * count) {
            return null;
        }
        int[] ids = new int[count];
        for (int i = 0; i < count; i++) {
            ids[i] = b.getInt();
        }
        return new Batch(id, BatchOp.values()[op], mask, source, new String(name, StandardCharsets.UTF_8), ids);
    }

    /** The list or null for anything malformed. */
    public static ListResponse list(byte[] data) {
        if (kind(data) != LIST || data.length < LIST_HEADER || data.length > MAX_LIST) {
            return null;
        }
        try {
            ByteBuffer b = ByteBuffer.wrap(data);
            b.position(2);
            int id = b.getInt(), total = b.getInt(), groupCount = b.getShort() & 65535, rowCount = b.getShort() & 65535;
            if (id < 0 || total < 0 || groupCount > MAX_GROUPS || rowCount > MAX_ROWS) {
                return null;
            }
            List<Group> groups = new ArrayList<Group>();
            for (int i = 0; i < groupCount; i++) {
                String name = text(b);
                int loaded = b.getInt();
                if (loaded < 0) {
                    return null;
                }
                groups.add(new Group(name, loaded));
            }
            List<Row> rows = new ArrayList<Row>();
            for (int i = 0; i < rowCount; i++) {
                int row = b.getInt();
                float distance = b.getFloat();
                int group = b.getShort();
                if (distance < 0 || distance != distance || group < -1 || group >= groupCount) {
                    return null;
                }
                rows.add(new Row(row, distance, group, text(b)));
            }
            return b.hasRemaining() ? null : new ListResponse(id, total, groups, rows);
        } catch (BufferUnderflowException e) {
            return null;
        }
    }

    private static String text(ByteBuffer b) {
        byte[] name = new byte[b.get() & 255];
        b.get(name);
        return new String(name, StandardCharsets.UTF_8);
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
