package noppes.npcs.rework.diag;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Zapis posnetka na disk.
 *
 * <p>Posnetek gre v {@code logs/rwdiag/}, ker mora biti dosegljiv tudi, ko se svet ne
 * nalozi. Zapise se v dveh oblikah: berljiva tabela za cloveka in JSON za primerjavo
 * pred/po. Ime datoteke vsebuje casovno znacko, zato se prejsnja meritev nikoli ne
 * prepise.
 */
public final class DiagDump {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private DiagDump() {
    }

    public static File directory() {
        return new File("logs", "rwdiag");
    }

    /**
     * Zapise posnetek in vrne zapisano datoteko s tabelo, ali {@code null}, ce zapis ni
     * uspel. Instrumentacija nikoli ne sme podreti serverja, zato se napaka vrne kot
     * {@code null} in ne kot izjema.
     */
    public static File write(DiagSnapshot snapshot, String label) {
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
        String safeLabel = label == null || label.isEmpty() ? "dump" : sanitize(label);
        File dir = directory();
        if (!dir.isDirectory() && !dir.mkdirs()) {
            return null;
        }
        File text = new File(dir, "rwdiag-" + stamp + "-" + safeLabel + ".txt");
        File json = new File(dir, "rwdiag-" + stamp + "-" + safeLabel + ".json");
        try {
            writeFile(text, snapshot.toText());
            writeFile(json, snapshot.toJson());
            return text;
        } catch (IOException e) {
            return null;
        }
    }

    private static void writeFile(File file, String content) throws IOException {
        Writer writer = new OutputStreamWriter(new FileOutputStream(file), UTF8);
        try {
            writer.write(content);
            writer.flush();
        } finally {
            writer.close();
        }
    }

    static String sanitize(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_';
            out.append(ok ? c : '-');
        }
        return out.length() == 0 ? "dump" : out.toString();
    }
}
