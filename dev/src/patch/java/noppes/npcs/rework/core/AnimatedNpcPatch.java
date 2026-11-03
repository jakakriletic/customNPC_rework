package noppes.npcs.rework.core;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * P7a (D-098, Q277-Q279): exact server action journal for the optional customnpcentities 0.7.0.
 * Only with {@code RwAnimHooks>=1} in {@code config/CustomNpcs.cfg} (read once at class load, as the
 * switch itself is read at startup) the loaded bytecode of {@code EntityAnimatedNpc} gets five static
 * calls into {@code noppes.npcs.rework.anim.AnimJournal}: head and every return of
 * {@code startAction(I)V}, head of {@code stopAction()V}, head of {@code onServerEvent(I,AnimEvent)V}
 * and right after the one {@code stopAction} in {@code tickAnimation()V} that is followed by
 * {@code onActionEnd} (the natural end). Each insertion leaves the operand stack as it was, so frames
 * stay valid; only max stack is recomputed. P7c (Q275): the one {@code areturn} of {@code animations()} also
 * goes through {@code noppes.npcs.rework.anim.AnimReload.library}, which selects a reloaded snapshot.
 * Every pattern is checked before anything changes; a
 * mismatch returns the original bytes (all or nothing) and the hooks stay off. The entities jar
 * itself is never changed. With 0 the class passes through untouched.
 */
public final class AnimatedNpcPatch {
    public static final String TARGET_CLASS = "dev.jakak.customnpcentities.entity.EntityAnimatedNpc";
    /** "1" patched, "0" pattern mismatch (hooks off), "off" switch 0. */
    public static final String PROP_PATCH = "rwcore.animpatch";
    static final String HOOKS = "noppes/npcs/rework/anim/AnimJournal";
    static final String EVENT_DESC = "(ILdev/jakak/customnpcentities/anim/AnimEvent;)V";
    static final String LIBRARY = "dev/jakak/customnpcentities/anim/AnimLibrary", RELOAD = "noppes/npcs/rework/anim/AnimReload";
    private static final String NPC_DESC = "(Ljava/lang/Object;)V";
    private static final Logger LOG = LogManager.getLogger("customnpcs-rework-core");

    private AnimatedNpcPatch() { }

    /** Switch value as written by the CustomNPCs config loader ({@code RwAnimHooks=n}); 0 when absent. */
    public static int configuredSwitch(File home) {
        File cfg = new File(new File(home, "config"), "CustomNpcs.cfg");
        if (!cfg.isFile()) { return 0; }
        try {
            for (String line : Files.readAllLines(cfg.toPath(), StandardCharsets.UTF_8)) {
                String t = line.trim();
                if (t.startsWith("RwAnimHooks=")) { return Integer.parseInt(t.substring("RwAnimHooks=".length()).trim()); }
            }
        } catch (Exception e) {
            LOG.error(RwCoreMod.MARKER + " RwAnimHooks ni berljiv v " + cfg + ": " + e);
        }
        return 0;
    }

    /** Transformer entry: reads the switch from the game directory. */
    public static byte[] apply(byte[] basicClass) {
        String home = System.getProperty(RwCoreMod.PROP_HOME, ".");
        if (configuredSwitch(new File(home)) < 1) {
            System.setProperty(PROP_PATCH, "off");
            return basicClass;
        }
        return instrument(basicClass);
    }

