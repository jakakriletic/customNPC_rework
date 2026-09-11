package noppes.npcs.rework.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;

/**
 * Type-safe replacement for the NBT&lt;-&gt;JSON conversion in {@code noppes.npcs.util.NBTJsonUtil}.
 *
 * <p>The on-disk text format is deliberately <b>unchanged</b>: files written here are byte
 * identical to what the original writer produced for the same input, so existing CustomNPCs
 * installations can still read them. Every defect fixed below was in the conversion logic,
 * not in the format.
 *
 * <p>Defects fixed, each reproduced against the original October 2019 build
 * (see {@code local.customnpcs.NbtJsonBaselineTest}):
 * <ul>
 *   <li>long arrays were read back through {@code getByte()}, truncating every value to its
 *       low 8 bits ({@code 300L} came back as {@code 44L});</li>
 *   <li>an empty {@code [B;]}, {@code [I;]} or {@code [L;]} array was read back as an empty
 *       {@link NBTTagList}, changing the tag type;</li>
 *   <li>a genuine {@link NBTTagList} of bytes, ints or longs was coerced into the matching
 *       array type, again changing the tag type. Reading such a value afterwards with
 *       {@code getTagList(key, type)} yields an empty list, so the value silently reverts to
 *       its default;</li>
 *   <li>whitespace directly after a string's opening quote was trimmed away, so
 *       {@code "  abc"} came back as {@code "abc"} and {@code " "} as {@code ""};</li>
 *   <li>a string ending in a backslash made the parser run past the end of the file and throw
 *       {@link StringIndexOutOfBoundsException}, making the whole file unreadable;</li>
 *   <li>{@code NaN} and {@code Infinity} were lower-cased before parsing, so any file
 *       containing one failed to load;</li>
 *   <li>an empty key was rejected outright;</li>
 *   <li>the parser recursed once per key, so a compound with more than a few thousand keys
 *       threw {@link StackOverflowError};</li>
 *   <li>both directions built strings by repeated concatenation, which is quadratic.</li>
 * </ul>
 *
 * <p>Escaping follows the original writer exactly: only {@code \} and {@code "} are escaped,
 * and control characters (including newlines) are written raw. The parser therefore treats
 * {@code \x} as a literal {@code x} for any {@code x}, which is what the original format means.
 * A hand written {@code \n} is a literal {@code n}, not a newline.
 */
public final class NbtJson {

    /** Structural nesting limit. Real NPC data nests about ten deep. */
    public static final int MAX_DEPTH = 512;

    private static final String INDENT = "    ";

    private NbtJson() {
    }

    /** Thrown when the text is not a well formed document in this format. */
    public static final class ParseException extends Exception {
        private static final long serialVersionUID = 1L;

        public ParseException(String message) {
            super(message);
        }
    }

    // ------------------------------------------------------------------ write

    /** Serialises a compound. Output matches the original writer byte for byte. */
    public static String write(NBTTagCompound compound) {
        if (compound == null) {
            throw new IllegalArgumentException("compound is null");
        }
        StringBuilder sb = new StringBuilder(1024);
        writeTag(sb, 0, null, compound, true);
        return sb.toString();
    }

    private static void writeTag(StringBuilder sb, int indent, String name, NBTBase base, boolean last) {
        indent(sb, indent);
        if (name != null) {
            sb.append('"').append(name).append("\": ");
        }
        byte id = base.getId();
        if (id == 9) {
            NBTTagList list = (NBTTagList) base;
            sb.append("[\n");
            int n = list.tagCount();
            for (int i = 0; i < n; i++) {
                writeTag(sb, indent + 1, null, list.get(i), i == n - 1);
            }
            indent(sb, indent);
            sb.append(']');
        } else if (id == 10) {
            NBTTagCompound c = (NBTTagCompound) base;
            sb.append("{\n");
            // getKeySet() order is the map's own order; the original writer used the same call,
            // so re-saving an unchanged compound produces an unchanged file.
            List<String> keys = new ArrayList<String>(c.getKeySet());
            int n = keys.size();
            for (int i = 0; i < n; i++) {
                String key = keys.get(i);
                writeTag(sb, indent + 1, key, c.getTag(key), i == n - 1);
            }
            indent(sb, indent);
            sb.append('}');
        } else {
            sb.append(base.toString());
        }
        if (!last) {
            sb.append(',');
        }
        sb.append('\n');
    }

    private static void indent(StringBuilder sb, int depth) {
        for (int i = 0; i < depth; i++) {
            sb.append(INDENT);
        }
    }

    // ------------------------------------------------------------------- read

