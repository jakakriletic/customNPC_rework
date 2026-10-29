package noppes.npcs.rework.script.hooks;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.junit.Test;

public class JavaLanguageTest {
    @Test public void emptyClasspathKeepsOriginalLanguages() {
        for (String classpath : new String[] {null, "", "  \t"}) {
            Map<String, String> languages = new HashMap<>();
            languages.put("ECMAScript", ".js");
            assertFalse(JavaLanguage.register(languages, classpath));
            assertEquals(1, languages.size());
            assertEquals(".js", languages.get("ECMAScript"));
        }
    }

    @Test public void classpathAddsJavaNextToExistingLanguages() {
        Map<String, String> languages = new HashMap<>();
        languages.put("ECMAScript", ".js");
        assertTrue(JavaLanguage.register(languages, "C:/scripts/classpath.txt"));
        assertEquals(".java", languages.get(ScriptHooks.JAVA));
        assertEquals(".js", languages.get("ECMAScript"));
        assertEquals(2, languages.size());
    }

    @Test public void otherHandlersKeepEveryLanguageExceptJavaInOrder() {
        NBTTagList list = new NBTTagList();
        for (String name : new String[] {"Java", "ECMAScript", "lua"}) {
            NBTTagCompound language = new NBTTagCompound();
            language.setString("Language", name);
            list.appendTag(language);
        }
        NBTTagList out = JavaLanguage.withoutJava(list);
        assertEquals(2, out.tagCount());
        assertEquals("ECMAScript", out.getCompoundTagAt(0).getString("Language"));
        assertEquals("lua", out.getCompoundTagAt(1).getString("Language"));
        assertEquals(3, list.tagCount());
        assertEquals(0, JavaLanguage.withoutJava(new NBTTagList()).tagCount());
    }
}
