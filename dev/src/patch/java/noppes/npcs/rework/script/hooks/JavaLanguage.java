package noppes.npcs.rework.script.hooks;

import java.util.Map;

/** M6.12 (Q184-Q187): Java in the existing script language selection.
 * The entry is added only when RwJavaClasspath is set, so the default language list, GUI and
 * world script folders stay the original. The existing ScriptController then loads
 * scripts/java/*.java as includes and lists them in the script GUIs; no new panel or packet.
 * P5 (Q263): every script GUI (NPC, player, forge, block, door, item) gets the same list.
 */
public final class JavaLanguage {
    /** File extension of Java includes in the world scripts/java folder. */
    public static final String EXTENSION = ".java";

    private JavaLanguage() { }

    /** Called once after ScriptController is built. Empty classpath = original language map. */
    public static boolean register(Map<String, String> languages, String classpath) {
        if (classpath == null || classpath.trim().isEmpty()) { return false; }
        languages.put(ScriptHooks.JAVA, EXTENSION);
        return true;
    }
}