    /** Parses a whole document. The outermost value must be a compound. */
    public static NBTTagCompound read(String json) throws ParseException {
        if (json == null) {
            throw new ParseException("No content");
        }
        Cursor cursor = new Cursor(json);
        cursor.skipWhitespace();
        if (cursor.eof()) {
            throw new ParseException("Empty document");
        }
        if (cursor.peek() != '{') {
            throw cursor.error("Document must start with {");
        }
        NBTTagCompound result = readCompound(cursor, 0);
        cursor.skipWhitespace();
        if (!cursor.eof()) {
            throw cursor.error("Trailing content after the closing }");
        }
        return result;
    }

    /**
     * Parses one value starting at the cursor and leaves the cursor just past it.
     * Exposed so the compatibility shim in {@code NBTJsonUtil} can keep its old signatures.
     */
    public static NBTBase readValue(Cursor cursor, int depth) throws ParseException {
        if (depth > MAX_DEPTH) {
            throw cursor.error("Nested deeper than " + MAX_DEPTH + " levels");
        }
        cursor.skipWhitespace();
        if (cursor.eof()) {
            throw cursor.error("Expected a value");
        }
        char c = cursor.peek();
        if (c == '{') {
            return readCompound(cursor, depth);
        }
        if (c == '[') {
            return readListOrArray(cursor, depth);
        }
        if (c == '"') {
            return new NBTTagString(readQuoted(cursor));
        }
        return readScalar(cursor);
    }

    /** Reads a compound. Iterates over keys instead of recursing once per key. */
    public static NBTTagCompound readCompound(Cursor cursor, int depth) throws ParseException {
        if (depth > MAX_DEPTH) {
            throw cursor.error("Nested deeper than " + MAX_DEPTH + " levels");
        }
        cursor.expect('{');
        NBTTagCompound compound = new NBTTagCompound();
        cursor.skipWhitespace();
        if (cursor.eof()) {
            throw cursor.error("Unterminated {");
        }
        if (cursor.peek() == '}') {
            cursor.next();
            return compound;
        }
        while (true) {
            cursor.skipWhitespace();
            String key = readKey(cursor);
            cursor.skipWhitespace();
            cursor.expect(':');
            NBTBase value = readValue(cursor, depth + 1);
            compound.setTag(key, value);
            cursor.skipWhitespace();
            if (cursor.eof()) {
                throw cursor.error("Unterminated {");
            }
            char c = cursor.next();
            if (c == '}') {
                return compound;
            }
            if (c != ',') {
                throw cursor.error("Expected , or } after the value of \"" + key + "\"");
            }
            cursor.skipWhitespace();
            // tolerate a trailing comma before }
            if (!cursor.eof() && cursor.peek() == '}') {
                cursor.next();
                return compound;
            }
        }
    }

    private static NBTBase readListOrArray(Cursor cursor, int depth) throws ParseException {
        cursor.expect('[');
        cursor.skipWhitespace();
        char kind = 0;
        if (cursor.remaining() >= 2 && cursor.charAt(1) == ';') {
            char c = cursor.peek();
            if (c == 'B' || c == 'I' || c == 'L') {
                kind = c;
                cursor.next();
                cursor.next();
            }
        }
        List<NBTBase> items = new ArrayList<NBTBase>();
        cursor.skipWhitespace();
        if (cursor.eof()) {
            throw cursor.error("Unterminated [");
        }
        if (cursor.peek() == ']') {
            cursor.next();
        } else {
            while (true) {
                items.add(readValue(cursor, depth + 1));
                cursor.skipWhitespace();
                if (cursor.eof()) {
                    throw cursor.error("Unterminated [");
                }
                char c = cursor.next();
                if (c == ']') {
                    break;
                }
                if (c != ',') {
                    throw cursor.error("Expected , or ] inside a list");
                }
                cursor.skipWhitespace();
                if (!cursor.eof() && cursor.peek() == ']') {
                    cursor.next();
                    break;
                }
            }
        }
        // The type marker decides the tag type. Without one this is a list, whatever it holds:
        // inferring an array type from the contents is what silently changed tag types before.
        switch (kind) {
            case 'B': {
                byte[] data = new byte[items.size()];
                for (int i = 0; i < data.length; i++) {
                    data[i] = primitive(cursor, items.get(i), "byte array").getByte();
                }
                return new NBTTagByteArray(data);
            }
            case 'I': {
                int[] data = new int[items.size()];
                for (int i = 0; i < data.length; i++) {
                    data[i] = primitive(cursor, items.get(i), "int array").getInt();
                }
                return new NBTTagIntArray(data);
            }
            case 'L': {
                long[] data = new long[items.size()];
                for (int i = 0; i < data.length; i++) {
                    data[i] = primitive(cursor, items.get(i), "long array").getLong();
                }
                return new NBTTagLongArray(data);
            }
            default: {
                NBTTagList list = new NBTTagList();
                for (int i = 0; i < items.size(); i++) {
                    list.appendTag(items.get(i));
                }
                return list;
            }
        }
    }

