package local.customnpcs;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.Random;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import noppes.npcs.client.model.Model2DRenderer;
import noppes.npcs.rework.render.HeadwearMask;
import noppes.npcs.rework.render.HeadwearQuads;
import org.junit.Test;

/**
 * M5-K K14b: izpuscanje stirikotnikov pokrivala ne spremeni ostalih oglisc, obseg uv ustreza
 * originalu, maska izpusti samo stirikotnike, ki vzorcijo izkljucno teksle z alfo 0.
 */
public class HeadwearSkipTest {
    private static final int STRIDE = DefaultVertexFormats.POSITION_TEX_NORMAL.getNextOffset();
    // Del "spredaj" ModelHeadwear: uv 40,8 velikosti 8 x 8 v teksturi 64 x 64; debelina = merilo 1/16.
    private static final float U1 = 40f / 64f, V1 = 8f / 64f, U2 = 48f / 64f, V2 = 16f / 64f, T = 0.0625f;
    private static final int W = 8, H = 8;

    private static byte[] original() {
        BufferBuilder b = new BufferBuilder(4096);
        try {
            Model2DRenderer.renderItemIn2D(b, U1, V1, U2, V2, W, H, T);
        } catch (IllegalStateException e) {
            // Tessellator.draw() na koncu: skupni medpomnilnik ni v risanju ("Not building!"), GL se
            // ne doseze. Oglisca so ze v b.
        }
        return bytes(b);
    }

    private static byte[] emit(boolean[] keep) {
        BufferBuilder b = new BufferBuilder(4096);
        HeadwearQuads.emit(b, U1, V1, U2, V2, W, H, T, keep);
        return bytes(b);
    }

    private static byte[] bytes(BufferBuilder b) {
        ByteBuffer bb = b.getByteBuffer();
        byte[] r = new byte[b.getVertexCount() * STRIDE];
        for (int i = 0; i < r.length; i++) {
            r[i] = bb.get(i);
        }
        return r;
    }

    private static boolean[] all(boolean v) {
        boolean[] k = new boolean[HeadwearQuads.count(W, H)];
        java.util.Arrays.fill(k, v);
        return k;
    }

    @Test
    public void vsiStirikotnikiSoBajtnoEnakiOriginalu() {
        byte[] o = original();
        assertEquals(HeadwearQuads.count(W, H) * 4 * STRIDE, o.length);
        assertArrayEquals(o, emit(all(true)));
        assertEquals(0, emit(all(false)).length);
    }

    @Test
    public void izpusceniStirikotnikiNeSpremenijoOstalih() {
        byte[] o = original();
        Random r = new Random(42);
        for (int n = 0; n < 50; n++) {
            boolean[] keep = new boolean[HeadwearQuads.count(W, H)];
            java.io.ByteArrayOutputStream exp = new java.io.ByteArrayOutputStream();
            for (int q = 0; q < keep.length; q++) {
                keep[q] = r.nextBoolean();
                if (keep[q]) {
                    exp.write(o, q * 4 * STRIDE, 4 * STRIDE);
                }
            }
            assertArrayEquals(exp.toByteArray(), emit(keep));
        }
    }

    @Test
    public void obsegUvUstrezaOgliscemOriginala() {
        ByteBuffer o = ByteBuffer.wrap(original()).order(java.nio.ByteOrder.nativeOrder());
        float[][] b = HeadwearQuads.uvBounds(U1, V1, U2, V2, W, H);
        for (int q = 0; q < b.length; q++) {
            float umin = Float.MAX_VALUE, umax = -Float.MAX_VALUE, vmin = Float.MAX_VALUE, vmax = -Float.MAX_VALUE;
            for (int v = 0; v < 4; v++) {
                int at = (4 * q + v) * STRIDE;
                float u = o.getFloat(at + 12), w = o.getFloat(at + 16);
                umin = Math.min(umin, u);
                umax = Math.max(umax, u);
                vmin = Math.min(vmin, w);
                vmax = Math.max(vmax, w);
            }
            assertArrayEquals("stirikotnik " + q, new float[] {umin, umax, vmin, vmax}, b[q], 0f);
        }
    }

    private static int[] tex(int... opaque) {
        int[] t = new int[64 * 64];
        for (int i = 0; i < opaque.length; i += 2) {
            t[opaque[i + 1] * 64 + opaque[i]] = 0xFF112233;
        }
        return t;
    }

