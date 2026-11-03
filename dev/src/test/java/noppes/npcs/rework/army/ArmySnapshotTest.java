package noppes.npcs.rework.army;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

/** M11.2: clusters, the build of a snapshot from the rights of one actor, the codec and the last known position. */
public class ArmySnapshotTest {
    private static final UUID P1 = UUID.fromString("0b5a1c2e-1111-4a2b-9c3d-123456789abc");
    private static final UUID P2 = UUID.fromString("7f000001-2222-4b3c-8d4e-abcdefabcdef");

    @After
    public void switchOff() {
        ArmyCommandService.setSwitch(0);
    }

    private static ArmyClusters.Point p(double x, double z) {
        return new ArmyClusters.Point(0, x, 4, z);
    }

    private static List<ArmyClusters.Cluster> cluster(int radius, int min, ArmyClusters.Point... points) {
        return ArmyClusters.cluster(new ArrayList<ArmyClusters.Point>(Arrays.asList(points)), radius, min);
    }

    private static List<ArmyClusters.Point> row(int count, double x, double z, double step) {
        List<ArmyClusters.Point> out = new ArrayList<ArmyClusters.Point>();
        for (int i = 0; i < count; i++) {
            out.add(p(x + i * step, z));
        }
        return out;
    }

    @Test
    public void noMembersNoCluster() {
        assertTrue(cluster(48, 8).isEmpty());
    }

    @Test
    public void radiusIsInclusiveAndHorizontal() {
        assertEquals(1, cluster(48, 1, p(0, 0), p(48, 0)).size());
        assertEquals(2, cluster(48, 1, p(0, 0), p(48.01, 0)).size());
        assertEquals(2, cluster(48, 1, p(0, 0), p(34, 34)).size());
        assertEquals(1, cluster(48, 1, new ArmyClusters.Point(0, 0, 0, 0), new ArmyClusters.Point(0, 0, 200, 0)).size());
    }

    @Test
    public void columnLongerThanTheRadiusStaysOneCluster() {
        List<ArmyClusters.Cluster> c = ArmyClusters.cluster(row(40, 0, 0, 10), 48, 8);
        assertEquals(1, c.size());
        assertEquals(40, c.get(0).size);
        assertEquals(195, c.get(0).x);
    }

    /** All pairs against the grid, including members two cells apart and negative positions. */
    @Test
    public void sameComponentsAsAllPairs() {
        Random random = new Random(11);
        for (int round = 0; round < 200; round++) {
            int radius = 1 + random.nextInt(60);
            List<ArmyClusters.Point> points = new ArrayList<ArmyClusters.Point>();
            int n = 1 + random.nextInt(60);
            for (int i = 0; i < n; i++) {
                points.add(p(random.nextDouble() * 600 - 300, random.nextDouble() * 600 - 300));
            }
            List<Integer> expected = componentSizes(points, radius);
            List<Integer> actual = new ArrayList<Integer>();
            for (ArmyClusters.Cluster c : ArmyClusters.cluster(points, radius, 1)) {
                actual.add(c.size);
            }
            assertEquals("round " + round, expected, actual);
        }
    }

