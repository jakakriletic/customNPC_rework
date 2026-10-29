package local.cnpctemplate;

import com.google.gson.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import noppes.npcs.rework.script.*;
import noppes.npcs.rework.script.hooks.*;

/** Offline template acceptance only: never constructs scripts or bootstraps a world. */
public final class TemplateAcceptance {
    private static int checks;
    private static void require(boolean ok, String message) {
        if (!ok) { throw new AssertionError(message); }
        checks++;
    }
    private static String text(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
    private static JsonObject json(Path path) throws Exception {
        return new JsonParser().parse(text(path)).getAsJsonObject();
    }
    private static Map<String, Method> bind(Map<String, byte[]> bytes) throws Exception {
        ClassLoader parent = NpcScript.class.getClassLoader();
        ClassLoader loader = JdkScriptCompiler.newLoader(bytes, parent);
        Class<?> script = Class.forName("example.StarterNpc", false, loader);
        require(script.getSuperclass() == NpcScript.class, "same API base identity (no copied API)");
        require(script.getConstructor().getParameterCount() == 0, "public no-arg constructor");
        require(script.getClassLoader() == loader, "script has its own loader");
        Map<String, Method> hooks = JavaHookBinder.bind(script, ScriptHooks.npc());
        require(hooks.keySet().equals(new HashSet<>(Arrays.asList("init", "tick", "interact", "timer"))), "only four overridden hooks");
        for (Map.Entry<String, Method> entry : hooks.entrySet()) {
            Method m = entry.getValue();
            require(m.getReturnType() == void.class, "void hook " + entry.getKey());
            require(m.getParameterTypes()[0] == ScriptHooks.npc(entry.getKey()).event, "same registry event identity " + entry.getKey());
        }
        return hooks;
    }
    public static void main(String[] args) throws Exception {
        Path template = Paths.get(args[0]);
        String cp = text(template.resolve(".local/api-classpath.txt")).trim();
        require(System.getProperty("java.specification.version").equals("1.8"), "JDK 8 acceptance JVM");
        Map<String, String> labelled = new LinkedHashMap<>();
        labelled.put("tab starter", text(template.resolve("src/main/java/example/StarterNpc.java")));
        labelled.put("include src/main/java/example/CounterText.java", text(template.resolve("src/main/java/example/CounterText.java")));
        JavaSources sources = new JavaSources(labelled);
        require(sources.main.equals("example.StarterNpc"), "runtime entry type and ordered include");
        JdkScriptCompiler compiler = new JdkScriptCompiler();
        JdkScriptCompiler.Result compiled = compiler.compile(sources.units, cp);
        require(compiled.success, "runtime compiler accepts the exact template sources: " + compiled.problems);
        Map<String, byte[]> memory = compiled.bytecode();
        require(memory.keySet().equals(new HashSet<>(Arrays.asList("example.StarterNpc", "example.CounterText"))), "two runtime bytecode units");
        Map<String, byte[]> ide = new LinkedHashMap<>();
        for (String name : memory.keySet()) {
            byte[] bytes = Files.readAllBytes(template.resolve("build/classes/java/main/" + name.replace('.', '/') + ".class"));
            require(bytes.length > 8 && bytes[6] == 0 && (bytes[7] & 255) == 52, "IDE Java 8 bytecode " + name);
            byte[] runtime = memory.get(name);
            require(runtime[6] == 0 && (runtime[7] & 255) == 52, "runtime Java 8 bytecode " + name);
            ide.put(name, bytes);
        }
        Map<String, Method> runtimeHooks = bind(memory);
        Map<String, Method> ideHooks = bind(ide);
        for (String name : runtimeHooks.keySet()) {
            require(Arrays.equals(runtimeHooks.get(name).getParameterTypes(), ideHooks.get(name).getParameterTypes()), "IDE/runtime hook types " + name);
        }
        JsonObject good = json(template.resolve(".local/compile.json"));
        require(good.get("success").getAsBoolean() && good.get("operation").getAsString().equals("compile-only"), "actual CLI success and operation");
        require(good.getAsJsonArray("problems").size() == 0 && good.getAsJsonArray("tabs").size() == 1, "one CLI tab, valid manifest");
        JsonObject tab = good.getAsJsonArray("tabs").get(0).getAsJsonObject();
        require(tab.get("main").getAsString().equals(sources.main), "CLI/runtime same entry type");
        require(tab.get("inputFingerprint").getAsString().equals(BytecodeCache.key(compiler.identity(), cp, sources.units)), "CLI/runtime same compiler, source, API fingerprint");
        Set<String> cliClasses = new HashSet<>();
        for (JsonElement e : tab.getAsJsonArray("classes")) { cliClasses.add(e.getAsString()); }
        require(cliClasses.equals(memory.keySet()), "IDE/CLI/runtime same output types");
        JsonObject bad = json(template.resolve(".local/invalid.json"));
        require(!bad.get("success").getAsBoolean() && bad.getAsJsonArray("problems").size() == 0, "invalid examples fail compilation, not manifest validation");
        require(bad.getAsJsonArray("tabs").size() == 3, "three negative cases");
        Set<String> failures = new HashSet<>();
        for (JsonElement e : bad.getAsJsonArray("tabs")) {
            JsonObject b = e.getAsJsonObject();
            String id = b.get("id").getAsString();
            require(failures.add(id), "unique negative tab");
            require(!b.get("success").getAsBoolean() && b.getAsJsonArray("classes").size() == 0, "no partial output " + id);
            String file = id.equals("hook") ? "WrongHook.java" : id.equals("type") ? "WrongType.java" : "BrokenCounterText.java";
            String origin = id.equals("include") ? "include " + file : "tab " + id;
            boolean located = false;
            for (JsonElement problem : b.getAsJsonArray("problems")) {
                JsonObject p = problem.getAsJsonObject();
                if (p.get("kind").getAsString().equals("ERROR") && p.get("file").getAsString().equals(file)
                        && p.get("origin").getAsString().equals(origin) && p.get("line").getAsInt() == 5
                        && p.get("column").getAsInt() > 0 && !p.get("code").getAsString().isEmpty()
                        && !p.get("message").getAsString().isEmpty()) { located = true; }
            }
            require(located, "original file/origin/line/column/compiler code " + id);
        }
        require(failures.equals(new HashSet<>(Arrays.asList("hook", "type", "include"))), "all negative cases checked");
        JsonObject proof = new JsonObject();
        proof.addProperty("success", true);
        proof.addProperty("checks", checks);
        proof.addProperty("classes", memory.size());
        proof.addProperty("hooks", runtimeHooks.size());
        proof.addProperty("invalidTabs", failures.size());
        proof.addProperty("inputFingerprint", tab.get("inputFingerprint").getAsString());
        proof.addProperty("condition", "offline IDE/CLI/runtime-compiler and binder contract; classes loaded without initialization, no constructors/hooks invoked, no Forge/world bootstrap");
        Files.write(Paths.get(args[1]), (new GsonBuilder().setPrettyPrinting().create().toJson(proof) + "\n").getBytes(StandardCharsets.UTF_8));
        System.out.println("M610 acceptance: " + checks + " checks");
    }
}
