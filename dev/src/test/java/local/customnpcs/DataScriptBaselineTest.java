package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.entity.data.DataScript;
import org.junit.Test;

/**
 * Baseline of {@code DataScript} when it is restored into {@code src/patch} (M6.2, D-003).
 *
 * <p>The NPC script handler is the edge where M6.2 adds the Java branch. This test pins the
 * 01Oct19 NBT keys, default language and the disabled no-op; event order with an entity and
 * a world is proved in the runtime scenario, not here.
 */
public class DataScriptBaselineTest {
    @Test
    public void defaultsAndNbtKeys() {
        DataScript data = new DataScript(null);
        assertEquals("ECMAScript", data.getLanguage());
        assertFalse(data.getEnabled());
        assertTrue(data.getScripts().isEmpty());
        NBTTagCompound saved = data.writeToNBT(new NBTTagCompound());
        assertEquals("ECMAScript", saved.getString("ScriptLanguage"));
        assertFalse(saved.getBoolean("ScriptEnabled"));
        assertEquals(0, saved.getTagList("Scripts", 10).tagCount());
    }

    @Test
    public void languageAndEnabledRoundTrip() {
        DataScript data = new DataScript(null);
        data.setLanguage("Java");
        data.setEnabled(true);
        DataScript loaded = new DataScript(null);
        loaded.readFromNBT(data.writeToNBT(new NBTTagCompound()));
        assertEquals("Java", loaded.getLanguage());
        assertTrue(loaded.getEnabled());
        // A missing key reads as the empty language, not the constructor default.
        DataScript empty = new DataScript(null);
        empty.readFromNBT(new NBTTagCompound());
        assertEquals("", empty.getLanguage());
    }

    @Test
    public void disabledHandlerNeverTouchesNpc() {
        // enabled=false short-circuits before the world/NPC is read.
        new DataScript(null).runScript(EnumScriptType.TICK, null);
    }
}
