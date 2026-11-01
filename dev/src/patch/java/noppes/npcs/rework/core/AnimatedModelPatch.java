package noppes.npcs.rework.core;

import java.io.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * P7b (D-098, Q274, Q282): client pose hook for the optional customnpcentities 0.7.0. Only with
 * {@code RwAnimHooks>=1} in this game directory's {@code config/CustomNpcs.cfg} the loaded
 * {@code AnimatedModel.buildPose} passes its result through
 * {@code noppes.npcs.rework.anim.AnimBlend.pose(pose, npc, this.partialTicks)} right before its single
 * {@code areturn}. The inserted code has no branches and leaves one Pose on the stack, so frames stay
 * valid; only max stack is recomputed. Both patterns (one areturn, float field {@code partialTicks}) are
 * checked first; a mismatch returns the original bytes and blending stays off (clips then just play).
 * The class is client-only, never loaded on a dedicated server; the entities jar is never changed.
 */
public final class AnimatedModelPatch {
    public static final String TARGET_CLASS = "dev.jakak.customnpcentities.anim.client.AnimatedModel";
    /** "1" patched, "0" pattern mismatch, "off" switch 0. */
    public static final String PROP_PATCH = "rwcore.blendpatch";
    static final String HOOK = "noppes/npcs/rework/anim/AnimBlend";
    static final String POSE = "dev/jakak/customnpcentities/anim/Pose";
    static final String BUILD_DESC = "(Ldev/jakak/customnpcentities/entity/EntityAnimatedNpc;FFFF)L" + POSE + ";";
    private static final Logger LOG = LogManager.getLogger("customnpcs-rework-core");

    private AnimatedModelPatch() { }

    /** Transformer entry: same switch and file as {@link AnimatedNpcPatch}. */
    public static byte[] apply(byte[] basicClass) {
        String home = System.getProperty(RwCoreMod.PROP_HOME, ".");
        if (AnimatedNpcPatch.configuredSwitch(new File(home)) < 1) {
            System.setProperty(PROP_PATCH, "off");
            return basicClass;
        }
        return instrument(basicClass);
    }

    /** @return the instrumented class or the very same array when any expected pattern is missing */
    public static byte[] instrument(byte[] basicClass) {
        try {
            ClassNode node = new ClassNode();
            new ClassReader(basicClass).accept(node, 0);
            MethodNode build = null;
            for (MethodNode m : node.methods) {
                if (!m.name.equals("buildPose") || !m.desc.equals(BUILD_DESC)) { continue; }
                if (build != null || (m.access & (Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT)) != 0) { return fail(basicClass, "buildPose ni enolicna metoda primerka"); }
                build = m;
            }
            if (build == null) { return fail(basicClass, "manjka buildPose" + BUILD_DESC); }
            boolean field = false;
            for (FieldNode f : node.fields) {
                if (f.name.equals("partialTicks") && f.desc.equals("F") && (f.access & Opcodes.ACC_STATIC) == 0) { field = true; }
            }
            if (!field) { return fail(basicClass, "manjka polje partialTicks"); }
            AbstractInsnNode ret = null;
            for (AbstractInsnNode n = build.instructions.getFirst(); n != null; n = n.getNext()) {
                if (n.getOpcode() == Opcodes.ARETURN) {
                    if (ret != null) { return fail(basicClass, "buildPose ima vec kot en areturn"); }
                    ret = n;
                }
                if (n.getOpcode() == Opcodes.ATHROW) { return fail(basicClass, "buildPose ima throw"); }
            }
            if (ret == null) { return fail(basicClass, "buildPose brez areturn"); }
            // All patterns found: now change.
            InsnList call = new InsnList();
            call.add(new VarInsnNode(Opcodes.ALOAD, 1));
            call.add(new VarInsnNode(Opcodes.ALOAD, 0));
            call.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, "partialTicks", "F"));
            call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "pose", "(Ljava/lang/Object;Ljava/lang/Object;F)Ljava/lang/Object;", false));
            call.add(new TypeInsnNode(Opcodes.CHECKCAST, POSE));
            build.instructions.insertBefore(ret, call);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            byte[] out = writer.toByteArray();
            System.setProperty(PROP_PATCH, "1");
            LOG.info(RwCoreMod.MARKER + " poseg v " + TARGET_CLASS + " uspel (P7b prehod poze blendTo)");
            return out;
        } catch (Throwable t) {
            return fail(basicClass, "izjema: " + t);
        }
    }

    private static byte[] fail(byte[] basicClass, String why) {
        System.setProperty(PROP_PATCH, "0");
        LOG.error(RwCoreMod.MARKER + " poseg v " + TARGET_CLASS + " NI uspel (" + why
                + "); razred ostane originalen, blendTo na tem klientu samo predvaja klip");
        return basicClass;
    }

    public static boolean patched() { return "1".equals(System.getProperty(PROP_PATCH)); }
}
