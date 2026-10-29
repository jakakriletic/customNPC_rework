package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.TreeMap;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.controllers.IScriptHandler;
import org.junit.Test;

/**
 * Baseline of {@code ScriptContainer} when it is restored into {@code src/patch} (M6.5, D-003).
 *
 * <p>The JS tab container is where M6.5 fixes B5. Its static initializer reads game constants,
 * so this test only inspects the class without initializing it: the public fields and methods
 * that scripts, wrappers and the GUI use. Reload behaviour is proved in the runtime scenario.
 */
public class ScriptContainerBaselineTest {
    private static Class<?> container() throws Exception {
        return Class.forName("noppes.npcs.controllers.ScriptContainer", false, ScriptContainerBaselineTest.class.getClassLoader());
    }

    @Test
    public void publicStateKeepsItsShape() throws Exception {
        Class<?> c = container();
        assertEquals(String.class, c.getField("script").getType());
        assertEquals(String.class, c.getField("fullscript").getType());
        assertEquals(List.class, c.getField("scripts").getType());
        assertEquals(TreeMap.class, c.getField("console").getType());
        assertEquals(boolean.class, c.getField("errored").getType());
        assertEquals(long.class, c.getField("lastCreated").getType());
        // Chest and projectile wrappers read the running container through this static field.
        Field current = c.getField("Current");
        assertTrue(Modifier.isStatic(current.getModifiers()));
        assertEquals(c, current.getType());
    }

    @Test
    public void publicMethodsKeepTheirSignatures() throws Exception {
        Class<?> c = container();
        c.getConstructor(IScriptHandler.class);
        assertEquals(void.class, c.getMethod("run", String.class, Object.class).getReturnType());
        assertEquals(boolean.class, c.getMethod("hasCode").getReturnType());
        assertEquals(boolean.class, c.getMethod("isValid").getReturnType());
        assertEquals(void.class, c.getMethod("appandConsole", String.class).getReturnType());
        assertEquals(void.class, c.getMethod("setEngine", String.class).getReturnType());
        assertEquals(void.class, c.getMethod("readFromNBT", NBTTagCompound.class).getReturnType());
        assertEquals(NBTTagCompound.class, c.getMethod("writeToNBT", NBTTagCompound.class).getReturnType());
    }
}
