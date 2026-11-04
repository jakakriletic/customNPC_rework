package local.customnpcs;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.client.gui.GuiNpcMobSpawner;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.IGuiData;
import org.junit.Test;
import static org.junit.Assert.*;

/** M13.4 import baseline; runs against both the mapped original and the restored class, without a GL context. */
public class GuiNpcMobSpawnerBaselineTest {
    @Test public void originalScreenAndPacketEntryPointsStayAvailable() throws Exception {
        assertEquals(GuiNPCInterface.class, GuiNpcMobSpawner.class.getSuperclass());
        assertTrue(IGuiData.class.isAssignableFrom(GuiNpcMobSpawner.class));
        assertTrue(Modifier.isPublic(GuiNpcMobSpawner.class.getConstructor(int.class, int.class, int.class).getModifiers()));
        for (String name : new String[] {"initGui", "save"}) {
            assertTrue(Modifier.isPublic(GuiNpcMobSpawner.class.getDeclaredMethod(name).getModifiers()));
        }
        Method action = GuiNpcMobSpawner.class.getDeclaredMethod("actionPerformed", GuiButton.class);
        assertTrue(Modifier.isProtected(action.getModifiers()));
        assertEquals(void.class, action.getReturnType());
        assertEquals(void.class, GuiNpcMobSpawner.class.getDeclaredMethod("setGuiData", NBTTagCompound.class).getReturnType());
        assertEquals(void.class, GuiNpcMobSpawner.class.getDeclaredMethod("keyTyped", char.class, int.class).getReturnType());
    }

    @Test public void cloneSelectionAndCoordinatesBelongToOriginalScreen() throws Exception {
        for (String name : new String[] {"posX", "posY", "posZ", "activeTab", "showingClones"}) {
            Field field = GuiNpcMobSpawner.class.getDeclaredField(name);
            assertEquals(int.class, field.getType());
            assertTrue(Modifier.isPrivate(field.getModifiers()));
            assertEquals("showingClones".equals(name), Modifier.isStatic(field.getModifiers()));
        }
        assertEquals(GuiCustomScroll.class, GuiNpcMobSpawner.class.getDeclaredField("scroll").getType());
        Method compound = GuiNpcMobSpawner.class.getDeclaredMethod("getCompound");
        assertTrue(Modifier.isPrivate(compound.getModifiers()));
        assertEquals(NBTTagCompound.class, compound.getReturnType());
    }
}
