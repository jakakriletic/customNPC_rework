package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import noppes.npcs.rework.formation.FormationMath;
import noppes.npcs.rework.formation.FormationShape;
import noppes.npcs.rework.formation.PathTrack;
import noppes.npcs.rework.formation.Slot;
import noppes.npcs.rework.formation.SlotAssigner;

import org.junit.Test;

/**
 * Geometrija formacij (M4.14a): konvencija yawa, oblike, dodelitev mest in pot sidra.
 * Napaka v konvenciji yawa se v svetu vidi kot zrcaljena formacija, zato je preverjena
 * posebej in ne samo posredno.
 */
public class FormationGeometryTest {
    private static final double EPS = 1.0E-9;

    @Test
    public void yawConventionMatchesMinecraft() {
        // yaw 0 gleda proti +Z, yaw 90 proti -X, desna roka pri yaw 0 kaze proti -X.
        assertEquals(0.0, FormationMath.forwardX(0), EPS);
        assertEquals(1.0, FormationMath.forwardZ(0), EPS);
        assertEquals(-1.0, FormationMath.forwardX(90), EPS);
        assertEquals(-1.0, FormationMath.rightX(0), EPS);
        assertEquals(0.0, FormationMath.yawOf(0, 1), EPS);
        assertEquals(90.0, FormationMath.yawOf(-1, 0), EPS);
        assertEquals(-90.0, FormationMath.yawOf(1, 0), EPS);
        assertTrue(Double.isNaN(FormationMath.yawOf(0, 0)));
    }

    @Test
    public void localAndWorldAreInverse() {
        Slot s = new Slot(3.0, -2.0);
        for (double yaw = -170; yaw < 180; yaw += 37) {
            double x = FormationMath.worldX(10, yaw, s);
            double z = FormationMath.worldZ(-4, yaw, s);
            assertEquals(3.0, FormationMath.localSide(x - 10, z + 4, yaw), 1.0E-9);
            assertEquals(-2.0, FormationMath.localBack(x - 10, z + 4, yaw), 1.0E-9);
        }
    }

    @Test
    public void turnTowardTakesShortWayAndIsBounded() {
        assertEquals(-176.0, FormationMath.turnToward(170, -170, 14), EPS);
        assertEquals(-170.0, FormationMath.turnToward(170, -170, 30), EPS);
        assertEquals(4.0, FormationMath.turnToward(0, 90, 4), EPS);
    }

    @Test
    public void legionHasScriptSpacingAndManipleGap() {
        Slot[] slots = FormationShape.legion(10, 10);
        assertEquals(10, slots.length);
        // en sam red: stolpci 0..4 razmik 2, med 4 in 5 razmik 3
        double[] sides = new double[10];
        for (int i = 0; i < 10; i++) {
            sides[i] = slots[i].side;
            assertEquals(0.0, slots[i].back, EPS);
        }
        java.util.Arrays.sort(sides);
        assertEquals(2.0, sides[1] - sides[0], EPS);
        assertEquals(3.0, sides[5] - sides[4], EPS);
        assertCentered(slots);
    }

    @Test
    public void partialLastRowIsCentered() {
        Slot[] slots = FormationShape.block(7, 5, 2.0, 0, 0);
        assertEquals(7, slots.length);
        double lastRowSum = 0;
        int lastRow = 0;
        for (Slot s : slots) {
            if (s.back > 0) {
                lastRowSum += s.side;
                lastRow++;
            }
        }
        assertEquals(2, lastRow);
        assertEquals(0.0, lastRowSum / lastRow, 1.0E-9);
    }

    @Test
    public void ringFillsInnerRingFirstAndFacesOut() {
        Slot[] slots = FormationShape.ring(20, 5.0);
        assertEquals(20, slots.length);
        int inner = 0;
        for (Slot s : slots) {
            assertTrue(s.faceOutward);
            double r = Math.hypot(s.side, s.back);
            if (Math.abs(r - 5.0) < 1.0E-6) {
                inner++;
            } else {
                assertEquals(8.0, r, 1.0E-6);
            }
        }
        assertEquals((int) Math.floor(5.0 * 2 * Math.PI / FormationShape.RING_SPACING), inner);
        assertMinDistance(slots, 1.5);
    }

    @Test
    public void columnIsNarrow() {
        Slot[] slots = FormationShape.column(9, 2);
        for (Slot s : slots) {
            assertTrue(Math.abs(s.side) <= FormationShape.COLUMN_SIDE_SPACING / 2 + EPS);
        }
        assertCentered(slots);
    }

