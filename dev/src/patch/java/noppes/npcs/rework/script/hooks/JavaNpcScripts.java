package noppes.npcs.rework.script.hooks;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraftforge.fml.common.eventhandler.Event;
import noppes.npcs.CustomNpcs;
import noppes.npcs.LogWriter;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.IScriptHandler;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.rework.script.BytecodeCache;
import noppes.npcs.rework.script.JdkScriptCompiler;

/** M6.2 (D-090, Q142-Q145): Java branch of one NPC script handler, selected by ScriptLanguage=Java.
 * Same tabs, order, event instance and console as ScriptContainer.run; the caller (DataScript)
 * keeps init-on-reload and EventHooks keeps the bus post after it. Trusted authors only (M6.8):
 * a loader is not a sandbox.
 * M6.4 (Q130, Q151-Q154): one generation per handler holds an own loader and instance per tab, so
 * static script state is never shared between NPCs, tabs or generations; only bytes are cached.
 * A script reload or a changed tab/include prepares a complete new generation before the event and
 * swaps it in only when every tab compiled, bound and instantiated. Otherwise the last valid
 * generation keeps running with the error in the tab console; without one nothing runs (M6.2).
 */
public final class JavaNpcScripts {
    /** Same interned literal as ScriptContainer's private monitor, so JS and Java tabs never
     * run concurrently and ScriptContainer.Current keeps one owner (chest/projectile wrappers). */
    private static final String LOCK = "lock";
    private static final BytecodeCache CACHE = new BytecodeCache(64, 16L << 20);
    private static JdkScriptCompiler compiler;
    private static long compiles;

    private static final class Tab {
        final ClassLoader loader;
        final Object instance;
        final Map<String, Method> hooks;
        Tab(ClassLoader loader, Object instance, Map<String, Method> hooks) {
            this.loader = loader; this.instance = instance; this.hooks = hooks;
        }
    }

    private static final class Generation {
        final long loaded;
        /** A GUI save replaces the containers; like a new JS engine, that starts a new generation. */
        final ScriptContainer[] owners;
        final List<Map<String, String>> sources;
        final Tab[] tabs;
        Generation(long loaded, List<ScriptContainer> owners, List<Map<String, String>> sources, Tab[] tabs) {
            this.loaded = loaded; this.owners = owners.toArray(new ScriptContainer[0]); this.sources = sources; this.tabs = tabs;
        }
        boolean matches(long loaded, List<ScriptContainer> owners, List<Map<String, String>> sources) {
            if (this.loaded != loaded || this.owners.length != owners.size() || !this.sources.equals(sources)) { return false; }
            for (int i = 0; i < this.owners.length; i++) { if (this.owners[i] != owners.get(i)) { return false; } }
            return true;
        }
    }

    /** The only references to script loaders and instances; replacing it drops the old generation. */
    private Generation current;
    /** Last rejected input, so an unchanged broken source is not recompiled on every event. */
    private Generation rejected;

    public void run(List<ScriptContainer> containers, IScriptHandler handler, EnumScriptType type, Event event) {
        if (!CustomNpcs.EnableScripting) { return; }
        long loaded = ScriptController.Instance.lastLoaded;
        List<Map<String, String>> sources = new ArrayList<>();
        for (ScriptContainer container : containers) { sources.add(sources(container)); }
        if ((current == null || !current.matches(loaded, containers, sources))
                && (rejected == null || !rejected.matches(loaded, containers, sources))) {
            Generation next = prepare(containers, handler, sources, loaded);
            if (next != null) { current = next; rejected = null; }
            else { rejected = new Generation(loaded, containers, sources, new Tab[0]); }
        }
        if (current == null) { return; }
        for (int i = 0; i < containers.size() && i < current.tabs.length; i++) {
            ScriptContainer container = containers.get(i);
            Tab tab = current.tabs[i];
            if (tab == null || container.errored) { continue; }
            Method hook = tab.hooks.get(type.function);
            if (hook == null) { continue; }
            synchronized (LOCK) {
                ScriptContainer.Current = container;
                try {
                    hook.invoke(tab.instance, event);
                } catch (InvocationTargetException e) {
                    fail(container, handler, stack(e.getCause()), true);
                } catch (Throwable e) {
                    fail(container, handler, stack(e), true);
                } finally {
                    ScriptContainer.Current = null;
                }
            }
        }
    }

    /** Inline tab code first, then included files in ScriptList order; each is one compilation unit. */
    private static Map<String, String> sources(ScriptContainer container) {
        Map<String, String> sources = new LinkedHashMap<>();
        if (!container.script.isEmpty()) { sources.put("", container.script); }
        for (String include : container.scripts) {
            String code = ScriptController.Instance.scripts.get(include);
            if (code != null && !code.isEmpty()) { sources.put(include, code); }
        }
        return sources;
    }

