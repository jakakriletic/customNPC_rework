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
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;

/** P7b: AnimatedModelPatch on the real pinned customnpcentities 0.7.0 client class (jar only read). */
public class AnimatedModelPatchTest {
    static final String ENTRY = "dev/jakak/customnpcentities/anim/client/AnimatedModel.class";
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    @After public void clear() { System.clearProperty(AnimatedModelPatch.PROP_PATCH); System.clearProperty(RwCoreMod.PROP_HOME); }

    static byte[] original() throws Exception {
        for (String path : new String[]{"../../customNPC_entities_mod/build/libs/customnpcentities-0.7.0.jar",
                "../customNPC_entities_mod/build/libs/customnpcentities-0.7.0.jar"}) {
            File jar = new File(path);
            if (!jar.isFile()) { continue; }
            try (ZipFile zip = new ZipFile(jar)) {
                ZipEntry e = zip.getEntry(ENTRY);
                assertNotNull("model class in the pinned jar", e);
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

    static List<Integer> opcodes(MethodNode m) {
        List<Integer> out = new ArrayList<>();
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) { if (i.getOpcode() >= 0) { out.add(i.getOpcode()); } }
        return out;
    }

    static byte[] write(ClassNode n) { ClassWriter w = new ClassWriter(0); n.accept(w); return w.toByteArray(); }

    @Test public void realClassGetsOnePoseCallBeforeItsReturnAndStaysVerifiable() throws Exception {
        byte[] before = original();
        byte[] after = AnimatedModelPatch.instrument(before);
        assertNotSame(before, after);
        assertEquals("1", System.getProperty(AnimatedModelPatch.PROP_PATCH));
        ClassNode n = node(after), o = node(before);
        assertEquals(o.methods.size(), n.methods.size());
        for (int i = 0; i < n.methods.size(); i++) {
            MethodNode m = n.methods.get(i), old = o.methods.get(i);
            List<Integer> ops = opcodes(m);
            if (m.name.equals("buildPose")) {
                List<Integer> expected = opcodes(old);
                // aload 1, aload 0, getfield partialTicks, invokestatic AnimBlend.pose, checkcast Pose, areturn
                expected.addAll(expected.size() - 1, java.util.Arrays.asList(Opcodes.ALOAD, Opcodes.ALOAD, Opcodes.GETFIELD, Opcodes.INVOKESTATIC, Opcodes.CHECKCAST));
                assertEquals(expected, ops);
                AbstractInsnNode last = m.instructions.getLast();
                while (last.getOpcode() < 0) { last = last.getPrevious(); }
                assertEquals(Opcodes.ARETURN, last.getOpcode());
                MethodInsnNode call = (MethodInsnNode) last.getPrevious().getPrevious();
                assertEquals(AnimatedModelPatch.HOOK + ".pose(Ljava/lang/Object;Ljava/lang/Object;F)Ljava/lang/Object;", call.owner + "." + call.name + call.desc);
            } else {
                assertEquals(m.name, opcodes(old), ops);
            }
            if ((m.access & Opcodes.ACC_ABSTRACT) == 0) { new Analyzer<BasicValue>(new BasicVerifier()).analyze(n.name, m); }
        }
    }

    @Test public void missingFieldOrSecondReturnReturnsTheSameBytes() throws Exception {
        ClassNode n = node(original());
        for (FieldNode f : n.fields) { if (f.name.equals("partialTicks")) { f.name = "partial"; } }
        byte[] changed = write(n);
        assertSame(changed, AnimatedModelPatch.instrument(changed));
        assertEquals("0", System.getProperty(AnimatedModelPatch.PROP_PATCH));

        ClassNode r = node(original());
        for (MethodNode m : r.methods) {
            if (!m.name.equals("buildPose")) { continue; }
            m.instructions.insert(new InsnNode(Opcodes.ARETURN));
            m.instructions.insert(new InsnNode(Opcodes.ACONST_NULL));
        }
        byte[] twice = write(r);
        assertSame(twice, AnimatedModelPatch.instrument(twice));
        assertEquals("0", System.getProperty(AnimatedModelPatch.PROP_PATCH));
    }

    @Test public void missingMethodReturnsTheSameBytes() throws Exception {
        ClassNode n = node(original());
        for (MethodNode m : n.methods) { if (m.name.equals("buildPose")) { m.desc = m.desc.replace("FFFF", "FFF"); } }
        byte[] changed = write(n);
        assertSame(changed, AnimatedModelPatch.instrument(changed));
        assertEquals("0", System.getProperty(AnimatedModelPatch.PROP_PATCH));
    }

    @Test public void switchZeroOrAbsentConfigLeavesTheClassUntouched() throws Exception {
        byte[] before = original();
        File home = tmp.newFolder("game");
        System.setProperty(RwCoreMod.PROP_HOME, home.getAbsolutePath());
        assertSame(before, AnimatedModelPatch.apply(before));
        assertEquals("off", System.getProperty(AnimatedModelPatch.PROP_PATCH));
        File cfg = new File(home, "config/CustomNpcs.cfg");
        assertTrue(cfg.getParentFile().mkdirs());
        Files.write(cfg.toPath(), "RwAnimHooks=0\n".getBytes(StandardCharsets.UTF_8));
        assertSame(before, AnimatedModelPatch.apply(before));
        Files.write(cfg.toPath(), "RwAnimHooks=1\n".getBytes(StandardCharsets.UTF_8));
        assertNotSame(before, AnimatedModelPatch.apply(before));
        assertEquals("1", System.getProperty(AnimatedModelPatch.PROP_PATCH));
    }
}
