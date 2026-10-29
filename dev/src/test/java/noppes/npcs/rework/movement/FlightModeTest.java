package noppes.npcs.rework.movement;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.util.NBTJsonUtil;
import org.junit.Test;

import static org.junit.Assert.*;

/** Save-format compatibility, not flight-physics certification. */
public class FlightModeTest {
    @Test
    public void savedIdsAreStableAndUnknownIdsStayOriginal() {
        assertEquals(0, FlightMode.ORIGINAL.id());
        assertEquals(1, FlightMode.HOVER.id());
        assertEquals(2, FlightMode.MOMENTUM.id());
        for (int id : new int[] {-1, 3, 255, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            assertSame(FlightMode.ORIGINAL, FlightMode.fromId(id));
        }
    }

    @Test
    public void oldNpcStaysOriginalAndWritingDoesNotAddAKey() {
        NBTTagCompound old = npcData();
        NBTTagCompound before = old.copy();
        assertSame(FlightMode.ORIGINAL, FlightMode.read(old));
        FlightMode.ORIGINAL.write(old);
        assertEquals(before, old);
    }

    @Test
    public void explicitOriginalRemovesAnEarlierSelection() {
        NBTTagCompound data = npcData();
        FlightMode.HOVER.write(data);
        FlightMode.ORIGINAL.write(data);
        assertFalse(data.hasKey(FlightMode.KEY));
        assertEquals(npcData(), data);
    }

    @Test
    public void readsDoNotModifyNbtAndRejectInvalidIds() {
        for (int id : new int[] {0, 1, 2, -1, 3, Integer.MAX_VALUE}) {
            NBTTagCompound data = npcData();
            data.setInteger(FlightMode.KEY, id);
            NBTTagCompound before = data.copy();
            assertSame(FlightMode.fromId(id), FlightMode.read(data));
            assertEquals(before, data);
        }
    }

    @Test
    public void wrongNbtTypesCannotAccidentallySelectFlight() {
        for (NBTBase tag : new NBTBase[] {new NBTTagByte((byte) 1), new NBTTagShort((short) 1),
                new NBTTagLong(4294967297L), new NBTTagDouble(1.5),
                new NBTTagString("1"), new NBTTagCompound(), new NBTTagList()}) {
            NBTTagCompound data = npcData();
            data.setTag(FlightMode.KEY, tag);
            assertSame(FlightMode.ORIGINAL, FlightMode.read(data));
        }
    }

    @Test
    public void everySelectionSurvivesCompressedWorldNbt() throws Exception {
        for (FlightMode mode : FlightMode.values()) {
            NBTTagCompound data = npcData();
            mode.write(data);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            CompressedStreamTools.writeCompressed(data, bytes);
            NBTTagCompound loaded = CompressedStreamTools.readCompressed(
                    new ByteArrayInputStream(bytes.toByteArray()));
            assertEquals(data, loaded);
            assertSame(mode, FlightMode.read(loaded));
        }
    }

    @Test
    public void everySelectionSurvivesCloneJsonAndLeavesMovementTypeAlone() throws Exception {
        for (int movement : new int[] {0, 1, 2}) {
            for (FlightMode mode : FlightMode.values()) {
                NBTTagCompound data = npcData();
                data.setInteger("MovementType", movement);
                mode.write(data);
                NBTTagCompound loaded = NBTJsonUtil.Convert(NBTJsonUtil.Convert(data));
                assertEquals(data, loaded);
                assertEquals(movement, loaded.getInteger("MovementType"));
                assertSame(mode, FlightMode.read(loaded));
            }
        }
    }

    @Test
    public void selectionRequestsRebuildOnlyWhenItChanges() throws Exception {
        // Same constructor-free entity shell as SquadAdapterTest: no world or tick.
        Class<?> type = Class.forName("sun.misc.Unsafe");
        Field field = type.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        EntityCustomNpc npc = (EntityCustomNpc) type.getMethod("allocateInstance", Class.class)
                .invoke(field.get(null), EntityCustomNpc.class);
        assertSame(FlightMode.ORIGINAL, npc.getRwFlightMode());
        npc.setRwFlightMode(0);
        assertFalse(npc.updateAI);
        npc.setRwFlightMode(1);
        assertSame(FlightMode.HOVER, npc.getRwFlightMode());
        assertTrue(npc.updateAI);
        npc.updateAI = false;
        npc.setRwFlightMode(1);
        assertFalse(npc.updateAI);
        npc.setRwFlightMode(2);
        assertSame(FlightMode.MOMENTUM, npc.getRwFlightMode());
        assertTrue(npc.updateAI);
        npc.updateAI = false;
        npc.setRwFlightMode(-1);
        assertSame(FlightMode.ORIGINAL, npc.getRwFlightMode());
        assertTrue(npc.updateAI);
    }

    private static NBTTagCompound npcData() {
        NBTTagCompound data = new NBTTagCompound();
        data.setString("Name", "flight schema fixture");
        data.setInteger("MovementType", 1);
        data.setInteger("RwNavBackend", 1);
        return data;
    }
}
