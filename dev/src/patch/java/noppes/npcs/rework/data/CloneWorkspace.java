package noppes.npcs.rework.data;

import com.google.gson.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import net.minecraft.nbt.*;

/** Disk-only editing workspace. No entity normalization, script execution or model registration. */
public final class CloneWorkspace {
    public static final String MARKER = "clone-workspace.json";
    private static final int MANIFEST_LIMIT = 512 * 1024, TOTAL_LIMIT = 64 * 1024 * 1024;
    private static final LinkOption[] NOFOLLOW = {LinkOption.NOFOLLOW_LINKS};
    private CloneWorkspace() { }

    // Package-private injection seam: tests exercise failed writes and publication races on real files.
    interface BeforePublish { void run(Path staging, Path target) throws IOException; }
    private static final BeforePublish NORMAL = (staging, target) -> { };

    public static Path unpack(Path root, String id, NBTTagCompound input) throws IOException {
        return unpack(root, id, input, NORMAL);
    }
    static Path unpack(Path root, String id, NBTTagCompound input, BeforePublish hook) throws IOException {
        Path base = directory(root), target = workspace(base, id);
        absent(target);
        NBTTagCompound expected = input.copy();
        Path staging = Files.createTempDirectory(base, ".clone-workspace-");
        try {
            write(staging.resolve("clone.json"), utf8(ReadableClone.encode(expected)));
            JsonObject manifest = new JsonObject();
            manifest.addProperty("$format", "customnpcs-clone-workspace");
            manifest.addProperty("version", 1);
            Integer count = tabCount(expected);
            manifest.add("scriptTabCount", count == null ? JsonNull.INSTANCE : new JsonPrimitive(count));
            JsonArray sources = new JsonArray();
            StringBuilder guide = new StringBuilder("# Clone workspace\n\nEdit settings in clone.json; edit code in the listed files.\n"
                    + "Keep the manifest and all listed files. Inline and external edits merge only when unambiguous; conflicting edits reject the entire import.\n"
                    + "To change tab structure, pack and unpack again. File extensions do not change ScriptLanguage.\n"
                    + "Includes, models, skins and libraries remain external dependencies. animations.json is the exact Clips string, not a validated animation library.\n"
                    + "pack creates a new clone; it never overwrites this workspace or an existing clone.\n"
                    + "For new live copies, /rwcreature reload then spawn <id>, or deploy <id>. Existing NPCs keep their own clone data.\n\n"
                    + "Commands: /rwcreature unpack clone <tab> <id> <name...>; unpack npc <target> <id>; pack <id> <tab> <name...>\n\n");
            if (count != null) {
                String ext = extension(expected);
                for (int i = 0; i < count; i++) {
                    extract(staging, expected, sources, guide, "script", i, scriptFile(i, ext));
                }
            }
            extract(staging, expected, sources, guide, "clips", -1, "animations.json");
            manifest.add("sources", sources);
            write(staging.resolve(MARKER), utf8(StrictCloneJson.write(manifest)));
            write(staging.resolve("WORKSPACE.md"), utf8(guide.toString()));
            checkPath(base); checkPath(staging); absent(target);
            if (!ReadableClone.same(expected, readDirectory(staging))) { throw error("Workspace verification failed"); }
            hook.run(staging, target); // A competing destination can appear AFTER the last existence check.
            checkPath(base); checkPath(staging);
            Files.move(staging, target); // No REPLACE_EXISTING and no ATOMIC_MOVE (which can replace).
            return target;
        } catch (IllegalArgumentException failure) { throw error(failure.getMessage()); }
        finally { cleanup(staging); }
    }

