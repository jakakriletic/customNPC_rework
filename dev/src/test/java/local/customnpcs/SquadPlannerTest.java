package local.customnpcs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import noppes.npcs.rework.formation.FormationShape;
import noppes.npcs.rework.formation.PathTrack;
import noppes.npcs.rework.formation.Slot;
import noppes.npcs.rework.formation.SlotAssigner;
import noppes.npcs.rework.formation.SquadPlanner;
import noppes.npcs.rework.formation.SquadPlanner.Phase;
import noppes.npcs.rework.formation.Terrain;

import org.junit.Test;

/**
 * Planer enote v simulaciji s tockastimi clani (M4.14a).
 *
 * <p>Simulacija ni Minecraft: clan se vsak tick premakne proti cilju za najvec zeleno
 * hitrost. Preverja se samo to, kar je planerjeva odgovornost: da se enota ne raztrga,
 * da pocaka zaostale in ne obtici zaradi enega, da se pred ozkim grlom stisne, da se
 * delna pot nadaljuje in da so koncna mesta poravnana in razlicna. Obnasanje v svetu
 * preveri scenarij M4.14 (docs/scenariji/M4.14-formacije.md).
 */
public class SquadPlannerTest {
    private static final double GROUND = 64;
    /** Priblizno hitrost NPC-ja s hitrostjo hoje 5: 2,2 * (0,25)^2 bloka na tick. */
    private static final double V = 0.137;

    /** Stanje simulacije. */
    private static final class Sim {
        final SquadPlanner planner;
        final double[] x;
        final double[] y;
        final double[] z;
        final boolean[] eligible;
        final double[] realSpeed;
        final boolean[] frozen;
        double worstMarchError;
        int maxTrailing;
        final Set<Phase> seen = new HashSet<Phase>();

        Sim(SquadPlanner planner, double[] x, double[] z, double[] realSpeed) {
            this.planner = planner;
            int n = x.length;
            this.x = x;
            this.z = z;
            this.y = new double[n];
            java.util.Arrays.fill(this.y, GROUND);
            this.eligible = new boolean[n];
            java.util.Arrays.fill(eligible, true);
            this.realSpeed = realSpeed;
            this.frozen = new boolean[n];
        }

        void step() {
            planner.update(x, y, z, eligible);
            seen.add(planner.phase());
            int trailingNow = 0;
            for (int i = 0; i < x.length; i++) {
                if (planner.trailing[i]) {
                    trailingNow++;
                }
                if (frozen[i] || planner.mode[i] == SquadPlanner.HOLD) {
                    continue;
                }
                double dx = planner.targetX[i] - x[i];
                double dz = planner.targetZ[i] - z[i];
                double d = Math.hypot(dx, dz);
                double v = Math.min(Math.min(planner.speed[i], realSpeed[i]), d);
                if (d > 1.0E-9) {
                    x[i] += dx / d * v;
                    z[i] += dz / d * v;
                }
            }
            maxTrailing = Math.max(maxTrailing, trailingNow);
            if (planner.phase() == Phase.MARCHING) {
                for (int i = 0; i < x.length; i++) {
                    if (!frozen[i] && !planner.excused[i] && !Double.isNaN(planner.error[i])) {
                        worstMarchError = Math.max(worstMarchError, planner.error[i]);
                    }
                }
            }
        }

        int runUntilDone(int maxTicks) {
            for (int t = 0; t < maxTicks; t++) {
                step();
                if (planner.phase() == Phase.DONE) {
                    return t;
                }
            }
            return -1;
        }
    }

    private static Sim legionMarch(int n, int width, PathTrack track, boolean reaches, double gx, double gz,
            Terrain terrain, double[] speeds) {
        double[] x = new double[n];
        double[] z = new double[n];
        // clani razmetani okoli zacetka
        java.util.Random rnd = new java.util.Random(11);
        for (int i = 0; i < n; i++) {
            x[i] = track.x(0) + rnd.nextDouble() * 12 - 6;
            z[i] = track.z(0) + rnd.nextDouble() * 12 - 6;
        }
        Slot[] shape = FormationShape.legion(n, width);
        double yaw = track.yawAt(0, 0, 4);
        int[] assignment = SlotAssigner.assign(x, z, shape, yaw);
        Slot[] slots = new Slot[n];
        for (int i = 0; i < n; i++) {
            slots[i] = shape[assignment[i]];
        }
        double[] vmax = speeds.clone();
        SquadPlanner p = new SquadPlanner(slots, vmax, track, reaches, gx, GROUND, gz, 0, terrain, true, true);
        return new Sim(p, x, z, speeds);
    }

    private static double[] uniform(int n, double v) {
        double[] out = new double[n];
        java.util.Arrays.fill(out, v);
        return out;
    }

