package noppes.npcs.rework.rig;

import java.util.List;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * M16 script API (JS: {@code var Rig = Java.type("noppes.npcs.rework.rig.RigApi");}, Java scripts import it).
 * Sets what an NPC wears; every change is saved in its NBT (clone tab, spawners) and sent to the clients.
 * Clips are played with {@code noppes.npcs.rework.anim.Animators.of(npc)}. Server thread. Needs RwAnimCore=1
 * to be drawn and played; the data is kept with 0 too.
 */
public final class RigApi {
    private RigApi() {
    }

    private static EntityNPCInterface npc(ICustomNpc<?> npc) {
        Object raw = npc == null ? null : npc.getMCEntity();
        if (!(raw instanceof EntityNPCInterface)) { throw new IllegalArgumentException("not a CustomNPCs NPC"); }
        return (EntityNPCInterface) raw;
    }

    /** Model ids that can be worn (built-in and from customnpcs/rigs). */
    public static String[] models() {
        return RigModels.ids().toArray(new String[0]);
    }

    /** Wears the model (empty string: back to the original look). False for an unknown id. */
    public static boolean setModel(ICustomNpc<?> npc, String model) {
        EntityNPCInterface e = npc(npc);
        String id = model == null ? "" : model.trim();
        if (!id.isEmpty() && RigModels.get(id) == null) { return false; }
        e.rwRigPlayer.stop(e);
        e.rwRig.model = id;
        e.updateClient();
        return true;
    }

    public static String getModel(ICustomNpc<?> npc) {
        return npc(npc).rwRig.model;
    }

    /** Texture resource ("customnpcs:textures/rig/troll.png"), empty for the model's own. */
    public static void setTexture(ICustomNpc<?> npc, String texture) {
        EntityNPCInterface e = npc(npc);
        e.rwRig.texture = texture == null ? "" : texture.trim();
        e.updateClient();
    }

    /** Size on top of the NPC's own size (0.05..20). */
    public static void setScale(ICustomNpc<?> npc, float scale) {
        EntityNPCInterface e = npc(npc);
        e.rwRig.scale = Math.max(0.05F, Math.min(20, scale));
        e.updateClient();
    }

    /**
     * The NPC's own clips (Bedrock-style JSON, {"animations":{"name":{...}}}), added to and replacing the
     * model's by name. Returns the problems found (empty = all clips loaded); the bad clips are left out.
     */
    public static String[] setClips(ICustomNpc<?> npc, String json) {
        EntityNPCInterface e = npc(npc);
        String text = json == null ? "" : json;
        if (text.length() > RigData.MAX_CLIPS) { return new String[] {"clips longer than " + RigData.MAX_CLIPS + " characters"}; }
        e.rwRigPlayer.stop(e);
        e.rwRig.clips = text;
        List<String> errors = check(e.rwRig);
        e.updateClient();
        return errors.toArray(new String[0]);
    }

    public static String getClips(ICustomNpc<?> npc) {
        return npc(npc).rwRig.clips;
    }

    /** Which clips loop or play by themselves: role idle|walk|hurt|death. */
    public static void bind(ICustomNpc<?> npc, String role, String clip) {
        EntityNPCInterface e = npc(npc);
        String c = clip == null ? "" : clip;
        switch (role) {
            case "idle": e.rwRig.idle = c; break;
            case "walk": e.rwRig.walk = c; break;
            case "hurt": e.rwRig.hurt = c; break;
            case "death": e.rwRig.death = c; break;
            default: throw new IllegalArgumentException("role must be idle, walk, hurt or death");
        }
        e.updateClient();
    }

    /** Clip names this NPC can play. */
    public static String[] clips(ICustomNpc<?> npc) {
        return npc(npc).rwRig.library().sortedClipNames().toArray(new String[0]);
    }

