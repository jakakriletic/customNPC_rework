package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;

import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.LinkedNpcController;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.DialogCategory;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.controllers.data.QuestCategory;
import noppes.npcs.roles.RoleTrader;
import noppes.npcs.util.NBTJsonUtil;

import org.junit.Test;

/** Characterizes the public save entry points before their file replacement paths change. */
public class SynchronousJsonControllerBaselineTest {
    @Test
    public void dialogSaveEntryPointKeepsItsOriginalSignature() throws Exception {
        Method method = DialogController.class.getDeclaredMethod("saveDialog",
                DialogCategory.class, Dialog.class);

        assertEquals(Dialog.class, method.getReturnType());
    }

    @Test
    public void questSaveEntryPointKeepsItsOriginalSignature() throws Exception {
        Method method = QuestController.class.getDeclaredMethod("saveQuest",
                QuestCategory.class, Quest.class);

        assertEquals(Void.TYPE, method.getReturnType());
    }

    @Test
    public void linkedNpcDataKeepsItsOriginalNbtShape() {
        LinkedNpcController.LinkedData data = new LinkedNpcController.LinkedData();

        assertEquals("LinkedNpc", data.getNBT().getString("LinkedName"));
        assertEquals(0, data.getNBT().getCompoundTag("NPCData").getSize());
    }

    @Test
    public void traderSaveEntryPointKeepsItsOriginalSignature() throws Exception {
        Method method = RoleTrader.class.getDeclaredMethod("save", RoleTrader.class,
                String.class);

        assertEquals(Void.TYPE, method.getReturnType());
    }

    @Test
    public void synchronousJsonSavesDoNotUseDeleteThenRenameTargets() throws Exception {
        boolean patched = isSafeWriterPatched();

        assertUnsafeLiteral(DialogController.class, ".json_new", patched);
        assertUnsafeLiteral(QuestController.class, ".json_new", patched);
        assertUnsafeLiteral(LinkedNpcController.class, ".json_new", patched);
        assertUnsafeLiteral(RoleTrader.class, "_new", patched);
    }

    private static void assertUnsafeLiteral(Class<?> type, String literal, boolean patched)
            throws Exception {
        boolean found = classBytes(type).contains(literal);
        if (patched) {
            assertFalse(type.getName() + " still contains unsafe save suffix " + literal, found);
        } else {
            assertTrue(type.getName() + " original save suffix changed unexpectedly", found);
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
