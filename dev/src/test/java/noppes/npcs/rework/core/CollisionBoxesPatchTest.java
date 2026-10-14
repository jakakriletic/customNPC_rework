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
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * M5.16: poseg v {@code World.getCollisionBoxes} se preveri na pravih bajtih vanilla razreda, kot
 * jih dobi transformer (v tem okolju z imeni MCP). Zahteva se natanko ena zamenjava: klic
 * {@code getEntitiesWithinAABBExcludingEntity} v tej metodi izgine, na njegovem mestu je
 * {@code INVOKESTATIC CollisionGate.entitiesForCollision}, drugi klici v metodi ostanejo.
 */
public class CollisionBoxesPatchTest {
    private static final String DESC =
            "(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;)Ljava/util/List;";

    @After
    public void pocisti() {
        System.clearProperty(CollisionBoxesPatch.PROP_PATCH);
    }

    private static byte[] razred(String ime) throws IOException {
        InputStream in = CollisionBoxesPatchTest.class.getResourceAsStream("/" + ime.replace('.', '/') + ".class");
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
            if (DESC.equals(m.desc) && ("getCollisionBoxes".equals(m.name) || "func_184144_a".equals(m.name))) {
                return m;
            }
        }
        return null;
    }

    private static int steviloKlicev(MethodNode m, String ime) {
        int n = 0;
        AbstractInsnNode[] insns = m.instructions.toArray();
        for (int i = 0; i < insns.length; ++i) {
            if (insns[i] instanceof MethodInsnNode && ime.equals(((MethodInsnNode) insns[i]).name)) {
                ++n;
            }
        }
        return n;
    }

    @Test
    public void posegZamenjaNatankoEnKlic() throws IOException {
        byte[] original = razred(CollisionBoxesPatch.TARGET_CLASS);
        MethodNode pred = metoda(original);
        assertNotNull("metode getCollisionBoxes ni v originalu", pred);
        int klicevPred = steviloKlicev(pred, "getEntitiesWithinAABBExcludingEntity")
                + steviloKlicev(pred, "func_72839_b");
        assertEquals("premisa posega: klic poizvedbe je v metodi natanko enkrat", 1, klicevPred);
        int vsehKlicevPred = 0;
        AbstractInsnNode[] insns = pred.instructions.toArray();
        for (int i = 0; i < insns.length; ++i) {
            if (insns[i] instanceof MethodInsnNode) {
                ++vsehKlicevPred;
            }
        }

        byte[] popravljen = CollisionBoxesPatch.apply(original);
        assertTrue("poseg je uspel", CollisionBoxesPatch.patched());
        assertFalse("bajti so se spremenili", java.util.Arrays.equals(original, popravljen));

        MethodNode po = metoda(popravljen);
        assertNotNull(po);
        assertEquals("klica poizvedbe ni vec", 0,
                steviloKlicev(po, "getEntitiesWithinAABBExcludingEntity") + steviloKlicev(po, "func_72839_b"));
        int vrata = 0;
        int vsehKlicevPo = 0;
        AbstractInsnNode[] poInsns = po.instructions.toArray();
        for (int i = 0; i < poInsns.length; ++i) {
            if (!(poInsns[i] instanceof MethodInsnNode)) {
                continue;
            }
            ++vsehKlicevPo;
            MethodInsnNode m = (MethodInsnNode) poInsns[i];
            if ("entitiesForCollision".equals(m.name)) {
                ++vrata;
                assertEquals("vrata so staticni klic", Opcodes.INVOKESTATIC, m.getOpcode());
                assertEquals("noppes/npcs/rework/core/CollisionGate", m.owner);
                assertEquals("(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;"
                        + "Lnet/minecraft/util/math/AxisAlignedBB;)Ljava/util/List;", m.desc);
            }
        }
        assertEquals("vrata so v metodi natanko enkrat", 1, vrata);
        assertEquals("drugih klicev poseg ni spremenil", vsehKlicevPred, vsehKlicevPo);
    }

    /** Drugi poseg na ze popravljen razred ne najde klica in vrne iste bajte. */
    @Test
    public void drugiPosegNeSpremeniNicesar() throws IOException {
        byte[] popravljen = CollisionBoxesPatch.apply(razred(CollisionBoxesPatch.TARGET_CLASS));
        assertTrue(CollisionBoxesPatch.patched());
        byte[] spet = CollisionBoxesPatch.apply(popravljen);
        assertArrayEquals(popravljen, spet);
        assertFalse("neuspel poseg je zapisan v lastnost", CollisionBoxesPatch.patched());
    }

    /** Razred brez ciljne metode: iste bajte in glasna napaka (poseg ni uspel). */
    @Test
    public void razredBrezCiljneMetodeOstaneNespremenjen() throws IOException {
        byte[] tuj = razred("noppes.npcs.rework.core.CollisionGate");
        byte[] izid = CollisionBoxesPatch.apply(tuj);
        assertArrayEquals(tuj, izid);
        assertFalse(CollisionBoxesPatch.patched());
    }

    /** Pokvarjeni bajti: izjema se ujame, vrnejo se isti bajti. */
    @Test
    public void pokvarjeniBajtiNePodrejoTransformerja() {
        byte[] smeti = { 1, 2, 3, 4 };
        assertArrayEquals(smeti, CollisionBoxesPatch.apply(smeti));
        assertFalse(CollisionBoxesPatch.patched());
    }
}
