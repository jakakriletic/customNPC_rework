package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.nbt.NBTTagString;
import noppes.npcs.util.NBTJsonUtil;

import org.junit.Test;

/**
 * Characterization of the NBT&lt;-&gt;JSON conversion used for every clone, dialog, quest,
 * player and script file.
 *
 * <p>The same class runs under two Gradle tasks:
 * <ul>
 *   <li>{@code testOriginal} runs against the untouched October 2019 binary, where these tests
 *       assert the original broken behaviour. That is the reproduction of the bug.</li>
 *   <li>{@code test} runs against the restored and patched classes, where the same tests assert
 *       the fixed behaviour.</li>
 * </ul>
 * Which set of expectations applies is decided by {@link #patched()}, so both tasks stay green
 * and the difference between the two builds stays visible in one place.
 */
public class NbtJsonBaselineTest {

    /** True when the reworked serializer is on the classpath. */
    private static boolean patched() {
        try {
            NBTJsonUtil.class.getField("REWORK_SERIALIZER");
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    private static NBTTagCompound one(String key, NBTBase value) {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setTag(key, value);
        return compound;
    }

    private static NBTTagCompound roundTrip(NBTTagCompound in) throws Exception {
        return NBTJsonUtil.Convert(NBTJsonUtil.Convert(in));
    }

    // ------------------------------------------------------------ data types

    @Test
    public void longArrayValuesSurviveRoundTrip() throws Exception {
        NBTTagCompound in = one("v", new NBTTagLongArray(new long[] { 1L, 300L, 1234567890123L }));
        NBTTagCompound out = roundTrip(in);
        if (patched()) {
            assertEquals(in, out);
        } else {
            // Original read long arrays through getByte(), keeping only the low 8 bits.
            assertEquals("Known original bug, NOT the desired behaviour", "{v:[L;1L,44L,-53L]}", out.toString());
        }
    }

    @Test
    public void emptyTypedArraysKeepTheirTagType() throws Exception {
        NBTTagCompound in = one("v", new NBTTagByteArray(new byte[0]));
        NBTTagCompound out = roundTrip(in);
        if (patched()) {
            assertEquals(7, out.getTag("v").getId());
        } else {
            assertEquals("Known original bug: empty byte array became an empty list", 9, out.getTag("v").getId());
        }
    }

    @Test
    public void emptyIntArrayKeepsItsTagType() throws Exception {
        NBTTagCompound out = roundTrip(one("v", new NBTTagIntArray(new int[0])));
        assertEquals(patched() ? 11 : 9, out.getTag("v").getId());
    }

    @Test
    public void listOfIntsStaysAList() throws Exception {
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagInt(1));
        list.appendTag(new NBTTagInt(2));
        NBTTagCompound out = roundTrip(one("v", list));
        if (patched()) {
            assertEquals(9, out.getTag("v").getId());
            assertEquals(2, out.getTagList("v", 3).tagCount());
        } else {
            // Original coerced it into an int array. Reading it back as a list then yields
            // nothing, which is how a saved setting silently reverts to its default.
            assertEquals("Known original bug: list of ints became an int array", 11, out.getTag("v").getId());
            assertEquals(0, out.getTagList("v", 3).tagCount());
        }
    }

    @Test
    public void listOfBytesStaysAList() throws Exception {
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagByte((byte) 1));
        list.appendTag(new NBTTagByte((byte) 2));
        NBTTagCompound out = roundTrip(one("v", list));
        assertEquals(patched() ? 9 : 7, out.getTag("v").getId());
    }

