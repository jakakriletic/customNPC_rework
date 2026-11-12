package noppes.npcs.rework.rig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * One clip of an .animation.json written by tools/anim/compile.py.
 * Loaded on both sides: the server reads `length` and `events` to time the
 * attacks, the client also samples the bone tracks.
 */
public final class AnimClip {
    public final String name;
    public final float length;
    public final boolean loop;
    /** Blocks travelled per full cycle for walk clips (0 = not distance driven). */
    public final float stride;
    public final List<AnimEvent> events;
    private final List<Track> tracks = new ArrayList<>();

    AnimClip(String name, JsonObject json) {
        this.name = name;
        this.length = json.has("animation_length") ? json.get("animation_length").getAsFloat() : 1;
        this.loop = json.has("loop") && json.get("loop").getAsBoolean();
        this.stride = json.has("stride") ? json.get("stride").getAsFloat() : 0;
        List<AnimEvent> list = new ArrayList<>();
        if (json.has("events")) {
            for (JsonElement e : json.getAsJsonArray("events")) list.add(new AnimEvent(e.getAsJsonObject()));
        }
        list.sort((a, b) -> Float.compare(a.time, b.time));
        this.events = Collections.unmodifiableList(list);
        if (json.has("bones")) {
            for (Map.Entry<String, JsonElement> bone : json.getAsJsonObject("bones").entrySet()) {
                for (Map.Entry<String, JsonElement> ch : bone.getValue().getAsJsonObject().entrySet()) {
                    int offset = ch.getKey().equals("rotation") ? Pose.ROT
                            : ch.getKey().equals("position") ? Pose.POS
                            : ch.getKey().equals("scale") ? Pose.SCALE : -1;
                    if (offset >= 0) tracks.add(new Track(bone.getKey(), offset, ch.getValue().getAsJsonObject()));
                }
            }
        }
    }

    /** Names of the bones this clip moves. */
    public java.util.Set<String> bones() {
        java.util.Set<String> out = new java.util.TreeSet<>();
        for (Track t : tracks) out.add(t.bone);
        return out;
    }

    public int lengthTicks() {
        return Math.round(length * 20);
    }

    /** Writes this clip's values at time t (seconds) into the pose. */
    public Pose sample(float t, Pose out) {
        if (loop && length > 0) {
            t %= length;
            if (t < 0) t += length;
        }
        for (Track track : tracks) track.sample(t, out.edit(track.bone));
        return out;
    }

    /** Events with from < time <= to (a loop wraps around). */
    public void eventsBetween(float from, float to, List<AnimEvent> out) {
        if (to <= from) return;
        if (loop && length > 0 && to - from < length) {
            float a = from % length, b = to % length;
            if (a < 0) a += length;
            if (b < 0) b += length;
            if (a <= b) collect(a, b, out);
            else {
                collect(a, length, out);
                collect(-1, b, out);
            }
            return;
        }
        collect(from, to, out);
    }

    private void collect(float from, float to, List<AnimEvent> out) {
        for (AnimEvent e : events) {
            if (e.time > from && e.time <= to) out.add(e);
        }
    }

    private static final class Track {
        final String bone;
        final int offset;
        final float[] times;
        final float[][] values;
        final String[] easing;

        Track(String bone, int offset, JsonObject keys) {
            this.bone = bone;
            this.offset = offset;
            List<Map.Entry<String, JsonElement>> entries = new ArrayList<>(keys.entrySet());
            entries.sort((a, b) -> Float.compare(Float.parseFloat(a.getKey()), Float.parseFloat(b.getKey())));
            times = new float[entries.size()];
            values = new float[entries.size()][3];
            easing = new String[entries.size()];
            for (int i = 0; i < entries.size(); i++) {
                times[i] = Float.parseFloat(entries.get(i).getKey());
                JsonElement v = entries.get(i).getValue();
                JsonElement vec = v.isJsonObject() ? v.getAsJsonObject().get("vector") : v;
                for (int a = 0; a < 3; a++) values[i][a] = vec.getAsJsonArray().get(a).getAsFloat();
                if (v.isJsonObject() && v.getAsJsonObject().has("easing")) {
                    easing[i] = v.getAsJsonObject().get("easing").getAsString();
                }
            }
        }

        void sample(float t, float[] out) {
            int n = times.length;
            float[] v;
            if (t <= times[0]) v = values[0];
            else if (t >= times[n - 1]) v = values[n - 1];
            else {
                int i = 1;
                while (times[i] < t) i++;
                float span = times[i] - times[i - 1];
                float k = span <= 0 ? 1 : Easing.apply(easing[i], (t - times[i - 1]) / span);
                float[] a = values[i - 1], b = values[i];
                out[offset] = a[0] + (b[0] - a[0]) * k;
                out[offset + 1] = a[1] + (b[1] - a[1]) * k;
                out[offset + 2] = a[2] + (b[2] - a[2]) * k;
                return;
            }
            out[offset] = v[0];
            out[offset + 1] = v[1];
            out[offset + 2] = v[2];
        }
    }
}
