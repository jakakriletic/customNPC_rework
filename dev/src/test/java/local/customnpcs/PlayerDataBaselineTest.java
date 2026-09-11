package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.controllers.data.PlayerData;

import org.junit.Test;

/** Characterizes the side-effect-free PlayerData NBT snapshot before its save path changes. */
public class PlayerDataBaselineTest {
    @Test
    public void emptyPlayerDataKeepsItsOriginalDefaults() {
        NBTTagCompound data = new PlayerData().getNBT();

        assertEquals("", data.getString("PlayerName"));
        assertEquals("", data.getString("UUID"));
        assertEquals(0, data.getInteger("PlayerCompanionId"));
        assertFalse(data.hasKey("PlayerCompanion"));
    }
}
