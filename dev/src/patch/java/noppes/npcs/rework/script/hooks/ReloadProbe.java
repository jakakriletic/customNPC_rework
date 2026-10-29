package noppes.npcs.rework.script.hooks;

import static noppes.npcs.rework.script.hooks.HookTraceProbe.*;

import com.google.gson.GsonBuilder;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import noppes.npcs.EventHooks;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataScript;

/** Explicit M6.4 fixture entry point only (java-hooks-run.ps1 -Package m64). Unspawned NPCs driven
 * through the real EventHooks: per NPC/tab/generation loaders, bytecode cache, atomic reload,
 * include invalidation and the chunk save/load NBT path. Restores the include map afterwards.
 */
public final class ReloadProbe {
    private ReloadProbe() { }

    private static final String HEAD = "package m64;\nimport noppes.npcs.api.entity.data.IData;\nimport noppes.npcs.api.event.NpcEvent;\n"
            + "import noppes.npcs.rework.script.NpcScript;\n";
    private static final String TRACE = " static void t(NpcEvent e, String s) { IData d = e.npc.getTempdata();"
            + " d.put(\"t\", (d.has(\"t\") ? d.get(\"t\") : \"\") + s + \";\"); }\n";
    /** Static and instance counter; identical source in two tabs and two NPCs gives one cache key. */
    static final String COUNTER = HEAD + "public class Counter extends NpcScript {\n static int s;\n int f;\n" + TRACE
            + " @Override public void tick(NpcEvent.UpdateEvent e) { s++; f++; t(e, \"c\" + s + \"/\" + f); }\n}\n";
    static final String USE = HEAD + "public class Use extends NpcScript {\n" + TRACE
            + " @Override public void tick(NpcEvent.UpdateEvent e) { t(e, \"v\" + Helper.v()); }\n}\n";
    static final String INCLUDE = "m64/helper.java";
    static String helper(String v) { return "package m64;\npublic class Helper {\n public static String v() { return \"" + v + "\"; }\n}\n"; }
    static final String HELPER_BROKEN = "package m64;\npublic class Helper {\n public static String v() { return 1; }\n}\n";
    static final String SYNTAX = "package m64;\npublic class Broken {\n public void tick(Object e) { int x = ; }\n}\n";

