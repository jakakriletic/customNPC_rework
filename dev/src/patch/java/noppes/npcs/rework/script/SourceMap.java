package noppes.npcs.rework.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** M6.5 (D-090): where a script error comes from. JS joins the inline tab code and its includes into
 * one evaluated text, so a line of that text maps back to the segment and its own line. Java compiles
 * every part as its own unit, so a frame maps through its unit path ("pkg/Name.java") to the origin.
 */
public final class SourceMap {
    private final List<Integer> starts = new ArrayList<>();
    private final List<String> origins = new ArrayList<>();
    /** Nashorn names evaluated code like this in stack frames. */
    public static final String EVAL = "<eval>";
    private static final int MAX_CAUSES = 8;

    /** Next segment starts at the given 1-based line of the joined text; segments are added in order. */
    public void add(int startLine, String origin) {
        if (startLine < 1 || (!starts.isEmpty() && startLine < starts.get(starts.size() - 1))) {
            throw new IllegalArgumentException("Segment start " + startLine);
        }
        starts.add(startLine);
        origins.add(origin);
    }

    /** "origin:localLine", or null when the line lies before the first segment or is unknown. */
    public String locate(int line) {
        for (int i = starts.size() - 1; i >= 0; i--) {
            if (line >= starts.get(i)) { return origins.get(i) + ":" + (line - starts.get(i) + 1); }
        }
        return null;
    }

    /** Newlines in a text, i.e. how many lines a segment ending in a newline occupies. */
    public static int lines(String text) {
        int n = 0;
        for (int i = 0; i < text.length(); i++) { if (text.charAt(i) == '\n') { n++; } }
        return n;
    }

    /** JS: line (and column, when known) of the first script location in the cause chain. */
    public String locateScript(Throwable error) {
        for (Throwable t : chain(error)) {
            if (t instanceof javax.script.ScriptException && ((javax.script.ScriptException) t).getLineNumber() > 0) {
                javax.script.ScriptException s = (javax.script.ScriptException) t;
                String at = locate(s.getLineNumber());
                if (at != null) { return s.getColumnNumber() >= 0 ? at + ":" + s.getColumnNumber() : at; }
            }
        }
        for (Throwable t : chain(error)) {
            for (StackTraceElement frame : t.getStackTrace()) {
                if (EVAL.equals(frame.getFileName()) && frame.getLineNumber() > 0) {
                    String at = locate(frame.getLineNumber());
                    if (at != null) { return at; }
                }
            }
        }
        return null;
    }

    /** Java: unit path of a frame, "pkg/Name.java" from its class and source file attribute. */
    public static String unitPath(StackTraceElement frame) {
        if (frame.getFileName() == null) { return null; }
        String cls = frame.getClassName();
        int dot = cls.lastIndexOf('.');
        return dot < 0 ? frame.getFileName() : cls.substring(0, dot).replace('.', '/') + "/" + frame.getFileName();
    }

    /** Java runtime error: each cause with its frames down to the deepest script frame, script frames
     * tagged with their tab/include origin; the remaining game/reflection frames are counted only.
     * Without any script frame the whole stack is kept. */
    public static String runtime(String hook, Throwable error, Map<String, String> origins) {
        StringBuilder out = new StringBuilder("Runtime error in hook ").append(hook).append(':');
        boolean first = true;
        for (Throwable t : chain(error)) {
            out.append('\n').append(first ? "" : "Caused by: ").append(t);
            first = false;
            StackTraceElement[] frames = t.getStackTrace();
            int last = -1;
            for (int i = 0; i < frames.length; i++) { if (origins.containsKey(unitPath(frames[i]))) { last = i; } }
            int shown = last < 0 ? frames.length : last + 1;
            for (int i = 0; i < shown; i++) {
                String origin = origins.get(unitPath(frames[i]));
                out.append("\n\tat ").append(frames[i]);
                if (origin != null) { out.append(" [").append(origin).append(']'); }
            }
            if (shown < frames.length) { out.append("\n\t... ").append(frames.length - shown).append(" more"); }
        }
        return out.toString();
    }

    private static List<Throwable> chain(Throwable error) {
        List<Throwable> out = new ArrayList<>();
        Map<Throwable, Boolean> seen = new IdentityHashMap<>();
        for (Throwable t = error; t != null && out.size() < MAX_CAUSES && seen.put(t, Boolean.TRUE) == null; t = t.getCause()) {
            out.add(t);
        }
        return Collections.unmodifiableList(out);
    }
}
