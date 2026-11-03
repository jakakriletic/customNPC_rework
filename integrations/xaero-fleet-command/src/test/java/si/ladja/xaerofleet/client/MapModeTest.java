package si.ladja.xaerofleet.client;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.*;

public class MapModeTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @After
    public void forget() {
        MapMode.useFile(null);
    }

    @Test
    public void startsOnTheWorldMap() throws IOException {
        MapMode.useFile(new File(tmp.getRoot(), MapMode.FILE_NAME));
        assertFalse(MapMode.isShipMap());
    }

    @Test
    public void choiceSurvivesARestart() throws IOException {
        File file = new File(tmp.getRoot(), "config/" + MapMode.FILE_NAME);
        MapMode.useFile(file);
        assertTrue(MapMode.toggle());
        assertTrue(file.isFile());

        MapMode.useFile(file); // as on the next start
        assertTrue(MapMode.isShipMap());
        assertFalse(MapMode.toggle());

        MapMode.useFile(file);
        assertFalse(MapMode.isShipMap());
    }

    @Test
    public void brokenOrForeignFileMeansTheWorldMap() throws IOException {
        File file = tmp.newFile(MapMode.FILE_NAME);
        try (FileWriter w = new FileWriter(file)) {
            w.write("map_mode=\\u12\n");      // a malformed escape
        }
        assertFalse(MapMode.read(file));
        try (FileWriter w = new FileWriter(file)) {
            w.write("map_mode=  SHIP  \n");
        }
        assertTrue(MapMode.read(file));
        try (FileWriter w = new FileWriter(file)) {
            w.write("something_else=ship\n");
        }
        assertFalse(MapMode.read(file));
    }

    @Test
    public void worksWithoutAFileAtAll() {
        MapMode.useFile(null);
        assertFalse(MapMode.isShipMap());
        assertTrue(MapMode.toggle());
        assertTrue(MapMode.isShipMap());
    }
}
