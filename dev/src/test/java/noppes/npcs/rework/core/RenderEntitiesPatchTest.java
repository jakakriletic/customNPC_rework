package noppes.npcs.rework.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * M5-V.5: poseg v {@code RenderGlobal.renderEntities} na pravih bajtih vanilla razreda (z imeni MCP). Zahteva
 * se natanko en vstavek - {@code ALOAD 1, ALOAD 2, FLOAD 3, INVOKESTATIC FarRender.renderEntities} - tik pred
 * konstanto {@code "blockentities"}; vse ostale instrukcije ostanejo. Brez sidra ali
 * z dvema sidroma poseg vrne iste bajte.
 */
public class RenderEntitiesPatchTest {
    @After
    public void pocisti() {
        System.clearProperty(RenderEntitiesPatch.PROP_PATCH);
    }

    private static byte[] razred(String ime) throws IOException {
        InputStream in = RenderEntitiesPatchTest.class.getResourceAsStream("/" + ime.replace('.', '/') + ".class");
        assertNotNull("razreda " + ime + " ni na classpathu", in);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    private static MethodNode metoda(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        for (MethodNode m : node.methods) {
            if (RenderEntitiesPatch.TARGET_DESC.equals(m.desc) && "renderEntities".equals(m.name)) {
                return m;
            }
        }
        return null;
    }

    private static int sidra(MethodNode m) {
        int n = 0;
        for (AbstractInsnNode i : m.instructions.toArray()) {
            if (i instanceof LdcInsnNode && RenderEntitiesPatch.ANCHOR.equals(((LdcInsnNode) i).cst)) {
                ++n;
            }
        }
        return n;
    }

    @Test
    public void vstaviNatankoEnKlicPredSidrom() throws IOException {
        byte[] original = razred(RenderEntitiesPatch.TARGET_CLASS);
        MethodNode pred = metoda(original);
        assertNotNull("metode renderEntities ni v originalu", pred);
        assertEquals("sidro mora biti v originalu natanko enkrat", 1, sidra(pred));

        byte[] popravljen = RenderEntitiesPatch.apply(original);
        assertTrue("poseg mora uspeti", RenderEntitiesPatch.patched());
        MethodNode po = metoda(popravljen);
        assertEquals(pred.instructions.size() + 4, po.instructions.size());
        AbstractInsnNode[] ins = po.instructions.toArray();
        int klicev = 0;
        for (int i = 0; i < ins.length; ++i) {
            if (ins[i] instanceof MethodInsnNode && RenderEntitiesPatch.HOOK_OWNER.equals(((MethodInsnNode) ins[i]).owner)) {
                ++klicev;
                MethodInsnNode m = (MethodInsnNode) ins[i];
                assertEquals(Opcodes.INVOKESTATIC, m.getOpcode());
                assertEquals("renderEntities", m.name);
                assertEquals(RenderEntitiesPatch.TARGET_DESC, m.desc);
                assertTrue(ins[i + 1] instanceof LdcInsnNode);
                assertEquals(RenderEntitiesPatch.ANCHOR, ((LdcInsnNode) ins[i + 1]).cst);
                assertArrayEquals(new int[] {Opcodes.ALOAD, Opcodes.ALOAD, Opcodes.FLOAD},
                        new int[] {ins[i - 3].getOpcode(), ins[i - 2].getOpcode(), ins[i - 1].getOpcode()});
                assertArrayEquals(new int[] {1, 2, 3}, new int[] {((VarInsnNode) ins[i - 3]).var,
                        ((VarInsnNode) ins[i - 2]).var, ((VarInsnNode) ins[i - 1]).var});
            }
        }
        assertEquals(1, klicev);
        // Vstavek je nevtralen za sklad; najvecja globina se lahko samo poveca.
        assertTrue(po.maxStack >= pred.maxStack);
        assertEquals(pred.maxLocals, po.maxLocals);
    }

    private static byte[] umetniRazred(int sider) {
        ClassNode node = new ClassNode();
        node.version = Opcodes.V1_8;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = "rwtest/RenderGlobal";
        node.superName = "java/lang/Object";
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, "renderEntities", RenderEntitiesPatch.TARGET_DESC, null, null);
        for (int i = 0; i < sider; ++i) {
            m.instructions.add(new LdcInsnNode(RenderEntitiesPatch.ANCHOR));
            m.instructions.add(new InsnNode(Opcodes.POP));
        }
        m.instructions.add(new InsnNode(Opcodes.RETURN));
        m.maxStack = 1;
        m.maxLocals = 4;
        node.methods.add(m);
        ClassWriter w = new ClassWriter(0);
        node.accept(w);
        return w.toByteArray();
    }

    @Test
    public void brezSidraVrneIsteBajte() {
        byte[] b = umetniRazred(0);
        assertArrayEquals(b, RenderEntitiesPatch.apply(b));
        assertFalse(RenderEntitiesPatch.patched());
        assertEquals("0", System.getProperty(RenderEntitiesPatch.PROP_PATCH));
    }

    @Test
    public void dveSidriVrneIsteBajte() {
        byte[] b = umetniRazred(2);
        assertArrayEquals(b, RenderEntitiesPatch.apply(b));
        assertFalse(RenderEntitiesPatch.patched());
    }

    @Test
    public void enoSidroUmetnegaRazreda() {
        byte[] b = umetniRazred(1);
        byte[] p = RenderEntitiesPatch.apply(b);
        assertTrue(RenderEntitiesPatch.patched());
        MethodNode m = metoda(p);
        assertEquals(1, sidra(m));
        assertTrue("COMPUTE_MAXS: trije argumenti na skladu", m.maxStack >= 3);
    }
}