    @Test
    public void keepPreservesRelativeLayout() {
        double[] xs = {0, 4, 0};
        double[] zs = {0, 0, 6};
        Slot[] slots = FormationShape.keep(xs, zs, 0);
        // pri yaw 0 je desno -X, nazaj -Z
        assertEquals(slots[0].side - slots[1].side, 4.0, EPS);
        assertEquals(slots[0].back - slots[2].back, 6.0, EPS);
        assertCentered(slots);
    }

    @Test
    public void assignerKeepsOrderWhenMembersAlreadyStandInShape() {
        Slot[] slots = FormationShape.legion(12, 4);
        double yaw = 30;
        double[] xs = new double[12];
        double[] zs = new double[12];
        // clani stojijo v isti obliki, a v premesanem vrstnem redu indeksov
        int[] perm = {5, 2, 11, 0, 7, 9, 1, 3, 10, 4, 8, 6};
        for (int i = 0; i < 12; i++) {
            Slot s = slots[perm[i]];
            xs[i] = FormationMath.worldX(100, yaw, s);
            zs[i] = FormationMath.worldZ(-50, yaw, s);
        }
        int[] got = SlotAssigner.assign(xs, zs, slots, yaw);
        for (int i = 0; i < 12; i++) {
            assertEquals("clan " + i, perm[i], got[i]);
        }
    }

    @Test
    public void assignerUsesEverySlotOnce() {
        Slot[] slots = FormationShape.legion(17, 5);
        double[] xs = new double[17];
        double[] zs = new double[17];
        java.util.Random rnd = new java.util.Random(7);
        for (int i = 0; i < 17; i++) {
            xs[i] = rnd.nextDouble() * 30;
            zs[i] = rnd.nextDouble() * 30;
        }
        int[] got = SlotAssigner.assign(xs, zs, slots, 0);
        Set<Integer> used = new HashSet<Integer>();
        for (int g : got) {
            assertTrue(g >= 0);
            assertTrue(used.add(g));
        }
    }

    @Test
    public void ringAssignmentFollowsAngle() {
        Slot[] slots = FormationShape.ring(10, 6.0);
        double[] xs = new double[10];
        double[] zs = new double[10];
        int[] perm = {3, 8, 1, 6, 0, 9, 2, 5, 7, 4};
        for (int i = 0; i < 10; i++) {
            Slot s = slots[perm[i]];
            xs[i] = FormationMath.worldX(0, 0, s) * 0.8;
            zs[i] = FormationMath.worldZ(0, 0, s) * 0.8;
        }
        int[] got = SlotAssigner.assign(xs, zs, slots, 0);
        for (int i = 0; i < 10; i++) {
            assertEquals(perm[i], got[i]);
        }
    }

    @Test
    public void trackInterpolatesByArcLength() {
        PathTrack t = new PathTrack(new double[] {0, 0, 10}, new double[] {64, 64, 64}, new double[] {0, 10, 10});
        assertEquals(20.0, t.length(), EPS);
        assertEquals(0.0, t.x(5), EPS);
        assertEquals(5.0, t.z(5), EPS);
        assertEquals(5.0, t.x(15), EPS);
        assertEquals(10.0, t.z(15), EPS);
        assertEquals(10.0, t.x(99), EPS);
        assertEquals(0.0, t.yawAt(2, 1, 1), EPS);
        assertEquals(-90.0, t.yawAt(16, 1, 1), EPS);
    }

    @Test
    public void fromAnchorDropsPointsBehindAnchor() {
        double[] xs = {0, 0, 0, 0, 0};
        double[] ys = {64, 64, 64, 64, 64};
        double[] zs = {0, 1, 2, 3, 4};
        PathTrack t = PathTrack.fromAnchor(0.2, 64, 2.9, xs, ys, zs);
        assertEquals(0.2, t.x(0), EPS);
        assertEquals(2.9, t.z(0), EPS);
        assertEquals(4.0, t.endZ(), EPS);
        assertTrue(t.length() < 1.5);
    }

    private static void assertCentered(Slot[] slots) {
        double s = 0;
        double b = 0;
        for (Slot sl : slots) {
            s += sl.side;
            b += sl.back;
        }
        assertEquals(0.0, s / slots.length, 1.0E-9);
        assertEquals(0.0, b / slots.length, 1.0E-9);
    }

    private static void assertMinDistance(Slot[] slots, double min) {
        for (int i = 0; i < slots.length; i++) {
            for (int j = i + 1; j < slots.length; j++) {
                double d = Math.hypot(slots[i].side - slots[j].side, slots[i].back - slots[j].back);
                assertTrue("mesti " + i + "," + j + " preblizu: " + d, d >= min);
            }
        }
    }
}
