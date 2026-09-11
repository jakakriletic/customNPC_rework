package noppes.npcs.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.rework.data.NbtJson;
import noppes.npcs.rework.data.SafeFileWriter;

/**
 * Compatibility shim. The public signatures are unchanged from the October 2019 release so
 * every existing caller and any third party add-on still links; the conversion itself is now
 * done by {@link NbtJson}, which fixes the data corruption documented there.
 *
 * <p>The on-disk format is unchanged, so files written by this class are still readable by an
 * unmodified CustomNPCs install.
 */
public class NBTJsonUtil {

    /**
     * Marker used by the test suite to tell the patched implementation from the original
     * binary. {@code local.customnpcs.NbtJsonBaselineTest} looks this field up reflectively.
     */
    public static final boolean REWORK_SERIALIZER = true;
    public static final boolean REWORK_SAFE_WRITER = true;

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    public static String Convert(NBTTagCompound compound) {
        return NbtJson.write(compound);
    }

    public static NBTTagCompound Convert(String json) throws JsonException {
        try {
            return NbtJson.read(json);
        } catch (NbtJson.ParseException e) {
            throw new JsonException(e.getMessage());
        }
    }

    /** Kept for binary compatibility; only ever called from within this class in the original. */
    public static void FillCompound(NBTTagCompound compound, JsonFile json) throws JsonException {
        NBTTagCompound parsed = Convert(json.rest());
        for (String key : parsed.getKeySet()) {
            compound.setTag(key, parsed.getTag(key));
        }
        json.consumeAll();
    }

    /** Kept for binary compatibility; only ever called from within this class in the original. */
    public static NBTBase ReadValue(JsonFile json) throws JsonException {
        NbtJson.Cursor cursor = new NbtJson.Cursor(json.rest());
        try {
            NBTBase value = NbtJson.readValue(cursor, 0);
            json.consume(cursor.index());
            return value;
        } catch (NbtJson.ParseException e) {
            throw new JsonException(e.getMessage());
        }
    }

    public static NBTTagCompound LoadFile(File file) throws IOException, JsonException {
        StringBuilder sb = new StringBuilder((int) Math.min(file.length() + 16L, 1 << 20));
        Reader reader = new InputStreamReader(new FileInputStream(file), UTF_8);
        try {
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
            }
        } finally {
            reader.close();
        }
        try {
            return NbtJson.read(sb.toString());
        } catch (NbtJson.ParseException e) {
            throw new JsonException(e.getMessage() + " in " + file.getName());
        }
    }

    /**
     * Writes the compound. The stream is flushed and closed even when writing fails, and the
     * failure is passed to the caller rather than leaving a half written file behind silently.
     */
    public static void SaveFile(File file, NBTTagCompound compound) throws IOException, JsonException {
        final NBTTagCompound expected = compound.copy();
        String json = NbtJson.write(expected);
        SafeFileWriter.writeUtf8(file, json, new SafeFileWriter.Validator() {
            @Override
            public void validate(File candidate) throws IOException {
                try {
                    NBTTagCompound actual = LoadFile(candidate);
                    if (!expected.equals(actual)) {
                        throw new IOException("NBT verification failed for " + candidate);
                    }
                } catch (JsonException e) {
                    throw new IOException("JSON verification failed for " + candidate, e);
                }
            }
        });
    }

    public static class JsonException extends Exception {
        private static final long serialVersionUID = 1L;

        public JsonException(String message) {
            super(message);
        }

        public JsonException(String message, JsonFile json) {
            super(message + ": " + json.getCurrentPos());
        }
    }

    /**
     * Text cursor from the original implementation. Retained because it appears in the public
     * signatures above; the parser no longer uses it.
     */
    static class JsonFile {
        private final String original;
        private String text;

        public JsonFile(String text) {
            this.text = text;
            this.original = text;
        }

        String rest() {
            return text;
        }

        void consume(int characters) {
            text = text.substring(Math.min(characters, text.length()));
        }

        void consumeAll() {
            text = "";
        }

        public int keyIndex() {
            boolean inQuote = false;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '"') {
                    inQuote = !inQuote;
                } else if (c == ':' && !inQuote) {
                    return i;
                }
            }
            return -1;
        }

        public String cutDirty(int i) {
            String s = text.substring(0, i);
            text = text.substring(i);
            return s;
        }

        public String cut(int i) {
            String s = text.substring(0, i);
            text = text.substring(i).trim();
            return s;
        }

        public String substring(int beginIndex, int endIndex) {
            return text.substring(beginIndex, endIndex);
        }

        public int indexOf(String s) {
            return text.indexOf(s);
        }

        public String getCurrentPos() {
            int currentPos = original.length() - text.length();
            String done = original.substring(0, Math.max(currentPos, 0));
            String[] lines = done.split("\r\n|\r|\n");
            int pos = 0;
            String line = "";
            if (lines.length > 0) {
                pos = lines[lines.length - 1].length();
                String[] all = original.split("\r\n|\r|\n");
                if (lines.length - 1 < all.length) {
                    line = all[lines.length - 1].trim();
                }
            }
            return "Line: " + lines.length + ", Pos: " + pos + ", Text: " + line;
        }

        public boolean startsWith(String... ss) {
            for (String s : ss) {
                if (text.startsWith(s)) {
                    return true;
                }
            }
            return false;
        }

        public boolean endsWith(String s) {
            return text.endsWith(s);
        }
    }
}
