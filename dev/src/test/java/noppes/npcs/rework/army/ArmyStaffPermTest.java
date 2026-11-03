package noppes.npcs.rework.army;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

/** M11.8b: kind 5 rights requests of the Permissions tab. */
public class ArmyStaffPermTest {
    private byte[] request() { return new ArmyStaffCodec.PermRequest(17, ArmyStaffCodec.PermOp.GRANT, 1, "Ally_2", "alpha").encode(); }
    @Test public void everyActionAndKindRoundTrips() {
        for (ArmyStaffCodec.PermOp op : ArmyStaffCodec.PermOp.values()) {
            for (int kind = 0; kind < ArmyStaffCodec.KINDS.length; kind++) {
                ArmyStaffCodec.PermRequest r = ArmyStaffCodec.permRequest(new ArmyStaffCodec.PermRequest(9, op, kind, "M118B_Ally", "all").encode());
                assertNotNull(r); assertEquals(op, r.op); assertEquals(kind, r.kind); assertEquals(9, r.id);
                assertEquals("M118B_Ally", r.player); assertEquals("all", r.army);
            }
        }
    }
    @Test public void emptyFieldsAndMaximumFitBound() {
        ArmyStaffCodec.PermRequest r = ArmyStaffCodec.permRequest(new ArmyStaffCodec.PermRequest(0, ArmyStaffCodec.PermOp.PLAYERS, 0, "", "").encode());
        assertEquals("", r.player); assertEquals("", r.army);
        String who = String.join("", Collections.nCopies(16, "p")), army = String.join("", Collections.nCopies(32, "x"));
        byte[] bytes = new ArmyStaffCodec.PermRequest(0, ArmyStaffCodec.PermOp.REVOKE, 2, who, army).encode();
        assertEquals(ArmyStaffCodec.MAX_PERM_REQUEST, bytes.length);
        assertEquals(army, ArmyStaffCodec.permRequest(bytes).army); assertEquals(who, ArmyStaffCodec.permRequest(bytes).player);
    }
    @Test public void allTruncationsAndTrailingBytesAreRejected() {
        byte[] b = request();
        for (int i = 0; i < b.length; i++) { assertNull(ArmyStaffCodec.permRequest(Arrays.copyOf(b, i))); }
        assertNull(ArmyStaffCodec.permRequest(Arrays.copyOf(b, b.length + 1)));
        assertNull(ArmyStaffCodec.permRequest(null));
        assertNull(ArmyStaffCodec.permRequest(new byte[ArmyStaffCodec.MAX_PERM_REQUEST + 1]));
    }
    @Test public void invalidVersionKindActionAndRightAreRejected() {
        for (int offset : new int[] {0, 1, 6, 7}) {
            byte[] b = request(); b[offset] = 127; assertNull("offset " + offset, ArmyStaffCodec.permRequest(b));
        }
        byte[] b = request(); b[7] = 3; assertNull(ArmyStaffCodec.permRequest(b));
        b = request(); b[2] = -1; assertNull(ArmyStaffCodec.permRequest(b));
    }
    @Test public void invalidPlayerAndArmyBytesAreRejected() {
        byte[] b = request(); b[9] = '-'; assertNull("player has no dash", ArmyStaffCodec.permRequest(b));
        b = request(); b[9] = -1; assertNull(ArmyStaffCodec.permRequest(b));
        b = request(); b[8] = 17; assertNull(ArmyStaffCodec.permRequest(b));
        b = request(); b[b.length - 1] = ' '; assertNull(ArmyStaffCodec.permRequest(b));
    }
    @Test(expected = IllegalArgumentException.class) public void clientCannotEncodeLongPlayer() {
        new ArmyStaffCodec.PermRequest(0, ArmyStaffCodec.PermOp.GRANTS, 0, "seventeen_chars_x", "").encode();
    }
    @Test public void kindsAreSeparateFromStaffAndSnapshot() {
        byte[] b = request();
        assertEquals(ArmyStaffCodec.PERM_REQUEST, ArmyStaffCodec.kind(b));
        assertNull(ArmyStaffCodec.request(b)); assertNull(ArmyStaffCodec.response(b));
        assertEquals(-1, ArmySnapshot.decodeRequest(b));
        assertNull(ArmyStaffCodec.permRequest(new ArmyStaffCodec.Request(1, ArmyStaffCodec.Op.STOP, -1, "alpha", 0, 0).encode()));
        assertNull(ArmyStaffCodec.permRequest(ArmySnapshot.encodeRequest(2)));
    }
    @Test public void kindWordsMatchTheCommand() {
        assertArrayEquals(new String[] {"view", "command", ArmyCommandService.ALL}, ArmyStaffCodec.KINDS);
    }
    @Test public void switchOffTouchesNoPlayerOrWorld() {
        ArmyCommandService.setSwitch(0);
        ArmyStaffCodec.Response r = ArmyStaffServer.executePerm(null, new ArmyStaffCodec.PermRequest(4, ArmyStaffCodec.PermOp.GRANT, 0, "Ally", "alpha"));
        assertFalse(r.ok); assertEquals(4, r.id); assertEquals(-1, r.npc); assertEquals("RWARMY off", r.text);
    }
}
