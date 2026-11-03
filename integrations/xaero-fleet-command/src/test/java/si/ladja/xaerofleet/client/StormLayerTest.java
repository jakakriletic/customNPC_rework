package si.ladja.xaerofleet.client;

import java.io.File;
import org.junit.Test;
import static org.junit.Assert.*;

public class StormLayerTest {
    @Test public void thirdTabHasIndependentHitArea() {
        MapModeSwitch s=MapModeSwitch.layout(800,45,55,65,182);
        assertEquals(3,s.count());int[] tab=s.half(MapModeSwitch.STORM);
        assertEquals(MapModeSwitch.STORM,s.at(tab[0],10));assertEquals(MapModeSwitch.NONE,s.at(tab[2],10));
        assertEquals(s.half(1)[2],tab[0]);
    }
    @Test public void stormModeSurvivesRestartAndCannotKeepFleetModeActive() throws Exception {
        File file=File.createTempFile("winter-map-mode-",".properties");
        try {
            MapMode.useFile(file);MapMode.setShipMap(true);MapMode.setMode(2);
            assertTrue(MapMode.isStormMap());assertFalse(MapMode.isShipMap());
            MapMode.useFile(file);assertEquals(2,MapMode.current());
            MapMode.setShipMap(false);assertEquals(0,MapMode.current());assertFalse(MapMode.isStormMap());
        } finally {MapMode.useFile(null);file.delete();}
    }
}
