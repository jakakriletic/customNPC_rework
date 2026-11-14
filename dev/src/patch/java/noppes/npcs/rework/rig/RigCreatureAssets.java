package noppes.npcs.rework.rig;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import noppes.npcs.CustomNpcs;
import noppes.npcs.LogWriter;
import noppes.npcs.rework.anim.ReloadCodec;
import noppes.npcs.util.NBTJsonUtil;

/** A server directory snapshot. The wire bundle contains only model, texture and animation data. */
public final class RigCreatureAssets {
    private static final int MAX_PACKS = 16;
    private static final int MAX_JSON = 512 * 1024;
    private static final int MAX_TEXTURE = 1024;
    private static final java.util.Set<String> RIG_FIELDS = new java.util.HashSet<>(java.util.Arrays.asList(
            "base", "name", "scale", "idle", "walk", "hurt", "death", "model", "script", "skinTexture", "skinPlayer", "skinUrl", "texture"));
    private static volatile Snapshot server = Snapshot.empty();
    private static volatile Snapshot client = Snapshot.empty();

    static final class Pack {
        final JsonObject config;
        final String clone;

        Pack(JsonObject config, String clone) {
            this.config = config;
            this.clone = clone;
        }
    }

    public static final class Snapshot {
        final Map<String, RigModels.Model> models;
        final Map<String, Pack> packs;
        final byte[] data, hash;

        private Snapshot(Map<String, RigModels.Model> models, Map<String, Pack> packs, byte[] data) {
            this.models = Collections.unmodifiableMap(models);
            this.packs = Collections.unmodifiableMap(packs);
            this.data = data;
            this.hash = data == null ? null : ReloadCodec.hash(data);
        }

        private static Snapshot empty() {
            return new Snapshot(Collections.emptyMap(), Collections.emptyMap(), null);
        }
    }

    private RigCreatureAssets() { }

    static Map<String, RigModels.Model> models(boolean remote) { return (remote ? client : server).models; }
    static Snapshot server() { return server; }
    public static List<String> ids() { return new ArrayList<>(server.packs.keySet()); }

    static void publishServer(Snapshot snapshot) {
        server = snapshot;
        RigModels.reload();
    }

    public static void publishClient(Snapshot snapshot) {
        client = snapshot == null ? Snapshot.empty() : snapshot;
        RigModels.reload();
    }

    static void clearServer() { publishServer(Snapshot.empty()); }

