package noppes.npcs.rework.data;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.nbt.*;

/** Versioned disk-only clone view. Never constructs an entity, executes code, or normalizes item/rig data. */
public final class ReadableClone {
    public static final String FORMAT = "customnpcs-readable-clone";
    public static final String DEFAULTS = "cnpc-01oct19-v1";
    public static final int MAX_BYTES = 32 * 1024 * 1024;
    private static final String[] SECTIONS = {"entity", "appearance", "stats", "ai", "inventory", "scripts", "rig", "advanced", "extensions"};
    private ReadableClone() { }

    private static final class Field {
        final String alias, nbt, kind, description;
        final NBTBase fallback;
        Field(String a, String n, String k, NBTBase d, String info) { alias = a; nbt = n; kind = k; fallback = d; description = info; }
    }
    private static Field f(String a, String n, String k, String info) { return new Field(a,n,k,null,info); }
    private static Field d(String a, String n, String k, NBTBase v, String info) { return new Field(a,n,k,v,info); }
    // Frozen cnpc-01oct19-v1. Values verified against DataDisplay, DataAI and DataInventory constructors/writers.
    private static final Field[] FIELDS = {
        f("entity.type", "id", "string", "Entity registry id"),
        f("entity.modRevision", "ModRev", "int", "CNPC data revision"),
        f("appearance.name", "Name", "string", "Display name; never defaulted (original random name)"),
        d("appearance.title", "Title", "string", new NBTTagString(""), "Title"),
        d("appearance.modelSize", "Size", "int", new NBTTagInt(5), "CNPC size units (5 = normal)"),
        f("appearance.texture", "Texture", "string", "Texture resource id"),
        f("appearance.skinUrl", "SkinUrl", "string", "Skin URL"),
        f("appearance.skinSource", "UsingSkinUrl", "byte", "Original numeric skin mode"),
        f("appearance.cloakTexture", "CloakTexture", "string", "Cloak resource id"),
        f("appearance.glowTexture", "GlowTexture", "string", "Glow resource id"),
        f("appearance.nameVisibility", "ShowName", "int", "Original numeric name visibility"),
        f("appearance.skinColor", "SkinColor", "int", "RGB integer"),
        f("appearance.visibility", "NpcVisible", "int", "Original numeric visibility"),
        f("appearance.disableLivingAnimation", "NoLivingAnimation", "boolean", "Disable vanilla living animation"),
        f("appearance.noHitbox", "IsStatue", "boolean", "Original statue flag"),
        f("appearance.bossBar", "BossBar", "byte", "Original numeric boss bar mode"),
        f("appearance.bossColor", "BossColor", "int", "Original numeric boss color"),
        f("stats.maxHealth", "MaxHealth", "int", "Health points"),
        f("stats.meleeDamage", "AttackStrenght", "int", "Damage points"),
        f("stats.attackInterval", "AttackSpeed", "int", "Ticks between attacks"),
        f("stats.attackRange", "AttackRange", "int", "Blocks"),
        f("stats.knockback", "KnockBack", "int", "Original knockback setting"),
        f("stats.aggroRange", "AggroRange", "int", "Blocks"),
        f("stats.respawnTime", "RespawnTime", "int", "Original respawn setting"),
        f("stats.healthRegen", "HealthRegen", "int", "Original regeneration setting"),
        f("stats.combatRegen", "CombatRegen", "int", "Original combat regeneration setting"),
        f("stats.immuneToFire", "ImmuneToFire", "boolean", "Fire immunity"),
        f("stats.canDrown", "CanDrown", "boolean", "Drowning enabled"),
        f("ai.walkingSpeed", "MoveSpeed", "int", "Original speed slider units"),
        f("ai.walkingRange", "WalkingRange", "int", "Blocks"),
        f("ai.movingPattern", "MovingPatern", "int", "Original numeric path pattern"),
        f("ai.movementType", "MovementType", "int", "Original numeric movement mode"),
        f("ai.movingState", "MovingState", "int", "Original numeric moving state"),
        f("ai.standingState", "StandingState", "int", "Original numeric standing state"),
        f("ai.animationState", "MoveState", "int", "Original numeric animation state"),
        f("ai.orientation", "Orientation", "int", "Original orientation units"),
        f("ai.canSwim", "CanSwim", "boolean", "Swimming enabled"),
        f("ai.returnToStart", "ReturnToStart", "boolean", "Return home"),
        f("ai.avoidWater", "AvoidsWater", "boolean", "Avoid water"),
        f("ai.avoidSun", "AvoidsSun", "boolean", "Avoid sunlight"),
        f("ai.reactToFire", "ReactsToFire", "boolean", "React to fire"),
        f("ai.canSprint", "CanSprint", "boolean", "Sprint enabled"),
        f("ai.canLeap", "CanLeap", "boolean", "Leaping enabled"),
        f("ai.directLineOfSight", "DirectLOS", "boolean", "Original direct line-of-sight setting"),
        f("ai.doorInteraction", "DoorInteract", "int", "Original numeric door mode"),
        f("ai.onAttack", "OnAttack", "int", "Original numeric response"),
        f("ai.tacticalVariant", "TacticalVariant", "int", "Original numeric tactic"),
        f("ai.tacticalRadius", "TacticalRadius", "int", "Blocks"),
        d("inventory.minExperience", "MinExp", "int", new NBTTagInt(0), "Experience points"),
        d("inventory.maxExperience", "MaxExp", "int", new NBTTagInt(0), "Experience points"),
        f("inventory.lootMode", "LootMode", "int", "Original numeric loot mode"),
        f("inventory.drops", "NpcInv", "items", "Ordered item slots (duplicates preserved)"),
        f("inventory.armor", "Armor", "items", "Armor slots: original slot indices"),
        f("inventory.weapons", "Weapons", "items", "Slots 0 right hand, 1 projectile, 2 left hand"),
        f("scripts.language", "ScriptLanguage", "string", "ECMAScript or Java; import never executes source"),
        f("scripts.enabled", "ScriptEnabled", "boolean", "Enable scripts"),
        f("scripts.tabs", "Scripts", "tabs", "Ordered script cards, including empty cards"),
        f("rig.model", "RwRig/Model", "string", "Rig model id (unknown ids preserved)"),
        f("rig.texture", "RwRig/Texture", "string", "Rig texture resource id"),
        f("rig.scale", "RwRig/Scale", "float", "Scale multiplier"),
        f("rig.clipJsonLines", "RwRig/Clips", "lines", "Exact clip JSON string, split into lines"),
        f("rig.idle", "RwRig/Idle", "string", "Idle clip name"),
        f("rig.walk", "RwRig/Walk", "string", "Walk clip name"),
        f("rig.hurt", "RwRig/Hurt", "string", "Hurt clip name"),
        f("rig.death", "RwRig/Death", "string", "Death clip name"),
        f("advanced.role", "Role", "int", "Original numeric role"),
        f("advanced.job", "Job", "int", "Original numeric job")
    };
    private static final Field[] TABS = {
        f("sourceLines", "Script", "lines", "Exact source text"),
        f("includes", "ScriptList", "includes", "Ordered include entries"),
        f("console", "Console", "raw", "Typed console records (timestamps are long)")
    };
    private static final Field[] ITEMS = {
        f("slot", "Slot", "byte", "Original slot index"), f("item", "id", "string", "Item id"),
        f("count", "Count", "byte", "Stack count"), f("damage", "Damage", "short", "Item metadata/damage")
    };
    private static final Field[] INCLUDES = { f("file", "Line", "string", "Include file name") };

