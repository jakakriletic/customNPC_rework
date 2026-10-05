package noppes.npcs.rework.entity;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;

public class HitboxGuiTest {

    @Test
    public void requestRoundTripsEveryMode() {
        for (int mode = 0; mode <= 2; mode++) {
            NBTTagCompound c = new NBTTagCompound();
            HitboxGui.writeRequest(c, mode);
            assertEquals(mode, HitboxGui.readRequest(c));
        }
    }

    /** Urejevalnik modela, transformacija in povezani NPC-ji poslejo DataDisplay brez kljuca. */
    @Test
    public void displayNbtWithoutRequestDoesNotChangeMode() {
        NBTTagCompound c = new NBTTagCompound();
        c.setString("Name", "Npc");
        c.setInteger("RwHitboxMode", HitboxWeights.SOLID);
        assertEquals(-1, HitboxGui.readRequest(c));
    }

    /** Izbira ORIGINAL v GUI-ju mora nacin ponastaviti, ne izpustiti kljuca. */
    @Test
    public void originalRequestIsExplicit() {
        NBTTagCompound c = new NBTTagCompound();
        HitboxGui.writeRequest(c, HitboxWeights.ORIGINAL);
        assertTrue(c.hasKey(HitboxGui.KEY_GUI));
        assertEquals(HitboxWeights.ORIGINAL, HitboxGui.readRequest(c));
    }

    @Test
    public void invalidRequestFallsBackToOriginal() {
        NBTTagCompound c = new NBTTagCompound();
        c.setInteger(HitboxGui.KEY_GUI, 7);
        assertEquals(HitboxWeights.ORIGINAL, HitboxGui.readRequest(c));
        NBTTagCompound s = new NBTTagCompound();
        s.setString(HitboxGui.KEY_GUI, "solid");
        assertEquals(-1, HitboxGui.readRequest(s));
    }

    /** Klient dobi shranjeni nacin posebej; ucinkovit je pri izklopljenem stikalu ORIGINAL. */
    @Test
    public void spawnCarriesStoredMode() {
        for (int mode = 1; mode <= 2; mode++) {
            NBTTagCompound c = new NBTTagCompound();
            HitboxGui.writeSpawn(c, mode);
            assertEquals(mode, HitboxGui.readStored(c));
        }
    }

    /** Privzeti NPC: spawn podatki ostanejo brez novih kljucev (enaki originalu). */
    @Test
    public void defaultNpcAddsNothingToSpawnData() {
        NBTTagCompound c = new NBTTagCompound();
        HitboxGui.writeSpawn(c, HitboxWeights.ORIGINAL);
        assertTrue(c.getKeySet().isEmpty());
        assertEquals(HitboxWeights.ORIGINAL, HitboxGui.readStored(c));
    }

    @Test
    public void labelsFollowModeOrder() {
        assertArrayEquals(new String[]{"Original", "Solid", "Smart"}, HitboxGui.labels());
        assertEquals("solid", HitboxWeights.name(HitboxWeights.SOLID));
    }
}
