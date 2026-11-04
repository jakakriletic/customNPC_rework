package noppes.npcs.rework.linked;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NBTTags;
import org.junit.Test;
import static org.junit.Assert.*;

public class LinkedOwnFieldsTest {
    private static NBTTagList path(int... xyz) {
        NBTTagList list = new NBTTagList();
        for (int i = 0; i + 2 < xyz.length; i += 3) {
            NBTTagCompound p = new NBTTagCompound();
            p.setIntArray("Array", new int[] {xyz[i], xyz[i + 1], xyz[i + 2]});
            list.appendTag(p);
        }
        return list;
    }

    private static NBTTagCompound npc(String name, int x, int orientation, int movingPos) {
        NBTTagCompound c = new NBTTagCompound();
        c.setString("Name", name);
        c.setIntArray("StartPosNew", new int[] {x, 4, 0});
        c.setInteger("Orientation", orientation);
        c.setTag("MovingPathNew", path(x, 4, 5, x, 4, 10));
        c.setInteger("MovingPos", movingPos);
        c.setFloat("PositionOffsetY", 5f);
        NBTTagCompound stats = new NBTTagCompound();
        stats.setInteger("MaxHealth", 20);
        c.setTag("Stats", stats);
        return c;
    }

    /** Group data as saveNpcData stores it: the edited NPC's data without MovingPathNew. */
    private static NBTTagCompound group() {
        NBTTagCompound g = npc("Edited", 0, 270, 0);
        g.removeTag("MovingPathNew");
        g.setFloat("PositionOffsetY", 3f);
        g.getCompoundTag("Stats").setInteger("MaxHealth", 77);
        return g;
    }

    @Test public void defaultSwitchIsOriginal() throws Exception {
        // Reflection only: reading the value would initialize CustomNpcs, which needs a bootstrapped game.
        java.lang.reflect.Field f = CustomNpcs.class.getField("RwLinkedOwnFields");
        assertSame(int.class, f.getType());
        noppes.npcs.config.ConfigProp prop = f.getAnnotation(noppes.npcs.config.ConfigProp.class);
        assertNotNull(prop);
        assertTrue(prop.info().endsWith("Default 0"));
    }

    @Test public void originalMergeTakesTheEditedNpcsHomeOrientationAndPathIndex() {
        NBTTagCompound merged = NBTTags.NBTMerge(npc("B", 40, 90, 1), group());
        assertArrayEquals(new int[] {0, 4, 0}, merged.getIntArray("StartPosNew"));
        assertEquals(270, merged.getInteger("Orientation"));
        assertEquals(0, merged.getInteger("MovingPos"));
        assertEquals(npc("B", 40, 90, 1).getTag("MovingPathNew"), merged.getTag("MovingPathNew"));
    }

    @Test public void restoreKeepsOwnFieldsAndStillTakesSharedOnes() {
        NBTTagCompound own = npc("B", 40, 90, 1);
        NBTTagCompound before = own.copy();
        NBTTagCompound merged = NBTTags.NBTMerge(own, group());
        LinkedOwnFields.restore(own, merged);
        assertArrayEquals(new int[] {40, 4, 0}, merged.getIntArray("StartPosNew"));
        assertEquals(90, merged.getInteger("Orientation"));
        assertEquals(1, merged.getInteger("MovingPos"));
        assertEquals(own.getTag("MovingPathNew"), merged.getTag("MovingPathNew"));
        assertEquals("Edited", merged.getString("Name"));
        assertEquals(77, merged.getCompoundTag("Stats").getInteger("MaxHealth"));
        assertEquals(3f, merged.getFloat("PositionOffsetY"), 0f);
        assertEquals("own data is not changed", before, own);
    }

    @Test public void restoredValuesAreCopiesNotShared() {
        NBTTagCompound own = npc("B", 40, 90, 1);
        NBTTagCompound merged = NBTTags.NBTMerge(own, group());
        LinkedOwnFields.restore(own, merged);
        merged.getTagList("MovingPathNew", 10).removeTag(0);
        assertEquals(2, own.getTagList("MovingPathNew", 10).tagCount());
    }

    @Test public void aKeyTheMemberLacksIsNotTakenFromTheGroup() {
        NBTTagCompound own = npc("B", 40, 90, 1);
        own.removeTag("StartPosNew");
        NBTTagCompound merged = NBTTags.NBTMerge(own, group());
        LinkedOwnFields.restore(own, merged);
        assertFalse(merged.hasKey("StartPosNew"));
    }

    @Test public void keysAreExactlyHomeOrientationPathAndPathIndex() {
        assertArrayEquals(new String[] {"StartPosNew", "Orientation", "MovingPathNew", "MovingPos"},
                LinkedOwnFields.KEYS);
    }

    @Test public void homeReadsTheMembersOwnHome() {
        assertEquals(new net.minecraft.util.math.BlockPos(40, 4, 0), LinkedOwnFields.home(npc("B", 40, 90, 1)));
    }

    @Test public void homeIsNullWithoutAValidHome() {
        NBTTagCompound own = npc("B", 40, 90, 1);
        own.removeTag("StartPosNew");
        assertNull(LinkedOwnFields.home(own));
        own.setIntArray("StartPosNew", new int[] {1, 2});
        assertNull(LinkedOwnFields.home(own));
    }
}
