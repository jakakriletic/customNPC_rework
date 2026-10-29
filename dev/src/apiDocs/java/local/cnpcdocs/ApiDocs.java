package local.cnpcdocs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import javax.tools.*;
import noppes.npcs.rework.script.hooks.ScriptHooks;

/** Build-time only. Reads javac symbols without loading API classes or starting Forge. */
public final class ApiDocs {
    static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    static final String BASE = "noppes.npcs.rework.script.NpcScript";
    static final String NO_DOC = "No source description is available in the compiled API; see the signature.";
    static final String CONTRACT = "Java 8; trusted authors only, with process permissions and no sandbox. "
            + "JS remains the default. Java dispatch is NPC-only. Public no-argument constructor required; "
            + "one instance/loader per NPC, tab and generation. Only overridden NpcScript hooks are bound. "
            + "Tabs run in their existing order, then the API bus; cancellation and missing hooks keep the existing handler behavior.";
    static final String SCOPE = "Published v1 reference: NPC script base, NPC/entity inheritance, NPC data interfaces, "
            + "NPC hook events and public role event variants, timers/data/NBT/position/damage API, NpcAPI, "
            + "the player/world API (IPlayer, IWorld), and optional action animation (Animators, IAnimator). "
            + "All public declared members are listed; protected members are included only on NpcScript. "
            + "Inheritance links point to documented ancestors, including Forge Event for cancellation. "
            + "Referenced types outside this explicit set are external references, not recursively published API. "
            + "Existing methods returning item/block/quest/dialog/faction types remain visible; "
            + "this does not implement new Java handlers for them.";
    /** M6.11 (Q188): one compiled player/world example, published after the hook examples. */
    static final String PLAYER_WORLD_EXAMPLE = "import noppes.npcs.api.IWorld;\n"
            + "import noppes.npcs.api.entity.IPlayer;\n"
            + "import noppes.npcs.api.event.NpcEvent;\n\n"
            + "public class Example_playerWorld extends " + BASE + " {\n"
            + "    @Override public void interact(NpcEvent.InteractEvent event) {\n"
            + "        IPlayer<?> player = event.player;\n"
            + "        IWorld world = event.npc.getWorld();\n"
            + "        long time = world.getTime();\n"
            + "        player.message(\"Hello \" + player.getDisplayName() + \" in \" + world.getName() + \" at \" + time);\n"
            + "        world.getTempdata().put(\"lastVisitor\", player.getName());\n"
            + "        if (!world.isDay()) {\n"
            + "            world.setBlock(event.npc.getBlockX(), event.npc.getBlockY() + 2, event.npc.getBlockZ(), \"minecraft:torch\", 0);\n"
            + "        }\n"
            + "    }\n}\n";
    static final String ANIMATION_EXAMPLE = "import noppes.npcs.rework.anim.Animators;\n"
            + "import noppes.npcs.rework.anim.IAnimator;\n"
            + "import noppes.npcs.api.event.NpcEvent;\n\n"
            + "public class Example_animation extends " + BASE + " {\n"
            + "    @Override public void interact(NpcEvent.InteractEvent event) {\n"
            + "        IAnimator animator = Animators.of(event.npc);\n"
            + "        if (animator.isAvailable() && animator.play(\"hurt\")) {\n"
            + "            animator.queue(\"idle\");\n"
            + "        }\n"
            + "    }\n}\n";

    static final String ANIMATION_HOOK = "Optional animation hook (M7.5), called only with RwAnimHooks=1 for an EntityAnimatedNpc of customnpcentities 0.7.0; "
            + "never called by EventHooks and not posted to the API event bus. The server observes the action once per world tick END and calls, "
            + "in this order, animationEnd of the previous action, animationStart of a new one (also the same clip again), then animationEvent for "
            + "each timed clip event the entity fired since the last observation, with tick = the action age it fired at. finished=false means "
            + "stopped, replaced or cut by death; removal or unload of the NPC calls no hook.";

    private ApiDocs() { }

