package noppes.npcs.rework.script.prototype;

import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.tools.ToolProvider;
import noppes.npcs.rework.script.JdkScriptCompiler;

/** Standalone controller-only experiment, outside the production source set.
 * Janino is never added to the mod or game runtime. */
public final class CompilerComparison {
    private static final Map<String,String> CASES = new LinkedHashMap<>();
    static {
        CASES.put("Basic", "public class Basic { public int run(){return 7;} }");
        CASES.put("Generic", "public class Generic { public int run(){ java.util.List<String> a=new java.util.ArrayList<String>(); a.add(\"seven\"); return a.size()+6;} }");
        CASES.put("Lambda", "public class Lambda { public int run(){ java.util.function.IntSupplier f=()->7; return f.getAsInt(); } }");
        CASES.put("Inner", "public class Inner { class Child { int n=7; } public int run(){ return new Child().n; } }");
        CASES.put("WrongGeneric", "public class WrongGeneric {\n public void run(){\n java.util.List<String> a=new java.util.ArrayList<String>();\n a.add(42);\n }}");
        CASES.put("Syntax", "public class Syntax {\n public void run(){ int x = ; }\n}");
    }
    private static final class Outcome {
        boolean compiled;
        Object value;
        Object diagnostics;
        ClassLoader loader;
    }
    private static Outcome javac(String name, String code) throws Exception {
        JdkScriptCompiler.Result result = new JdkScriptCompiler().compile(Collections.singletonMap(name, code), System.getProperty("java.class.path"));
        Outcome o=new Outcome(); o.compiled=result.success; o.diagnostics=result.problems;
        if (result.success) { o.loader=result.newLoader(CompilerComparison.class.getClassLoader()); }
        return o;
    }
    private static Outcome janino(Class<?> compiler, String name, String code) throws Exception {
        Outcome o=new Outcome(); Object c=compiler.newInstance();
        compiler.getMethod("setParentClassLoader", ClassLoader.class).invoke(c, CompilerComparison.class.getClassLoader());
        compiler.getMethod("setDebuggingInformation", boolean.class, boolean.class, boolean.class).invoke(c, true,true,true);
        try {
            compiler.getMethod("cook", String.class, java.io.Reader.class).invoke(c, name+".java", new StringReader(code));
            o.compiled=true; o.loader=(ClassLoader)compiler.getMethod("getClassLoader").invoke(c);
        } catch (InvocationTargetException failure) {
            Throwable cause=failure.getCause();
            if (!(cause instanceof Exception)) { throw failure; }
            Map<String,Object> error=new LinkedHashMap<>(); error.put("message", cause.toString());
            try {
                Object location=cause.getClass().getMethod("getLocation").invoke(cause);
                if (location!=null) {
                    for (String field : new String[]{"FileName","LineNumber","ColumnNumber"}) {
                        error.put(field, location.getClass().getMethod("get"+field).invoke(location));
                    }
                }
            } catch (ReflectiveOperationException ignored) { /* Non-compile failures remain visible. */ }
            o.diagnostics=error;
        }
        return o;
    }
    public static void main(String[] args) throws Exception {
        String backend=args[0]; String output=args[1];
        Map<String,Object> proof=new LinkedHashMap<>(); proof.put("backend",backend);
        proof.put("java",System.getProperty("java.runtime.version")); proof.put("success",false);
        try {
            if (backend.equals("absent")) {
                if (ToolProvider.getSystemJavaCompiler()!=null) { throw new AssertionError("Provider unexpectedly present in isolated JRE"); }
                JdkScriptCompiler.Result result=new JdkScriptCompiler().compile(Collections.singletonMap("Basic",CASES.get("Basic")), "");
                proof.put("diagnostics", result.problems);
                if (result.success || !result.problems.get(0).code.equals("compiler.unavailable")) { throw new AssertionError("Missing actionable compiler error"); }
                javax.script.ScriptEngine js=new javax.script.ScriptEngineManager().getEngineByName("nashorn");
                if (js==null || ((Number)js.eval("3+4")).intValue()!=7) { throw new AssertionError("Existing JS unavailable without compiler"); }
                proof.put("jsWithoutCompiler",true);
            } else {
                long coldStart=System.nanoTime();
                Class<?> janino=null;
                if (backend.equals("janino")) {
                    URLClassLoader libraries=new URLClassLoader(new URL[]{new File(args[2]).toURI().toURL(),new File(args[3]).toURI().toURL()},CompilerComparison.class.getClassLoader());
                    janino=libraries.loadClass("org.codehaus.janino.SimpleCompiler");
                }
                Outcome first=backend.equals("javac") ? javac("Basic",CASES.get("Basic")) : janino(janino,"Basic",CASES.get("Basic"));
                proof.put("coldBasicNanos",System.nanoTime()-coldStart);
                if (!first.compiled) { throw new AssertionError("Basic cold compilation failed: "+first.diagnostics); }
                List<Map<String,Object>> rows=new ArrayList<>(); proof.put("cases", rows);
                for (Map.Entry<String,String> fixture: CASES.entrySet()) {
                    String name=fixture.getKey();
                    Outcome result=backend.equals("javac") ? javac(name,fixture.getValue()) : janino(janino,name,fixture.getValue());
                    Map<String,Object> row=new LinkedHashMap<>(); row.put("name",name); row.put("compiled",result.compiled); row.put("diagnostics",result.diagnostics);
                    rows.add(row);
                    if (result.compiled && !name.equals("WrongGeneric") && !name.equals("Syntax")) {
                        Class<?> c=result.loader.loadClass(name); row.put("value",c.getMethod("run").invoke(c.newInstance()));
                        if (!Integer.valueOf(7).equals(row.get("value"))) { throw new AssertionError("Execution "+name); }
                    }
                    if (backend.equals("javac")) {
                        boolean expected=!name.equals("WrongGeneric") && !name.equals("Syntax");
                        if (result.compiled!=expected) { throw new AssertionError("Java 8 matrix "+name); }
                        if (!expected) {
                            @SuppressWarnings("unchecked") List<JdkScriptCompiler.Problem> problems=(List<JdkScriptCompiler.Problem>)result.diagnostics;
                            long line=name.equals("WrongGeneric") ? 4 : 2;
                            if (!problems.stream().anyMatch(p->p.kind.equals("ERROR") && p.file.endsWith(name+".java") && p.line==line && p.column>0)) { throw new AssertionError("Source location "+name); }
                        }
                    } else if ((name.equals("Basic") || name.equals("Inner")) && !result.compiled) { throw new AssertionError("Janino baseline "+name); }
                    // Janino language/type differences are measured outcomes, not a Java acceptance waiver.
                    if (name.equals("Syntax") && result.compiled) { throw new AssertionError("Compiler accepted syntax error"); }
                }
                List<Long> warm=new ArrayList<>();
                // Exactly five fresh warm compilations of the same ordinary unit for both providers.
                for(int i=0;i<5;i++) {
                    long start=System.nanoTime();
                    Outcome result=backend.equals("javac") ? javac("Basic",CASES.get("Basic")) : janino(janino,"Basic",CASES.get("Basic"));
                    warm.add(System.nanoTime()-start);
                    if(!result.compiled) { throw new AssertionError("Warm compilation"); }
                }
                proof.put("warmBasicNanos",warm);
            }
            proof.put("success",true);
        } catch(Throwable failure) { proof.put("error",failure.toString()); failure.printStackTrace(); }
        Files.write(Paths.get(output),new GsonBuilder().setPrettyPrinting().create().toJson(proof).getBytes(StandardCharsets.UTF_8));
        if (!Boolean.TRUE.equals(proof.get("success"))) { System.exit(1); }
    }
}
