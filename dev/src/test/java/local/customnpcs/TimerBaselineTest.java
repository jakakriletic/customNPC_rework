package local.customnpcs;

import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.entity.data.DataTimers;
import org.junit.Test;
import static org.junit.Assert.*;

/** Characterization of the October 2019 release, including its known persistence bug. */
public class TimerBaselineTest {
    @Test public void preservesKnownOriginalIntervalBugUntilExplicitlyFixed() {
        DataTimers timers = new DataTimers(null);
        timers.start(7, 200, true);
        NBTTagCompound root = new NBTTagCompound();
        timers.writeToNBT(root);
        NBTTagCompound saved = root.getTagList("NpcsTimers", 10).getCompoundTagAt(0);
        assertEquals(7, saved.getInteger("ID"));
        assertEquals("Known original bug, NOT the desired fixed behavior", 7, saved.getInteger("TimerTicks"));
        assertEquals(200, saved.getInteger("Ticks"));
        assertTrue(saved.getBoolean("Repeat"));
    }

    @Test public void supportsStopAndForceStart() {
        DataTimers timers = new DataTimers(null);
        timers.start(7, 200, false);
        timers.forceStart(7, 100, true);
        assertTrue(timers.has(7));
        assertTrue(timers.stop(7));
        assertFalse(timers.stop(7));
        assertFalse(timers.has(7));
    }

    @Test public void loadsExistingSavedTimerWithoutChangingRemainingTicks() {
        DataTimers original = new DataTimers(null);
        original.start(12, 100, false);
        NBTTagCompound root = new NBTTagCompound();
        original.writeToNBT(root);
        DataTimers loaded = new DataTimers(null);
        loaded.readFromNBT(root);
        NBTTagCompound savedAgain = new NBTTagCompound();
        loaded.writeToNBT(savedAgain);
        assertEquals(root, savedAgain);
        assertTrue(loaded.has(12));
    }
}
