package noppes.npcs.rework.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** M5-K K12: okvir in domet upodobitve ({@link RenderBox}). */
public class RenderBoxTest {
    private static final double E = 1e-6;

    @Test
    public void hitboxOstaneSirinaDometa() {
        assertEquals(0.6f, RenderBox.rangeWidth(0.6f, 5, false, -1.0f), E);
        assertEquals(1.8f, RenderBox.rangeWidth(1.8f, 15, false, -1.0f), E);
        assertEquals(0.9f, RenderBox.rangeWidth(0.9f, 5, false, 0.9f), E);
    }

    @Test
    public void brezHitboxaSirinaKotSHitboxom() {
        // updateHitbox: 0,6 (stoji) x Size / 5; morf max(0,1, sirina x Size / 5).
        assertEquals(0.6, RenderBox.rangeWidth(1.0E-5f, 5, false, -1.0f), E);
        assertEquals(1.8, RenderBox.rangeWidth(1.0E-5f, 15, false, -1.0f), 1e-5);
        assertEquals(0.9, RenderBox.rangeWidth(1.0E-5f, 5, false, 0.9f), E);
        assertEquals(0.1, RenderBox.rangeWidth(1.0E-5f, 1, false, 0.2f), E);
    }

    @Test
    public void lezecDometKotStojec() {
        // Lezec ali ubit NPC: hitbox 0,8 x 0,4 (domet 0,67), model dolg kot stojec; domet stojecega 0,6 x baseHeight.
        assertEquals(0.6, RenderBox.rangeWidth(0.8f, 5, true, -1.0f), E);
        assertEquals(0.6, RenderBox.rangeWidth(1.0E-5f, 5, true, -1.0f), E);
        assertEquals(1.8, RenderBox.rangeWidth(2.4f, 15, true, -1.0f), 1e-5);
        assertEquals(1.9, RenderBox.rangeHeight(0.4f, 5, true, -1.0f, 1.9f), 1e-6);
        assertEquals(5.7, RenderBox.rangeHeight(1.2f, 15, true, -1.0f, 1.9f), 1e-5);
        assertEquals("stoji", 1.9, RenderBox.rangeHeight(1.9f, 5, false, -1.0f, 1.9f), 1e-6);
        assertEquals("morf ne lezi", 0.9, RenderBox.rangeWidth(0.9f, 5, true, 0.9f), 1e-6);
        assertEquals("morf ne lezi", 0.4, RenderBox.rangeHeight(0.4f, 5, true, 0.9f, 1.9f), 1e-6);
        // RD 10 (utez 1,25): kamera K18 je od RWV7_Mrtev ~58,5 bloka; hitbox lezecega ~53,3, stojec ~82.
        assertFalse(RenderBox.inRange(58.5 * 58.5, 0.8, 0.4, 1.25));
        assertTrue(RenderBox.inRange(58.5 * 58.5, RenderBox.rangeWidth(0.8f, 5, true, -1.0f),
                RenderBox.rangeHeight(0.4f, 5, true, -1.0f, 1.9f), 1.25));
    }

    @Test
    public void dometBrezHitboxaJeDometSHitboxom() {
        double w = 1.0E-5;
        double h = 1.9;
        // Original: povprecni rob (2 x 1e-5 + 1,9) / 3 x 64 = 40,5 blokov pri RD 8 (utez 1).
        assertTrue(RenderBox.inRange(40.0 * 40.0, w, h, 1.0));
        assertFalse(RenderBox.inRange(41.0 * 41.0, w, h, 1.0));
        double rw = RenderBox.rangeWidth(1.0E-5f, 5, false, -1.0f);
        // S hitboxom 0,6: (0,6 + 1,9 + 0,6) / 3 x 64 = 66,1.
        assertTrue(RenderBox.inRange(66.0 * 66.0, rw, h, 1.0));
        assertFalse(RenderBox.inRange(66.2 * 66.2, rw, h, 1.0));
        assertTrue("utez RD 10 = 1,25", RenderBox.inRange(82.0 * 82.0, rw, h, 1.25));
    }

