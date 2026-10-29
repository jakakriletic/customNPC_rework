package noppes.npcs.rework.script;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** M6.1 opt-in prototype, not registered with ScriptController.
 * Trusted source only; a loader is not a sandbox. Compilation never loads script classes.
 * The caller supplies the compiler classpath for its MCP/SRG environment explicitly.
 */
public final class JdkScriptCompiler {
    private static final List<String> OPTIONS = Collections.unmodifiableList(Arrays.asList("-source", "8", "-target", "8",
            "-encoding", "UTF-8", "-proc:none", "-implicit:none", "-g"));
    private final JavaCompiler compiler;
    public JdkScriptCompiler() { this(ToolProvider.getSystemJavaCompiler()); }
    // Deterministic absent-provider test without changing global state.
    JdkScriptCompiler(JavaCompiler compiler) { this.compiler = compiler; }

    public Result compile(Map<String, String> sources, String classpath) throws IOException {
        if (compiler == null) {
            return new Result(false, Collections.<String, byte[]>emptyMap(), Collections.singletonList(
                    new Problem("ERROR", "compiler.unavailable", "", -1, -1,
                            "Java scripting requires JDK 8 with a JavaCompiler provider; JS is unchanged.")), "");
        }
        if (sources.isEmpty()) { throw new IllegalArgumentException("No source units"); }
        List<JavaFileObject> units = new ArrayList<>();
        for (Map.Entry<String, String> source : sources.entrySet()) {
            if (!source.getKey().matches("[A-Za-z_$][\\w$]*(\\.[A-Za-z_$][\\w$]*)*")) {
                throw new IllegalArgumentException("Invalid binary name: " + source.getKey());
            }
            final String code = source.getValue();
            units.add(new SimpleJavaFileObject(uri(source.getKey(), JavaFileObject.Kind.SOURCE), JavaFileObject.Kind.SOURCE) {
                @Override public CharSequence getCharContent(boolean ignoreErrors) { return code; }
            });
        }
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StringWriter extra = new StringWriter();
        // No annotation processors or implicit source discovery from the game/CLI classpath.
        List<String> options = new ArrayList<>(OPTIONS);
        options.addAll(Arrays.asList("-classpath", classpath, "-sourcepath", ""));
        try (StandardJavaFileManager standard = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8);
             MemoryFiles files = new MemoryFiles(standard)) {
            boolean success = Boolean.TRUE.equals(compiler.getTask(extra, files, diagnostics, options, null, units).call());
            List<Problem> problems = new ArrayList<>();
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                problems.add(new Problem(diagnostic.getKind().name(), diagnostic.getCode(),
                        diagnostic.getSource() == null ? "" : diagnostic.getSource().getName(),
                        diagnostic.getLineNumber(), diagnostic.getColumnNumber(), diagnostic.getMessage(Locale.ROOT)));
            }
            Map<String, byte[]> bytes = new LinkedHashMap<>();
            // Never expose partially generated output on failure.
            if (success) {
                for (Map.Entry<String, ByteArrayOutputStream> output : files.output.entrySet()) {
                    bytes.put(output.getKey(), output.getValue().toByteArray());
                }
            }
            return new Result(success, bytes, problems, extra.toString());
        }
    }
    /** M6.4 cache key part: the same provider, JVM and options give the same bytes for the same input. */
    public String identity() {
        return (compiler == null ? "none" : compiler.getClass().getName()) + "|" + System.getProperty("java.version") + "|" + OPTIONS;
    }
    /** Fresh loader over compiled bytes; no class initialization until the caller explicitly executes. */
    public static ClassLoader newLoader(final Map<String, byte[]> code, ClassLoader apiParent) {
        return new ClassLoader(apiParent) {
            @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] bytes = code.get(name);
                if (bytes == null) { throw new ClassNotFoundException(name); }
                return defineClass(name, bytes, 0, bytes.length);
            }
        };
    }
    private static URI uri(String name, JavaFileObject.Kind kind) {
        return URI.create("memory:///" + name.replace('.', '/') + kind.extension);
    }
    private static final class MemoryFiles extends ForwardingJavaFileManager<StandardJavaFileManager> {
        final Map<String, ByteArrayOutputStream> output = new LinkedHashMap<>();
        MemoryFiles(StandardJavaFileManager delegate) { super(delegate); }
        @Override public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String name,
                JavaFileObject.Kind kind, FileObject sibling) throws IOException {
            if (kind != JavaFileObject.Kind.CLASS) { throw new IOException("Unexpected output: " + kind); }
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            output.put(name, buffer);
            return new SimpleJavaFileObject(uri(name, kind), kind) {
                @Override public OutputStream openOutputStream() { return buffer; }
            };
        }
    }
    public static final class Problem {
        public final String kind, code, file, message;
        public final long line, column;
        Problem(String kind, String code, String file, long line, long column, String message) {
            this.kind = kind; this.code = code; this.file = file;
            this.line = line; this.column = column; this.message = message;
        }
    }
    public static final class Result {
        public final boolean success;
        public final List<Problem> problems;
        public final String compilerOutput;
        private final Map<String, byte[]> bytes;
        Result(boolean success, Map<String, byte[]> bytes, List<Problem> problems, String compilerOutput) {
            this.success = success; this.bytes = bytes;
            this.problems = Collections.unmodifiableList(new ArrayList<>(problems));
            this.compilerOutput = compilerOutput;
        }
        public Map<String, byte[]> bytecode() {
            Map<String, byte[]> copy = new LinkedHashMap<>();
            for (Map.Entry<String, byte[]> entry : bytes.entrySet()) { copy.put(entry.getKey(), entry.getValue().clone()); }
            return copy;
        }
        /** Fresh loader; no class initialization until the caller explicitly executes. */
        public ClassLoader newLoader(ClassLoader apiParent) {
            if (!success) { throw new IllegalStateException("Compilation failed"); }
            return JdkScriptCompiler.newLoader(bytecode(), apiParent);
        }
    }
}
