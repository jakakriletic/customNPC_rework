package noppes.npcs.rework.army;

import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.rework.formation.*;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArmySwarmTest {
    @After public void off() { ArmyCommandService.setSwitch(0); }
    private ArmyOrder order(ArmyOrder.Mode mode, ArmyOrder.Stance stance) {
        return new ArmyOrder(0, 0, 4, 24, SquadOrder.Shape.LEGIJA, 3, 8, ArmyOrder.State.MOVING, mode, stance);
    }
    @Test public void everyModeAndStanceSurvivesNbtAndStateChanges() {
        for (ArmyOrder.Mode mode : ArmyOrder.Mode.values()) {
            for (ArmyOrder.Stance stance : ArmyOrder.Stance.values()) {
                ArmyOrder copy = ArmyOrder.read(order(mode, stance).write()).withState(ArmyOrder.State.JOINING);
                assertEquals(mode, copy.mode); assertEquals(stance, copy.stance);
                assertEquals(24, copy.z, 0); assertEquals(ArmyOrder.State.JOINING, copy.state);
            }
        }
    }
    @Test public void legacyOrderRetainsFormationAndEngage() {
        NBTTagCompound c = order(ArmyOrder.Mode.SWARM, ArmyOrder.Stance.PASSIVE).write();
        c.removeTag("Mode"); c.removeTag("Stance");
        ArmyOrder copy = ArmyOrder.read(c);
        assertEquals(ArmyOrder.Mode.FORMATION, copy.mode); assertEquals(ArmyOrder.Stance.ENGAGE, copy.stance);
    }
    @Test public void unknownOrWrongTypeNeverSilentlyEnablesCombat() {
        for (String key : new String[] {"Mode", "Stance"}) {
            NBTTagCompound c = order(ArmyOrder.Mode.SWARM, ArmyOrder.Stance.PASSIVE).write();
            c.setString(key, "UNKNOWN"); assertNull(ArmyOrder.read(c));
            c.setInteger(key, 1); assertNull(ArmyOrder.read(c));
        }
    }
    @Test public void stanceChangePreservesModeGeometryAndPhase() {
        ArmyOrder copy = order(ArmyOrder.Mode.SWARM, ArmyOrder.Stance.ENGAGE).withStance(ArmyOrder.Stance.PASSIVE);
        assertEquals(ArmyOrder.Mode.SWARM, copy.mode); assertEquals(ArmyOrder.Stance.PASSIVE, copy.stance);
        assertEquals(ArmyOrder.State.MOVING, copy.state); assertEquals(24, copy.z, 0);
    }
    @Test public void scatterIsStableBoundedAndKeepsAllDestinationsApart() {
        for (int count = 1; count <= 24; count++) {
            long[] seeds = new long[count];
            for (int i = 0; i < count; i++) { seeds[i] = i * 7919L; }
            Slot[] a = ArmySwarm.scatter(seeds, ArmySwarm.SEPARATION);
            Slot[] b = ArmySwarm.scatter(seeds, ArmySwarm.SEPARATION);
            assertEquals(count, a.length);
            for (int i = 0; i < count; i++) {
                assertEquals(a[i].side, b[i].side, 0); assertEquals(a[i].back, b[i].back, 0);
                assertTrue(Math.hypot(a[i].side, a[i].back) <= ArmySwarm.radius(count, ArmySwarm.SEPARATION));
                for (int j = 0; j < i; j++) {
                    assertTrue(Math.hypot(a[i].side - a[j].side, a[i].back - a[j].back) >= ArmySwarm.SEPARATION);
                }
            }
        }
    }
    @Test public void repeatedSeedsStillProduceSeparatedDestinations() {
        Slot[] p = ArmySwarm.scatter(new long[24], 1.8);
        for (int i = 0; i < p.length; i++) {
            for (int j = 0; j < i; j++) {
                assertTrue(Math.hypot(p[i].side - p[j].side, p[i].back - p[j].back) >= 1.8);
            }
        }
    }
    @Test(expected = IllegalArgumentException.class) public void unboundedScatterIsRefused() {
        ArmySwarm.scatter(new long[2049], 1.8);
    }
    @Test public void scatterInputAndResultAreNotSharedWithAnOrder() {
        Slot[] p = {new Slot(1, 2)};
        SquadOrder o = new SquadOrder(SquadOrder.Shape.LEGIJA, 4, 0, 4, 24, 0, 3, true, true, true, p);
        p[0] = new Slot(9, 9); assertEquals(1, o.scatter()[0].side, 0);
        o.scatter()[0] = new Slot(9, 9); assertEquals(2, o.scatter()[0].back, 0);
    }
    @Test public void looseMarchUsesTheSharedPathWithoutForming() {
        SquadPlanner p = planner(); p.beginLooseMarch();
        assertEquals(SquadPlanner.Phase.MARCHING, p.phase());
        p.update(new double[] {0}, new double[] {4}, new double[] {0}, new boolean[] {true});
        assertTrue(p.progress() > 0); assertTrue(p.progress() < 1);
    }
    private SquadPlanner planner() {
        return new SquadPlanner(new Slot[] {new Slot(0, 0)}, new double[] {.25},
                PathTrack.straight(0, 4, 0, 0, 4, 24), true, 0, 4, 24, 0, new Terrain.Flat(4), false, false);
    }
    @Test public void fighterCannotBeForgottenAtTheSettleTimeoutAndReturnsToSameGoal() {
        SquadPlanner p = planner(); p.blockCompletion(true);
        double[] x = {0}, y = {4}, z = {24};
        for (int i = 0; i < 600; i++) { p.update(x, y, z, new boolean[] {false}); }
        assertEquals(SquadPlanner.Phase.SETTLING, p.phase());
        p.blockCompletion(false); z[0] = 10;
        p.update(x, y, z, new boolean[] {true});
        assertNotEquals(SquadPlanner.Phase.DONE, p.phase()); assertEquals(24, p.targetZ[0], 0);
        z[0] = 24; p.update(x, y, z, new boolean[] {true});
        assertEquals(SquadPlanner.Phase.DONE, p.phase());
    }
    @Test public void switchZeroRejectsNewCallsBeforeDereferencingAnyWorld() {
        ArmyCommandService.setSwitch(0);
        assertFalse(ArmyCommandService.submitMove(null, null, "x", 0, 0, 4, 24, "swarm", 3, "passive").ok);
        assertFalse(ArmyCommandService.engagement(null, null, "x", "engage").ok);
        assertFalse(ArmyApi.move("x", 0, 0, 4, 24, "swarm", 3, "passive"));
        assertFalse(ArmyApi.engagement("x", "passive")); assertFalse(ArmyMarches.isPassive(null));
    }
    @Test public void badStanceIsNeverInterpretedAsEngage() {
        assertNull(ArmyCommandService.parseStance("attack!")); assertNull(ArmyCommandService.parseStance(null));
        assertEquals(ArmyOrder.Stance.ENGAGE, ArmyCommandService.parseStance("ENGAGE"));
        assertEquals(ArmyOrder.Stance.PASSIVE, ArmyCommandService.parseStance("passive"));
    }
    @Test public void snappedSwarmDestinationsRemainSeparatedForNativeNavigation() {
        SquadPlanner p = new SquadPlanner(new Slot[] {new Slot(.2, .2), new Slot(1.9, .8)},
                new double[] {.25, .25}, PathTrack.straight(0, 4, 0, 0, 4, 24), true,
                0, 4, 24, 0, new Terrain.Flat(4), false, true);
        p.finalGap(2);
        p.update(new double[] {0, 0}, new double[] {4, 4}, new double[] {0, 0}, new boolean[] {true, true});
        assertTrue(Math.hypot(p.targetX[0] - p.targetX[1], p.targetZ[0] - p.targetZ[1]) >= 2);
        assertEquals(.5, p.targetX[0] - Math.floor(p.targetX[0]), 0);
        assertEquals(.5, p.targetZ[1] - Math.floor(p.targetZ[1]), 0);
    }
}
