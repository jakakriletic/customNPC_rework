package local.cnpcdocs;

import com.google.gson.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.tools.*;
import noppes.npcs.rework.script.hooks.ScriptHooks;

/** Controller acceptance: independent reflection inventory, full schema, deterministic bytes and compiled examples. */
public final class ApiDocsAcceptance {
    private static int checks;
    private static void require(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
        checks++;
    }

    private static String type(Type t) { return t.getTypeName().replace('$', '.'); }
    private static String id(String name, Class<?>[] params) {
        List<String> names = new ArrayList<>();
        for (Class<?> p : params) { names.add(p.getCanonicalName()); }
        return name + "(" + String.join(",", names) + ")";
    }
    private static boolean visible(int mods, Class<?> c) {
        return Modifier.isPublic(mods) || (c.getName().equals(ApiDocs.BASE) && Modifier.isProtected(mods));
    }
    private static JsonObject member(JsonObject c, String id) {
        for (JsonElement e : c.getAsJsonArray("members")) {
            JsonObject m = e.getAsJsonObject();
            if (m.get("id").getAsString().equals(id)) { return m; }
        }
        throw new AssertionError("Missing " + c.get("name") + "#" + id);
    }
    private static void parameters(JsonObject member, Type[] types) {
        JsonArray args = member.getAsJsonArray("parameters");
        require(args.size() == types.length, "parameter count " + member.get("id"));
        for (int i = 0; i < types.length; i++) {
            JsonObject p = args.get(i).getAsJsonObject();
            require(p.get("name").getAsString().equals("arg" + i), "positional parameter name");
            require(p.get("type").getAsString().equals(type(types[i])), "generic parameter type " + member.get("id"));
        }
    }

    private static void generics(JsonArray listed, TypeVariable<?>[] expected) {
        require(listed.size() == expected.length, "generic parameter count");
        for (int i = 0; i < expected.length; i++) {
            JsonObject p = listed.get(i).getAsJsonObject();
            require(p.get("name").getAsString().equals(expected[i].getName()), "generic parameter name");
            JsonArray bounds = p.getAsJsonArray("bounds");
            Type[] types = expected[i].getBounds();
            require(bounds.size() == types.length, "generic bound count");
            for (int b = 0; b < types.length; b++) { require(bounds.get(b).getAsString().equals(type(types[b])), "generic bound"); }
        }
    }

