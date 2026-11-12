package noppes.npcs.rework.rig;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import noppes.npcs.CustomNpcs;
import noppes.npcs.LogWriter;

/**
 * M16: the rigged models an NPC can wear. Built-in ones live in the jar
 * ({@code assets/customnpcs/rig/<id>.geo.json}, {@code .animation.json}, {@code textures/rig/<id>.png});
 * more can be dropped into {@code customnpcs/rigs/} next to {@code clones/} (same three files), with no
 * rebuild. A file there with a built-in id replaces that model. Lookups never throw: a missing or broken
 * model is logged once and gives an empty clip library (the NPC is drawn as the original biped).
 */
public final class RigModels {
    /** Built-in models: id, head bone (null for none), shadow size, drawn with the NPC's own skin, has gear. */
    private static final Object[][] BUILT_IN = {
            {"steve", "head", 0.5F, true, false},
            {"legionary", "head", 0.5F, true, true},
            {"humanoid", "head", 0.5F},
            {"knight", "head", 0.5F},
            {"skeleton_warrior", "head", 0.5F},
            {"ogre", "head", 1.2F},
            {"troll", "head", 1.6F},
            {"warg", "head", 1.0F},
            {"giant_spider", "head", 1.4F},
            {"cave_bear", "head", 1.2F},
    };
    private static final Map<String, Model> CACHE = new HashMap<String, Model>();
    private static volatile int generation;

    /** One model: where its files are and its parsed clips. */
    public static final class Model {
        public final String id;
        public final String headBone;
        public final float shadow;
        /** Folder of a user model, or null for a jar one. */
        public final File folder;
        public final AnimLibrary clips;
        public final List<String> errors;
        /** The body has player-skin UVs (64x64) and is drawn with the NPC's CustomNPCs skin. */
        public final boolean skin;
        /** A second mesh {@code <id>_gear} (weapon, shield ...) with its own texture, same bones. */
        public final boolean gear;

        Model(String id, String headBone, float shadow, File folder, AnimLibrary clips, List<String> errors, boolean skin, boolean gear) {
            this.id = id; this.headBone = headBone; this.shadow = shadow; this.folder = folder;
            this.clips = clips; this.errors = Collections.unmodifiableList(errors); this.skin = skin; this.gear = gear;
        }

        public boolean builtIn() { return folder == null; }
    }

    private RigModels() {
    }

    /** Changes on every reload (clients drop meshes and textures). */
    public static int generation() {
        return generation;
    }

    /** User model folder, {@code customnpcs/rigs}. */
    public static File folder() {
        return CustomNpcs.Dir == null ? new File("customnpcs", "rigs") : new File(CustomNpcs.Dir, "rigs");
    }

    /** Valid id: lower-case letters, digits and underscores, 1..48 characters. */
    public static boolean validId(String id) {
        return id != null && id.matches("[a-z0-9_]{1,48}");
    }

    /** Every model id, built-in and from the folder, sorted. */
    public static List<String> ids() {
        TreeSet<String> out = new TreeSet<String>();
        for (Object[] b : BUILT_IN) { out.add((String) b[0]); }
        File[] files = folder().listFiles();
        if (files != null) {
            for (File f : files) {
                String n = f.getName();
                if (n.endsWith(".geo.json")) {
                    String id = n.substring(0, n.length() - ".geo.json".length());
                    if (validId(id)) { out.add(id); }
                }
            }
        }
        return new ArrayList<String>(out);
    }

    /** The model, or null when the id is unknown. */
    public static synchronized Model get(String id) {
        if (!validId(id)) { return null; }
        Model m = CACHE.get(id);
        if (m == null && !CACHE.containsKey(id)) {
            m = load(id);
            CACHE.put(id, m);
        }
        return m;
    }

    /** Forgets every parsed model (files changed); the next lookup reads them again. */
    public static synchronized void reload() {
        CACHE.clear();
        generation++;
        RigData.clearCache();
    }

    private static Model load(String id) {
        String head = "head";
        float shadow = 0.6F;
        boolean builtIn = false, skin = false, gear = false;
        for (Object[] b : BUILT_IN) {
            if (b[0].equals(id)) {
                head = (String) b[1]; shadow = (Float) b[2]; builtIn = true;
                if (b.length > 3) { skin = (Boolean) b[3]; gear = (Boolean) b[4]; }
            }
        }
        File dir = folder();
        File geo = new File(dir, id + ".geo.json");
        boolean user = geo.isFile();
        if (!user && !builtIn) { return null; }
        List<String> errors = new ArrayList<String>();
        AnimLibrary clips = AnimLibrary.EMPTY;
        try (InputStream in = user ? open(new File(dir, id + ".animation.json"))
                : RigModels.class.getResourceAsStream("/assets/customnpcs/rig/" + id + ".animation.json")) {
            if (in != null) {
                JsonObject root = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                clips = AnimLibrary.of(root, errors);
                if (root.has("rig") && root.get("rig").isJsonObject()) {
                    JsonObject rig = root.getAsJsonObject("rig");
                    if (rig.has("head")) { head = rig.get("head").isJsonNull() ? null : rig.get("head").getAsString(); }
                    if (rig.has("shadow")) { shadow = rig.get("shadow").getAsFloat(); }
                    if (rig.has("skin")) { skin = rig.get("skin").getAsBoolean(); }
                    if (rig.has("gear")) { gear = rig.get("gear").getAsBoolean(); }
                }
            } else {
                errors.add("no " + id + ".animation.json");
            }
        } catch (Exception e) {
            errors.add(id + ".animation.json: " + e);
        }
        for (String e : errors) { LogWriter.error("RWRIG model " + id + ": " + e); }
        return new Model(id, head, shadow, user ? dir : null, clips, errors, skin, gear);
    }

    private static InputStream open(File f) throws java.io.IOException {
        return f.isFile() ? new FileInputStream(f) : null;
    }

    /** Short description per id for the command list: clip names. */
    public static Map<String, String> describe() {
        Map<String, String> out = new LinkedHashMap<String, String>();
        for (String id : ids()) {
            Model m = get(id);
            out.put(id, m == null ? "?" : (m.builtIn() ? "" : "[folder] ") + String.join(", ", m.clips.sortedClipNames()));
        }
        return out;
    }
}