    /** At startup ignore individual broken packs. A live reload keeps the previous snapshot on any error. */
    static Snapshot read(boolean startup) throws IOException {
        Path root = CustomNpcs.Dir.toPath().resolve("creatures");
        if (!Files.exists(root)) {
            return new Snapshot(Collections.emptyMap(), Collections.emptyMap(), ReloadCodec.encodeBundle(Collections.emptyMap()));
        }
        if (!Files.isDirectory(root)) { throw new IOException("creatures must be a directory"); }
        Map<String, byte[]> files = new TreeMap<>();
        Map<String, Pack> packs = new TreeMap<>();
        Map<String, RigModels.Model> models = new TreeMap<>();
        int total = 4;
        try (java.nio.file.DirectoryStream<Path> dirs = Files.newDirectoryStream(root)) {
            List<Path> sorted = new ArrayList<>();
            for (Path dir : dirs) {
                if (Files.isRegularFile(dir.resolve("creature.json"))) { sorted.add(dir); }
            }
            Collections.sort(sorted);
            for (Path dir : sorted) {
                String id = dir.getFileName().toString();
                try {
                    if (!RigModels.validId(id) || packs.size() == MAX_PACKS) {
                        throw new IllegalArgumentException("use a lowercase id and at most " + MAX_PACKS + " packs");
                    }
                    if (!dir.toRealPath().startsWith(root.toRealPath())) { throw new IOException("folder outside creatures"); }
                    JsonObject config = object(read(dir, "creature.json", MAX_JSON));
                    for (Map.Entry<String, JsonElement> field : config.entrySet()) {
                        if (!RIG_FIELDS.contains(field.getKey()) || field.getValue().isJsonNull()) {
                            throw new IllegalArgumentException("invalid creature.json field " + field.getKey());
                        }
                    }
                    if (config.has("base")) { requireString(config.get("base"), "base"); }
                    String base = config.has("base") ? config.get("base").getAsString() : "";
                    boolean custom = base.isEmpty();
                    if (custom && RigModels.isBuiltIn(id)) {
                        throw new IllegalArgumentException("built-in id requires a base model");
                    }
                    if (!custom && !RigModels.isBuiltIn(base)) { throw new IllegalArgumentException("unknown base model " + base); }
                    if (config.has("model")) {
                        requireString(config.get("model"), "model");
                        if (!config.get("model").getAsString().equals(id)) { throw new IllegalArgumentException("model must match the folder name"); }
                    }
                    config.addProperty("model", id);
                    byte[] animations = read(dir, "animations.json", MAX_JSON);
                    byte[] geo = custom ? read(dir, "model.geo.json", MAX_JSON) : null;
                    byte[] png = custom ? read(dir, "texture.png", ReloadCodec.MAX_FILE) : null;
                    RigModels.Model model = model(id, base, animations, geo, png);
                    for (String key : new String[] {"name", "texture", "idle", "walk", "hurt", "death", "skinTexture", "skinPlayer", "skinUrl"}) {
                        if (config.has(key)) { requireString(config.get(key), key); }
                    }
                    if (config.has("scale")) {
                        float scale = config.get("scale").getAsFloat();
                        if (!Float.isFinite(scale) || scale < 0.05F || scale > 20) { throw new IllegalArgumentException("scale must be 0.05 to 20"); }
                    }
                    for (String key : new String[] {"idle", "walk", "hurt", "death"}) {
                        if (config.has(key) && !config.get(key).getAsString().isEmpty() && !model.clips.has(config.get(key).getAsString())) {
                            throw new IllegalArgumentException("unknown " + key + " clip");
                        }
                        if (!config.has(key) && !model.clips.has(key)) { config.addProperty(key, ""); }
                    }
                    if (Files.exists(dir.resolve("script.js"))) {
                        if (config.has("script")) { throw new IllegalArgumentException("use script.js or inline script"); }
                        config.addProperty("script", new String(read(dir, "script.js", MAX_JSON), StandardCharsets.UTF_8));
                    }
                    if (config.has("script")) {
                        JsonElement script = config.get("script");
                        if (script.isJsonArray()) {
                            for (JsonElement line : script.getAsJsonArray()) { requireString(line, "script line"); }
                        } else { requireString(script, "script"); }
                    }
                    String clone = Files.exists(dir.resolve("clone.json"))
                            ? new String(read(dir, "clone.json", MAX_JSON), StandardCharsets.UTF_8) : "";
                    if (!clone.isEmpty()) { NBTJsonUtil.Convert(clone); }
                    byte[] baseBytes = base.getBytes(StandardCharsets.UTF_8);
                    int size = animations.length + baseBytes.length + 2 * (8 + id.length());
                    if (custom) { size += geo.length + png.length + 2 * (8 + id.length()); }
                    if (total + size > ReloadCodec.MAX_BUNDLE) { throw new IOException("creature bundle exceeds 8 MiB"); }
                    total += size;
                    files.put("b_" + id, baseBytes);
                    files.put("a_" + id, animations);
                    if (custom) {
                        files.put("g_" + id, geo);
                        files.put("t_" + id, png);
                    }
                    models.put(id, model);
                    packs.put(id, new Pack(config, clone));
                } catch (Exception error) {
                    String message = "RWRIG creature " + id + ": " + error.getMessage();
                    if (!startup) { throw new IOException(message, error); }
                    LogWriter.error(message);
                }
            }
        }
        return new Snapshot(models, packs, ReloadCodec.encodeBundle(files));
    }

    public static Snapshot decode(byte[] bundle) throws IOException {
        TreeMap<String, byte[]> files = ReloadCodec.decodeBundle(bundle);
        if (files == null) { throw new IOException("invalid creature bundle"); }
        Map<String, RigModels.Model> models = new TreeMap<>();
        for (String key : new ArrayList<>(files.keySet())) {
            if (!key.startsWith("b_")) { continue; }
            String id = key.substring(2);
            String base = new String(files.remove(key), StandardCharsets.UTF_8);
            if (!RigModels.validId(id) || models.size() >= MAX_PACKS) { throw new IOException("invalid creature id " + id); }
            models.put(id, model(id, base, files.remove("a_" + id), files.remove("g_" + id), files.remove("t_" + id)));
        }
        if (!files.isEmpty()) { throw new IOException("unclaimed creature assets"); }
        return new Snapshot(models, Collections.emptyMap(), bundle);
    }

    private static RigModels.Model model(String id, String base, byte[] animations, byte[] geo, byte[] png) throws IOException {
        RigModels.Model builtIn = base.isEmpty() ? null : RigModels.builtIn(base);
        if (!base.isEmpty() && builtIn == null) { throw new IOException("unknown base " + base); }
        if (animations == null || animations.length > MAX_JSON) { throw new IOException("missing or oversized animations"); }
        if (builtIn == null) {
            if (RigModels.isBuiltIn(id) || geo == null || geo.length > MAX_JSON || png == null) {
                throw new IOException("custom creature needs a new id, model and texture");
            }
            checkGeometry(geo);
            checkTexture(png);
        } else if (geo != null || png != null) {
            throw new IOException("base model already supplies geometry and texture");
        }
        JsonObject root = object(animations);
        if (!root.has("animations") || !root.get("animations").isJsonObject()) {
            throw new IllegalArgumentException("animations.json must contain an animations object");
        }
        JsonObject clips = root.getAsJsonObject("animations");
        if (clips.size() > 128) { throw new IllegalArgumentException("at most 128 clips"); }
        for (Map.Entry<String, JsonElement> entry : clips.entrySet()) {
            if (entry.getKey().length() > RigNet.MAX_NAME) { throw new IllegalArgumentException("clip name too long"); }
            checkClip(entry.getKey(), entry.getValue().getAsJsonObject());
        }
        List<String> errors = new ArrayList<>();
        AnimLibrary library = AnimLibrary.merge(builtIn == null ? AnimLibrary.EMPTY : builtIn.clips, root.toString(), errors);
        if (!errors.isEmpty()) { throw new IOException(String.join(", ", errors)); }
        return new RigModels.Model(id, builtIn == null ? "head" : builtIn.headBone,
                builtIn == null ? 0.6F : builtIn.shadow, null, library, errors,
                builtIn != null && builtIn.skin, builtIn != null && builtIn.gear, base.isEmpty() ? id : base, geo, png);
    }

