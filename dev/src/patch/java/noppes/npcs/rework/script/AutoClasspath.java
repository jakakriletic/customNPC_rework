package noppes.npcs.rework.script;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import noppes.npcs.rework.core.RwCoreMod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** P5 (Q263-Q265): RwJavaClasspath=auto builds the compiler classpath of this exact runtime.
 * Every file the game class loader reads (launch classpath, Forge, mods, contained libraries) is
 * on it. In production the game classes on disk are obfuscated (a, vg) and only get their runtime
 * SRG names inside LaunchClassLoader, so once per game/mod set the Minecraft classes are passed
 * through Forge's own transformers in loader order (binary patches, deobfuscation, access and side
 * transformers; never a mod coremod or mixin) and written to one jar in front of the classpath.
 * Forge's own classes in its universal jar also refer to obfuscated game names on disk (WorldEvent
 * getWorld returns amu) and get renamed by the same chain at load time, so they go into the jar too,
 * except the packages the loader never transforms (P5 attempt 2, Q268).
 * In a dev (MCP) runtime the classes on disk already have their runtime names; no jar is written.
 * Scripts compile against runtime names: MCP in dev, SRG members in production (same as before).
 */
public final class AutoClasspath {
    public static final String VALUE = "auto";
    /** Below the game directory; one jar per runtime key, reused after a restart. */
    static final String DIR = "customnpcs-java";
    /** Part of the key, so a jar of an earlier layout (only net/minecraft) is not reused. */
    static final String FORMAT = "2";
    /** classLoaderExceptions + transformerExceptions of LaunchClassLoader 1.12 and FML, used only
     * when the loader fields cannot be read. */
    static final List<String> DEFAULT_EXCEPTIONS = java.util.Arrays.asList("java.", "sun.", "org.lwjgl.",
            "org.apache.logging.", "net.minecraft.launchwrapper.", "javax.", "argo.", "org.objectweb.asm.",
            "com.google.common.", "org.bouncycastle.", "net.minecraftforge.fml.relauncher.",
            "net.minecraftforge.fml.repackage.", "net.minecraftforge.fml.common.asm.transformers.",
            "net.minecraftforge.fml.common.patcher.", "net.minecraftforge.fml.common.launcher.",
            "net.minecraftforge.classloading.");
    private static final Logger LOG = LogManager.getLogger("customnpcs-rework");
    private static String cached;

    private AutoClasspath() { }

    public static boolean isAuto(String value) {
        return value != null && VALUE.equalsIgnoreCase(value.trim());
    }

    /** Built once per JVM; a failure is reported to the caller and retried on the next compile. */
    public static synchronized String get() throws IOException {
        if (cached != null) { return cached; }
        if (!(AutoClasspath.class.getClassLoader() instanceof LaunchClassLoader)) {
            throw new IOException("RwJavaClasspath=auto needs the Forge game class loader");
        }
        long start = System.nanoTime();
        LaunchClassLoader loader = (LaunchClassLoader) AutoClasspath.class.getClassLoader();
        List<File> sources = sources(loader.getSources());
        File generated = null;
        int classes = 0;
        if (RwCoreMod.runtimeDeobf()) {
            List<IClassTransformer> chain = forgeChain(loader.getTransformers());
            File home = Launch.minecraftHome == null ? new File(".") : Launch.minecraftHome;
            generated = new File(new File(home, DIR), "minecraft-" + key(sources, chain) + ".jar");
            if (!generated.isFile()) { classes = write(loader, chain, generated); }
        }
        cached = join(generated, sources);
        LOG.info("RWJAVA auto classpath: " + sources.size() + " sources"
                + (generated == null ? ", dev names" : ", runtime jar " + generated.getName() + (classes > 0 ? " (" + classes + " classes written)" : " (reused)"))
                + ", " + (System.nanoTime() - start) / 1000000L + " ms");
        return cached;
    }

    /** Existing files and directories of the loader sources, in loader order, without duplicates. */
    static List<File> sources(Collection<URL> urls) {
        Set<File> out = new LinkedHashSet<>();
        for (URL url : urls) {
            if (url == null || !"file".equals(url.getProtocol())) { continue; }
            File file;
            try { file = new File(url.toURI()); } catch (Exception e) { file = new File(url.getPath()); }
            if (file.exists()) { out.add(file.getAbsoluteFile()); }
        }
        return new ArrayList<>(out);
    }

    /** Generated runtime jar first, so it wins over the obfuscated game jar's net/minecraft entries. */
    static String join(File generated, List<File> sources) {
        StringBuilder out = new StringBuilder();
        if (generated != null) { out.append(generated.getAbsolutePath()); }
        for (File file : sources) {
            if (out.length() > 0) { out.append(File.pathSeparatorChar); }
            out.append(file.getPath());
        }
        return out.toString();
    }