    /**
     * Applies a rig package (one JSON file an AI or a person writes):
     * <pre>
     * { "model": "knight", "texture": "", "scale": 1.2, "name": "Sir Boss",
     *   "idle": "guard", "walk": "walk", "hurt": "hurt", "death": "death",
     *   "clips": {"animations": {...}},          // the NPC's own clips (optional)
     *   "script": ["function tick(e) {", "...", "}"] }  // JS (a string or lines), replaces the NPC's scripts
     * </pre>
     * Everything lands in the NPC's NBT, so saving it in the clone tab keeps it. Returns the problems found.
     */
    public static String[] applyPackage(ICustomNpc<?> npc, String json) {
        EntityNPCInterface e = npc(npc);
        List<String> problems = new java.util.ArrayList<String>();
        com.google.gson.JsonObject p;
        try {
            p = new com.google.gson.JsonParser().parse(json).getAsJsonObject();
        } catch (RuntimeException ex) {
            return new String[] {"package is not a JSON object: " + ex.getMessage()};
        }
        String model = p.has("model") ? p.get("model").getAsString() : e.rwRig.model;
        if (RigModels.get(model) == null) { return new String[] {"unknown model '" + model + "'"}; }
        e.rwRigPlayer.stop(e);
        RigData r = e.rwRig;
        r.model = model;
        if (p.has("texture")) { r.texture = p.get("texture").getAsString(); }
        if (p.has("scale")) { r.scale = Math.max(0.05F, Math.min(20, p.get("scale").getAsFloat())); }
        if (p.has("idle")) { r.idle = p.get("idle").getAsString(); }
        if (p.has("walk")) { r.walk = p.get("walk").getAsString(); }
        if (p.has("hurt")) { r.hurt = p.get("hurt").getAsString(); }
        if (p.has("death")) { r.death = p.get("death").getAsString(); }
        if (p.has("clips")) {
            String clips = p.get("clips").isJsonPrimitive() ? p.get("clips").getAsString() : p.get("clips").toString();
            if (clips.length() > RigData.MAX_CLIPS) { problems.add("clips longer than " + RigData.MAX_CLIPS + " characters, left out"); }
            else { r.clips = clips; problems.addAll(check(r)); }
        }
        for (String role : new String[] {r.idle, r.walk, r.hurt, r.death}) {
            if (!role.isEmpty() && r.library().clipOrNull(role) == null) { problems.add("no clip '" + role + "' (idle/walk/hurt/death)"); }
        }
        if (p.has("name")) { e.display.setName(p.get("name").getAsString()); }
        // Skin of a player-shaped model (steve, legionary): a resource, a URL or a player name, as in Display.
        if (p.has("skinTexture")) { e.display.setSkinTexture(p.get("skinTexture").getAsString()); }
        if (p.has("skinUrl")) { e.display.setSkinUrl(p.get("skinUrl").getAsString()); }
        if (p.has("skinPlayer")) { e.display.setSkinPlayer(p.get("skinPlayer").getAsString()); }
        if (p.has("script")) {
            com.google.gson.JsonElement s = p.get("script");
            StringBuilder src = new StringBuilder();
            if (s.isJsonArray()) { for (com.google.gson.JsonElement line : s.getAsJsonArray()) { src.append(line.getAsString()).append('\n'); } }
            else { src.append(s.getAsString()); }
            net.minecraft.nbt.NBTTagCompound tab = new net.minecraft.nbt.NBTTagCompound();
            tab.setString("Script", src.toString());
            tab.setTag("Console", new net.minecraft.nbt.NBTTagList());
            tab.setTag("ScriptList", new net.minecraft.nbt.NBTTagList());
            net.minecraft.nbt.NBTTagList tabs = new net.minecraft.nbt.NBTTagList();
            tabs.appendTag(tab);
            net.minecraft.nbt.NBTTagCompound data = new net.minecraft.nbt.NBTTagCompound();
            data.setTag("Scripts", tabs);
            data.setString("ScriptLanguage", "ECMAScript");
            data.setBoolean("ScriptEnabled", true);
            e.script.readFromNBT(data);
            e.script.lastInited = -1;
        }
        e.updateClient();
        return problems.toArray(new String[0]);
    }

    static List<String> check(RigData rig) {
        List<String> errors = new java.util.ArrayList<String>();
        AnimLibrary.merge(AnimLibrary.EMPTY, rig.clips, errors);
        return errors;
    }
}
