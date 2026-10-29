package noppes.npcs.rework.script.hooks;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** M6.2: binds the public methods of a Java tab class to registry hooks.
 * A method named like a hook with a wrong shape is an error, never a silently missing hook (Q128).
 */
public final class JavaHookBinder {
    private static final Pattern COMMENTS = Pattern.compile("/\\*.*?\\*/|//[^\\n]*", Pattern.DOTALL);
    private static final Pattern PACKAGE = Pattern.compile("\\bpackage\\s+([A-Za-z_$][\\w$]*(?:\\s*\\.\\s*[A-Za-z_$][\\w$]*)*)\\s*;");
    private static final Pattern PUBLIC_TYPE = Pattern.compile(
            "\\bpublic\\s+(?:(?:final|abstract|strictfp)\\s+)*(class|interface|enum)\\s+([A-Za-z_$][\\w$]*)");

    public static final class BindException extends Exception {
        public final String code;
        BindException(String code, String message) { super(message); this.code = code; }
    }

    private JavaHookBinder() { }

    /** Binary name of the public top-level type in one compilation unit; javac requires
     * the unit name to match it. Comments are skipped; string literals are not parsed. */
    public static String unitName(String source) throws BindException {
        String code = COMMENTS.matcher(source).replaceAll(" ");
        Matcher type = PUBLIC_TYPE.matcher(code);
        if (!type.find()) { throw new BindException("unit.noPublicType", "A Java tab needs one public top-level class"); }
        Matcher pkg = PACKAGE.matcher(code);
        String name = type.group(2);
        return pkg.find() ? pkg.group(1).replaceAll("\\s", "") + "." + name : name;
    }

    /** Hook name -> method. The class must be public with a public no-argument constructor. */
    public static Map<String, Method> bind(Class<?> script, List<ScriptHooks.Hook> hooks) throws BindException {
        int mod = script.getModifiers();
        if (!Modifier.isPublic(mod) || Modifier.isAbstract(mod) || script.isInterface()) {
            throw new BindException("unit.notInstantiable", script.getName() + " must be a public, non-abstract class");
        }
        Map<String, ScriptHooks.Hook> byName = new LinkedHashMap<>();
        for (ScriptHooks.Hook hook : hooks) { byName.put(hook.name, hook); }
        Map<String, Method> bound = new LinkedHashMap<>();
        Map<String, String> seen = new LinkedHashMap<>();
        for (Class<?> c = script; c != null && c != Object.class; c = c.getSuperclass()) {
            Method[] methods = c.getDeclaredMethods();
            // getDeclaredMethods has no defined order; sort so the reported error is deterministic.
            Arrays.sort(methods, (a, b) -> a.toString().compareTo(b.toString()));
            for (Method method : methods) {
                ScriptHooks.Hook hook = byName.get(method.getName());
                if (hook == null || method.isSynthetic() || method.isBridge()) { continue; }
                String signature = Arrays.toString(method.getParameterTypes());
                String previous = seen.get(hook.name);
                if (previous != null) {
                    if (previous.equals(signature)) { continue; } // overridden in a subclass
                    throw new BindException("hook.ambiguous", "Hook " + hook.name + " is declared more than once in " + script.getName());
                }
                seen.put(hook.name, signature);
                int m = method.getModifiers();
                Class<?>[] params = method.getParameterTypes();
                if (!Modifier.isPublic(m) || Modifier.isStatic(m) || params.length != 1 || !params[0].isAssignableFrom(hook.event)) {
                    throw new BindException("hook.signature", "Hook " + method + " must be: public void "
                            + hook.name + "(" + hook.event.getName().replace('$', '.') + " event)");
                }
                bound.put(hook.name, method);
            }
        }
        return bound;
    }
}