    /** @return the instrumented class or the very same array when any expected pattern is missing */
    public static byte[] instrument(byte[] basicClass) {
        try {
            ClassReader reader = new ClassReader(basicClass);
            ClassNode node = new ClassNode();
            reader.accept(node, 0);
            String owner = node.name;
            MethodNode start = method(node, "startAction", "(I)V");
            MethodNode stop = method(node, "stopAction", "()V");
            MethodNode event = method(node, "onServerEvent", EVENT_DESC);
            MethodNode tick = method(node, "tickAnimation", "()V");
            MethodNode animations = method(node, "animations", "()L" + LIBRARY + ";");
            if (start == null || stop == null || event == null || tick == null || animations == null) {
                return fail(basicClass, "manjka startAction/stopAction/onServerEvent/tickAnimation/animations");
            }
            AbstractInsnNode libraryReturn = null;
            for (AbstractInsnNode n = animations.instructions.getFirst(); n != null; n = n.getNext()) {
                if (n.getOpcode() == Opcodes.ARETURN) {
                    if (libraryReturn != null) { return fail(basicClass, "animations ima vec kot en areturn"); }
                    libraryReturn = n;
                }
            }
            if (libraryReturn == null) { return fail(basicClass, "animations brez areturn"); }
            List<AbstractInsnNode> returns = new ArrayList<>();
            for (AbstractInsnNode n = start.instructions.getFirst(); n != null; n = n.getNext()) {
                if (n.getOpcode() == Opcodes.RETURN) { returns.add(n); }
                if (n.getOpcode() == Opcodes.ATHROW) { return fail(basicClass, "startAction ima throw"); }
            }
            if (returns.isEmpty()) { return fail(basicClass, "startAction brez return"); }
            MethodInsnNode natural = null;
            for (AbstractInsnNode n = tick.instructions.getFirst(); n != null; n = n.getNext()) {
                if (!isCall(n, owner, "stopAction", "()V")) { continue; }
                AbstractInsnNode a = real(n.getNext()), b = a == null ? null : real(a.getNext()), c = b == null ? null : real(b.getNext());
                if (a != null && a.getOpcode() == Opcodes.ALOAD && ((VarInsnNode) a).var == 0
                        && b != null && b.getOpcode() == Opcodes.ILOAD && isCall(c, owner, "onActionEnd", "(I)V")) {
                    if (natural != null) { return fail(basicClass, "naravni stop se pojavi veckrat"); }
                    natural = (MethodInsnNode) n;
                }
            }
            if (natural == null) { return fail(basicClass, "naravnega stopAction+onActionEnd v tickAnimation ni"); }
            // All patterns found: now change.
            start.instructions.insert(call("beforeStart", false));
            for (AbstractInsnNode r : returns) { start.instructions.insertBefore(r, call("afterStart", false)); }
            stop.instructions.insert(call("beforeStop", false));
            event.instructions.insert(call("serverEvent", true));
            tick.instructions.insert(natural, call("naturalEnd", false));
            InsnList library = new InsnList();
            library.add(new VarInsnNode(Opcodes.ALOAD, 0));
            library.add(new MethodInsnNode(Opcodes.INVOKESTATIC, RELOAD, "library", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false));
            library.add(new TypeInsnNode(Opcodes.CHECKCAST, LIBRARY));
            animations.instructions.insertBefore(libraryReturn, library);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            byte[] out = writer.toByteArray();
            System.setProperty(PROP_PATCH, "1");
            LOG.info(RwCoreMod.MARKER + " poseg v " + TARGET_CLASS + " uspel (P7a dnevnik akcij: start, stop, dogodek, naravni konec)");
            return out;
        } catch (Throwable t) {
            return fail(basicClass, "izjema: " + t);
        }
    }

    private static InsnList call(String name, boolean event) {
        InsnList list = new InsnList();
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        if (event) {
            list.add(new VarInsnNode(Opcodes.ILOAD, 1));
            list.add(new VarInsnNode(Opcodes.ALOAD, 2));
            list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, name, "(Ljava/lang/Object;ILjava/lang/Object;)V", false));
        } else {
            list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, name, NPC_DESC, false));
        }
        return list;
    }

    private static MethodNode method(ClassNode node, String name, String desc) {
        MethodNode found = null;
        for (MethodNode m : node.methods) {
            if (m.name.equals(name) && m.desc.equals(desc)) {
                if (found != null || (m.access & (Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT)) != 0) { return null; }
                found = m;
            }
        }
        return found;
    }

    private static boolean isCall(AbstractInsnNode n, String owner, String name, String desc) {
        if (!(n instanceof MethodInsnNode)) { return false; }
        MethodInsnNode m = (MethodInsnNode) n;
        return (m.getOpcode() == Opcodes.INVOKEVIRTUAL || m.getOpcode() == Opcodes.INVOKESPECIAL)
                && m.owner.equals(owner) && m.name.equals(name) && m.desc.equals(desc);
    }

    /** Next instruction that executes (skips labels, line numbers and frames). */
    private static AbstractInsnNode real(AbstractInsnNode n) {
        while (n != null && n.getOpcode() < 0) { n = n.getNext(); }
        return n;
    }

    private static byte[] fail(byte[] basicClass, String why) {
        System.setProperty(PROP_PATCH, "0");
        LOG.error(RwCoreMod.MARKER + " poseg v " + TARGET_CLASS + " NI uspel (" + why
                + "); razred ostane originalen, animacijski hooki (RwAnimHooks) so izklopljeni");
        return basicClass;
    }

    /** Whether the journal hooks are in the loaded class (system property, valid across loaders). */
    public static boolean patched() {
        return "1".equals(System.getProperty(PROP_PATCH));
    }

}
