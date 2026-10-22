package noppes.npcs.rework.far;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

/** M5-V.7 (D-050): nacin izrisa L2/L3 glede na OptiFine senčila ({@link FarCompat}). */
public class FarCompatTest {
    @Test
    public void brezPaketaSencilVednoIzris() {
        assertEquals(FarCompat.DRAW, FarCompat.mode(false, false, 0));
        assertEquals(FarCompat.DRAW, FarCompat.mode(false, false, 1));
    }

    @Test
    public void paketSencilPrivzetoIzklopi() {
        // RwFarShaders=0: s paketom senčil klient rise original, tudi v senčnem prehodu.
        assertEquals(FarCompat.OFF, FarCompat.mode(true, false, 0));
        assertEquals(FarCompat.OFF, FarCompat.mode(true, true, 0));
    }

    @Test
    public void senciniPrehodNikoliImpostorjev() {
        assertEquals(FarCompat.DRAW, FarCompat.mode(true, false, 1));
        assertEquals(FarCompat.SKIP, FarCompat.mode(true, true, 1));
        // Senčni prehod brez paketa (ne bi smel obstajati) tudi ne rise impostorjev.
        assertEquals(FarCompat.SKIP, FarCompat.mode(false, true, 1));
    }

    @Test
    public void brezOptiFineIzrisNespremenjen() {
        // V testnem okolju razreda net.optifine.shaders.Shaders ni: vedno DRAW, ne glede na stikalo.
        assertFalse(FarCompat.optifine());
        assertFalse(FarCompat.shaderPack());
        assertEquals(FarCompat.DRAW, FarCompat.mode(0));
        assertEquals(FarCompat.DRAW, FarCompat.mode(1));
    }
}