    @Test
    public void legionMarchesTogetherAndArrivesOnDistinctBlockCenters() {
        int n = 20;
        PathTrack track = PathTrack.straight(0.5, GROUND, 0.5, 0.5, GROUND, 80.5);
        Sim sim = legionMarch(n, 5, track, true, 0.5, 80.5, new Terrain.Flat(GROUND), uniform(n, V));
        int ticks = sim.runUntilDone(4000);
        assertTrue("enota ni koncala", ticks > 0);
        assertTrue(sim.seen.contains(Phase.FORMING));
        assertTrue(sim.seen.contains(Phase.MARCHING));
        assertTrue(sim.seen.contains(Phase.SETTLING));
        // kohezija: med pohodom noben clan ne zaostane dlje od praga, pri katerem sidro stoji
        assertTrue("najvecja napaka med pohodom " + sim.worstMarchError,
                sim.worstMarchError <= SquadPlanner.LAG_STOP + 0.5);
        Set<Long> blocks = new HashSet<Long>();
        for (int i = 0; i < n; i++) {
            assertEquals(Math.floor(sim.x[i]) + 0.5, sim.x[i], SquadPlanner.SETTLED + 1.0E-6);
            assertEquals(Math.floor(sim.z[i]) + 0.5, sim.z[i], SquadPlanner.SETTLED + 1.0E-6);
            assertTrue("dva clana na istem bloku",
                    blocks.add(((long) Math.floor(sim.x[i]) << 32) ^ ((long) Math.floor(sim.z[i]) & 0xffffffffL)));
        }
        assertEquals(0, sim.planner.unsettledAtEnd());
    }

    @Test
    public void anchorAdaptsToSlowestMember() {
        int n = 12;
        double[] speeds = uniform(n, V);
        speeds[3] = V * 0.5;
        PathTrack track = PathTrack.straight(0.5, GROUND, 0.5, 60.5, GROUND, 0.5);
        Sim sim = legionMarch(n, 4, track, true, 60.5, 0.5, new Terrain.Flat(GROUND), speeds);
        assertEquals(V * 0.5 * SquadPlanner.CRUISE_FRACTION, sim.planner.cruise(), 1.0E-12);
        assertTrue(sim.runUntilDone(6000) > 0);
        assertTrue(sim.worstMarchError <= SquadPlanner.LAG_STOP + 0.5);
    }

    @Test
    public void unitWaitsForLaggardButNotForeverForAStuckOne() {
        int n = 10;
        PathTrack track = PathTrack.straight(0.5, GROUND, 0.5, 0.5, GROUND, 100.5);
        Sim sim = legionMarch(n, 5, track, true, 0.5, 100.5, new Terrain.Flat(GROUND), uniform(n, V));
        while (sim.planner.phase() != Phase.MARCHING) {
            sim.step();
        }
        for (int t = 0; t < 40; t++) {
            sim.step();
        }
        sim.frozen[7] = true;
        // najprej sidro upocasni in pocaka ...
        double slowest = Double.MAX_VALUE;
        double remainingWhenExcused = -1;
        for (int t = 0; t < 400 && remainingWhenExcused < 0; t++) {
            sim.step();
            slowest = Math.min(slowest, sim.planner.anchorSpeed());
            if (sim.planner.excused[7]) {
                remainingWhenExcused = sim.planner.remaining();
            }
        }
        assertEquals("sidro bi moralo pocakati", 0.0, slowest, 1.0E-12);
        // ... nato obticanega opravici in gre naprej brez njega
        assertTrue("obtican clan ni bil opravicen", remainingWhenExcused > 0);
        for (int t = 0; t < 200; t++) {
            sim.step();
        }
        assertTrue("enota bi morala nadaljevati", sim.planner.remaining() < remainingWhenExcused - 10);
        sim.frozen[7] = false;
        assertTrue(sim.runUntilDone(6000) > 0);
    }

    /** Zid pri z = 30 s tremi bloki sirokimi vrati pri x = -1..1. */
    private static final class WallWithDoor implements Terrain {
        @Override
        public double standY(double x, double yHint, double z) {
            if (Math.floor(z) == 30 && (Math.floor(x) < -1 || Math.floor(x) > 1)) {
                return Double.NaN;
            }
            return GROUND;
        }
    }

