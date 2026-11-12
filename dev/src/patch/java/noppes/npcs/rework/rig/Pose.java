package noppes.npcs.rework.rig;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-bone channel values in FILE units (as written in .animation.json):
 * rotation degrees xyz, position pixels xyz, scale xyz.  Bones that are not in
 * the map are in their bind pose.
 */
public final class Pose {
    public static final int ROT = 0, POS = 3, SCALE = 6;
    private static final float[] IDENTITY = {0, 0, 0, 0, 0, 0, 1, 1, 1};

    public final Map<String, float[]> bones = new HashMap<>();

    public float[] get(String bone) {
        float[] v = bones.get(bone);
        return v != null ? v : IDENTITY;
    }

    public boolean has(String bone) {
        return bones.containsKey(bone);
    }

    /** Bones named prop_* (a held rock ...) are drawn only while a clip gives them a scale. */
    public static boolean isProp(String bone) {
        return bone.startsWith("prop_");
    }

    public float[] edit(String bone) {
        return bones.computeIfAbsent(bone, b -> IDENTITY.clone());
    }

    /** this = lerp(this, other, w), bone by bone. */
    public Pose blend(Pose other, float w) {
        if (w <= 0) return this;
        for (String bone : other.bones.keySet()) bones.computeIfAbsent(bone, b -> IDENTITY.clone());
        for (Map.Entry<String, float[]> e : bones.entrySet()) {
            float[] a = e.getValue();
            float[] b = other.get(e.getKey());
            for (int i = 0; i < 9; i++) a[i] += (b[i] - a[i]) * w;
        }
        return this;
    }
}
