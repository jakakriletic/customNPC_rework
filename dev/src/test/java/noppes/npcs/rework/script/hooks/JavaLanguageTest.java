package noppes.npcs.rework.script.hooks;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
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

    @Test public void autoClasspathAlsoAddsJava() {
        Map<String, String> languages = new HashMap<>();
        assertTrue(JavaLanguage.register(languages, " auto "));
        assertEquals(".java", languages.get(ScriptHooks.JAVA));
    }
}
