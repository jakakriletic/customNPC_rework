package si.ladja.xaerofleet.client;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class CaptainIconTest {

    @Test
    public void theHatCarriesTheFactionColour() {
        int[] ally = CaptainIcon.pixels(CaptainIcon.hatColour("ALLY"));
        int[] enemy = CaptainIcon.pixels(CaptainIcon.hatColour("ENEMY"));
        assertEquals(CaptainIcon.SIZE * CaptainIcon.SIZE, ally.length);
        boolean hatDiffers = false;
        boolean faceSame = true;
        for (int y = 0; y < CaptainIcon.SIZE; y++) {
            for (int x = 0; x < CaptainIcon.SIZE; x++) {
                int i = y * CaptainIcon.SIZE + x;
                if (y < 7 && ally[i] != enemy[i]) hatDiffers = true;
                if (y >= 7 && ally[i] != enemy[i]) faceSame = false;
            }
        }
        assertTrue("klobuk mora biti v barvi frakcije", hatDiffers);
        assertTrue("obraz je isti kapitan", faceSame);
        assertTrue(contains(ally, CaptainIcon.ALLY_HAT));
        assertTrue(contains(enemy, CaptainIcon.ENEMY_HAT));
    }

    @Test
    public void factionsHaveDistinctHats() {
        assertNotEquals(CaptainIcon.hatColour("ALLY"), CaptainIcon.hatColour("ENEMY"));
        assertNotEquals(CaptainIcon.hatColour("ENEMY"), CaptainIcon.hatColour("PIRATE"));
        assertEquals(CaptainIcon.UNKNOWN_HAT, CaptainIcon.hatColour("???"));
    }

    @Test
    public void theEdgeOfTheCanvasStaysClearSoTheOutlineFits() {
        int[] icon = CaptainIcon.pixels(CaptainIcon.ALLY_HAT);
        for (int i = 0; i < CaptainIcon.SIZE; i++) {
            assertEquals(0, icon[i] >>> 24);
            assertEquals(0, icon[(CaptainIcon.SIZE - 1) * CaptainIcon.SIZE + i] >>> 24);
        }
    }

    private static boolean contains(int[] pixels, int colour) {
        for (int p : pixels) if (p == colour) return true;
        return false;
    }
}
