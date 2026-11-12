package noppes.npcs.rework.rig;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import static org.junit.Assert.*;

/** M16: clip parsing, merging an NPC's own clips, the RwRig NBT, the rwrig message; no world. */
public class RigCoreTest {
    private static final String WAVE = "{\"animations\":{\"wave\":{\"animation_length\":1.0,\"loop\":false,"
            + "\"bones\":{\"armL\":{\"rotation\":{\"0.0\":[0,0,0],\"0.5\":{\"vector\":[-150,0,-20],\"easing\":\"easeOutQuad\"},\"1.0\":[0,0,0]}}},"
            + "\"events\":[{\"t\":0.5,\"type\":\"hit\",\"radius\":2},{\"t\":0.2,\"type\":\"sound\",\"sound\":\"troll.roar\"}]}}}";

    @Test public void clipSamplesKeysEasingAndEvents() {
        List<String> errors = new ArrayList<String>();
        AnimLibrary lib = AnimLibrary.merge(AnimLibrary.EMPTY, WAVE, errors);
        assertTrue(errors.toString(), errors.isEmpty());
        AnimClip wave = lib.clipOrNull("wave");
        assertNotNull(wave);
        assertEquals(20, wave.lengthTicks());
        assertEquals(-150, wave.sample(0.5F, new Pose()).get("armL")[Pose.ROT], 1e-4);
        assertEquals(0, wave.sample(1.0F, new Pose()).get("armL")[Pose.ROT], 1e-4);
        float mid = wave.sample(0.25F, new Pose()).get("armL")[Pose.ROT];
        assertTrue("eased out: past half way at half time " + mid, mid < -75);
        List<AnimEvent> fired = new ArrayList<AnimEvent>();
        wave.eventsBetween(-1, 1, fired);
        assertEquals("sorted by time", "sound", fired.get(0).type);
        assertEquals(2, fired.get(1).getFloat("radius", 0), 0);
    }

    @Test public void ownClipsAddToAndReplaceTheModels() {
        List<String> errors = new ArrayList<String>();
        AnimLibrary base = AnimLibrary.merge(AnimLibrary.EMPTY, "{\"animations\":{\"idle\":{\"animation_length\":2},\"wave\":{\"animation_length\":9}}}", errors);
        AnimLibrary merged = AnimLibrary.merge(base, WAVE, errors);
        assertTrue(merged.has("idle"));
        assertEquals("own clip replaces by name", 1.0F, merged.clipOrNull("wave").length, 0);
        assertEquals("base untouched", 9F, base.clipOrNull("wave").length, 0);
        assertEquals("[idle, wave]", merged.sortedClipNames().toString());
    }

    @Test public void badClipsAreReportedNotThrown() {
        List<String> errors = new ArrayList<String>();
        AnimLibrary lib = AnimLibrary.merge(AnimLibrary.EMPTY, "{\"animations\":{\"ok\":{\"animation_length\":1},\"bad\":5,"
                + "\"broken\":{\"bones\":{\"a\":{\"rotation\":{\"x\":[1,2,3]}}}}}}", errors);
        assertTrue(lib.has("ok"));
        assertFalse(lib.has("bad"));
        assertFalse(lib.has("broken"));
        assertEquals(errors.toString(), 2, errors.size());
        errors.clear();
        AnimLibrary.merge(AnimLibrary.EMPTY, "not json {", errors);
        assertEquals(1, errors.size());
    }

    @Test public void rigNbtRoundTripKeepsOnlyWhatIsSet() {
        NBTTagCompound npc = new NBTTagCompound();
        assertFalse(RigData.read(npc).active());
        RigData d = new RigData();
        d.model = "knight"; d.scale = 1.25F; d.clips = WAVE; d.idle = "guard";
        d.write(npc);
        NBTTagCompound t = npc.getCompoundTag("RwRig");
        assertEquals("knight", t.getString("Model"));
        assertFalse("default walk not written", t.hasKey("Walk"));
        RigData back = RigData.read(npc);
        assertTrue(back.sameAs(d));
        assertEquals("guard", back.idle);
        assertEquals("walk", back.walk);
        new RigData().write(npc);
        assertFalse("no rig removes the tag", npc.hasKey("RwRig"));
    }

    @Test public void invalidModelIdsAndScalesAreSanitised() {
        NBTTagCompound npc = new NBTTagCompound();
        NBTTagCompound t = new NBTTagCompound();
        t.setString("Model", "../evil");
        t.setFloat("Scale", 999);
        npc.setTag("RwRig", t);
        RigData d = RigData.read(npc);
        assertFalse(d.active());
        assertEquals(20, d.scale, 0);
        assertTrue(RigModels.validId("skeleton_warrior"));
        assertFalse(RigModels.validId("Troll"));
        assertFalse(RigModels.validId(""));
    }

    @Test public void messageRoundTripAndRejectsGarbage() {
        Object[] m = RigNet.decode(RigNet.encode(42, "leap_slam", 3, 10, 7));
        assertArrayEquals(new Object[] {42, "leap_slam", 3, 10, 7}, m);
        assertNull(RigNet.decode(new byte[] {1, 2}));
        byte[] good = RigNet.encode(1, "x", 0, 0, 0);
        byte[] longer = java.util.Arrays.copyOf(good, good.length + 1);
        assertNull("trailing bytes", RigNet.decode(longer));
    }

    @Test public void olderNewsDoesNotUndoANewerClip() {
        RigPlayer p = new RigPlayer();
        p.accept("slash", 0, 0, 5);
        p.accept("", 0, 0, 4);
        assertEquals("slash", p.clip());
        p.accept("", 0, 0, 6);
        assertFalse(p.playing());
    }

    @Test public void everyBuiltInModelHasItsFilesAndClips() throws Exception {
        for (String id : new String[] {"humanoid", "knight", "skeleton_warrior", "ogre", "troll", "warg", "giant_spider", "cave_bear"}) {
            for (String path : new String[] {"/assets/customnpcs/rig/" + id + ".geo.json", "/assets/customnpcs/rig/" + id + ".animation.json",
                    "/assets/customnpcs/textures/rig/" + id + ".png"}) {
                assertNotNull(path, RigCoreTest.class.getResource(path));
            }
            try (InputStream in = RigCoreTest.class.getResourceAsStream("/assets/customnpcs/rig/" + id + ".animation.json")) {
                JsonObject root = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                List<String> errors = new ArrayList<String>();
                AnimLibrary lib = AnimLibrary.of(root, errors);
                assertTrue(id + " " + errors, errors.isEmpty());
                for (String role : new String[] {"idle", "walk", "hurt", "death"}) { assertTrue(id + " has " + role, lib.has(role)); }
            }
        }
    }
}
