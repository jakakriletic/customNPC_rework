package noppes.npcs.rework.army;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

/** M11.11d: one map flag of an army is chosen by position and split off with its grants. No world. */
public class ArmyFlagTest {
    private static final UUID OWNER = UUID.fromString("0b5a1c2e-1111-4a2b-9c3d-123456789abc");
    private static final UUID P = UUID.fromString("11111111-3333-4c4d-9e5f-000000000001");

    private static List<ArmyClusters.Point> twoGroups() {
        List<ArmyClusters.Point> out = new ArrayList<ArmyClusters.Point>();
        for (int i = 0; i < 10; i++) { out.add(new ArmyClusters.Point(0, i, 64, 0)); }        // around 4.5, 0
        for (int i = 0; i < 6; i++) { out.add(new ArmyClusters.Point(0, 200 + i, 64, 100)); } // around 202.5, 100
        return out;
    }

    @Test public void nearestFlagSelectsOnlyItsMembers() {
        List<Integer> far = ArmyClusters.flag(twoGroups(), 48, 203, 101, 48);
        assertEquals(6, far.size());
        for (int i : far) { assertTrue(i >= 10); }
        List<Integer> near = ArmyClusters.flag(twoGroups(), 48, 5, 0, 48);
        assertEquals(10, near.size());
        assertEquals(new HashSet<Integer>(far).size(), far.size());
    }

    @Test public void aFlagThatMovedAwayIsNotGuessed() {
        assertTrue(ArmyClusters.flag(twoGroups(), 48, 100, 50, 48).isEmpty());
        assertTrue(ArmyClusters.flag(new ArrayList<ArmyClusters.Point>(), 48, 0, 0, 48).isEmpty());
    }

    @Test public void splitNamesAreFreeValidAndBounded() {
        ArmyRegistry reg = new ArmyRegistry();
        reg.create(new Army("North", Army.OwnerType.PLAYER, OWNER.toString(), "", 0));
        assertEquals("North-2", ArmyCommandService.splitName(reg, "North"));
        reg.create(new Army("north-2", Army.OwnerType.PLAYER, OWNER.toString(), "", 0));
        assertEquals("North-3", ArmyCommandService.splitName(reg, "North"));
        String longName = "abcdefghijklmnopqrstuvwxyz012345";
        String split = ArmyCommandService.splitName(reg, longName);
        assertEquals(32, split.length());
        assertTrue(split.endsWith("-2") && Army.validName(split));
    }

    @Test public void splitArmyKeepsSelectedGrantsOnly() {
        ArmyRegistry reg = new ArmyRegistry();
        Army north = new Army("North", Army.OwnerType.PLAYER, OWNER.toString(), "", 0);
        Army part = new Army("North-2", Army.OwnerType.PLAYER, OWNER.toString(), "", 0);
        Army other = new Army("Other", Army.OwnerType.PLAYER, OWNER.toString(), "", 0);
        reg.create(north); reg.create(part); reg.create(other);
        reg.grants().grant(OWNER.toString(), P.toString(), ArmyGrants.Access.COMMAND, north.key());
        assertEquals(ArmyGrants.Access.NONE, reg.grants().access(part, P));
        reg.grants().copySelected(north.key(), part.key());
        assertEquals(ArmyGrants.Access.COMMAND, reg.grants().access(part, P));
        assertEquals(ArmyGrants.Access.NONE, reg.grants().access(other, P));
        reg.remove("North-2");
        assertEquals(ArmyGrants.Access.COMMAND, reg.grants().access(north, P));
    }
}
