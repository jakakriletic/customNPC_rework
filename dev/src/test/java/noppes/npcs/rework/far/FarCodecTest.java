package noppes.npcs.rework.far;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

/** M5-V.2: kodirnik kanala {@code rwfar}. */
public class FarCodecTest {
    /** Zapise razbrano sporocilo kot seznam nizov. */
    private static final class Rec implements FarCodec.Sink {
        final List<String> out = new ArrayList<String>();
        final List<int[]> entries = new ArrayList<int[]>();

        @Override
        public void clear() {
            this.out.add("clear");
        }

        @Override
        public void remove(int id) {
            this.out.add("remove " + id);
        }

        @Override
        public void entry(int id, int qx24, int qy24, int qz24, int yaw, int look, int flags) {
            this.out.add("entry " + id);
            this.entries.add(new int[] {id, qx24, qy24, qz24, yaw, look, flags});
        }
    }

    @Test
    public void varintKotVanilla() {
        int[] values = {0, 1, 127, 128, 255, 16383, 16384, 2097151, 2097152, Integer.MAX_VALUE, -1};
        int[] sizes = {1, 1, 1, 2, 2, 2, 3, 3, 4, 5, 5};
        for (int i = 0; i < values.length; ++i) {
            ByteBuf b = Unpooled.buffer();
            FarCodec.writeVarInt(b, values[i]);
            assertEquals("velikost " + values[i], sizes[i], b.readableBytes());
            assertEquals(sizes[i], FarCodec.varIntSize(values[i]));
            assertEquals(values[i], FarCodec.readVarInt(b));
        }
    }

    @Test
    public void vnosJeSestnajstBajtovZaObicajenId() {
        ByteBuf b = Unpooled.buffer();
        // id 5.000 (2 B varint), videz 0 (1 B): 2 + 9 + 1 + 1 + 1 = 14 B; id ~1.000.000 (3 B) z videzom
        // 300 (2 B) = 16 B (raziskava M5-V.0 §5.1: ~16 B).
        FarCodec.writeEntry(b, 5000, 1, 2, 3, 4, 0, 0);
        assertEquals(14, b.readableBytes());
        b.clear();
        FarCodec.writeEntry(b, 1000000, 1, 2, 3, 4, 300, 0);
        assertEquals(16, b.readableBytes());
    }

    @Test
    public void razvoj24BitovOkoliIgralca() {
        Random r = new Random(42L);
        for (int i = 0; i < 100000; ++i) {
            // Svet do +-30.000.000 blokov = +-240.000.000 v 1/8; NPC do 2^20 blokov od igralca.
            int ref = r.nextInt(480000000) - 240000000;
            int d = r.nextInt(1 << 23) - (1 << 22);
            int v = ref + d;
            assertEquals(v, FarCodec.unwrap24(v & 0xFFFFFF, ref));
        }
        assertEquals(-1, FarCodec.unwrap24(0xFFFFFF, 0));
        assertEquals(-8 * 368, FarCodec.unwrap24((-8 * 368) & 0xFFFFFF, 0));
    }

    @Test
    public void kvantizacijaInYaw() {
        assertEquals(4, FarCodec.quantize(0.5));
        assertEquals(-1, FarCodec.quantize(-0.01));
        assertEquals(24004, FarCodec.quantize(3000.5));
        assertEquals(0, FarCodec.yawByte(0.0f));
        assertEquals(128, FarCodec.yawByte(180.0f));
        assertEquals(192, FarCodec.yawByte(-90.0f));
        assertEquals(64, FarCodec.yawByte(450.0f));
        assertEquals(2, FarCodec.yawDiff(255, 1));
        assertEquals(128, FarCodec.yawDiff(0, 128));
        assertEquals(10, FarCodec.yawDiff(5, 251));
    }

    @Test
    public void posnetekSeRazbereNazaj() {
        int[] removed = {7, 300000};
        int[] entries = {
            1234, -2944 & 0xFFFFFF, 128, 23988, 128, 0, FarCodec.F_WALKING,
            99999, 100, 32, -100, 5, 17, FarCodec.F_SITTING | FarCodec.F_HALF_VISIBLE,
        };
        List<ByteBuf> msgs = FarCodec.snaps(removed, 2, entries, 2);
        assertEquals(1, msgs.size());
        Rec rec = new Rec();
        assertEquals(FarCodec.SNAP, FarCodec.readServerMessage(msgs.get(0), rec));
        assertEquals("[remove 7, remove 300000, entry 1234, entry 99999]", rec.out.toString());
        assertArrayEquals(new int[] {1234, -2944 & 0xFFFFFF, 128, 23988, 128, 0, FarCodec.F_WALKING},
                rec.entries.get(0));
        assertArrayEquals(new int[] {99999, 100, 32, -100 & 0xFFFFFF, 5, 17,
            FarCodec.F_SITTING | FarCodec.F_HALF_VISIBLE}, rec.entries.get(1));
        assertTrue("prebrano do konca", !msgs.get(0).isReadable());
    }

    @Test
    public void velikPosnetekSeRazdeliInNicNeIzgubi() {
        int n = 5000;
        int[] entries = new int[n * FarCodec.ENTRY_INTS];
        for (int i = 0; i < n; ++i) {
            entries[i * FarCodec.ENTRY_INTS] = 100 + i;
        }
        int[] removed = new int[1500];
        for (int i = 0; i < removed.length; ++i) {
            removed[i] = 20000 + i;
        }
        List<ByteBuf> msgs = FarCodec.snaps(removed, removed.length, entries, n);
        assertEquals((int) Math.ceil((n + 1500) / (double) FarCodec.MAX_ITEMS), msgs.size());
        Rec rec = new Rec();
        int bytes = 0;
        for (ByteBuf b : msgs) {
            bytes += b.readableBytes();
            FarCodec.readServerMessage(b, rec);
        }
        assertEquals(n + 1500, rec.out.size());
        for (int i = 0; i < 1500; ++i) {
            assertEquals("remove " + (20000 + i), rec.out.get(i));
        }
        for (int i = 0; i < n; ++i) {
            assertEquals(100 + i, rec.entries.get(i)[0]);
        }
        // 5.000 vnosov z 2-bajtnim id-jem: 14 B; skupaj s 1.500 odstranitvami pod 80 KB (ocena §5.1).
        assertTrue("bajtov " + bytes, bytes < 80000);
        assertTrue(FarCodec.snaps(removed, 0, entries, 0).isEmpty());
    }

    @Test
    public void sporocilaKlienta() {
        ByteBuf h = FarCodec.hello(384);
        assertArrayEquals(new int[] {FarCodec.HELLO, FarCodec.VERSION, 384}, FarCodec.readClientMessage(h));
        assertArrayEquals(new int[] {FarCodec.BYE, 0, 0}, FarCodec.readClientMessage(FarCodec.bye()));
        ByteBuf unknown = Unpooled.buffer();
        unknown.writeByte(9);
        assertNull(FarCodec.readClientMessage(unknown));
        assertNull(FarCodec.readClientMessage(Unpooled.buffer()));
        Rec rec = new Rec();
        assertEquals(FarCodec.CLEAR, FarCodec.readServerMessage(FarCodec.clear(), rec));
        assertEquals("[clear]", rec.out.toString());
    }
}
