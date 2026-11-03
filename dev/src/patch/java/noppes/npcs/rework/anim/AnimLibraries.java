package noppes.npcs.rework.anim;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import noppes.npcs.entity.EntityNPCInterface;

/** P7c (D-098, Q275): reflective bridge to AnimLibrary of customnpcentities 0.7.0, no compile dependency.
 * A snapshot is a new library from the original private constructor (jar mob section) with its clips
 * replaced; the static cache, the entity field and the jar are never written. */
public final class AnimLibraries {
    private static final String MOD = "customnpcentities", LIB = "dev.jakak.customnpcentities.anim.AnimLibrary",
            CLIP = "dev.jakak.customnpcentities.anim.AnimClip";

    private AnimLibraries() { }

    private static String path(String id) { return "/assets/" + MOD + "/animations/" + id + ".animation.json"; }

    private static Class<?> lib() throws ClassNotFoundException { return Class.forName(LIB); }

    static boolean available() {
        try { lib(); return true; }
        catch (ClassNotFoundException | LinkageError absent) { return false; }
    }

    static boolean known(String id) throws ClassNotFoundException {
        return ReloadCodec.validId(id) && lib().getResource(path(id)) != null;
    }

    private static Object jar(String id) throws ReflectiveOperationException {
        return lib().getMethod("get", String.class, String.class).invoke(null, MOD, id);
    }

    /** Clips the jar's mob attack table of this entity names. */
    static List<String> required(String id) throws ReflectiveOperationException {
        Object mob = lib().getMethod("mob").invoke(jar(id));
        List<String> out = new ArrayList<>();
        if (mob == null) { return out; }
        for (Object attack : (Collection<?>) mob.getClass().getField("actions").get(mob)) {
            out.add((String) attack.getClass().getField("clip").get(attack));
        }
        return out;
    }

    /** @return null when all files are valid for their entity, otherwise {@code <id>: <path>: <reason>} */
    static String check(Map<String, byte[]> files) throws ReflectiveOperationException {
        for (Map.Entry<String, byte[]> file : files.entrySet()) {
            if (!known(file.getKey())) { return file.getKey() + ": unknown entity id"; }
            String problem = AnimFileValidator.check(new String(file.getValue(), StandardCharsets.UTF_8), required(file.getKey()));
            if (problem != null) { return file.getKey() + ": " + problem; }
        }
        return null;
    }

    /** One logical side's immutable table: jar library to snapshot. Nothing is published here. */
    static Map<Object, Object> build(Map<String, byte[]> files) throws ReflectiveOperationException {
        Map<Object, Object> table = new IdentityHashMap<>();
        Constructor<?> library = lib().getDeclaredConstructor(String.class);
        library.setAccessible(true);
        Field clips = lib().getDeclaredField("clips");
        clips.setAccessible(true);
        Constructor<?> clip = Class.forName(CLIP).getDeclaredConstructor(String.class, JsonObject.class);
        clip.setAccessible(true);
        for (Map.Entry<String, byte[]> file : files.entrySet()) {
            Object snapshot = library.newInstance(path(file.getKey()));
            @SuppressWarnings("unchecked") Map<String, Object> target = (Map<String, Object>) clips.get(snapshot);
            target.clear();
            JsonObject root = new JsonParser().parse(new String(file.getValue(), StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("animations").entrySet()) {
                target.put(e.getKey(), clip.newInstance(e.getKey(), e.getValue().getAsJsonObject()));
            }
            table.put(jar(file.getKey()), snapshot);
        }
        return table;
    }

    /** Ids whose file differs between two override states. */
    static Set<String> changed(Map<String, byte[]> before, Map<String, byte[]> after) {
        Set<String> out = new HashSet<>(before.keySet());
        out.addAll(after.keySet());
        out.removeIf(id -> before.containsKey(id) == after.containsKey(id)
                && (!before.containsKey(id) || Arrays.equals(before.get(id), after.get(id))));
        return out;
    }

    /** Harness: the clip names in the optional mod's static jar cache, which a reload must never change. */
    public static List<String> jarClipNames(String id) throws ReflectiveOperationException {
        Object names = lib().getMethod("sortedClipNames").invoke(jar(id));
        return new ArrayList<>((Collection<String>) names);
    }

    /** Stops the running action of every loaded NPC of these ids; on the server also its script queue.
     * @return number of NPCs that were playing */
    static int stop(World world, Set<String> ids) throws ReflectiveOperationException {
        if (ids.isEmpty()) { return 0; }
        Class<?> base = Class.forName(Animators.ANIMATED);
        Method entityId = base.getMethod("entityId"), action = base.getMethod("getAction"), stop = base.getDeclaredMethod("stopAction");
        stop.setAccessible(true);
        int stopped = 0;
        for (Entity entity : new ArrayList<>(world.loadedEntityList)) {
            if (!base.isInstance(entity) || !ids.contains(entityId.invoke(entity))) { continue; }
            if (!world.isRemote && entity instanceof EntityNPCInterface) { Animators.clearQueue((EntityNPCInterface) entity); }
            if (((Number) action.invoke(entity)).intValue() != 0) { stop.invoke(entity); stopped++; }
        }
        return stopped;
    }
}
