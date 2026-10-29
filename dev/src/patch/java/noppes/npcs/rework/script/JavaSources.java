package noppes.npcs.rework.script;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Ordered compilation units and source origins shared by the NPC runtime and offline CLI.
 * This is the M6.2 naming rule (comments skipped, string literals not parsed), not a Java parser.
 * No game classes, script loaders or class initialization are involved.
 */
public final class JavaSources {
    private static final Pattern COMMENTS = Pattern.compile("/\\*.*?\\*/|//[^\\n]*", Pattern.DOTALL);
    private static final Pattern PACKAGE = Pattern.compile("\\bpackage\\s+([A-Za-z_$][\\w$]*(?:\\s*\\.\\s*[A-Za-z_$][\\w$]*)*)\\s*;");
    private static final Pattern PUBLIC_TYPE = Pattern.compile(
            "\\bpublic\\s+(?:(?:final|abstract|strictfp)\\s+)*(class|interface|enum)\\s+([A-Za-z_$][\\w$]*)");

    public static final class SourceException extends Exception {
        public final String code;
        public final String origin;
        SourceException(String code, String message) { this(code, message, ""); }
        SourceException(String code, String message, String origin) { super(message); this.code = code; this.origin = origin; }
    }

    public final Map<String, String> units;
    public final Map<String, String> origins;
    public final String main;

    /** Origin label -> source, in runtime order (main tab first, then includes). */
    public JavaSources(Map<String, String> sources) throws SourceException {
        Map<String, String> code = new LinkedHashMap<>();
        Map<String, String> locations = new LinkedHashMap<>();
        String first = null;
        for (Map.Entry<String, String> source : sources.entrySet()) {
            String name;
            try { name = unitName(source.getValue()); }
            catch (SourceException e) { throw new SourceException(e.code, e.getMessage(), source.getKey()); }
            if (code.put(name, source.getValue()) != null) {
                throw new SourceException("unit.duplicate", "Type " + name + " is declared twice in this tab", source.getKey());
            }
            locations.put(name.replace('.', '/') + ".java", source.getKey());
            if (first == null) { first = name; }
        }
        units = Collections.unmodifiableMap(code);
        origins = Collections.unmodifiableMap(locations);
        main = first;
    }

    public static String unitName(String source) throws SourceException {
        String code = COMMENTS.matcher(source).replaceAll(" ");
        Matcher type = PUBLIC_TYPE.matcher(code);
        if (!type.find()) { throw new SourceException("unit.noPublicType", "A Java tab needs one public top-level class"); }
        Matcher pkg = PACKAGE.matcher(code);
        String name = type.group(2);
        return pkg.find() ? pkg.group(1).replaceAll("\\s", "") + "." + name : name;
    }

    public static String unitPath(String diagnosticFile) {
        return diagnosticFile.startsWith("/") ? diagnosticFile.substring(1) : diagnosticFile;
    }
}
