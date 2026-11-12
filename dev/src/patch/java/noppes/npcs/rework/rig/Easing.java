package noppes.npcs.rework.rig;

/** Keyframe easing, identical to tools/anim/rig.py ease(). */
public final class Easing {
    private Easing() {
    }

    public static float apply(String name, float t) {
        if (name == null || name.equals("linear")) return t;
        if (name.equals("step")) return t < 1 ? 0 : 1;
        if (name.startsWith("easeInOut")) {
            String fn = name.substring(9);
            return t < 0.5F ? in(fn, 2 * t) / 2 : 1 - in(fn, 2 - 2 * t) / 2;
        }
        if (name.startsWith("easeIn")) return in(name.substring(6), t);
        if (name.startsWith("easeOut")) return 1 - in(name.substring(7), 1 - t);
        return t;
    }

    private static float in(String fn, float t) {
        switch (fn) {
            case "Sine": return (float) (1 - Math.cos(t * Math.PI / 2));
            case "Quad": return t * t;
            case "Cubic": return t * t * t;
            case "Quart": return t * t * t * t;
            case "Expo": return t == 0 ? 0 : (float) Math.pow(2, 10 * (t - 1));
            case "Back": {
                float k = 1.70158F;
                return t * t * ((k + 1) * t - k);
            }
            case "Elastic":
                if (t == 0 || t == 1) return t;
                return (float) (-Math.pow(2, 10 * (t - 1)) * Math.sin((t - 1.075) * (2 * Math.PI) / 0.3));
            case "Bounce": return 1 - bounceOut(1 - t);
            default: return t;
        }
    }

    private static float bounceOut(float t) {
        if (t < 1 / 2.75F) return 7.5625F * t * t;
        if (t < 2 / 2.75F) { t -= 1.5F / 2.75F; return 7.5625F * t * t + 0.75F; }
        if (t < 2.5 / 2.75F) { t -= 2.25F / 2.75F; return 7.5625F * t * t + 0.9375F; }
        t -= 2.625F / 2.75F;
        return 7.5625F * t * t + 0.984375F;
    }
}
