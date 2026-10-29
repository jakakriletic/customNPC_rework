package noppes.npcs.rework.nav;

import java.lang.reflect.Field;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagString;
import noppes.npcs.entity.EntityCustomNpc;
import org.junit.Test;
import static org.junit.Assert.*;

/** Editor wire contract: legacy saves, strict NBT types and untrusted status. */
public class NavGuiDataTest {
    @Test
    public void legacySavePreservesAnExistingSelection() throws Exception {
        EntityCustomNpc npc = shell();
        npc.setRwNavBackend(1);
        NavGuiData.applySelection(npc, new NBTTagCompound());
        assertEquals(1, npc.getRwNavBackend());
    }

    @Test
    public void onlyExplicitIntegerZeroOrOneMayChangeSelection() throws Exception {
        EntityCustomNpc npc = shell();
        NBTTagCompound data = new NBTTagCompound();
        for (int value : new int[] {1, 0, 1, 0}) {
            data.setInteger(NavGuiData.CHOICE, value);
            NavGuiData.applySelection(npc, data);
            assertEquals(value, npc.getRwNavBackend());
        }
        for (NBTBase value : new NBTBase[] {new NBTTagByte((byte) 0), new NBTTagDouble(0),
                new NBTTagString("0"), new NBTTagCompound()}) {
            npc.setRwNavBackend(1);
            data.setTag(NavGuiData.CHOICE, value);
            NavGuiData.applySelection(npc, data);
            assertEquals(1, npc.getRwNavBackend());
        }
        for (int value : new int[] {-1, 2, Integer.MAX_VALUE}) {
            data.setInteger(NavGuiData.CHOICE, value);
            NavGuiData.applySelection(npc, data);
            assertEquals(1, npc.getRwNavBackend());
        }
    }

    @Test
    public void forgedServerStatusCannotChangeChoice() throws Exception {
        EntityCustomNpc npc = shell();
        NBTTagCompound data = new NBTTagCompound();
        NBTTagCompound forged = new NBTTagCompound();
        forged.setInteger("Actual", 1);
        forged.setString("Reason", "active");
        data.setTag(NavGuiData.STATUS, forged);
        NavGuiData.applySelection(npc, data);
        assertEquals(0, npc.getRwNavBackend());
    }

    @Test
    public void guiChoiceIsIndependentOfActualServerState() {
        NavGuiState state = new NavGuiState();
        NBTTagCompound response = response(1, 0, "global_off");
        NBTTagCompound before = response.copy();
        state.load(response);
        assertTrue(state.ready());
        assertEquals(1, state.choice());
        assertEquals("Vanilla", state.actualName());
        assertEquals("Global setting disabled", state.reasonText());
        assertFalse(state.pending());
        state.choose(0);
        assertTrue(state.pending());
        assertEquals("Vanilla", state.actualName());
        state.choose(1);
        assertFalse(state.pending());
        assertEquals(before, response);
    }

    @Test
    public void saveContainsOnlyChoiceAndPreservesOtherAiFields() {
        NavGuiState state = new NavGuiState();
        state.load(response(0, 0, "selected_vanilla"));
        state.choose(1);
        NBTTagCompound save = new NBTTagCompound();
        save.setInteger("MovementType", 2);
        save.setInteger("MoveSpeed", 7);
        NBTTagCompound original = save.copy();
        state.writeChoice(save);
        assertTrue(save.hasKey(NavGuiData.CHOICE, 3));
        assertEquals(1, save.getInteger(NavGuiData.CHOICE));
        assertFalse(save.hasKey(NavGuiData.STATUS));
        save.removeTag(NavGuiData.CHOICE);
        assertEquals(original, save);
    }

    @Test
    public void missingOrMalformedStatusStaysUnknownAndDoesNotWriteAChoice() {
        NavGuiState state = new NavGuiState();
        state.choose(1);
        NBTTagCompound save = new NBTTagCompound();
        state.writeChoice(save);
        assertTrue(save.hasNoTags());
        NBTTagCompound response = response(1, 1, "active");
        state.load(response);
        assertTrue(state.ready());
        response.getCompoundTag(NavGuiData.STATUS).setInteger("Actual", 99);
        state.load(response);
        assertFalse(state.ready());
        assertEquals("Unknown", state.actualName());
        state.load(new NBTTagCompound());
        assertFalse(state.ready());
        state.writeChoice(save);
        assertTrue(save.hasNoTags());
    }

    @Test
    public void fallbackReasonsAreSpecificAndUnknownReasonsAreSafe() {
        for (String reason : new String[] {"selected_vanilla", "global_off", "missing_mod", "movement",
                "riding", "carrying", "squad", "killed", "unavailable", "unsupported", "incompatible", "not_attached"}) {
            NavGuiState state = new NavGuiState();
            state.load(response(1, 0, reason));
            assertTrue(state.ready());
            assertNotEquals("Unknown server reason", state.reasonText());
        }
        NavGuiState state = new NavGuiState();
        state.load(response(1, 0, "untrusted text"));
        assertEquals("Unknown server reason", state.reasonText());
    }

    @Test
    public void lateServerReplyDoesNotOverwritePendingEdits() {
        NavGuiState state = new NavGuiState();
        state.load(response(0, 0, "selected_vanilla"));
        state.choose(1);
        state.load(response(0, 0, "selected_vanilla"));
        assertEquals(1, state.choice());
        assertTrue(state.pending());
    }

    private static NBTTagCompound response(int selected, int actual, String reason) {
        NBTTagCompound data = new NBTTagCompound();
        data.setInteger(NavGuiData.CHOICE, selected);
        NBTTagCompound status = new NBTTagCompound();
        status.setInteger("Actual", actual);
        status.setString("Reason", reason);
        data.setTag(NavGuiData.STATUS, status);
        return data;
    }

    private static EntityCustomNpc shell() throws Exception {
        Class<?> type = Class.forName("sun.misc.Unsafe");
        Field field = type.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (EntityCustomNpc) type.getMethod("allocateInstance", Class.class)
                .invoke(field.get(null), EntityCustomNpc.class);
    }
}
