package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.rework.data.WorldSaveSession;
import noppes.npcs.util.NBTJsonUtil;

import org.junit.After;
import org.junit.Test;

public class WorldSaveSessionTest {
    @After
    public void stopAnySessionLeftByAFailedTest() {
        WorldSaveSession.end(5, TimeUnit.SECONDS);
    }

    @Test
    public void drainsEachWorldBeforeTheNextSessionAndRejectsLateWrites() throws Exception {
        File worldA = Files.createTempDirectory("world-save-a").toFile();
        File worldB = Files.createTempDirectory("world-save-b").toFile();
        NBTTagCompound dataA = data("world-a");
        NBTTagCompound dataB = data("world-b");

        WorldSaveSession.begin(worldA);
        WorldSaveSession sessionA = WorldSaveSession.current();
        assertTrue(sessionA.savePlayerData("same.json", dataA));
        assertTrue(WorldSaveSession.end(5, TimeUnit.SECONDS));
        assertFalse(sessionA.savePlayerData("late.json", dataA));

        WorldSaveSession.begin(worldB);
        assertTrue(WorldSaveSession.current().savePlayerData("same.json", dataB));
        assertTrue(WorldSaveSession.end(5, TimeUnit.SECONDS));

        assertEquals(dataA, NBTJsonUtil.LoadFile(new File(worldA, "playerdata/same.json")));
        assertEquals(dataB, NBTJsonUtil.LoadFile(new File(worldB, "playerdata/same.json")));
        assertFalse(new File(worldA, "playerdata/late.json").exists());
    }

    @Test
    public void reportsQueuedWriteFailureWhenDraining() throws Exception {
        File rootThatIsAFile = Files.createTempFile("world-save-invalid", ".tmp").toFile();
        WorldSaveSession.begin(rootThatIsAFile);

        assertTrue(WorldSaveSession.current().savePlayerData("player.json", data("value")));
        assertFalse(WorldSaveSession.end(5, TimeUnit.SECONDS));
    }

    private static NBTTagCompound data(String value) {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setString("value", value);
        return compound;
    }
}
