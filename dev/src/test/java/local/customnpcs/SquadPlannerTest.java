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

    /**
     * G/5 on 44f3390b holds its column slot at tick 180, but pathUntil=211
     * still sends it to PATH when the anchor moves at 200. Reproduce the
     * planner transition without Minecraft physics or private-field writes.
     */
    private static Sim stuckMemberRecovery() {
        return stuckMemberRecovery(new Terrain.Flat(GROUND));
    }

    private static Sim stuckMemberRecovery(Terrain terrain) {
        SquadPlanner p = new SquadPlanner(new Slot[] {new Slot(0, 0), new Slot(2, 0)},
                uniform(2, 0.606375),
                PathTrack.straight(0.5, GROUND, 0.5, 0.5, GROUND, 80.5), true,
                0.5, GROUND, 80.5, 0, terrain, true, true);
        Sim sim = new Sim(p, new double[] {0.5, -1.5}, new double[] {0.5, 0.5},
                uniform(2, 0.606375));
        p.update(sim.x, sim.y, sim.z, sim.eligible);
        assertEquals("fixture must begin marching", Phase.MARCHING, p.phase());
        sim.z[0] -= 2.5;
        for (int t = 0; t < 2 * SquadPlanner.STUCK_WINDOW + 2 && p.stuckEvents() == 0; t++) {
            // Member 0 does not move; member 1 follows. Only the planner's
            // own stuck detector may start recovery, never a seeded penalty.
            sim.x[1] = p.targetX[1];
            sim.z[1] = p.targetZ[1];
            p.update(sim.x, sim.y, sim.z, sim.eligible);
        }
        assertTrue("fixture must detect the blocked member", p.stuckEvents() > 0);
        assertEquals("fixture must actually request recovery", SquadPlanner.PATH, p.mode[0]);
        return sim;
    }

    private static void acquireRecoverySlotWhileAnchorWaits(Sim sim) {
        SquadPlanner p = sim.planner;
        sim.x[0] = p.targetX[0];
        sim.z[0] = p.targetZ[0];
        sim.x[1] = p.targetX[1];
        sim.z[1] = p.targetZ[1] - SquadPlanner.LAG_STOP - 0.5;
        p.update(sim.x, sim.y, sim.z, sim.eligible);
        assertEquals("the other member must keep the anchor stopped", 0, p.anchorSpeed(), 0);
        assertEquals("recovery has acquired the current slot", SquadPlanner.HOLD, p.mode[0]);
        assertTrue(p.settled[0]);
        assertEquals(Phase.MARCHING, p.phase());
    }

    @Test
    public void acquiredRecoverySlotResumesWithSteerWhenAnchorMoves() {
        Sim sim = stuckMemberRecovery();
        SquadPlanner p = sim.planner;
        acquireRecoverySlotWhileAnchorWaits(sim);
        sim.x[1] = p.targetX[1];
        sim.z[1] = p.targetZ[1];
        p.update(sim.x, sim.y, sim.z, sim.eligible);
        assertTrue("fixture must restart the anchor", p.anchorSpeed() > 0);
        double distance = Math.hypot(p.targetX[0] - sim.x[0], p.targetZ[0] - sim.z[0]);
        assertTrue("the new slot needs motion within steering range",
                distance > SquadPlanner.SETTLED && distance < SquadPlanner.STEER_MAX);
        assertEquals("arrival must end the old recovery episode, not restart searches",
                SquadPlanner.STEER, p.mode[0]);
    }

    @Test
    public void unrecoveredBlockedMemberStillNeedsPath() {
        Sim sim = stuckMemberRecovery();
        SquadPlanner p = sim.planner;
        sim.x[1] = p.targetX[1];
        sim.z[1] = p.targetZ[1];
        p.update(sim.x, sim.y, sim.z, sim.eligible);
        assertEquals("an unfinished recovery must not be cancelled indiscriminately",
                SquadPlanner.PATH, p.mode[0]);
    }

    @Test
    public void acquiredSlotStillNeedsPathAfterDistantDisplacement() {
        // D-083: zunaj STEER_MAX je iskanje potrebno, ko ravna crta do cilja ni prehodna
        // (zid pri x = 3..4 med premaknjenim clanom pri x ~7,5 in mestom pri x ~0,5).
        Sim sim = stuckMemberRecovery(new SideWall());
        SquadPlanner p = sim.planner;
        acquireRecoverySlotWhileAnchorWaits(sim);
        sim.x[0] += SquadPlanner.STEER_MAX + 1;
        p.update(sim.x, sim.y, sim.z, sim.eligible);
        assertEquals("arrival must not suppress recovery outside steering range",
                SquadPlanner.PATH, p.mode[0]);
    }

    @Test
    public void acquiredSlotSteersBackAfterDistantDisplacementOnClearLine() {
        // D-083: tocka kolone po prosti ravni crti je STEER tudi zunaj STEER_MAX.
        Sim sim = stuckMemberRecovery();
        SquadPlanner p = sim.planner;
        acquireRecoverySlotWhileAnchorWaits(sim);
        sim.x[0] += SquadPlanner.STEER_MAX + 1;
        p.update(sim.x, sim.y, sim.z, sim.eligible);
        assertTrue("fixture: beyond steering range", p.error[0] > SquadPlanner.STEER_MAX);
        assertEquals(SquadPlanner.STEER, p.mode[0]);
    }

    /** Zid vzdolz proge pri x = 3..4 (bloka), brez odprtine. */
    private static final class SideWall implements Terrain {
        @Override
        public double standY(double x, double yHint, double z) {
            return Math.floor(x) == 3 || Math.floor(x) == 4 ? Double.NaN : GROUND;
        }
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
    public void settlingRequiresNewArrivalWhenTheFinalSlotChanges() {
        // Vanilla may end a complete path up to GOAL_REACHED (2 blocks) from
        // the requested centre. This one ends one block before the final slot.
        // A flag acquired at the moving slot must not count as arrival there.
        SquadPlanner p = new SquadPlanner(FormationShape.legion(1, 1), uniform(1, V),
                PathTrack.straight(0.5, GROUND, 0.5, 0.5, GROUND, 20.5), true,
                0.5, GROUND, 21.5, 0, new Terrain.Flat(GROUND), true, true);
        double[] x = {0.5};
        double[] y = {GROUND};
        double[] z = {0.5};
        boolean[] eligible = {true};
        // Follow each moving target exactly, without relying on simulated
        // collision or Minecraft's speed formula. Only phase state is tested.
        for (int t = 0; t < 1000 && p.phase() != Phase.SETTLING; t++) {
            p.update(x, y, z, eligible);
            if (p.phase() != Phase.SETTLING) {
                x[0] = p.targetX[0];
                z[0] = p.targetZ[0];
            }
        }
        assertEquals("did not reach the phase boundary", Phase.SETTLING, p.phase());
        double error = Math.hypot(p.targetX[0] - x[0], p.targetZ[0] - z[0]);
        assertTrue("fixture must be outside arrival tolerance", error > SquadPlanner.SETTLED);
        assertTrue("fixture must expose inherited hysteresis", error < SquadPlanner.UNSETTLE);

        p.update(x, y, z, eligible);
        assertEquals("a moving-slot flag is not final-slot arrival", Phase.SETTLING, p.phase());
        assertFalse("must reacquire the new slot", p.settled[0]);
        assertEquals(SquadPlanner.STEER, p.mode[0]);
        // After acquiring that slot, normal completion must still work.
        x[0] = p.targetX[0];
        z[0] = p.targetZ[0];
        p.update(x, y, z, eligible);
        assertEquals(Phase.DONE, p.phase());
        assertEquals(0, p.unsettledAtEnd());
    }

    @Test
    public void completionPublishesOutwardFacingForEveryRingMember() {
        Slot[] slots = FormationShape.ring(8, 5);
        SquadPlanner p = new SquadPlanner(slots, uniform(slots.length, V),
                PathTrack.straight(0.5, GROUND, 0.5, 0.5, GROUND, 0.5), true,
                0.5, GROUND, 0.5, 0, new Terrain.Flat(GROUND), true, true);
        double[] x = new double[slots.length];
        double[] y = uniform(slots.length, GROUND);
        double[] z = new double[slots.length];
        boolean[] eligible = new boolean[slots.length];
        java.util.Arrays.fill(eligible, true);
        // All members acquire their slots on the same tick: there need not be
        // an earlier HOLD tick from which Squad can remember the final yaw.
        for (int i = 0; i < slots.length; i++) {
            x[i] = Math.floor(0.5 - slots[i].side) + 0.5;
            z[i] = Math.floor(0.5 - slots[i].back) + 0.5;
        }
        p.update(x, y, z, eligible);
        assertEquals(Phase.DONE, p.phase());
        for (int i = 0; i < slots.length; i++) {
            assertEquals(SquadPlanner.HOLD, p.mode[i]);
            assertFalse("final yaw missing for member " + i, Double.isNaN(p.faceYaw[i]));
            double expected = Math.toDegrees(Math.atan2(-(x[i] - 0.5), z[i] - 0.5));
            assertEquals(expected, p.faceYaw[i], 1.0E-6);
        }
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

    // M4.14d poskus 2 (D-083): pravo iskanje samo ob dokazu. Skupine iz razclenitve D-078
    // (navigator sled 984dc27a, proga G) in podpis zavrnjenega poskusa 1 (Q65).

    private static SquadPlanner settlingSingle(PathTrack track, double gx, double gz, Terrain terrain) {
        return new SquadPlanner(new Slot[] {new Slot(0, 0)}, uniform(1, 0.606375), track, true,
                gx, GROUND, gz, 0, terrain, false, true);
    }

    /** Clan 0 zaostaja 4,5 bloka, a se premika: po WAIT_LIMIT je opravicen, ne obtical. */
    private static Sim excusedLaggard() {
        SquadPlanner p = new SquadPlanner(new Slot[] {new Slot(0, 0), new Slot(2, 0)},
                uniform(2, 0.606375),
                PathTrack.straight(0.5, GROUND, 0.5, 0.5, GROUND, 80.5), true,
                0.5, GROUND, 80.5, 0, new Terrain.Flat(GROUND), true, true);
        Sim sim = new Sim(p, new double[] {0.5, -1.5}, new double[] {0.5, 0.5}, uniform(2, 0.606375));
        p.update(sim.x, sim.y, sim.z, sim.eligible);
        assertEquals("fixture must begin marching", Phase.MARCHING, p.phase());
        for (int t = 0; t < 4 * SquadPlanner.WAIT_LIMIT && !p.excused[0]; t++) {
            sim.x[0] = p.targetX[0];
            sim.z[0] = p.targetZ[0] - 4.5;
            sim.x[1] = p.targetX[1];
            sim.z[1] = p.targetZ[1];
            p.update(sim.x, sim.y, sim.z, sim.eligible);
        }
        assertTrue("fixture must excuse the laggard", p.excused[0]);
        assertEquals("fixture: a moving laggard is not stuck", 0, p.stuckEvents());
        return sim;
    }

    @Test
    public void excuseAloneDoesNotForceSearch() {
        // Opravicilo (G/7@170, G/1@193): prej je kazen pathUntil pomenila A* k tocki
        // kolone, ceprav je bila ravna crta do nje prosta.
        Sim sim = excusedLaggard();
        SquadPlanner p = sim.planner;
        assertTrue(p.trailing[0]);
        assertEquals(SquadPlanner.STEER, p.mode[0]);
    }

    @Test
    public void distantColumnPointWithClearLineIsSteeredNotSearched() {
        // Tocka kolone premaknjena dlje od STEER_MAX (G/1/4@200, 2@201, 0@202): na prosti
        // ravni crti do tocke na skupni poti iskanje ni potrebno.
        Sim sim = excusedLaggard();
        SquadPlanner p = sim.planner;
        sim.x[0] = p.targetX[0];
        sim.z[0] = p.targetZ[0] - 8;
        sim.x[1] = p.targetX[1];
        sim.z[1] = p.targetZ[1];
        p.update(sim.x, sim.y, sim.z, sim.eligible);
        assertTrue(p.trailing[0]);
        assertTrue("fixture: beyond steering range", p.error[0] > SquadPlanner.STEER_MAX);
        assertEquals(SquadPlanner.STEER, p.mode[0]);
    }

    @Test
    public void blockedMemberBesidePathFollowsItForwardThroughDoor() {
        // Clan ob zidu bocno od vrat (z ~71,7 ob zidu 72 na progi G), cilj za vrati:
        // ravna crta zadene zid, skupna pot gre skozi vrata.
        PathTrack track = PathTrack.straight(0.5, GROUND, 0.5, 0.5, GROUND, 40.5);
        SquadPlanner p = settlingSingle(track, 0.5, 40.5, new WallWithDoor());
        assertEquals(Phase.SETTLING, p.phase());
        p.update(new double[] {-3.0}, new double[] {GROUND}, new double[] {28.5}, new boolean[] {true});
        assertTrue("fixture: beyond steering range", p.error[0] > SquadPlanner.STEER_MAX);
        assertEquals(SquadPlanner.TRACK, p.mode[0]);
        assertEquals(28.0, p.trackFrom[0], 1.0E-9);
        assertEquals(40.0, p.trackTo[0], 1.0E-9);
    }

    @Test
    public void blockedMemberWithTargetBehindDoesNotFollowPathBackwards() {
        // Podpis poskusa 1 (Q65, 151/181/193/211): TRACK s ciljem za clanom je vlekel
        // clane nazaj po sredinski crti; nazaj sme le pravo iskanje.
        PathTrack track = PathTrack.straight(0.5, GROUND, 0.5, 0.5, GROUND, 40.5);
        SquadPlanner p = settlingSingle(track, 0.5, 20.5, new WallWithDoor());
        p.update(new double[] {-3.0}, new double[] {GROUND}, new double[] {31.5}, new boolean[] {true});
        assertTrue(Double.isNaN(p.trackFrom[0]));
        assertEquals(SquadPlanner.PATH, p.mode[0]);
    }

    @Test
    public void memberFarFromSharedPathStillSearches() {
        PathTrack track = PathTrack.straight(0.5, GROUND, 0.5, 0.5, GROUND, 40.5);
        SquadPlanner p = settlingSingle(track, 0.5, 40.5, new WallWithDoor());
        p.update(new double[] {0.5 - SquadPlanner.TRACK_JOIN - 1}, new double[] {GROUND}, new double[] {28.5},
                new boolean[] {true});
        assertEquals(SquadPlanner.PATH, p.mode[0]);
    }

    @Test
    public void stalledTrackFollowerSearchesOncePerStuckEvent() {
        // Sledenje brez napredka je obticanje kot pri STEER; obticanje dovoli eno iskanje
        // (FA5: 1 + obnove + obticanja), potem clan spet sledi skupni poti.
        PathTrack track = PathTrack.straight(0.5, GROUND, 0.5, 0.5, GROUND, 40.5);
        SquadPlanner p = settlingSingle(track, 0.5, 40.5, new WallWithDoor());
        double[] x = {-3.0};
        double[] y = {GROUND};
        double[] z = {28.5};
        boolean[] eligible = {true};
        for (int t = 0; t < 2 * SquadPlanner.STUCK_WINDOW + 2 && p.stuckEvents() == 0; t++) {
            p.update(x, y, z, eligible);
        }
        assertEquals(1, p.stuckEvents());
        assertEquals(SquadPlanner.PATH, p.mode[0]);
        p.searched(0);
        p.update(x, y, z, eligible);
        assertEquals(1, p.stuckEvents());
        assertEquals(SquadPlanner.TRACK, p.mode[0]);
    }

    @Test
    public void straightLineProbeRespectsWallsDoorWidthAndStepHeight() {
        WallWithDoor wall = new WallWithDoor();
        assertTrue(SquadPlanner.straightWalkable(new Terrain.Flat(GROUND), 0.5, GROUND, 0.5, 9.5, 17.5));
        assertTrue("sredina vrat", SquadPlanner.straightWalkable(wall, 0.5, GROUND, 25.5, 0.5, 35.5));
        assertTrue("polsirina NPC-ja se ujame v vrata",
                SquadPlanner.straightWalkable(wall, -0.6, GROUND, 25.5, -0.6, 35.5));
        assertFalse("rob NPC-ja zadene zid ob vratih",
                SquadPlanner.straightWalkable(wall, -0.8, GROUND, 25.5, -0.8, 35.5));
        assertFalse(SquadPlanner.straightWalkable(wall, -5.5, GROUND, 25.5, -5.5, 35.5));
        Terrain step = new Terrain() {
            @Override
            public double standY(double x, double yHint, double z) {
                return z < 10 ? GROUND : GROUND + 1;
            }
        };
        Terrain cliff = new Terrain() {
            @Override
            public double standY(double x, double yHint, double z) {
                return z < 10 ? GROUND : GROUND + 2;
            }
        };
        assertTrue("stopnica", SquadPlanner.straightWalkable(step, 0.5, GROUND, 5.5, 0.5, 15.5));
        assertFalse("dva bloka", SquadPlanner.straightWalkable(cliff, 0.5, GROUND, 5.5, 0.5, 15.5));
    }

    @Test
    public void nearestArcLengthProjectsOntoSegments() {
        PathTrack track = new PathTrack(new double[] {0.5, 0.5, 4.5}, new double[] {GROUND, GROUND, GROUND},
                new double[] {0.5, 10.5, 10.5});
        assertEquals(5.0, track.nearestS(2.0, 5.5), 1.0E-9);
        assertEquals(12.0, track.nearestS(2.5, 13.0), 1.0E-9);
        assertEquals(0.0, track.nearestS(-3.0, -4.0), 1.0E-9);
        assertEquals(14.0, track.nearestS(9.0, 10.5), 1.0E-9);
        assertEquals(10.0, track.pointS(1), 1.0E-9);
        assertEquals(4.5, track.pointX(2), 1.0E-9);
    }
}
