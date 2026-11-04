package noppes.npcs.rework.linked;

import java.util.Arrays;

import org.junit.Test;
import static org.junit.Assert.*;

public class LinkedGuiCodecTest {
    private static final char Z_CARON = (char) 0x17E;
    private static final String GROUP = "Stra" + Z_CARON + "a", TEXT = (char) 0x10C + "akaj";

    private static String repeat(char c, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }

    private static byte[] request() {
        return new LinkedGuiCodec.Request(7, LinkedGuiCodec.Op.SET, LinkedCategories.STATS, GROUP).encode();
    }

    private static byte[] response() {
        return new LinkedGuiCodec.Response(7, LinkedGuiCodec.RESULT_NOTHING, LinkedCategories.ALL, true, 200, GROUP, TEXT).encode();
    }

    @Test public void requestRoundTripKeepsEveryFieldAndNonAsciiName() {
        LinkedGuiCodec.Request r = LinkedGuiCodec.request(request());
        assertEquals(7, r.id);
        assertEquals(LinkedGuiCodec.Op.SET, r.op);
        assertEquals(LinkedCategories.STATS, r.mask);
        assertEquals(GROUP, r.group);
    }

    @Test public void emptyGroupIsTheProbe() {
        LinkedGuiCodec.Request r = LinkedGuiCodec.request(new LinkedGuiCodec.Request(0, LinkedGuiCodec.Op.GET, 0, "").encode());
        assertEquals("", r.group);
        assertEquals(LinkedGuiCodec.Op.GET, r.op);
    }

    @Test public void responseRoundTripKeepsEveryField() {
        LinkedGuiCodec.Response r = LinkedGuiCodec.response(response());
        assertEquals(7, r.id);
        assertEquals(LinkedGuiCodec.RESULT_NOTHING, r.result);
        assertEquals(LinkedCategories.ALL, r.mask);
        assertTrue(r.undo);
        assertEquals(200, r.loaded);
        assertEquals(GROUP, r.group);
        assertEquals(TEXT, r.text);
    }

    @Test public void longestMessagesFitTheirLimits() {
        byte[] request = new LinkedGuiCodec.Request(1, LinkedGuiCodec.Op.GET, 0, repeat('a', LinkedGuiCodec.MAX_NAME)).encode();
        assertEquals(LinkedGuiCodec.MAX_REQUEST, request.length);
        assertNotNull(LinkedGuiCodec.request(request));
        byte[] response = new LinkedGuiCodec.Response(1, 0, 0, false, 0, repeat('a', LinkedGuiCodec.MAX_NAME), repeat('b', LinkedGuiCodec.MAX_TEXT)).encode();
        assertEquals(LinkedGuiCodec.MAX_RESPONSE, response.length);
        assertNotNull(LinkedGuiCodec.response(response));
    }

    @Test public void everyTruncatedMessageIsRejected() {
        byte[] request = request(), response = response();
        for (int i = 0; i < request.length; i++) {
            assertNull("request " + i, LinkedGuiCodec.request(Arrays.copyOf(request, i)));
        }
        for (int i = 0; i < response.length; i++) {
            assertNull("response " + i, LinkedGuiCodec.response(Arrays.copyOf(response, i)));
        }
    }

    @Test public void trailingBytesAreRejected() {
        assertNull(LinkedGuiCodec.request(Arrays.copyOf(request(), request().length + 1)));
        assertNull(LinkedGuiCodec.response(Arrays.copyOf(response(), response().length + 1)));
    }

    @Test public void wrongVersionKindAndNullAreRejected() {
        byte[] request = request();
        request[0] = 2;
        assertNull(LinkedGuiCodec.request(request));
        assertNull(LinkedGuiCodec.request(response()));
        assertNull(LinkedGuiCodec.response(request()));
        assertNull(LinkedGuiCodec.request(null));
        assertNull(LinkedGuiCodec.response(null));
    }

    @Test public void forgedFieldsAreRejected() {
        byte[] negativeId = request();
        negativeId[2] = (byte) 0x80;
        assertNull(LinkedGuiCodec.request(negativeId));
        byte[] op = request();
        op[6] = 3;
        assertNull(LinkedGuiCodec.request(op));
        byte[] mask = request();
        mask[9] = 1; // bit 8, above every category
        assertNull(LinkedGuiCodec.request(mask));
        byte[] result = response();
        result[6] = 4;
        assertNull(LinkedGuiCodec.response(result));
        byte[] undo = response();
        undo[11] = 2;
        assertNull(LinkedGuiCodec.response(undo));
    }

    @Test(expected = IllegalArgumentException.class) public void nameOverTheLimitCannotBeEncoded() {
        new LinkedGuiCodec.Request(1, LinkedGuiCodec.Op.GET, 0, repeat('a', LinkedGuiCodec.MAX_NAME + 1)).encode();
    }

    @Test public void nameLimitCountsBytesNotCharacters() {
        assertTrue(LinkedGuiCodec.validGroup(repeat('a', LinkedGuiCodec.MAX_NAME)));
        assertFalse(LinkedGuiCodec.validGroup(repeat(Z_CARON,LinkedGuiCodec.MAX_NAME / 2 + 1)));
        assertFalse(LinkedGuiCodec.validGroup(null));
    }

    @Test(expected = IllegalArgumentException.class) public void maskOutsideTheCategoriesCannotBeEncoded() {
        new LinkedGuiCodec.Request(1, LinkedGuiCodec.Op.SET, LinkedCategories.ALL + 1, "g").encode();
    }

    @Test public void everyCategoryMaskIsValid() {
        for (int mask = 0; mask <= LinkedCategories.ALL; mask++) {
            assertTrue(LinkedGuiCodec.validMask(mask));
        }
        assertFalse(LinkedGuiCodec.validMask(-1));
    }
}
