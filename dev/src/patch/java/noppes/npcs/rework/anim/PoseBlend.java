package noppes.npcs.rework.anim;

import java.util.HashMap;
import java.util.Map;

/** P7b (D-098, Q274, Q282): pure pose math for {@link IAnimator#blendTo}. Poses are the bone maps of
 * customnpcentities 0.7.0 ({@code Pose.bones}: 9 channels rot xyz, pos xyz, scale xyz; a missing bone
 * is the bind pose). No game or optional-mod types. */
public final class PoseBlend {
    /** Longest allowed blend in ticks (10 s). */
    public static final int MAX_TICKS = 200;
    private static final float[] IDENTITY = {0, 0, 0, 0, 0, 0, 1, 1, 1};

    private PoseBlend() { }

    /** Same rule as {@code Pose.isProp}: props are shown or hidden, never interpolated. */
    public static boolean isProp(String bone) { return bone.startsWith("prop_"); }

    public static boolean validTicks(int ticks) { return ticks >= 0 && ticks <= MAX_TICKS; }

    /** Progress of a blend over {@code ticks} at action age {@code age} plus the frame fraction, 0..1. */
    public static float weight(int age, float partial, int ticks) {
        if (ticks <= 0) { return 1; }
        float w = (age + partial) / ticks;
        return w < 0 ? 0 : (w > 1 ? 1 : w);
    }

    /** {@code built = from + (built - from) * w} for every non-prop bone of either pose, bone by bone.
     * Props stay exactly as built (the target layer). New arrays are written, so arrays shared with
     * clip data are never changed. {@code w >= 1} leaves {@code built} untouched. */
    public static void blend(Map<String, float[]> from, Map<String, float[]> built, float w) {
        if (w >= 1) { return; }
        for (String bone : from.keySet()) {
            if (!isProp(bone) && !built.containsKey(bone)) { built.put(bone, IDENTITY.clone()); }
        }
        for (Map.Entry<String, float[]> e : built.entrySet()) {
            if (isProp(e.getKey())) { continue; }
            float[] b = e.getValue();
            float[] a = from.get(e.getKey());
            if (a == null) { a = IDENTITY; }
            float[] out = new float[9];
            for (int i = 0; i < 9; i++) { out[i] = a[i] + (b[i] - a[i]) * w; }
            e.setValue(out);
        }
    }

    /** Deep copy (bone arrays cloned). */
    public static Map<String, float[]> copy(Map<String, float[]> pose) {
        Map<String, float[]> out = new HashMap<>();
        for (Map.Entry<String, float[]> e : pose.entrySet()) { out.put(e.getKey(), e.getValue().clone()); }
        return out;
    }

    /** Sum of absolute channel differences over all non-prop bones of either pose (missing = bind pose). */
    public static double distance(Map<String, float[]> a, Map<String, float[]> b) {
        double sum = 0;
        for (Map.Entry<String, float[]> e : a.entrySet()) {
            if (isProp(e.getKey())) { continue; }
            float[] x = e.getValue(), y = b.get(e.getKey());
            if (y == null) { y = IDENTITY; }
            for (int i = 0; i < 9; i++) { sum += Math.abs(x[i] - y[i]); }
        }
        for (Map.Entry<String, float[]> e : b.entrySet()) {
            if (isProp(e.getKey()) || a.containsKey(e.getKey())) { continue; }
            float[] y = e.getValue();
            for (int i = 0; i < 9; i++) { sum += Math.abs(IDENTITY[i] - y[i]); }
        }
        return sum;
    }
}
