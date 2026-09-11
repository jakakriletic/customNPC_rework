package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

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
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;
import noppes.npcs.util.NBTJsonUtil;

import org.junit.Test;

/**
 * Differential fuzz over randomly generated NBT structures, seeded so every run is identical.
 *
 * <p>Under {@code testOriginal} this measures how often the October 2019 conversion loses or
 * changes data; a non-zero failure count there is the reproduction. Under {@code test} the
 * patched conversion must not lose anything at all.
 */
public class NbtJsonFuzzTest {

    private static final int ITERATIONS = 1500;

    private static boolean patched() {
        try {
            NBTJsonUtil.class.getField("REWORK_SERIALIZER");
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    @Test
    public void randomStructuresSurviveARoundTrip() {
        int failures = 0;
        StringBuilder firstFailure = new StringBuilder();
        for (int seed = 0; seed < ITERATIONS; seed++) {
            NBTTagCompound in = new Generator(new Random(seed)).compound(0);
            String json;
            try {
                json = NBTJsonUtil.Convert(in);
            } catch (Throwable t) {
                failures++;
                continue;
            }
            try {
                NBTTagCompound out = NBTJsonUtil.Convert(json);
                if (!equalIgnoringNaN(in, out)) {
                    failures++;
                    if (firstFailure.length() == 0) {
                        firstFailure.append("seed ").append(seed).append("\n  in  ").append(in)
                                .append("\n  out ").append(out);
                    }
                }
            } catch (Throwable t) {
                failures++;
                if (firstFailure.length() == 0) {
                    firstFailure.append("seed ").append(seed).append(" threw ").append(t)
                            .append("\n  in  ").append(in);
                }
            }
        }
        if (patched()) {
            assertEquals("Reworked conversion must not lose data. First failure:\n" + firstFailure,
                    0, failures);
        } else {
            assertTrue("The October 2019 conversion was expected to lose data on random structures",
                    failures > 0);
        }
    }

    /** {@code NaN != NaN}, so tag equality cannot be used directly on floating point values. */
    private static boolean equalIgnoringNaN(NBTBase a, NBTBase b) {
        return a.toString().replace("NaN", "#N").equals(b.toString().replace("NaN", "#N"));
    }

    /** Produces homogeneous lists and non-empty keys, matching what real NBT contains. */
    private static final class Generator {
        private final Random random;

        Generator(Random random) {
            this.random = random;
        }

        NBTTagCompound compound(int depth) {
            NBTTagCompound compound = new NBTTagCompound();
            int n = random.nextInt(6);
            for (int i = 0; i < n; i++) {
                compound.setTag(key(), tag(depth));
            }
            return compound;
        }

        private NBTBase tag(int depth) {
            switch (random.nextInt(depth > 3 ? 9 : 13)) {
                case 0:
                    return new NBTTagByte((byte) (random.nextInt(256) - 128));
                case 1:
                    return new NBTTagShort((short) (random.nextInt(65536) - 32768));
                case 2:
                    return new NBTTagInt(random.nextInt());
                case 3:
                    return new NBTTagLong(random.nextLong());
                case 4:
                    return new NBTTagFloat(Float.intBitsToFloat(random.nextInt()));
                case 5:
                    return new NBTTagDouble(Double.longBitsToDouble(random.nextLong()));
                case 6:
                    return new NBTTagString(text());
                case 7: {
                    byte[] data = new byte[random.nextInt(5)];
                    random.nextBytes(data);
                    return new NBTTagByteArray(data);
                }
                case 8: {
                    int[] data = new int[random.nextInt(5)];
                    for (int i = 0; i < data.length; i++) {
                        data[i] = random.nextInt();
                    }
                    return new NBTTagIntArray(data);
                }
                case 9: {
                    long[] data = new long[random.nextInt(5)];
                    for (int i = 0; i < data.length; i++) {
                        data[i] = random.nextLong();
                    }
                    return new NBTTagLongArray(data);
                }
                case 10: {
                    NBTTagList list = new NBTTagList();
                    int n = random.nextInt(4);
                    if (n > 0) {
                        NBTBase first = tag(depth + 1);
                        list.appendTag(first);
                        for (int i = 1; i < n; i++) {
                            NBTBase next = tag(depth + 1);
                            if (next.getId() == first.getId()) {
                                list.appendTag(next);
                            }
                        }
                    }
                    return list;
                }
                default:
                    return compound(depth + 1);
            }
        }

        private String key() {
            String alphabet = "abcXYZ_019";
            int n = 1 + random.nextInt(7);
            StringBuilder sb = new StringBuilder(n);
            for (int i = 0; i < n; i++) {
                sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            return sb.toString();
        }

        private String text() {
            String pool = "abc XYZ019 \\\"{}[]:,\n\tscz";
            int n = random.nextInt(12);
            StringBuilder sb = new StringBuilder(n);
            for (int i = 0; i < n; i++) {
                sb.append(pool.charAt(random.nextInt(pool.length())));
            }
            return sb.toString();
        }
    }
}
