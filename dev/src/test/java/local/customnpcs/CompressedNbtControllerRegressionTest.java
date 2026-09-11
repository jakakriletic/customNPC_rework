package local.customnpcs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import noppes.npcs.controllers.BankController;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.GlobalDataController;
import noppes.npcs.controllers.RecipeController;
import noppes.npcs.controllers.SchematicController;
import noppes.npcs.controllers.SpawnController;
import noppes.npcs.controllers.TransportController;
import noppes.npcs.client.controllers.PresetController;
import noppes.npcs.util.NBTJsonUtil;

import org.junit.Test;

/** Proves patched compressed-NBT controllers no longer expose delete-then-rename targets. */
public class CompressedNbtControllerRegressionTest {
    @Test
    public void compressedNbtSavesDoNotUseDatNewTargets() throws Exception {
        boolean patched = isSafeWriterPatched();
        assertUnsafeLiteral(BankController.class, patched);
        assertUnsafeLiteral(FactionController.class, patched);
        assertUnsafeLiteral(GlobalDataController.class, patched);
        assertUnsafeLiteral(TransportController.class, patched);
        assertUnsafeLiteral(RecipeController.class, patched);
        assertUnsafeLiteral(SpawnController.class, patched);
        assertUnsafeLiteral(PresetController.class, "_new", patched);
        assertSafeHelperReference(SchematicController.class, patched);
    }

    private static void assertUnsafeLiteral(Class<?> type, boolean patched) throws Exception {
        assertUnsafeLiteral(type, ".dat_new", patched);
    }

    private static void assertUnsafeLiteral(Class<?> type, String literal, boolean patched)
            throws Exception {
        boolean found = classBytes(type).contains(literal);
        if (patched) {
            assertFalse(type.getName() + " still contains unsafe save path " + literal, found);
        } else {
            assertTrue(type.getName() + " original save path changed unexpectedly", found);
        }
    }

    private static void assertSafeHelperReference(Class<?> type, boolean patched)
            throws Exception {
        boolean found = classBytes(type).contains("noppes/npcs/rework/data/CompressedNbtFile");
        if (patched) {
            assertTrue(type.getName() + " does not call the safe compressed-NBT writer", found);
        } else {
            assertFalse(type.getName() + " original unexpectedly calls the rework writer", found);
        }
    }

    private static boolean isSafeWriterPatched() {
        try {
            return NBTJsonUtil.class.getField("REWORK_SAFE_WRITER").getBoolean(null);
        } catch (Exception originalBinary) {
            return false;
        }
    }

    private static String classBytes(Class<?> type) throws Exception {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        InputStream input = type.getResourceAsStream(resource);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        } finally {
            input.close();
        }
        return new String(output.toByteArray(), Charset.forName("ISO-8859-1"));
    }
}
