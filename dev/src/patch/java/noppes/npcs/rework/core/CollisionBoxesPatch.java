package noppes.npcs.rework.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * M5.16 (D-031): edini poseg v vanilla bajtkodo. V
 * {@code World.getCollisionBoxes(Entity, AxisAlignedBB)} zamenja klic
 * {@code world.getEntitiesWithinAABBExcludingEntity(entity, box)} s klicem
 * {@link CollisionGate#entitiesForCollision}.
 *
 * <p>Zamenjava {@code INVOKEVIRTUAL} s {@code INVOKESTATIC} je najmanjsi mozen poseg: sklad je pred
 * klicem enak ({@code world}, {@code entity}, {@code box}), prejemnik postane prvi argument,
 * vrnjena vrednost je ista ({@code java.util.List}). Nic drugega v metodi se ne spremeni, zato
 * {@code ClassWriter(0)} zadosca (okvirji in velikost sklada ostanejo veljavni).
 *
 * <p>Imena so dvojna: v dev okolju MCP, v produkciji SRG (transformer tece za Forgeovim
 * deobfuskacijskim transformerjem, {@code SortingIndex} 1001). Ce metode ali klica ne najde, vrne
 * **nespremenjene** bajte in to glasno zapise; takrat {@link CollisionGate} nikoli ni klican in
 * obnasanje ostane originalno. Izid je v sistemski lastnosti {@code rwcore.patch}, da ga vidi
 * {@code /rwmove}.
 */
public final class CollisionBoxesPatch {
    public static final String TARGET_CLASS = "net.minecraft.world.World";
    public static final String PROP_PATCH = "rwcore.patch";

    /** {@code (Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;)Ljava/util/List;} */
    private static final String TARGET_DESC =
            "(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;)Ljava/util/List;";
    private static final String[] TARGET_METHOD = { "getCollisionBoxes", "func_184144_a" };
    private static final String[] QUERY_METHOD = { "getEntitiesWithinAABBExcludingEntity", "func_72839_b" };

    private static final String GATE_OWNER = "noppes/npcs/rework/core/CollisionGate";
    private static final String GATE_NAME = "entitiesForCollision";
    private static final String GATE_DESC = "(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;"
            + "Lnet/minecraft/util/math/AxisAlignedBB;)Ljava/util/List;";

    private static final Logger LOG = LogManager.getLogger("customnpcs-rework-core");

    private CollisionBoxesPatch() {
    }

    private static boolean nameIs(String actual, String[] candidates) {
        for (int i = 0; i < candidates.length; ++i) {
            if (candidates[i].equals(actual)) {
                return true;
            }
        }
        return false;
    }

    /** @return popravljeni razred ali iste bajte, ce poseg ni uspel */
    public static byte[] apply(byte[] basicClass) {
        try {
            ClassReader reader = new ClassReader(basicClass);
            ClassNode node = new ClassNode();
            reader.accept(node, 0);
            MethodNode target = null;
            for (int i = 0; i < node.methods.size(); ++i) {
                MethodNode m = node.methods.get(i);
                if (TARGET_DESC.equals(m.desc) && nameIs(m.name, TARGET_METHOD)) {
                    target = m;
                    break;
                }
            }
            if (target == null) {
                return fail(basicClass, "metode getCollisionBoxes" + TARGET_DESC + " ni");
            }
            MethodInsnNode call = null;
            AbstractInsnNode[] insns = target.instructions.toArray();
            for (int i = 0; i < insns.length; ++i) {
                if (insns[i].getOpcode() != Opcodes.INVOKEVIRTUAL) {
                    continue;
                }
                MethodInsnNode m = (MethodInsnNode) insns[i];
                if (nameIs(m.name, QUERY_METHOD)) {
                    if (call != null) {
                        return fail(basicClass, "klic poizvedbe entitet se v metodi pojavi veckrat");
                    }
                    call = m;
                }
            }
            if (call == null) {
                return fail(basicClass, "klica poizvedbe entitet v metodi ni");
            }
            target.instructions.set(call,
                    new MethodInsnNode(Opcodes.INVOKESTATIC, GATE_OWNER, GATE_NAME, GATE_DESC, false));
            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            System.setProperty(PROP_PATCH, "1");
            LOG.info(RwCoreMod.MARKER + " poseg v " + TARGET_CLASS + "." + target.name
                    + " uspel (poizvedba entitet za trke gre skozi CollisionGate)");
            return writer.toByteArray();
        } catch (Throwable t) {
            return fail(basicClass, "izjema: " + t);
        }
    }

    private static byte[] fail(byte[] basicClass, String why) {
        System.setProperty(PROP_PATCH, "0");
        LOG.error(RwCoreMod.MARKER + " poseg v " + TARGET_CLASS + " NI uspel (" + why
                + "); obnasanje ostane originalno, stikalo RwMoveBoxes ne bo imelo ucinka");
        return basicClass;
    }

    /** Ali je poseg uspel (bere sistemsko lastnost, zato velja med nalagalniki razredov). */
    public static boolean patched() {
        return "1".equals(System.getProperty(PROP_PATCH));
    }
}
