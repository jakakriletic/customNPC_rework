package noppes.npcs.rework.rig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A timed event from an .anim.yaml `events:` list.  `type` decides who handles
 * it (see tools/anim/README.md); the remaining keys are read with the typed
 * getters, falling back to the given default.
 */
public final class AnimEvent {
    public final float time;
    public final String type;
    private final JsonObject data;

    AnimEvent(JsonObject data) {
        this.data = data;
        this.time = data.get("t").getAsFloat();
        this.type = data.get("type").getAsString();
    }

    public float getFloat(String key, float fallback) {
        return data.has(key) ? data.get(key).getAsFloat() : fallback;
    }

    public String getString(String key, String fallback) {
        return data.has(key) && data.get(key).isJsonPrimitive() ? data.get(key).getAsString() : fallback;
    }

    public boolean getBool(String key, boolean fallback) {
        return data.has(key) ? data.get(key).getAsBoolean() : fallback;
    }

    /** A list of numbers (e.g. a projectile's baked `origin`); empty if missing. */
    public float[] getFloats(String key) {
        if (!data.has(key) || !data.get(key).isJsonArray()) return new float[0];
        com.google.gson.JsonArray a = data.getAsJsonArray(key);
        float[] out = new float[a.size()];
        for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsFloat();
        return out;
    }

    /** A key that may be one string or a list of strings (e.g. `sound`). */
    public List<String> getStrings(String key) {
        if (!data.has(key)) return Collections.emptyList();
        JsonElement v = data.get(key);
        if (!v.isJsonArray()) return Collections.singletonList(v.getAsString());
        List<String> out = new ArrayList<>();
        for (JsonElement e : v.getAsJsonArray()) out.add(e.getAsString());
        return out;
    }

    @Override
    public String toString() {
        return data.toString();
    }
}
