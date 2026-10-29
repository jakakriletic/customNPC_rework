package noppes.npcs.rework.script.hooks;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import noppes.npcs.api.event.DialogEvent;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.api.event.RoleEvent;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.rework.script.JdkScriptCompiler;
import org.junit.Test;

public class ScriptHooksTest {
    @Test public void npcRegistryIsDeterministicAndMatchesEnumNames() {
        List<ScriptHooks.Hook> hooks = ScriptHooks.npc();
        assertEquals(16, hooks.size());
        int last = -1;
        for (ScriptHooks.Hook hook : hooks) {
            assertTrue("enum order", hook.type.ordinal() > last);
            last = hook.type.ordinal();
            assertEquals(hook.type.function, hook.name);
            assertSame(hook, ScriptHooks.npc(hook.name));
            assertSame(hook, ScriptHooks.npcByEvent(hook.event));
        }
        assertEquals(NpcEvent.InitEvent.class, ScriptHooks.npc("init").event);
        assertEquals(NpcEvent.UpdateEvent.class, ScriptHooks.npc("tick").event);
        assertEquals(NpcEvent.TimerEvent.class, ScriptHooks.npc("timer").event);
        assertEquals(DialogEvent.CloseEvent.class, ScriptHooks.npc("dialogClose").event);
        assertEquals(RoleEvent.class, ScriptHooks.npc("role").event);
        assertNull("block-only hook", ScriptHooks.npc(EnumScriptType.REDSTONE.function));
    }

    @Test public void javaRegistryAppendsNamedAnimationHooks() {
        List<ScriptHooks.Hook> hooks = ScriptHooks.java();
        assertEquals(19, hooks.size());
        assertEquals(ScriptHooks.npc(), hooks.subList(0, 16));
        assertEquals(ScriptHooks.animation(), hooks.subList(16, 19));
        assertEquals("[animationStart(noppes.npcs.rework.anim.AnimationEvent$Started), animationEnd(noppes.npcs.rework.anim.AnimationEvent$Ended), "
                + "animationEvent(noppes.npcs.rework.anim.AnimationEvent$ClipEvent)]", ScriptHooks.animation().toString());
        for (ScriptHooks.Hook hook : ScriptHooks.animation()) {
            assertNull("no EnumScriptType", hook.type);
            assertNull("not an EventHooks hook", ScriptHooks.npc(hook.name));
            assertSame(hook, ScriptHooks.java(hook.name));
        }
    }

    @Test public void forgeStringHookNameFollowsEventHooksRule() {
        assertEquals("npcEventInitEvent", ScriptHooks.forgeHookName(NpcEvent.InitEvent.class));
        assertEquals("string", ScriptHooks.forgeHookName(String.class));
    }

    @Test public void unitNameSkipsCommentsAndReadsPackage() throws Exception {
        assertEquals("a.b.Tab", JavaHookBinder.unitName("// public class Wrong {}\npackage a . b;\n/* public class No */ public final class Tab {}"));
        assertEquals("Tab", JavaHookBinder.unitName("class Helper {} public class Tab {}"));
        try { JavaHookBinder.unitName("class OnlyPackagePrivate {}"); fail(); }
        catch (JavaHookBinder.BindException e) { assertEquals("unit.noPublicType", e.code); }
    }

    /** Test workers need not expose the API jar on java.class.path; add the jars of the types used. */
    private static String classpath() throws Exception {
        StringBuilder cp = new StringBuilder(System.getProperty("java.class.path"));
        for (Class<?> c : new Class<?>[] { NpcEvent.class, net.minecraftforge.fml.common.eventhandler.Event.class, noppes.npcs.api.entity.ICustomNpc.class }) {
            cp.append(java.io.File.pathSeparator).append(new java.io.File(c.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath());
        }
        return cp.toString();
    }

    private static Class<?> compile(String source) throws Exception {
        String name = JavaHookBinder.unitName(source);
        JdkScriptCompiler.Result r = new JdkScriptCompiler().compile(Collections.singletonMap(name, source), classpath());
        assertTrue(String.valueOf(r.problems.isEmpty() ? "" : r.problems.get(0).message), r.success);
        return r.newLoader(ScriptHooksTest.class.getClassLoader()).loadClass(name);
    }

    @Test public void bindsExactSupertypeAndIgnoresUnrelatedNames() throws Exception {
        Class<?> c = compile("import noppes.npcs.api.event.NpcEvent;\npublic class Tab {\n"
                + " public void tick(NpcEvent.UpdateEvent e) {}\n public void init(NpcEvent e) {}\n"
                + " public void timer(Object e) {}\n public void helper(int x) {}\n}");
        Map<String, Method> bound = JavaHookBinder.bind(c, ScriptHooks.npc());
        assertEquals(new ArrayList<>(bound.keySet()).toString(), 3, bound.size());
        assertTrue(bound.containsKey("tick") && bound.containsKey("init") && bound.containsKey("timer"));
    }

    @Test public void overrideInSubclassIsOneHook() throws Exception {
        Class<?> c = compile("import noppes.npcs.api.event.NpcEvent;\npublic class Tab extends Base {\n"
                + " public void tick(NpcEvent.UpdateEvent e) {}\n}\nclass Base { public void tick(NpcEvent.UpdateEvent e) {} }");
        assertEquals(c, JavaHookBinder.bind(c, ScriptHooks.npc()).get("tick").getDeclaringClass());
    }

    private static String bindError(String source) throws Exception {
        try { JavaHookBinder.bind(compile(source), ScriptHooks.npc()); }
        catch (JavaHookBinder.BindException e) { return e.code; }
        return null;
    }

    @Test public void wrongHookShapesAreErrorsNotMissingHooks() throws Exception {
        String im = "import noppes.npcs.api.event.NpcEvent;\n";
        assertEquals("hook.signature", bindError(im + "public class Tab { public void tick(String e) {} }"));
        assertEquals("hook.signature", bindError(im + "public class Tab { public void tick(NpcEvent.TimerEvent e) {} }"));
        assertEquals("hook.signature", bindError(im + "public class Tab { public void tick() {} }"));
        assertEquals("hook.signature", bindError(im + "public class Tab { void tick(NpcEvent.UpdateEvent e) {} }"));
        assertEquals("hook.signature", bindError(im + "public class Tab { public static void tick(NpcEvent.UpdateEvent e) {} }"));
        assertEquals("hook.ambiguous", bindError(im + "public class Tab { public void tick(NpcEvent.UpdateEvent e) {} public void tick(Object e) {} }"));
        assertEquals("unit.notInstantiable", bindError("public abstract class Tab { }"));
    }
}
