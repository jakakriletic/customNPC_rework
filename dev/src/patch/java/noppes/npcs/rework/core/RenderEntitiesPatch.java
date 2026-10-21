package noppes.npcs.rework.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * M5-V.5 (D-031, D-047): poseg v klientski {@code net.minecraft.client.renderer.RenderGlobal}. V
 * {@code renderEntities(Entity, ICamera, float)} tik pred konstanto {@code "blockentities"}
 * ({@code profiler.endStartSection("blockentities")}, konec risanja entitet) vstavi klic
 * {@code FarRender.renderEntities(renderViewEntity, camera, partialTicks)}.
 *
 * <p>Vstavek je nevtralen za sklad: nalozi tri argumente metode in poklice staticno metodo brez vrnjene
 * vrednosti, zato okvirji ostanejo veljavni; {@code ClassWriter.COMPUTE_MAXS} samo na novo izracuna najvecjo
 * globino sklada (ne nalaga razredov, kar {@code COMPUTE_FRAMES} bi). Konstanta mora biti v metodi natanko
 * enkrat (sidro raziskave M5-V.0 §5.5); ce je ni ali je vec, vrne <b>nespremenjene</b> bajte in to glasno
 * zapise - takrat {@code FarRender} rise v {@code RenderWorldLastEvent} (rezerva). Izid je v sistemski
 * lastnosti {@code rwcore.patch.render}.
 */
public final class RenderEntitiesPatch {
    public static final String TARGET_CLASS = "net.minecraft.client.renderer.RenderGlobal";
    public static final String PROP_PATCH = "rwcore.patch.render";
    public static final String ANCHOR = "blockentities";

    static final String TARGET_DESC =
            "(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V";
    static final String[] TARGET_METHOD = { "renderEntities", "func_180446_a" };

    static final String HOOK_OWNER = "noppes/npcs/rework/far/FarRender";
    static final String HOOK_NAME = "renderEntities";
    static final String HOOK_DESC = TARGET_DESC;

    private static final Logger LOG = LogManager.getLogger("customnpcs-rework-core");

    private RenderEntitiesPatch() {
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
                if (TARGET_DESC.equals(m.desc) && (TARGET_METHOD[0].equals(m.name) || TARGET_METHOD[1].equals(m.name))) {
                    target = m;
                    break;
                }
            }
            if (target == null) {
                return fail(basicClass, "metode renderEntities" + TARGET_DESC + " ni");
            }
            if ((target.access & Opcodes.ACC_STATIC) != 0) {
                return fail(basicClass, "metoda renderEntities je staticna");
            }
            LdcInsnNode anchor = null;
            AbstractInsnNode[] insns = target.instructions.toArray();
            for (int i = 0; i < insns.length; ++i) {
                if (insns[i] instanceof LdcInsnNode && ANCHOR.equals(((LdcInsnNode) insns[i]).cst)) {
                    if (anchor != null) {
                        return fail(basicClass, "sidro \"" + ANCHOR + "\" se v metodi pojavi veckrat");
                    }
                    anchor = (LdcInsnNode) insns[i];
                }
            }
            if (anchor == null) {
                return fail(basicClass, "sidra \"" + ANCHOR + "\" v metodi ni");
            }
            InsnList call = new InsnList();
            call.add(new VarInsnNode(Opcodes.ALOAD, 1));
            call.add(new VarInsnNode(Opcodes.ALOAD, 2));
            call.add(new VarInsnNode(Opcodes.FLOAD, 3));
            call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER, HOOK_NAME, HOOK_DESC, false));
            target.instructions.insertBefore(anchor, call);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            System.setProperty(PROP_PATCH, "1");
            LOG.info(RwCoreMod.MARKER + " poseg v " + TARGET_CLASS + "." + target.name
                    + " uspel (izris nadomestkov L3 pred \"" + ANCHOR + "\")");
            return writer.toByteArray();
        } catch (Throwable t) {
            return fail(basicClass, "izjema: " + t);
        }
    }

    private static byte[] fail(byte[] basicClass, String why) {
        System.setProperty(PROP_PATCH, "0");
        LOG.error(RwCoreMod.MARKER + " poseg v " + TARGET_CLASS + " NI uspel (" + why
                + "); izris nadomestkov L3 gre v RenderWorldLastEvent (rezerva)");
        return basicClass;
    }

    /** Ali je poseg uspel (bere sistemsko lastnost, zato velja med nalagalniki razredov). */
    public static boolean patched() {
        return "1".equals(System.getProperty(PROP_PATCH));
    }
}
