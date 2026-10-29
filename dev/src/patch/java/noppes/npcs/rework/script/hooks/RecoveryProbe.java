package noppes.npcs.rework.script.hooks;

import static noppes.npcs.rework.script.hooks.HookTraceProbe.*;

import com.google.gson.GsonBuilder;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.world.WorldServer;
import noppes.npcs.EventHooks;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.controllers.IScriptHandler;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.entity.EntityNPCInterface;

/** Explicit M6.5 fixture entry point only (java-hooks-run.ps1 -Package m65). B5 side by side: the
 * untouched ScriptContainer bytes from the original jar (own child-first loader) and the patched
 * class run the same JS steps through the real reload (loadCategories, as /noppes script reload).
 * Java tabs: mapped compile/runtime origins and recovery after a fix and reload. Restores the
 * script directory and include map afterwards.
 */
public final class RecoveryProbe {
    private RecoveryProbe() { }

    private static final String DIR = "m65";
    private static final String HEAD = "package m65;\nimport noppes.npcs.api.event.NpcEvent;\nimport noppes.npcs.rework.script.NpcScript;\n";
    private static final String TRACE = " static void t(NpcEvent e, String s) { noppes.npcs.api.entity.data.IData d = e.npc.getTempdata();"
            + " d.put(\"t\", (d.has(\"t\") ? d.get(\"t\") : \"\") + s + \";\"); }\n";
    // Line 5 is the hook; Util.v() line 3 throws in its first version.
    static final String HOOKS = HEAD + "public class Hooks extends NpcScript {\n"
            + " @Override public void tick(NpcEvent.UpdateEvent e) { t(e, Util.v()); }\n" + TRACE + "}\n";
    static final String UTIL_THROW = "package m65;\npublic class Util {\n public static String v() { throw new IllegalStateException(\"m65 fixture\"); }\n}\n";
    static final String UTIL_OK = "package m65;\npublic class Util {\n public static String v() { return \"ok\"; }\n}\n";
    static final String MISSING = HEAD + "public class Missing extends NpcScript {\n"
            + " @Override public void tick(NpcEvent.UpdateEvent e) { t(e, \"tick\"); }\n" + TRACE + "}\n";
    static final String MISSING_ADDED = HEAD + "public class Missing extends NpcScript {\n"
            + " @Override public void tick(NpcEvent.UpdateEvent e) { t(e, \"tick\"); }\n"
            + " @Override public void timer(NpcEvent.TimerEvent e) { t(e, \"timer\" + e.id); }\n" + TRACE + "}\n";
    static final String MAIN = HEAD + "public class Main extends NpcScript {\n"
            + " @Override public void tick(NpcEvent.UpdateEvent e) { t(e, \"p\" + Part.v()); }\n" + TRACE + "}\n";
    static final String PART_BROKEN = "package m65;\npublic class Part {\n static String v() { return 1; }\n}\n";
    static final String PART_OK = "package m65;\npublic class Part {\n static String v() { return \"1\"; }\n}\n";
    static final String INLINE_SYNTAX = "package m65;\npublic class Inline {\n public void tick(Object e) { int x = ; }\n}\n";

