package noppes.npcs.rework.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Field;

import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.pathfinding.RwNavigatorAccess;

import org.junit.Before;
import org.junit.Test;

/**
 * M3.3: ponovi natanko prepis, ki ga vanilla {@code EntityLiving.updateEntityActionState}
 * jahaca naredi nosilcu (bytecode, M3.2): {@code setPath(jahac.getPath(), 1.5)} in
 * {@code moveHelper.read(jahac.moveHelper)}. Prvi test pokaze napako (pade "pred
 * popravkom"), drugi, da jo {@link MountGuard#restore()} razveljavi.
 *
 * <p>{@code PathNavigate} zahteva entiteto in svet; za ta test ju ne potrebuje, ker
 * {@code setPath(null, ..)} bere samo polje. Zato se ustvari brez konstruktorja.
 */
public class MountGuardTest {

    private PathNavigate mountNav;
    private EntityMoveHelper mountHelper;
    private EntityMoveHelper riderHelper;
    private Path mountPath;

    @Before
    public void setUp() throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field f = unsafeClass.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Object unsafe = f.get(null);
        mountNav = (PathNavigate) unsafeClass.getMethod("allocateInstance", Class.class)
                .invoke(unsafe, PathNavigateGround.class);
        mountPath = new Path(new PathPoint[] {new PathPoint(0, 4, 4), new PathPoint(0, 4, 5)});
        RwNavigatorAccess.restore(mountNav, mountPath, 0.7);

        mountHelper = new EntityMoveHelper(null);
        mountHelper.setMoveTo(0.5, 4.0, 5.5, 0.7);
        riderHelper = new EntityMoveHelper(null); // jahac brez cilja: WAIT
    }

    private void vanillaRiderOverwrite() {
        mountNav.setPath(null, 1.5);
        mountHelper.read(riderHelper);
    }

    @Test
    public void withoutGuardRiderErasesMountPath() {
        vanillaRiderOverwrite();
        assertNull("R1: jahac brez poti nosilcu izbrise pot", mountNav.getPath());
        assertEquals(EntityMoveHelper.Action.WAIT, mountHelper.action);
        assertEquals(1.0, mountHelper.getSpeed(), 0.0); // read: max(speed, 1.0)
    }

    @Test
    public void guardRestoresPathAndMoveHelper() {
        MountGuard g = MountGuard.capture(mountNav, mountHelper);
        vanillaRiderOverwrite();
        g.restore();
        assertSame(mountPath, mountNav.getPath());
        assertEquals(0.7, RwNavigatorAccess.speed(mountNav), 0.0);
        assertEquals(EntityMoveHelper.Action.MOVE_TO, mountHelper.action);
        assertEquals(0.5, mountHelper.getX(), 0.0);
        assertEquals(4.0, mountHelper.getY(), 0.0);
        assertEquals(5.5, mountHelper.getZ(), 0.0);
        assertEquals(0.7, mountHelper.getSpeed(), 0.0);
    }

    @Test
    public void guardAlsoUndoesRiderPathWhenBothHaveOne() {
        MountGuard g = MountGuard.capture(mountNav, mountHelper);
        Path riderPath = new Path(new PathPoint[] {new PathPoint(9, 4, 9)});
        RwNavigatorAccess.restore(mountNav, riderPath, 1.5); // ucinek setPath(riderPath, 1.5)
        mountHelper.read(riderHelper);
        g.restore();
        assertSame(mountPath, mountNav.getPath());
        assertNotSame(riderPath, mountNav.getPath());
    }

    @Test
    public void originalModeCreatesNoGuard() {
        RiderState.setMode(RiderState.ORIGINAL);
        assertNull(MountGuard.beforeRiderTick(null)); // v ORIGINAL se jahac niti ne bere
    }
}
