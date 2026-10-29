package noppes.npcs.rework.script.hooks;

import java.util.Map;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/** M6.12 (Q184-Q187): Java in the existing script language selection, NPC handlers only.
 * The entry is added only when RwJavaClasspath is set, so the default language list, GUI and
 * world script folders stay the original. The existing ScriptController then loads
 * scripts/java/*.java as includes and lists them in the NPC GUI; no new panel or packet.
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

    /** Language list for the non-NPC script GUIs: their Java branch is v2 (ScriptHooks.V1_EXCLUDED). */
    public static NBTTagList withoutJava(NBTTagList languages) {
        NBTTagList out = new NBTTagList();
        for (int i = 0; i < languages.tagCount(); i++) {
            NBTTagCompound language = languages.getCompoundTagAt(i);
            if (!ScriptHooks.JAVA.equals(language.getString("Language"))) { out.appendTag(language); }
        }
        return out;
    }
}
