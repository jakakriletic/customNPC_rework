package si.ladja.xaerofleet.client;

import org.junit.Test;

import static org.junit.Assert.*;

public class MapModeSwitchTest {

    @Test
    public void centredOnAWideScreen() {
        MapModeSwitch sw = MapModeSwitch.layout(640, 45, 52, 182);
        int width = sw.right() - sw.left();
        assertEquals(45 + 52 + 4 * MapModeSwitch.PAD, width);
        assertEquals((640 - width) / 2, sw.left());
        assertEquals(sw.half(MapModeSwitch.WORLD)[2], sw.half(MapModeSwitch.SHIP)[0]);
    }

    @Test
    public void keepsOffTheFleetPanelOnANarrowScreen() {
        MapModeSwitch sw = MapModeSwitch.layout(320, 45, 52, 182);
        assertEquals(182, sw.left());
    }

    @Test
    public void clicksLandOnTheRightHalf() {
        MapModeSwitch sw = MapModeSwitch.layout(640, 45, 52, 182);
        int[] world = sw.half(MapModeSwitch.WORLD);
        int[] ship = sw.half(MapModeSwitch.SHIP);
        int y = MapModeSwitch.TOP + MapModeSwitch.HEIGHT / 2;
        assertEquals(MapModeSwitch.WORLD, sw.at(world[0], y));
        assertEquals(MapModeSwitch.WORLD, sw.at(world[2] - 0.5, y));
        assertEquals(MapModeSwitch.SHIP, sw.at(ship[0], y));
        assertEquals(MapModeSwitch.SHIP, sw.at(ship[2] - 0.5, y));
        assertEquals(MapModeSwitch.NONE, sw.at(ship[2], y));
        assertEquals(MapModeSwitch.NONE, sw.at(world[0] - 1, y));
        assertEquals(MapModeSwitch.NONE, sw.at(world[0] + 3, MapModeSwitch.TOP - 1));
        assertEquals(MapModeSwitch.NONE, sw.at(world[0] + 3, sw.bottom()));
    }
}
