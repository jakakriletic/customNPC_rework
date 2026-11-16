package noppes.npcs.rework.linked;

import java.io.File;
import java.nio.file.Files;
import noppes.npcs.controllers.LinkedNpcController;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/** Q459: removing a Linked NPC group with RwLinkedGroups=1 deletes its files; the original keeps them (Q393). */
public class LinkedRemoveTest {
    private static File dir;

    /** getDir would need CustomNpcs, which needs a bootstrapped game. */
    private static class Controller extends LinkedNpcController {
        @Override public File getDir() { return dir; }
    }

    @Before public void setUp() throws Exception {
        dir = Files.createTempDirectory("linkednpcs").toFile();
    }

    @After public void tearDown() {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) { f.delete(); }
        }
        dir.delete();
    }

    private static File file(String name) { return new File(dir, name); }

    private static Controller withGroups() throws Exception {
        Controller c = new Controller();
        c.addData("Guards");
        c.addData("Keep");
        Files.copy(file("Guards.json").toPath(), file("Guards.json_prev").toPath());
        return c;
    }

    @Test public void originalKeepsTheFileSoTheGroupComesBack() throws Exception {
        Controller c = withGroups();
        c.removeData("guards", false);
        assertNull(c.getData("Guards"));
        assertTrue(file("Guards.json").isFile());
        assertTrue(file("Guards.json_prev").isFile());
        assertNotNull(new Controller().getData("Guards"));
    }

    @Test public void switchDeletesBothFilesAndTheGroupStaysGone() throws Exception {
        Controller c = withGroups();
        c.removeData("guards", true);
        assertNull(c.getData("Guards"));
        assertFalse(file("Guards.json").exists());
        assertFalse(file("Guards.json_prev").exists());
        Controller reloaded = new Controller();
        assertNull(reloaded.getData("Guards"));
        assertNotNull(reloaded.getData("Keep"));
        assertTrue(file("Keep.json").isFile());
    }

    @Test public void unknownNameDeletesNothing() throws Exception {
        Controller c = withGroups();
        c.removeData("Other", true);
        assertTrue(file("Guards.json").isFile());
        assertTrue(file("Guards.json_prev").isFile());
        assertEquals(2, new Controller().list.size());
    }

    @Test public void switchTextNamesTheDeletion() throws Exception {
        String info = noppes.npcs.CustomNpcs.class.getField("RwLinkedGroups")
                .getAnnotation(noppes.npcs.config.ConfigProp.class).info();
        assertTrue(info.contains("deletes its <group>.json and <group>.json_prev (Q459"));
        assertTrue(info.endsWith("Default 0"));
    }
}
