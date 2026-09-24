package net.minecraft.entity.ai;

/**
 * M3.3: dostop do zasciteneh polj {@code EntityMoveHelper} za
 * {@link noppes.npcs.rework.entity.MountGuard}.
 *
 * <p>Vanilla {@code EntityLiving.updateEntityActionState} jahaca nosilcu naredi
 * {@code getMoveHelper().read(jahac.getMoveHelper())}. Da se to da razveljaviti, je treba
 * shraniti natanko polja, ki jih {@code read} prepise. Razred je v istem paketu kot
 * {@code EntityMoveHelper} (vzorec {@code RwWorldAccess}, M3.1), zato brez refleksije.
 */
public final class RwMoveHelperAccess {
    private RwMoveHelperAccess() {
    }

    /** Stanje move helperja: posX, posY, posZ, speed. */
    public static double[] doubles(EntityMoveHelper helper) {
        return new double[] {helper.posX, helper.posY, helper.posZ, helper.speed};
    }

    /** Stanje move helperja: moveForward, moveStrafe. */
    public static float[] floats(EntityMoveHelper helper) {
        return new float[] {helper.moveForward, helper.moveStrafe};
    }

    public static void restore(EntityMoveHelper helper, EntityMoveHelper.Action action,
            double[] d, float[] f) {
        helper.action = action;
        helper.posX = d[0];
        helper.posY = d[1];
        helper.posZ = d[2];
        helper.speed = d[3];
        helper.moveForward = f[0];
        helper.moveStrafe = f[1];
    }
}