    @Test
    public void formationSqueezesThroughDoorAndReopens() {
        int n = 16;
        PathTrack track = PathTrack.straight(0.5, GROUND, 0.5, 0.5, GROUND, 70.5);
        WallWithDoor wall = new WallWithDoor();
        Sim sim = legionMarch(n, 8, track, true, 0.5, 70.5, wall, uniform(n, V));
        int trailingAfter = -1;
        for (int t = 0; t < 6000 && sim.planner.phase() != Phase.DONE; t++) {
            sim.step();
            for (int i = 0; i < n; i++) {
                assertFalse("cilj v zidu", Double.isNaN(wall.standY(sim.planner.targetX[i], GROUND, sim.planner.targetZ[i])));
            }
            if (sim.planner.phase() == Phase.MARCHING && sim.planner.anchorZ() > 55) {
                int c = 0;
                for (boolean b : sim.planner.trailing) {
                    if (b) {
                        c++;
                    }
                }
                trailingAfter = c;
            }
        }
        assertEquals(Phase.DONE, sim.planner.phase());
        assertTrue("pred vrati bi se morala formacija stisniti", sim.maxTrailing >= 4);
        assertEquals("za vrati bi se morala spet razpreti", 0, trailingAfter);
    }

    @Test
    public void partialPathIsContinued() {
        int n = 8;
        PathTrack first = PathTrack.straight(0.5, GROUND, 0.5, 0.5, GROUND, 30.5);
        Sim sim = legionMarch(n, 4, first, false, 0.5, 70.5, new Terrain.Flat(GROUND), uniform(n, V));
        boolean replanned = false;
        for (int t = 0; t < 6000 && sim.planner.phase() != Phase.DONE; t++) {
            sim.step();
            if (sim.planner.needsReplan()) {
                double ax = sim.planner.anchorX();
                double az = sim.planner.anchorZ();
                sim.planner.replaceTrack(PathTrack.straight(ax, GROUND, az, 0.5, GROUND, 70.5), true);
                replanned = true;
            }
        }
        assertTrue(replanned);
        assertEquals(Phase.DONE, sim.planner.phase());
        assertFalse(sim.planner.goalUnreachable());
        double cz = 0;
        for (int i = 0; i < n; i++) {
            cz += sim.z[i];
        }
        assertEquals(70.5, cz / n, 1.5);
    }

    @Test
    public void emptyContinuationsEndInSettlingAtPathEnd() {
        int n = 4;
        PathTrack first = PathTrack.straight(0.5, GROUND, 0.5, 0.5, GROUND, 20.5);
        Sim sim = legionMarch(n, 4, first, false, 0.5, 90.5, new Terrain.Flat(GROUND), uniform(n, V));
        for (int t = 0; t < 6000 && sim.planner.phase() != Phase.DONE; t++) {
            sim.step();
            if (sim.planner.needsReplan()) {
                sim.planner.replaceTrack(null, false);
            }
        }
        assertEquals(Phase.DONE, sim.planner.phase());
        assertTrue(sim.planner.goalUnreachable());
    }

    @Test
    public void shortMoveSkipsFormingAndMarching() {
        Slot[] slots = FormationShape.legion(3, 3);
        PathTrack track = PathTrack.straight(0.5, GROUND, 0.5, 0.5, GROUND, 5.5);
        SquadPlanner p = new SquadPlanner(slots, uniform(3, V), track, true, 0.5, GROUND, 5.5, 90,
                new Terrain.Flat(GROUND), true, true);
        assertEquals(Phase.SETTLING, p.phase());
    }

    @Test
    public void ringMembersFaceOutwardWhenSettled() {
        int n = 8;
        Slot[] shape = FormationShape.ring(n, 5);
        double[] x = new double[n];
        double[] z = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = 2 * Math.cos(i);
            z[i] = 2 * Math.sin(i);
        }
        int[] a = SlotAssigner.assign(x, z, shape, 0);
        Slot[] slots = new Slot[n];
        for (int i = 0; i < n; i++) {
            slots[i] = shape[a[i]];
        }
        SquadPlanner p = new SquadPlanner(slots, uniform(n, V), PathTrack.straight(0.5, GROUND, 0.5, 0.5, GROUND, 0.5),
                true, 0.5, GROUND, 0.5, 0, new Terrain.Flat(GROUND), true, true);
        Sim sim = new Sim(p, x, z, uniform(n, V));
        for (int t = 0; t < 2000 && p.phase() != Phase.DONE; t++) {
            sim.step();
            if (p.phase() == Phase.SETTLING) {
                boolean all = true;
                for (int i = 0; i < n; i++) {
                    all &= p.mode[i] == SquadPlanner.HOLD;
                }
                if (all) {
                    for (int i = 0; i < n; i++) {
                        double expected = Math.toDegrees(Math.atan2(-(p.targetX[i] - 0.5), p.targetZ[i] - 0.5));
                        assertEquals(expected, p.faceYaw[i], 1.0E-6);
                    }
                }
            }
        }
        assertEquals(Phase.DONE, p.phase());
    }
}