    /** Returns a strict JSON document and verifies its complete typed roundtrip before returning. */
    public static String encode(NBTTagCompound input) {
        JsonObject doc = new JsonObject();
        doc.addProperty("$format", FORMAT); doc.addProperty("version", 1); doc.addProperty("defaults", DEFAULTS);
        JsonObject body = exportScope(input, FIELDS, true);
        for (Map.Entry<String, JsonElement> e : body.entrySet()) { doc.add(e.getKey(), e.getValue()); }
        String text = StrictCloneJson.write(doc); checkSize(text);
        if (!same(input, decode(text))) { throw CloneNbt.bad("/", "Export changed typed NBT"); }
        return text;
    }
    public static NBTTagCompound decode(String text) {
        checkSize(text);
        JsonObject doc = CloneNbt.object(StrictCloneJson.read(text), "/");
        if (!FORMAT.equals(CloneNbt.string(doc.get("$format"), "/$format"))) { throw CloneNbt.bad("/$format", "Unknown clone format"); }
        if (CloneNbt.integer(doc.get("version"), 1, 1, false, "/version") != 1) { throw CloneNbt.bad("/version", "Unknown version"); }
        if (!DEFAULTS.equals(CloneNbt.string(doc.get("defaults"), "/defaults"))) { throw CloneNbt.bad("/defaults", "Unknown defaults profile"); }
        JsonObject body = new JsonObject();
        for (Map.Entry<String, JsonElement> e : doc.entrySet()) {
            if (!Arrays.asList("$format", "version", "defaults").contains(e.getKey())) { body.add(e.getKey(), e.getValue()); }
        }
        return importScope(body, FIELDS, true, "");
    }
    public static boolean same(NBTBase a, NBTBase b) { return CloneNbt.same(a,b); }
    private static void checkSize(String s) { if (s.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) { throw CloneNbt.bad("/", "Clone exceeds 32 MiB UTF-8"); } }

