package noppes.npcs.rework.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.ai.RwMoveHelperAccess;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.RwNavigatorAccess;
import noppes.npcs.entity.EntityNPCInterface;

/**
 * M3.3 — izvede odlocitev {@link RiderState} na nosilcu.
 *
 * <p>Uporaba v {@code EntityNPCInterface.onLivingUpdate} jahaca, okoli
 * {@code super.onLivingUpdate()}:
 * <pre>
 * MountGuard g = MountGuard.beforeRiderTick(this);
 * super.onLivingUpdate();
 * if (g != null) g.restore();
 * </pre>
 * Nosilec se v svetu posodobi pred svojimi potniki
 * ({@code World.updateEntityWithOptionalForce}), zato je stanje, zajeto pred tickom
 * jahaca, natanko stanje po nosilcevem lastnem AI ticku. {@code restore} torej razveljavi
 * samo prepis iz {@code updateEntityActionState} jahaca in nic drugega.
 *
 * <p>Znana meja: ce ima tudi jahac pot, vanilla {@code setPath} nosilcu ponastavi zasebno
 * stanje zaznave zataknitve; tega {@code restore} ne vrne (polja so zasebna). V nacinu 1
 * se to zgodi samo, ko imata pot oba.
 */
public final class MountGuard {
    private final PathNavigate navigator;
    private final Path path;
    private final double navSpeed;
    private final EntityMoveHelper helper;
    private final EntityMoveHelper.Action action;
    private final double[] doubles;
    private final float[] floats;

    private MountGuard(PathNavigate navigator, EntityMoveHelper helper) {
        this.navigator = navigator;
        this.path = RwNavigatorAccess.currentPath(navigator);
        this.navSpeed = RwNavigatorAccess.speed(navigator);
        this.helper = helper;
        this.action = helper.action;
        this.doubles = RwMoveHelperAccess.doubles(helper);
        this.floats = RwMoveHelperAccess.floats(helper);
    }

    /** Zajame stanje; neposredno uporabno tudi v testih brez entitete. */
    public static MountGuard capture(PathNavigate navigator, EntityMoveHelper helper) {
        return new MountGuard(navigator, helper);
    }

    /**
     * Vrne varovalo, ce mora ta tick krmiliti nosilec, sicer {@code null}. V nacinu
     * {@link RiderState#ORIGINAL} je to eno branje polja in nobena alokacija.
     */
    public static MountGuard beforeRiderTick(EntityLiving rider) {
        int m = RiderState.mode();
        if (m == RiderState.ORIGINAL) {
            return null;
        }
        Entity vehicle = rider.getRidingEntity();
        if (!(vehicle instanceof EntityLiving)) {
            return null;
        }
        EntityLiving mount = (EntityLiving) vehicle;
        RiderState.Steering s = RiderState.decide(m,
                mount instanceof EntityNPCInterface,
                !mount.getNavigator().noPath(),
                !rider.getNavigator().noPath());
        if (s != RiderState.Steering.MOUNT) {
            return null;
        }
        return capture(mount.getNavigator(), mount.getMoveHelper());
    }

    /** M3.4: {@link RiderState#riderMovementBlocked} za dano entiteto. */
    public static boolean riderMovementBlocked(EntityLiving rider) {
        int m = RiderState.mode();
        if (m == RiderState.ORIGINAL) {
            return false;
        }
        Entity vehicle = rider.getRidingEntity();
        if (!(vehicle instanceof EntityNPCInterface)) {
            return false;
        }
        return RiderState.riderMovementBlocked(m, true,
                !((EntityNPCInterface) vehicle).getNavigator().noPath());
    }

    public void restore() {
        RwNavigatorAccess.restore(navigator, path, navSpeed);
        RwMoveHelperAccess.restore(helper, action, doubles, floats);
    }
}
