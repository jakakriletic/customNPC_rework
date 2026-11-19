package noppes.npcs.rework.data;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;
import noppes.npcs.util.NBTJsonUtil;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class CloneWorkspaceTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    private Path root() throws IOException { return temp.newFolder().toPath(); }
    private interface Attempt { void run() throws Exception; }
    private static void refused(Attempt attempt) throws Exception {
        try { attempt.run(); fail("Invalid workspace accepted"); }
        catch (IOException expected) { assertNotNull(expected.getMessage()); }
    }
    private static NBTTagCompound sample(String language) {
        NBTTagCompound n = new NBTTagCompound(); n.setString("ScriptLanguage", language); n.setBoolean("ScriptEnabled", false);
        n.setString("Name", "Original"); n.setLong("mod-extra", Long.MAX_VALUE);
        NBTTagList tabs = new NBTTagList();
        for (String text : new String[]{"\ufeff// Ž 🦊\r\nline\rnext\n", "", "last line", "surrogate \ud800"}) {
            NBTTagCompound tab = new NBTTagCompound(); tab.setString("Script", text);
            tab.setString("ScriptList", "deliberately wrong type"); tab.setLong("Console", 7); tabs.appendTag(tab);
        }
        NBTTagCompound wrong = new NBTTagCompound(); wrong.setInteger("Script", 7); tabs.appendTag(wrong);
        tabs.appendTag(new NBTTagCompound()); n.setTag("Scripts", tabs);
        NBTTagCompound rig = new NBTTagCompound(); rig.setString("Model", "unknown_model"); rig.setString("Clips", "{invalid clips}\r\n");
        n.setTag("RwRig", rig); return n;
    }
    private static void equal(NBTTagCompound expected, NBTTagCompound actual) { assertTrue("Typed NBT equality", ReadableClone.same(expected, actual)); }
    private static void text(Path file, String value) throws IOException { Files.write(file, value.getBytes(StandardCharsets.UTF_8)); }
    private static JsonObject manifest(Path dir) throws IOException { return StrictCloneJson.read(new String(Files.readAllBytes(dir.resolve(CloneWorkspace.MARKER)), StandardCharsets.UTF_8)).getAsJsonObject(); }
    private static void manifest(Path dir, JsonObject doc) throws IOException { text(dir.resolve(CloneWorkspace.MARKER), StrictCloneJson.write(doc)); }
    private static void inline(Path dir, NBTTagCompound n) throws IOException { text(dir.resolve("clone.json"), ReadableClone.encode(n)); }
    private static Map<String, byte[]> snapshot(Path root) throws IOException {
        Map<String, byte[]> out = new TreeMap<>();
        try (java.util.stream.Stream<Path> files = Files.walk(root)) {
            for (Path p : (Iterable<Path>)files.filter(Files::isRegularFile)::iterator) { out.put(root.relativize(p).toString(), Files.readAllBytes(p)); }
        } return out;
    }
    private static void unchanged(Map<String, byte[]> expected, Path dir) throws IOException {
        Map<String, byte[]> actual = snapshot(dir); assertEquals(expected.keySet(), actual.keySet());
        for (String key : expected.keySet()) { assertArrayEquals(key, expected.get(key), actual.get(key)); }
    }

    @Test public void entireCorpusBothFormatsRoundtripWithoutEntityConstruction() throws Exception {
        Path work = root(), clones = root(); int count = 0;
        for (Path source : new Path[]{Paths.get("testworld/customnpcs/clones"), Paths.get("../examples/clones")}) {
            assertTrue(Files.isDirectory(source));
            try (java.util.stream.Stream<Path> files = Files.walk(source)) {
                for (Path p : (Iterable<Path>)files.filter(f -> f.toString().endsWith(".json"))::iterator) {
                    byte[] original = Files.readAllBytes(p); NBTTagCompound n = NBTJsonUtil.LoadFile(p.toFile());
                    for (boolean readable : new boolean[]{false, true}) {
                        Path input = clones.resolve("input.json"); ReadableCloneFiles.save(input.toFile(), n, readable);
                        String id = "corpus_" + count + "_" + readable;
                        CloneWorkspace.unpack(work, id, ReadableCloneFiles.load(input.toFile()));
                        equal(n, CloneWorkspace.read(work, id));
                        Path output = CloneWorkspace.pack(work, id, clones, 1, id, readable);
                        equal(n, ReadableCloneFiles.load(output.toFile()));
                    }
                    assertArrayEquals(original, Files.readAllBytes(p)); count++;
                }
            }
        }
        assertTrue("Full corpus including sparse Konjenik", count >= 89);
        System.out.println("CLONEWORKSPACE corpus=" + count + " input/output formats=2");
    }
    @Test public void exactBytesAllTabsLanguagesAndInlineSurrogates() throws Exception {
        for (String language : new String[]{"Java", "ECMAScript", "python", ""}) {
            Path work = root(); NBTTagCompound n = sample(language);
            if (language.isEmpty()) { n.removeTag("ScriptLanguage"); }
            Path dir = CloneWorkspace.unpack(work, "npc", n);
            String ext = language.equals("Java") ? "java" : language.equals("ECMAScript") ? "js" : "txt";
            assertArrayEquals(CloneWorkspace.utf8(n.getTagList("Scripts", 10).getCompoundTagAt(0).getString("Script")), Files.readAllBytes(dir.resolve("script." + ext)));
            assertEquals(0, Files.size(dir.resolve("scripts/tab-0002." + ext)));
            assertArrayEquals("last line".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(dir.resolve("scripts/tab-0003." + ext)));
            assertArrayEquals("{invalid clips}\r\n".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(dir.resolve("animations.json")));
            assertFalse(Files.exists(dir.resolve("scripts/tab-0004." + ext)));
            assertFalse(Files.exists(dir.resolve("scripts/tab-0005." + ext)));
            assertFalse(Files.exists(dir.resolve("scripts/tab-0006." + ext)));
            assertTrue(new String(Files.readAllBytes(dir.resolve("WORKSPACE.md")), StandardCharsets.UTF_8).contains("/Scripts/3/Script stays inline"));
            equal(n, CloneWorkspace.read(work, "npc"));
        }
    }
    @Test public void absentWrongTypeEmptyListsAndFloatBitsRemainExact() throws Exception {
        for (NBTBase scripts : new NBTBase[]{null, new NBTTagInt(2), CloneNbt.emptyList(0), CloneNbt.emptyList(10), CloneNbt.emptyList(8)}) {
            Path work = root(); NBTTagCompound n = new NBTTagCompound();
            if (scripts != null) { n.setTag("Scripts", scripts); }
            for (int i = 0; i <= 12; i++) { n.setTag("empty_" + i, CloneNbt.emptyList(i)); }
            n.setDouble("negativeZero", -0.0); n.setFloat("payload", Float.intBitsToFloat(0x7fc01234));
            n.setDouble("payload2", Double.longBitsToDouble(0x7ff8000000001234L)); n.setString("extra", "\ud800");
            n.setInteger("RwRig", 8);
            Path dir = CloneWorkspace.unpack(work, "npc", n); equal(n, CloneWorkspace.read(work, "npc"));
            assertFalse(Files.exists(dir.resolve("script.txt"))); assertFalse(Files.exists(dir.resolve("animations.json")));
            Path packed = CloneWorkspace.pack(work, "npc", root(), 1, "typed special values", true);
            equal(n, ReadableCloneFiles.load(packed.toFile()));
        }
    }
    @Test public void fiveMergeCasesAndSettingsOnlyChangeExpectedPaths() throws Exception {
        for (int c = 0; c < 5; c++) {
            Path root = root(); NBTTagCompound original = sample("Java"), expected = original.copy();
            Path dir = CloneWorkspace.unpack(root, "npc", original);
            NBTTagCompound edited = original.copy(); edited.setString("Name", "Edited setting"); expected.setString("Name", "Edited setting");
            String old = original.getTagList("Scripts", 10).getCompoundTagAt(0).getString("Script");
            String a = c == 1 || c == 3 || c == 4 ? "edit A\r\n" : old;
            String b = c == 2 || c == 3 ? "edit A\r\n" : c == 4 ? "edit B\n" : old;
            edited.getTagList("Scripts", 10).getCompoundTagAt(0).setString("Script", a); inline(dir, edited);
            text(dir.resolve("script.java"), b);
            Map<String, byte[]> before = snapshot(dir);
            if (c == 4) { refused(() -> CloneWorkspace.read(root, "npc")); }
            else {
                expected.getTagList("Scripts", 10).getCompoundTagAt(0).setString("Script", c == 1 ? a : b);
                equal(expected, CloneWorkspace.read(root, "npc"));
            }
            unchanged(before, dir);
        }
    }
    @Test public void clipEditAndSecondTabKeepAllOtherTypedPaths() throws Exception {
        Path work = root(); NBTTagCompound n = sample("ECMAScript"), expected = n.copy();
        Path dir = CloneWorkspace.unpack(work, "npc", n);
        text(dir.resolve("animations.json"), "\ufeffnew invalid clips\r");
        text(dir.resolve("scripts/tab-0002.js"), "\nnew code\r\n");
        expected.getCompoundTag("RwRig").setString("Clips", "\ufeffnew invalid clips\r");
        expected.getTagList("Scripts", 10).getCompoundTagAt(1).setString("Script", "\nnew code\r\n");
        equal(expected, CloneWorkspace.read(work, "npc"));
    }
    @Test public void changedTabCountOrMappedTypeIsRefused() throws Exception {
        for (int choice = 0; choice < 4; choice++) {
            Path work = root(); NBTTagCompound n = sample("Java"); Path dir = CloneWorkspace.unpack(work, "npc", n);
            if (choice == 0) { n.getTagList("Scripts", 10).appendTag(new NBTTagCompound()); }
            if (choice == 1) { n.getTagList("Scripts", 10).getCompoundTagAt(0).removeTag("Script"); }
            if (choice == 2) { n.getTagList("Scripts", 10).getCompoundTagAt(0).setInteger("Script", 2); }
            if (choice == 3) { n.setString("Scripts", "wrong type"); }
            inline(dir, n); refused(() -> CloneWorkspace.read(work, "npc"));
        }
    }
    @Test public void invalidManifestFieldsVersionsTypesDuplicatesAndPathsRefused() throws Exception {
        for (int c = 0; c < 15; c++) {
            Path work = root(), dir = CloneWorkspace.unpack(work, "npc", sample("Java"));
            JsonObject doc = manifest(dir); JsonArray sources = doc.getAsJsonArray("sources"); JsonObject first = sources.get(0).getAsJsonObject();
            switch (c) {
                case 0: doc.addProperty("version", 2); break;
                case 1: doc.addProperty("unknown", 2); break;
                case 2: doc.addProperty("version", "1"); break;
                case 3: doc.addProperty("scriptTabCount", 5); break;
                case 4: sources.add(StrictCloneJson.read(first.toString())); break;
                case 5: first.addProperty("file", "../escape.java"); break;
                case 6: first.addProperty("file", "C:/escape.java"); break;
                case 7: first.addProperty("file", "script.java:ads"); break;
                case 8: first.addProperty("file", "SCRIPT.java"); break;
                case 9: first.addProperty("kind", "unknown"); break;
                case 10: first.addProperty("tab", 100); break;
                case 11: first.addProperty("baselineSha256", "bad"); break;
                case 12: first.addProperty("unknown", true); break;
                case 13: doc.remove("sources"); break;
                case 14: doc.addProperty("scriptTabCount", 6.5); break;
                default: throw new AssertionError();
            }
            manifest(dir, doc); refused(() -> CloneWorkspace.read(work, "npc"));
        }
        Path work = root(), dir = CloneWorkspace.unpack(work, "npc", sample("Java"));
        String duplicate = new String(Files.readAllBytes(dir.resolve(CloneWorkspace.MARKER)), StandardCharsets.UTF_8).replace("\"version\": 1", "\"version\": 1, \"version\": 1");
        text(dir.resolve(CloneWorkspace.MARKER), duplicate); refused(() -> CloneWorkspace.read(work, "npc"));
    }
    @Test public void missingFilesMalformedUtf8AndAmbiguousMarkerRefused() throws Exception {
        for (int c = 0; c < 5; c++) {
            Path work = root(), dir = CloneWorkspace.unpack(work, "npc", sample("Java"));
            if (c == 0) { Files.delete(dir.resolve("script.java")); }
            if (c == 1) { Files.write(dir.resolve("script.java"), new byte[]{(byte)0xc3, 0x28}); }
            if (c == 2) { Files.write(dir.resolve("clone.json"), new byte[]{(byte)0xc3, 0x28}); }
            if (c == 3) { Files.write(dir.resolve(CloneWorkspace.MARKER), new byte[]{(byte)0xc3, 0x28}); }
            if (c == 4) { text(dir.resolve("creature.json"), "{}"); }
            Map<String, byte[]> before = snapshot(dir); refused(() -> CloneWorkspace.read(work, "npc")); unchanged(before, dir);
        }
    }
    @Test public void boundsOnCloneManifestSourceAndWholeWorkspaceRefused() throws Exception {
        for (int c = 0; c < 4; c++) {
            Path work = root(), dir = CloneWorkspace.unpack(work, "npc", sample("Java"));
            Path file = dir.resolve(c == 0 ? "clone.json" : c == 1 ? CloneWorkspace.MARKER : c == 2 ? "script.java" : "extra.bin");
            try (RandomAccessFile out = new RandomAccessFile(file.toFile(), "rw")) {
                out.setLength(c == 1 ? 512 * 1024 + 1 : c == 3 ? 64 * 1024 * 1024 + 1 : ReadableClone.MAX_BYTES + 1);
            }
            refused(() -> CloneWorkspace.read(work, "npc"));
        }
    }
    @Test public void sourceAndExistingTargetsSurviveFailuresAndRace() throws Exception {
        NBTTagCompound input = sample("Java");
        input.getTagList("Scripts", 10).getCompoundTagAt(3).setString("Script", "representable in both output formats");
        Path work = root(), dir = CloneWorkspace.unpack(work, "npc", input), clones = root();
        Map<String, byte[]> before = snapshot(dir);
        refused(() -> CloneWorkspace.unpack(work, "npc", new NBTTagCompound())); unchanged(before, dir);
        refused(() -> CloneWorkspace.unpack(work, "fault", sample("Java"), (stage, target) -> { throw new IOException("Injected I/O failure"); }));
        assertFalse(Files.exists(work.resolve("fault")));
        refused(() -> CloneWorkspace.unpack(work, "race", sample("Java"), (stage, target) -> { Files.createDirectory(target); text(target.resolve("owner.txt"), "owner"); }));
        assertEquals("owner", new String(Files.readAllBytes(work.resolve("race/owner.txt")), StandardCharsets.UTF_8));
        for (boolean readable : new boolean[]{false, true}) {
            String name = "target " + readable;
            Path target = CloneWorkspace.pack(work, "npc", clones, 1, name, readable);
            byte[] bytes = Files.readAllBytes(target);
            refused(() -> CloneWorkspace.pack(work, "npc", clones, 1, name, readable)); assertArrayEquals(bytes, Files.readAllBytes(target));
            refused(() -> CloneWorkspace.pack(work, "npc", clones, 1, "fault " + readable, readable, (stage, dest) -> { throw new IOException("Injected writer failure"); }));
            assertFalse(Files.exists(clones.resolve("1/fault " + readable + ".json")));
            refused(() -> CloneWorkspace.pack(work, "npc", clones, 1, "race " + readable, readable, (stage, dest) -> text(dest, "owner")));
            assertEquals("owner", new String(Files.readAllBytes(clones.resolve("1/race " + readable + ".json")), StandardCharsets.UTF_8));
        }
        unchanged(before, dir);
        try (java.util.stream.Stream<Path> files = Files.walk(work)) { assertFalse(files.anyMatch(p -> p.getFileName().toString().startsWith(".clone-"))); }
        try (java.util.stream.Stream<Path> files = Files.walk(clones)) { assertFalse(files.anyMatch(p -> p.getFileName().toString().startsWith(".clone-"))); }
    }
    @Test public void invalidOutputNamesNeverEscapeRoots() throws Exception {
        Path work = root(), clones = root(); CloneWorkspace.unpack(work, "npc", sample("Java"));
        for (String id : new String[]{"../escape", "UPPER", "nul", "con", "x:y", ""}) { refused(() -> CloneWorkspace.unpack(work, id, sample("Java"))); }
        for (String name : new String[]{"../escape", "C:\\escape", "file:ads", "nul", "CON.txt", "a.", "a ", "", "a/b", "a\nb"}) {
            refused(() -> CloneWorkspace.pack(work, "npc", clones, 1, name, true));
            refused(() -> CloneWorkspace.loadClone(clones, 1, name));
        }
        for (int tab : new int[]{0, 10}) { refused(() -> CloneWorkspace.pack(work, "npc", clones, tab, "name", true)); }
    }
    @Test public void unrepresentableLegacyWriteFailsWithoutPublishingOrChangingSource() throws Exception {
        Path work = root(), clones = root(), dir = CloneWorkspace.unpack(work, "npc", sample("Java"));
        Map<String, byte[]> before = snapshot(dir);
        refused(() -> CloneWorkspace.pack(work, "npc", clones, 1, "legacy cannot represent surrogate", false));
        assertFalse(Files.exists(clones.resolve("1/legacy cannot represent surrogate.json")));
        unchanged(before, dir);
        Path output = CloneWorkspace.pack(work, "npc", clones, 1, "readable retains surrogate", true);
        equal(sample("Java"), ReadableCloneFiles.load(output.toFile()));
    }
    @Test public void conversionCostOneSampleAfterWarmup() throws Exception {
        Path work = root(); NBTTagCompound n = sample("Java");
        for (int i = 0; i < 2; i++) { CloneWorkspace.unpack(work, "warm_" + i, n); equal(n, CloneWorkspace.read(work, "warm_" + i)); }
        long start = System.nanoTime(); CloneWorkspace.unpack(work, "measured", n); long middle = System.nanoTime();
        NBTTagCompound result = CloneWorkspace.read(work, "measured"); long end = System.nanoTime(); equal(n, result);
        System.out.println("CLONEWORKSPACE cost data only unpackMs=" + (middle-start)/1e6 + " mergeMs=" + (end-middle)/1e6 + " repetitions=1");
    }
    @Test public void linksAndJunctionAncestorsNeverReadWriteOrEscape() throws Exception {
        Path work = root(), outside = root(), clones = root();
        Path dir = CloneWorkspace.unpack(work, "npc", sample("Java"));
        Path link = work.resolve("linked");
        if (System.getProperty("os.name").startsWith("Windows")) {
            Process process = new ProcessBuilder("cmd.exe", "/c", "mklink", "/J", link.toString(), outside.toString()).redirectErrorStream(true).start();
            try (InputStream in = process.getInputStream()) { while (in.read() != -1) { /* drain */ } }
            assertEquals("Junction fixture must actually be created", 0, process.waitFor());
        } else { Files.createSymbolicLink(link, outside); }
        try {
            Map<String, byte[]> before = snapshot(outside);
            refused(() -> CloneWorkspace.unpack(link, "escape", sample("Java")));
            refused(() -> CloneWorkspace.read(work, "linked"));
            refused(() -> CloneWorkspace.pack(work, "npc", link, 1, "escape", true));
            unchanged(before, outside);
        } finally { Files.delete(link); }
        // A source file link must be rejected as well (Unix; Windows junction exercises ancestors above).
        if (!System.getProperty("os.name").startsWith("Windows")) {
            Files.delete(dir.resolve("script.java"));
            Path file = outside.resolve("owner.java"); text(file, "owner");
            Files.createSymbolicLink(dir.resolve("script.java"), file);
            try { refused(() -> CloneWorkspace.read(work, "npc")); }
            finally { Files.delete(dir.resolve("script.java")); }
            assertEquals("owner", new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
            text(dir.resolve("script.java"), sample("Java").getTagList("Scripts", 10).getCompoundTagAt(0).getString("Script"));
        }
        refused(() -> CloneWorkspace.pack(work, "npc", dir.resolve("inside"), 1, "overlap", true));
        assertFalse(Files.exists(dir.resolve("inside")));
        assertTrue(Files.isDirectory(clones));
    }
}