    public static boolean run(ICustomNpc control, String output, String mode, String originalJar) throws Exception {
        Map<String, Object> proof = new LinkedHashMap<>();
        List<Map<String, Object>> checks = new ArrayList<>();
        Map<String, Object> traces = new LinkedHashMap<>();
        proof.put("mode", mode); proof.put("checks", checks); proof.put("traces", traces); proof.put("success", false);
        Map<String, String> includes = ScriptController.Instance.scripts;
        ScriptContainer caller = ScriptContainer.Current;
        File jsDir = null;
        try {
            WorldServer world = control.getWorld().getMCWorld();

            // 1. Java: a runtime error names its include origins, stops the tab until a reload, then recovers.
            EntityNPCInterface r = javaNpc(world, "", "m65/hooks.java", "m65/util.java");
            includes.put("m65/hooks.java", HOOKS); includes.put("m65/util.java", UTIL_THROW);
            EventHooks.onNPCTick(r); EventHooks.onNPCTick(r);
            traces.put("javaRuntime", trace(r)); traces.put("javaRuntimeConsole", console(r));
            check(checks, "java-runtime-mapped-stack", console(r).contains("Runtime error in hook tick:\njava.lang.IllegalStateException: m65 fixture")
                    && console(r).contains("at m65.Util.v(Util.java:3) [include m65/util.java]")
                    && console(r).contains("at m65.Hooks.tick(Hooks.java:5) [include m65/hooks.java]"));
            check(checks, "java-runtime-stops-until-reload", "".equals(trace(r)) && tab(r, 0).errored);
            includes.put("m65/util.java", UTIL_OK);
            reloadJava();
            EventHooks.onNPCTick(r);
            traces.put("javaRecovered", trace(r));
            check(checks, "java-runtime-recovers-after-reload", "ok;".equals(trace(r)) && !tab(r, 0).errored);

            // 2. Java: a hook missing in the old generation runs after the include adds it (only that event).
            EntityNPCInterface m = javaNpc(world, "", "m65/missing.java");
            includes.put("m65/missing.java", MISSING);
            EventHooks.onNPCTick(m); EventHooks.onNPCTimer(m, 7);
            includes.put("m65/missing.java", MISSING_ADDED);
            reloadJava();
            EventHooks.onNPCTimer(m, 7);
            traces.put("javaMissing", trace(m));
            check(checks, "java-missing-hook-after-reload", "tick;timer7;".equals(trace(m)) && !tab(m, 0).errored);

            // 3. Java: first compile failure in an include names file:line:column and the include; a fix recovers.
            EntityNPCInterface p = javaNpc(world, MAIN, "m65/part.java");
            includes.put("m65/part.java", PART_BROKEN);
            EventHooks.onNPCTick(p);
            traces.put("javaCompileConsole", console(p));
            check(checks, "java-compile-include-origin", "".equals(trace(p)) && tab(p, 0).errored
                    && Pattern.compile("ERROR m65/Part\\.java:3:\\d+ \\[include m65/part\\.java\\] compiler\\.err\\.").matcher(console(p)).find());
            includes.put("m65/part.java", PART_OK);
            reloadJava();
            EventHooks.onNPCTick(p);
            traces.put("javaCompileFixed", trace(p));
            check(checks, "java-first-failure-recovers", "p1;".equals(trace(p)) && !tab(p, 0).errored);
            EntityNPCInterface s = javaNpc(world, INLINE_SYNTAX);
            EventHooks.onNPCTick(s);
            traces.put("javaInlineConsole", console(s));
            check(checks, "java-compile-inline-origin", tab(s, 0).errored
                    && Pattern.compile("ERROR m65/Inline\\.java:3:\\d+ \\[tab 1\\] compiler\\.err\\.").matcher(console(s)).find());

            // 4. JS B5 side by side through the real script reload; the include map is reloaded from disk.
            ScriptController.Instance.loadCategories();
            jsDir = new File(ScriptController.Instance.dir, "ecmascript/" + DIR);
            IScriptHandler handler = npc(world, "ECMAScript").script;
            Class<?> original = originalContainer(originalJar);
            check(checks, "original-container-bytes", original != ScriptContainer.class && original.getClassLoader() != ScriptContainer.class.getClassLoader());
            Map<String, Object> js = new LinkedHashMap<>();
            for (Object[] impl : new Object[][] { { "o", original }, { "f", ScriptContainer.class } }) {
                String id = (String) impl[0];
                Class<?> k = (Class<?>) impl[1];
                js.put(id + "Unknown", unknownHook(jsDir, k, handler, id));
                Js e = new Js(k, handler);
                js.put(id + "Errored", errored(jsDir, e, id));
                js.put(id + "ErroredFlag", e.errored());
                js.put(id + "ErroredConsole", e.console());
            }
            Js syntax = new Js(ScriptContainer.class, handler);
            write(jsDir, "fs.js", "function tick(e) {\n  var x = ;\n}");
            reload();
            syntax.include("m65/fs.js");
            syntax.run("tick", new ArrayList<String>());
            js.put("fSyntaxConsole", syntax.console());
            traces.putAll(js);
            check(checks, "js-original-b5-unknown-hook-skipped", "tick;|;tick;interact;".equals(js.get("oUnknown")));
            check(checks, "js-fixed-unknown-hook-after-reload", "tick;|;interact;tick;interact;".equals(js.get("fUnknown")));
            check(checks, "js-original-b5-errored-stays", "boom;|;|;".equals(js.get("oErrored")) && Boolean.TRUE.equals(js.get("oErroredFlag")));
            check(checks, "js-fixed-errored-recovers-only-after-reload", "boom;|;|;ok;".equals(js.get("fErrored")) && Boolean.FALSE.equals(js.get("fErroredFlag")));
            check(checks, "js-runtime-source-map", ((String) js.get("fErroredConsole")).contains("Script source: m65/fe.js:3"));
            check(checks, "js-syntax-source-map", syntax.errored() && syntax.console().contains("Script source: m65/fs.js:2"));
            // M6.6 restores the live controller, rather than clearing a nested caller.
            check(checks, "current-caller-restored", caller != null
                    && ((EntityNPCInterface) control.getMCEntity()).script.getScripts().contains(caller)
                    && ScriptContainer.Current == caller);
            check(checks, "current-top-level-js-cleared", topLevelCleared(world, "ECMAScript", JS_ONE,
                    JS_ONE.replace("t(e,'1:tick');", "throw new Error('m62 fixture');")));
            check(checks, "current-top-level-java-cleared", topLevelCleared(world, ScriptHooks.JAVA, JAVA_ONE, JAVA_THROW));
            boolean allOk = true;
            for (Map<String, Object> c : checks) { allOk &= Boolean.TRUE.equals(c.get("ok")); }
            proof.put("success", allOk);
            return allOk;
        } catch (Throwable failure) {
            proof.put("error", failure.toString());
            failure.printStackTrace();
            return false;
        } finally {
            if (jsDir != null) {
                File[] files = jsDir.listFiles();
                if (files != null) { for (File f : files) { f.delete(); } }
                jsDir.delete();
            }
            // Reload from disk: drops the fixture includes (JS files and the Java map entries).
            ScriptController.Instance.loadCategories();
            Files.write(Paths.get(output), new GsonBuilder().setPrettyPrinting().create().toJson(proof).getBytes(StandardCharsets.UTF_8));
        }
    }