    @Test
    public void listOfLongsStaysAList() throws Exception {
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagLong(1L));
        list.appendTag(new NBTTagLong(2L));
        NBTTagCompound out = roundTrip(one("v", list));
        assertEquals(patched() ? 9 : 12, out.getTag("v").getId());
    }

    // --------------------------------------------------------------- strings

    @Test
    public void leadingWhitespaceInStringsSurvives() throws Exception {
        NBTTagCompound out = roundTrip(one("v", new NBTTagString("  abc")));
        assertEquals(patched() ? "  abc" : "abc", out.getString("v"));
    }

    @Test
    public void aStringOfOnlySpacesSurvives() throws Exception {
        NBTTagCompound out = roundTrip(one("v", new NBTTagString(" ")));
        assertEquals(patched() ? " " : "", out.getString("v"));
    }

    @Test
    public void aStringEndingInABackslashIsReadable() throws Exception {
        NBTTagCompound in = one("v", new NBTTagString("path\\"));
        if (patched()) {
            assertEquals(in, roundTrip(in));
        } else {
            try {
                roundTrip(in);
                fail("Original was expected to fail on a string ending in a backslash");
            } catch (StringIndexOutOfBoundsException expected) {
                // Known original bug: the parser runs past the end and the file cannot be read.
            }
        }
    }

    @Test
    public void interiorEscapesStillSurvive() throws Exception {
        NBTTagCompound in = new NBTTagCompound();
        in.setString("quote", "he said \"hi\"");
        in.setString("backslash", "a\\b");
        in.setString("newline", "line1\nline2");
        in.setString("unicode", "ščž");
        assertEquals("Behaviour that was already correct must not regress", in, roundTrip(in));
    }

    // ---------------------------------------------------------------- numbers

    @Test
    public void notANumberIsReadable() throws Exception {
        NBTTagCompound in = one("v", new NBTTagDouble(Double.NaN));
        if (patched()) {
            assertTrue(Double.isNaN(roundTrip(in).getDouble("v")));
        } else {
            try {
                roundTrip(in);
                fail("Original was expected to fail on NaN");
            } catch (NBTJsonUtil.JsonException expected) {
                // Known original bug: the value was lower-cased before parsing.
            }
        }
    }

    @Test
    public void infinityIsReadable() throws Exception {
        NBTTagCompound in = one("v", new NBTTagFloat(Float.POSITIVE_INFINITY));
        if (patched()) {
            assertEquals(Float.POSITIVE_INFINITY, roundTrip(in).getFloat("v"), 0.0f);
        } else {
            try {
                roundTrip(in);
                fail("Original was expected to fail on Infinity");
            } catch (NBTJsonUtil.JsonException expected) {
                // Known original bug.
            }
        }
    }

    @Test
    public void ordinaryNumbersStillSurvive() throws Exception {
        NBTTagCompound in = new NBTTagCompound();
        in.setByte("b", (byte) -128);
        in.setShort("s", Short.MIN_VALUE);
        in.setInteger("i", Integer.MIN_VALUE);
        in.setLong("l", Long.MIN_VALUE);
        in.setFloat("f", 1.5f);
        in.setDouble("d", 1.0e20);
        in.setBoolean("flag", true);
        assertEquals("Behaviour that was already correct must not regress", in, roundTrip(in));
    }

    // -------------------------------------------------------------- structure

    @Test
    public void largeCompoundsDoNotOverflowTheStack() throws Exception {
        NBTTagCompound in = new NBTTagCompound();
        for (int i = 0; i < 8000; i++) {
            in.setInteger("k" + i, i);
        }
        if (patched()) {
            assertEquals(in, roundTrip(in));
        } else {
            try {
                roundTrip(in);
                fail("Original was expected to overflow the stack");
            } catch (StackOverflowError expected) {
                // Known original bug: the parser recursed once per key.
            }
        }
    }

    @Test
    public void anEmptyKeyIsReadable() throws Exception {
        NBTTagCompound in = one("", new NBTTagString("x"));
        if (patched()) {
            assertEquals(in, roundTrip(in));
        } else {
            try {
                roundTrip(in);
                fail("Original was expected to reject an empty key");
            } catch (NBTJsonUtil.JsonException expected) {
                // Known original bug.
            }
        }
    }

    @Test
    public void realisticNpcDataStillSurvives() throws Exception {
        NBTTagCompound npc = new NBTTagCompound();
        npc.setString("Name", "Konjenik");
        npc.setInteger("ModRev", 1);
        npc.setBoolean("MovingPathPause", false);
        npc.setFloat("Health", 20.0f);
        npc.setDouble("StartPosX", 100.5);
        npc.setString("id", "customnpcs:customnpc");
        NBTTagCompound ai = new NBTTagCompound();
        ai.setInteger("MovingType", 1);
        ai.setInteger("StandingType", 0);
        ai.setBoolean("ReturnToStart", true);
        npc.setTag("TransformAI", ai);
        NBTTagList inventory = new NBTTagList();
        NBTTagCompound slot = new NBTTagCompound();
        slot.setInteger("Slot", 0);
        slot.setString("id", "minecraft:diamond_sword");
        inventory.appendTag(slot);
        npc.setTag("Inventory", inventory);
        assertEquals("Behaviour that was already correct must not regress", npc, roundTrip(npc));
    }

    // ------------------------------------------------------------ file format

    @Test
    public void writtenTextIsUnchangedFromTheOriginalFormat() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setString("Name", "Guard");
        NBTTagCompound inner = new NBTTagCompound();
        inner.setInteger("Role", 2);
        compound.setTag("Advanced", inner);
        String json = NBTJsonUtil.Convert(compound);
        assertTrue("must start with {", json.startsWith("{\n"));
        assertTrue("must end with }", json.endsWith("}\n"));
        assertTrue("keys are quoted and indented", json.contains("\n    \"Name\": \"Guard\""));
        assertTrue("nested compounds are indented one more level", json.contains("\n        \"Role\": 2\n"));
        assertFalse("no trailing comma before a closing brace", json.contains(",\n    }"));
    }
}