    private static void inventory(JsonObject catalog) throws Exception {
        Set<String> names = new TreeSet<>();
        for (JsonElement entry : catalog.getAsJsonArray("types")) {
            JsonObject c = entry.getAsJsonObject();
            String name = c.get("name").getAsString();
            require(names.add(name), "unique type " + name);
            // Resolve nested names without initializing their classes, including Forge Event and NpcAPI.
            Class<?> actual = null;
            String binary = name;
            while (actual == null) {
                try { actual = Class.forName(binary, false, ApiDocsAcceptance.class.getClassLoader()); }
                catch (ClassNotFoundException e) {
                    int dot = binary.lastIndexOf('.');
                    if (dot < 0) { throw e; }
                    binary = binary.substring(0, dot) + "$" + binary.substring(dot + 1);
                }
            }
            Set<String> expected = new TreeSet<>();
            for (Method m : actual.getDeclaredMethods()) {
                if (!visible(m.getModifiers(), actual) || m.isSynthetic() || m.isBridge()) { continue; }
                String key = id(m.getName(), m.getParameterTypes());
                expected.add(key);
                JsonObject got = member(c, key);
                require(got.get("kind").getAsString().equals("method"), "method kind");
                require(got.get("returnType").getAsString().equals(type(m.getGenericReturnType())), "generic return type " + name + "#" + key);
                require(got.get("varargs").getAsBoolean() == m.isVarArgs(), "varargs");
                generics(got.getAsJsonArray("typeParameters"), m.getTypeParameters());
                parameters(got, m.getGenericParameterTypes());
                JsonArray thrown = got.getAsJsonArray("throws");
                Type[] exceptions = m.getGenericExceptionTypes();
                require(thrown.size() == exceptions.length, "throws count");
                for (int i = 0; i < exceptions.length; i++) { require(thrown.get(i).getAsString().equals(type(exceptions[i])), "throws type"); }
            }
            for (Constructor<?> ctor : actual.getDeclaredConstructors()) {
                if (!visible(ctor.getModifiers(), actual) || ctor.isSynthetic()) { continue; }
                String key = id("<init>", ctor.getParameterTypes());
                expected.add(key);
                JsonObject got = member(c, key);
                require(got.get("kind").getAsString().equals("constructor"), "constructor kind");
                parameters(got, ctor.getGenericParameterTypes());
            }
            for (Field f : actual.getDeclaredFields()) {
                if (!visible(f.getModifiers(), actual) || f.isSynthetic()) { continue; }
                expected.add(f.getName());
                JsonObject got = member(c, f.getName());
                require(got.get("kind").getAsString().equals("field"), "field kind");
                require(got.get("type").getAsString().equals(type(f.getGenericType())), "generic field type");
                Set<String> flags = new HashSet<>();
                for (JsonElement flag : got.getAsJsonArray("modifiers")) { flags.add(flag.getAsString()); }
                require(flags.contains("final") == Modifier.isFinal(f.getModifiers()), "field mutability");
            }
            Set<String> listed = new TreeSet<>();
            for (JsonElement e : c.getAsJsonArray("members")) {
                JsonObject m = e.getAsJsonObject();
                require(listed.add(m.get("id").getAsString()), "unique member ID");
                require(!m.get("description").getAsString().trim().isEmpty(), "explicit description or absence");
            }
            require(expected.equals(listed), "complete declared member set for " + name + ": " + expected + " != " + listed);
            generics(c.getAsJsonArray("typeParameters"), actual.getTypeParameters());
            require(c.getAsJsonArray("interfaces").size() == actual.getGenericInterfaces().length, "interface count");
            Set<String> expectedInterfaces = new TreeSet<>();
            for (Type parent : actual.getGenericInterfaces()) { expectedInterfaces.add(type(parent)); }
            Set<String> listedInterfaces = new TreeSet<>();
            for (JsonElement parent : c.getAsJsonArray("interfaces")) { listedInterfaces.add(parent.getAsString()); }
            require(expectedInterfaces.equals(listedInterfaces), "generic interface links");
            if (actual.getGenericSuperclass() != null) { require(c.get("superclass").getAsString().equals(type(actual.getGenericSuperclass())), "superclass"); }
        }
        require(names.containsAll(ApiDocs.roots()), "all explicit roots");
        require(names.contains("noppes.npcs.api.entity.IEntity"), "inherited NPC entity API");
        require(names.contains("noppes.npcs.api.event.RoleEvent.TraderEvent"), "polymorphic role event payload");
        require(names.contains("net.minecraftforge.fml.common.eventhandler.Event"), "inherited cancellation API");
        for (JsonElement entry : catalog.getAsJsonArray("types")) {
            JsonObject c = entry.getAsJsonObject();
            if (c.get("name").getAsString().equals("noppes.npcs.api.event.RoleEvent.TraderEvent")) {
                require(c.getAsJsonArray("declaredAnnotations").toString().contains("net.minecraftforge.fml.common.eventhandler.Cancelable"), "role subtype cancellation annotation published");
            }
        }
        require(names.contains("noppes.npcs.api.IWorld") && names.contains("noppes.npcs.api.entity.IPlayer"), "M6.11 player/world published");
        require(names.contains("noppes.npcs.rework.anim.Animators") && names.contains("noppes.npcs.rework.anim.IAnimator"), "M7.9 animation API published");
        require(!names.contains("noppes.npcs.api.block.IBlock") && !names.contains("noppes.npcs.api.handler.data.IQuest"), "v2 block/quest types stay external");
        for (JsonElement ref : catalog.getAsJsonArray("externalReferences")) { require(!names.contains(ref.getAsString()), "external references disjoint"); }
    }

