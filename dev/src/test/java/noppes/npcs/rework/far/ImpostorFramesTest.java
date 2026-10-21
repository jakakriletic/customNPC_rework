package noppes.npcs.rework.far;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** M5-V.4: postavitev atlasa, izbira okvirja, obrez, pomanjsava in mipmapi z ohranjeno pokritostjo alfe. */
public class ImpostorFramesTest {
    @Test
    public void traki_soPoravnaniZaVseStopnje() {
        assertEquals(168, ImpostorFrames.slots(2048));
        assertEquals(42, ImpostorFrames.slots(1024));
        assertEquals(672, ImpostorFrames.slots(4096));
        int align = 1 << (ImpostorFrames.LEVELS - 1);
        assertEquals(0, ImpostorFrames.CELL_W % align);
        assertEquals(0, ImpostorFrames.CELL_H % align);
        for (int slot = 0; slot < ImpostorFrames.slots(2048); ++slot) {
            int x = ImpostorFrames.slotX(slot, 2048);
            int y = ImpostorFrames.slotY(slot, 2048);
            assertEquals(0, x % align);
            assertEquals(0, y % align);
            assertTrue(x + ImpostorFrames.STRIP_W <= 2048);
            assertTrue(y + ImpostorFrames.CELL_H <= 2048);
        }
        // Zadnji trak se ne prekriva s predzadnjim.
        assertEquals(5 * ImpostorFrames.STRIP_W, ImpostorFrames.slotX(5, 2048));
        assertEquals(ImpostorFrames.CELL_H, ImpostorFrames.slotY(6, 2048));
    }

    @Test
    public void okvir_spredajJe0() {
        // NPC gleda proti kameri: yaw NPC-ja = yaw smeri kamera->NPC + 180.
        assertEquals(0, ImpostorFrames.frameFor(30.0f, 210.0f));
        assertEquals(0, ImpostorFrames.frameFor(-170.0f, 10.0f));
        // Hrbet.
        assertEquals(4, ImpostorFrames.frameFor(30.0f, 30.0f));
        // Okvir k je yaw k*45 pri kameri, ki gleda proti -Z (yaw smeri 180).
        for (int k = 0; k < 8; ++k) {
            assertEquals(k, ImpostorFrames.frameFor(180.0f, k * 45.0f));
            assertEquals(k, ImpostorFrames.frameFor(180.0f, k * 45.0f + 22.0f));
            assertEquals(k, ImpostorFrames.frameFor(180.0f, k * 45.0f - 22.0f - 360.0f));
        }
        assertEquals(180.0f, Math.abs(ImpostorFrames.yawOf(0.0, -1.0)), 1e-4f);
        assertEquals(0.0f, ImpostorFrames.yawOf(0.0, 1.0), 1e-4f);
        assertEquals(90.0f, ImpostorFrames.yawOf(-1.0, 0.0), 1e-4f);
    }

    @Test
    public void obrez_simetricenOkoliOsi() {
        int w = 16, h = 8;
        int[] img = new int[w * h];
        img[2 * w + 9] = 0xFF112233;
        img[5 * w + 12] = 0x80445566;
        assertArrayEquals(new int[] {9, 2, 12, 5}, ImpostorFrames.alphaBounds(img, w, h));
        assertNull(ImpostorFrames.alphaBounds(new int[w * h], w, h));
        assertArrayEquals(new int[] {1, 2, 12, 5}, ImpostorFrames.union(new int[] {1, 3, 4, 4}, new int[] {9, 2, 12, 5}));
        // Os med stolpcema 7 in 8: desno do 12 -> polovica 5, izrez 3..12.
        assertArrayEquals(new int[] {3, 2, 10, 4}, ImpostorFrames.symmetricCrop(new int[] {9, 2, 12, 5}, 8));
        assertFalse(ImpostorFrames.touchesEdge(new int[] {1, 1, 14, 6}, w, h));
        assertTrue(ImpostorFrames.touchesEdge(new int[] {0, 1, 14, 6}, w, h));
        assertTrue(ImpostorFrames.touchesEdge(new int[] {1, 1, 14, 7}, w, h));
    }

    @Test
    public void velikostOkvirja_sorazmernoInNikoliVecja() {
        assertArrayEquals(new int[] {32, 64}, ImpostorFrames.frameSize(64, 128));
        assertArrayEquals(new int[] {20, 64}, ImpostorFrames.frameSize(50, 160));
        assertArrayEquals(new int[] {32, 16}, ImpostorFrames.frameSize(128, 64));
        assertArrayEquals(new int[] {10, 12}, ImpostorFrames.frameSize(10, 12));
    }

