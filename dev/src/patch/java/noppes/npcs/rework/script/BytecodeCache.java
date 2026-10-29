package noppes.npcs.rework.script;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** M6.4 (Q130): bounded LRU of compiled bytes only, never live classes, loaders, instances or
 * MethodHandles, so sharing it cannot share script state. The key covers the ordered units,
 * compiler identity/options and the API classpath with a size/mtime fingerprint of every entry.
 */
public final class BytecodeCache {
    private final int maxEntries;
    private final long maxBytes;
    private final LinkedHashMap<String, Map<String, byte[]>> entries = new LinkedHashMap<>(16, 0.75f, true);
    private long bytes, hits, misses, evictions;

    public BytecodeCache(int maxEntries, long maxBytes) {
        if (maxEntries < 1 || maxBytes < 1) { throw new IllegalArgumentException("Empty cache bound"); }
        this.maxEntries = maxEntries; this.maxBytes = maxBytes;
    }

    /** Unmodifiable bytes for the key, or null; callers only define classes from them. */
    public synchronized Map<String, byte[]> get(String key) {
        Map<String, byte[]> code = entries.get(key);
        if (code == null) { misses++; } else { hits++; }
        return code;
    }

    /** Stores a defensive copy; an entry larger than the whole budget is not kept. */
    public synchronized void put(String key, Map<String, byte[]> code) {
        Map<String, byte[]> copy = new LinkedHashMap<>();
        long size = 0;
        for (Map.Entry<String, byte[]> e : code.entrySet()) { copy.put(e.getKey(), e.getValue().clone()); size += e.getValue().length; }
        Map<String, byte[]> old = entries.remove(key);
        if (old != null) { bytes -= size(old); }
        if (size > maxBytes) { return; }
        entries.put(key, Collections.unmodifiableMap(copy));
        bytes += size;
        Iterator<Map<String, byte[]>> eldest = entries.values().iterator();
        while ((entries.size() > maxEntries || bytes > maxBytes) && eldest.hasNext()) {
            bytes -= size(eldest.next());
            eldest.remove();
            evictions++;
        }
    }

    public synchronized boolean contains(String key) { return entries.containsKey(key); }
    /** entries, bytes, hits, misses, evictions. */
    public synchronized long[] stats() { return new long[] { entries.size(), bytes, hits, misses, evictions }; }

    private static long size(Map<String, byte[]> code) {
        long size = 0;
        for (byte[] b : code.values()) { size += b.length; }
        return size;
    }

    /** SHA-256 over length-prefixed parts, so no concatenation of two parts can collide with another. */
    public static String key(String compiler, String classpath, Map<String, String> units) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            part(sha, compiler);
            part(sha, classpath);
            part(sha, fingerprint(classpath));
            for (Map.Entry<String, String> unit : units.entrySet()) { part(sha, unit.getKey()); part(sha, unit.getValue()); }
            StringBuilder hex = new StringBuilder();
            for (byte b : sha.digest()) { hex.append(String.format("%02x", b)); }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** A changed, added or removed API jar changes the key. A directory entry (dev only) counts by its own mtime. */
    static String fingerprint(String classpath) {
        StringBuilder out = new StringBuilder();
        for (String path : classpath.split(File.pathSeparator)) {
            if (path.isEmpty()) { continue; }
            File f = new File(path);
            out.append(path).append('|').append(f.isFile() ? "f" : f.isDirectory() ? "d" : "-")
                    .append('|').append(f.isFile() ? f.length() : 0).append('|').append(f.lastModified()).append('\n');
        }
        return out.toString();
    }

    private static void part(MessageDigest sha, String s) {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        sha.update(new byte[] { (byte) (b.length >>> 24), (byte) (b.length >>> 16), (byte) (b.length >>> 8), (byte) b.length });
        sha.update(b);
    }
}
