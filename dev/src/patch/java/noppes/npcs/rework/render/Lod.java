package noppes.npcs.rework.render;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomNpcs;

/**
 * M5-K K5: stopnja podrobnosti L1 po razdalji ({@code RwLodStart}).
 *
 * <p>NPC, ki je od kamere dlje kot {@code RwLodStart} blokov, se upodobi brez oci (skupaj z
 * obrvmi in sijajem; {@code LayerEyes}), s plosko plastjo pokrivala modela ({@code bipedHeadwear}, videz
 * {@code HeadWearType=0}) namesto 3D pokrivala ({@code ModelHeadwear}: 6 ekstrudiranih delov, ~816
 * oglisc) in brez imena. Profil M5-K.1: pokrivalo 26 % in oci 14 % render niti pri 500 NPC-jih.
 * Razdalja je kot pri imenu v originalu ({@code RenderNPCInterface.renderName}):
 * {@code getDistanceSq} do {@code renderViewEntity}. 0 = original (vse podrobnosti na vsaki razdalji).
 */
@SideOnly(Side.CLIENT)
public final class Lod {
    /** Statistika za harness (render nit): odlocitve pokrivala (enkrat na NPC na izris). */
    public static long farDraws;
    public static long nearDraws;

    private Lod() {
    }

    /** Ali je NPC na kvadratu razdalje {@code distSq} pri pragu {@code start} blokov na stopnji L1. */
    public static boolean far(double distSq, int start) {
        return start > 0 && distSq > (double) start * start;
    }

    /** Ali se NPC ta trenutek upodobi na stopnji L1 (brez stetja). */
    public static boolean far(Entity npc) {
        int start = CustomNpcs.RwLodStart;
        if (start <= 0) {
            return false;
        }
        Entity view = Minecraft.getMinecraft().getRenderManager().renderViewEntity;
        return view != null && far(npc.getDistanceSq(view), start);
    }

    /** Kot {@link #far(Entity)}, steje odlocitev; klice se enkrat na NPC na izris. */
    public static boolean farCounted(Entity npc) {
        boolean f = far(npc);
        if (f) {
            farDraws++;
        } else if (CustomNpcs.RwLodStart > 0) {
            nearDraws++;
        }
        return f;
    }
}