    private static JsonObject exportScope(NBTTagCompound input, Field[] fields, boolean root) {
        NBTTagCompound raw = input.copy(); JsonObject doc = new JsonObject(); JsonArray absent = new JsonArray();
        if (root) { for (String s : SECTIONS) { doc.add(s, new JsonObject()); } }
        for (Field f : fields) {
            NBTBase t = get(input, f.nbt);
            JsonElement v = t == null ? null : readable(t, f.kind);
            if (v != null) {
                remove(raw, f.nbt);
                if (f.fallback == null || !same(t, f.fallback)) {
                    if (f.kind.equals("lines")) { textOut(owner(doc,f.alias), leaf(f.alias), ((NBTTagString)t).getString()); }
                    else { owner(doc,f.alias).add(leaf(f.alias), v); }
                }
            } else if (f.fallback != null) { absent.add(new JsonPrimitive(aliasPointer(f.alias))); }
        }
        JsonObject meta = new JsonObject();
        if (absent.size() > 0) { meta.add("absent", absent); }
        if (!raw.hasNoTags()) { meta.add("raw", CloneNbt.encode(raw)); }
        if (meta.entrySet().size() > 0) { doc.add("_nbt", meta); }
        return doc;
    }
    private static JsonElement readable(NBTBase t, String kind) {
        if (kind.equals("raw")) { return CloneNbt.encode(t); }
        if (kind.equals("lines")) { return t.getId() == 8 ? new JsonPrimitive("") : null; }
        if (kind.equals("tabs") || kind.equals("items") || kind.equals("includes")) {
            if (t.getId() != 9) { return null; } NBTTagList l = (NBTTagList)t;
            // Empty typed lists other than end need the raw fallback, because [] denotes an end list.
            if (l.tagCount() == 0 && l.getTagType() != 0 || l.tagCount() > 0 && l.getTagType() != 10) { return null; }
            JsonArray a = new JsonArray(); Field[] sub = subfields(kind);
            for (int i = 0; i < l.tagCount(); i++) { a.add(exportScope(l.getCompoundTagAt(i), sub, false)); } return a;
        }
        if (kind.equals("boolean")) {
            if (t.getId() != 1) { return null; } byte b = ((NBTTagByte)t).getByte();
            return b == 0 || b == 1 ? new JsonPrimitive(b == 1) : null;
        }
        int id = Arrays.asList(CloneNbt.TYPES).indexOf(kind);
        if (id != t.getId()) { return null; }
        JsonObject node = CloneNbt.encode(t); return node.has("bits") ? null : node.get("value");
    }
    private static Field[] subfields(String kind) { return kind.equals("tabs") ? TABS : kind.equals("items") ? ITEMS : INCLUDES; }
    private static NBTTagCompound importScope(JsonObject doc, Field[] fields, boolean root, String p) {
        Set<String> allowed = new HashSet<String>(); allowed.add("_nbt");
        for (Field f : fields) {
            String group = group(f.alias); allowed.add(group == null ? f.alias : group);
            if (f.kind.equals("lines") && group == null) { allowed.add("lineEnding"); allowed.add("lineEndings"); }
        }
        if (root) { allowed.addAll(Arrays.asList(SECTIONS)); }
        CloneNbt.keys(doc, p, allowed.toArray(new String[0]));
        if (root) {
            for (String section : SECTIONS) {
                if (!doc.has(section)) { continue; }
                List<String> keys = new ArrayList<String>();
                for (Field f : fields) {
                    if (section.equals(group(f.alias))) { keys.add(leaf(f.alias)); if (f.kind.equals("lines")) { keys.add("lineEnding"); keys.add("lineEndings"); } }
                }
                CloneNbt.keys(CloneNbt.object(doc.get(section), p + "/" + section), p + "/" + section, keys.toArray(new String[0]));
            }
        }
        JsonObject meta = doc.has("_nbt") ? CloneNbt.object(doc.get("_nbt"), p + "/_nbt") : new JsonObject();
        CloneNbt.keys(meta, p + "/_nbt", "raw", "absent");
        NBTTagCompound result = new NBTTagCompound();
        if (meta.has("raw")) {
            NBTBase t = CloneNbt.decode(meta.get("raw"), p + "/_nbt/raw");
            if (!(t instanceof NBTTagCompound)) { throw CloneNbt.bad(p + "/_nbt/raw", "Expected compound"); } result = (NBTTagCompound)t;
        }
        Set<String> absent = new HashSet<String>();
        if (meta.has("absent")) {
            for (JsonElement a : CloneNbt.array(meta.get("absent"), p + "/_nbt/absent")) {
                String key = CloneNbt.string(a, p + "/_nbt/absent"); boolean known = false;
                for (Field f : fields) { if (f.fallback != null && aliasPointer(f.alias).equals(key)) { known = true; } }
                if (!known || !absent.add(key)) { throw CloneNbt.bad(p + "/_nbt/absent", "Unknown or repeated default path " + key); }
            }
        }
        for (Field f : fields) {
            JsonObject section = readOwner(doc, f.alias); boolean has = section != null && section.has(leaf(f.alias));
            String fp = p + aliasPointer(f.alias);
            if (has) {
                if (absent.contains(aliasPointer(f.alias))) { throw CloneNbt.bad(fp, "Explicit field conflicts with absent"); }
                if (get(result, f.nbt) != null || blockedParent(result, f.nbt)) { throw CloneNbt.bad(fp, "Readable field conflicts with raw NBT"); }
                NBTBase t = f.kind.equals("lines") ? new NBTTagString(textIn(section, leaf(f.alias), fp)) : parseReadable(section.get(leaf(f.alias)), f.kind, fp);
                put(result, f.nbt, t);
            } else {
                if (f.kind.equals("lines") && section != null && (section.has("lineEnding") || section.has("lineEndings"))) { throw CloneNbt.bad(fp, "Line metadata without text"); }
                if (f.fallback != null && !absent.contains(aliasPointer(f.alias))) {
                    if (get(result, f.nbt) != null || blockedParent(result,f.nbt)) { throw CloneNbt.bad(fp, "Default conflicts with raw; mark it absent"); }
                    put(result, f.nbt, f.fallback.copy());
                }
            }
        }
        return result;
    }
    private static NBTBase parseReadable(JsonElement v, String kind, String p) {
        if (kind.equals("raw")) { return CloneNbt.decode(v,p); }
        if (kind.equals("tabs") || kind.equals("items") || kind.equals("includes")) {
            JsonArray a = CloneNbt.array(v,p); NBTTagList l = new NBTTagList();
            for (int i = 0; i < a.size(); i++) { l.appendTag(importScope(CloneNbt.object(a.get(i),p + "/" + i),subfields(kind),false,p + "/" + i)); } return l;
        }
        if (kind.equals("boolean")) {
            if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isBoolean()) { throw CloneNbt.bad(p,"Expected boolean"); } return new NBTTagByte((byte)(v.getAsBoolean() ? 1 : 0));
        }
        JsonObject o = new JsonObject(); o.addProperty("type",kind); o.add("value",v); return CloneNbt.decode(o,p);
    }
    private static void textOut(JsonObject o, String key, String text) {
        JsonArray lines = new JsonArray(), endings = new JsonArray(); int begin = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r') {
                lines.add(new JsonPrimitive(text.substring(begin,i))); String end;
                if (c == '\r' && i + 1 < text.length() && text.charAt(i+1) == '\n') { i++; end = "CRLF"; } else { end = c == '\r' ? "CR" : "LF"; }
                endings.add(new JsonPrimitive(end)); begin = i + 1;
            }
        }
        lines.add(new JsonPrimitive(text.substring(begin))); o.add(key, lines);
        String common = endings.size() == 0 ? "LF" : endings.get(0).getAsString(); boolean same = true;
        for (JsonElement e : endings) { if (!common.equals(e.getAsString())) { same = false; } }
        if (!same) { o.add("lineEndings", endings); } else if (!common.equals("LF")) { o.addProperty("lineEnding",common); }
    }
    private static String textIn(JsonObject o, String key, String p) {
        JsonArray lines = CloneNbt.array(o.get(key),p); if (lines.size() == 0) { throw CloneNbt.bad(p,"At least one line required"); }
        if (o.has("lineEnding") && o.has("lineEndings")) { throw CloneNbt.bad(p,"Conflicting line metadata"); }
        String common = o.has("lineEnding") ? CloneNbt.string(o.get("lineEnding"),p + "/lineEnding") : "LF";
        ending(common,p);
        JsonArray ends = o.has("lineEndings") ? CloneNbt.array(o.get("lineEndings"),p + "/lineEndings") : null;
        if (ends != null && ends.size() != lines.size() - 1) { throw CloneNbt.bad(p,"Need exactly N-1 line endings"); }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            String s = CloneNbt.string(lines.get(i),p + "/" + i);
            if (s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) { throw CloneNbt.bad(p + "/" + i,"Line contains a separator"); }
            if (i > 0) { b.append(ending(ends == null ? common : CloneNbt.string(ends.get(i-1),p + "/lineEndings/" + (i-1)),p)); } b.append(s);
        }
        return b.toString();
    }
    private static String ending(String e, String p) { if (e.equals("LF")) { return "\n"; } if (e.equals("CRLF")) { return "\r\n"; } if (e.equals("CR")) { return "\r"; } throw CloneNbt.bad(p,"Unknown line ending " + e); }
    private static String group(String alias) { int i = alias.indexOf('.'); return i < 0 ? null : alias.substring(0,i); }
    private static String leaf(String alias) { return alias.substring(alias.indexOf('.') + 1); }
    private static String aliasPointer(String a) { return "/" + a.replace('.', '/'); }
    private static JsonObject owner(JsonObject doc,String alias) { String g = group(alias); if (g == null) { return doc; } if (!doc.has(g)) { doc.add(g,new JsonObject()); } return doc.getAsJsonObject(g); }
    private static JsonObject readOwner(JsonObject doc,String alias) { String g = group(alias); return g == null ? doc : doc.has(g) ? CloneNbt.object(doc.get(g),"/" + g) : null; }
    private static NBTBase get(NBTTagCompound n,String path) {
        int i = path.indexOf('/'); if (i < 0) { return n.getTag(path); } String parent = path.substring(0,i);
        return n.hasKey(parent,10) ? n.getCompoundTag(parent).getTag(path.substring(i+1)) : null;
    }
    private static boolean blockedParent(NBTTagCompound n,String path) { int i = path.indexOf('/'); return i >= 0 && n.hasKey(path.substring(0,i)) && !n.hasKey(path.substring(0,i),10); }
    private static void remove(NBTTagCompound n,String path) { int i = path.indexOf('/'); if (i < 0) { n.removeTag(path); } else { n.getCompoundTag(path.substring(0,i)).removeTag(path.substring(i+1)); } }
    private static void put(NBTTagCompound n,String path,NBTBase v) {
        int i = path.indexOf('/'); if (i < 0) { n.setTag(path,v); } else { String parent = path.substring(0,i); if (!n.hasKey(parent)) { n.setTag(parent,new NBTTagCompound()); } n.getCompoundTag(parent).setTag(path.substring(i+1),v); }
    }
    /** Generated from the very catalog used by import/export. A .md file cannot appear as a clone. */
    public static String guide() {
        StringBuilder b = new StringBuilder("<!-- customnpcs-readable-clone-guide-v1 -->\n# Readable clones v1\n\nUTF-8 JSON. `$format`: `" + FORMAT + "`, `version`: 1, `defaults`: `" + DEFAULTS + "`.\n\n");
        b.append("Missing defaults are restored. `_nbt.absent` is a list of readable JSON Pointers suppressing defaults; it distinguishes missing NBT from explicit defaults. Do not change the profile name.\n\n");
        b.append("| Readable path | NBT path | Type | Frozen default | Meaning / units |\n|---|---|---|---|---|\n");
        for (Field f : FIELDS) { b.append('|').append(f.alias).append('|').append(f.nbt).append('|').append(f.kind).append('|').append(f.fallback == null ? "none" : f.fallback.toString()).append('|').append(f.description).append("|\n"); }
        b.append("\nItems are ordered arrays of objects: `slot` (byte), `item` (string id), `count` (byte), `damage` (short), optional `_nbt.raw` compound for item tags/extensions. Empty [] means empty end-typed list. Other empty element types use raw.\n\n");
        b.append("Script tabs: `sourceLines` array (include last empty line), optional `lineEnding` LF (default), CRLF or CR; mixed lines use `lineEndings` with exactly N-1 entries. Includes are objects with `file`; `console` is typed NBT. `rig.clipJsonLines` uses the same line rules. No source is executed during conversion.\n\n");
        b.append("Unknown/wrong-type data stays in `_nbt.raw`: {type: compound, value: {OriginalKey: {type: int, value: 7}}}. Types: byte/short/int/long/float/double/byteArray/string/list/compound/intArray/longArray. Long and longArray values are decimal strings. Lists carry elementType and typed value entries. Nonfinite floats use raw IEEE hex `bits` (8 or 16 digits). Negative zero is preserved. No duplicate keys, comments, trailing content or alias/raw conflicts. Maximum 32 MiB UTF-8 and JSON depth 512.\n\n");
        b.append("Read both formats at either switch value. RwReadableClones=1 writes readable JSON; 0 writes legacy. Reading never migrates. First explicit legacy overwrite at 1 keeps exact bytes in .json.v0.bak (never replaced). The original mod cannot read v1: restore the backup or explicitly save at 0 before rollback. Backups and this guide do not appear in the clone list.\n");
        return b.toString();
    }
}
