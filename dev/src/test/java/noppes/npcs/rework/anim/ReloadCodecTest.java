package noppes.npcs.rework.anim;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.TreeMap;
import org.junit.Test;

/** P7c: reload messages, bundles and the part assembler; every malformed input is refused. */
public class ReloadCodecTest {
    private static TreeMap<String, byte[]> files() {
        TreeMap<String, byte[]> files = new TreeMap<>();
        files.put("cave_bear", "{\"a\":1}".getBytes(StandardCharsets.UTF_8));
        files.put("troll", "{}".getBytes(StandardCharsets.UTF_8));
        return files;
    }

    @Test public void idsCannotNameAnotherPath() {
        assertTrue(ReloadCodec.validId("cave_bear"));
        for (String id : new String[]{null, "", "../evil", "a/b", "a\\b", "a.b", "A", "a b", "con:"}) { assertFalse(String.valueOf(id), ReloadCodec.validId(id)); }
    }

    @Test public void bundleRoundTripIsSortedAndStable() {
        byte[] bundle = ReloadCodec.encodeBundle(files());
        TreeMap<String, byte[]> back = ReloadCodec.decodeBundle(bundle);
        assertEquals("[cave_bear, troll]", back.keySet().toString());
        assertArrayEquals(files().get("troll"), back.get("troll"));
        assertArrayEquals(bundle, ReloadCodec.encodeBundle(back));
        assertEquals(ReloadCodec.HASH_SIZE, ReloadCodec.hash(bundle).length);
        assertEquals(0, ReloadCodec.decodeBundle(ReloadCodec.encodeBundle(new TreeMap<>())).size());
    }

    @Test public void malformedBundlesAreRefused() {
        byte[] bundle = ReloadCodec.encodeBundle(files());
        assertNull(ReloadCodec.decodeBundle(null));
        assertNull(ReloadCodec.decodeBundle(new byte[3]));
        assertNull("trailing data", ReloadCodec.decodeBundle(Arrays.copyOf(bundle, bundle.length + 1)));
        assertNull("truncated", ReloadCodec.decodeBundle(Arrays.copyOf(bundle, bundle.length - 1)));
        byte[] huge = bundle.clone();
        huge[0] = 127;
        assertNull("count", ReloadCodec.decodeBundle(huge));
    }

    @Test public void largeBundleSurvivesPartsInOrder() {
        byte[] bundle = new byte[ReloadCodec.PART_SIZE * 2 + 17];
        new Random(5).nextBytes(bundle);
        byte[][] parts = ReloadCodec.parts(7, bundle);
        assertEquals(3, parts.length);
        ReloadCodec.Assembler a = new ReloadCodec.Assembler();
        byte[] whole = null;
        for (byte[] p : parts) {
            assertTrue(ReloadCodec.isReload(p[0]));
            assertNull(whole);
            ReloadCodec.Message m = ReloadCodec.decode(p);
            assertEquals(7, m.revision);
            whole = a.accept(m);
        }
        assertArrayEquals(bundle, whole);
    }

    @Test public void partOutOfOrderDropsTheBundle() {
        byte[][] parts = ReloadCodec.parts(7, new byte[ReloadCodec.PART_SIZE * 2 + 1]);
        ReloadCodec.Assembler a = new ReloadCodec.Assembler();
        assertNull(a.accept(ReloadCodec.decode(parts[0])));
        assertNull(a.accept(ReloadCodec.decode(parts[2])));
        assertNull("nothing left to continue", a.accept(ReloadCodec.decode(parts[1])));
    }

    @Test public void messagesRoundTripAndWrongSizesAreRefused() {
        byte[] hash = ReloadCodec.hash(new byte[]{1});
        ReloadCodec.Message c = ReloadCodec.decode(ReloadCodec.commit(3, hash));
        assertEquals(ReloadCodec.COMMIT, c.kind);
        assertArrayEquals(hash, c.hash);
        assertEquals(4, ReloadCodec.decode(ReloadCodec.abort(4)).revision);
        ReloadCodec.Message ack = ReloadCodec.decode(ReloadCodec.ack(5, true, hash));
        assertTrue(ack.ok);
        assertFalse(ReloadCodec.decode(ReloadCodec.ack(5, false, hash)).ok);
        assertNull(ReloadCodec.decode(null));
        assertNull(ReloadCodec.decode(new byte[]{ReloadCodec.COMMIT, 0, 0, 0, 1}));
        assertNull("revision 0", ReloadCodec.decode(ReloadCodec.abort(0)));
        assertNull("too large", ReloadCodec.decode(new byte[ReloadCodec.MAX_MESSAGE + 1]));
        assertNull("blend version is not a reload kind", ReloadCodec.decode(new byte[]{BlendCodec.VERSION, 0, 0, 0, 1}));
        assertFalse(ReloadCodec.isReload(BlendCodec.VERSION));
    }

    @Test public void partRangesAreCheckedBeforeAllocation() {
        byte[] part = ReloadCodec.parts(1, new byte[10])[0];
        byte[] overTotal = part.clone();
        overTotal[5] = 127;
        assertNull("total above the limit", ReloadCodec.decode(overTotal));
        byte[] overRange = part.clone();
        overRange[12] = 5;
        assertNull("offset past the total", ReloadCodec.decode(overRange));
    }
}