    private static NBTPrimitive primitive(Cursor cursor, NBTBase base, String what) throws ParseException {
        if (!(base instanceof NBTPrimitive)) {
            throw cursor.error("Non numeric entry in a " + what);
        }
        return (NBTPrimitive) base;
    }

    private static String readKey(Cursor cursor) throws ParseException {
        if (cursor.eof()) {
            throw cursor.error("Expected a key");
        }
        if (cursor.peek() == '"') {
            return readQuoted(cursor);
        }
        StringBuilder sb = new StringBuilder();
        while (!cursor.eof() && cursor.peek() != ':') {
            sb.append(cursor.next());
        }
        if (cursor.eof()) {
            throw cursor.error("Expected : after a key");
        }
        return sb.toString().trim();
    }

    /**
     * Reads a quoted string. Preserves leading and trailing whitespace, handles a value that
     * ends in an escaped backslash, and never runs past the end of the text.
     */
    private static String readQuoted(Cursor cursor) throws ParseException {
        cursor.expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (cursor.eof()) {
                throw cursor.error("Unterminated string");
            }
            char c = cursor.next();
            if (c == '\\') {
                if (cursor.eof()) {
                    throw cursor.error("Unterminated escape at the end of a string");
                }
                sb.append(cursor.next());
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
    }

    private static NBTBase readScalar(Cursor cursor) throws ParseException {
        StringBuilder sb = new StringBuilder();
        while (!cursor.eof()) {
            char c = cursor.peek();
            if (c == ',' || c == ']' || c == '}') {
                break;
            }
            sb.append(cursor.next());
        }
        String raw = sb.toString().trim();
        if (raw.isEmpty()) {
            // The original produced an empty string here. Kept so existing files still load.
            return new NBTTagString("");
        }
        return parseNumber(cursor, raw);
    }

    private static NBTBase parseNumber(Cursor cursor, String raw) throws ParseException {
        char suffix = Character.toLowerCase(raw.charAt(raw.length() - 1));
        String body = raw.substring(0, raw.length() - 1);
        try {
            switch (suffix) {
                case 'd':
                    return new NBTTagDouble(Double.parseDouble(body));
                case 'f':
                    return new NBTTagFloat(Float.parseFloat(body));
                case 'b':
                    return new NBTTagByte(Byte.parseByte(body));
                case 's':
                    return new NBTTagShort(Short.parseShort(body));
                case 'l':
                    return new NBTTagLong(Long.parseLong(body));
                default:
                    break;
            }
            String lower = raw.toLowerCase(Locale.ROOT);
            if (lower.indexOf('.') >= 0 || lower.indexOf("nan") >= 0 || lower.indexOf("infinity") >= 0) {
                return new NBTTagDouble(Double.parseDouble(raw));
            }
            return new NBTTagInt(Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            throw cursor.error("Cannot read \"" + raw + "\" as a number");
        }
    }

    // ----------------------------------------------------------------- cursor

    /** Position in the text being parsed. Reports line and column on failure. */
    public static final class Cursor {
        private final String text;
        private int index;

        public Cursor(String text) {
            this.text = text;
            // strip a UTF-8 byte order mark, which some editors add
            if (this.text.length() > 0 && this.text.charAt(0) == '\uFEFF') {
                this.index = 1;
            }
        }

        public int index() {
            return index;
        }

        public String text() {
            return text;
        }

        boolean eof() {
            return index >= text.length();
        }

        int remaining() {
            return text.length() - index;
        }

        char peek() {
            return text.charAt(index);
        }

        char charAt(int offset) {
            return text.charAt(index + offset);
        }

        char next() {
            return text.charAt(index++);
        }

        void skipWhitespace() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }

        void expect(char expected) throws ParseException {
            skipWhitespace();
            if (eof() || text.charAt(index) != expected) {
                throw error("Expected " + expected);
            }
            index++;
        }

        ParseException error(String message) {
            int line = 1;
            int column = 1;
            int limit = Math.min(index, text.length());
            for (int i = 0; i < limit; i++) {
                if (text.charAt(i) == '\n') {
                    line++;
                    column = 1;
                } else {
                    column++;
                }
            }
            return new ParseException(message + " (line " + line + ", column " + column + ")");
        }
    }
}
