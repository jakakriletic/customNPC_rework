package noppes.npcs.rework.script;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class JdkScriptCompilerTest {
    private JdkScriptCompiler.Result compile(String source) throws Exception {
        return new JdkScriptCompiler().compile(Collections.singletonMap("Example", source), System.getProperty("java.class.path"));
    }
    @Test public void fullJava8WithInnerClassAndLambda() throws Exception {
        JdkScriptCompiler.Result r = compile("public class Example { class Inner { int n=7; } public int run(){"
                + "java.util.function.Supplier<Integer> s=()->new Inner().n; java.util.List<Integer> a=java.util.Arrays.asList(s.get()); return a.get(0); }}");
        assertTrue(r.success);
        assertTrue(r.bytecode().containsKey("Example$Inner"));
        Class<?> c = r.newLoader(getClass().getClassLoader()).loadClass("Example");
        assertEquals(7, c.getMethod("run").invoke(c.newInstance()));
    }
    @Test public void wrongGenericTypeReportsSourceLocationAndNoOutput() throws Exception {
        JdkScriptCompiler.Result r = compile("public class Example {\n public void run(){\n java.util.List<String> a=new java.util.ArrayList<String>();\n a.add(42);\n }}");
        assertFalse(r.success);
        assertTrue(r.bytecode().isEmpty());
        assertTrue(r.problems.stream().anyMatch(p -> p.kind.equals("ERROR") && p.file.endsWith("Example.java") && p.line==4 && p.column>0));
    }
    @Test public void absentCompilerGivesActionableErrorWithoutFallback() throws Exception {
        JdkScriptCompiler.Result r = new JdkScriptCompiler(null).compile(Collections.singletonMap("Example", ""), "");
        assertFalse(r.success);
        assertEquals("compiler.unavailable", r.problems.get(0).code);
        assertTrue(r.problems.get(0).message.contains("JDK 8"));
    }
    @Test public void compileDoesNotInitializeAndEachLoaderHasOwnStaticState() throws Exception {
        String key = "rw.m61.unit.initialized";
        System.clearProperty(key);
        JdkScriptCompiler.Result r = compile("public class Example { static { System.setProperty(\""+key+"\",\"yes\"); } public static int n; }");
        assertTrue(r.success);
        assertNull(System.getProperty(key));
        Class<?> a = r.newLoader(getClass().getClassLoader()).loadClass("Example");
        assertNull(System.getProperty(key));
        Class<?> b = r.newLoader(getClass().getClassLoader()).loadClass("Example");
        a.getField("n").setInt(null, 9);
        assertEquals(0, b.getField("n").getInt(null));
        System.clearProperty(key);
    }
    @Test public void failedBatchDiscardsPartialClassesAndOutputIsDefensive() throws Exception {
        Map<String,String> source = new LinkedHashMap<>();
        source.put("Good", "public class Good {}"); source.put("Bad", "public class Bad { int a = \"wrong\"; }");
        assertTrue(new JdkScriptCompiler().compile(source, "").bytecode().isEmpty());
        JdkScriptCompiler.Result r = compile("public class Example {}");
        r.bytecode().get("Example")[0]=0;
        assertEquals((byte)0xca, r.bytecode().get("Example")[0]);
    }
    public static class TripwireProcessor extends AbstractProcessor {
        static { System.setProperty("rw.m61.processor", "loaded"); }
        @Override public boolean process(Set<? extends TypeElement> types, RoundEnvironment round) { return false; }
    }
    @Test public void classpathAnnotationProcessorIsNeverInitialized() throws Exception {
        System.clearProperty("rw.m61.processor");
        Path directory=Files.createTempDirectory("m61-processor");
        Path service=directory.resolve("META-INF/services/javax.annotation.processing.Processor");
        Files.createDirectories(service.getParent());
        Files.write(service, Collections.singletonList(getClass().getName()+"$TripwireProcessor"), StandardCharsets.UTF_8);
        try {
            String cp=directory + java.io.File.pathSeparator + new java.io.File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            JdkScriptCompiler.Result r=new JdkScriptCompiler().compile(Collections.singletonMap("Example","public class Example {}"),cp);
            assertTrue(r.success);
            assertNull(System.getProperty("rw.m61.processor"));
        } finally {
            Files.delete(service); Files.delete(service.getParent()); Files.delete(service.getParent().getParent()); Files.delete(directory);
            System.clearProperty("rw.m61.processor");
        }
    }
}
