package noppes.npcs.rework.core;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;

/** P7a: AnimatedNpcPatch on the real pinned customnpcentities 0.7.0 class (read from the sibling
 * entities project's built jar; the jar itself is only read). */
public class AnimatedNpcPatchTest {
    static final String ENTRY = "dev/jakak/customnpcentities/entity/EntityAnimatedNpc.class";
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    @After public void clear() { System.clearProperty(AnimatedNpcPatch.PROP_PATCH); System.clearProperty(RwCoreMod.PROP_HOME); }

    static byte[] original() throws Exception {
        for (String path : new String[]{"../../customNPC_entities_mod/build/libs/customnpcentities-0.7.0.jar",
                "../customNPC_entities_mod/build/libs/customnpcentities-0.7.0.jar"}) {
            File jar = new File(path);
            if (!jar.isFile()) { continue; }
            try (ZipFile zip = new ZipFile(jar)) {
                ZipEntry e = zip.getEntry(ENTRY);
                assertNotNull("entity class in the pinned jar", e);
                try (InputStream in = zip.getInputStream(e)) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    for (int n; (n = in.read(buf)) > 0; ) { out.write(buf, 0, n); }
                    return out.toByteArray();
                }
            }
        }
        fail("pinned customnpcentities-0.7.0.jar not found next to the rework repository");
        return null;
    }

    static ClassNode node(byte[] bytes) { ClassNode n = new ClassNode(); new ClassReader(bytes).accept(n, 0); return n; }

    static List<String> calls(ClassNode n, String method) {
        List<String> out = new ArrayList<>();
        for (MethodNode m : n.methods) {
            if (!m.name.equals(method)) { continue; }
            for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
                if (i instanceof MethodInsnNode && (((MethodInsnNode) i).owner.equals(AnimatedNpcPatch.HOOKS) || ((MethodInsnNode) i).owner.equals(AnimatedNpcPatch.RELOAD))) { out.add(((MethodInsnNode) i).name); }
            }
        }
        return out;
    }

    /** Opcodes without the inserted hook calls and their argument loads. */
    static List<Integer> opcodes(MethodNode m, boolean strip) {
        List<Integer> out = new ArrayList<>();
        AbstractInsnNode[] all = m.instructions.toArray();
        for (int i = 0; i < all.length; i++) {
            if (all[i].getOpcode() < 0) { continue; }
            // P7c: aload_0, library call and checkcast before the areturn of animations().
            if (strip && all[i] instanceof MethodInsnNode && ((MethodInsnNode) all[i]).owner.equals(AnimatedNpcPatch.RELOAD)) {
                out.remove(out.size() - 1);
                i++;
                while (all[i].getOpcode() < 0) { i++; }
                continue;
            }
            out.add(all[i].getOpcode());
            if (strip && all[i] instanceof MethodInsnNode && ((MethodInsnNode) all[i]).owner.equals(AnimatedNpcPatch.HOOKS)) {
                int args = ((MethodInsnNode) all[i]).desc.startsWith("(Ljava/lang/Object;I") ? 4 : 2;
                for (int k = 0; k < args; k++) { out.remove(out.size() - 1); }
            }
        }
        return out;
    }

    @Test public void realClassGetsFiveHookSitesAndStaysVerifiable() throws Exception {
        byte[] before = original();
        byte[] after = AnimatedNpcPatch.instrument(before);
        assertNotSame(before, after);
        assertEquals("1", System.getProperty(AnimatedNpcPatch.PROP_PATCH));
        ClassNode n = node(after), o = node(before);
        assertEquals("[beforeStart, afterStart]", calls(n, "startAction").toString());
        assertEquals("[beforeStop]", calls(n, "stopAction").toString());
        assertEquals("[serverEvent]", calls(n, "onServerEvent").toString());
        assertEquals("[naturalEnd]", calls(n, "tickAnimation").toString());
        assertEquals("[library]", calls(n, "animations").toString());
        assertEquals(o.methods.size(), n.methods.size());
        for (int i = 0; i < n.methods.size(); i++) {
            MethodNode m = n.methods.get(i);
            assertEquals(m.name, opcodes(o.methods.get(i), false), opcodes(m, true));
            if ((m.access & Opcodes.ACC_ABSTRACT) == 0) { new Analyzer<BasicValue>(new BasicVerifier()).analyze(n.name, m); }
        }
        // The natural hook sits right after the stopAction that precedes onActionEnd.
        for (MethodNode m : n.methods) {
            if (!m.name.equals("tickAnimation")) { continue; }
            for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
                if (i instanceof MethodInsnNode && ((MethodInsnNode) i).name.equals("naturalEnd")) {
                    AbstractInsnNode p = i.getPrevious().getPrevious();
                    assertEquals("stopAction", ((MethodInsnNode) p).name);
                    AbstractInsnNode q = i.getNext();
                    for (int k = 0; k < 3; q = q.getNext()) { if (q.getOpcode() >= 0 && ++k == 3) { break; } }
                    assertEquals("onActionEnd", ((MethodInsnNode) q).name);
                }
            }
        }
    }

    @Test public void missingNaturalEndPatternReturnsTheSameBytes() throws Exception {
        ClassNode n = node(original());
        for (MethodNode m : n.methods) {
            if (!m.name.equals("tickAnimation")) { continue; }
            for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
                if (i instanceof MethodInsnNode && ((MethodInsnNode) i).name.equals("onActionEnd")) { ((MethodInsnNode) i).name = "onActionEnded"; }
            }
        }
        ClassWriter w = new ClassWriter(0);
        n.accept(w);
        byte[] changed = w.toByteArray();
        assertSame(changed, AnimatedNpcPatch.instrument(changed));
        assertEquals("0", System.getProperty(AnimatedNpcPatch.PROP_PATCH));
    }

    @Test public void missingAnimationsMethodReturnsTheSameBytes() throws Exception {
        ClassNode n = node(original());
        for (MethodNode m : n.methods) { if (m.name.equals("animations")) { m.name = "library"; } }
        ClassWriter w = new ClassWriter(0);
        n.accept(w);
        byte[] changed = w.toByteArray();
        assertSame(changed, AnimatedNpcPatch.instrument(changed));
        assertEquals("0", System.getProperty(AnimatedNpcPatch.PROP_PATCH));
    }

    @Test public void missingMethodReturnsTheSameBytes() throws Exception {
        ClassNode n = node(original());
        for (MethodNode m : n.methods) { if (m.name.equals("stopAction")) { m.name = "halt"; } }
        ClassWriter w = new ClassWriter(0);
        n.accept(w);
        byte[] changed = w.toByteArray();
        assertSame(changed, AnimatedNpcPatch.instrument(changed));
        assertEquals("0", System.getProperty(AnimatedNpcPatch.PROP_PATCH));
    }

    @Test public void switchZeroOrAbsentConfigLeavesTheClassUntouched() throws Exception {
        byte[] before = original();
        File home = tmp.newFolder("game");
        System.setProperty(RwCoreMod.PROP_HOME, home.getAbsolutePath());
        assertSame(before, AnimatedNpcPatch.apply(before));
        assertEquals("off", System.getProperty(AnimatedNpcPatch.PROP_PATCH));
        File cfg = new File(home, "config/CustomNpcs.cfg");
        assertTrue(cfg.getParentFile().mkdirs());
        Files.write(cfg.toPath(), "#x\nRwAnimHooks=0\n".getBytes(StandardCharsets.UTF_8));
        assertSame(before, AnimatedNpcPatch.apply(before));
        Files.write(cfg.toPath(), "#Rework M7.5 ...\nRwAnimHooks=2\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(2, AnimatedNpcPatch.configuredSwitch(home));
        assertNotSame(before, AnimatedNpcPatch.apply(before));
        assertEquals("1", System.getProperty(AnimatedNpcPatch.PROP_PATCH));
    }
}
