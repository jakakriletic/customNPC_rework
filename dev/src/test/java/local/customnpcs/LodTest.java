package local.customnpcs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import noppes.npcs.rework.render.Lod;
import org.junit.Test;

/**
 * M5-K K5: prag stopnje L1 ({@code RwLodStart}) na kvadratu razdalje, kot ga uporabi ime v originalu
 * ({@code getDistanceSq > 512}): strogo vecje od praga, 0 pomeni original na vsaki razdalji.
 */
public class LodTest {
    @Test
    public void nicJeOriginalNaVsakiRazdalji() {
        assertFalse(Lod.far(0.0, 0));
        assertFalse(Lod.far(1.0e9, 0));
        assertFalse("negativen prag ni veljaven vklop", Lod.far(1.0e9, -5));
    }

    @Test
    public void pragJeStrogoVecjiOdKvadrata() {
        assertFalse("tocno na pragu ostane polna podrobnost", Lod.far(32.0 * 32.0, 32));
        assertTrue(Lod.far(32.0 * 32.0 + 1.0e-6, 32));
        assertFalse(Lod.far(31.9 * 31.9, 32));
        assertTrue(Lod.far(43.0 * 43.0, 32));
    }

    @Test
    public void velikPragBrezPrekoracitve() {
        assertTrue(Lod.far(600.0 * 600.0, 512));
        assertFalse(Lod.far(511.0 * 511.0, 512));
    }
}
