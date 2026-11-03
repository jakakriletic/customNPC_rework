package noppes.npcs.rework.army;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import noppes.npcs.rework.formation.SquadOrder;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

/** M11.7a: discover and invoke exactly as a separately compiled, reflection-only bridge will. */
public class ArmyBridgeContractTest {
    private static final String SERVICE = "noppes.npcs.rework.army.ArmyCommandService";
    private static final UUID OWNER = UUID.fromString("11111111-3333-4c4d-9e5f-000000000001");
    private static final UUID VIEWER = UUID.fromString("11111111-3333-4c4d-9e5f-000000000002");

    @After public void switchOff() { ArmyCommandService.setSwitch(0); }

    private Method method(String name, Class<?> result, Class<?>... args) throws Exception {
        Method m = Class.forName(SERVICE).getMethod(name, args);
        assertTrue(Modifier.isStatic(m.getModifiers()));
        assertEquals(result, m.getReturnType());
        return m;
    }

    private Method move() throws Exception {
        return method("submitMoveOrder", ArmyCommandService.Reply.class, MinecraftServer.class, UUID.class,
                String.class, int.class, double.class, double.class, double.class, String.class, double.class, String.class);
    }

    private void reply(Object result, boolean accepted, String text) throws Exception {
        Class<?> type = Class.forName(SERVICE + "$Reply");
        assertEquals(type, result.getClass());
        assertEquals(boolean.class, type.getMethod("isAccepted").getReturnType());
        assertEquals(String.class, type.getMethod("getMessage").getReturnType());
        assertEquals(accepted, type.getMethod("isAccepted").invoke(result));
        assertTrue(((String) type.getMethod("getMessage").invoke(result)).contains(text));
    }

    @Test public void discoverVersionAndExactStaticSignatures() throws Exception {
        assertEquals(1, method("getApiVersion", int.class).invoke(null));
        assertEquals(1, ArmyApi.getApiVersion());
        method("armiesVisibleTo", List.class, MinecraftServer.class, UUID.class, int.class);
        move();
        method("cancelOrder", ArmyCommandService.Reply.class, MinecraftServer.class, UUID.class, String.class);
        method("setOrderEngagement", ArmyCommandService.Reply.class, MinecraftServer.class, UUID.class,
                String.class, String.class);
    }

    @Test public void disabledBridgeInvokesWithNoWorldOrPlayer() throws Exception {
        ArmyCommandService.setSwitch(0);
        assertTrue(((List<?>) method("armiesVisibleTo", List.class, MinecraftServer.class, UUID.class, int.class)
                .invoke(null, null, null, 0)).isEmpty());
        reply(move().invoke(null, null, null, "alpha", 0, 1d, 4d, 2d, "legion", 1d, "passive"), false, "off");
        reply(method("cancelOrder", ArmyCommandService.Reply.class, MinecraftServer.class, UUID.class, String.class)
                .invoke(null, null, null, "alpha"), false, "off");
        reply(method("setOrderEngagement", ArmyCommandService.Reply.class, MinecraftServer.class, UUID.class,
                String.class, String.class).invoke(null, null, null, "alpha", "engage"), false, "off");
    }

    @Test public void missingServerOrIdentityFailsClosedWhenEnabled() throws Exception {
        ArmyCommandService.setSwitch(1);
        assertTrue(ArmyCommandService.armiesVisibleTo(null, OWNER, 0).isEmpty());
        reply(move().invoke(null, null, OWNER, "alpha", 0, 1d, 4d, 2d, "legion", 1d, null), false, "server thread");
        reply(ArmyCommandService.cancelOrder(null, null, "alpha"), false, "server thread");
        reply(ArmyCommandService.setOrderEngagement(null, OWNER, "alpha", "engage"), false, "server thread");
    }

    private ArmyRegistry registry() {
        ArmyRegistry reg = new ArmyRegistry();
        reg.create(new Army("alpha", Army.OwnerType.PLAYER, OWNER.toString(), "Owner", 0));
        reg.create(new Army("hidden", Army.OwnerType.PLAYER, UUID.randomUUID().toString(), "Secret", 0));
        return reg;
    }

