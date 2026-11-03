package si.ladja.xaerofleet.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CaptainSnapshotPacketTest {
    @Test
    public void roundTripsBoundedCaptainData() {
        UUID captainId = UUID.randomUUID();
        UUID shipId = UUID.randomUUID();
        CaptainView source = new CaptainView(captainId, shipId, 2,
                12.5, 64, -9.25, 37.5f, "ALLY", "SAILING", "Zeleni kapitan", true);
        CaptainSnapshotPacket encoded = new CaptainSnapshotPacket(Arrays.asList(source));
        ByteBuf bytes = Unpooled.buffer();
        encoded.toBytes(bytes);

        CaptainSnapshotPacket decoded = new CaptainSnapshotPacket();
        decoded.fromBytes(bytes);

        assertEquals(1, decoded.captains().size());
        CaptainView result = decoded.captains().get(0);
        assertEquals(captainId, result.captainId);
        assertEquals(shipId, result.shipId);
        assertEquals(2, result.dimension);
        assertEquals(12.5, result.x, 0.0);
        assertEquals(-9.25, result.z, 0.0);
        assertEquals("ALLY", result.faction);
        assertEquals("SAILING", result.orderState);
        assertEquals("Zeleni kapitan", result.label);
        assertTrue(result.commandable);
        assertEquals("", result.orderEngagement);
    }

    @Test
    public void carriesTheOrderEngagement() {
        CaptainView source = new CaptainView(UUID.randomUUID(), UUID.randomUUID(), 0,
                0, 64, 0, 0, "ALLY", "SAILING", "Napadalec", true, "ENGAGE");
        ByteBuf bytes = Unpooled.buffer();
        new CaptainSnapshotPacket(Arrays.asList(source)).toBytes(bytes);
        CaptainSnapshotPacket decoded = new CaptainSnapshotPacket();
        decoded.fromBytes(bytes);
        assertEquals("ENGAGE", decoded.captains().get(0).orderEngagement);
        assertTrue(decoded.captains().get(0).isEngaging());
    }

    @Test
    public void trimsUntrustedStringsBeforeEncoding() {
        CaptainView view = new CaptainView(UUID.randomUUID(), UUID.randomUUID(), 0,
                0, 0, 0, 0, repeat('f', 40), repeat('s', 40), repeat('l', 100), false);
        assertEquals(16, view.faction.length());
        assertEquals(24, view.orderState.length());
        assertEquals(64, view.label.length());
    }

    private static String repeat(char value, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, value);
        return new String(chars);
    }
}
