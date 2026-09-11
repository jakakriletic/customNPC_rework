package local.customnpcs;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.DialogCategory;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.controllers.data.QuestCategory;

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
}
