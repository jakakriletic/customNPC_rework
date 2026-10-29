package noppes.npcs.rework.script;

import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.event.NpcEvent;

/** Explicit M6.1 fixture entry point only. Never installed into a normal NPC handler. */
public final class JavaCompilerProbe {
    private JavaCompilerProbe() { }
    public static boolean run(ICustomNpc npc, String classpathFile, String output, String mode) throws Exception {
        Map<String, Object> proof = new LinkedHashMap<>();
        List<Map<String, Object>> checks = new ArrayList<>();
        proof.put("mode", mode); proof.put("checks", checks); proof.put("success", false);
        proof.put("java", System.getProperty("java.runtime.version"));
        proof.put("apiLoader", ICustomNpc.class.getClassLoader().getClass().getName());
        boolean success = false;
        try {
            check(checks, "environment", FMLLaunchHandler.isDeobfuscatedEnvironment() == mode.equals("dev"));
            check(checks, "game-loader", ICustomNpc.class.getClassLoader().getClass().getName().equals("net.minecraft.launchwrapper.LaunchClassLoader"));
            String cp = new String(Files.readAllBytes(Paths.get(classpathFile)), StandardCharsets.UTF_8).trim();
            // Only the existing public API and event type appear in script source; no MC implementation methods.
            String source = "package m61;\nimport noppes.npcs.api.event.NpcEvent;\npublic class TimerScript {\n"
                    + " public String run(NpcEvent.TimerEvent e) {\n"
                    + "  java.util.function.Supplier<String> name=()->e.npc.getName();\n"
                    + "  java.util.List<String> names=java.util.Arrays.asList(name.get());\n"
                    + "  e.npc.getTempdata().put(\"m61-java\", \"ok-\"+e.id);\n"
                    + "  return names.get(0)+\"|\"+e.id;\n }\n"
                    + " public void fail() { throw new IllegalStateException(\"fixture\"); }\n}";
            long start = System.nanoTime();
            JdkScriptCompiler compiler = new JdkScriptCompiler();
            JdkScriptCompiler.Result result = compiler.compile(Collections.singletonMap("m61.TimerScript", source), cp);
            proof.put("coldCompileNanos", System.nanoTime() - start);
            proof.put("diagnostics", result.problems);
            check(checks, "typed-compile", result.success);
            ClassLoader loader = result.newLoader(ICustomNpc.class.getClassLoader());
            check(checks, "same-api-class", loader.loadClass(ICustomNpc.class.getName()) == ICustomNpc.class);
            check(checks, "same-event-class", loader.loadClass(NpcEvent.TimerEvent.class.getName()) == NpcEvent.TimerEvent.class);
            Class<?> script = loader.loadClass("m61.TimerScript");
            Object instance = script.newInstance();
            NpcEvent.TimerEvent event = new NpcEvent.TimerEvent(npc, 7);
            Object value = script.getMethod("run", NpcEvent.TimerEvent.class).invoke(instance, event);
            proof.put("value", value); proof.put("uuid", npc.getUUID());
            check(checks, "real-wrapper", npc.getClass().getName().startsWith("noppes.npcs.api.wrapper."));
            check(checks, "typed-call", (npc.getName()+"|7").equals(value));
            check(checks, "world-write", "ok-7".equals(npc.getTempdata().get("m61-java")));
            boolean line = false;
            try { script.getMethod("fail").invoke(instance); }
            catch (InvocationTargetException failure) {
                for (StackTraceElement frame : failure.getCause().getStackTrace()) {
                    if (frame.getClassName().equals("m61.TimerScript") && "TimerScript.java".equals(frame.getFileName()) && frame.getLineNumber()==10) { line=true; }
                }
                proof.put("runtimeError", failure.getCause().toString());
            }
            check(checks, "runtime-file-line", line);
            JdkScriptCompiler.Result wrong = compiler.compile(Collections.singletonMap("m61.BadApi",
                    "package m61;\nimport noppes.npcs.api.event.NpcEvent;\npublic class BadApi {\n void run(NpcEvent.TimerEvent e){ int x=e.npc.getName(); }\n}"), cp);
            proof.put("badApiDiagnostics", wrong.problems);
            check(checks, "api-type-error", !wrong.success && wrong.problems.stream().anyMatch(p -> p.kind.equals("ERROR") && p.file.endsWith("BadApi.java") && p.line==4 && p.column>0));
            List<Long> warm = new ArrayList<>();
            for (int i=0; i<5; i++) {
                start=System.nanoTime();
                JdkScriptCompiler.Result next=compiler.compile(Collections.singletonMap("m61.TimerScript", source), cp);
                warm.add(System.nanoTime()-start);
                check(checks, "warm-"+i, next.success);
            }
            proof.put("warmCompileNanos", warm);
            success=true;
        } catch (Throwable failure) {
            proof.put("error", failure.toString());
            failure.printStackTrace();
        } finally {
            proof.put("success", success);
            Files.write(Paths.get(output), new GsonBuilder().setPrettyPrinting().create().toJson(proof).getBytes(StandardCharsets.UTF_8));
        }
        return success;
    }
    private static void check(List<Map<String, Object>> checks, String key, boolean ok) {
        Map<String,Object> check = new LinkedHashMap<>(); check.put("key", key); check.put("ok", ok); checks.add(check);
        if (!ok) { throw new IllegalStateException("M6.1 " + key); }
    }
}