    private ArmySnapshot snapshot(ArmyRegistry reg, UUID actor) {
        Map<String, List<ArmyClusters.Point>> points = new HashMap<String, List<ArmyClusters.Point>>();
        points.put("alpha", Arrays.asList(new ArmyClusters.Point(0, 1, 4, 2),
                new ArmyClusters.Point(0, 3, 4, 2), new ArmyClusters.Point(-1, 20, 5, 30)));
        points.put("hidden", Collections.singletonList(new ArmyClusters.Point(0, 10, 4, 10)));
        points.put("orphan", points.get("hidden"));
        return ArmySnapshot.build(0, 123, 48, 8, reg, ArmyActor.player(actor, false), points);
    }

    @Test public void reflectedMarkerGettersContainOnlyJdkValuesAndPrivateFinalState() throws Exception {
        Class<?> type = Class.forName("noppes.npcs.rework.army.ArmyMapMarker");
        for (String getter : new String[] {"getArmyName", "getOwnerName", "getKind", "getOrderState",
                "getOrderMode", "getOrderShape", "getOrderEngagement"}) {
            assertEquals(String.class, type.getMethod(getter).getReturnType());
        }
        for (String getter : new String[] {"getDimension", "getX", "getY", "getZ", "getMemberCount",
                "getLoadedCount", "getOmittedClusters", "getOmittedMembers"}) {
            assertEquals(int.class, type.getMethod(getter).getReturnType());
        }
        assertEquals(long.class, type.getMethod("getLastSeen").getReturnType());
        assertEquals(boolean.class, type.getMethod("isCommandable").getReturnType());
        assertTrue(Modifier.isFinal(type.getModifiers()));
        assertEquals(0, type.getConstructors().length);
        for (Field f : type.getDeclaredFields()) {
            assertTrue(Modifier.isPrivate(f.getModifiers())); assertTrue(Modifier.isFinal(f.getModifiers()));
        }
    }

    @Test public void markersFilterDimensionKeepKindsAndCopyOrder() throws Exception {
        ArmyRegistry reg = registry();
        reg.order("alpha", new ArmyOrder(0, 10, 4, 10, SquadOrder.Shape.KOLONA, 1, 8,
                ArmyOrder.State.MOVING, ArmyOrder.Mode.SWARM, ArmyOrder.Stance.PASSIVE));
        ArmySnapshot view = snapshot(reg, OWNER);
        Object main = ArmyMapMarker.copy(view, reg, 0).get(0);
        Class<?> type = main.getClass();
        assertEquals("alpha", type.getMethod("getArmyName").invoke(main));
        assertEquals("Owner", type.getMethod("getOwnerName").invoke(main));
        assertEquals("main", type.getMethod("getKind").invoke(main));
        assertEquals(2, type.getMethod("getX").invoke(main));
        assertEquals(4, type.getMethod("getY").invoke(main));
        assertEquals(2, type.getMethod("getZ").invoke(main));
        assertEquals(2, type.getMethod("getMemberCount").invoke(main));
        assertEquals(3, type.getMethod("getLoadedCount").invoke(main));
        assertEquals(true, type.getMethod("isCommandable").invoke(main));
        assertEquals("MOVING", type.getMethod("getOrderState").invoke(main));
        assertEquals("SWARM", type.getMethod("getOrderMode").invoke(main));
        assertEquals("KOLONA", type.getMethod("getOrderShape").invoke(main));
        assertEquals("PASSIVE", type.getMethod("getOrderEngagement").invoke(main));
        ArmyMapMarker other = ArmyMapMarker.copy(view, reg, -1).get(0);
        assertEquals(-1, other.getDimension()); assertEquals("straggler", other.getKind());
        assertEquals(1, other.getMemberCount()); assertEquals(1, ArmyMapMarker.copy(view, reg, 0).size());
        assertTrue(ArmyMapMarker.copy(view, reg, 7).isEmpty());
        reg.order("alpha", reg.order("alpha").withState(ArmyOrder.State.CANCELLED));
        assertEquals("MOVING", type.getMethod("getOrderState").invoke(main));
    }

