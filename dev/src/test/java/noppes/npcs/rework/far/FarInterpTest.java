package noppes.npcs.rework.far;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** M5-V.5: interpolacija nadomestka, vnos shrambe, billboard in projekcija (D-047). */
public class FarInterpTest {
    @Test
    public void delezPremikaJeOmejenInPrenesePreliv() {
        assertEquals(0.0f, FarInterp.fraction(1000, 1000, 1000), 0.0f);
        assertEquals(0.0f, FarInterp.fraction(1000, 900, 1000), 0.0f);
        assertEquals(0.25f, FarInterp.fraction(1000, 1250, 1000), 1e-6f);
        assertEquals(1.0f, FarInterp.fraction(1000, 2000, 1000), 0.0f);
        assertEquals(1.0f, FarInterp.fraction(1000, 99999, 1000), 0.0f);
        // Cas v ms kot int se prelije; razlika ostane pravilna.
        assertEquals(0.5f, FarInterp.fraction(Integer.MAX_VALUE - 100, Integer.MIN_VALUE + 399, 1000), 1e-6f);
        assertEquals(1.0f, FarInterp.fraction(5, 6, 0), 0.0f);
    }

    @Test
    public void novNpcNimaPremikaInSvetlobaSeNeVe() {
        int[] e = FarInterp.entryOf(null, 800, 32, -400, 10, 3, 0, 5000);
        assertArrayEquals(new int[] {800, 32, -400, 10, 3, 0, 800, 32, -400, 5000, -1}, e);
        assertEquals(100.0, FarInterp.lerpBlocks(e[FarInterp.E_PX], e[0], 0.3f), 1e-9);
    }

    @Test
    public void premikSeZacnePriPrikazanemPolozaju() {
        int[] a = FarInterp.entryOf(null, 0, 32, 0, 0, 1, 0, 0);
        int[] b = FarInterp.entryOf(a, 80, 32, 0, 0, 1, 0, 1000);
        // Prvi premik: od 0 do 80 (10 blokov) v 1 s.
        assertEquals(0, b[FarInterp.E_PX]);
        assertEquals(5.0, FarInterp.lerpBlocks(b[FarInterp.E_PX], b[0], FarInterp.fraction(b[FarInterp.E_T], 1500, FarInterp.DURATION_MS)), 1e-9);
        // Nov posnetek sredi premika: zacetek je prikazani polozaj (40 = 5 blokov), ne stari cilj.
        int[] c = FarInterp.entryOf(b, 160, 32, 0, 0, 1, 0, 1500);
        assertEquals(40, c[FarInterp.E_PX]);
        assertEquals(1500, c[FarInterp.E_T]);
    }

    @Test
    public void skokNadSestnajstBlokovSeNeInterpolira() {
        int[] a = FarInterp.entryOf(null, 0, 32, 0, 0, 1, 0, 0);
        int[] b = FarInterp.entryOf(a, 16 * 8 + 1, 32, 0, 0, 1, 0, 2000);
        assertEquals(b[0], b[FarInterp.E_PX]);
        int[] c = FarInterp.entryOf(a, 16 * 8, 32, 0, 0, 1, 0, 2000);
        assertEquals(0, c[FarInterp.E_PX]);
    }

    @Test
    public void svetlobaOstaneVIstemBloku() {
        int[] a = FarInterp.entryOf(null, 8, 32, 8, 0, 1, 0, 0);
        a[FarInterp.E_LIGHT] = 0xF000F0;
        assertEquals(0xF000F0, FarInterp.entryOf(a, 15, 39, 15, 0, 1, 0, 100)[FarInterp.E_LIGHT]);
        assertEquals(-1, FarInterp.entryOf(a, 16, 32, 8, 0, 1, 0, 100)[FarInterp.E_LIGHT]);
        assertEquals(-1, FarInterp.entryOf(a, 8, 40, 8, 0, 1, 0, 100)[FarInterp.E_LIGHT]);
    }

    @Test
    public void yawIzBajta() {
        assertEquals(0.0f, FarInterp.yawOf(0), 0.0f);
        assertEquals(90.0f, FarInterp.yawOf(64), 1e-6f);
        assertEquals(180.0f, FarInterp.yawOf(128), 1e-6f);
        assertEquals(FarInterp.yawOf(FarCodec.yawByte(270.0f)), 270.0f, 1.5f);
    }

    @Test
    public void desnoKotPriPekiInImpostorCheck() {
        // Kamera gleda proti -Z (smer do NPC-ja (0, -1)): desno je +X, kot v ImpostorCheck.
        float[] r = FarInterp.right(0.0, -10.0);
        assertEquals(1.0f, r[0], 1e-6f);
        assertEquals(0.0f, r[1], 1e-6f);
        // Pogled proti +Z: desno je -X (yaw 0 v Minecraftu, vzhod je levo).
        r = FarInterp.right(0.0, 5.0);
        assertEquals(-1.0f, r[0], 1e-6f);
        r = FarInterp.right(3.0, 4.0);
        assertEquals(1.0, Math.hypot(r[0], r[1]), 1e-6);
        assertEquals(0.0, r[0] * 3.0 + r[1] * 4.0, 1e-6);
        assertArrayEquals(new float[] {-1.0f, 0.0f}, FarInterp.right(0.0, 0.0), 0.0f);
    }

    @Test
    public void projekcijaSrediscaInTockeZaKamero() {
        float[] id = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
        // Perspektiva 90 stopinj, razmerje 1, near 1, far 100 (stolpcno kot OpenGL).
        float f = 1.0f;
        float n = 1.0f;
        float fa = 100.0f;
        float[] pr = {f, 0, 0, 0, 0, f, 0, 0, 0, 0, (fa + n) / (n - fa), -1, 0, 0, 2 * fa * n / (n - fa), 0};
        float[] c = FarInterp.project(id, pr, 0.0, 0.0, -10.0, 200, 100);
        assertEquals(100.0f, c[0], 1e-3f);
        assertEquals(50.0f, c[1], 1e-3f);
        float[] d = FarInterp.project(id, pr, 10.0, 10.0, -10.0, 200, 100);
        assertEquals(200.0f, d[0], 1e-3f);
        assertEquals(100.0f, d[1], 1e-3f);
        assertNull(FarInterp.project(id, pr, 0.0, 0.0, 10.0, 200, 100));
    }

    @Test
    public void okvirIzSmeriInYawa() {
        // NPC gleda proti kameri (kamera -> NPC je yaw 180, NPC yaw 0 gleda proti +Z, torej v kamero): okvir 0.
        assertEquals(0, ImpostorFrames.frameFor(ImpostorFrames.yawOf(0.0, -10.0), FarInterp.yawOf(0)));
        // NPC obrnjen za 45 stopinj: okvir 1.
        assertEquals(1, ImpostorFrames.frameFor(ImpostorFrames.yawOf(0.0, -10.0), FarInterp.yawOf(32)));
    }
}
