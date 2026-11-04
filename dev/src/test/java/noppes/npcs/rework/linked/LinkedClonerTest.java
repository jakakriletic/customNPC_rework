package noppes.npcs.rework.linked;

import org.junit.Test;
import static org.junit.Assert.*;

public class LinkedClonerTest {
    @Test public void cloneNamesMayBeReadableButNotPaths() {
        assertTrue(LinkedCloner.validName("Castle Guard"));
        assertTrue(LinkedCloner.validName("Guard-2.0"));
        assertTrue(LinkedCloner.validName("Stra" + (char)0x17E + "a"));
        for (String name : new String[]{"", ".", "..", "../guard", "..\\guard", "C:guard", "guard/child", "guard.", " guard", "guard ", "a\n"}) {
            assertFalse(name, LinkedCloner.validName(name));
        }
        assertFalse(LinkedCloner.validName(null));
    }
    @Test public void windowsDeviceNamesCannotBecomeGroupFiles() {
        for (String name : new String[]{"CON", "con.json", "CON .json", "PRN", "AUX", "NUL", "COM1", "Lpt9", "bad?", "bad*", "bad|", "bad<", "bad>"}) {
            assertFalse(name, LinkedCloner.validName(name));
        }
        assertTrue(LinkedCloner.validName("CONguard"));
    }
    @Test public void filenameLimitUsesUtf8Bytes() {
        char[] text = new char[120];
        java.util.Arrays.fill(text, 'g');
        assertTrue(LinkedCloner.validName(new String(text)));
        assertFalse(LinkedCloner.validName(new String(text) + "g"));
        java.util.Arrays.fill(text, (char)0x17E);
        assertFalse(LinkedCloner.validName(new String(text)));
    }
    @Test public void feedbackNeverClaimsAnUnloadedTotalOrCompletedPendingTransfers() {
        String text = LinkedUpdates.message("Guard", 3, 2);
        assertTrue(text.contains("updated 3 loaded NPCs; pending 2"));
        assertTrue(text.contains("total unknown"));
        assertTrue(LinkedUpdates.message("Guard", 0, 0).contains("updated 0 loaded NPCs; pending 0"));
    }
}
