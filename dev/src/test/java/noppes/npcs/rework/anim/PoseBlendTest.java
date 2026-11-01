package noppes.npcs.rework.anim;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/** P7b: pose blending math (Q274, Q282). */
public class PoseBlendTest {
    static Map<String, float[]> pose(Object... pairs) {
        Map<String, float[]> m = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) { m.put((String) pairs[i], (float[]) pairs[i + 1]); }
        return m;
    }
    static float[] rot(float x) { return new float[]{x, 0, 0, 0, 0, 0, 1, 1, 1}; }

    @Test public void weightIsAgePlusFractionOverTicksClamped() {
        assertEquals(0f, PoseBlend.weight(0, 0f, 10), 0f);
        assertEquals(0.05f, PoseBlend.weight(0, 0.5f, 10), 1e-6f);
        assertEquals(0.5f, PoseBlend.weight(5, 0f, 10), 1e-6f);
        assertEquals(1f, PoseBlend.weight(10, 0f, 10), 0f);
        assertEquals(1f, PoseBlend.weight(40, 0.9f, 10), 0f);
        assertEquals(1f, PoseBlend.weight(0, 0f, 0), 0f);
        assertTrue(PoseBlend.validTicks(0) && PoseBlend.validTicks(200));
        assertFalse(PoseBlend.validTicks(-1) || PoseBlend.validTicks(201));
    }

    @Test public void blendIsLinearPerChannelWithBindPoseForMissingBones() {
        float[] shared = rot(40);
        Map<String, float[]> from = pose("head", rot(10), "tail", rot(30));
        Map<String, float[]> built = pose("head", shared, "jaw", rot(20));
        PoseBlend.blend(from, built, 0.25f);
        assertEquals(10 + (40 - 10) * 0.25f, built.get("head")[0], 1e-5f);
        assertEquals(30 * 0.75f, built.get("tail")[0], 1e-5f);    // to bind pose
        assertEquals(20 * 0.25f, built.get("jaw")[0], 1e-5f);     // from bind pose
        assertEquals(1f, built.get("tail")[6], 0f);
        assertEquals("clip arrays are never written", 40f, shared[0], 0f);
    }

    @Test public void endpointsAndDistanceRatio() {
        Map<String, float[]> from = pose("head", rot(10), "body", rot(-20));
        Map<String, float[]> target = pose("head", rot(50), "body", rot(20));
        Map<String, float[]> zero = PoseBlend.copy(target);
        PoseBlend.blend(from, zero, 0f);
        assertEquals(0, PoseBlend.distance(zero, from), 1e-6);
        Map<String, float[]> one = PoseBlend.copy(target);
        PoseBlend.blend(from, one, 1f);
        assertEquals(0, PoseBlend.distance(one, target), 0);
        Map<String, float[]> mid = PoseBlend.copy(target);
        PoseBlend.blend(from, mid, 0.4f);
        double full = PoseBlend.distance(from, target);
        assertEquals(80, full, 1e-6);
        assertEquals(0.6 * full, PoseBlend.distance(mid, target), 1e-4);
        assertEquals(0.4 * full, PoseBlend.distance(mid, from), 1e-4);
    }

    @Test public void propsComeFromTheTargetUnblended() {
        float[] rock = {0, 0, 0, 0, 0, 0, 1, 1, 1};
        Map<String, float[]> from = pose("arm", rot(90), "prop_old", rot(5));
        Map<String, float[]> built = pose("arm", rot(0), "prop_rock", rock);
        PoseBlend.blend(from, built, 0.5f);
        assertSame(rock, built.get("prop_rock"));
        assertFalse("a prop of the old pose is not drawn", built.containsKey("prop_old"));
        assertEquals(45f, built.get("arm")[0], 1e-5f);
        assertEquals(0, PoseBlend.distance(pose("prop_x", rot(9)), pose()), 0);
    }

    @Test public void copyIsDeep() {
        Map<String, float[]> a = pose("head", rot(1));
        Map<String, float[]> b = PoseBlend.copy(a);
        b.get("head")[0] = 7;
        assertEquals(1f, a.get("head")[0], 0f);
    }
}
