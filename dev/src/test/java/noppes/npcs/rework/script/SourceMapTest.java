package noppes.npcs.rework.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.script.ScriptException;
import org.junit.Test;

/** M6.5: error locations map back to the tab or include file (JS joined text, Java units). */
public class SourceMapTest {
    /** Built like ScriptContainer.getFullCode: tab code, then every include, each ending in a newline. */
    private static SourceMap joined(String tab, String... includes) {
        SourceMap map = new SourceMap();
        String full = tab;
        if (!full.isEmpty()) { map.add(1, "tab"); full += "\n"; }
        for (int i = 0; i < includes.length; i += 2) {
            map.add(1 + SourceMap.lines(full), includes[i]);
            full += includes[i + 1] + "\n";
        }
        return map;
    }

    @Test
    public void joinedLinesMapToTheirSegment() {
        SourceMap map = joined("var a = 1;\nvar b = 2;", "m65/one.js", "function f() {\n  x();\n}", "m65/two.js", "y();");
        assertEquals("tab:1", map.locate(1));
        assertEquals("tab:2", map.locate(2));
        assertEquals("m65/one.js:1", map.locate(3));
        assertEquals("m65/one.js:2", map.locate(4));
        assertEquals("m65/two.js:1", map.locate(6));
        assertNull(map.locate(0));
        // Without tab code the first include starts at line 1.
        assertEquals("m65/one.js:2", joined("", "m65/one.js", "a();\nb();").locate(2));
        assertNull(new SourceMap().locate(1));
    }

    @Test
    public void scriptExceptionLineAndColumnInCauseChain() {
        SourceMap map = joined("var a = 1;\nvar b = 2;", "m65/b5e.js", "function tick(e) {\n  e.add('boom');\n  throw new Error('x');\n}");
        ScriptException withColumn = new ScriptException("x", "<eval>", 5, 2);
        assertEquals("m65/b5e.js:3:2", map.locateScript(new RuntimeException(withColumn)));
        assertEquals("m65/b5e.js:3", map.locateScript(new ScriptException("x", "<eval>", 5)));
        // Fallback: a Java exception thrown under a script frame named <eval>.
        RuntimeException fromApi = new RuntimeException("api");
        fromApi.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("noppes.npcs.api.Wrapper", "call", "Wrapper.java", 10),
                new StackTraceElement("jdk.nashorn.internal.scripts.Script$Recompilation$1$tick", "tick", "<eval>", 4) });
        assertEquals("m65/b5e.js:2", map.locateScript(fromApi));
        assertNull(map.locateScript(new IllegalStateException("no script frame")));
    }

    @Test
    public void segmentsMustBeOrdered() {
        SourceMap map = new SourceMap();
        map.add(3, "a");
        try { map.add(2, "b"); throw new AssertionError("accepted"); } catch (IllegalArgumentException expected) { }
        try { new SourceMap().add(0, "c"); throw new AssertionError("accepted"); } catch (IllegalArgumentException expected) { }
    }

    @Test
    public void runtimeStackTagsScriptFramesAndCutsTheRest() {
        Map<String, String> origins = new HashMap<>();
        origins.put("m65/Util.java", "include m65/util.java");
        origins.put("m65/Hooks.java", "include m65/hooks.java");
        IllegalStateException e = new IllegalStateException("m65 fixture");
        e.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("m65.Util", "v", "Util.java", 3),
                new StackTraceElement("m65.Hooks", "tick", "Hooks.java", 5),
                new StackTraceElement("sun.reflect.NativeMethodAccessorImpl", "invoke0", null, -2),
                new StackTraceElement("noppes.npcs.EventHooks", "onNPCTick", "EventHooks.java", 80) });
        String text = SourceMap.runtime("tick", e, origins);
        assertTrue(text, text.startsWith("Runtime error in hook tick:\njava.lang.IllegalStateException: m65 fixture"));
        assertTrue(text, text.contains("at m65.Util.v(Util.java:3) [include m65/util.java]"));
        assertTrue(text, text.contains("at m65.Hooks.tick(Hooks.java:5) [include m65/hooks.java]"));
        assertTrue(text, text.endsWith("\t... 2 more"));
        assertFalse(text, text.contains("EventHooks"));
    }

    @Test
    public void runtimeStackKeepsApiFramesAboveTheScriptAndCauses() {
        Map<String, String> origins = Collections.singletonMap("Main.java", "tab 2");
        RuntimeException cause = new RuntimeException("inner");
        cause.setStackTrace(new StackTraceElement[] { new StackTraceElement("x.Api", "get", "Api.java", 7) });
        RuntimeException e = new RuntimeException("outer", cause);
        e.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("x.Api", "call", "Api.java", 9),
                new StackTraceElement("Main", "tick", "Main.java", 4),
                new StackTraceElement("x.Game", "loop", "Game.java", 1) });
        String text = SourceMap.runtime("tick", e, origins);
        assertTrue(text, text.contains("at x.Api.call(Api.java:9)\n\tat Main.tick(Main.java:4) [tab 2]\n\t... 1 more"));
        // No script frame in the cause: its whole stack stays.
        assertTrue(text, text.contains("Caused by: java.lang.RuntimeException: inner\n\tat x.Api.get(Api.java:7)"));
    }

    @Test
    public void causeCycleIsBounded() {
        RuntimeException a = new RuntimeException("a");
        RuntimeException b = new RuntimeException("b", a);
        a.initCause(b);
        String text = SourceMap.runtime("tick", a, Collections.<String, String>emptyMap());
        assertEquals(text, 1, text.split("Caused by").length - 1);
    }

    @Test
    public void unitPathFromClassAndSourceFile() {
        assertEquals("m65/Util.java", SourceMap.unitPath(new StackTraceElement("m65.Util$1", "run", "Util.java", 3)));
        assertEquals("Main.java", SourceMap.unitPath(new StackTraceElement("Main", "tick", "Main.java", 4)));
        assertNull(SourceMap.unitPath(new StackTraceElement("m65.Util", "run", null, -1)));
    }
}
