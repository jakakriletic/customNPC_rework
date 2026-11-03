package si.ladja.xaerofleet.client.xaero;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The projection, and what happens to it when Xaero changes underneath.
 *
 * <p>Xaero's own fields cannot be tested here - they are whatever the jar on
 * the classpath says - but everything built on top of them can, and it is the
 * part that goes wrong silently: a marker in the wrong place still looks like
 * a marker.</p>
 */
public class MapViewTransformTest {

    /** Camera at (100, -40), two pixels to the block, no GUI scaling. */
    private static MapViewTransform overworld() {
        return MapViewTransform.of(100, -40, 2.0D, 1.0D, 0, 200, 120, 1);
    }

    @Test
    public void aMarkerAtTheCameraSitsAtTheCentreOfTheScreen() {
        MapViewTransform view = overworld();
        assertEquals(200, view.screenX(100));
        assertEquals(120, view.screenY(-40));
    }

    @Test
    public void zoomScalesBlocksIntoPixels() {
        MapViewTransform view = overworld();
        assertEquals(200 + 2 * 50, view.screenX(150));
        assertEquals(120 - 2 * 10, view.screenY(-50));
    }

    @Test
    public void screenAndWorldAreInverseOfEachOther() {
        MapViewTransform view = overworld();
        for (int screenX = 0; screenX <= 400; screenX += 37) {
            assertEquals(screenX, view.screenX(view.worldX(screenX)));
        }
        for (int screenY = 0; screenY <= 240; screenY += 23) {
            assertEquals(screenY, view.screenY(view.worldZ(screenY)));
        }
    }

    @Test
    public void netherCoordinatesAreEightTimesTheMapScale() {
        // Xaero draws the nether at the overworld's scale, so a world
        // coordinate there is eight map coordinates; a marker that ignored
        // this would sit eight times too far from the middle.
        MapViewTransform nether = MapViewTransform.of(0, 0, 1.0D, 8.0D, -1, 100, 100, 1);
        assertEquals(100 + 10, nether.screenX(80));
        assertEquals(80.0D, nether.worldX(110), 1.0E-9D);
    }

    @Test
    public void guiScalingDoesNotMoveAMarkerOffItsShip() {
        // Xaero measures in real display pixels; drawScreen draws in GUI
        // pixels. On a 2560x1440 screen at GUI scale 4 the two differ by a
        // factor of four, and a marker that confused them would be drawn a
        // quarter of the way to its ship - the further from the middle of the
        // screen, the further out.
        MapViewTransform view = MapViewTransform.of(0, 0, 0.5D, 1.0D, 0, 1280, 720, 4);
        assertEquals(320, view.screenX(0));
        assertEquals(180, view.screenY(0));
        // 800 blocks east is 400 real pixels, which is 100 GUI pixels.
        assertEquals(420, view.screenX(800));
        assertEquals(280, view.screenY(800));
    }

    @Test
    public void theMouseIsReadInTheSamePixelsXaeroReadsIt() {
        MapViewTransform view = MapViewTransform.of(0, 0, 0.5D, 1.0D, 0, 1280, 720, 4);
        // Exactly Xaero's own cursor arithmetic: (pixel - centre) / scale.
        assertEquals(800.0D, view.worldXAtPixel(1680), 1.0E-9D);
        assertEquals(-1440.0D, view.worldZAtPixel(0), 1.0E-9D);
        // And a GUI coordinate is the same point once it is scaled back up.
        assertEquals(view.worldXAtPixel(1680), view.worldX(420), 1.0E-9D);
    }

    @Test
    public void everyGuiScaleKeepsScreenAndWorldInverse() {
        for (int factor : new int[] {1, 2, 3, 4}) {
            MapViewTransform view = MapViewTransform.of(-321.5D, 96.25D, 1.7D, 1.0D, 0,
                    1280, 720, factor);
            for (int screenX = 0; screenX <= 640; screenX += 53) {
                assertEquals(screenX, view.screenX(view.worldX(screenX)));
            }
        }
    }

    @Test
    public void anXaeroWithoutDimensionScalingIsStillSupported() {
        // 1.34 has the camera and the zoom but no per-dimension coordinate
        // scale. The overlay must run there, with a scale of one, rather than
        // failing on a method that build never had.
        assertTrue(MapViewTransform.isSupported());
        MapViewTransform plain = MapViewTransform.of(0, 0, 1.0D, 1.0D, 0, 100, 100, 1);
        assertEquals(180, plain.screenX(80));
    }

    @Test
    public void aMapThatIsNotThereIsNotRead() {
        assertNull(MapViewTransform.read(null));
    }

    @Test
    public void anUnsupportedXaeroNamesWhatItIsMissing() {
        // On a supported build nothing is missing and the overlay runs; on an
        // unsupported one the adapter must say which field went, because that
        // single line is the whole diagnosis after an Xaero update.
        if (MapViewTransform.isSupported()) {
            assertEquals("", MapViewTransform.missingFields());
        } else {
            assertTrue(MapViewTransform.missingFields().length() > 0);
        }
    }
}
