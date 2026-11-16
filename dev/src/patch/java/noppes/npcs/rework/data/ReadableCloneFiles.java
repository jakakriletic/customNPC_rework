package noppes.npcs.rework.data;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.Arrays;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.util.NBTJsonUtil;

/** Only the clone controllers use this adapter; network NBT and all other data stay unchanged. */
public final class ReadableCloneFiles {
    private ReadableCloneFiles() { }

    /** A root key is reserved, even for an unknown value. Strings/nested keys cannot select the format. */
    public static boolean hasEnvelope(String s) {
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') {
                int begin = i++;
                while (i < s.length()) {
                    char q = s.charAt(i);
                    if (q == '\\') { i += 2; continue; }
                    if (q == '"') { break; } i++;
                }
                if (depth == 1 && i < s.length()) {
                    int end = i + 1; while (end < s.length() && " \r\n\t".indexOf(s.charAt(end)) >= 0) { end++; }
                    if (end < s.length() && s.charAt(end) == ':') {
                        try { if ("$format".equals(StrictCloneJson.read(s.substring(begin,i+1)).getAsString())) { return true; } }
                        catch (IllegalArgumentException ex) { /* Legacy keys keep their old syntax. */ }
                    }
                }
            } else if (c == '{' || c == '[') { depth++; }
            else if (c == '}' || c == ']') { depth--; }
        }
        return false;
    }
    public static NBTTagCompound load(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (!hasEnvelope(text)) {
            try { return NBTJsonUtil.LoadFile(file); }
            catch (NBTJsonUtil.JsonException ex) { throw new IOException(ex.getMessage(),ex); }
        }
        if (bytes.length > ReadableClone.MAX_BYTES) { throw new IOException("Readable clone exceeds 32 MiB: " + file); }
        try {
            // Malformed UTF-8 is an error, not replacement characters silently changing script source.
            text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            return ReadableClone.decode(text);
        } catch (IllegalArgumentException ex) { throw new IOException(file + ": " + ex.getMessage(),ex); }
    }
    public static void save(File file, NBTTagCompound data, boolean readable) throws IOException {
        if (!readable) {
            try { NBTJsonUtil.SaveFile(file,data); }
            catch (NBTJsonUtil.JsonException ex) { throw new IOException(ex.getMessage(),ex); }
            return;
        }
        final NBTTagCompound expected = data.copy(); final String text;
        try { text = ReadableClone.encode(expected); }
        catch (IllegalArgumentException ex) { throw new IOException(ex.getMessage(),ex); }
        Files.createDirectories(file.getAbsoluteFile().getParentFile().toPath());
        if (file.isFile()) {
            byte[] old = Files.readAllBytes(file.toPath());
            if (!hasEnvelope(new String(old,StandardCharsets.UTF_8))) {
                Path backup = file.toPath().resolveSibling(file.getName() + ".v0.bak");
                if (!Files.exists(backup)) {
                    // The original bytes, not a parsed/reformatted legacy representation.
                    Files.copy(file.toPath(),backup);
                    if (!Arrays.equals(old,Files.readAllBytes(backup))) { throw new IOException("Legacy backup verification failed: " + backup); }
                } else if (!Files.isRegularFile(backup)) { throw new IOException("Legacy backup is not a file: " + backup); }
            }
        }
        writeGuide(file.getAbsoluteFile().getParentFile());
        SafeFileWriter.writeUtf8(file,text,new SafeFileWriter.Validator() {
            public void validate(File candidate) throws IOException {
                if (!ReadableClone.same(expected,load(candidate))) { throw new IOException("Typed clone verification failed: " + candidate); }
            }
        });
    }
    private static void writeGuide(File directory) throws IOException {
        Path guide = new File(directory,"CLONE-FORMAT.md").toPath();
        // Never replace a user's description, including edited generated guides.
        if (!Files.exists(guide)) { Files.write(guide,ReadableClone.guide().getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE_NEW); }
    }
}
