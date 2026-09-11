/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.util.text.translation.I18n
 */
package noppes.npcs;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.Arrays;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.LogWriter;
import noppes.npcs.entity.EntityNPCInterface;

public class NoppesStringUtils {
    static final int[] illegalChars = new int[]{34, 60, 62, 124, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 58, 42, 63, 92, 47};

    public static String cleanFileName(String badFileName) {
        StringBuilder cleanName = new StringBuilder();
        for (int i = 0; i < badFileName.length(); ++i) {
            char c = badFileName.charAt(i);
            if (Arrays.binarySearch(illegalChars, (int)c) >= 0) continue;
            cleanName.append(c);
        }
        return cleanName.toString();
    }

    public static String removeHidden(String text) {
        int codePoint;
        StringBuilder newString = new StringBuilder(text.length());
        block3: for (int offset = 0; offset < text.length(); offset += Character.charCount(codePoint)) {
            codePoint = text.codePointAt(offset);
            switch (Character.getType(codePoint)) {
                case 0: 
                case 16: 
                case 18: 
                case 19: {
                    continue block3;
                }
            }
            newString.append(Character.toChars(codePoint));
        }
        return newString.toString();
    }

    public static String formatText(String text, Object ... obs) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        text = NoppesStringUtils.translate(text);
        for (Object ob : obs) {
            if (ob instanceof EntityPlayer) {
                String username = ((EntityPlayer)ob).getDisplayNameString();
                text = text.replace("{player}", username);
                text = text.replace("@p", username);
                continue;
            }
            if (!(ob instanceof EntityNPCInterface)) continue;
            text = text.replace("@npc", ((EntityNPCInterface)((Object)ob)).getName());
        }
        text = text.replace("&", Character.toChars(167)[0] + "");
        return text;
    }

    public static void setClipboardContents(String aString) {
        StringSelection stringSelection = new StringSelection(aString);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, (arg0, arg1) -> {});
    }

    public static String getClipboardContents() {
        boolean hasTransferableText;
        String result = "";
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        boolean bl = hasTransferableText = contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
        if (hasTransferableText) {
            try {
                result = (String)contents.getTransferData(DataFlavor.stringFlavor);
            }
            catch (Exception ex) {
                LogWriter.except(ex);
            }
        }
        return NoppesStringUtils.removeHidden(result);
    }

    public static String translate(Object ... arr) {
        String s = "";
        for (Object str : arr) {
            s = s + I18n.translateToLocal((String)str.toString());
        }
        return s;
    }

    public static String[] splitLines(String s) {
        return s.split("\r\n|\r|\n");
    }

    public static String newLine() {
        return System.getProperty("line.separator");
    }

    public static int parseInt(String s, int i) {
        try {
            return Integer.parseInt(s);
        }
        catch (NumberFormatException numberFormatException) {
            return i;
        }
    }

    static {
        Arrays.sort(illegalChars);
    }
}

