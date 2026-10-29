package noppes.npcs.rework.script;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class BytecodeCacheTest {
    private static Map<String, String> units(String... pairs) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) { m.put(pairs[i], pairs[i + 1]); }
        return m;
    }
    private static Map<String, byte[]> code(int size) { return Collections.singletonMap("A", new byte[size]); }

    @Test public void keyCoversUnitsOrderCompilerAndClasspath() {
        String base = BytecodeCache.key("javac|1.8|opts", "", units("A", "class A{}", "B", "class B{}"));
        assertEquals(base, BytecodeCache.key("javac|1.8|opts", "", units("A", "class A{}", "B", "class B{}")));
        assertNotEquals(base, BytecodeCache.key("javac|1.8|opts", "", units("A", "class A{ }", "B", "class B{}")));
        assertNotEquals(base, BytecodeCache.key("javac|1.8|opts", "", units("B", "class B{}", "A", "class A{}")));
        assertNotEquals(base, BytecodeCache.key("javac|1.8.0_2|opts", "", units("A", "class A{}", "B", "class B{}")));
        assertNotEquals(base, BytecodeCache.key("javac|1.8|opts", "x.jar", units("A", "class A{}", "B", "class B{}")));
        // Length prefixes: moving text between parts is a different key.
        assertNotEquals(BytecodeCache.key("c", "", units("AB", "C")), BytecodeCache.key("c", "", units("A", "BC")));
    }

    @Test public void changedApiJarAtTheSamePathInvalidates() throws Exception {
        Path jar = Files.createTempFile("m64-api", ".jar");
        try {
            Files.write(jar, new byte[] { 1, 2, 3 });
            String before = BytecodeCache.key("c", jar.toString(), units("A", "class A{}"));
            assertEquals(before, BytecodeCache.key("c", jar.toString(), units("A", "class A{}")));
            Files.write(jar, new byte[] { 1, 2, 3, 4 });
            assertNotEquals(before, BytecodeCache.key("c", jar.toString(), units("A", "class A{}")));
            String present = BytecodeCache.key("c", jar.toString() + File.pathSeparator, units("A", "class A{}"));
            Files.delete(jar);
            assertNotEquals(present, BytecodeCache.key("c", jar.toString() + File.pathSeparator, units("A", "class A{}")));
        } finally {
            Files.deleteIfExists(jar);
        }
    }

    @Test public void boundedLruByEntriesAndBytes() {
        BytecodeCache cache = new BytecodeCache(2, 100);
        cache.put("a", code(10)); cache.put("b", code(10));
        assertNotNull(cache.get("a")); // a is now most recently used
        cache.put("c", code(10));
        assertTrue(cache.contains("a")); assertFalse(cache.contains("b")); assertTrue(cache.contains("c"));
        cache.put("d", code(95)); // over the byte budget together with a or c: both evicted
        assertFalse(cache.contains("a")); assertFalse(cache.contains("c")); assertTrue(cache.contains("d"));
        cache.put("e", code(101)); // larger than the whole budget: never stored
        assertFalse(cache.contains("e"));
        long[] s = cache.stats();
        assertEquals(1, s[0]); assertEquals(95, s[1]); assertEquals(1, s[2]); assertEquals(0, s[3]); assertEquals(3, s[4]);
        assertNull(cache.get("b"));
        assertEquals(1, cache.stats()[3]);
    }

    @Test public void storesOnlyDefensiveImmutableBytes() {
        BytecodeCache cache = new BytecodeCache(4, 100);
        byte[] raw = { 7 };
        cache.put("a", Collections.singletonMap("A", raw));
        raw[0] = 0;
        assertEquals(7, cache.get("a").get("A")[0]);
        try { cache.get("a").put("B", raw); fail("mutable cache entry"); } catch (UnsupportedOperationException expected) { }
        cache.put("a", code(30)); // replacing an entry keeps the byte account exact
        assertEquals(30, cache.stats()[1]);
    }

    @Test public void cachedBytesGiveSeparateClassesAndStaticState() throws Exception {
        BytecodeCache cache = new BytecodeCache(4, 1 << 20);
        JdkScriptCompiler javac = new JdkScriptCompiler();
        Map<String, String> u = units("S", "public class S { public static int n; }");
        String key = BytecodeCache.key(javac.identity(), System.getProperty("java.class.path"), u);
        cache.put(key, javac.compile(u, System.getProperty("java.class.path")).bytecode());
        Map<String, byte[]> bytes = cache.get(key);
        Class<?> a = JdkScriptCompiler.newLoader(bytes, getClass().getClassLoader()).loadClass("S");
        Class<?> b = JdkScriptCompiler.newLoader(bytes, getClass().getClassLoader()).loadClass("S");
        assertNotSame(a, b);
        a.getField("n").setInt(null, 5);
        assertEquals(0, b.getField("n").getInt(null));
        assertFalse(javac.identity().isEmpty());
    }
}
