package noppes.npcs.rework.script;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarFile;
import net.minecraft.launchwrapper.IClassTransformer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AutoClasspathTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    @Test public void autoValueIsCaseAndSpaceInsensitive() {
        assertTrue(AutoClasspath.isAuto("auto"));
        assertTrue(AutoClasspath.isAuto(" AUTO "));
        assertFalse(AutoClasspath.isAuto(""));
        assertFalse(AutoClasspath.isAuto(null));
        assertFalse(AutoClasspath.isAuto("C:/auto/classpath.txt"));
    }

    @Test public void sourcesKeepLoaderOrderAndDropMissingAndDuplicates() throws Exception {
        File a = tmp.newFile("a.jar"), b = tmp.newFolder("classes"), gone = new File(tmp.getRoot(), "gone.jar");
        List<URL> urls = Arrays.asList(b.toURI().toURL(), a.toURI().toURL(), gone.toURI().toURL(), b.toURI().toURL(),
                new URL("http://example.invalid/x.jar"), null);
        assertEquals(Arrays.asList(b.getAbsoluteFile(), a.getAbsoluteFile()), AutoClasspath.sources(urls));
    }

    @Test public void generatedJarComesFirst() throws Exception {
        File a = tmp.newFile("a.jar"), g = new File(tmp.getRoot(), "minecraft-x.jar");
        String sep = File.pathSeparator;
        assertEquals(g.getAbsolutePath() + sep + a.getPath(), AutoClasspath.join(g, Collections.singletonList(a)));
        assertEquals(a.getPath(), AutoClasspath.join(null, Collections.singletonList(a)));
    }

    static class Plain implements IClassTransformer {
        public byte[] transform(String name, String transformedName, byte[] basicClass) { return basicClass; }
    }

    /** Same shape as FML's ASMTransformerWrapper.TransformerWrapper: protected getParentClass. */
    abstract static class Wrapper implements IClassTransformer {
        public byte[] transform(String name, String transformedName, byte[] basicClass) { return basicClass; }
        protected abstract String getParentClass();
    }

    static IClassTransformer wrapped(String parent) {
        return new Wrapper() { @Override protected String getParentClass() { return parent; } };
    }

    @Test public void forgeChainKeepsOnlyForgeTransformersInOrder() {
        IClassTransformer patch = wrapped("net.minecraftforge.fml.common.asm.transformers.PatchingTransformer");
        IClassTransformer mod = wrapped("noppes.npcs.rework.core.RwTransformer");
        IClassTransformer plain = new Plain();
        IClassTransformer side = wrapped("net.minecraftforge.fml.common.asm.transformers.SideTransformer");
        List<IClassTransformer> chain = AutoClasspath.forgeChain(Arrays.asList(patch, mod, plain, side));
        assertEquals(Arrays.asList(patch, side), chain);
        assertEquals(Plain.class.getName(), AutoClasspath.transformerName(plain));
        assertEquals("noppes.npcs.rework.core.RwTransformer", AutoClasspath.transformerName(mod));
    }

    @Test public void keyDependsOnSourcesAndChain() throws Exception {
        File a = tmp.newFile("a.jar");
        List<File> one = Collections.singletonList(a);
        String k = AutoClasspath.key(one, new ArrayList<>());
        assertEquals(16, k.length());
        assertEquals(k, AutoClasspath.key(one, new ArrayList<>()));
        assertNotEquals(k, AutoClasspath.key(one, Collections.singletonList(wrapped("net.minecraftforge.X"))));
        Files.write(a.toPath(), new byte[] {1, 2, 3});
        assertNotEquals(k, AutoClasspath.key(one, new ArrayList<>()));
    }

    /** P5 attempt 2 (Q268): Forge classes of the jar holding MinecraftForge go through the chain,
     * apart from the loader's untransformed packages. */
    @Test public void forgeJarClassesAreListedByPrefixWithoutLoaderExceptions() throws Exception {
        File forge = new File(tmp.getRoot(), "forge-universal.jar");
        Map<String, byte[]> entries = new TreeMap<>();
        entries.put("net/minecraftforge/common/MinecraftForge.class", new byte[] {1});
        entries.put("net/minecraftforge/event/world/WorldEvent$Save.class", new byte[] {2});
        entries.put("net/minecraftforge/fml/relauncher/Side.class", new byte[] {3});
        entries.put("net/minecraft/world/World.class", new byte[] {4});
        entries.put("net/minecraftforge/notes.txt", new byte[] {5});
        AutoClasspath.writeJar(entries, forge);
        URL resource = new URL("jar:" + forge.toURI().toURL() + "!/net/minecraftforge/common/MinecraftForge.class");
        Set<String> names = AutoClasspath.jarClasses(resource, "net/minecraftforge/");
        assertEquals(new TreeSet<>(Arrays.asList("net/minecraftforge/common/MinecraftForge",
                "net/minecraftforge/event/world/WorldEvent$Save", "net/minecraftforge/fml/relauncher/Side")), names);
        assertTrue(AutoClasspath.jarClasses(null, "net/").isEmpty());
        assertTrue(AutoClasspath.jarClasses(tmp.getRoot().toURI().toURL(), "net/").isEmpty());
        assertTrue(AutoClasspath.excluded("net/minecraftforge/fml/relauncher/Side", AutoClasspath.DEFAULT_EXCEPTIONS));
        assertTrue(AutoClasspath.excluded("net/minecraftforge/fml/common/asm/transformers/SideTransformer", AutoClasspath.DEFAULT_EXCEPTIONS));
        assertFalse(AutoClasspath.excluded("net/minecraftforge/event/world/WorldEvent$Save", AutoClasspath.DEFAULT_EXCEPTIONS));
        assertFalse(AutoClasspath.excluded("net/minecraftforge/common/MinecraftForge", Collections.emptyList()));
    }

    @Test public void jarIsWrittenAtomicallyWithSortedEntries() throws Exception {
        File target = new File(new File(tmp.getRoot(), "customnpcs-java"), "minecraft-k.jar");
        Map<String, byte[]> entries = new TreeMap<>();
        entries.put("net/minecraft/world/World.class", new byte[] {1});
        entries.put("net/minecraft/entity/Entity.class", new byte[] {2, 3});
        assertEquals(2, AutoClasspath.writeJar(entries, target));
        try (JarFile jar = new JarFile(target)) {
            assertEquals(2, jar.getEntry("net/minecraft/entity/Entity.class").getSize());
            assertNotNull(jar.getEntry("net/minecraft/world/World.class"));
        }
        assertEquals(1, target.getParentFile().listFiles().length);
    }
}