    private static List<Integer> componentSizes(List<ArmyClusters.Point> points, int radius) {
        int n = points.size();
        int[] group = new int[n];
        for (int i = 0; i < n; i++) {
            group[i] = i;
        }
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double dx = points.get(i).x - points.get(j).x, dz = points.get(i).z - points.get(j).z;
                if (dx * dx + dz * dz <= (double) radius * radius && group[i] != group[j]) {
                    int from = group[j], to = group[i];
                    for (int k = 0; k < n; k++) {
                        if (group[k] == from) {
                            group[k] = to;
                        }
                    }
                }
            }
        }
        Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
        for (int g : group) {
            counts.put(g, counts.containsKey(g) ? counts.get(g) + 1 : 1);
        }
        List<Integer> sizes = new ArrayList<Integer>(counts.values());
        Collections.sort(sizes, Collections.reverseOrder());
        return sizes;
    }

    @Test
    public void kindsFollowSizeAndThreshold() {
        List<ArmyClusters.Point> points = row(9, 0, 0, 1);
        points.addAll(row(8, 1000, 0, 1));
        points.addAll(row(7, 2000, 0, 1));
        List<ArmyClusters.Cluster> c = ArmyClusters.cluster(points, 48, 8);
        assertEquals(ArmyClusters.Kind.MAIN, c.get(0).kind);
        assertEquals(9, c.get(0).size);
        assertEquals(ArmyClusters.Kind.GROUP, c.get(1).kind);
        assertEquals(8, c.get(1).size);
        assertEquals(ArmyClusters.Kind.STRAGGLER, c.get(2).kind);
        assertEquals(7, c.get(2).size);
    }

    @Test
    public void largestIsMainEvenBelowTheThreshold() {
        List<ArmyClusters.Cluster> c = cluster(48, 20, p(0, 0), p(1, 0), p(500, 0));
        assertEquals(ArmyClusters.Kind.MAIN, c.get(0).kind);
        assertEquals(2, c.get(0).size);
        assertEquals(ArmyClusters.Kind.STRAGGLER, c.get(1).kind);
    }

    @Test
    public void dimensionsNeverMix() {
        List<ArmyClusters.Cluster> c = cluster(48, 1, new ArmyClusters.Point(-1, 5, 4, 5), new ArmyClusters.Point(0, 5, 4, 5));
        assertEquals(2, c.size());
        assertEquals("the same size goes by dimension", -1, c.get(0).dimension);
    }

    @Test
    public void orderDoesNotDependOnInput() {
        List<ArmyClusters.Point> points = row(5, 0, 0, 2);
        points.addAll(row(5, 300, 0, 2));
        points.addAll(row(3, -900, 7, 2));
        String first = text(ArmyClusters.cluster(points, 48, 4));
        for (long seed = 0; seed < 20; seed++) {
            Collections.shuffle(points, new Random(seed));
            assertEquals(first, text(ArmyClusters.cluster(points, 48, 4)));
        }
    }

    private static String text(List<ArmyClusters.Cluster> clusters) {
        StringBuilder b = new StringBuilder();
        for (ArmyClusters.Cluster c : clusters) {
            b.append(c.kind).append(c.size).append('@').append(c.x).append(',').append(c.z).append(';');
        }
        return b.toString();
    }

    @Test
    public void settingsAreClamped() {
        assertEquals(1, ArmyClusters.clampRadius(0));
        assertEquals(ArmyClusters.MAX_RADIUS, ArmyClusters.clampRadius(100000));
        assertEquals(1, ArmyClusters.clampMinMembers(-3));
        assertEquals(ArmyClusters.MAX_MIN_MEMBERS, ArmyClusters.clampMinMembers(100000));
    }

    private static ArmyRegistry registry() {
        ArmyRegistry r = new ArmyRegistry();
        r.create(new Army("North", Army.OwnerType.PLAYER, P1.toString(), "owner", 1));
        r.create(new Army("south", Army.OwnerType.PLAYER, P1.toString(), "", 2));
        r.create(new Army("guard", Army.OwnerType.FACTION, "3", "", 3));
        return r;
    }

    private static Map<String, List<ArmyClusters.Point>> members() {
        Map<String, List<ArmyClusters.Point>> byKey = new HashMap<String, List<ArmyClusters.Point>>();
        byKey.put("north", row(9, 100, 0, 1));
        byKey.get("north").addAll(row(2, 400, 0, 1));
        byKey.put("south", row(3, -50, 0, 1));
        byKey.put("orphan", row(5, 0, 0, 1));
        return byKey;
    }

    private static ArmySnapshot build(ArmyRegistry r, ArmyActor actor, Map<String, List<ArmyClusters.Point>> byKey) {
        return ArmySnapshot.build(7, 1000, 48, 8, r, actor, byKey);
    }

    @Test
    public void operatorSeesAllArmiesAndNoOrphan() {
        ArmySnapshot s = build(registry(), ArmyActor.SYSTEM, members());
        assertEquals(3, s.armies.size());
        assertEquals("guard", s.armies.get(0).name);
        assertNull(s.entry("orphan"));
        ArmySnapshot.Entry north = s.entry("north");
        assertEquals("North", north.name);
        assertEquals("owner", north.ownerName);
        assertTrue(north.command);
        assertEquals(11, north.loaded);
        assertEquals(2, north.clusters.size());
        assertEquals(0, s.entry("guard").loaded);
    }

    @Test
    public void snapshotFollowsTheRightsOfTheActor() {
        ArmyRegistry r = registry();
        ArmyActor stranger = ArmyActor.player(P2, false);
        assertTrue(build(r, stranger, members()).armies.isEmpty());
        assertEquals(ArmyGrants.Result.GRANTED, r.grants().grant(ArmyGrants.SERVER, P2.toString(), ArmyGrants.Access.VIEW, "north"));
        ArmySnapshot view = build(r, stranger, members());
        assertEquals(1, view.armies.size());
        assertFalse(view.entry("north").command);
        assertEquals(ArmyGrants.Result.GRANTED, r.grants().grant(ArmyGrants.SERVER, P2.toString(), ArmyGrants.Access.COMMAND, "north"));
        assertTrue(build(r, stranger, members()).entry("north").command);
        r.grants().revoke(null, P2.toString(), null, "north");
        assertTrue("a revoke applies to the next snapshot", build(r, stranger, members()).armies.isEmpty());
        ArmySnapshot owner = build(r, ArmyActor.player(P1, false), members());
        assertEquals(2, owner.armies.size());
        assertNull("a faction army is not the owner's", owner.entry("guard"));
    }

    @Test
    public void lastPositionFollowsTheMainClusterOnlyAfterARealMove() {
        ArmyRegistry r = registry();
        Map<String, List<ArmyClusters.Point>> byKey = members();
        ArmySnapshot first = build(r, ArmyActor.SYSTEM, byKey);
        assertTrue("a first position is a change", first.lastMoved);
        assertEquals(104, first.entry("north").lastX);
        assertEquals(1000, first.entry("north").lastSeen);
        assertFalse("no members, no position", first.entry("guard").hasLast());

        ArmySnapshot still = ArmySnapshot.build(8, 1500, 48, 8, r, ArmyActor.SYSTEM, byKey);
        assertFalse("a standing army makes nothing dirty", still.lastMoved);
        assertEquals(1500, still.entry("north").lastSeen);

        byKey.put("north", row(9, 115, 0, 1));
        ArmySnapshot small = ArmySnapshot.build(9, 1600, 48, 8, r, ArmyActor.SYSTEM, byKey);
        assertFalse(small.lastMoved);
        assertEquals("under the limit the position stays", 104, small.entry("north").lastX);

        byKey.put("north", row(9, 116, 0, 1));
        ArmySnapshot moved = ArmySnapshot.build(10, 1700, 48, 8, r, ArmyActor.SYSTEM, byKey);
        assertTrue(moved.lastMoved);
        assertEquals(120, moved.entry("north").lastX);

        byKey.remove("north");
        ArmySnapshot gone = ArmySnapshot.build(11, 1800, 48, 8, r, ArmyActor.SYSTEM, byKey);
        assertFalse(gone.lastMoved);
        ArmySnapshot.Entry north = gone.entry("north");
        assertEquals(0, north.loaded);
        assertTrue(north.clusters.isEmpty());
        assertEquals("unloaded members keep the last position", 120, north.lastX);
        assertEquals(1700, north.lastSeen);
    }

    @Test
    public void lastPositionSurvivesTheRegisterRoundTrip() {
        ArmyRegistry r = registry();
        build(r, ArmyActor.SYSTEM, members());
        Army before = r.get("north");
        NBTTagCompound nbt = r.write(new NBTTagCompound());
        ArmyRegistry loaded = new ArmyRegistry();
        loaded.read(nbt);
        assertEquals(0, loaded.skippedOnRead());
        assertEquals(before, loaded.get("north"));
        assertEquals(104, loaded.get("north").lastX());
        assertEquals(1000, loaded.get("north").lastSeen());
        assertEquals(-1, loaded.get("guard").lastSeen());
    }

    @Test
    public void brokenLastPositionKeepsTheArmy() {
        ArmyRegistry r = registry();
        build(r, ArmyActor.SYSTEM, members());
        NBTTagCompound nbt = r.write(new NBTTagCompound());
        for (int i = 0; i < nbt.getTagList("Armies", 10).tagCount(); i++) {
            nbt.getTagList("Armies", 10).getCompoundTagAt(i).setIntArray("Last", new int[] {1, 2});
        }
        ArmyRegistry loaded = new ArmyRegistry();
        loaded.read(nbt);
        assertEquals(0, loaded.skippedOnRead());
        assertEquals(3, loaded.size());
        assertEquals(-1, loaded.get("north").lastSeen());
    }

    @Test
    public void registerWithoutPositionsStillLoads() {
        NBTTagCompound old = new NBTTagCompound();
        old.setInteger("Version", 1);
        old.setTag("Armies", registry().write(new NBTTagCompound()).getTagList("Armies", 10));
        ArmyRegistry loaded = new ArmyRegistry();
        loaded.read(old);
        assertEquals(3, loaded.size());
        assertEquals(0, loaded.skippedOnRead());
    }

    @Test
    public void onlyTheLargestClustersAreSentTheRestCounted() {
        ArmyRegistry r = registry();
        List<ArmyClusters.Point> many = new ArrayList<ArmyClusters.Point>();
        for (int i = 0; i < 20; i++) {
            many.add(p(i * 1000, 0));
        }
        many.addAll(row(4, 500, 500, 1));
        Map<String, List<ArmyClusters.Point>> byKey = new HashMap<String, List<ArmyClusters.Point>>();
        byKey.put("north", many);
        ArmySnapshot.Entry e = build(r, ArmyActor.SYSTEM, byKey).entry("north");
        assertEquals(24, e.loaded);
        assertEquals(ArmySnapshot.MAX_CLUSTERS, e.clusters.size());
        assertEquals(4, e.clusters.get(0).size);
        assertEquals(21 - ArmySnapshot.MAX_CLUSTERS, e.omittedClusters);
        assertEquals(5, e.omittedMembers);
        assertNotNull(ArmySnapshot.decode(build(r, ArmyActor.SYSTEM, byKey).encode()));
    }

    @Test
    public void codecRoundTrip() {
        ArmySnapshot s = build(registry(), ArmyActor.SYSTEM, members());
        ArmySnapshot d = ArmySnapshot.decode(s.encode());
        assertNotNull(d);
        assertEquals(7, d.requestId);
        assertEquals(1000, d.worldTime);
        assertEquals(48, d.radius);
        assertEquals(8, d.minMembers);
        assertEquals(3, d.armies.size());
        ArmySnapshot.Entry north = d.entry("north");
        assertEquals("North", north.name);
        assertEquals("owner", north.ownerName);
        assertTrue(north.command);
        assertEquals(11, north.loaded);
        assertEquals(104, north.lastX);
        assertEquals(1000, north.lastSeen);
        assertEquals(ArmyClusters.Kind.STRAGGLER, north.clusters.get(1).kind);
        assertEquals(2, north.clusters.get(1).size);
        assertFalse(d.entry("guard").hasLast());
        assertArrayEquals(s.encode(), d.encode());
    }

    /** The largest register with the largest entries fits one message. */
    @Test
    public void fullRegisterStaysBelowTheLimit() {
        ArmyRegistry r = new ArmyRegistry();
        Map<String, List<ArmyClusters.Point>> byKey = new HashMap<String, List<ArmyClusters.Point>>();
        StringBuilder longOwner = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longOwner.append('ž');
        }
        for (int i = 0; i < ArmyRegistry.MAX_ARMIES; i++) {
            String name = String.format("army-%027d", i);
            assertEquals(ArmyRegistry.Result.CREATED, r.create(new Army(name, Army.OwnerType.FACTION, "1", longOwner.toString(), 0)));
            List<ArmyClusters.Point> points = new ArrayList<ArmyClusters.Point>();
            for (int c = 0; c < 40; c++) {
                points.add(p(c * 10000, 0));
            }
            byKey.put(Army.key(name), points);
        }
        byte[] bytes = ArmySnapshot.build(1, 1, 48, 8, r, ArmyActor.SYSTEM, byKey).encode();
        assertTrue("size " + bytes.length, bytes.length < ArmySnapshot.MAX_MESSAGE);
        ArmySnapshot d = ArmySnapshot.decode(bytes);
        assertEquals(ArmyRegistry.MAX_ARMIES, d.armies.size());
        assertEquals(ArmySnapshot.MAX_OWNER_NAME, d.armies.get(0).ownerName.length());
    }

    @Test
    public void malformedSnapshotsAreRefused() {
        byte[] good = build(registry(), ArmyActor.SYSTEM, members()).encode();
        assertNull(ArmySnapshot.decode(null));
        assertNull(ArmySnapshot.decode(new byte[0]));
        assertNull(ArmySnapshot.decode(new byte[] {1}));
        assertNull(ArmySnapshot.decode(new byte[ArmySnapshot.MAX_MESSAGE + 1]));
        byte[] version = good.clone();
        version[0] = 9;
        assertNull(ArmySnapshot.decode(version));
        byte[] kind = good.clone();
        kind[1] = ArmySnapshot.KIND_REQUEST;
        assertNull(ArmySnapshot.decode(kind));
        assertNull("leftover bytes", ArmySnapshot.decode(Arrays.copyOf(good, good.length + 1)));
        for (int length = 0; length < good.length; length++) {
            assertNull("cut at " + length, ArmySnapshot.decode(Arrays.copyOf(good, length)));
        }
        Random random = new Random(5);
        for (int i = 0; i < 2000; i++) {
            byte[] noise = good.clone();
            noise[random.nextInt(noise.length)] = (byte) random.nextInt(256);
            ArmySnapshot.decode(noise); // never throws
        }
    }

    @Test
    public void requestCodec() {
        assertEquals(42, ArmySnapshot.decodeRequest(ArmySnapshot.encodeRequest(42)));
        assertEquals(-1, ArmySnapshot.decodeRequest(null));
        assertEquals(-1, ArmySnapshot.decodeRequest(new byte[] {9, 9, 9}));
        assertEquals(-1, ArmySnapshot.decodeRequest(new byte[ArmySnapshot.REQUEST_SIZE]));
        assertEquals(-1, ArmySnapshot.decodeRequest(ArmySnapshot.encodeRequest(-1)));
        byte[] kind = ArmySnapshot.encodeRequest(1);
        kind[1] = ArmySnapshot.KIND_SNAPSHOT;
        assertEquals(-1, ArmySnapshot.decodeRequest(kind));
    }

    @Test
    public void nothingAtZero() {
        ArmyCommandService.setSwitch(0);
        assertNull(ArmyCommandService.snapshot(null, ArmyActor.SYSTEM, 1));
        List<String> lines = ArmyCommandService.describeClusters(null, ArmyActor.SYSTEM, null);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("off"));
    }
}