    public static boolean run(ICustomNpc control, String output, String mode) throws Exception {
        Map<String, Object> proof = new LinkedHashMap<>();
        List<Map<String, Object>> checks = new ArrayList<>();
        Map<String, Object> traces = new LinkedHashMap<>();
        Map<String, Object> data = new LinkedHashMap<>();
        proof.put("mode", mode); proof.put("checks", checks); proof.put("traces", traces); proof.put("data", data); proof.put("success", false);
        Map<String, String> includes = ScriptController.Instance.scripts;
        boolean hadInclude = includes.containsKey(INCLUDE);
        String oldInclude = includes.get(INCLUDE);
        try {
            WorldServer world = control.getWorld().getMCWorld();
            long c0 = JavaNpcScripts.cacheStats()[0];
            data.put("cacheBefore", Arrays.toString(JavaNpcScripts.cacheStats()));

            // 1. Two NPCs x two tabs of the same source: one compilation, four loaders, no shared static.
            EntityNPCInterface a = npc(world, ScriptHooks.JAVA, COUNTER, COUNTER);
            EntityNPCInterface b = npc(world, ScriptHooks.JAVA, COUNTER, COUNTER);
            EventHooks.onNPCTick(a); EventHooks.onNPCTick(a); EventHooks.onNPCTick(b);
            traces.put("a", trace(a)); traces.put("b", trace(b));
            check(checks, "two-npcs-two-tabs-static-separate", "c1/1;c1/1;c2/2;c2/2;".equals(trace(a)) && "c1/1;c1/1;".equals(trace(b)));
            List<ClassLoader> loaders = new ArrayList<>(tabs(a).loaders());
            loaders.addAll(tabs(b).loaders());
            check(checks, "own-loader-per-npc-tab", loaders.size() == 4 && distinct(loaders)
                    && tabs(a).instance(0).getClass() != tabs(a).instance(1).getClass()
                    && tabs(a).instance(0).getClass() != tabs(b).instance(0).getClass());
            data.put("cacheAfterTwoNpcs", Arrays.toString(JavaNpcScripts.cacheStats()));
            check(checks, "cache-one-compile-for-four-tabs", JavaNpcScripts.cacheStats()[0] - c0 == 1);

            // 2. Script reload: new generation with new loaders from cached bytes; static state reset.
            List<ClassLoader> before = tabs(a).loaders();
            WeakReference<ClassLoader> old = new WeakReference<>(before.get(0));
            before = null; loaders = null;
            long c1 = JavaNpcScripts.cacheStats()[0];
            clear(a);
            ScriptController.Instance.lastLoaded = ScriptController.Instance.lastLoaded + 1;
            EventHooks.onNPCTick(a);
            traces.put("aReload", trace(a));
            check(checks, "reload-resets-static-and-fields", "c1/1;c1/1;".equals(trace(a)));
            check(checks, "reload-reuses-bytes", JavaNpcScripts.cacheStats()[0] == c1);
            check(checks, "reload-drops-old-loader", !containsSame(tabs(a).loaders(), old.get()) && distinct(tabs(a).loaders()));
            // Data only: a JVM need not collect on request (Q130), so this is not a gate.
            for (int i = 0; i < 3 && old.get() != null; i++) { System.gc(); Thread.sleep(50); }
            data.put("oldLoaderCollectedAfterGc", old.get() == null);

            // 3. A GUI save (new containers, same code) starts a new generation like a new JS engine.
            clear(a);
            NBTTagCompound saved = a.script.writeToNBT(new NBTTagCompound());
            a.script.readFromNBT(saved);
            a.script.lastInited = -1L;
            EventHooks.onNPCTick(a);
            traces.put("aGuiSave", trace(a));
            check(checks, "new-containers-new-generation", "c1/1;c1/1;".equals(trace(a)));

            // 4. Included file: change invalidates, a broken change keeps the last valid generation.
            includes.put(INCLUDE, helper("1"));
            EntityNPCInterface c = npc(world, ScriptHooks.JAVA, COUNTER, USE);
            c.script.getScripts().get(1).scripts.add(INCLUDE);
            EventHooks.onNPCTick(c);
            traces.put("c1", trace(c));
            check(checks, "include-compiles", "c1/1;v1;".equals(trace(c)));
            long c2 = JavaNpcScripts.cacheStats()[0];
            includes.put(INCLUDE, helper("2"));
            clear(c); EventHooks.onNPCTick(c);
            traces.put("c2", trace(c));
            check(checks, "include-change-invalidates", "c1/1;v2;".equals(trace(c)) && JavaNpcScripts.cacheStats()[0] - c2 == 1);
            includes.put(INCLUDE, HELPER_BROKEN);
            clear(c); EventHooks.onNPCTick(c);
            traces.put("cBroken", trace(c)); traces.put("cBrokenConsole", console(c));
            check(checks, "broken-reload-keeps-last-generation", "c2/2;v2;".equals(trace(c))
                    && !c.script.getScripts().get(0).errored && !c.script.getScripts().get(1).errored
                    && console(c).contains("Reload rejected") && console(c).contains("m64/Helper.java:3:"));
            long c3 = JavaNpcScripts.cacheStats()[0];
            clear(c); EventHooks.onNPCTick(c);
            traces.put("cBrokenAgain", trace(c));
            check(checks, "rejected-input-not-recompiled", "c3/3;v2;".equals(trace(c)) && JavaNpcScripts.cacheStats()[0] == c3);
            includes.put(INCLUDE, helper("3"));
            clear(c); EventHooks.onNPCTick(c);
            traces.put("cFixed", trace(c));
            check(checks, "fixed-include-swaps-in", "c1/1;v3;".equals(trace(c)));

            // 5. First load with one broken tab: no tab of the handler runs (all or nothing).
            EntityNPCInterface d = npc(world, ScriptHooks.JAVA, COUNTER, SYNTAX);
            EventHooks.onNPCTick(d); EventHooks.onNPCTick(d);
            traces.put("d", trace(d)); traces.put("dConsole", console(d));
            check(checks, "first-failure-runs-nothing", "".equals(trace(d)) && d.script.getScripts().get(1).errored
                    && !d.script.getScripts().get(0).errored && console(d).contains("Broken.java:3:"));

            // 6. Chunk save/load path: stored data kept, script fields fresh, bytes from the cache.
            a.wrappedNPC.getStoreddata().put("m64", 5);
            NBTTagCompound chunk = new NBTTagCompound();
            boolean written = a.writeToNBTOptional(chunk);
            Entity loaded = EntityList.createEntityFromNBT(chunk, world);
            long c4 = JavaNpcScripts.cacheStats()[0];
            boolean restored = written && loaded instanceof EntityNPCInterface;
            if (restored) {
                EntityNPCInterface a2 = (EntityNPCInterface) loaded;
                EventHooks.onNPCTick(a2);
                Object stored = a2.wrappedNPC.getStoreddata().get("m64");
                traces.put("aRestored", trace(a2));
                restored = "c1/1;c1/1;".equals(trace(a2)) && stored instanceof Number && ((Number) stored).intValue() == 5
                        && ScriptHooks.JAVA.equals(a2.script.getLanguage()) && a2.getUniqueID().equals(a.getUniqueID())
                        && JavaNpcScripts.cacheStats()[0] == c4 && !containsAny(tabs(a2).loaders(), tabs(a).loaders());
            }
            check(checks, "save-load-stored-kept-fields-fresh", restored);
            check(checks, "current-cleared", ScriptContainer.Current == null);
            data.put("cacheAfter", Arrays.toString(JavaNpcScripts.cacheStats()));
            boolean allOk = true;
            for (Map<String, Object> ch : checks) { allOk &= Boolean.TRUE.equals(ch.get("ok")); }
            proof.put("success", allOk);
            return allOk;
        } catch (Throwable failure) {
            proof.put("error", failure.toString());
            failure.printStackTrace();
            return false;
        } finally {
            if (hadInclude) { includes.put(INCLUDE, oldInclude); } else { includes.remove(INCLUDE); }
            Files.write(Paths.get(output), new GsonBuilder().setPrettyPrinting().create().toJson(proof).getBytes(StandardCharsets.UTF_8));
        }
    }

    private static JavaNpcScripts tabs(EntityNPCInterface npc) throws Exception {
        Field f = DataScript.class.getDeclaredField("javaTabs");
        f.setAccessible(true);
        return (JavaNpcScripts) f.get(npc.script);
    }

    private static void clear(EntityNPCInterface npc) { npc.wrappedNPC.getTempdata().remove("t"); }

    private static boolean distinct(List<ClassLoader> loaders) {
        Map<ClassLoader, Boolean> seen = new IdentityHashMap<>();
        for (ClassLoader l : loaders) { if (l == null || seen.put(l, Boolean.TRUE) != null) { return false; } }
        return true;
    }

    private static boolean containsSame(List<ClassLoader> loaders, ClassLoader loader) {
        for (ClassLoader l : loaders) { if (l == loader && loader != null) { return true; } }
        return false;
    }

    private static boolean containsAny(List<ClassLoader> a, List<ClassLoader> b) {
        for (ClassLoader l : b) { if (containsSame(a, l)) { return true; } }
        return false;
    }
}
