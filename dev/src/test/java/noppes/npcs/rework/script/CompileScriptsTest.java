package noppes.npcs.rework.script;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class CompileScriptsTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    private String cp() { return System.getProperty("java.class.path"); }
    private Path write(String name, String text) throws Exception {
        Path file = temp.getRoot().toPath().resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, text.getBytes(StandardCharsets.UTF_8));
        return file;
    }
    private Path manifest(String... files) throws Exception {
        CompileScripts.TabInput tab = new CompileScripts.TabInput();
        tab.id="1"; tab.files=Arrays.asList(files);
        CompileScripts.Manifest input = new CompileScripts.Manifest();
        input.schema=1; input.tabs=Arrays.asList(tab);
        return write("scripts.json", new Gson().toJson(input));
    }
    @Test public void typedApiIncludesLambdaAndNoStaticInitialization() throws Exception {
        String key="rw.m67.script.initialized";
        System.clearProperty(key);
        write("Main.java", "package sample; public class Main extends noppes.npcs.rework.script.NpcScript {"
                + "static { System.setProperty(\""+key+"\",\"BAD\"); }"
                + "@Override public void tick(noppes.npcs.api.event.NpcEvent.UpdateEvent e) {"
                + " java.util.function.Supplier<String> s=()->Helper.name(e.npc); log(s.get()); }}");
        write("inc/Helper.java", "package sample; public class Helper {"
                + " public static String name(noppes.npcs.api.entity.ICustomNpc n){return n.getName();}}");
        CompileScripts.Report r=CompileScripts.compile(manifest("Main.java","inc/Helper.java"), cp());
        assertTrue(new Gson().toJson(r), r.success);
        assertEquals("sample.Main",r.tabs.get(0).main);
        assertEquals(new java.util.HashSet<>(Arrays.asList("sample.Main","sample.Helper")),new java.util.HashSet<>(r.tabs.get(0).classes));
        assertNull(System.getProperty(key));
        assertTrue(r.elapsedNanos>0);
        assertEquals(64,r.tabs.get(0).inputFingerprint.length());
    }
    @Test public void includeErrorMatchesCompilerAndNamesOriginalFileLineColumn() throws Exception {
        String main="public class Main { Helper h; }";
        String helper="public class Helper {\n public int n=\"wrong\";\n}";
        write("Main.java",main); write("inc/Helper.java",helper);
        CompileScripts.Report r=CompileScripts.compile(manifest("Main.java","inc/Helper.java"),cp());
        assertFalse(r.success); assertTrue(r.tabs.get(0).classes.isEmpty());
        CompileScripts.Problem p=r.tabs.get(0).problems.stream().filter(x->x.kind.equals("ERROR")).findFirst().get();
        Map<String,String> units=new LinkedHashMap<>(); units.put("Main",main); units.put("Helper",helper);
        JdkScriptCompiler.Problem direct=new JdkScriptCompiler().compile(units,cp()).problems.stream()
                .filter(x->x.kind.equals("ERROR")).findFirst().get();
        assertEquals("inc/Helper.java",p.file); assertEquals("include inc/Helper.java",p.origin);
        assertEquals(2,p.line); assertTrue(p.column>0); assertEquals(direct.column,p.column); assertEquals(direct.code,p.code);
        assertEquals(BytecodeCache.key(new JdkScriptCompiler().identity(),cp(),units),r.tabs.get(0).inputFingerprint);
    }
    @Test public void tabsAreSeparateCompilationsAndFailuresDoNotHideLaterTabs() throws Exception {
        write("a/Example.java","public class Example { int n=\"bad\"; }");
        write("b/Example.java","public class Example { }");
        Path input=write("scripts.json","{\"schema\":1,\"tabs\":[{\"id\":\"a\",\"files\":[\"a/Example.java\"]},"
                + "{\"id\":\"b\",\"files\":[\"b/Example.java\"]}]}");
        CompileScripts.Report r=CompileScripts.compile(input,cp());
        assertFalse(r.success); assertEquals(2,r.tabs.size());
        assertFalse(r.tabs.get(0).success); assertTrue(r.tabs.get(1).success);
    }
    @Test public void missingFileIsFailureWithPath() throws Exception {
        CompileScripts.Report r=CompileScripts.compile(manifest("Missing.java"),cp());
        assertFalse(r.success); assertEquals("input.read",r.tabs.get(0).problems.get(0).code);
        assertEquals("Missing.java",r.tabs.get(0).problems.get(0).file);
    }
    @Test public void malformedEmptyAndDuplicateManifestFailClosed() throws Exception {
        for (String text : Arrays.asList("{", "null", "{\"schema\":1,\"tabs\":[]}",
                "{\"schema\":2,\"tabs\":[{\"id\":\"a\",\"files\":[\"Main.java\"]}]}",
                "{\"schema\":1,\"tabs\":[{\"id\":\"a\",\"files\":[\"Main.java\"]},{\"id\":\"a\",\"files\":[\"Main.java\"]}]}",
                "{\"schema\":1,\"tabs\":[{\"id\":\"a\",\"files\":[\"Main.java\",\"./Main.java\"]}]}")) {
            CompileScripts.Report r=CompileScripts.compile(write("scripts.json",text),cp());
            assertFalse(r.success); assertTrue(r.tabs.isEmpty()); assertEquals("input.invalid",r.problems.get(0).code);
        }
    }
    @Test public void duplicateUnitAndNoPublicTypeUseSharedSourceErrors() throws Exception {
        write("Main.java","public class Main {}"); write("inc/Helper.java","public class Main {}");
        CompileScripts.Report r=CompileScripts.compile(manifest("Main.java","inc/Helper.java"),cp());
        assertFalse(r.success); assertEquals("unit.duplicate",r.tabs.get(0).problems.get(0).code);
        assertEquals("inc/Helper.java",r.tabs.get(0).problems.get(0).file);
        write("Main.java","// no public class\nclass Main {}");
        r=CompileScripts.compile(manifest("Main.java","inc/Helper.java"),cp());
        assertEquals("unit.noPublicType",r.tabs.get(0).problems.get(0).code);
        assertEquals("Main.java",r.tabs.get(0).problems.get(0).file);
    }
    @Test public void absentProviderReportsCompilerUnavailable() throws Exception {
        write("Main.java","public class Main {}");
        CompileScripts.Report r=CompileScripts.compile(manifest("Main.java"),cp(),new JdkScriptCompiler(null));
        assertFalse(r.success); assertEquals("compiler.unavailable",r.tabs.get(0).problems.get(0).code);
        assertTrue(r.tabs.get(0).classes.isEmpty());
    }
    public static class TripwireProcessor extends AbstractProcessor {
        static { System.setProperty("rw.m67.processor", "BAD"); }
        @Override public boolean process(Set<? extends TypeElement> types, RoundEnvironment round) { return false; }
    }
    @Test public void cliNeverLoadsClasspathAnnotationProcessor() throws Exception {
        System.clearProperty("rw.m67.processor");
        write("META-INF/services/javax.annotation.processing.Processor",getClass().getName()+"$TripwireProcessor");
        write("Main.java","public class Main {}");
        CompileScripts.Report r=CompileScripts.compile(manifest("Main.java"),temp.getRoot()+java.io.File.pathSeparator+cp());
        assertTrue(r.success); assertNull(System.getProperty("rw.m67.processor"));
    }
    @Test public void utf8BomAndSpacesAreAccepted() throws Exception {
        write("space dir/Main.java","\uFEFFpublic class Main { String s=\"čšž\"; }");
        Path path=manifest("space dir/Main.java");
        write("scripts.json","\uFEFF"+new String(Files.readAllBytes(path),StandardCharsets.UTF_8));
        assertTrue(CompileScripts.compile(path,cp()).success);
    }
}