    public static NBTTagCompound read(Path root, String id) throws IOException {
        Path base = existingDirectory(root);
        return readDirectory(workspace(base, id));
    }
    public static NBTTagCompound loadClone(Path cloneRoot, int tab, String name) throws IOException {
        return load(cloneFile(existingDirectory(cloneRoot), tab, name));
    }
    public static Path pack(Path root, String id, Path cloneRoot, int tab, String name, boolean readable) throws IOException {
        return pack(root, id, cloneRoot, tab, name, readable, NORMAL);
    }
    static Path pack(Path root, String id, Path cloneRoot, int tab, String name, boolean readable, BeforePublish hook) throws IOException {
        // All inputs are read and validated before any output directory is made.
        NBTTagCompound expected = read(root, id);
        validateName(tab, name);
        Path source = workspace(existingDirectory(root), id);
        Path requested = cloneRoot.toAbsolutePath().normalize().resolve(Integer.toString(tab));
        if (requested.startsWith(source)) { throw error("Clone output overlaps source workspace"); }
        Path base = directory(cloneRoot), parent = directory(base.resolve(Integer.toString(tab)));
        Path target = cloneFile(base, tab, name);
        absent(target);
        Path staging = Files.createTempDirectory(parent, ".clone-pack-");
        try {
            Path candidate = staging.resolve("clone.json");
            ReadableCloneFiles.save(candidate.toFile(), expected, readable);
            checkPath(parent); checkPath(candidate); absent(target);
            if (!ReadableClone.same(expected, load(candidate))) { throw error("Typed clone verification failed"); }
            hook.run(candidate, target);
            checkPath(parent); checkPath(candidate);
            Files.move(candidate, target);
            return target;
        } finally { cleanup(staging); }
    }

    private static void extract(Path dir, NBTTagCompound nbt, JsonArray sources, StringBuilder guide,
                                String kind, int tab, String file) throws IOException {
        NBTTagCompound owner = owner(nbt, kind, tab);
        String key = kind.equals("script") ? "Script" : "Clips";
        if (owner == null || !owner.hasKey(key, 8)) { return; }
        byte[] bytes;
        try { bytes = utf8(owner.getString(key)); }
        catch (CharacterCodingException invalid) {
            guide.append(nbtPath(kind, tab)).append(" stays inline: unpaired UTF-16 surrogate cannot be encoded losslessly as UTF-8.\n");
            return;
        }
        if (file.startsWith("scripts/")) { Files.createDirectories(dir.resolve("scripts")); }
        write(dir.resolve(file), bytes);
        JsonObject source = new JsonObject(); source.addProperty("kind", kind);
        if (kind.equals("script")) { source.addProperty("tab", tab); }
        source.addProperty("file", file); source.addProperty("baselineSha256", hash(bytes)); sources.add(source);
        guide.append(file).append(" -> ").append(nbtPath(kind, tab)).append('\n');
    }