    /** Supply a null entry owner, run normal and throwing events through the real handler,
     * then restore the suspended controller even when fixture setup fails. */
    private static boolean topLevelCleared(WorldServer world, String language, String normal, String throwing) {
        synchronized ("lock") {
            ScriptContainer caller = ScriptContainer.Current;
            try {
                ScriptContainer.Current = null;
                EntityNPCInterface npc = npc(world, language, normal);
                EventHooks.onNPCTick(npc);
                boolean ok = ScriptContainer.Current == null && !tab(npc, 0).errored
                        && "1:init:1;1:tick;".equals(trace(npc));
                npc = npc(world, language, throwing);
                EventHooks.onNPCTick(npc);
                return ok && ScriptContainer.Current == null && tab(npc, 0).errored
                        && "1:init:1;".equals(trace(npc)) && console(npc).contains("m62 fixture");
            } finally {
                ScriptContainer.Current = caller;
            }
        }
    }

    /** Fresh include, one hook called before and only the previously missing hook after the reload. */
    private static String unknownHook(File dir, Class<?> k, IScriptHandler handler, String id) throws Exception {
        String file = id + "u.js";
        write(dir, file, "function tick(e) { e.add('tick'); }");
        reload();
        Js c = new Js(k, handler);
        c.include(DIR + "/" + file);
        List<String> ev = new ArrayList<>();
        c.run("tick", ev); c.run("interact", ev);
        write(dir, file, "function tick(e) { e.add('tick'); }\nfunction interact(e) { e.add('interact'); }");
        reload();
        ev.add("|");
        c.run("interact", ev);
        c.run("tick", ev); c.run("interact", ev);
        return join(ev);
    }

