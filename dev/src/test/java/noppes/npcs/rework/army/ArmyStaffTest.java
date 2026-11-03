package noppes.npcs.rework.army;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArmyStaffTest {
    private byte[] request() { return new ArmyStaffCodec.Request(17, ArmyStaffCodec.Op.MOVE, 42, "alpha", 3, 0).encode(); }
    @Test public void everyActionAndShapeRoundTrips() {
        for (ArmyStaffCodec.Op op : ArmyStaffCodec.Op.values()) {
            for (int shape = 0; shape < 4; shape++) {
                ArmyStaffCodec.Request r = ArmyStaffCodec.request(new ArmyStaffCodec.Request(9, op, -1, "A_12-z", shape, 1).encode());
                assertNotNull(r); assertEquals(op, r.op); assertEquals(shape, r.shape); assertEquals(1, r.stance);
                assertEquals("A_12-z", r.army); assertEquals(-1, r.npc); assertEquals(9, r.id);
            }
        }
    }
    @Test public void emptyNameCanUnassignAndMaxNameFitsBound() {
        assertEquals("", ArmyStaffCodec.request(new ArmyStaffCodec.Request(0, ArmyStaffCodec.Op.ASSIGN, 4, "", 0, 0).encode()).army);
        String name = String.join("", java.util.Collections.nCopies(32, "x"));
        byte[] bytes = new ArmyStaffCodec.Request(0, ArmyStaffCodec.Op.NEAR, -1, name, 0, 0).encode();
        assertEquals(ArmyStaffCodec.MAX_REQUEST, bytes.length); assertEquals(name, ArmyStaffCodec.request(bytes).army);
    }
    @Test public void allTruncationsAndTrailingBytesAreRejected() {
        byte[] b = request();
        for (int i = 0; i < b.length; i++) { assertNull(ArmyStaffCodec.request(Arrays.copyOf(b, i))); }
        assertNull(ArmyStaffCodec.request(Arrays.copyOf(b, b.length + 1))); assertNull(ArmyStaffCodec.request(null));
    }
    @Test public void invalidVersionKindAndActionAreRejected() {
        for (int offset : new int[] {0, 1, 6, 11, 12}) {
            byte[] b = request(); b[offset] = 127; assertNull("offset " + offset, ArmyStaffCodec.request(b));
        }
    }
    @Test public void negativeIdAndInvalidNameAreRejected() {
        byte[] b = request(); b[2] = -1; assertNull(ArmyStaffCodec.request(b));
        b = request(); b[14] = 32; assertNull(ArmyStaffCodec.request(b));
        b = request(); b[14] = -1; assertNull(ArmyStaffCodec.request(b));
        b = request(); b[13] = 33; assertNull(ArmyStaffCodec.request(b));
    }
    @Test(expected = IllegalArgumentException.class) public void clientCannotEncodeInvalidName() {
        new ArmyStaffCodec.Request(0, ArmyStaffCodec.Op.MOVE, -1, "x y", 0, 0).encode();
    }
    @Test public void responsePreservesCorrelationAndUnicodeMessage() {
        ArmyStaffCodec.Response r = ArmyStaffCodec.response(new ArmyStaffCodec.Response(7, false, 42, "alpha", "RWARMY četa").encode());
        assertNotNull(r); assertFalse(r.ok); assertEquals(7, r.id); assertEquals(42, r.npc);
        assertEquals("alpha", r.army); assertEquals("RWARMY četa", r.text);
    }
    @Test public void badResponsesAreRejectedWithoutAllocationFromLengths() {
        byte[] b = new ArmyStaffCodec.Response(0, true, -1, "", "ok").encode();
        for (int i = 0; i < b.length; i++) { assertNull(ArmyStaffCodec.response(Arrays.copyOf(b, i))); }
        assertNull(ArmyStaffCodec.response(Arrays.copyOf(b, b.length + 1)));
        byte[] c = b.clone(); c[6] = 2; assertNull(ArmyStaffCodec.response(c));
        c = b.clone(); c[11] = 127; assertNull(ArmyStaffCodec.response(c));
        c = b.clone(); c[12] = 127; assertNull(ArmyStaffCodec.response(c));
        assertNull(ArmyStaffCodec.response(new byte[ArmyStaffCodec.MAX_RESPONSE + 1]));
    }
    @Test public void snapshotProtocolIsStillSeparate() {
        assertNull(ArmyStaffCodec.request(ArmySnapshot.encodeRequest(2)));
        assertEquals(-1, ArmySnapshot.decodeRequest(request()));
        assertNull(ArmyStaffCodec.response(request()));
    }
    @Test public void switchOffTouchesNoPlayerOrWorld() {
        ArmyCommandService.setSwitch(0);
        assertFalse(ArmyStaffServer.execute(null, new ArmyStaffCodec.Request(0, ArmyStaffCodec.Op.MOVE, -1, "alpha", 0, 0)).ok);
    }
}