    private static boolean[] keep(int[] t) {
        return HeadwearMask.keep(t, 64, 64, HeadwearQuads.uvBounds(U1, V1, U2, V2, W, H));
    }

    @Test
    public void prozornoObmocjeNeNariseNicesar() {
        assertEquals(0, HeadwearMask.kind(keep(tex())));
        // Delno prozoren teksel (alfa 1) ni prozoren: original ga lahko narise ob drugacnem alfa testu.
        int[] t = tex();
        t[12 * 64 + 44] = 0x01FFFFFF;
        assertEquals(1, HeadwearMask.kind(keep(t)));
    }

    @Test
    public void enTekselOhraniObrazaInSvojaTrakova() {
        // teksel stolpec 3, vrstica 5 obraza (x 43, y 13)
        boolean[] k = keep(tex(43, 13));
        assertTrue("spredaj", k[0]);
        assertTrue("zadaj", k[1]);
        int vidnih = 0;
        for (boolean b : k) {
            vidnih += b ? 1 : 0;
        }
        // dva trakova stolpca 3 (normala -x in +x) in dva trakova vrstice 5 (+y in -y)
        assertEquals(6, vidnih);
        assertTrue(k[2 + 3]);
        assertTrue(k[2 + W + 3]);
        // trakovi v: f8 = v2 + (v1 - v2) * k / h - pol teksla -> vrstica y + h - 1 - k
        assertTrue(k[2 + 2 * W + (8 - 1 - 5)]);
        assertTrue(k[2 + 2 * W + H + (8 - 1 - 5)]);
    }

    @Test
    public void tekselTikZaRobomOhraniSamoStirikotnikeDoRoba() {
        // stolpec x = 48 je ze zunaj obraza (40..47); rob u2 = 48/64 ga lahko vzorci: obraza in
        // oba trakova vrstice 12 (segata po u do roba), trakovi stolpcev pa ne (sredina teksla).
        boolean[] k = keep(tex(48, 12));
        assertTrue(k[0]);
        assertTrue(k[1]);
        int vidnih = 0;
        for (int q = 2; q < 2 + 2 * W; q++) {
            assertFalse("trak stolpca " + q, k[q]);
        }
        for (boolean b : k) {
            vidnih += b ? 1 : 0;
        }
        assertEquals(4, vidnih);
        assertEquals("dva teksla od roba ne vzorci nihce", 0, HeadwearMask.kind(keep(tex(50, 12))));
    }

    @Test
    public void brezRobaIzpustiPrazenObrazObNeprozornemSosedu() {
        // RwHeadwearSkip 1: neprozorni teksli tik ob obrazu (stolpec 48, vrstica 16, ovijanje) ne stejejo
        float[][] b = HeadwearQuads.uvBounds(U1, V1, U2, V2, W, H);
        assertEquals(0, HeadwearMask.kind(HeadwearMask.keep(tex(48, 12, 44, 16, 39, 10, 44, 7), 64, 64, b, true)));
        boolean[] k = HeadwearMask.keep(tex(43, 13), 64, 64, b, true);
        int vidnih = 0;
        for (boolean x : k) {
            vidnih += x ? 1 : 0;
        }
        assertEquals("obraza in stirje trakovi teksla 43,13 kot z robom", 6, vidnih);
        // HD: sredina stolpca modela na meji tekslov slike vzame teksel desno (floor)
        int[] t = new int[128 * 128];
        t[26 * 128 + 87] = 0xFF000000;
        assertTrue(HeadwearMask.keep(t, 128, 128, b, true)[2 + 3]);
    }

    @Test
    public void hdSkinInOvijanje() {
        // 128 x 128: teksel modela je 2 x 2; sredina stolpca modela je na meji tekslov slike
        int[] t = new int[128 * 128];
        t[26 * 128 + 87] = 0xFF000000; // stolpec modela 43 (slika 86..87), vrstica 13 (26..27)
        boolean[] k = HeadwearMask.keep(t, 128, 128, HeadwearQuads.uvBounds(U1, V1, U2, V2, W, H));
        assertTrue(k[0]);
        assertTrue(k[2 + 3]);
        // ovijanje: obraz pri u = 0 vzorci tudi zadnji stolpec slike
        int[] w = tex(63, 0);
        assertTrue(HeadwearMask.keep(w, 64, 64, HeadwearQuads.uvBounds(0f, 0f, 8f / 64f, 8f / 64f, 8, 8))[0]);
    }
}
