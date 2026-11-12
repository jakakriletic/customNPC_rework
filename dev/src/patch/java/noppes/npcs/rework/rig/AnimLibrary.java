package noppes.npcs.rework.rig;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * M16: the clips of one rigged NPC. A model's own {@code .animation.json} plus the clips an NPC carries
 * in its NBT ({@code RwRig.Clips}), which add to and replace the model's by name. Parsed on both sides:
 * the server times events and lengths, the client samples bone tracks. Never null clips, never throws
 * for a bad NPC file: {@link #parse} reports the error and returns what it could read.
 */
public final class AnimLibrary {
    public static final AnimLibrary EMPTY = new AnimLibrary();
    private final Map<String, AnimClip> clips = new HashMap<String, AnimClip>();
    private List<String> sorted;

    private AnimLibrary() {
    }

    /** A copy of base with the clips of {@code json} ({"animations":{...}} or the map itself) added. */
    public static AnimLibrary merge(AnimLibrary base, String json, List<String> errors) {
        AnimLibrary out = new AnimLibrary();
        if (base != null) { out.clips.putAll(base.clips); }
        if (json == null || json.trim().isEmpty()) { return out; }
        JsonObject root;
        try {
            root = new JsonParser().parse(json).getAsJsonObject();
        } catch (RuntimeException e) {
            errors.add("clips are not a JSON object: " + e.getMessage());
            return out;
        }
        out.add(root, errors);
        return out;
    }

    /** Clips of a parsed animation file ({"animations":{...}}). */
    public static AnimLibrary of(JsonObject root, List<String> errors) {
        AnimLibrary out = new AnimLibrary();
        out.add(root, errors);
        return out;
    }

    private void add(JsonObject root, List<String> errors) {
        JsonObject map = root.has("animations") && root.get("animations").isJsonObject() ? root.getAsJsonObject("animations") : root;
        for (Map.Entry<String, JsonElement> e : map.entrySet()) {
            String name = e.getKey();
            if (!e.getValue().isJsonObject()) {
                if (map != root) { errors.add("clip '" + name + "' is not an object"); }
                continue;
            }
            try {
                clips.put(name, new AnimClip(name, e.getValue().getAsJsonObject()));
            } catch (RuntimeException ex) {
                errors.add("clip '" + name + "': " + ex);
            }
        }
        sorted = null;
    }

    public AnimClip clipOrNull(String name) {
        return name == null ? null : clips.get(name);
    }

    public boolean has(String name) {
        return name != null && clips.containsKey(name);
    }

    public Set<String> clipNames() {
        return Collections.unmodifiableSet(clips.keySet());
    }

    /** Clip names in a fixed order, the same on server and client. */
    public List<String> sortedClipNames() {
        if (sorted == null) {
            List<String> names = new ArrayList<String>(clips.keySet());
            Collections.sort(names);
            sorted = Collections.unmodifiableList(names);
        }
        return sorted;
    }
}
