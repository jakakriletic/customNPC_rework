package noppes.npcs.rework.script;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** M6.7 compile-only CLI. Deliberately never creates a loader, resolves hooks, constructs a script,
 * boots Forge or runs annotation processors. Runtime binding remains a separate validation step.
 */
public final class CompileScripts {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static final class Manifest {
        public int schema;
        public List<TabInput> tabs;
    }
    public static final class TabInput {
        public String id;
        /** First file is the main unit, subsequent files are includes, just like one runtime tab. */
        public List<String> files;
    }
    public static final class Problem {
        public String kind, code, file, origin, message;
        public long line, column;
        Problem(String kind, String code, String file, String origin, long line, long column, String message) {
            this.kind=kind; this.code=code; this.file=file; this.origin=origin;
            this.line=line; this.column=column; this.message=message;
        }
    }
    public static final class TabResult {
        public String id, main, inputFingerprint;
        public boolean success;
        public long elapsedNanos;
        public String compilerOutput = "";
        public final List<String> classes = new ArrayList<>();
        public final List<Problem> problems = new ArrayList<>();
    }
    public static final class Report {
        public final int schema = 1;
        public final String operation = "compile-only";
        public String compiler;
        public boolean success;
        public long elapsedNanos;
        public final List<TabResult> tabs = new ArrayList<>();
        public final List<Problem> problems = new ArrayList<>();
        public final List<String> inputs = new ArrayList<>();
    }

    private CompileScripts() { }

    public static Report compile(Path manifest, String classpath) {
        return compile(manifest, classpath, new JdkScriptCompiler());
    }

    // Same service used by main, with an absent-provider seam for deterministic tests.
    static Report compile(Path manifest, String classpath, JdkScriptCompiler compiler) {
        long start = System.nanoTime();
        Report report = new Report();
        report.compiler = compiler.identity();
        manifest = manifest.toAbsolutePath().normalize();
        report.inputs.add(manifest.toString());
        try {
            String text = read(manifest);
            // PowerShell 5 writes UTF-8 with BOM; accept it for manifests and source files.
            Manifest input = JSON.fromJson(text, Manifest.class);
            if (input == null || input.schema != 1 || input.tabs == null || input.tabs.isEmpty()) {
                throw new IllegalArgumentException("Expected schema=1 and a nonempty tabs array");
            }
            Set<String> ids = new HashSet<>();
            // Validate all entries before compiling anything; a broken manifest is never a partial success.
            for (TabInput tab : input.tabs) {
                if (tab == null || tab.id == null || tab.id.trim().isEmpty() || !ids.add(tab.id)
                        || tab.files == null || tab.files.isEmpty()) {
                    throw new IllegalArgumentException("Every tab needs a unique nonempty id and nonempty files array");
                }
                Set<Path> paths = new HashSet<>();
                for (String file : tab.files) {
                    if (file == null || file.trim().isEmpty() || !file.endsWith(".java")) {
                        throw new IllegalArgumentException("Every file must be a nonempty .java path");
                    }
                    Path path = manifest.getParent().resolve(file).normalize().toAbsolutePath();
                    if (!paths.add(path)) { throw new IllegalArgumentException("Duplicate file in tab " + tab.id + ": " + file); }
                    report.inputs.add(path.toString());
                }
            }
            for (TabInput tab : input.tabs) { report.tabs.add(compileTab(tab, manifest.getParent(), classpath, compiler)); }
            report.success = report.tabs.stream().allMatch(tab -> tab.success);
        } catch (Exception e) {
            report.problems.add(new Problem("ERROR", "input.invalid", manifest.toString(), "manifest", -1, -1, e.toString()));
        }
        report.elapsedNanos = System.nanoTime() - start;
        return report;
    }

    private static TabResult compileTab(TabInput tab, Path base, String classpath, JdkScriptCompiler compiler) {
        long start = System.nanoTime();
        TabResult out = new TabResult();
        out.id = tab.id;
        String current = tab.files.get(0);
        Map<String, String> filesByOrigin = new LinkedHashMap<>();
        try {
            Map<String, String> labelled = new LinkedHashMap<>();
            for (int i = 0; i < tab.files.size(); i++) {
                current = tab.files.get(i);
                String origin = i == 0 ? "tab " + tab.id : "include " + current;
                labelled.put(origin, read(base.resolve(current)));
                filesByOrigin.put(origin, current);
            }
            JavaSources input = new JavaSources(labelled);
            out.main = input.main;
            out.inputFingerprint = BytecodeCache.key(compiler.identity(), classpath, input.units);
            JdkScriptCompiler.Result result = compiler.compile(input.units, classpath);
            out.success = result.success;
            out.compilerOutput = result.compilerOutput;
            for (JdkScriptCompiler.Problem p : result.problems) {
                String unit = JavaSources.unitPath(p.file);
                String origin = input.origins.get(unit);
                out.problems.add(new Problem(p.kind, p.code, filesByOrigin.getOrDefault(origin, unit),
                        origin == null ? "" : origin, p.line, p.column, p.message));
            }
            out.classes.addAll(result.bytecode().keySet());
        } catch (JavaSources.SourceException e) {
            out.problems.add(new Problem("ERROR", e.code, filesByOrigin.getOrDefault(e.origin, current),
                    e.origin, -1, -1, e.getMessage()));
        } catch (Exception e) {
            out.problems.add(new Problem("ERROR", "input.read", current, "tab " + tab.id, -1, -1, e.toString()));
        }
        out.elapsedNanos = System.nanoTime() - start;
        return out;
    }

    private static String read(Path path) throws IOException {
        String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        return text.startsWith("\uFEFF") ? text.substring(1) : text;
    }

    /** Args: manifest JSON, report JSON, compiler classpath. Exit 0=all compiled, 1=failed, 2=usage/output. */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: CompileScripts <manifest.json> <report.json> <compiler-classpath>");
            System.exit(2); return;
        }
        Report report = compile(Paths.get(args[0]), args[2]);
        String json = JSON.toJson(report);
        System.out.println(json);
        try {
            Path target = Paths.get(args[1]).toAbsolutePath().normalize();
            for (String input : report.inputs) {
                Path path = Paths.get(input);
                if (target.equals(path) || (Files.exists(target) && Files.exists(path) && Files.isSameFile(target, path))) {
                    throw new IOException("Report would overwrite input: " + target);
                }
            }
            Files.createDirectories(target.getParent());
            Files.write(target, (json + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.println("report.write: " + e); System.exit(2); return;
        }
        System.exit(report.success ? 0 : 1);
    }
}
