package noppes.npcs.rework.far;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import org.junit.Test;

/** M5-V.3: opis videza in njegov hash (kloni isti, ime ne vpliva, sprememba videza nov hash). */
public class FarLookTest {
    private static final String CLS = "customnpcs:customnpc";

    /** Spawn NBT kot ga pise {@code writeSpawnData}; {@code reverse} vstavi kljuce v obratnem vrstnem redu. */
    private static NBTTagCompound spawn(String name, String title, boolean reverse) {
        NBTTagCompound c = new NBTTagCompound();
        Runnable[] puts = {
            () -> c.setString("Name", name),
            () -> c.setString("Title", title),
            () -> c.setString("SkinUrl", ""),
            () -> c.setString("Texture", "customnpcs:textures/entity/humanmale/steve.png"),
            () -> c.setString("CloakTexture", ""),
            () -> c.setString("GlowTexture", ""),
            () -> c.setByte("UsingSkinUrl", (byte) 0),
            () -> c.setInteger("Size", 5),
            () -> c.setInteger("ShowName", 0),
            () -> c.setInteger("SkinColor", 0xFFFFFF),
            () -> c.setInteger("NpcVisible", 0),
            () -> c.setBoolean("NoLivingAnimation", false),
            () -> c.setBoolean("IsStatue", false),
            () -> c.setByte("BossBar", (byte) 0),
            () -> c.setInteger("BossColor", 0),
            () -> c.setInteger("MaxHealth", 20),
            () -> c.setTag("Armor", new NBTTagList()),
            () -> c.setTag("Weapons", new NBTTagList()),
            () -> c.setInteger("Speed", 5),
            () -> c.setBoolean("DeadBody", false),
            () -> c.setInteger("StandingState", 0),
            () -> c.setInteger("MovingState", 0),
            () -> c.setInteger("Orientation", 0),
            () -> c.setFloat("PositionXOffset", 5f),
            () -> c.setFloat("PositionYOffset", 5f),
            () -> c.setFloat("PositionZOffset", 5f),
            () -> c.setInteger("Role", 0),
            () -> c.setInteger("Job", 0),
            () -> c.setTag("ModelData", model(0.5f)),
        };
        for (int i = 0; i < puts.length; ++i) {
            puts[reverse ? puts.length - 1 - i : i].run();
        }
        return c;
    }

    private static NBTTagCompound model(float headScale) {
        NBTTagCompound m = new NBTTagCompound();
        NBTTagCompound head = new NBTTagCompound();
        head.setFloat("ScaleX", headScale);
        head.setFloat("ScaleY", 1f);
        m.setTag("HeadConfig", head);
        m.setString("EntityClass", "");
        return m;
    }

    private static long hash(NBTTagCompound spawn) {
        return FarLook.hash(FarLook.describe(CLS, spawn));
    }

    @Test
    public void kloniImajoIstiHash() {
        // Kloni z razlicnim imenom, naslovom in vrstnim redom kljucev (HashMap v NBTTagCompound).
        long a = hash(spawn("Atesson", "kovac", false));
        long b = hash(spawn("Kineth", "", true));
        assertEquals(a, b);
        assertNotEquals(0L, a);
    }

    @Test
    public void imeInNaslovNistaVOpisu() {
        NBTTagCompound d = FarLook.describe(CLS, spawn("Skrivnostni", "Kralj", false));
        assertFalse(d.hasKey("Name"));
        assertFalse(d.hasKey("Title"));
        assertFalse(d.toString().contains("Skrivnostni"));
        assertFalse(d.toString().contains("Kralj"));
        // Tudi ne-vizualni kljuci (statistike, vloge, boss bar) ne.
        for (String k : new String[] {"MaxHealth", "Speed", "Role", "Job", "BossBar", "ShowName", "IsStatue"}) {
            assertFalse(k, d.hasKey(k));
        }
        assertEquals(CLS, d.getString(FarLook.CLS));
        assertTrue(d.hasKey("Texture"));
        assertTrue(d.hasKey("ModelData"));
    }