    @Test
    public void pomanjsava_premnozenaAlfa() {
        // 2 x 1 -> 1 x 1: neprozoren rdec in prozoren zelen teksel; zelena ne sme obarvati.
        int[] src = {0xFFFF0000, 0x0000FF00};
        int[] out = ImpostorFrames.resample(src, 2, 1, 0, 0, 2, 1, 1, 1);
        int a = out[0] >>> 24;
        assertTrue(a == 127 || a == 128);
        assertEquals(0xFF0000, out[0] & 0xFFFFFF);
        // Enaka velikost: brez spremembe.
        int[] same = ImpostorFrames.resample(new int[] {0xFF010203, 0x7F040506}, 2, 1, 0, 0, 2, 1, 2, 1);
        assertArrayEquals(new int[] {0xFF010203, 0x7F040506}, same);
        // Izven vira je prozorno.
        int[] edge = ImpostorFrames.resample(new int[] {0xFFFFFFFF}, 1, 1, -1, 0, 2, 1, 1, 1);
        assertTrue((edge[0] >>> 24) >= 127 && (edge[0] >>> 24) <= 128);
    }

    @Test
    public void celica_okvirNaDnuInNaSredini() {
        int[] frame = new int[10 * 20];
        java.util.Arrays.fill(frame, 0xFFABCDEF);
        int[] cell = ImpostorFrames.cell(frame, 10, 20);
        assertEquals(ImpostorFrames.CELL_W * ImpostorFrames.CELL_H, cell.length);
        int ox = ImpostorFrames.frameX(10);
        int oy = ImpostorFrames.frameY(20);
        assertEquals(ImpostorFrames.PAD + 11, ox);
        assertEquals(ImpostorFrames.PAD + 44, oy);
        assertEquals(0xFFABCDEF, cell[oy * ImpostorFrames.CELL_W + ox]);
        assertEquals(0xFFABCDEF, cell[(oy + 19) * ImpostorFrames.CELL_W + ox + 9]);
        assertEquals(0, cell[(oy - 1) * ImpostorFrames.CELL_W + ox]);
        // Rob celice ostane prozoren.
        int[] b = ImpostorFrames.alphaBounds(cell, ImpostorFrames.CELL_W, ImpostorFrames.CELL_H);
        assertEquals(ImpostorFrames.CELL_H - 1 - ImpostorFrames.PAD, b[3]);
    }

    @Test
    public void mipmapi_ohranijoPokritostAlfe() {
        // Tanka silhueta z alfo 40: navadno povprecje bi jo v manjsih stopnjah spustilo pod alfa test.
        int[] cell = new int[ImpostorFrames.CELL_W * ImpostorFrames.CELL_H];
        for (int y = 8; y < 64; ++y) {
            for (int x = 18; x < 22; ++x) {
                cell[y * ImpostorFrames.CELL_W + x] = (y % 3 == 0 ? 0xFF : 0x28) << 24 | 0x336699;
            }
        }
        int[][] levels = ImpostorFrames.mipChain(cell);
        assertEquals(ImpostorFrames.LEVELS, levels.length);
        double c0 = ImpostorFrames.coverage(levels[0], 1.0) / (double) levels[0].length;
        int w = ImpostorFrames.CELL_W;
        int h = ImpostorFrames.CELL_H;
        for (int l = 1; l < ImpostorFrames.LEVELS; ++l) {
            w /= 2;
            h /= 2;
            assertEquals(w * h, levels[l].length);
            double c = ImpostorFrames.coverage(levels[l], 1.0) / (double) levels[l].length;
            assertTrue("stopnja " + l + ": pokritost " + c + " proti " + c0, Math.abs(c - c0) <= 0.5 * c0 + 1.0 / levels[l].length);
            assertTrue(c > 0);
        }
    }

    @Test
    public void faktorAlfe_vrnePokritostKiJoAlfaTestRes_obdrzi() {
        // 5 tekslov z alfo 20 in 5 z 10: pri faktorju 1 nic ne prestane testa (26), cilj je polovica.
        int[] img = new int[10];
        for (int i = 0; i < 10; ++i) {
            img[i] = (i < 5 ? 20 : 10) << 24 | 0x00AA00;
        }
        assertEquals(0, ImpostorFrames.coverage(img, 1.0));
        double s = ImpostorFrames.coverageScale(img, 0.5);
        assertEquals(5, ImpostorFrames.coverage(img, s));
        ImpostorFrames.scaleAlpha(img, s);
        assertEquals("po zaokrozitvi ista pokritost", 5, ImpostorFrames.coverage(img, 1.0));
        assertEquals(0x00AA00, img[0] & 0xFFFFFF);
        // Ze pravilna pokritost ostane pri faktorju 1.
        int[] ok = {0xFF000000, 0};
        assertEquals(1.0, ImpostorFrames.coverageScale(ok, 0.5), 0.0);
    }

    @Test
    public void bleed_barvaProzornihIzSosedov() {
        int[] img = {0, 0, 0, 0xFF102030, 0, 0};
        ImpostorFrames.bleed(img, 3, 2, 1);
        assertEquals(0xFF102030, img[3]);
        assertEquals(0x102030, img[0]);
        assertEquals(0x102030, img[4]);
        assertEquals(0, img[2] & 0xFFFFFF);
        assertEquals(0, img[0] >>> 24);
    }
}
