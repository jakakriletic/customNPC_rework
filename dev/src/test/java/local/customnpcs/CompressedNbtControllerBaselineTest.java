package local.customnpcs;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import noppes.npcs.controllers.BankController;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.GlobalDataController;
import noppes.npcs.controllers.RecipeController;
import noppes.npcs.controllers.SpawnController;
import noppes.npcs.controllers.TransportController;
import noppes.npcs.client.controllers.PresetController;

import org.junit.Test;

/** Characterizes compressed-NBT save entry points before file replacement changes. */
public class CompressedNbtControllerBaselineTest {
    @Test
    public void saveEntryPointsKeepTheirOriginalSignatures() throws Exception {
        assertVoidMethod(BankController.class, "saveBanks");
        assertVoidMethod(FactionController.class, "saveFactions");
        assertVoidMethod(GlobalDataController.class, "saveData");
        assertVoidMethod(TransportController.class, "saveCategories");
        assertVoidMethod(RecipeController.class, "saveCategories");
        assertVoidMethod(SpawnController.class, "saveData");
        assertVoidMethod(PresetController.class, "save");
    }

    private static void assertVoidMethod(Class<?> type, String name) throws Exception {
        Method method = type.getDeclaredMethod(name);
        assertEquals(Void.TYPE, method.getReturnType());
    }
}
