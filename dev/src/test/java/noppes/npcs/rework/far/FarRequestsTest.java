package noppes.npcs.rework.far;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import org.junit.Test;

/** M5-V.3: omejitev zahtev REQ in sporocili REQ/LOOK. */
public class FarRequestsTest {
    @Test
    public void samoPrejetiInVsakEnkrat() {
        FarRequests r = new FarRequests();
        r.offered(5);
        r.offered(0);
        r.request(5);
        r.request(7);
        r.request(5);
        r.request(0);
        assertEquals(5, r.next());
        assertEquals(0, r.next());
        assertEquals(4, r.requested);
        assertEquals(2, r.rejected);
        assertEquals(1, r.duplicate);
        assertFalse(r.hasPending());
        // Nov HELLO: klient je pozabil, a id mora znova prejeti.
        r.clear();
        r.request(5);
        assertEquals(0, r.next());
        r.offered(5);
        r.request(5);
        assertEquals(5, r.next());
    }

    @Test
    public void vrstaJeOmejena() {
        FarRequests r = new FarRequests();
        for (int i = 1; i <= FarRequests.QUEUE_CAP + 10; ++i) {
            r.offered(i);
            r.request(i);
        }
        assertEquals(10, r.dropped);
        int n = 0;
        while (r.next() != 0) {
            ++n;
        }
        assertEquals(FarRequests.QUEUE_CAP, n);
    }

    @Test
    public void sporociliReqInLook() {
        int[] ids = new int[300];
        for (int i = 0; i < ids.length; ++i) {
            ids[i] = 1 + i * 1000;
        }
        ByteBuf req = FarCodec.req(ids, 10, 300);
        int[] msg = FarCodec.readClientMessage(req);
        assertEquals(FarCodec.REQ, msg[0]);
        assertEquals(FarCodec.MAX_REQ, msg[1]);
        assertEquals(ids[10], msg[2]);
        assertEquals(ids[10 + FarCodec.MAX_REQ - 1], msg[1 + FarCodec.MAX_REQ]);

        byte[] data = {9, 8, 7, 6};
        final List<Object> got = new ArrayList<Object>();
        int type = FarCodec.readServerMessage(FarCodec.look(77, 0x1234567890ABCDEFL, data), new FarCodec.Sink() {
            @Override
            public void clear() {
            }

            @Override
            public void remove(int id) {
            }

            @Override
            public void entry(int id, int qx24, int qy24, int qz24, int yaw, int look, int flags) {
            }

            @Override
            public void look(int id, long hash, byte[] d) {
                got.add(id);
                got.add(hash);
                got.add(d);
            }
        });
        assertEquals(FarCodec.LOOK, type);
        assertEquals(77, got.get(0));
        assertEquals(0x1234567890ABCDEFL, got.get(1));
        assertArrayEquals(data, (byte[]) got.get(2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void prevelikReqJeNeveljaven() {
        ByteBuf buf = io.netty.buffer.Unpooled.buffer();
        buf.writeByte(FarCodec.REQ);
        FarCodec.writeVarInt(buf, FarCodec.MAX_REQ + 1);
        FarCodec.readClientMessage(buf);
    }
}
