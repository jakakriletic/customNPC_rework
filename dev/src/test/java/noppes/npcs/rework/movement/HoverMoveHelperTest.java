package noppes.npcs.rework.movement;

import java.lang.reflect.Field;
import net.minecraft.entity.SharedMonsterAttributes;
import noppes.npcs.ai.FlyingMoveHelper;
import noppes.npcs.entity.EntityCustomNpc;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/** Real helper on an entity shell; collision/travel and path quality require the world run. */
public class HoverMoveHelperTest {
    private EntityCustomNpc npc;
    private HoverMoveHelper helper;

    @Before
    public void setUp() throws Exception {
        Class<?> type = Class.forName("sun.misc.Unsafe");
        Field f = type.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        npc = (EntityCustomNpc) type.getMethod("allocateInstance", Class.class)
                .invoke(f.get(null), EntityCustomNpc.class);
        npc.getAttributeMap().registerAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.25);
        helper = new HoverMoveHelper(npc);
    }

    @Test
    public void originalReproducesIgnoredNavigatorSpeedAndUncontrolledDrift() {
        // Target <1 block: the original collision loop has no iterations, so no world mock.
        FlyingMoveHelper original = new FlyingMoveHelper(npc);
        original.setMoveTo(0, 0, 0.75, 1);
        original.onUpdateMoveHelper();
        double full = npc.motionZ;
        npc.motionZ = 0;
        original = new FlyingMoveHelper(npc);
        original.setMoveTo(0, 0, 0.75, 0.5);
        original.onUpdateMoveHelper();
        assertEquals(full, npc.motionZ, 0);
        // Stop requesting movement: original keeps the old impulse; Hover stops immediately.
        double drift = 0;
        for (int i = 0; i < 40; i++) {
            original.onUpdateMoveHelper();
            drift += npc.motionZ;
            npc.motionZ *= 0.91;
        }
        assertTrue(drift > 0.25);
        helper.onUpdateMoveHelper();
        assertStopped();
    }

    @Test
    public void onlyHoverSelectsTheNewFlyingHelper() {
        for (int mode : new int[] {0, 2, -1, 3}) {
            npc.setRwFlightMode(mode);
            assertEquals(FlyingMoveHelper.class, HoverMoveHelper.forFlying(npc).getClass());
        }
        npc.setRwFlightMode(1);
        assertEquals(HoverMoveHelper.class, HoverMoveHelper.forFlying(npc).getClass());
    }

    @Test
    public void navigatorSpeedAndMovementAttributeScaleVelocity() {
        helper.setMoveTo(0, 0, 20, 1);
        helper.onUpdateMoveHelper();
        assertEquals(0.25, npc.motionZ, 1e-12);
        helper.setMoveTo(0, 0, 20, 0.5);
        helper.onUpdateMoveHelper();
        assertEquals(0.125, npc.motionZ, 1e-12);
        npc.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.4);
        helper.setMoveTo(0, 0, 20, 0.5);
        helper.onUpdateMoveHelper();
        assertEquals(0.2, npc.motionZ, 1e-12);
    }

    @Test
    public void threeDimensionalSpeedHasOneBudget() {
        helper.setMoveTo(10, 10, 10, 1);
        helper.onUpdateMoveHelper();
        assertEquals(0.25, Math.sqrt(npc.motionX * npc.motionX + npc.motionY * npc.motionY
                + npc.motionZ * npc.motionZ), 1e-12);
        assertEquals(npc.motionX, npc.motionY, 1e-12);
        assertEquals(npc.motionX, npc.motionZ, 1e-12);
    }

    @Test
    public void slowsBeforeTargetAndNeverOvershootsIt() {
        npc.motionZ = 2;
        helper.setMoveTo(0, 0, 0.1, 1);
        helper.onUpdateMoveHelper();
        assertEquals(0.05, npc.motionZ, 1e-12);
        for (int i = 0; i < 30; i++) {
            helper.setMoveTo(0, 0, 0.1, 1);
            helper.onUpdateMoveHelper();
            npc.posZ += npc.motionZ;
            npc.motionZ *= 0.91; // air travel damping, not a replacement for the world run
            assertTrue(npc.posZ <= 0.1);
        }
        assertEquals(0.1, npc.posZ, 0.001);
    }

    @Test
    public void idleAndCancelledCommandsStopOnTheNextTick() {
        helper.setMoveTo(0, 10, 10, 1);
        helper.onUpdateMoveHelper();
        assertFalse(helper.isUpdating());
        helper.onUpdateMoveHelper(); // navigator stopped sending MOVE_TO
        assertStopped();
        npc.motionX = 3;
        npc.motionY = -2;
        npc.motionZ = 1;
        helper.onUpdateMoveHelper();
        assertStopped();
    }

    @Test
    public void zeroDistanceAndInvalidCommandsCannotProduceNaNOrReverseMotion() {
        double[][] commands = {{0, 0, 0, 1}, {0, 0, 10, 0}, {0, 0, 10, -1},
                {Double.NaN, 0, 10, 1}, {0, Double.POSITIVE_INFINITY, 0, 1},
                {0, 0, 10, Double.NaN}, {0, 0, 10, Double.POSITIVE_INFINITY}};
        for (double[] c : commands) {
            npc.motionX = 1;
            helper.setMoveTo(c[0], c[1], c[2], c[3]);
            helper.onUpdateMoveHelper();
            assertStopped();
        }
    }

    @Test
    public void yawTurnsAtMostTwentyDegreesIncludingWrapAndVerticalFlight() {
        npc.rotationYaw = 179;
        helper.setMoveTo(-0.1, 0, -10, 1);
        helper.onUpdateMoveHelper();
        assertTrue(Math.abs(wrap(npc.rotationYaw - 179)) <= 20.001);
        npc.rotationYaw = 0;
        helper.setMoveTo(10, 0, 0, 1);
        helper.onUpdateMoveHelper();
        // Vanilla limitAngle represents -20 degrees as 340; compare the direction.
        assertEquals(-20, wrap(npc.rotationYaw), 1e-6);
        float yawBeforeVertical = npc.rotationYaw;
        helper.setMoveTo(0, 10, 0, 1);
        helper.onUpdateMoveHelper();
        assertEquals(yawBeforeVertical, npc.rotationYaw, 0);
        assertTrue(npc.motionY > 0);
    }

    @Test
    public void staleTravelInputsCannotAddAnExtraForwardAcceleration() {
        npc.moveForward = 1;
        npc.moveStrafing = 1;
        npc.moveVertical = 1;
        helper.setMoveTo(0, 0, 10, 1);
        helper.onUpdateMoveHelper();
        assertEquals(0, npc.moveForward, 0);
        assertEquals(0, npc.moveStrafing, 0);
        assertEquals(0, npc.moveVertical, 0);
    }

    private void assertStopped() {
        assertEquals(0, npc.motionX, 0);
        assertEquals(0, npc.motionY, 0);
        assertEquals(0, npc.motionZ, 0);
    }

    private static double wrap(double yaw) {
        return ((yaw % 360) + 540) % 360 - 180;
    }
}
