package noppes.npcs.rework.rig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.NBTTagCompound;

/**
 * M16: what a rigged NPC wears, kept in its NBT as {@code RwRig} (so it travels with the clone tab's
 * JSON, {@code /noppes clone} and spawners) and in its spawn data (every client draws it).
 *
 * <pre>
 * "RwRig": {
 *   "Model": "troll",                       // RigModels id
 *   "Texture": "customnpcs:textures/rig/troll_red.png",   // optional, else the model's own
 *   "Scale": 1.0,                           // optional, on top of the NPC's size
 *   "Clips": "{\"animations\":{...}}",      // optional, Bedrock-style clips added to the model's
 *   "Idle": "idle", "Walk": "walk", "Hurt": "hurt", "Death": "death"   // optional clip names
 * }
 * </pre>
 */
public final class RigData {
    /** Longest Clips string kept (it is in every spawn packet of the NPC). */
    public static final int MAX_CLIPS = 200000;
    private static final Map<String, AnimLibrary> LIBRARIES = new LinkedHashMap<String, AnimLibrary>(16, 0.75F, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, AnimLibrary> e) { return size() > 64; }
    };

    public String model = "";
    public String texture = "";
    public float scale = 1;
    public String clips = "";
    public String idle = "idle", walk = "walk", hurt = "hurt", death = "death";
    private AnimLibrary library;
    private int libraryGen = -1;
    private String libraryKey;
    private static volatile int generation;
    private List<String> errors = Collections.emptyList();

    public boolean active() {
        return !model.isEmpty();
    }

    public static RigData read(NBTTagCompound npc) {
        RigData d = new RigData();
        if (!npc.hasKey("RwRig", 10)) { return d; }
        NBTTagCompound t = npc.getCompoundTag("RwRig");
        d.model = RigModels.validId(t.getString("Model")) ? t.getString("Model") : "";
        d.texture = t.getString("Texture");
        d.scale = t.hasKey("Scale") ? Math.max(0.05F, Math.min(20, t.getFloat("Scale"))) : 1;
        String c = t.getString("Clips");
        d.clips = c.length() > MAX_CLIPS ? "" : c;
        d.idle = name(t, "Idle", "idle");
        d.walk = name(t, "Walk", "walk");
        d.hurt = name(t, "Hurt", "hurt");
        d.death = name(t, "Death", "death");
        return d;
    }

    private static String name(NBTTagCompound t, String key, String fallback) {
        return t.hasKey(key, 8) ? t.getString(key) : fallback;
    }

    /** Writes RwRig, or removes it when the NPC wears no rig. */
    public void write(NBTTagCompound npc) {
        if (!active()) { npc.removeTag("RwRig"); return; }
        NBTTagCompound t = new NBTTagCompound();
        t.setString("Model", model);
        if (!texture.isEmpty()) { t.setString("Texture", texture); }
        if (scale != 1) { t.setFloat("Scale", scale); }
        if (!clips.isEmpty()) { t.setString("Clips", clips); }
        if (!idle.equals("idle")) { t.setString("Idle", idle); }
        if (!walk.equals("walk")) { t.setString("Walk", walk); }
        if (!hurt.equals("hurt")) { t.setString("Hurt", hurt); }
        if (!death.equals("death")) { t.setString("Death", death); }
        npc.setTag("RwRig", t);
    }

    public RigModels.Model modelDef() {
        return modelDef(false);
    }

    public RigModels.Model modelDef(boolean client) {
        return RigModels.get(model, client);
    }

    /** The model's clips with this NPC's own added; empty when the model is unknown. */
    public AnimLibrary library() {
        return library(false);
    }

    public AnimLibrary library(boolean client) {
        String key = (client ? "c" : "s") + model + "\n" + clips;
        // Model or own clips changed (RigApi, /rwrig) or files reloaded: look the library up again.
        if (library == null || libraryGen != generation || !key.equals(libraryKey)) {
            int currentGeneration = generation;
            RigModels.Model m = modelDef(client);
            libraryGen = currentGeneration;
            libraryKey = key;
            synchronized (LIBRARIES) {
                library = LIBRARIES.get(key);
                if (library == null) {
                    List<String> found = new ArrayList<String>();
                    library = AnimLibrary.merge(m == null ? AnimLibrary.EMPTY : m.clips, clips, found);
                    LIBRARIES.put(key, library);
                    errors = found;
                }
            }
        }
        return library;
    }

    /** Problems found in this NPC's own clips (empty when none or not parsed yet). */
    public List<String> errors() {
        return errors;
    }

    static void clearCache() {
        synchronized (LIBRARIES) { LIBRARIES.clear(); generation++; }
    }

    /** Same rig (what a client draws). */
    public boolean sameAs(RigData o) {
        return o != null && model.equals(o.model) && texture.equals(o.texture) && scale == o.scale && clips.equals(o.clips)
                && idle.equals(o.idle) && walk.equals(o.walk) && hurt.equals(o.hurt) && death.equals(o.death);
    }
}
