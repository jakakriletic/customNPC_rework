package noppes.npcs.rework.data;

import com.google.gson.*;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import net.minecraft.nbt.*;

/** Explicitly typed fallback. No double intermediary for integers, no reflection on obfuscated fields. */
final class CloneNbt {
    static final String[] TYPES = {"end", "byte", "short", "int", "long", "float", "double", "byteArray", "string", "list", "compound", "intArray", "longArray"};
    static IllegalArgumentException bad(String path, String why) { return new IllegalArgumentException(path + ": " + why); }
    static String pointer(String key) { return key.replace("~", "~0").replace("/", "~1"); }
    static JsonObject object(JsonElement e, String p) {
        if (e == null || !e.isJsonObject()) { throw bad(p, "Expected object"); } return e.getAsJsonObject();
    }
    static JsonArray array(JsonElement e, String p) {
        if (e == null || !e.isJsonArray()) { throw bad(p, "Expected array"); } return e.getAsJsonArray();
    }
    static String string(JsonElement e, String p) {
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) { throw bad(p, "Expected string"); } return e.getAsString();
    }
    static void keys(JsonObject o, String p, String... allowed) {
        Set<String> a = new HashSet<String>(Arrays.asList(allowed));
        for (Map.Entry<String, JsonElement> e : o.entrySet()) { if (!a.contains(e.getKey())) { throw bad(p + "/" + pointer(e.getKey()), "Unknown field"); } }
    }
    static long integer(JsonElement e, long min, long max, boolean quoted, String p) {
        if (e == null || !e.isJsonPrimitive() || (quoted ? !e.getAsJsonPrimitive().isString() : !e.getAsJsonPrimitive().isNumber())) { throw bad(p, quoted ? "Expected decimal string" : "Expected integer"); }
        String s = e.getAsString();
        if (!s.matches("-?(0|[1-9][0-9]*)")) { throw bad(p, "Expected decimal integer"); }
        try { long n = Long.parseLong(s); if (n < min || n > max) { throw new NumberFormatException(); } return n; }
        catch (NumberFormatException ex) { throw bad(p, "Integer outside range"); }
    }
    static JsonObject encode(NBTBase t) { return encode(t, 0); }
    private static JsonObject encode(NBTBase t, int depth) {
        if (depth > 512) { throw bad("/_nbt/raw", "NBT depth exceeds 512"); }
        int id = t.getId(); JsonObject o = new JsonObject(); o.addProperty("type", TYPES[id]);
        JsonElement v;
        switch (id) {
            case 1: case 2: case 3: v = new JsonPrimitive(((NBTPrimitive)t).getLong()); break;
            case 4: v = new JsonPrimitive(Long.toString(((NBTPrimitive)t).getLong())); break;
            case 5: {
                float f = ((NBTPrimitive)t).getFloat();
                if (!Float.isFinite(f)) { o.addProperty("bits", String.format(Locale.ROOT, "%08x", Float.floatToRawIntBits(f))); return o; }
                v = new JsonPrimitive(f); break;
            }
            case 6: {
                double d = ((NBTPrimitive)t).getDouble();
                if (!Double.isFinite(d)) { o.addProperty("bits", String.format(Locale.ROOT, "%016x", Double.doubleToRawLongBits(d))); return o; }
                v = new JsonPrimitive(d); break;
            }
            case 8: v = new JsonPrimitive(((NBTTagString)t).getString()); break;
            case 10: {
                JsonObject c = new JsonObject(); NBTTagCompound n = (NBTTagCompound)t;
                for (String k : new TreeSet<String>(n.getKeySet())) { c.add(k, encode(n.getTag(k), depth + 1)); }
                v = c; break;
            }
            case 9: {
                NBTTagList l = (NBTTagList)t; o.addProperty("elementType", TYPES[l.getTagType()]);
                JsonArray a = new JsonArray(); for (int i = 0; i < l.tagCount(); i++) { a.add(encode(l.get(i), depth + 1)); } v = a; break;
            }
            case 7: { JsonArray a = new JsonArray(); for (byte n : ((NBTTagByteArray)t).getByteArray()) { a.add(new JsonPrimitive(n)); } v = a; break; }
            case 11: { JsonArray a = new JsonArray(); for (int n : ((NBTTagIntArray)t).getIntArray()) { a.add(new JsonPrimitive(n)); } v = a; break; }
            case 12: { JsonArray a = new JsonArray(); for (long n : longs(t)) { a.add(new JsonPrimitive(Long.toString(n))); } v = a; break; }
            default: throw bad("/_nbt/raw", "Unsupported NBT type " + id);
        }
        o.add("value", v); return o;
    }
    static NBTBase decode(JsonElement e, String p) { return decode(e, p, 0); }
    private static NBTBase decode(JsonElement e, String p, int depth) {
        if (depth > 512) { throw bad(p, "NBT depth exceeds 512"); }
        JsonObject o = object(e, p); int id = type(string(o.get("type"), p + "/type"), p);
        if ((id == 5 || id == 6) && o.has("bits")) {
            keys(o, p, "type", "bits"); String h = string(o.get("bits"), p + "/bits");
            if (!h.matches(id == 5 ? "[0-9a-fA-F]{8}" : "[0-9a-fA-F]{16}")) { throw bad(p, "Invalid IEEE bits"); }
            long bits = Long.parseUnsignedLong(h, 16);
            return id == 5 ? new NBTTagFloat(Float.intBitsToFloat((int)bits)) : new NBTTagDouble(Double.longBitsToDouble(bits));
        }
        keys(o, p, id == 9 ? new String[]{"type", "elementType", "value"} : new String[]{"type", "value"});
        JsonElement v = o.get("value"); String vp = p + "/value";
        switch (id) {
            case 1: return new NBTTagByte((byte)integer(v, -128, 127, false, vp));
            case 2: return new NBTTagShort((short)integer(v, -32768, 32767, false, vp));
            case 3: return new NBTTagInt((int)integer(v, Integer.MIN_VALUE, Integer.MAX_VALUE, false, vp));
            case 4: return new NBTTagLong(integer(v, Long.MIN_VALUE, Long.MAX_VALUE, true, vp));
            case 5: case 6: return floating(v, id, vp);
            case 8: return new NBTTagString(string(v, vp));
            case 10: {
                NBTTagCompound c = new NBTTagCompound();
                for (Map.Entry<String, JsonElement> en : object(v, vp).entrySet()) { c.setTag(en.getKey(), decode(en.getValue(), vp + "/" + pointer(en.getKey()), depth + 1)); } return c;
            }
            case 9: {
                int et = type(string(o.get("elementType"), p + "/elementType"), p);
                JsonArray a = array(v, vp); NBTTagList l = emptyList(et);
                if (et == 0 && a.size() != 0) { throw bad(p, "Nonempty list of end tags"); }
                for (int i = 0; i < a.size(); i++) {
                    NBTBase t = decode(a.get(i), vp + "/" + i, depth + 1);
                    if (t.getId() != et) { throw bad(vp + "/" + i, "List element type differs"); } l.appendTag(t);
                }
                return l;
            }
            case 7: { JsonArray a = array(v, vp); byte[] b = new byte[a.size()]; for (int i = 0; i < b.length; i++) { b[i] = (byte)integer(a.get(i), -128, 127, false, vp + "/" + i); } return new NBTTagByteArray(b); }
            case 11: { JsonArray a = array(v, vp); int[] b = new int[a.size()]; for (int i = 0; i < b.length; i++) { b[i] = (int)integer(a.get(i), Integer.MIN_VALUE, Integer.MAX_VALUE, false, vp + "/" + i); } return new NBTTagIntArray(b); }
            case 12: { JsonArray a = array(v, vp); long[] b = new long[a.size()]; for (int i = 0; i < b.length; i++) { b[i] = integer(a.get(i), Long.MIN_VALUE, Long.MAX_VALUE, true, vp + "/" + i); } return new NBTTagLongArray(b); }
            default: throw bad(p, "End is only permitted as an empty list elementType");
        }
    }
    static NBTBase floating(JsonElement v, int id, String p) {
        if (v == null || !v.isJsonPrimitive() || !v.getAsJsonPrimitive().isNumber()) { throw bad(p, "Expected finite number"); }
        String s = v.getAsString();
        try {
            new BigDecimal(s);
            if (id == 5) { float f = Float.parseFloat(s); if (Float.isFinite(f)) { return new NBTTagFloat(f); } }
            else { double d = Double.parseDouble(s); if (Double.isFinite(d)) { return new NBTTagDouble(d); } }
        } catch (NumberFormatException ex) { throw bad(p, "Invalid number"); }
        throw bad(p, "Floating point overflow; use raw IEEE bits");
    }
    private static int type(String s, String p) { for (int i = 0; i < TYPES.length; i++) { if (TYPES[i].equals(s)) { return i; } } throw bad(p, "Unknown NBT type " + s); }
    static boolean same(NBTBase a, NBTBase b) {
        return a != null && b != null && StrictCloneJson.write(encode(a)).equals(StrictCloneJson.write(encode(b)));
    }
    private static long[] longs(NBTBase t) {
        // 1.12's long array has no public accessor. Use the documented binary NBT envelope.
        try {
            NBTTagCompound root = new NBTTagCompound(); root.setTag("v", t);
            ByteArrayOutputStream b = new ByteArrayOutputStream(); CompressedStreamTools.write(root, new DataOutputStream(b));
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(b.toByteArray()));
            in.readByte(); in.readUTF(); in.readByte(); in.readUTF();
            long[] a = new long[in.readInt()]; for (int i = 0; i < a.length; i++) { a[i] = in.readLong(); } return a;
        } catch (IOException ex) { throw new IllegalArgumentException("Cannot read long array", ex); }
    }
    static NBTTagList emptyList(int type) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream(); DataOutputStream o = new DataOutputStream(b);
            o.writeByte(10); o.writeUTF(""); o.writeByte(9); o.writeUTF("v"); o.writeByte(type); o.writeInt(0); o.writeByte(0);
            return (NBTTagList)CompressedStreamTools.read(new DataInputStream(new ByteArrayInputStream(b.toByteArray()))).getTag("v");
        } catch (IOException ex) { throw new IllegalArgumentException("Cannot create typed empty list", ex); }
    }
}