    private static NBTTagCompound readDirectory(Path dir) throws IOException {
        existingDirectory(dir);
        if (Files.exists(dir.resolve("creature.json"), NOFOLLOW)) { throw error("Both creature.json and " + MARKER + " are present"); }
        checkTotal(dir);
        try {
            JsonElement parsed = StrictCloneJson.read(decode(readBytes(dir.resolve(MARKER), MANIFEST_LIMIT)));
            if (!parsed.isJsonObject()) { throw error("Manifest must be an object"); }
            JsonObject manifest = parsed.getAsJsonObject();
            fields(manifest, "$format", "version", "scriptTabCount", "sources");
            if (!string(manifest, "$format").equals("customnpcs-clone-workspace") || integer(manifest, "version") != 1) {
                throw error("Unknown clone workspace format/version");
            }
            NBTTagCompound result = load(dir.resolve("clone.json"));
            Integer count = tabCount(result);
            JsonElement declared = manifest.get("scriptTabCount");
            if (declared == null || (declared.isJsonNull() ? count != null : count == null || integer(manifest, "scriptTabCount") != count)) {
                throw error("/Scripts structure/tab count changed; pack then unpack before restructuring tabs");
            }
            JsonElement list = manifest.get("sources");
            if (list == null || !list.isJsonArray()) { throw error("Manifest sources must be an array"); }
            Set<String> targets = new HashSet<>(), files = new HashSet<>();
            for (JsonElement entry : list.getAsJsonArray()) {
                if (!entry.isJsonObject()) { throw error("Source must be an object"); }
                JsonObject source = entry.getAsJsonObject();
                String kind = string(source, "kind"), file = string(source, "file");
                int tab = -1;
                if (kind.equals("script")) {
                    fields(source, "kind", "tab", "file", "baselineSha256"); tab = integer(source, "tab");
                    if (count == null || tab < 0 || tab >= count) { throw error("Invalid script tab " + tab); }
                    boolean valid = false;
                    for (String ext : new String[]{"js", "java", "txt"}) { valid |= file.equals(scriptFile(tab, ext)); }
                    if (!valid) { throw error("Invalid source file " + file); }
                } else if (kind.equals("clips")) {
                    fields(source, "kind", "file", "baselineSha256");
                    if (!file.equals("animations.json")) { throw error("Invalid clips file " + file); }
                } else { throw error("Unknown source kind " + kind); }
                String path = nbtPath(kind, tab), baseline = string(source, "baselineSha256");
                if (!baseline.matches("[0-9a-f]{64}") || !targets.add(path) || !files.add(file.toLowerCase(Locale.ROOT))) {
                    throw error("Invalid hash or duplicate source " + file + " " + path);
                }
                NBTTagCompound owner = owner(result, kind, tab);
                String key = kind.equals("script") ? "Script" : "Clips";
                if (owner == null || !owner.hasKey(key, 8)) { throw error("Missing/string type changed: " + path); }
                byte[] bytes = readBytes(dir.resolve(file), ReadableClone.MAX_BYTES);
                String inline = owner.getString(key), external = decode(bytes);
                boolean inlineSame;
                try { inlineSame = baseline.equals(hash(utf8(inline))); }
                catch (CharacterCodingException invalid) { inlineSame = false; }
                boolean externalSame = baseline.equals(hash(bytes));
                if (!inlineSame && !externalSame && !inline.equals(external)) {
                    throw error("Conflicting edits: " + file + " and " + path);
                }
                if (inlineSame) { owner.setString(key, external); }
            }
            // Enforce the same output bound after merging expanded external text.
            ReadableClone.encode(result);
            return result;
        } catch (IllegalArgumentException failure) { throw error(failure.getMessage()); }
    }
    private static Integer tabCount(NBTTagCompound n) {
        NBTBase value = n.getTag("Scripts");
        if (!(value instanceof NBTTagList)) { return null; }
        NBTTagList tabs = (NBTTagList)value;
        // Even an empty list's element type is data. Non-compound lists stay entirely inline.
        return tabs.getTagType() == 10 ? tabs.tagCount() : null;
    }
    private static NBTTagCompound owner(NBTTagCompound n, String kind, int tab) {
        if (kind.equals("clips")) { return n.hasKey("RwRig", 10) ? n.getCompoundTag("RwRig") : null; }
        Integer count = tabCount(n);
        return count != null && tab >= 0 && tab < count ? n.getTagList("Scripts", 10).getCompoundTagAt(tab) : null;
    }
    private static String nbtPath(String kind, int tab) { return kind.equals("clips") ? "/RwRig/Clips" : "/Scripts/" + tab + "/Script"; }
    private static String extension(NBTTagCompound n) {
        String language = n.getString("ScriptLanguage");
        return language.equals("Java") ? "java" : language.equals("ECMAScript") ? "js" : "txt";
    }
    private static String scriptFile(int tab, String ext) {
        return tab == 0 ? "script." + ext : String.format(Locale.ROOT, "scripts/tab-%04d.%s", tab + 1, ext);
    }
    private static void fields(JsonObject object, String... allowed) throws IOException {
        Set<String> names = new HashSet<>(Arrays.asList(allowed));
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (!names.remove(entry.getKey())) { throw error("Unknown field " + entry.getKey()); }
        }
        if (!names.isEmpty()) { throw error("Missing fields " + names); }
    }
    private static String string(JsonObject object, String key) throws IOException {
        JsonElement v = object.get(key);
        if (v == null || !v.isJsonPrimitive() || !v.getAsJsonPrimitive().isString()) { throw error("Expected string " + key); }
        return v.getAsString();
    }
    private static int integer(JsonObject object, String key) throws IOException {
        JsonElement v = object.get(key);
        if (v == null || !v.isJsonPrimitive() || !v.getAsJsonPrimitive().isNumber()) { throw error("Expected integer " + key); }
        try { return new BigDecimal(v.getAsString()).intValueExact(); }
        catch (ArithmeticException | NumberFormatException invalid) { throw error("Invalid integer " + key); }
    }
    static byte[] utf8(String text) throws CharacterCodingException {
        ByteBuffer buffer = StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(text));
        byte[] bytes = new byte[buffer.remaining()]; buffer.get(bytes); return bytes;
    }
    private static String decode(byte[] bytes) throws CharacterCodingException {
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
    }
    private static String hash(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes); StringBuilder out = new StringBuilder();
            for (byte b : digest) { out.append(String.format(Locale.ROOT, "%02x", b & 255)); } return out.toString();
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static NBTTagCompound load(Path file) throws IOException {
        try { return ReadableCloneFiles.parse(decode(readBytes(file, ReadableClone.MAX_BYTES))); }
        catch (Exception failure) { throw new IOException(file + ": " + failure.getMessage(), failure); }
    }
    private static byte[] readBytes(Path file, int limit) throws IOException {
        checkPath(file);
        if (!Files.isRegularFile(file, NOFOLLOW) || Files.size(file) > limit) { throw error("Not a regular file or exceeds limit: " + file); }
        try (InputStream in = Files.newInputStream(file); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int n, total = 0;
            while ((n = in.read(buffer)) != -1) {
                total += n; if (total > limit) { throw error("File exceeds limit: " + file); } out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }
    private static void write(Path file, byte[] bytes) throws IOException {
        if (bytes.length > ReadableClone.MAX_BYTES) { throw error("File exceeds 32 MiB: " + file); }
        Files.write(file, bytes, StandardOpenOption.CREATE_NEW);
    }
    private static Path workspace(Path root, String id) throws IOException {
        if (id == null || !id.matches("[a-z0-9_]{1,48}")) { throw error("Invalid workspace id"); }
        validateWindowsName(id);
        return root.resolve(id);
    }
    private static Path cloneFile(Path root, int tab, String name) throws IOException {
        validateName(tab, name); return root.resolve(Integer.toString(tab)).resolve(name + ".json");
    }
    private static void validateName(int tab, String name) throws IOException {
        if (tab < 1 || tab > 9) { throw error("Clone tab must be 1-9"); }
        validateWindowsName(name);
    }
    private static void validateWindowsName(String name) throws IOException {
        if (name == null || name.isEmpty() || name.length() > 180 || name.matches(".*[<>:\"/\\\\|?*\\p{Cntrl}].*")
                || name.endsWith(".") || name.endsWith(" ") || name.equals(".") || name.equals("..")
                || name.toUpperCase(Locale.ROOT).matches("(CON|PRN|AUX|NUL|COM[1-9¹²³]|LPT[1-9¹²³])(\\..*)?")) {
            throw error("Invalid Windows file name: " + name);
        }
    }
    /** Check every existing ancestor, including junctions (real path differs even when isSymbolicLink is false). */
    private static void checkPath(Path path) throws IOException {
        Path absolute = path.toAbsolutePath().normalize(), current = absolute.getRoot();
        for (Path part : absolute) {
            current = current.resolve(part);
            if (Files.exists(current, NOFOLLOW) && (Files.isSymbolicLink(current) || !current.toRealPath().equals(current))) {
                throw error("Link/reparse path is not allowed: " + current);
            }
        }
    }
    private static Path directory(Path path) throws IOException {
        Path dir = path.toAbsolutePath().normalize(); checkPath(dir);
        Files.createDirectories(dir); return existingDirectory(dir);
    }
    private static Path existingDirectory(Path dir) throws IOException {
        dir = dir.toAbsolutePath().normalize(); checkPath(dir);
        if (!Files.isDirectory(dir, NOFOLLOW)) { throw error("Not a directory: " + dir); } return dir;
    }
    private static void absent(Path target) throws IOException {
        checkPath(target);
        if (Files.exists(target, NOFOLLOW)) { throw new FileAlreadyExistsException(target.toString()); }
    }
    private static void checkTotal(Path dir) throws IOException {
        long total = 0;
        try (java.util.stream.Stream<Path> paths = Files.walk(dir)) {
            Iterator<Path> iterator = paths.iterator();
            while (iterator.hasNext()) {
                Path p = iterator.next(); checkPath(p);
                if (Files.isRegularFile(p, NOFOLLOW)) { total += Files.size(p); }
                else if (!Files.isDirectory(p, NOFOLLOW)) { throw error("Not a regular workspace entry: " + p); }
                if (total > TOTAL_LIMIT) { throw error("Workspace exceeds 64 MiB"); }
            }
        }
    }
    private static void cleanup(Path own) throws IOException {
        if (!Files.exists(own, NOFOLLOW)) { return; }
        checkPath(own);
        // Only our randomly named staging tree; never follow links or remove the published/user target.
        List<Path> paths = new ArrayList<>();
        try (java.util.stream.Stream<Path> walk = Files.walk(own)) { walk.forEach(paths::add); }
        Collections.reverse(paths);
        for (Path p : paths) { checkPath(p); Files.delete(p); }
    }
    private static IOException error(String message) { return new IOException(message); }
}
