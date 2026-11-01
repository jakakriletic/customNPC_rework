package noppes.npcs.rework.anim;

import static org.junit.Assert.*;

import org.junit.Test;

/** P7b: fixed-size rwanim message, validated before use. */
public class BlendCodecTest {
    @Test public void roundTrip() {
        byte[] data = BlendCodec.encode(-1, 1234, 0x0123456789ABCDEFL, -42L, (77 << 8) | 3, 200);
        assertEquals(BlendCodec.SIZE, data.length);
        BlendCodec.Message m = BlendCodec.decode(data);
        assertNotNull(m);
        assertEquals(-1, m.dimension); assertEquals(1234, m.entityId);
        assertEquals(0x0123456789ABCDEFL, m.most); assertEquals(-42L, m.least);
        assertEquals((77 << 8) | 3, m.token); assertEquals(200, m.ticks);
    }

    @Test public void rejectsWrongSizeVersionTokenOrTicks() {
        byte[] ok = BlendCodec.encode(0, 1, 2, 3, 0x105, 10);
        assertNull(BlendCodec.decode(null));
        assertNull(BlendCodec.decode(new byte[BlendCodec.SIZE - 1]));
        assertNull(BlendCodec.decode(new byte[BlendCodec.SIZE + 1]));
        byte[] version = ok.clone(); version[0] = 2;
        assertNull(BlendCodec.decode(version));
        byte[] idle = ok.clone(); idle[1 + 4 + 4 + 16 + 3] = 0;   // low byte of the token: no action
        assertNull(BlendCodec.decode(idle));
        byte[] zero = ok.clone(); zero[BlendCodec.SIZE - 1] = 0; zero[BlendCodec.SIZE - 2] = 0;
        assertNull(BlendCodec.decode(zero));
        byte[] big = ok.clone(); big[BlendCodec.SIZE - 2] = 1;   // 266 ticks
        assertNull(BlendCodec.decode(big));
        byte[] negative = ok.clone(); negative[BlendCodec.SIZE - 2] = (byte) 0xFF;
        assertNull(BlendCodec.decode(negative));
        assertNotNull(BlendCodec.decode(ok));
    }

    @Test(expected = IllegalArgumentException.class) public void encodeRefusesAPlainPlay() {
        BlendCodec.encode(0, 1, 2, 3, 0x105, 0);
    }
}