    @Test public void freshRightsHideOrDowngradeMarkersAfterRevoke() {
        ArmyRegistry reg = registry();
        assertTrue(ArmyMapMarker.copy(snapshot(reg, VIEWER), reg, 0).isEmpty());
        reg.grants().grant(OWNER.toString(), VIEWER.toString(), ArmyGrants.Access.COMMAND, "alpha");
        assertTrue(ArmyMapMarker.copy(snapshot(reg, VIEWER), reg, 0).get(0).isCommandable());
        reg.grants().revoke(OWNER.toString(), VIEWER.toString(), ArmyGrants.Access.COMMAND, "alpha");
        reg.grants().grant(OWNER.toString(), VIEWER.toString(), ArmyGrants.Access.VIEW, "alpha");
        assertFalse(ArmyMapMarker.copy(snapshot(reg, VIEWER), reg, 0).get(0).isCommandable());
        reg.grants().revoke(OWNER.toString(), VIEWER.toString(), null, "alpha");
        assertTrue(ArmyMapMarker.copy(snapshot(reg, VIEWER), reg, 0).isEmpty());
    }

    @Test public void lastKnownIsOnlyFallbackAndHasNoInventedMembers() {
        ArmyRegistry reg = registry();
        assertTrue(ArmyMapMarker.copy(ArmySnapshot.build(0, 1, 48, 8, reg, ArmyActor.player(OWNER, false),
                Collections.emptyMap()), reg, 0).isEmpty());
        snapshot(reg, OWNER); // note the main cluster at 2,4,2
        ArmySnapshot empty = ArmySnapshot.build(0, 200, 48, 8, reg, ArmyActor.player(OWNER, false), Collections.emptyMap());
        ArmyMapMarker marker = ArmyMapMarker.copy(empty, reg, 0).get(0);
        assertEquals("last_known", marker.getKind()); assertEquals(0, marker.getMemberCount());
        assertEquals(0, marker.getLoadedCount()); assertEquals(123L, marker.getLastSeen());
        assertEquals(2, marker.getX()); assertEquals("NONE", marker.getOrderState());
        assertEquals("", marker.getOrderMode()); assertEquals("", marker.getOrderShape());
        assertEquals("", marker.getOrderEngagement()); assertTrue(ArmyMapMarker.copy(empty, reg, -1).isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class) public void markerListCannotBeChanged() {
        ArmyRegistry reg = registry();
        ArmyMapMarker.copy(snapshot(reg, OWNER), reg, 0).clear();
    }

    @Test public void omittedCountsSurviveTheCopyWithoutExtraMarkers() {
        ArmyRegistry reg = registry();
        ArmySnapshot.Entry e = new ArmySnapshot.Entry("alpha", "Owner", true, 9, 2, 7,
                Collections.singletonList(new ArmyClusters.Cluster(ArmyClusters.Kind.MAIN, 0, 1, 4, 2, 2)),
                0, 1, 4, 2, 10);
        List<ArmyMapMarker> out = ArmyMapMarker.copy(new ArmySnapshot(0, 10, 48, 8,
                Collections.singletonList(e), false), reg, 0);
        assertEquals(1, out.size()); assertEquals(2, out.get(0).getOmittedClusters());
        assertEquals(7, out.get(0).getOmittedMembers()); assertEquals(9, out.get(0).getLoadedCount());
    }

    @Test public void acceptedAndRefusedRepliesHaveTheSameReflectiveShape() throws Exception {
        reply(new ArmyCommandService.Reply(true, "accepted"), true, "RWARMY accepted");
        reply(new ArmyCommandService.Reply(false, "unknown army alpha"), false, "RWARMY unknown army alpha");
    }
}
