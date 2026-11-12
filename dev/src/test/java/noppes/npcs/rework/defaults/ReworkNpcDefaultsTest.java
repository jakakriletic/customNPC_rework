package noppes.npcs.rework.defaults;

import noppes.npcs.ModelEyeData;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import static org.junit.Assert.*;

/** Owner request 12. 11. 2026: a new NPC has no extra eye layer (its skin's own eyes); saved eye data is kept. */
public class ReworkNpcDefaultsTest {
    @Test public void newNpcHasNoEyeLayer() {
        assertFalse(new ModelEyeData().isEnabled());
    }

    @Test public void savedEyesAreKept() {
        ModelEyeData saved = new ModelEyeData();
        saved.setType(0);
        NBTTagCompound tag = saved.writeToNBT();
        ModelEyeData loaded = new ModelEyeData();
        loaded.readFromNBT(tag);
        assertTrue(loaded.isEnabled());
    }
}
