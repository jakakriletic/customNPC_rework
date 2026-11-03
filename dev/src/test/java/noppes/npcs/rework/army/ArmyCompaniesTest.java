package noppes.npcs.rework.army;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.rework.formation.*;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArmyCompaniesTest {
    private ArmyRegistry registry() {
        ArmyRegistry r = new ArmyRegistry();
        r.create(new Army("north", Army.OwnerType.FACTION, "0", "", 1));
        return r;
    }
    private ArmyOrder move(ArmyOrder.State state) {
        return new ArmyOrder(-1, 123.5, 64, -27.25, SquadOrder.Shape.KOLONA, 2, 8, state);
    }
    @After public void off() { ArmyCommandService.setSwitch(0); }

    @Test public void restartKeepsIntentForEveryPhase() {
        for (ArmyOrder.State state : ArmyOrder.State.values()) {
            ArmyRegistry a = registry(); a.order("NORTH", move(state));
            ArmyRegistry b = new ArmyRegistry(); b.read(a.write(new NBTTagCompound()));
            ArmyOrder o = b.order("north");
            assertEquals(state, o.state); assertEquals(-1, o.dimension);
            assertEquals(123.5, o.x, 0); assertEquals(64, o.y, 0); assertEquals(-27.25, o.z, 0);
            assertEquals(SquadOrder.Shape.KOLONA, o.shape); assertEquals(8, o.companySize);
            assertEquals(2, o.speed, 0); assertEquals(0, b.skippedOnRead());
        }
    }
    @Test public void legacyRegisterNeverCreatesAnOrder() {
        ArmyRegistry a = registry(), b = new ArmyRegistry();
        b.read(a.write(new NBTTagCompound())); assertNull(b.order("north"));
    }
    @Test public void removeAndRecreateDoesNotReviveAMove() {
        ArmyRegistry r = registry(); r.order("north", move(ArmyOrder.State.MOVING));
        Army a = r.remove("NORTH"); r.create(a); assertNull(r.order("north"));
        assertFalse(r.order("missing", move(ArmyOrder.State.WAITING)));
    }
    @Test public void readReplacesPreviousIntentAndWriteRemovesStaleTag() {
        ArmyRegistry r = registry(); r.order("north", move(ArmyOrder.State.MOVING));
        NBTTagCompound c = r.write(new NBTTagCompound());
        r.read(registry().write(new NBTTagCompound()));
        r.write(c); assertFalse(c.hasKey("Orders")); assertNull(r.order("north"));
    }
    @Test public void corruptIntentNeverBecomesAMoveToOrigin() {
        for (String key : Arrays.asList("X", "Y", "Z", "Dimension", "Company", "Speed", "State", "Shape")) {
            NBTTagCompound tag = move(ArmyOrder.State.MOVING).write(); tag.removeTag(key);
            assertNull(key, ArmyOrder.read(tag));
        }
    }
    @Test public void invalidNumbersAndUnsupportedShapeAreRefused() {
        assertFalse(ArmyOrder.valid(Double.NaN, 4, 0, SquadOrder.Shape.LEGIJA, 3));
        assertFalse(ArmyOrder.valid(0, Double.POSITIVE_INFINITY, 0, SquadOrder.Shape.LEGIJA, 3));
        assertFalse(ArmyOrder.valid(0, 4, 30000000, SquadOrder.Shape.LEGIJA, 3));
        assertFalse(ArmyOrder.valid(0, 256, 0, SquadOrder.Shape.LEGIJA, 3));
        assertFalse(ArmyOrder.valid(0, 4, 0, SquadOrder.Shape.MARCH, 3));
        assertFalse(ArmyOrder.valid(0, 4, 0, SquadOrder.Shape.LEGIJA, 0));
        assertFalse(ArmyOrder.valid(0, 4, 0, SquadOrder.Shape.LEGIJA, Double.NaN));
        assertTrue(ArmyOrder.valid(0, 4, 0, SquadOrder.Shape.LEGIJA, 3));
    }
    @Test public void duplicateOrOrphanOrdersAreCountedAndIgnored() {
        ArmyRegistry r = registry(); NBTTagCompound c = r.write(new NBTTagCompound());
        NBTTagList orders = new NBTTagList();
        NBTTagCompound valid = move(ArmyOrder.State.MOVING).write(); valid.setString("Army", "north");
        orders.appendTag(valid); orders.appendTag(valid.copy());
        NBTTagCompound orphan = move(ArmyOrder.State.MOVING).write(); orphan.setString("Army", "missing");
        orders.appendTag(orphan); c.setTag("Orders", orders); r.read(c);
        assertEquals(2, r.skippedOnRead()); assertNotNull(r.order("north")); assertNull(r.order("missing"));
    }
    @Test public void terminalOrdersStayTerminalAfterRestart() {
        assertTrue(move(ArmyOrder.State.WAITING).active()); assertTrue(move(ArmyOrder.State.JOINING).active());
        assertTrue(move(ArmyOrder.State.MOVING).active()); assertFalse(move(ArmyOrder.State.BLOCKED).active());
        assertFalse(move(ArmyOrder.State.ARRIVED).active()); assertFalse(move(ArmyOrder.State.CANCELLED).active());
    }
    @Test public void thirtyTwoMembersMakeFourCompaniesOfEight() {
        List<Integer> members = new ArrayList<Integer>(); for (int i = 0; i < 32; i++) { members.add(i); }
        List<List<Integer>> companies = ArmyCompanies.split(members, 8);
        assertEquals(4, companies.size()); for (List<Integer> c : companies) { assertEquals(8, c.size()); }
    }
    @Test public void distantStragglerApproachesWithinNavigatorRangeBeforeRejoining() {
        // Failed functional run: direct join from -60 to the main centroid returned no path.
        double[] first = ArmyCompanies.joinTarget(-60, 4, -4, 0, 4, -4);
        assertArrayEquals(new double[] {-36, 4, -4}, first, 1e-9);
        boolean[] main = ArmyCompanies.mainMembers(new double[] {-7, -5, first[0]},
                new double[] {-4, -4, first[2]}, 48);
        assertTrue(main[0]); assertTrue(main[1]); assertTrue(main[2]);
    }
    @Test public void joinWaypointKeepsNearbyGoalAndBoundsThreeDimensionalDistance() {
        assertArrayEquals(new double[] {3, 6, 9}, ArmyCompanies.joinTarget(3, 6, 9, 3, 6, 9), 0);
        assertArrayEquals(new double[] {4, 6, 9}, ArmyCompanies.joinTarget(3, 6, 9, 4, 6, 9), 0);
        double[] p = ArmyCompanies.joinTarget(0, 0, 0, 30, 40, 0);
        assertEquals(24, Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]), 1e-9);
        assertEquals(3.0 / 4, p[0] / p[1], 1e-9);
    }
    @Test public void mainComponentCannotBeChosenByDistanceToItsCentroid() {
        // A connected hollow ring around a disconnected singleton at its centroid.
        double[] xs = new double[17], zs = new double[17];
        for (int i = 0; i < 16; i++) {
            xs[i] = 40 * Math.cos(i * Math.PI / 8); zs[i] = 40 * Math.sin(i * Math.PI / 8);
        }
        boolean[] main = ArmyCompanies.mainMembers(xs, zs, 16);
        for (int i = 0; i < 16; i++) { assertTrue(main[i]); } assertFalse(main[16]);
    }
    @Test public void partialCompanyNeverLosesOrDuplicatesMembers() {
        List<Integer> members = new ArrayList<Integer>(); for (int i = 0; i < 35; i++) { members.add(i); }
        List<Integer> flattened = new ArrayList<Integer>();
        for (List<Integer> c : ArmyCompanies.split(members, 8)) { flattened.addAll(c); }
        assertEquals(members, flattened); assertEquals(35, new HashSet<Integer>(flattened).size());
        assertTrue(ArmyCompanies.split(new ArrayList<Integer>(), 8).isEmpty());
        assertEquals(1, ArmyCompanies.clampSize(0)); assertEquals(24, ArmyCompanies.clampSize(200));
    }
    @Test public void macroCompaniesHaveSpaceAndAreCentred() {
        Slot[] s = ArmyCompanies.macro(4, 8, SquadOrder.Shape.LEGIJA);
        double x = 0, z = 0;
        for (int i = 0; i < s.length; i++) {
            x += s[i].side; z += s[i].back;
            for (int j = i + 1; j < s.length; j++) {
                assertTrue(Math.hypot(s[i].side - s[j].side, s[i].back - s[j].back) >= 8);
            }
        }
        assertEquals(0, x, 1e-9); assertEquals(0, z, 1e-9);
        assertEquals(4, ArmyCompanies.macro(4, 8, SquadOrder.Shape.KOLONA).length);
        assertTrue(ArmyCompanies.macro(4, 8, SquadOrder.Shape.OBRAMBA)[0].faceOutward);
    }
    @Test public void companiesKeepOneCurvedRouteIncludingHeightAndLength() {
        PathTrack p = new PathTrack(new double[] {0, 10, 10}, new double[] {4, 4, 6}, new double[] {0, 0, 10});
        PathTrack lane = ArmyCompanies.lane(p, new Slot(4, 2), 0);
        assertEquals(p.size(), lane.size()); assertEquals(p.length(), lane.length(), 0);
        for (int i = 0; i <= 20; i++) {
            double d = p.length() * i / 20;
            assertEquals(p.x(d) - 4, lane.x(d), 1e-9); assertEquals(p.z(d) - 2, lane.z(d), 1e-9);
            assertEquals(p.y(d), lane.y(d), 0);
        }
    }
    @Test public void leadBarrierWaitsAndThenResumesWithoutGoingBackwards() {
        SquadPlanner p = new SquadPlanner(new Slot[] {new Slot(0, 0)}, new double[] {0.25},
                PathTrack.straight(0, 4, 0, 0, 4, 30), true, 0, 4, 30, 0, new Terrain.Flat(4), true, false);
        double[] x = {0}, y = {4}, z = {0}; boolean[] ok = {true};
        p.limitAdvance(4);
        for (int i = 0; i < 100; i++) { p.update(x, y, z, ok); x[0] = p.targetX[0]; z[0] = p.targetZ[0]; }
        assertEquals(4, p.progress(), 1e-9);
        p.limitAdvance(0); p.update(x, y, z, ok); assertEquals(4, p.progress(), 1e-9);
        p.limitAdvance(8);
        for (int i = 0; i < 100; i++) { p.update(x, y, z, ok); x[0] = p.targetX[0]; z[0] = p.targetZ[0]; }
        assertEquals(8, p.progress(), 1e-9);
    }
    @Test public void switchZeroRejectsEveryNewEntryWithoutWorldOrActor() {
        ArmyCommandService.setSwitch(0);
        assertFalse(ArmyCommandService.submitMove(null, null, "north", 0, 0, 4, 24, "legion", 3).ok);
        assertFalse(ArmyCommandService.cancel(null, null, "north").ok);
        assertFalse(ArmyCommandService.orderStatus(null, null, "north").ok);
        assertFalse(ArmyApi.move("north", 0, 0, 4, 24, "legion", 3));
        assertFalse(ArmyApi.stop("north")); assertEquals("", ArmyApi.status("north"));
    }
}
