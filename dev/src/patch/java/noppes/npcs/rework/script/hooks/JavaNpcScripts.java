package noppes.npcs.rework.script.hooks;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraftforge.fml.common.eventhandler.Event;
import noppes.npcs.CustomNpcs;
import noppes.npcs.LogWriter;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.IScriptHandler;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.rework.script.JdkScriptCompiler;

/** M6.2 (D-090, Q142-Q145): Java branch of one NPC script handler, selected by ScriptLanguage=Java.
 * Same tabs, order, event instance and console as ScriptContainer.run; the caller (DataScript)
 * keeps init-on-reload and EventHooks keeps the bus post after it. Trusted authors only (M6.8):
 * a loader is not a sandbox. Bytecode cache and atomic reload are M6.4, typed base M6.3.
 */
public final class JavaNpcScripts {
    /** Same interned literal as ScriptContainer's private monitor, so JS and Java tabs never
     * run concurrently and ScriptContainer.Current keeps one owner (chest/projectile wrappers). */
    private static final String LOCK = "lock";
    private static JdkScriptCompiler compiler;

    private static final class Tab {
        final long generation;
        final Map<String, String> units;
        final Object instance;
        final Map<String, Method> hooks;
        Tab(long generation, Map<String, String> units, Object instance, Map<String, Method> hooks) {
            this.generation = generation; this.units = units; this.instance = instance; this.hooks = hooks;
        }
    }

    // ScriptContainer has identity equality; a GUI save creates new containers, so old tabs drop out.
    private final Map<ScriptContainer, Tab> tabs = new WeakHashMap<>();

    public void run(ScriptContainer container, IScriptHandler handler, EnumScriptType type, Event event) {
        if (container.errored || !CustomNpcs.EnableScripting) { return; }
        Map<String, String> sources = sources(container);
        if (sources.isEmpty()) { return; }
        long generation = ScriptController.Instance.lastLoaded;
        Tab tab = tabs.get(container);
        if (tab == null || tab.generation < generation || !tab.units.equals(sources)) {
            tab = load(container, handler, sources, generation);
            if (tab == null) { return; }
            tabs.put(container, tab);
        }
        Method hook = tab.hooks.get(type.function);
        if (hook == null) { return; }
        synchronized (LOCK) {
            ScriptContainer.Current = container;
            try {
                hook.invoke(tab.instance, event);
            } catch (InvocationTargetException e) {
                fail(container, handler, stack(e.getCause()));
            } catch (Throwable e) {
                fail(container, handler, stack(e));
            } finally {
                ScriptContainer.Current = null;
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

    private Tab load(ScriptContainer container, IScriptHandler handler, Map<String, String> sources, long generation) {
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
            JdkScriptCompiler.Result result = compiler().compile(units, classpath);
            for (JdkScriptCompiler.Problem p : result.problems) {
                report.append(p.kind).append(' ').append(p.file.replace("memory:///", "")).append(':').append(p.line)
                        .append(':').append(p.column).append(' ').append(p.code).append(": ").append(p.message).append('\n');
            }
            if (!result.success) { fail(container, handler, report.toString().trim()); return null; }
            ClassLoader loader = result.newLoader(ICustomNpc.class.getClassLoader());
            Class<?> script = loader.loadClass(main);
            Map<String, Method> hooks = JavaHookBinder.bind(script, ScriptHooks.npc());
            Object instance = script.getConstructor().newInstance();
            if (report.length() > 0) { container.appandConsole(report.toString().trim()); }
            return new Tab(generation, sources, instance, hooks);
        } catch (JavaHookBinder.BindException e) {
            fail(container, handler, e.code + ": " + e.getMessage());
        } catch (InvocationTargetException e) {
            fail(container, handler, stack(e.getCause()));
        } catch (Throwable e) {
            fail(container, handler, stack(e));
        }
        return null;
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

    private static void fail(ScriptContainer container, IScriptHandler handler, String message) {
        container.errored = true;
        container.appandConsole(message);
        LogWriter.info("Java script errored: " + message);
        NoppesUtilServer.NotifyOPs(handler.noticeString() + " script errored", new Object[0]);
    }

    private static String stack(Throwable e) {
        StringWriter out = new StringWriter();
        e.printStackTrace(new PrintWriter(out));
        return out.toString().trim();
    }
}