    @Test
    public void okvirObjameRokeHumanoida() {
        // Telo + roki = 1,0 bloka pri Size 5, hitbox 0,6.
        assertEquals(0.5, RenderBox.halfWidth(0.6f, 5, false, -1.0f, -1.0f, 1.9f, 1.0f, false), E);
        assertEquals(1.5, RenderBox.halfWidth(1.8f, 15, false, -1.0f, -1.0f, 1.9f, 1.0f, false), E);
        assertEquals("brez hitboxa", 0.5, RenderBox.halfWidth(1.0E-5f, 5, false, -1.0f, -1.0f, 1.9f, 1.0f, false), E);
        assertEquals("skala delov 1,5", 0.75, RenderBox.halfWidth(0.6f, 5, false, -1.0f, -1.0f, 1.9f, 1.5f, false), E);
        assertEquals("mec v roki", 1.0, RenderBox.halfWidth(0.6f, 5, false, -1.0f, -1.0f, 1.9f, 1.0f, true), E);
        assertEquals("mec v roki, Size 15", 3.0, RenderBox.halfWidth(1.8f, 15, false, -1.0f, -1.0f, 1.9f, 1.0f, true), E);
        assertEquals("skala pod 1 ne zmanjsa", 0.5, RenderBox.halfWidth(0.6f, 5, false, -1.0f, -1.0f, 1.9f, 0.5f, false), E);
    }

    @Test
    public void okvirNikoliOziOdHitboxa() {
        // Sirok hitbox (npr. iz skripte ali morfa) ostane.
        assertEquals(2.0, RenderBox.halfWidth(4.0f, 5, false, -1.0f, -1.0f, 1.9f, 1.0f, false), E);
        assertEquals(1.5, RenderBox.halfWidth(3.0f, 5, false, 0.5f, 0.5f, 1.9f, 1.0f, false), E);
        assertEquals(2.5, RenderBox.height(2.5f, 5, true, -1.0f, -1.0f), E);
        assertEquals(1.9, RenderBox.height(1.9f, 5, false, -1.0f, -1.0f), E);
    }

    @Test
    public void lezecModelDobiDolzinoTelesa() {
        // Lezec: hitbox 0,8 x 0,4, model dolg ~baseHeight v neznani smeri.
        assertEquals(1.9, RenderBox.halfWidth(0.8f, 5, true, -1.0f, -1.0f, 1.9f, 1.0f, false), 1e-5);
        assertEquals(0.6, RenderBox.height(0.4f, 5, true, -1.0f, -1.0f), E);
        assertEquals(1.8, RenderBox.height(0.4f, 15, true, -1.0f, -1.0f), 1e-5);
        assertEquals("morf ne lezi", 0.4, RenderBox.height(0.4f, 5, true, 0.2f, 0.2f), 1e-6);
    }

    @Test
    public void morfDobiVecjoOdSirineInVisineEntitete() {
        // Prasic 0,9 x 0,9: model z gobcem ~0,9 od sredine; okvir max(sirina, visina) na vsako stran.
        assertEquals(0.9, RenderBox.halfWidth(1.0E-5f, 5, false, 0.9f, 0.9f, 1.9f, 1.0f, false), 1e-6);
        assertEquals(2.7, RenderBox.halfWidth(2.7f, 15, false, 0.9f, 0.9f, 1.9f, 1.0f, false), 1e-5);
        // Glava prasica sega do 1,0 bloka: okvir 1,5 x 0,9.
        assertEquals(1.35, RenderBox.height(0.9f, 5, false, 0.9f, 0.9f), 1e-6);
        // Konj 1,3965 x 1,6: visina prevlada.
        assertEquals(1.6, RenderBox.halfWidth(1.3965f, 5, false, 1.3965f, 1.6f, 1.9f, 1.0f, false), 1e-6);
    }
}
