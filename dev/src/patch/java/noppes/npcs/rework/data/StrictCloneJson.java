package noppes.npcs.rework.data;

import com.google.gson.*;
import java.math.BigDecimal;
import java.util.Map;

/** Small RFC 8259 reader/writer for the clone envelope, independent of Gson's lenient reader. */
final class StrictCloneJson {
    private final String text;
    private int pos;
    private StrictCloneJson(String s) { text = s; }
    static JsonElement read(String s) {
        StrictCloneJson p = new StrictCloneJson(s);
        JsonElement e = p.value(0);
        p.space();
        if (p.pos != s.length()) { throw p.error("Trailing content"); }
        return e;
    }
    private IllegalArgumentException error(String s) { return new IllegalArgumentException("JSON character " + pos + ": " + s); }
    private void space() { while (pos < text.length() && " \t\r\n".indexOf(text.charAt(pos)) >= 0) { pos++; } }
    private boolean take(char c) { space(); if (pos < text.length() && text.charAt(pos) == c) { pos++; return true; } return false; }
    private void need(char c) { if (!take(c)) { throw error("Expected " + c); } }
    private JsonElement value(int depth) {
        if (depth > 512) { throw error("Depth exceeds 512"); }
        space();
        if (pos == text.length()) { throw error("Missing value"); }
        char c = text.charAt(pos);
        if (c == '{') {
            pos++; JsonObject o = new JsonObject();
            if (take('}')) { return o; }
            do {
                space(); String key = string(); need(':');
                if (o.has(key)) { throw error("Duplicate key " + key); }
                o.add(key, value(depth + 1));
                if (take('}')) { return o; }
                need(',');
            } while (true);
        }
        if (c == '[') {
            pos++; JsonArray a = new JsonArray();
            if (take(']')) { return a; }
            do { a.add(value(depth + 1)); if (take(']')) { return a; } need(','); } while (true);
        }
        if (c == '"') { return new JsonPrimitive(string()); }
        for (String literal : new String[]{"true", "false", "null"}) {
            if (text.startsWith(literal, pos)) {
                pos += literal.length();
                return literal.equals("null") ? JsonNull.INSTANCE : new JsonPrimitive(literal.equals("true"));
            }
        }
        int begin = pos;
        if (c == '-') { pos++; }
        if (pos >= text.length()) { throw error("Missing number"); }
        if (text.charAt(pos) == '0') { pos++; }
        else { digits(true); }
        if (pos < text.length() && text.charAt(pos) == '.') { pos++; digits(true); }
        if (pos < text.length() && (text.charAt(pos) == 'e' || text.charAt(pos) == 'E')) {
            pos++;
            if (pos < text.length() && (text.charAt(pos) == '+' || text.charAt(pos) == '-')) { pos++; }
            digits(true);
        }
        String n = text.substring(begin, pos);
        // Gson's lazy Number preserves -0 and the lexical value (BigDecimal loses -0).
        try { new BigDecimal(n); } catch (NumberFormatException ex) { throw error("Invalid number"); }
        return new JsonPrimitive(new LexicalNumber(n));
    }
    private void digits(boolean required) {
        int begin = pos;
        while (pos < text.length() && text.charAt(pos) >= '0' && text.charAt(pos) <= '9') { pos++; }
        if (required && begin == pos) { throw error("Expected digit"); }
    }
    private String string() {
        if (pos >= text.length() || text.charAt(pos++) != '"') { throw error("Expected quoted key/string"); }
        StringBuilder b = new StringBuilder();
        while (pos < text.length()) {
            char c = text.charAt(pos++);
            if (c == '"') { return b.toString(); }
            if (c < 32) { throw error("Unescaped control character"); }
            if (c == '\\') {
                if (pos == text.length()) { throw error("Incomplete escape"); }
                char e = text.charAt(pos++);
                switch (e) {
                    case '"': case '\\': case '/': c = e; break;
                    case 'b': c = '\b'; break; case 'f': c = '\f'; break;
                    case 'n': c = '\n'; break; case 'r': c = '\r'; break; case 't': c = '\t'; break;
                    case 'u':
                        if (pos + 4 > text.length()) { throw error("Incomplete Unicode escape"); }
                        int code = 0;
                        for (int i = 0; i < 4; i++) {
                            char h = text.charAt(pos++);
                            int digit = h <= 127 ? Character.digit(h, 16) : -1;
                            if (digit < 0) { throw error("Invalid Unicode escape"); }
                            code = (code << 4) | digit;
                        }
                        c = (char) code; break;
                    default: throw error("Invalid escape");
                }
            }
            b.append(c);
        }
        throw error("Unterminated string");
    }
    private static final class LexicalNumber extends Number {
        private final String value;
        LexicalNumber(String s) { value = s; }
        public int intValue() { return new BigDecimal(value).intValue(); }
        public long longValue() { return new BigDecimal(value).longValue(); }
        public float floatValue() { return Float.parseFloat(value); }
        public double doubleValue() { return Double.parseDouble(value); }
        public String toString() { return value; }
    }
    static String write(JsonElement e) { StringBuilder b = new StringBuilder(); write(b, e, 0); b.append('\n'); return b.toString(); }
    private static void write(StringBuilder b, JsonElement e, int depth) {
        if (depth > 512) { throw new IllegalArgumentException("JSON depth exceeds 512"); }
        if (e.isJsonObject()) {
            b.append('{'); int i = 0;
            for (Map.Entry<String, JsonElement> entry : e.getAsJsonObject().entrySet()) {
                if (i++ > 0) { b.append(','); } indent(b, depth + 1);
                quote(b, entry.getKey()); b.append(": "); write(b, entry.getValue(), depth + 1);
            }
            if (i > 0) { indent(b, depth); } b.append('}');
        } else if (e.isJsonArray()) {
            b.append('['); int i = 0;
            for (JsonElement item : e.getAsJsonArray()) {
                if (i++ > 0) { b.append(','); } indent(b, depth + 1); write(b, item, depth + 1);
            }
            if (i > 0) { indent(b, depth); } b.append(']');
        } else if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) { quote(b, e.getAsString()); }
        else { b.append(e.toString()); }
    }
    private static void indent(StringBuilder b, int n) { b.append('\n'); for (int i = 0; i < n; i++) { b.append("  "); } }
    private static void quote(StringBuilder b, String s) {
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') { b.append('\\').append(c); }
            else if (c < 32 || Character.isSurrogate(c)) {
                String h = Integer.toHexString(c); b.append("\\u"); for (int j = h.length(); j < 4; j++) { b.append('0'); } b.append(h);
            } else { b.append(c); }
        }
        b.append('"');
    }
}