    private static void checkClip(String name, JsonObject json) {
        AnimClip clip = new AnimClip(name, json);
        if (!Float.isFinite(clip.length) || clip.length <= 0 || clip.length > 600 || !Float.isFinite(clip.stride)) {
            throw new IllegalArgumentException("invalid duration or stride of " + name);
        }
        for (AnimEvent event : clip.events) {
            if (!Float.isFinite(event.time) || event.time < 0 || event.time > clip.length) {
                throw new IllegalArgumentException("event outside clip " + name);
            }
        }
        // The regular animation parser also verifies vector shapes and easing data.
        clip.sample(0, new Pose());
    }

    private static void checkGeometry(byte[] bytes) {
        JsonObject geometry = object(bytes).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonObject desc = geometry.getAsJsonObject("description");
        int width = desc.get("texture_width").getAsInt(), height = desc.get("texture_height").getAsInt();
        if (width < 1 || height < 1 || width > MAX_TEXTURE || height > MAX_TEXTURE) {
            throw new IllegalArgumentException("invalid model texture dimensions");
        }
        JsonArray bones = geometry.getAsJsonArray("bones");
        if (bones.size() == 0 || bones.size() > 256) { throw new IllegalArgumentException("expected 1 to 256 bones"); }
        Map<String, String> parents = new HashMap<>();
        int cubes = 0;
        for (JsonElement element : bones) {
            JsonObject bone = element.getAsJsonObject();
            String name = bone.get("name").getAsString();
            if (name.isEmpty() || parents.put(name, bone.has("parent") ? bone.get("parent").getAsString() : "") != null) {
                throw new IllegalArgumentException("empty or duplicate bone " + name);
            }
            checkVector(bone, "pivot");
            checkVector(bone, "rotation");
            if (!bone.has("cubes")) { continue; }
            for (JsonElement part : bone.getAsJsonArray("cubes")) {
                if (++cubes > 2048) { throw new IllegalArgumentException("at most 2048 cubes"); }
                JsonObject cube = part.getAsJsonObject();
                for (String field : new String[] {"origin", "size", "pivot", "rotation"}) { checkVector(cube, field); }
                if (cube.has("inflate") && !Float.isFinite(cube.get("inflate").getAsFloat())) {
                    throw new IllegalArgumentException("invalid cube inflate");
                }
                JsonObject uv = cube.getAsJsonObject("uv");
                if (uv != null) {
                    for (Map.Entry<String, JsonElement> face : uv.entrySet()) {
                        checkVector(face.getValue().getAsJsonObject(), "uv");
                        checkVector(face.getValue().getAsJsonObject(), "uv_size");
                    }
                }
            }
        }
        for (String name : parents.keySet()) {
            String parent = name;
            int depth = 0;
            while (!parent.isEmpty()) {
                parent = parents.get(parent);
                if (parent == null || ++depth > 64) { throw new IllegalArgumentException("invalid parent chain of " + name); }
            }
        }
    }

    private static void checkVector(JsonObject owner, String key) {
        if (!owner.has(key)) { return; }
        for (JsonElement value : owner.getAsJsonArray(key)) {
            if (!Float.isFinite(value.getAsFloat())) { throw new IllegalArgumentException("invalid " + key); }
        }
    }

    private static void checkTexture(byte[] bytes) throws IOException {
        try (MemoryCacheImageInputStream stream = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            ImageReader reader = ImageIO.getImageReadersByFormatName("png").next();
            try {
                reader.setInput(stream, true, true);
                int w = reader.getWidth(0), h = reader.getHeight(0);
                if (w < 1 || h < 1 || w > MAX_TEXTURE || h > MAX_TEXTURE) { throw new IOException("PNG exceeds 1024×1024"); }
                reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }

    private static void requireString(JsonElement json, String field) {
        if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(field + " must be text");
        }
    }

    private static JsonObject object(byte[] bytes) {
        return new JsonParser().parse(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static byte[] read(Path dir, String name, int max) throws IOException {
        Path path = dir.resolve(name);
        if (!Files.isRegularFile(path) || !path.toRealPath().startsWith(dir.toRealPath())) {
            throw new IOException("missing file " + name);
        }
        try (InputStream in = Files.newInputStream(path)) {
            byte[] data = com.google.common.io.ByteStreams.toByteArray(com.google.common.io.ByteStreams.limit(in, max + 1L));
            if (data.length > max) { throw new IOException(name + " exceeds " + max + " bytes"); }
            return data;
        }
    }
}
