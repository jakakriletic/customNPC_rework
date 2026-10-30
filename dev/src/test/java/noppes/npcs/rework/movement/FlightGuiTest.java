package noppes.npcs.rework.movement;

import java.lang.reflect.Field;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagString;
import noppes.npcs.entity.EntityCustomNpc;
import org.junit.Test;
import static org.junit.Assert.*;

/** P6 editor wire contract: legacy saves, strict NBT types, MOMENTUM preserved. */
public class FlightGuiTest {
    @Test
    public void legacyOrUntouchedSavePreservesStoredMode() throws Exception {
        for (int stored : new int[] {0, 1, 2}) {
            EntityCustomNpc npc = shell();
            npc.setRwFlightMode(stored);
            FlightGui.applySelection(npc, new NBTTagCompound());
            assertEquals(stored, npc.getRwFlightMode().id());

            FlightGui state = new FlightGui();
            state.load(reply(stored));
            NBTTagCompound save = new NBTTagCompound();
            state.writeChoice(save);
            assertTrue(save.hasNoTags());
        }
    }

    @Test
    public void onlyExplicitIntegerOriginalOrHoverMayChangeMode() throws Exception {
        EntityCustomNpc npc = shell();
        NBTTagCompound data = new NBTTagCompound();
        for (int value : new int[] {1, 0, 1}) {
            data.setInteger(FlightGui.CHOICE, value);
            FlightGui.applySelection(npc, data);
            assertEquals(value, npc.getRwFlightMode().id());
        }
        for (NBTBase value : new NBTBase[] {new NBTTagByte((byte) 0), new NBTTagDouble(0),
                new NBTTagString("0"), new NBTTagCompound()}) {
            data.setTag(FlightGui.CHOICE, value);
            FlightGui.applySelection(npc, data);
            assertEquals(1, npc.getRwFlightMode().id());
        }
        for (int value : new int[] {-1, 2, 3, Integer.MAX_VALUE}) {
            data.setInteger(FlightGui.CHOICE, value);
            FlightGui.applySelection(npc, data);
            assertEquals(1, npc.getRwFlightMode().id());
        }
    }

    @Test
    public void changedChoiceIsSentAndRevertedChoiceIsNot() {
        FlightGui state = new FlightGui();
        state.load(reply(0));
        assertTrue(state.ready());
        assertTrue(state.selectable());
        assertArrayEquals(new String[] {"Fly: Orig", "Fly: Hover"}, state.labels());
        state.choose(1);
        assertTrue(state.pending());
        NBTTagCompound save = new NBTTagCompound();
        save.setInteger("MovementType", 1);
        state.writeChoice(save);
        assertEquals(1, save.getInteger(FlightGui.CHOICE));
        assertEquals(1, save.getInteger("MovementType"));
        state.choose(0);
        NBTTagCompound again = new NBTTagCompound();
        state.writeChoice(again);
        assertTrue(again.hasNoTags());
    }

    @Test
    public void storedMomentumIsShownButCannotBeChanged() {
        FlightGui state = new FlightGui();
        state.load(reply(2));
        assertTrue(state.ready());
        assertFalse(state.selectable());
        assertArrayEquals(new String[] {"Fly: Mom."}, state.labels());
        assertEquals(0, state.buttonValue());
        state.choose(0);
        state.choose(1);
        NBTTagCompound save = new NBTTagCompound();
        state.writeChoice(save);
        assertTrue(save.hasNoTags());
    }

    @Test
    public void missingReplyIsNotReadyAndWritesNothing() {
        FlightGui state = new FlightGui();
        state.choose(1);
        state.load(new NBTTagCompound());
        assertFalse(state.ready());
        NBTTagCompound save = new NBTTagCompound();
        state.writeChoice(save);
        assertTrue(save.hasNoTags());
        NBTTagCompound wrongType = new NBTTagCompound();
        wrongType.setString(FlightGui.CHOICE, "1");
        state.load(wrongType);
        assertFalse(state.ready());
    }

    @Test
    public void lateReplyKeepsPendingEdit() {
        FlightGui state = new FlightGui();
        state.load(reply(0));
        state.choose(1);
        state.load(reply(0));
        assertEquals(1, state.choice());
        assertTrue(state.pending());
    }

    @Test
    public void replyCarriesStoredMode() throws Exception {
        EntityCustomNpc npc = shell();
        npc.setRwFlightMode(1);
        NBTTagCompound data = new NBTTagCompound();
        FlightGui.writeReply(data, npc);
        assertEquals(1, data.getInteger(FlightGui.CHOICE));
        assertFalse(data.hasKey(FlightMode.KEY));
    }

    private static NBTTagCompound reply(int mode) {
        NBTTagCompound data = new NBTTagCompound();
        data.setInteger(FlightGui.CHOICE, mode);
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