    @Test
    public void nevizualneSpremembeNeSpremenijoHasha() {
        long a = hash(spawn("A", "", false));
        NBTTagCompound s = spawn("A", "", false);
        s.setInteger("MaxHealth", 500);
        s.setInteger("Role", 6);
        s.setInteger("ShowName", 1);
        s.setByte("BossBar", (byte) 1);
        assertEquals(a, hash(s));
    }

    @Test
    public void spremembaVidezaDaNovHash() {
        long base = hash(spawn("A", "", false));
        NBTTagCompound tex = spawn("A", "", false);
        tex.setString("Texture", "customnpcs:textures/entity/humanfemale/stephanie.png");
        NBTTagCompound size = spawn("A", "", false);
        size.setInteger("Size", 7);
        NBTTagCompound pose = spawn("A", "", false);
        pose.setInteger("StandingState", 2);
        NBTTagCompound mdl = spawn("A", "", false);
        mdl.setTag("ModelData", model(0.75f));
        NBTTagCompound armor = spawn("A", "", false);
        NBTTagList list = new NBTTagList();
        NBTTagCompound item = new NBTTagCompound();
        item.setByte("Slot", (byte) 0);
        item.setString("id", "minecraft:iron_helmet");
        item.setByte("Count", (byte) 1);
        list.appendTag(item);
        armor.setTag("Armor", list);
        long[] hashes = {hash(tex), hash(size), hash(pose), hash(mdl), hash(armor)};
        for (int i = 0; i < hashes.length; ++i) {
            assertNotEquals("sprememba " + i, base, hashes[i]);
            for (int j = i + 1; j < hashes.length; ++j) {
                assertNotEquals(i + "/" + j, hashes[i], hashes[j]);
            }
        }
        assertNotEquals(base, FarLook.hash(FarLook.describe("customnpcs:customnpc64x32", spawn("A", "", false))));
    }

    @Test
    public void skinIgralcaBrezPodpisanihLastnosti() {
        NBTTagCompound a = spawn("A", "", false);
        a.setByte("UsingSkinUrl", (byte) 1);
        NBTTagCompound profile = new NBTTagCompound();
        profile.setString("Name", "Notch");
        a.setTag("SkinUsername", profile);
        NBTTagCompound b = a.copy();
        NBTTagCompound props = new NBTTagCompound();
        NBTTagList tex = new NBTTagList();
        tex.appendTag(new NBTTagString("podpis-s-casom-1234"));
        props.setTag("textures", tex);
        b.getCompoundTag("SkinUsername").setTag("Properties", props);
        assertEquals(hash(a), hash(b));
        assertFalse(FarLook.describe(CLS, b).getCompoundTag("SkinUsername").hasKey("Properties"));
        NBTTagCompound c = a.copy();
        c.getCompoundTag("SkinUsername").setString("Name", "jeb_");
        assertNotEquals(hash(a), hash(c));
        // Brez UsingSkinUrl == 1 se profil ne uporablja in ni v opisu.
        NBTTagCompound d = a.copy();
        d.setByte("UsingSkinUrl", (byte) 0);
        assertFalse(FarLook.describe(CLS, d).hasKey("SkinUsername"));
    }

    @Test
    public void gzipPovratnaPotOhraniHash() throws Exception {
        NBTTagCompound d = FarLook.describe(CLS, spawn("A", "", false));
        byte[] data = FarLook.encode(d);
        assertTrue("bajtov " + data.length, data.length < 1024);
        NBTTagCompound back = FarLook.decode(data);
        assertEquals(d, back);
        assertEquals(FarLook.hash(d), FarLook.hash(back));
        // Isti opis da iste bajte (GZIP glava brez casa): odgovori so ponovljivi.
        assertTrue(java.util.Arrays.equals(data, FarLook.encode(back)));
    }
}