    /** All tabs or none. A failing tab is marked errored only when no valid generation exists yet. */
    private Generation prepare(List<ScriptContainer> containers, IScriptHandler handler, List<Map<String, String>> sources, long loaded) {
        Tab[] tabs = new Tab[containers.size()];
        for (int i = 0; i < tabs.length; i++) {
            if (sources.get(i).isEmpty()) { continue; }
            tabs[i] = load(containers.get(i), handler, sources.get(i), current == null);
            if (tabs[i] == null) { return null; }
        }
        return new Generation(loaded, containers, Collections.unmodifiableList(sources), tabs);
    }

    private static Tab load(ScriptContainer container, IScriptHandler handler, Map<String, String> sources, boolean first) {
        StringBuilder report = new StringBuilder();
        try {
            Map<String, String> units = new LinkedHashMap<>();
            String main = null;
            for (Map.Entry<String, String> source : sources.entrySet()) {
                String name = JavaHookBinder.unitName(source.getValue());
                if (units.put(name, source.getValue()) != null) {
                    throw new JavaHookBinder.BindException("unit.duplicate", "Type " + name + " is declared twice in this tab");
                }
                if (main == null) { main = name; }
            }
            String classpath = classpath();
            String key = BytecodeCache.key(compiler().identity(), classpath, units);
            Map<String, byte[]> code = CACHE.get(key);
            if (code == null) {
                JdkScriptCompiler.Result result = compiler().compile(units, classpath);
                synchronized (JavaNpcScripts.class) { compiles++; }
                for (JdkScriptCompiler.Problem p : result.problems) {
                    report.append(p.kind).append(' ').append(p.file.replace("memory:///", "")).append(':').append(p.line)
                            .append(':').append(p.column).append(' ').append(p.code).append(": ").append(p.message).append('\n');
                }
                if (!result.success) { fail(container, handler, report.toString().trim(), first); return null; }
                code = result.bytecode();
                CACHE.put(key, code);
            }
            ClassLoader loader = JdkScriptCompiler.newLoader(code, ICustomNpc.class.getClassLoader());
            Class<?> script = loader.loadClass(main);
            Map<String, Method> hooks = JavaHookBinder.bind(script, ScriptHooks.npc());
            Object instance = script.getConstructor().newInstance();
            if (report.length() > 0) { container.appandConsole(report.toString().trim()); }
            return new Tab(loader, instance, hooks);
        } catch (JavaHookBinder.BindException e) {
            fail(container, handler, e.code + ": " + e.getMessage(), first);
        } catch (InvocationTargetException e) {
            fail(container, handler, stack(e.getCause()), first);
        } catch (Throwable e) {
            fail(container, handler, stack(e), first);
        }
        return null;
    }

    /** Probe/diagnostic view: loader of each tab in the current generation (null for an empty tab). */
    List<ClassLoader> loaders() {
        List<ClassLoader> out = new ArrayList<>();
        if (current != null) { for (Tab tab : current.tabs) { out.add(tab == null ? null : tab.loader); } }
        return out;
    }

    Object instance(int tab) { return current == null || current.tabs[tab] == null ? null : current.tabs[tab].instance; }

    /** compiles, then BytecodeCache.stats(): entries, bytes, hits, misses, evictions. */
    static synchronized long[] cacheStats() {
        long[] cache = CACHE.stats();
        long[] out = new long[cache.length + 1];
        out[0] = compiles;
        System.arraycopy(cache, 0, out, 1, cache.length);
        return out;
    }

    private static synchronized JdkScriptCompiler compiler() {
        if (compiler == null) { compiler = new JdkScriptCompiler(); }
        return compiler;
    }

    /** RwJavaClasspath: a file holding the compiler classpath, or the classpath itself. */
    private static String classpath() throws Exception {
        String value = CustomNpcs.RwJavaClasspath.trim();
        if (value.isEmpty()) {
            throw new JavaHookBinder.BindException("java.classpath.unset",
                    "Set RwJavaClasspath in CustomNpcs.cfg to the API/game classpath for Java scripts; JS is unchanged");
        }
        Path file = Paths.get(value);
        return Files.isRegularFile(file) ? new String(Files.readAllBytes(file), StandardCharsets.UTF_8).trim() : value;
    }

    /** errored=false keeps the tab of the still running previous generation alive. */
    private static void fail(ScriptContainer container, IScriptHandler handler, String message, boolean errored) {
        if (errored) { container.errored = true; }
        container.appandConsole(errored ? message : "Reload rejected, previous Java generation kept:\n" + message);
        LogWriter.info("Java script errored: " + message);
        NoppesUtilServer.NotifyOPs(handler.noticeString() + " script errored", new Object[0]);
    }

    private static String stack(Throwable e) {
        StringWriter out = new StringWriter();
        e.printStackTrace(new PrintWriter(out));
        return out.toString().trim();
    }
}