    static Map<String, Object> obj(Object... pairs) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) { out.put((String) pairs[i], pairs[i + 1]); }
        return out;
    }

    static JavaFileObject source(String name, String body) {
        return new SimpleJavaFileObject(URI.create("string:///" + name + ".java"), JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignore) { return body; }
        };
    }

    static SortedSet<String> roots() {
        SortedSet<String> names = new TreeSet<>(Arrays.asList(BASE,
                "noppes.npcs.api.NpcAPI", "noppes.npcs.api.entity.ICustomNpc",
                "noppes.npcs.api.entity.data.IData", "noppes.npcs.api.ITimers",
                "noppes.npcs.api.INbt", "noppes.npcs.api.IPos", "noppes.npcs.api.IDamageSource",
                "noppes.npcs.api.IWorld", "noppes.npcs.api.entity.IPlayer",
                "noppes.npcs.rework.anim.Animators", "noppes.npcs.rework.anim.IAnimator",
                "noppes.npcs.rework.anim.AnimationEvent"));
        for (String name : Arrays.asList("INPCAdvanced", "INPCAi", "INPCDisplay", "INPCInventory", "INPCJob",
                "INPCMelee", "INPCRanged", "INPCRole", "INPCStats")) {
            names.add("noppes.npcs.api.entity.data." + name);
        }
        for (ScriptHooks.Hook hook : ScriptHooks.java()) { names.add(hook.event.getName().replace('$', '.')); }
        return names;
    }

    static Map<String, Object> catalog(boolean reverseRoots) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) { throw new IllegalStateException("JDK 8 compiler is required to generate API docs"); }
        DiagnosticCollector<JavaFileObject> errors = new DiagnosticCollector<>();
        Model processor = new Model(reverseRoots);
        try (StandardJavaFileManager files = compiler.getStandardFileManager(errors, Locale.ROOT, StandardCharsets.UTF_8)) {
            JavaCompiler.CompilationTask task = compiler.getTask(null, files, errors,
                    Arrays.asList("-proc:only", "-source", "8", "-target", "8", "-encoding", "UTF-8",
                            "-classpath", System.getProperty("java.class.path")), null,
                    Collections.singletonList(source("ApiDocsSymbols", "final class ApiDocsSymbols {}")));
            // Explicitly selected internal processor: never discover classpath processors or run API/script code.
            task.setProcessors(Collections.singletonList(processor));
            if (!task.call() || processor.result == null) { throw new IllegalStateException(errors.getDiagnostics().toString()); }
        }
        Map<String, Object> result = processor.result;
        result.put("signatureFingerprint", sha((JSON.toJson(result) + "\n").getBytes(StandardCharsets.UTF_8)));
        return result;
    }

    private static final class Model extends AbstractProcessor {
        private final boolean reverse;
        private final SortedMap<String, TypeElement> published = new TreeMap<>();
        private final SortedSet<String> referenced = new TreeSet<>();
        private Elements elements;
        Map<String, Object> result;
        Model(boolean reverse) { this.reverse = reverse; }
        @Override public Set<String> getSupportedAnnotationTypes() { return Collections.singleton("*"); }
        @Override public SourceVersion getSupportedSourceVersion() { return SourceVersion.RELEASE_8; }
        @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
            if (result != null || round.processingOver()) { return false; }
            elements = processingEnv.getElementUtils();
            List<String> names = new ArrayList<>(roots());
            if (reverse) { Collections.reverse(names); }
            for (String name : names) {
                TypeElement type = elements.getTypeElement(name);
                if (type == null) { throw new IllegalStateException("Missing API symbol: " + name); }
                publish(type);
            }
            // RoleEvent is a polymorphic NPC hook; its public variants carry different fields/cancellation.
            TypeElement role = elements.getTypeElement("noppes.npcs.api.event.RoleEvent");
            for (Element child : role.getEnclosedElements()) {
                if (child instanceof TypeElement && child.getModifiers().contains(Modifier.PUBLIC)) { publish((TypeElement) child); }
            }
            List<Object> types = new ArrayList<>();
            for (TypeElement type : published.values()) { types.add(describe(type)); }
            List<Object> hooks = new ArrayList<>();
            for (ScriptHooks.Hook hook : ScriptHooks.java()) {
                String event = hook.event.getName().replace('$', '.');
                TypeElement type = elements.getTypeElement(event);
                boolean cancelable = type.getAnnotationMirrors().stream().anyMatch(a -> a.getAnnotationType().toString()
                        .equals("net.minecraftforge.fml.common.eventhandler.Cancelable"));
                hooks.add(obj("name", hook.name, "enum", hook.type == null ? "none" : hook.type.name(), "event", event,
                        "signature", "public void " + hook.name + "(" + event + " event)",
                        "cancelableOnDeclaredEvent", cancelable,
                        "description", hook.type == null ? ANIMATION_HOOK : "NPC hook from ScriptHooks; event payload and inherited members are in API.md. Role variants may be cancelable independently.",
                        "example", example(hook.name, event)));
            }
            referenced.removeAll(published.keySet());
            result = obj("schema", 1, "operation", "api-reference", "javaVersion", "8", "scope", SCOPE,
                    "contract", CONTRACT, "descriptionPolicy", NO_DOC,
                    "parameterNames", "arg0, arg1, ... are positional labels, not original source parameter names.",
                    "excludedJavaHandlers", ScriptHooks.V1_EXCLUDED,
                    "excludedSurfaces", Arrays.asList("item/block/quest/dialog/faction/container/scoreboard/dimension types reached from IPlayer/IWorld, their expansions and Java handlers (v2)",
                            "implementation wrappers, Minecraft/Forge internals except the inherited Event contract",
                            "Object members, private/package members, synthetic/bridge members and constant values",
                            "runtime frequency, subtype instances and behavior not derivable from signatures"),
                    "roots", roots(), "types", types, "externalReferences", new ArrayList<>(referenced), "hooks", hooks,
                    "playerWorldExample", PLAYER_WORLD_EXAMPLE, "animationExample", ANIMATION_EXAMPLE);
            return false;
        }

        private void publish(TypeElement type) {
            String name = type.getQualifiedName().toString();
            if (published.put(name, type) != null) { return; }
            List<TypeMirror> parents = new ArrayList<>(type.getInterfaces());
            parents.add(type.getSuperclass());
            for (TypeMirror parent : parents) {
                if (parent.getKind() != TypeKind.DECLARED) { continue; }
                TypeElement p = (TypeElement) ((DeclaredType) parent).asElement();
                String n = p.getQualifiedName().toString();
                if (n.startsWith("noppes.npcs.api.") || n.equals("net.minecraftforge.fml.common.eventhandler.Event")) { publish(p); }
            }
        }

        private String type(TypeMirror t) {
            if (t.getKind() == TypeKind.ERROR) { throw new IllegalStateException("Unresolved signature: " + t); }
            if (t.getKind() == TypeKind.DECLARED) {
                DeclaredType d = (DeclaredType) t;
                referenced.add(((TypeElement) d.asElement()).getQualifiedName().toString());
                for (TypeMirror arg : d.getTypeArguments()) { type(arg); }
                if (d.getEnclosingType().getKind() != TypeKind.NONE) { type(d.getEnclosingType()); }
            } else if (t.getKind() == TypeKind.ARRAY) {
                type(((ArrayType) t).getComponentType());
            } else if (t.getKind() == TypeKind.WILDCARD) {
                WildcardType w = (WildcardType) t;
                if (w.getExtendsBound() != null) { type(w.getExtendsBound()); }
                if (w.getSuperBound() != null) { type(w.getSuperBound()); }
            }
            return t.toString();
        }

        private List<Object> parameters(List<? extends TypeParameterElement> params) {
            List<Object> out = new ArrayList<>();
            for (TypeParameterElement p : params) {
                List<String> bounds = new ArrayList<>();
                for (TypeMirror b : p.getBounds()) { bounds.add(type(b)); }
                out.add(obj("name", p.getSimpleName().toString(), "bounds", bounds));
            }
            return out;
        }

        private Map<String, Object> describe(TypeElement c) {
            String name = c.getQualifiedName().toString();
            List<String> classModifiers = new ArrayList<>();
            for (Modifier m : c.getModifiers()) { classModifiers.add(m.toString()); }
            Collections.sort(classModifiers);
            List<String> annotations = new ArrayList<>();
            for (AnnotationMirror a : c.getAnnotationMirrors()) { annotations.add(type(a.getAnnotationType())); }
            Collections.sort(annotations);
            List<String> interfaces = new ArrayList<>();
            for (TypeMirror i : c.getInterfaces()) { interfaces.add(type(i)); }
            Collections.sort(interfaces);
            SortedMap<String, Object> members = new TreeMap<>();
            for (Element e : c.getEnclosedElements()) {
                if (!(e.getModifiers().contains(Modifier.PUBLIC)
                        || (name.equals(BASE) && e.getModifiers().contains(Modifier.PROTECTED)))) { continue; }
                String description = elements.getDocComment(e);
                if (description == null || description.trim().isEmpty()) { description = NO_DOC; }
                List<String> modifiers = new ArrayList<>();
                for (Modifier m : e.getModifiers()) { modifiers.add(m.toString()); }
                Collections.sort(modifiers);
                if (e.getKind() == ElementKind.METHOD || e.getKind() == ElementKind.CONSTRUCTOR) {
                    ExecutableElement method = (ExecutableElement) e;
                    List<Object> args = new ArrayList<>();
                    List<String> erased = new ArrayList<>();
                    for (VariableElement arg : method.getParameters()) {
                        args.add(obj("name", "arg" + args.size(), "type", type(arg.asType())));
                        erased.add(processingEnv.getTypeUtils().erasure(arg.asType()).toString());
                    }
                    List<String> thrown = new ArrayList<>();
                    for (TypeMirror t : method.getThrownTypes()) { thrown.add(type(t)); }
                    String id = e.getSimpleName() + "(" + String.join(",", erased) + ")";
                    members.put(id, obj("id", id, "kind", e.getKind().name().toLowerCase(Locale.ROOT),
                            "name", e.getKind() == ElementKind.CONSTRUCTOR ? c.getSimpleName().toString() : e.getSimpleName().toString(), "modifiers", modifiers,
                            "typeParameters", parameters(method.getTypeParameters()), "parameters", args,
                            "returnType", type(method.getReturnType()), "throws", thrown, "varargs", method.isVarArgs(),
                            "description", description));
                } else if (e.getKind() == ElementKind.FIELD || e.getKind() == ElementKind.ENUM_CONSTANT) {
                    String id = e.getSimpleName().toString();
                    members.put(id, obj("id", id, "kind", "field", "name", id, "modifiers", modifiers,
                            "type", type(e.asType()), "description", description));
                }
            }
            return obj("name", name, "kind", c.getKind().name().toLowerCase(Locale.ROOT), "modifiers", classModifiers,
                    "declaredAnnotations", annotations,
                    "description", name.equals(BASE) ? "Optional typed NPC script base; override only the hooks needed by this tab." : NO_DOC,
                    "typeParameters", parameters(c.getTypeParameters()), "superclass", type(c.getSuperclass()),
                    "interfaces", interfaces, "members", new ArrayList<>(members.values()));
        }
    }

    static String example(String name, String event) {
        return "public class Example_" + name + " extends " + BASE + " {\n"
                + "    @Override public void " + name + "(" + event + " event) {\n"
                + "        log(event.npc.getName());\n    }\n}\n";
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> rows(Object value) { return (List<Map<String, Object>>) value; }

    static String typeParameters(Object value) {
        List<String> params = new ArrayList<>();
        for (Map<String, Object> p : rows(value)) {
            List<String> bounds = new ArrayList<>();
            for (Object b : (List<?>) p.get("bounds")) { if (!b.equals("java.lang.Object")) { bounds.add(b.toString()); } }
            params.add(p.get("name") + (bounds.isEmpty() ? "" : " extends " + String.join(" & ", bounds)));
        }
        return params.isEmpty() ? "" : "<" + String.join(", ", params) + ">";
    }

    static String join(Object value) {
        List<String> parts = new ArrayList<>();
        for (Object p : (List<?>) value) { parts.add(p.toString()); }
        return String.join(", ", parts);
    }

    static String signature(Map<String, Object> m) {
        String modifiers = join(m.get("modifiers")).replace(", ", " ");
        if (m.get("kind").equals("field")) { return modifiers + " " + m.get("type") + " " + m.get("name"); }
        List<String> args = new ArrayList<>();
        List<Map<String, Object>> params = rows(m.get("parameters"));
        for (Map<String, Object> p : params) {
            String type = p.get("type").toString();
            if (Boolean.TRUE.equals(m.get("varargs")) && args.size() == params.size() - 1) { type = type.substring(0, type.length() - 2) + "..."; }
            args.add(type + " " + p.get("name"));
        }
        String generic = typeParameters(m.get("typeParameters"));
        return modifiers + " " + (generic.isEmpty() ? "" : generic + " ")
                + (m.get("kind").equals("constructor") ? "" : m.get("returnType") + " ")
                + m.get("name") + "(" + String.join(", ", args) + ")"
                + (((List<?>) m.get("throws")).isEmpty() ? "" : " throws " + join(m.get("throws")));
    }

    static Map<String, byte[]> render(Map<String, Object> catalog) {
        Map<String, byte[]> out = new LinkedHashMap<>();
        out.put("API.json", (JSON.toJson(catalog) + "\n").getBytes(StandardCharsets.UTF_8));
        StringBuilder api = new StringBuilder("# Java NPC API v1\n\nGenerated by `.\\dev.ps1 generateApiDocs --offline`; do not edit.\n\n");
        api.append(SCOPE).append("\n\n").append(CONTRACT).append("\n\n")
                .append("Signature fingerprint: `").append(catalog.get("signatureFingerprint")).append("`.\n\n")
                .append("Descriptions: ").append(NO_DOC).append(" Parameter labels `arg0`, `arg1`, ... are positional. ")
                .append("Read inherited members through superclass/interface links; generic type arguments are preserved on those links. ")
                .append("No constant values are read and no API instances are created.\n\n")
                .append("## Exclusions\n\nJava handlers: `").append(catalog.get("excludedJavaHandlers")).append("`.\n\n");
        for (Object excluded : (List<?>) catalog.get("excludedSurfaces")) { api.append("- ").append(excluded).append("\n"); }
        api.append("\n## Types\n\n");
        for (Map<String, Object> t : rows(catalog.get("types"))) {
            api.append("### ").append(t.get("name")).append("\n\n").append(t.get("description")).append("\n\n")
                    .append("Kind: `").append(join(t.get("modifiers"))).append(" ").append(t.get("kind"))
                    .append("`; declared annotations: `").append(join(t.get("declaredAnnotations")))
                    .append("`; type parameters: `").append(typeParameters(t.get("typeParameters")))
                    .append("`; superclass: `").append(t.get("superclass")).append("`; interfaces: `").append(t.get("interfaces")).append("`.\n\n");
            for (Map<String, Object> m : rows(t.get("members"))) {
                api.append("- `").append(signature(m)).append("` — ").append(m.get("description")).append("\n");
            }
            api.append("\n");
        }
        api.append("## External references (not expanded)\n\n");
        for (Object ref : (List<?>) catalog.get("externalReferences")) { api.append("- `").append(ref).append("`\n"); }
        out.put("API.md", api.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder hooks = new StringBuilder("# NPC hooks v1\n\nGenerated from `ScriptHooks.java()` in registry order; do not edit.\n\n");
        hooks.append(CONTRACT).append("\n\nAll examples are separate Java files, named after their public class. ")
                .append("`public void` with the exact event type is the recommended typed signature. ")
                .append("The plain-class binder also accepts a parameter supertype; overloads are rejected. ")
                .append("Payload fields, mutability and inheritance are in [API.md](API.md). ")
                .append("`event.setCanceled(true)` is valid only for a cancelable actual event; role subtypes differ. ")
                .append("Cancelable metadata below describes the declared event type, not every runtime subtype. ")
                .append("No frequency guarantee follows from the registry order.\n\n")
                .append("Forge string hooks remain outside NPC Java v1: the existing rule is uncapitalize(simple binary event name with `$` removed). ")
                .append("Excluded Java handlers: `").append(catalog.get("excludedJavaHandlers")).append("`.\n\n");
        for (Map<String, Object> h : rows(catalog.get("hooks"))) {
            hooks.append("## ").append(h.get("name")).append("\n\n`").append(h.get("signature")).append("`\n\n")
                    .append("Enum: `").append(h.get("enum")).append("`; cancelable on declared type: `")
                    .append(h.get("cancelableOnDeclaredEvent")).append("`. ").append(h.get("description"))
                    .append("\n\n```java\n").append(h.get("example")).append("```\n\n");
        }
        hooks.append("## Player and world\n\nThe existing `event.player` (IPlayer, interact and other player events) and `event.npc.getWorld()` (IWorld); ")
                .append("their members are in [API.md](API.md).\n\n```java\n").append(catalog.get("playerWorldExample")).append("```\n");
        hooks.append("\n## Optional action animation\n\n`Animators.of(event.npc)` returns a non-null `IAnimator`. Requires a live ")
                .append("`EntityAnimatedNpc` from customnpcentities 0.7.0; ordinary NPCs and a missing/incompatible mod return an inert handle ")
                .append("(`isAvailable=false`, play/queue/stop=false, clip empty, tick 0, clips empty). No change to `ICustomNpc`. ")
                .append("Call from the server thread; mutations elsewhere return false. Clip names are case-sensitive; unknown/null clips return false. ")
                .append("`play` replaces the action and clears the queue only on success; `stop` clears it and stops the action. ")
                .append("`queue` starts immediately when idle, otherwise appends FIFO, up to 32 pending clips shared by all tabs/handles of the entity. ")
                .append("The next clip starts at world tick END after the current action ends. External AI/actions may interrupt; the queue waits for idle. ")
                .append("Pending clips are discarded on death/entity unload and are not saved in NBT; script-tab reload does not clear the shared entity queue. No client locomotion or blending; the animation hooks above need RwAnimHooks=1. ")
                .append("`getClip`/`getTick` describe the action only (idle: empty/0); `getClips` returns a fresh sorted array.\n\n```java\n")
                .append(catalog.get("animationExample")).append("```\n\nJS: `var a = Java.type('noppes.npcs.rework.anim.Animators').of(event.npc);`\n");
        out.put("HOOKS.md", hooks.toString().getBytes(StandardCharsets.UTF_8));
        return out;
    }

    static String sha(byte[] bytes) throws Exception {
        StringBuilder s = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes)) { s.append(String.format(Locale.ROOT, "%02x", b & 255)); }
        return s.toString();
    }

    static void write(Path dir, Map<String, byte[]> files) throws IOException {
        Files.createDirectories(dir);
        for (Map.Entry<String, byte[]> entry : files.entrySet()) { Files.write(dir.resolve(entry.getKey()), entry.getValue()); }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) { throw new IllegalArgumentException("Usage: ApiDocs <output-directory>"); }
        Map<String, Object> catalog = catalog(false);
        write(Paths.get(args[0]), render(catalog));
        System.out.println("Generated API reference: " + rows(catalog.get("types")).size() + " types, "
                + rows(catalog.get("hooks")).size() + " hooks, " + catalog.get("signatureFingerprint"));
    }
}
