package noppes.npcs.rework.script;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.rework.script.hooks.JavaHookBinder;
import noppes.npcs.rework.script.hooks.ScriptHooks;
import org.junit.Test;

public class NpcScriptTest {
    private static final String HEAD = "import noppes.npcs.api.event.NpcEvent;\nimport noppes.npcs.rework.script.NpcScript;\n";

    /** Test workers need not expose the API jar or patch classes on java.class.path; add the jars of the types used. */
    private static String classpath() throws Exception {
        StringBuilder cp = new StringBuilder(System.getProperty("java.class.path"));
        for (Class<?> c : new Class<?>[] { NpcEvent.class, net.minecraftforge.fml.common.eventhandler.Event.class, NpcScript.class }) {
            cp.append(java.io.File.pathSeparator).append(new java.io.File(c.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath());
        }
        return cp.toString();
    }

    private static JdkScriptCompiler.Result compile(String source) throws Exception {
        String name = JavaHookBinder.unitName(source);
        return new JdkScriptCompiler().compile(Collections.singletonMap(name, source), classpath());
    }

    private static Class<?> load(String source) throws Exception {
        JdkScriptCompiler.Result r = compile(source);
        assertTrue(String.valueOf(r.problems.isEmpty() ? "" : r.problems.get(0).message), r.success);
        return r.newLoader(NpcScriptTest.class.getClassLoader()).loadClass(JavaHookBinder.unitName(source));
    }

    @Test public void contractHasExactlyTheRegistryHooks() throws Exception {
        int hooks = 0;
        for (Method m : NpcScript.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(m.getModifiers())) { continue; }
            ScriptHooks.Hook hook = ScriptHooks.java(m.getName());
            assertNotNull("public method is a registry hook: " + m, hook);
            assertArrayEquals(m.toString(), new Class<?>[] { hook.event }, m.getParameterTypes());
            assertEquals(void.class, m.getReturnType());
            assertFalse("overridable", Modifier.isFinal(m.getModifiers()) || Modifier.isStatic(m.getModifiers()));
            hooks++;
        }
        assertEquals(ScriptHooks.java().size(), hooks);
        for (ScriptHooks.Hook hook : ScriptHooks.java()) { assertNotNull(NpcScript.class.getMethod(hook.name, hook.event)); }
    }

    @Test public void onlyOverridesAreBound() throws Exception {
        Class<?> c = load(HEAD + "public class Tab extends NpcScript {\n"
                + " @Override public void tick(NpcEvent.UpdateEvent e) { log(\"t\"); }\n"
                + " @Override public void interact(NpcEvent.InteractEvent e) { e.setCanceled(true); }\n"
                + " @Override public void timer(NpcEvent.TimerEvent e) { log(e.id); }\n"
                + " public void helper(int unrelated) { }\n}");
        Map<String, Method> bound = JavaHookBinder.bind(c, ScriptHooks.npc());
        assertEquals("[tick, interact, timer]", sortedByRegistry(bound).toString());
        for (Method m : bound.values()) { assertEquals(c, m.getDeclaringClass()); }
        assertTrue(JavaHookBinder.bind(load(HEAD + "public class Empty extends NpcScript { }"), ScriptHooks.npc()).isEmpty());
    }

    private static ArrayList<String> sortedByRegistry(Map<String, Method> bound) {
        ArrayList<String> names = new ArrayList<>();
        for (ScriptHooks.Hook hook : ScriptHooks.npc()) { if (bound.containsKey(hook.name)) { names.add(hook.name); } }
        return names;
    }

    @Test public void overrideWithWrongEventTypeIsRejectedByCompiler() throws Exception {
        JdkScriptCompiler.Result r = compile(HEAD + "public class Wrong extends NpcScript {\n"
                + " @Override public void tick(NpcEvent.TimerEvent e) { }\n}");
        assertFalse(r.success);
        assertEquals("compiler.err.method.does.not.override.superclass", r.problems.get(0).code);
        assertEquals(4, r.problems.get(0).line);
        JdkScriptCompiler.Result typo = compile(HEAD + "public class Typo extends NpcScript {\n"
                + " @Override public void tic(NpcEvent.UpdateEvent e) { }\n}");
        assertFalse(typo.success);
        assertEquals("compiler.err.method.does.not.override.superclass", typo.problems.get(0).code);
    }

    @Test public void overloadWithWrongEventTypeIsSignatureError() throws Exception {
        Class<?> c = load(HEAD + "public class Overload extends NpcScript {\n public void tick(NpcEvent.TimerEvent e) { }\n}");
        try { JavaHookBinder.bind(c, ScriptHooks.npc()); fail(); }
        catch (JavaHookBinder.BindException e) { assertEquals("hook.signature", e.code); }
    }

    @Test public void eventApiIsTheExistingTypedInterface() throws Exception {
        // A typed call on the existing API compiles; a wrong argument type does not.
        assertTrue(compile(HEAD + "public class Api extends NpcScript {\n"
                + " @Override public void timer(NpcEvent.TimerEvent e) { e.npc.getDisplay().setName(\"n\" + e.id); }\n}").success);
        JdkScriptCompiler.Result r = compile(HEAD + "public class ApiWrong extends NpcScript {\n"
                + " @Override public void timer(NpcEvent.TimerEvent e) { e.npc.getDisplay().setName(e.id); }\n}");
        assertFalse(r.success);
        assertEquals(4, r.problems.get(0).line);
    }
}
