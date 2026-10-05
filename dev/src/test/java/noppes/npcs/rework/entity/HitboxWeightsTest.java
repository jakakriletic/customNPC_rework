package noppes.npcs.rework.entity;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HitboxWeightsTest {
    private static final double EPS = 1e-9;
    private static final int O = HitboxWeights.ORIGINAL;
    private static final int S = HitboxWeights.SOLID;
    private static final int M = HitboxWeights.SMART;

    private static double m(double w, double h) {
        return HitboxWeights.mass(w, h, 1.0);
    }

    @Test
    public void originalJeVanilla() {
        assertArrayEquals(new double[] {1, 1}, HitboxWeights.shares(O, m(0.6, 1.8), O, m(6, 18)), EPS);
    }

    @Test
    public void solidSeNePremakneDrugegaOdrineKotVanilla() {
        assertArrayEquals(new double[] {0, 1}, HitboxWeights.shares(S, m(0.6, 1.8), O, m(0.6, 1.8)), EPS);
        assertArrayEquals(new double[] {1, 0}, HitboxWeights.shares(M, m(6, 18), S, m(0.6, 1.8)), EPS);
    }

    @Test
    public void dvaSolidSeRazdelitaPoMasi() {
        assertArrayEquals(new double[] {1, 1}, HitboxWeights.shares(S, m(0.6, 1.8), S, m(0.6, 1.8)), EPS);
    }

    @Test
    public void smartEnakaVelikostJeVanilla() {
        assertArrayEquals(new double[] {1, 1}, HitboxWeights.shares(M, m(0.6, 1.8), M, m(0.6, 1.8)), EPS);
        assertArrayEquals(new double[] {1, 1}, HitboxWeights.shares(M, m(0.6, 1.8), O, m(0.6, 1.8)), EPS);
    }

    @Test
    public void smartMaliNePremakneVelikana() {
        // NPC velikosti 1 proti velikosti 10 (sirina in visina x10 -> masa x1000)
        double[] s = HitboxWeights.shares(M, m(0.6, 1.8), M, m(6, 18));
        assertEquals(2.0 * 1000 / 1001, s[0], EPS);
        assertEquals(2.0 / 1001, s[1], EPS);
        assertTrue("velikan se premakne manj kot 0,2 % vanilla", s[1] < 0.002);
    }

    @Test
    public void scitPodvojiMaso() {
        double brez = HitboxWeights.mass(0.6, 1.8, 1.0);
        double s = HitboxWeights.mass(0.6, 1.8, 2.0);
        assertEquals(2 * brez, s, EPS);
        double[] d = HitboxWeights.shares(M, s, M, brez);
        assertEquals(2.0 / 3.0, d[0], EPS);
        assertEquals(4.0 / 3.0, d[1], EPS);
    }

    @Test
    public void brezTlorisaNeDeliZNic() {
        double[] s = HitboxWeights.shares(M, HitboxWeights.mass(1.0E-5, 1.8, 1.0), M, 0.0);
        assertEquals(2.0, s[0] + s[1], EPS);
    }

    @Test
    public void imenaInNeznaniNacini() {
        assertEquals(S, HitboxWeights.parse("solid"));
        assertEquals(M, HitboxWeights.parse("2"));
        assertEquals(-1, HitboxWeights.parse("trdo"));
        assertEquals(O, HitboxWeights.sanitize(7));
        assertEquals("smart", HitboxWeights.name(M));
    }
}