    /** Class name of a transformer; FML wraps coremod transformers (ASMTransformerWrapper) and names
     * the real one in the protected getParentClass. */
    static String transformerName(Object transformer) {
        for (Class<?> c = transformer.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Method parent = c.getDeclaredMethod("getParentClass");
                if (parent.getReturnType() != String.class) { break; }
                parent.setAccessible(true);
                Object name = parent.invoke(transformer);
                if (name instanceof String) { return (String) name; }
                break;
            } catch (NoSuchMethodException next) {
                // keep walking up
            } catch (ReflectiveOperationException | RuntimeException e) {
                break;
            }
        }
        return transformer.getClass().getName();
    }

    /** Forge's own transformers in loader order; mod coremods and mixins change classes per modpack
     * and may have side effects when run twice, so they are not replayed. */
    static List<IClassTransformer> forgeChain(List<IClassTransformer> transformers) {
        List<IClassTransformer> out = new ArrayList<>();
        for (IClassTransformer t : transformers) {
            if (transformerName(t).startsWith("net.minecraftforge.")) { out.add(t); }
        }
        return out;
    }

    /** Same game and mod files with the same transformer chain = same classes. */
    static String key(List<File> sources, List<IClassTransformer> chain) throws IOException {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(("format " + FORMAT + "\n").getBytes("UTF-8"));
            for (File file : sources) { sha.update((file.getPath() + "|" + file.length() + "|" + file.lastModified() + "\n").getBytes("UTF-8")); }
            for (IClassTransformer t : chain) { sha.update((transformerName(t) + "\n").getBytes("UTF-8")); }
            StringBuilder hex = new StringBuilder();
            byte[] digest = sha.digest();
            for (int i = 0; i < 8; i++) { hex.append(String.format("%02x", digest[i] & 0xff)); }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) { throw new IOException(e); }
    }

    /** Obfuscated game classes (mapping keys plus net/minecraft entries of the game jar) and the
     * transformed net/minecraftforge classes of the Forge jar through the chain, renamed like LaunchClassLoader does; written to a temporary file and moved in place. */
    private static int write(LaunchClassLoader loader, List<IClassTransformer> chain, File target) throws IOException {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        Set<String> names = new TreeSet<>(remapper.getObfedClasses());
        names.addAll(jarClasses(loader.findResource(remapper.unmap("net/minecraft/world/World") + ".class"), "net/minecraft/"));
        List<String> exceptions = exceptions(loader);
        for (String forge : jarClasses(loader.findResource("net/minecraftforge/common/MinecraftForge.class"), "net/minecraftforge/")) {
            if (!excluded(forge, exceptions)) { names.add(forge); }
        }
        Map<String, byte[]> out = new TreeMap<>();
        int skipped = 0;
        for (String obf : names) {
            String name = obf.replace('/', '.');
            // findResource, not getClassBytes: the latter keeps every class in the loader's resource cache.
            byte[] bytes = read(loader.findResource(obf + ".class"));
            if (bytes == null) { continue; } // the other side's classes are in the joined mapping
            String mapped = remapper.map(obf).replace('/', '.');
            try {
                for (IClassTransformer t : chain) {
                    bytes = t.transform(name, mapped, bytes);
                    if (bytes == null) { break; }
                }
            } catch (RuntimeException | LinkageError e) {
                // SideTransformer rejects a class of the other side; the runtime cannot load it either.
                // A Forge class whose supertypes do not link on this side cannot load either.
                skipped++;
                continue;
            }
            if (bytes != null) { out.put(mapped.replace('.', '/') + ".class", bytes); }
        }
        if (skipped > 0) { LOG.info("RWJAVA auto classpath: " + skipped + " classes rejected by the Forge chain, not on the classpath"); }
        return writeJar(out, target);
    }

    /** Class names (internal form) under the prefix in the jar that holds the resource. */
    static Set<String> jarClasses(URL resource, String prefix) throws IOException {
        Set<String> names = new TreeSet<>();
        if (resource == null || !"jar".equals(resource.getProtocol())) { return names; }
        String path = resource.getPath();
        try (JarFile jar = new JarFile(new File(new URL(path.substring(0, path.indexOf("!/"))).toURI()))) {
            for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements(); ) {
                String entry = e.nextElement().getName();
                if (entry.startsWith(prefix) && entry.endsWith(".class")) { names.add(entry.substring(0, entry.length() - 6)); }
            }
        } catch (Exception e) { throw new IOException("jar " + path, e); }
        return names;
    }

    /** The loader's classLoaderExceptions and transformerExceptions: such classes load untransformed,
     * so their bytes on disk are already the runtime ones. */
    @SuppressWarnings("unchecked")
    static List<String> exceptions(LaunchClassLoader loader) {
        List<String> out = new ArrayList<>();
        try {
            for (String field : new String[] {"classLoaderExceptions", "transformerExceptions"}) {
                java.lang.reflect.Field f = LaunchClassLoader.class.getDeclaredField(field);
                f.setAccessible(true);
                out.addAll((Set<String>) f.get(loader));
            }
            return out;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return DEFAULT_EXCEPTIONS;
        }
    }

    static boolean excluded(String internalName, List<String> exceptions) {
        String name = internalName.replace('/', '.');
        for (String prefix : exceptions) {
            if (name.startsWith(prefix)) { return true; }
        }
        return false;
    }

    private static byte[] read(URL url) {
        if (url == null) { return null; }
        try (java.io.InputStream in = url.openStream()) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (int n; (n = in.read(buffer)) > 0; ) { out.write(buffer, 0, n); }
            return out.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    static int writeJar(Map<String, byte[]> entries, File target) throws IOException {
        File dir = target.getAbsoluteFile().getParentFile();
        Files.createDirectories(dir.toPath());
        Path tmp = Files.createTempFile(dir.toPath(), target.getName(), ".tmp");
        try {
            try (OutputStream file = Files.newOutputStream(tmp); JarOutputStream jar = new JarOutputStream(file)) {
                for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                    JarEntry e = new JarEntry(entry.getKey());
                    e.setTime(0L);
                    jar.putNextEntry(e);
                    jar.write(entry.getValue());
                    jar.closeEntry();
                }
            }
            Files.move(tmp, target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(tmp);
        }
        return entries.size();
    }
}
