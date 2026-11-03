package si.ladja.xaerofleet.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class MoveOrderPacketTest {
    @Test
    public void roundTripsSelectedCaptainsAndZone() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        MoveOrderPacket source = new MoveOrderPacket(Arrays.asList(first, second),
                -1, 123.25, -456.5, 34);
        ByteBuf bytes = Unpooled.buffer();
        source.toBytes(bytes);

        MoveOrderPacket decoded = new MoveOrderPacket();
        decoded.fromBytes(bytes);

        assertEquals(Arrays.asList(first, second), decoded.captainIds());
        assertEquals(-1, decoded.dimension());
        assertEquals(123.25, decoded.centerX(), 0.0D);
        assertEquals(-456.5, decoded.centerZ(), 0.0D);
        assertEquals(34, decoded.radius(), 0.0D);
    }
}
