package noppes.npcs.rework.anim;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** P7c (D-098, Q275): full check of an external {@code <id>.animation.json} before anything is published,
 * on the server and again on each client. Everything the optional mod's parser and tick code would
 * throw on later is refused here, with the JSON path. Only {@code animations} is read; {@code mob} stays
 * from the jar. No Minecraft or optional-mod types. */
public final class AnimFileValidator {
    /** The optional mod encodes free clips as 128 + sorted index in one byte. */
    public static final int MAX_CLIPS = 128;
    static final int MAX_EVENTS = 256, MAX_BONES = 256, MAX_KEYS = 4096;
    static final float MAX_LENGTH = 3600;
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_.:-]{1,64}");
    /** Event keys customnpcentities 0.7.0 reads with getFloat, which throws on anything but a number. */
    private static final Set<String> NUMERIC = new HashSet<>(Arrays.asList("RockDamage", "RockEffectSeconds", "RockKnockback",
            "RockLift", "RockRadius", "RockShake", "RockSize", "above", "arc", "below", "count", "damage", "dir", "duration",
            "effect_level", "effect_seconds", "falloff", "fire", "forward", "inaccuracy", "knockback", "lead", "lift", "max_alive",
            "pitch", "radius", "shake", "side", "size", "speed", "strength", "ticks", "up", "volume"));

    private AnimFileValidator() { }

    /** @return null when valid, otherwise {@code <json path>: <reason>}
     * @param required clips the mob attack table of this entity names; they cannot be removed */
    public static String check(String text, Collection<String> required) {
        try {
            JsonElement root = new JsonParser().parse(text);
            if (!root.isJsonObject()) { return "$: not an object"; }
            JsonElement all = root.getAsJsonObject().get("animations");
            if (all == null || !all.isJsonObject()) { return "animations: missing object"; }
            Set<Map.Entry<String, JsonElement>> clips = all.getAsJsonObject().entrySet();
            if (clips.isEmpty()) { return "animations: no clips"; }
            if (clips.size() > MAX_CLIPS) { return "animations: more than " + MAX_CLIPS + " clips"; }
            for (String name : required) {
                if (!all.getAsJsonObject().has(name)) { return "animations." + name + ": required by the mob attack table"; }
            }
            for (Map.Entry<String, JsonElement> clip : clips) {
                String path = "animations." + clip.getKey();
                if (!NAME.matcher(clip.getKey()).matches()) { return path + ": bad clip name"; }
                String problem = clip.getValue().isJsonObject() ? clip(path, clip.getValue().getAsJsonObject()) : path + ": not an object";
                if (problem != null) { return problem; }
            }
            return null;
        } catch (JsonParseException | IllegalStateException | NumberFormatException failure) {
            return "$: " + failure.getMessage();
        } catch (StackOverflowError nested) {
            return "$: nested too deeply";
        }
    }

    private static String clip(String path, JsonObject clip) {
        float length = 1;
        if (clip.has("animation_length")) {
            if (!finite(clip.get("animation_length"))) { return path + ".animation_length: not a finite number"; }
            length = clip.get("animation_length").getAsFloat();
            // The optional mod ticks clips by Math.round(length * 20), which must stay at least 1.
            if (Math.round(length * 20) < 1 || length > MAX_LENGTH) { return path + ".animation_length: out of range"; }
        }
        if (clip.has("loop") && !(clip.get("loop").isJsonPrimitive() && clip.get("loop").getAsJsonPrimitive().isBoolean())) {
            return path + ".loop: not a boolean";
        }
        if (clip.has("stride") && (!finite(clip.get("stride")) || clip.get("stride").getAsFloat() < 0)) {
            return path + ".stride: not a non-negative number";
        }
        if (clip.has("events")) {
            if (!clip.get("events").isJsonArray()) { return path + ".events: not an array"; }
            if (clip.getAsJsonArray("events").size() > MAX_EVENTS) { return path + ".events: more than " + MAX_EVENTS; }
            int i = 0;
            for (JsonElement event : clip.getAsJsonArray("events")) {
                String at = path + ".events[" + i++ + "]";
                String problem = event.isJsonObject() ? event(at, event.getAsJsonObject(), length) : at + ": not an object";
                if (problem != null) { return problem; }
            }
        }
        if (clip.has("bones")) {
            if (!clip.get("bones").isJsonObject()) { return path + ".bones: not an object"; }
            Set<Map.Entry<String, JsonElement>> bones = clip.getAsJsonObject("bones").entrySet();
            if (bones.size() > MAX_BONES) { return path + ".bones: more than " + MAX_BONES; }
            for (Map.Entry<String, JsonElement> bone : bones) {
                String at = path + ".bones." + bone.getKey();
                if (!NAME.matcher(bone.getKey()).matches()) { return at + ": bad bone name"; }
                if (!bone.getValue().isJsonObject()) { return at + ": not an object"; }
                for (Map.Entry<String, JsonElement> channel : bone.getValue().getAsJsonObject().entrySet()) {
                    String key = channel.getKey();
                    if (key.equals("rotation") || key.equals("position") || key.equals("scale")) {
                        String problem = track(at + "." + key, channel.getValue());
                        if (problem != null) { return problem; }
                    }
                }
            }
        }
        return null;
    }

    private static String event(String path, JsonObject event, float length) {
        if (!finite(event.get("t"))) { return path + ".t: not a finite number"; }
        float t = event.get("t").getAsFloat();
        if (t < 0 || t > length) { return path + ".t: outside the clip"; }
        JsonElement type = event.get("type");
        if (type == null || !type.isJsonPrimitive() || !type.getAsJsonPrimitive().isString() || type.getAsString().isEmpty()) {
            return path + ".type: not a string";
        }
        for (Map.Entry<String, JsonElement> e : event.entrySet()) {
            JsonElement value = e.getValue();
            if (NUMERIC.contains(e.getKey()) && !finite(value)) { return path + "." + e.getKey() + ": not a finite number"; }
            if (value.isJsonObject() || value.isJsonNull()) { return path + "." + e.getKey() + ": unsupported value"; }
            if (value.isJsonArray()) {
                for (JsonElement item : value.getAsJsonArray()) {
                    if (!item.isJsonPrimitive()) { return path + "." + e.getKey() + ": unsupported array item"; }
                }
            }
        }
        return null;
    }

    private static String track(String path, JsonElement track) {
        if (!track.isJsonObject()) { return path + ": not an object"; }
        Set<Map.Entry<String, JsonElement>> keys = track.getAsJsonObject().entrySet();
        if (keys.isEmpty()) { return path + ": no keyframes"; }
        if (keys.size() > MAX_KEYS) { return path + ": more than " + MAX_KEYS + " keyframes"; }
        for (Map.Entry<String, JsonElement> key : keys) {
            String at = path + "." + key.getKey();
            float time;
            try { time = Float.parseFloat(key.getKey()); }
            catch (NumberFormatException notTime) { return at + ": keyframe time is no number"; }
            if (Float.isNaN(time) || Float.isInfinite(time) || time < 0) { return at + ": keyframe time out of range"; }
            JsonElement vector = key.getValue().isJsonObject() ? key.getValue().getAsJsonObject().get("vector") : key.getValue();
            if (vector == null || !vector.isJsonArray() || vector.getAsJsonArray().size() != 3) { return at + ": not a vector of 3"; }
            for (JsonElement v : vector.getAsJsonArray()) {
                if (!finite(v)) { return at + ": vector value is not a finite number"; }
            }
            if (key.getValue().isJsonObject() && key.getValue().getAsJsonObject().has("easing")) {
                JsonElement easing = key.getValue().getAsJsonObject().get("easing");
                if (!easing.isJsonPrimitive() || !easing.getAsJsonPrimitive().isString()) { return at + ".easing: not a string"; }
            }
        }
        return null;
    }

    private static boolean finite(JsonElement e) {
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) { return false; }
        float f = e.getAsFloat();
        return !Float.isNaN(f) && !Float.isInfinite(f);
    }
}