    private static void examples(JsonObject catalog, Path work) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> errors = new DiagnosticCollector<>();
        List<JavaFileObject> examples = new ArrayList<>();
        List<String> hookNames = new ArrayList<>();
        for (JsonElement entry : catalog.getAsJsonArray("hooks")) {
            JsonObject hook = entry.getAsJsonObject();
            String name = hook.get("name").getAsString();
            String event = hook.get("event").getAsString();
            hookNames.add(name);
            require(ScriptHooks.npc(name).event.getName().replace('$', '.').equals(event), "registry event " + name);
            examples.add(ApiDocs.source("Example_" + name, hook.get("example").getAsString()));
        }
        require(hookNames.equals(Arrays.asList("init", "tick", "interact", "dialog", "damaged", "died", "meleeAttack",
                "target", "collide", "kill", "dialogOption", "targetLost", "role", "rangedLaunched", "timer", "dialogClose")), "16 expected hooks in registry order");
        Files.createDirectories(work);
        List<String> options = Arrays.asList("-proc:none", "-source", "8", "-target", "8", "-encoding", "UTF-8",
                "-classpath", System.getProperty("java.class.path"), "-d", work.toString());
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(errors, Locale.ROOT, StandardCharsets.UTF_8)) {
            require(compiler.getTask(null, fm, errors, options, null, examples).call(), "all 16 published Java examples compile: " + errors.getDiagnostics());
            errors = new DiagnosticCollector<>();
            JavaFileObject wrong = ApiDocs.source("Wrong", "public class Wrong extends " + ApiDocs.BASE + " {\n"
                    + "@Override public void tick(noppes.npcs.api.event.NpcEvent.InitEvent event) {}\n}\n");
            require(!compiler.getTask(null, fm, errors, options, null, Collections.singletonList(wrong)).call(), "wrong event rejected");
            require(errors.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR && d.getLineNumber() == 2 && d.getColumnNumber() > 0), "wrong example has line/column");
            errors = new DiagnosticCollector<>();
            JavaFileObject playerWorld = ApiDocs.source("Example_playerWorld", catalog.get("playerWorldExample").getAsString());
            require(compiler.getTask(null, fm, errors, options, null, Collections.singletonList(playerWorld)).call(), "published player/world example compiles: " + errors.getDiagnostics());
            errors = new DiagnosticCollector<>();
            JavaFileObject animation = ApiDocs.source("Example_animation", catalog.get("animationExample").getAsString());
            require(compiler.getTask(null, fm, errors, options, null, Collections.singletonList(animation)).call(), "published animation example compiles: " + errors.getDiagnostics());
            errors = new DiagnosticCollector<>();
            JavaFileObject lossy = ApiDocs.source("LossyTime", "public class LossyTime extends " + ApiDocs.BASE + " {\n"
                    + "@Override public void interact(noppes.npcs.api.event.NpcEvent.InteractEvent event) {\n"
                    + "int time = event.npc.getWorld().getTime(); }\n}\n");
            require(!compiler.getTask(null, fm, errors, options, null, Collections.singletonList(lossy)).call(), "long world time into int rejected");
            require(errors.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR && d.getLineNumber() == 3
                    && d.getColumnNumber() > 0 && "compiler.err.prob.found.req".equals(d.getCode())), "typed world diagnostic has line/column/code");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) { throw new IllegalArgumentException("Usage: ApiDocsAcceptance <output-directory> <work-directory>"); }
        long start = System.nanoTime();
        Map<String, Object> model = ApiDocs.catalog(false);
        Map<String, byte[]> rendered = ApiDocs.render(model);
        Map<String, byte[]> reversed = ApiDocs.render(ApiDocs.catalog(true));
        for (String file : rendered.keySet()) { require(Arrays.equals(rendered.get(file), reversed.get(file)), "deterministic bytes with reversed discovery: " + file); }
        JsonObject json = new JsonParser().parse(new String(rendered.get("API.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        require(json.get("schema").getAsInt() == 1 && json.get("javaVersion").getAsString().equals("8"), "schema/version");
        inventory(json);
        // Deliberate damage must be detected by the independent inventory check.
        JsonObject missing = new JsonParser().parse(json.toString()).getAsJsonObject();
        JsonArray members = missing.getAsJsonArray("types").get(0).getAsJsonObject().getAsJsonArray("members");
        require(members.size() > 0, "mutation fixture has members");
        members.remove(0);
        boolean rejected = false;
        try { inventory(missing); } catch (AssertionError expected) { rejected = true; }
        require(rejected, "missing API member rejected");
        examples(json, Paths.get(args[1]).resolve("examples"));
        String markdown = new String(rendered.get("API.md"), StandardCharsets.UTF_8);
        String hooks = new String(rendered.get("HOOKS.md"), StandardCharsets.UTF_8);
        for (Map<String, Object> t : ApiDocs.rows(model.get("types"))) {
            require(markdown.contains("### " + t.get("name") + "\n"), "type heading in Markdown");
            for (Map<String, Object> m : ApiDocs.rows(t.get("members"))) { require(markdown.contains("`" + ApiDocs.signature(m) + "`"), "member rendered in Markdown"); }
        }
        for (Map<String, Object> h : ApiDocs.rows(model.get("hooks"))) {
            require(hooks.contains("## " + h.get("name") + "\n") && hooks.contains("```java\n" + h.get("example") + "```"), "same hook example rendered in Markdown");
        }
        require(hooks.contains("no sandbox") && markdown.contains("arg0") && markdown.contains("## Exclusions"), "scope/trust/parameter limitations published");
        require(hooks.contains("## Player and world\n") && hooks.contains("```java\n" + model.get("playerWorldExample") + "```"), "same player/world example rendered in Markdown");
        require(!markdown.contains("pending M6.11"), "M6.11 exclusion removed");
        require(hooks.contains("## Optional action animation\n") && hooks.contains("```java\n" + model.get("animationExample") + "```"), "same animation example rendered in Markdown");
        Map<String, Object> proof = ApiDocs.obj("success", true, "checks", checks, "types", json.getAsJsonArray("types").size(),
                "hooks", json.getAsJsonArray("hooks").size(), "signatureFingerprint", model.get("signatureFingerprint"),
                "elapsedMillis", (System.nanoTime() - start) / 1_000_000.0,
                "condition", "javac symbols vs independent reflection inventory; reversed discovery bytes; all 16 examples compiled, never executed; no game launch");
        ApiDocs.write(Paths.get(args[0]), rendered);
        ApiDocs.write(Paths.get(args[1]), Collections.singletonMap("proof.json", (ApiDocs.JSON.toJson(proof) + "\n").getBytes(StandardCharsets.UTF_8)));
        System.out.println(ApiDocs.JSON.toJson(proof));
    }
}
