package noppes.npcs.rework.anim;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.Test;

/** P7c: the external file check accepts all four real files of the pinned jar and names the path of a fault. */
public class AnimFileValidatorTest {
    private static final List<String> NONE = Collections.emptyList();

    private static String valid(String clipBody) { return "{\"animations\":{\"c\":" + clipBody + "}}"; }

    private static void refused(String json, String path) {
        String problem = AnimFileValidator.check(json, NONE);
        assertNotNull(json, problem);
        assertTrue(problem, problem.startsWith(path));
    }

    @Test public void allRealFilesOfThePinnedJarPass() throws Exception {
        for (String path : new String[]{"../../customNPC_entities_mod/build/libs/customnpcentities-0.7.0.jar",
                "../customNPC_entities_mod/build/libs/customnpcentities-0.7.0.jar"}) {
            File jar = new File(path);
            if (!jar.isFile()) { continue; }
            try (ZipFile zip = new ZipFile(jar)) {
                for (String id : new String[]{"cave_bear", "giant_spider", "troll", "warg"}) {
                    ZipEntry e = zip.getEntry("assets/customnpcentities/animations/" + id + ".animation.json");
                    assertNotNull(id, e);
                    try (InputStream in = zip.getInputStream(e)) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        byte[] buf = new byte[8192];
                        for (int n; (n = in.read(buf)) > 0; ) { out.write(buf, 0, n); }
                        assertNull(id, AnimFileValidator.check(new String(out.toByteArray(), StandardCharsets.UTF_8), NONE));
                    }
                }
                return;
            }
        }
        fail("pinned customnpcentities-0.7.0.jar not found next to the rework repository");
    }

    @Test public void minimalClipPasses() {
        assertNull(AnimFileValidator.check(valid("{\"animation_length\":1,\"loop\":true,\"stride\":2,"
                + "\"events\":[{\"t\":0.5,\"type\":\"lunge\",\"forward\":1,\"sound\":[\"a\",\"b\"]}],"
                + "\"bones\":{\"b\":{\"rotation\":{\"0.0\":[1,2,3],\"1.0\":{\"vector\":[0,0,0],\"easing\":\"linear\"}},\"other\":{\"x\":1}}}}"), NONE));
        assertNull("the jar default length", AnimFileValidator.check(valid("{}"), NONE));
    }

    @Test public void structureFaultsNameTheirPath() {
        refused("not json {", "$");
        refused("[]", "$");
        refused("{}", "animations");
        refused("{\"animations\":[]}", "animations");
        refused("{\"animations\":{}}", "animations");
        refused("{\"animations\":{\"bad/name\":{}}}", "animations.bad/name");
        refused("{\"animations\":{\"c\":1}}", "animations.c");
    }

    @Test public void lengthsMustBeFiniteAndAtLeastOneTick() {
        for (String v : new String[]{"-1", "0", "0.01", "\"1\"", "NaN", "1e99", "Infinity", "7200"}) {
            refused(valid("{\"animation_length\":" + v + "}"), "animations.c.animation_length");
        }
        refused(valid("{\"stride\":-1}"), "animations.c.stride");
        refused(valid("{\"loop\":1}"), "animations.c.loop");
    }

    @Test public void eventFaultsWouldOtherwiseThrowInTheTick() {
        refused(valid("{\"events\":{}}"), "animations.c.events");
        refused(valid("{\"events\":[1]}"), "animations.c.events[0]");
        refused(valid("{\"events\":[{\"type\":\"x\"}]}"), "animations.c.events[0].t");
        refused(valid("{\"animation_length\":1,\"events\":[{\"t\":2,\"type\":\"x\"}]}"), "animations.c.events[0].t");
        refused(valid("{\"events\":[{\"t\":0}]}"), "animations.c.events[0].type");
        refused(valid("{\"events\":[{\"t\":0,\"type\":\"hit\",\"damage\":\"abc\"}]}"), "animations.c.events[0].damage");
        refused(valid("{\"events\":[{\"t\":0,\"type\":\"hit\",\"x\":{}}]}"), "animations.c.events[0].x");
    }

    @Test public void trackFaultsWouldOtherwiseThrowInTheParserOrSampler() {
        String bone = "animations.c.bones.b.rotation";
        refused(valid("{\"bones\":{\"b\":{\"rotation\":{}}}}"), bone);
        refused(valid("{\"bones\":{\"b\":{\"rotation\":{\"x\":[1,2,3]}}}}"), bone + ".x");
        refused(valid("{\"bones\":{\"b\":{\"rotation\":{\"-1\":[1,2,3]}}}}"), bone + ".-1");
        refused(valid("{\"bones\":{\"b\":{\"rotation\":{\"0\":[1,2]}}}}"), bone + ".0");
        refused(valid("{\"bones\":{\"b\":{\"rotation\":{\"0\":[1,2,\"a\"]}}}}"), bone + ".0");
        refused(valid("{\"bones\":{\"b\":{\"rotation\":{\"0\":{\"vector\":[1,2,3],\"easing\":1}}}}}"), bone + ".0.easing");
    }

    @Test public void limitsAndRequiredClips() {
        StringBuilder many = new StringBuilder("{\"animations\":{");
        for (int i = 0; i <= AnimFileValidator.MAX_CLIPS; i++) { many.append(i > 0 ? "," : "").append("\"c").append(i).append("\":{}"); }
        refused(many + "}}", "animations");
        String problem = AnimFileValidator.check(valid("{}"), Arrays.asList("swipe"));
        assertTrue(problem, problem.startsWith("animations.swipe: required"));
        assertNull(AnimFileValidator.check(valid("{}"), Arrays.asList("c")));
        StringBuilder deep = new StringBuilder();
        for (int i = 0; i < 200000; i++) { deep.append('['); }
        assertNotNull(AnimFileValidator.check(deep.toString(), NONE));
    }
}
