package si.ladja.xaerofleet.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EngagementOrderPacketTest {

    @Test
    public void roundTripsModeAndDeduplicatesIds() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        ByteBuf bytes = Unpooled.buffer();
        new EngagementOrderPacket(Arrays.asList(a, b, a), true).toBytes(bytes);
        EngagementOrderPacket decoded = new EngagementOrderPacket();
        decoded.fromBytes(bytes);
        assertTrue(decoded.engage());
        assertEquals(Arrays.asList(a, b), decoded.captainIds());

        bytes = Unpooled.buffer();
        new EngagementOrderPacket(Arrays.asList(a), false).toBytes(bytes);
        decoded = new EngagementOrderPacket();
        decoded.fromBytes(bytes);
        assertFalse(decoded.engage());
    }

    @Test(expected = IllegalArgumentException.class)
    public void refusesAnUnknownMode() {
        ByteBuf bytes = Unpooled.buffer();
        bytes.writeByte(7);
        bytes.writeByte(1);
        bytes.writeLong(1L);
        bytes.writeLong(2L);
        new EngagementOrderPacket().fromBytes(bytes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void refusesAnEmptyFleet() {
        ByteBuf bytes = Unpooled.buffer();
        bytes.writeByte(EngagementOrderPacket.ENGAGE);
        bytes.writeByte(0);
        new EngagementOrderPacket().fromBytes(bytes);
    }
}
