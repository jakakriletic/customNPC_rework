package noppes.npcs.rework.anim;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** P7c (D-098, Q275, Q287): messages of {@code /npcanim reload} on the {@value BlendCodec#CHANNEL} channel,
 * told from a blend message by the first byte. A bundle is the whole override state (entity id to raw
 * animation file, sorted), sent in checked parts; its SHA-256 binds prepare, confirmation and commit.
 * All sizes are checked before any allocation. */
public final class ReloadCodec {
    public static final byte PART = 2, COMMIT = 3, ABORT = 4, ACK = 5;
    public static final int MAX_FILE = 2 << 20, MAX_BUNDLE = 8 << 20, PART_SIZE = 256 * 1024, MAX_IDS = 64, HASH_SIZE = 32;
    /** Largest message: kind, revision, total, offset, one part. */
    public static final int MAX_MESSAGE = 1 + 4 + 4 + 4 + PART_SIZE;
    private static final int COMMIT_SIZE = 1 + 4 + HASH_SIZE, ABORT_SIZE = 1 + 4, ACK_SIZE = 1 + 4 + 1 + HASH_SIZE;
    private static final Pattern ID = Pattern.compile("[a-z0-9_]{1,64}");

    private ReloadCodec() { }

    public static final class Message {
        public final byte kind;
        public final int revision, total, offset;
        public final byte[] data, hash;
        public final boolean ok;
        Message(byte kind, int revision, int total, int offset, byte[] data, byte[] hash, boolean ok) {
            this.kind = kind; this.revision = revision; this.total = total; this.offset = offset;
            this.data = data; this.hash = hash; this.ok = ok;
        }
    }

    /** No separator, dot or capital can pass: an id is safe as a file name part. */
    public static boolean validId(String id) { return id != null && ID.matcher(id).matches(); }

    static boolean isReload(byte first) { return first >= PART && first <= ACK; }

    public static byte[][] parts(int revision, byte[] bundle) {
        if (bundle.length == 0 || bundle.length > MAX_BUNDLE) { throw new IllegalArgumentException("bundle size " + bundle.length); }
        List<byte[]> out = new ArrayList<>();
        for (int offset = 0; offset < bundle.length; offset += PART_SIZE) {
            int n = Math.min(PART_SIZE, bundle.length - offset);
            out.add(ByteBuffer.allocate(13 + n).put(PART).putInt(revision).putInt(bundle.length).putInt(offset)
                    .put(bundle, offset, n).array());
        }
        return out.toArray(new byte[0][]);
    }

    public static byte[] commit(int revision, byte[] hash) { return ByteBuffer.allocate(COMMIT_SIZE).put(COMMIT).putInt(revision).put(hash).array(); }

    public static byte[] abort(int revision) { return ByteBuffer.allocate(ABORT_SIZE).put(ABORT).putInt(revision).array(); }

    public static byte[] ack(int revision, boolean ok, byte[] hash) {
        return ByteBuffer.allocate(ACK_SIZE).put(ACK).putInt(revision).put((byte) (ok ? 1 : 0)).put(hash).array();
    }

    /** @return the message, or null for a wrong kind, size or range */
    public static Message decode(byte[] data) {
        if (data == null || data.length < 5 || data.length > MAX_MESSAGE) { return null; }
        ByteBuffer in = ByteBuffer.wrap(data);
        byte kind = in.get();
        int revision = in.getInt();
        if (revision < 1) { return null; }
        switch (kind) {
            case PART: {
                if (data.length < 14) { return null; }
                int total = in.getInt(), offset = in.getInt(), n = data.length - 13;
                if (total < 1 || total > MAX_BUNDLE || offset < 0 || n > total || offset > total - n) { return null; }
                return new Message(kind, revision, total, offset, Arrays.copyOfRange(data, 13, data.length), null, false);
            }
            case COMMIT: {
                if (data.length != COMMIT_SIZE) { return null; }
                return new Message(kind, revision, 0, 0, null, Arrays.copyOfRange(data, 5, data.length), false);
            }
            case ABORT: return data.length == ABORT_SIZE ? new Message(kind, revision, 0, 0, null, null, false) : null;
            case ACK: {
                if (data.length != ACK_SIZE) { return null; }
                byte ok = in.get();
                if (ok != 0 && ok != 1) { return null; }
                return new Message(kind, revision, 0, 0, null, Arrays.copyOfRange(data, 6, data.length), ok == 1);
            }
            default: return null;
        }
    }

    public static byte[] encodeBundle(Map<String, byte[]> files) {
        if (files.size() > MAX_IDS) { throw new IllegalArgumentException("too many entity ids"); }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(files.size());
            for (Map.Entry<String, byte[]> e : new TreeMap<>(files).entrySet()) {
                if (!validId(e.getKey()) || e.getValue().length > MAX_FILE) { throw new IllegalArgumentException("file of " + e.getKey()); }
                out.writeUTF(e.getKey());
                out.writeInt(e.getValue().length);
                out.write(e.getValue());
            }
            return bytes.toByteArray();
        } catch (IOException impossible) { throw new IllegalStateException(impossible); }
    }

    /** @return the sorted files, or null for anything {@link #encodeBundle} would not write */
    public static TreeMap<String, byte[]> decodeBundle(byte[] bundle) {
        if (bundle == null || bundle.length < 4 || bundle.length > MAX_BUNDLE) { return null; }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bundle))) {
            int count = in.readInt();
            if (count < 0 || count > MAX_IDS) { return null; }
            TreeMap<String, byte[]> files = new TreeMap<>();
            String last = null;
            for (int i = 0; i < count; i++) {
                String id = in.readUTF();
                int n = in.readInt();
                if (!validId(id) || (last != null && last.compareTo(id) >= 0) || n < 0 || n > MAX_FILE || n > in.available()) { return null; }
                byte[] file = new byte[n];
                in.readFully(file);
                files.put(id, file);
                last = id;
            }
            return in.available() == 0 ? files : null;
        } catch (IOException malformed) { return null; }
    }

    public static byte[] hash(byte[] bundle) {
        try { return MessageDigest.getInstance("SHA-256").digest(bundle); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    static String hex(byte[] hash) {
        if (hash == null) { return "none"; }
        StringBuilder out = new StringBuilder();
        for (byte b : hash) { out.append(String.format("%02x", b)); }
        return out.toString();
    }

    /** Puts checked parts of one bundle back together, in order. */
    public static final class Assembler {
        private int revision;
        private byte[] buffer;
        private int filled;

        /** @return the whole bundle after its last part, otherwise null; a part out of order drops the bundle */
        public byte[] accept(Message part) {
            if (part.offset == 0) {
                revision = part.revision; buffer = new byte[part.total]; filled = 0;
            } else if (buffer == null || part.revision != revision || part.total != buffer.length || part.offset != filled) {
                buffer = null;
                return null;
            }
            System.arraycopy(part.data, 0, buffer, filled, part.data.length);
            filled += part.data.length;
            if (filled < buffer.length) { return null; }
            byte[] whole = buffer;
            buffer = null;
            return whole;
        }
    }
}
