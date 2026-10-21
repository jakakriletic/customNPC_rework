package noppes.npcs.rework.far;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

/**
 * M5-V.3: opis videza NPC-ja za nadomestke na daljavo (raziskava M5-V.0 §5.2, D-045).
 *
 * <p>Opis je podmnozica spawn NBT ({@code EntityNPCInterface.writeSpawnData}) s kljuci, ki dolocajo
 * izris: ime registra razreda ({@link #CLS}), iz {@code display} skin/tekstura/plasc/sij, velikost, barva
 * koze in {@code NoLivingAnimation}, nato {@code Armor}, {@code Weapons}, {@code StandingState},
 * {@code Orientation}, odmiki telesa in {@code ModelData}. <b>Brez imena in naslova</b> (oddaljen NPC
 * ne razkrije zgodbe) in brez jobov/vlog, statistik ali hitboxa. {@code SkinUsername} je v opisu samo pri
 * {@code UsingSkinUrl == 1} in brez {@code Properties} (podpisane teksture Mojanga nosijo cas, ki bi
 * klonom dal razlicen hash). Pol-vidnost nosi zastavica vnosa ({@link FarCodec#F_HALF_VISIBLE}).
 *
 * <p>Id videza je 64-bitni {@link #hash} kanonicne oblike opisa (kljuci sestavljenih tagov urejeni, zato
 * vrstni red vstavljanja ne vpliva): kloni imajo isti hash. Brez sveta in entitet, testljivo z JUnit.
 */
public final class FarLook {
    /** Kljuc imena registra razreda entitete v opisu. */
    public static final String CLS = "Cls";
    /** Kljuci iz {@code display}, ki vplivajo na izris. */
    static final String[] DISPLAY_KEYS = {"SkinUrl", "Texture", "CloakTexture", "GlowTexture", "UsingSkinUrl",
        "Size", "SkinColor", "NoLivingAnimation"};
    /** Ostali kljuci spawn NBT, ki vplivajo na izris. */
    static final String[] SPAWN_KEYS = {"Armor", "Weapons", "StandingState", "Orientation", "PositionXOffset",
        "PositionYOffset", "PositionZOffset", "ModelData"};
    /** Najvec bajtov stisnjenega opisa (vecji videz ostane neznan, 0). */
    public static final int MAX_BYTES = 65536;
    /** Meja NBTSizeTracker pri branju (kot vanilla {@code PacketBuffer.readCompoundTag}). */
    static final long MAX_NBT = 2097152L;

    private static final long FNV_OFFSET = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    private FarLook() {
    }

    /** Opis videza iz spawn NBT; {@code entityKey} je ime registra razreda ({@code EntityList.getKey}). */
    public static NBTTagCompound describe(String entityKey, NBTTagCompound spawn) {
        NBTTagCompound out = new NBTTagCompound();
        out.setString(CLS, entityKey == null ? "" : entityKey);
        for (String k : DISPLAY_KEYS) {
            copy(spawn, out, k);
        }
        if (spawn.getByte("UsingSkinUrl") == 1 && spawn.hasKey("SkinUsername", 10)) {
            NBTTagCompound profile = spawn.getCompoundTag("SkinUsername").copy();
            profile.removeTag("Properties");
            out.setTag("SkinUsername", profile);
        }
        for (String k : SPAWN_KEYS) {
            copy(spawn, out, k);
        }
        return out;
    }

    private static void copy(NBTTagCompound from, NBTTagCompound to, String key) {
        NBTBase tag = from.getTag(key);
        if (tag != null) {
            to.setTag(key, tag.copy());
        }
    }

    /** 64-bitni hash kanonicne oblike (FNV-1a + koncno mesanje); nikoli 0 (0 = neznan videz). */
    public static long hash(NBTTagCompound description) {
        long[] h = {FNV_OFFSET};
        feed(h, description);
        long x = h[0];
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;
        return x == 0L ? 1L : x;
    }

    private static void feed(long[] h, NBTBase tag) {
        int id = tag.getId();
        b(h, id);
        switch (id) {
            case 1:
            case 2:
            case 3:
            case 4:
                l(h, ((NBTPrimitive) tag).getLong());
                break;
            case 5:
                i(h, Float.floatToIntBits(((NBTPrimitive) tag).getFloat()));
                break;
            case 6:
                l(h, Double.doubleToLongBits(((NBTPrimitive) tag).getDouble()));
                break;
            case 7: {
                byte[] a = ((NBTTagByteArray) tag).getByteArray();
                i(h, a.length);
                for (byte v : a) {
                    b(h, v);
                }
                break;
            }
            case 8:
                s(h, ((NBTTagString) tag).getString());
                break;
            case 9: {
                NBTTagList list = (NBTTagList) tag;
                i(h, list.getTagType());
                i(h, list.tagCount());
                for (int k = 0; k < list.tagCount(); ++k) {
                    feed(h, list.get(k));
                }
                break;
            }
            case 10: {
                NBTTagCompound c = (NBTTagCompound) tag;
                List<String> keys = new ArrayList<String>(c.getKeySet());
                Collections.sort(keys);
                i(h, keys.size());
                for (String k : keys) {
                    s(h, k);
                    feed(h, c.getTag(k));
                }
                break;
            }
            case 11: {
                int[] a = ((NBTTagIntArray) tag).getIntArray();
                i(h, a.length);
                for (int v : a) {
                    i(h, v);
                }
                break;
            }
            default:
                s(h, tag.toString());
                break;
        }
    }

    private static void b(long[] h, int v) {
        h[0] = (h[0] ^ (v & 0xFF)) * FNV_PRIME;
    }

    private static void i(long[] h, int v) {
        b(h, v);
        b(h, v >>> 8);
        b(h, v >>> 16);
        b(h, v >>> 24);
    }

    private static void l(long[] h, long v) {
        i(h, (int) v);
        i(h, (int) (v >>> 32));
    }

    private static void s(long[] h, String v) {
        byte[] u = v.getBytes(StandardCharsets.UTF_8);
        i(h, u.length);
        for (byte x : u) {
            b(h, x);
        }
    }

    /** GZIP oblika opisa za odgovor {@code LOOK}. */
    public static byte[] encode(NBTTagCompound description) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(512);
        DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bytes));
        try {
            CompressedStreamTools.write(description, out);
        } finally {
            out.close();
        }
        return bytes.toByteArray();
    }

    /** Razbere {@link #encode} z mejo {@link #MAX_NBT} (NBTSizeTracker). */
    public static NBTTagCompound decode(byte[] data) throws IOException {
        DataInputStream in = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(data)));
        try {
            return CompressedStreamTools.read(in, new NBTSizeTracker(MAX_NBT));
        } finally {
            in.close();
        }
    }
}
