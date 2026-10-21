package noppes.npcs.rework.far;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * M5-V.2: oblika sporocil kanala {@code rwfar} (raziskava M5-V.0 §5.1, tocke 5-6).
 *
 * <p>Vsako sporocilo se zacne z bajtom vrste:
 * <ul>
 *   <li>{@link #HELLO} (klient -&gt; streznik): {@code varint verzija}, {@code varint zeleni domet} v blokih;</li>
 *   <li>{@link #BYE} (klient -&gt; streznik): klient nadomestkov ne zeli vec;</li>
 *   <li>{@link #SNAP} (streznik -&gt; klient): {@code varint m}, {@code m x varint id} odstranjenih,
 *       {@code varint n}, {@code n} vnosov;</li>
 *   <li>{@link #CLEAR} (streznik -&gt; klient): pozabi vse nadomestke (izklop na strezniku).</li>
 * </ul>
 * Vnos: {@code varint entityId}, pozicija 3 x 24 bitov v 1/8 bloka (spodnjih 24 bitov; klient jih
 * razvije okoli svoje pozicije, {@link #unwrap24}, zato velja na vsaki koordinati sveta, dokler je NPC
 * blizje od 2^20 blokov), {@code yaw} 1 bajt (256 korakov po 1,40625 stopinje), {@code varint videz}
 * (M5-V.3; do takrat 0), zastavice 1 bajt ({@link #F_WALKING} ...). Mirujoc NPC tako stane ~16 B enkrat.
 *
 * <p>Brez Minecraft tipov (samo Netty), testljivo brez sveta.
 */
public final class FarCodec {
    public static final int VERSION = 1;
    public static final String CHANNEL = "rwfar";

    public static final int HELLO = 0;
    public static final int SNAP = 1;
    public static final int CLEAR = 2;
    public static final int BYE = 3;

    /** Najvec odstranitev + vnosov v enem sporocilu (~16 KB). */
    public static final int MAX_ITEMS = 1024;
    /** Stevilo int-ov na vnos v ploskem polju: id, qx, qy, qz, yaw, videz, zastavice. */
    public static final int ENTRY_INTS = 7;

    public static final int F_WALKING = 1;
    public static final int F_SITTING = 1 << 1;
    public static final int F_LYING = 1 << 2;
    public static final int F_RIDING = 1 << 3;
    public static final int F_HALF_VISIBLE = 1 << 4;
    public static final int F_ATTACKING = 1 << 5;
    public static final int F_DEAD = 1 << 6;

    private static final int MASK24 = 0xFFFFFF;

    private FarCodec() {
    }

    /** Prejemnik razbranega sporocila streznika. */
    public interface Sink {
        void clear();

        void remove(int id);

        /** Pozicija je v spodnjih 24 bitih (glej {@link #unwrap24}). */
        void entry(int id, int qx24, int qy24, int qz24, int yaw, int look, int flags);
    }

    // --- osnovni tipi ---------------------------------------------------------

    public static void writeVarInt(ByteBuf buf, int value) {
        while ((value & ~0x7F) != 0) {
            buf.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        buf.writeByte(value);
    }

    public static int readVarInt(ByteBuf buf) {
        int value = 0;
        int shift = 0;
        while (true) {
            if (shift >= 35) {
                throw new IllegalArgumentException("varint je predolg");
            }
            int b = buf.readUnsignedByte();
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return value;
            }
            shift += 7;
        }
    }

    public static int varIntSize(int value) {
        int n = 1;
        while ((value & ~0x7F) != 0) {
            ++n;
            value >>>= 7;
        }
        return n;
    }

    /** Koordinata v 1/8 bloka (floor). */
    public static int quantize(double v) {
        return (int) Math.floor(v * 8.0);
    }

    /** Yaw v 256 korakih, kot vanilla v paketih entitet. */
    public static int yawByte(float yaw) {
        return ((int) Math.floor(yaw * 256.0f / 360.0f)) & 0xFF;
    }

    /** Najmanjsa razlika dveh bajtov yaw (0..128). */
    public static int yawDiff(int a, int b) {
        int d = (a - b) & 0xFF;
        return d > 128 ? 256 - d : d;
    }

    /** Polna koordinata iz spodnjih 24 bitov: tista, ki je najblizje {@code ref} (oboje v 1/8 bloka). */
    public static int unwrap24(int low24, int ref) {
        int d = (low24 - ref) & MASK24;
        if (d >= 0x800000) {
            d -= 0x1000000;
        }
        return ref + d;
    }

    // --- sporocila klienta ----------------------------------------------------

    public static ByteBuf hello(int range) {
        ByteBuf buf = Unpooled.buffer(8);
        buf.writeByte(HELLO);
        writeVarInt(buf, VERSION);
        writeVarInt(buf, Math.max(0, range));
        return buf;
    }

    public static ByteBuf bye() {
        ByteBuf buf = Unpooled.buffer(1);
        buf.writeByte(BYE);
        return buf;
    }

    /**
     * Razbere sporocilo klienta. Vrne {@code {vrsta, verzija, domet}} za HELLO, {@code {BYE, 0, 0}}
     * za BYE, sicer {@code null} (neznana vrsta se zavrze).
     */
    public static int[] readClientMessage(ByteBuf buf) {
        if (!buf.isReadable()) {
            return null;
        }
        int type = buf.readUnsignedByte();
        if (type == HELLO) {
            int version = readVarInt(buf);
            int range = readVarInt(buf);
            return new int[] {HELLO, version, range};
        }
        if (type == BYE) {
            return new int[] {BYE, 0, 0};
        }
        return null;
    }

    // --- sporocila streznika --------------------------------------------------

    public static ByteBuf clear() {
        ByteBuf buf = Unpooled.buffer(1);
        buf.writeByte(CLEAR);
        return buf;
    }

    public static void writeEntry(ByteBuf buf, int id, int qx, int qy, int qz, int yaw, int look, int flags) {
        writeVarInt(buf, id);
        buf.writeMedium(qx & MASK24);
        buf.writeMedium(qy & MASK24);
        buf.writeMedium(qz & MASK24);
        buf.writeByte(yaw & 0xFF);
        writeVarInt(buf, look);
        buf.writeByte(flags & 0xFF);
    }

    /**
     * Posnetek razdeli na sporocila z najvec {@link #MAX_ITEMS} postavkami: najprej odstranitve, nato
     * vnosi (ploskih {@link #ENTRY_INTS} int-ov na vnos). Prazen posnetek da prazen seznam.
     */
    public static List<ByteBuf> snaps(int[] removed, int nRemoved, int[] entries, int nEntries) {
        List<ByteBuf> out = new ArrayList<ByteBuf>();
        int r = 0;
        int e = 0;
        while (r < nRemoved || e < nEntries) {
            int rTake = Math.min(nRemoved - r, MAX_ITEMS);
            int eTake = Math.min(nEntries - e, MAX_ITEMS - rTake);
            ByteBuf buf = Unpooled.buffer(4 + rTake * 3 + eTake * 16);
            buf.writeByte(SNAP);
            writeVarInt(buf, rTake);
            for (int i = 0; i < rTake; ++i) {
                writeVarInt(buf, removed[r + i]);
            }
            writeVarInt(buf, eTake);
            for (int i = 0; i < eTake; ++i) {
                int o = (e + i) * ENTRY_INTS;
                writeEntry(buf, entries[o], entries[o + 1], entries[o + 2], entries[o + 3], entries[o + 4],
                        entries[o + 5], entries[o + 6]);
            }
            r += rTake;
            e += eTake;
            out.add(buf);
        }
        return out;
    }

    /** Razbere sporocilo streznika v {@code sink}. Neznana vrsta se zavrze; vrne vrsto ali -1. */
    public static int readServerMessage(ByteBuf buf, Sink sink) {
        if (!buf.isReadable()) {
            return -1;
        }
        int type = buf.readUnsignedByte();
        if (type == CLEAR) {
            sink.clear();
            return type;
        }
        if (type != SNAP) {
            return -1;
        }
        int m = readVarInt(buf);
        for (int i = 0; i < m; ++i) {
            sink.remove(readVarInt(buf));
        }
        int n = readVarInt(buf);
        for (int i = 0; i < n; ++i) {
            int id = readVarInt(buf);
            int qx = buf.readUnsignedMedium();
            int qy = buf.readUnsignedMedium();
            int qz = buf.readUnsignedMedium();
            int yaw = buf.readUnsignedByte();
            int look = readVarInt(buf);
            int flags = buf.readUnsignedByte();
            sink.entry(id, qx, qy, qz, yaw, look, flags);
        }
        return type;
    }
}