    /** Runtime error at include line 3 (joined line 5), a tick without reload, then fixed file + reload. */
    private static String errored(File dir, Js c, String id) throws Exception {
        String file = id + "e.js";
        write(dir, file, "function tick(e) {\n  e.add('boom');\n  throw new Error('m65 fixture');\n}");
        reload();
        c.script("var a = 1;\nvar b = 2;");
        c.include(DIR + "/" + file);
        List<String> ev = new ArrayList<>();
        c.run("tick", ev); c.run("tick", ev);
        write(dir, file, "function tick(e) { e.add('ok'); }");
        ev.add("|");
        c.run("tick", ev);
        reload();
        ev.add("|");
        c.run("tick", ev);
        return join(ev);
    }

    /** One reflective view over both the original and the patched container class. */
    private static final class Js {
        final Class<?> k;
        final Object c;
        Js(Class<?> k, IScriptHandler handler) throws Exception { this.k = k; this.c = k.getConstructor(IScriptHandler.class).newInstance(handler); }
        @SuppressWarnings("unchecked")
        void include(String name) throws Exception { ((List<String>) k.getField("scripts").get(c)).add(name); }
        void script(String code) throws Exception { k.getField("script").set(c, code); }
        void run(String hook, Object event) throws Exception { k.getMethod("run", String.class, Object.class).invoke(c, hook, event); }
        boolean errored() throws Exception { return k.getField("errored").getBoolean(c); }
        String console() throws Exception {
            StringBuilder out = new StringBuilder();
            for (Object line : ((Map<?, ?>) k.getField("console").get(c)).values()) { out.append(line).append('\n'); }
            return out.toString();
        }
    }

    /** Untouched ScriptContainer (+ inner classes) from the original jar; everything else from the game loader. */
    static Class<?> originalContainer(String jar) throws Exception {
        final Map<String, byte[]> bytes = new HashMap<>();
        try (ZipFile zip = new ZipFile(jar)) {
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();) {
                ZipEntry entry = e.nextElement();
                String name = entry.getName();
                if (!name.startsWith("noppes/npcs/controllers/ScriptContainer") || !name.endsWith(".class")) { continue; }
                try (InputStream in = zip.getInputStream(entry)) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    for (int n; (n = in.read(buffer)) > 0;) { out.write(buffer, 0, n); }
                    bytes.put(name.substring(0, name.length() - 6).replace('/', '.'), out.toByteArray());
                }
            }
        }
        if (!bytes.containsKey(ScriptContainer.class.getName())) { throw new IllegalStateException("No ScriptContainer in " + jar); }
        ClassLoader loader = new ClassLoader(ScriptContainer.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                byte[] b = bytes.get(name);
                if (b == null) { return super.loadClass(name, resolve); }
                synchronized (getClassLoadingLock(name)) {
                    Class<?> c = findLoadedClass(name);
                    if (c == null) { c = defineClass(name, b, 0, b.length); }
                    if (resolve) { resolveClass(c); }
                    return c;
                }
            }
        };
        return loader.loadClass(ScriptContainer.class.getName());
    }

    private static EntityNPCInterface javaNpc(WorldServer world, String code, String... tabIncludes) {
        EntityNPCInterface npc = npc(world, ScriptHooks.JAVA, code);
        for (String include : tabIncludes) { tab(npc, 0).scripts.add(include); }
        return npc;
    }

    private static ScriptContainer tab(EntityNPCInterface npc, int i) { return npc.script.getScripts().get(i); }

    /** Java includes come from the map; a script reload advances lastLoaded like loadCategories. */
    private static void reloadJava() { ScriptController.Instance.lastLoaded = ScriptController.Instance.lastLoaded + 1; }

    /** /noppes script reload; lastLoaded is wall-clock milliseconds, so keep reloads apart. */
    private static void reload() throws InterruptedException {
        Thread.sleep(5);
        ScriptController.Instance.loadCategories();
    }

    private static void write(File dir, String file, String code) throws Exception {
        if (!dir.isDirectory() && !dir.mkdirs()) { throw new IllegalStateException("Cannot create " + dir); }
        Files.write(new File(dir, file).toPath(), code.getBytes(StandardCharsets.UTF_8));
    }

    private static String join(List<String> events) {
        StringBuilder out = new StringBuilder();
        for (String e : events) { out.append(e).append(';'); }
        return out.toString();
    }
}
