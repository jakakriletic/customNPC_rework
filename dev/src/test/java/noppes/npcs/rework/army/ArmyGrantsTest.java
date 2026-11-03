package noppes.npcs.rework.army;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import noppes.npcs.rework.army.ArmyGrants.Access;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/** M11.1b (research 3.5a): the rights matrix, its NBT round trip and the switch at 0. */
public class ArmyGrantsTest {
    private static final UUID OWNER = UUID.fromString("0b5a1c2e-1111-4a2b-9c3d-123456789abc");
    private static final UUID OTHER = UUID.fromString("7f000001-2222-4b3c-8d4e-abcdefabcdef");
    private static final UUID P = UUID.fromString("11111111-3333-4c4d-9e5f-000000000001");
    private static final UUID Q = UUID.fromString("11111111-3333-4c4d-9e5f-000000000002");

    private ArmyRegistry reg;
    private Army north;
    private Army south;
    private Army west;
    private Army guard;

    @Before
    public void armies() {
        reg = new ArmyRegistry();
        north = add("North", Army.OwnerType.PLAYER, OWNER.toString());
        south = add("south", Army.OwnerType.PLAYER, OWNER.toString());
        west = add("west", Army.OwnerType.PLAYER, OTHER.toString());
        guard = add("guard", Army.OwnerType.FACTION, "0");
    }

    @After
    public void switchOff() {
        ArmyCommandService.setSwitch(0);
    }

    private Army add(String name, Army.OwnerType type, String owner) {
        Army army = new Army(name, type, owner, "", 0L);
        assertSame(ArmyRegistry.Result.CREATED, reg.create(army));
        return army;
    }

    private Access of(Army army, UUID player) {
        return reg.access(army, ArmyActor.player(player, false));
    }

    private void grant(String grantor, UUID grantee, Access kind, Army army) {
        assertSame(ArmyGrants.Result.GRANTED,
                reg.grants().grant(grantor, grantee.toString(), kind, army == null ? null : army.key()));
    }

    @Test
    public void ownerAndOperatorHaveEverythingOthersNothing() {
        assertSame(Access.COMMAND, of(north, OWNER));
        assertSame(Access.NONE, of(west, OWNER));
        assertSame(Access.NONE, of(guard, OWNER));
        assertSame(Access.NONE, of(north, P));
        ArmyActor op = ArmyActor.player(P, true);
        for (Army a : new Army[] {north, south, west, guard}) {
            assertSame(Access.COMMAND, reg.access(a, op));
            assertSame(Access.COMMAND, reg.access(a, ArmyActor.SYSTEM));
            assertSame(Access.NONE, reg.access(a, ArmyActor.NOBODY));
        }
    }

    @Test
    public void selectedGrantCoversOnlyThatArmyAndCommandIncludesView() {
        grant(OWNER.toString(), P, Access.VIEW, north);
        grant(OWNER.toString(), P, Access.COMMAND, south);
        assertSame(Access.VIEW, of(north, P));
        assertFalse(of(north, P).canCommand());
        assertSame(Access.COMMAND, of(south, P));
        assertTrue(of(south, P).canView());
        assertSame(Access.NONE, of(west, P));
        assertSame(Access.NONE, of(north, Q));
        assertEquals(2, reg.visible(ArmyActor.player(P, false)).size());
    }

    @Test
    public void ownerAllCoversOwnArmiesAlsoLaterOnesNeverForeign() {
        grant(OWNER.toString(), P, Access.VIEW, null);
        assertSame(Access.VIEW, of(north, P));
        assertSame(Access.NONE, of(west, P));
        assertSame(Access.NONE, of(guard, P));
        Army later = add("later", Army.OwnerType.PLAYER, OWNER.toString());
        assertSame(Access.VIEW, of(later, P));
        Army foreign = add("foreign", Army.OwnerType.PLAYER, OTHER.toString());
        assertSame(Access.NONE, of(foreign, P));
    }

    @Test
    public void serverAllCoversEveryArmyAlsoLaterAndFactionOnes() {
        grant(ArmyGrants.SERVER, P, Access.COMMAND, null);
        for (Army a : new Army[] {north, west, guard, add("later", Army.OwnerType.FACTION, "1")}) {
            assertSame(a.name(), Access.COMMAND, of(a, P));
        }
    }

    @Test
    public void strongestGrantOfAnyGrantorWins() {
        grant(OWNER.toString(), P, Access.VIEW, north);
        grant(ArmyGrants.SERVER, P, Access.COMMAND, north);
        assertSame(Access.COMMAND, of(north, P));
        reg.grants().revoke(ArmyGrants.SERVER, P.toString(), null, north.key());
        assertSame(Access.VIEW, of(north, P));
    }

    @Test
    public void repeatedGrantIsAlready() {
        grant(OWNER.toString(), P, Access.VIEW, north);
        assertSame(ArmyGrants.Result.ALREADY, reg.grants().grant(OWNER.toString(), P.toString(), Access.VIEW, north.key()));
        grant(OWNER.toString(), P, Access.VIEW, null);
        assertSame(ArmyGrants.Result.ALREADY, reg.grants().grant(OWNER.toString(), P.toString(), Access.VIEW, null));
        assertEquals(1, reg.grants().size());
    }

    @Test
    public void revokeAppliesToTheNextCheckAndOnlyToThatKind() {
        grant(OWNER.toString(), P, Access.VIEW, north);
        grant(OWNER.toString(), P, Access.COMMAND, north);
        assertEquals(1, reg.grants().revoke(OWNER.toString(), P.toString(), Access.COMMAND, north.key()));
        assertSame(Access.VIEW, of(north, P));
        assertEquals(0, reg.grants().revoke(OWNER.toString(), P.toString(), Access.COMMAND, north.key()));
        assertEquals(1, reg.grants().revoke(OWNER.toString(), P.toString(), Access.VIEW, north.key()));
        assertSame(Access.NONE, of(north, P));
        assertEquals("an empty record is gone", 0, reg.grants().size());
    }

    @Test
    public void revokeAllClearsTheFlagAndTheSelectedArmies() {
        grant(OWNER.toString(), P, Access.VIEW, null);
        grant(OWNER.toString(), P, Access.VIEW, north);
        grant(OWNER.toString(), P, Access.COMMAND, south);
        assertEquals(2, reg.grants().revoke(OWNER.toString(), P.toString(), Access.VIEW, null));
        assertSame(Access.NONE, of(north, P));
        assertSame(Access.COMMAND, of(south, P));
        assertEquals(1, reg.grants().revoke(OWNER.toString(), P.toString(), null, null));
        assertEquals(0, reg.grants().size());
    }

    @Test
    public void grantorRevokesOnlyOwnRecordOperatorEveryRecord() {
        grant(OWNER.toString(), P, Access.VIEW, north);
        grant(ArmyGrants.SERVER, P, Access.VIEW, north);
        assertEquals(0, reg.grants().revoke(OTHER.toString(), P.toString(), null, null));
        assertEquals(1, reg.grants().revoke(OWNER.toString(), P.toString(), null, null));
        assertSame("the operator's grant stays", Access.VIEW, of(north, P));
        grant(OWNER.toString(), P, Access.VIEW, north);
        assertEquals(2, reg.grants().revoke(null, P.toString(), null, null));
        assertSame(Access.NONE, of(north, P));
    }

    @Test
    public void removedArmyTakesSelectedGrantsNotAllGrants() {
        grant(OWNER.toString(), P, Access.COMMAND, north);
        grant(ArmyGrants.SERVER, Q, Access.VIEW, north);
        grant(OWNER.toString(), Q, Access.VIEW, null);
        reg.remove("NORTH");
        assertEquals(1, reg.grants().size());
        Army again = add("north", Army.OwnerType.PLAYER, OWNER.toString());
        assertSame("a new army of that name has no old grants", Access.NONE, of(again, P));
        assertSame("all covers the new army", Access.VIEW, of(again, Q));
    }

    @Test
    public void foreignRecordDoesNotApplyToAnArmyOfAnotherOwner() {
        assertSame(ArmyGrants.Result.GRANTED, reg.grants().grant(OTHER.toString(), P.toString(), Access.COMMAND, north.key()));
        assertSame(Access.NONE, of(north, P));
    }

    @Test
    public void grantsAreBounded() {
        for (int i = 0; i < ArmyGrants.MAX_GRANTS; i++) {
            assertSame(ArmyGrants.Result.GRANTED,
                    reg.grants().grant(OWNER.toString(), new UUID(1L, i).toString(), Access.VIEW, north.key()));
        }
        assertSame(ArmyGrants.Result.FULL, reg.grants().grant(OWNER.toString(), P.toString(), Access.VIEW, north.key()));
        assertSame(ArmyGrants.Result.GRANTED,
                reg.grants().grant(OWNER.toString(), new UUID(1L, 0).toString(), Access.VIEW, south.key()));
    }

    @Test
    public void grantsSurviveNbtRoundTripThroughBytes() throws Exception {
        grant(OWNER.toString(), P, Access.VIEW, north);
        grant(OWNER.toString(), P, Access.COMMAND, null);
        grant(ArmyGrants.SERVER, P, Access.VIEW, null);
        grant(ArmyGrants.SERVER, Q, Access.COMMAND, guard);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(reg.write(new NBTTagCompound()), bytes);
        ArmyRegistry back = new ArmyRegistry();
        back.read(CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes.toByteArray())));
        assertEquals(0, back.skippedOnRead());
        assertEquals(3, back.grants().size());
        assertEquals(reg.write(new NBTTagCompound()), back.write(new NBTTagCompound()));
        assertSame(Access.COMMAND, back.access(back.get("south"), ArmyActor.player(P, false)));
        assertSame(Access.VIEW, back.access(back.get("west"), ArmyActor.player(P, false)));
        assertSame(Access.COMMAND, back.access(back.get("guard"), ArmyActor.player(Q, false)));
        assertSame(Access.NONE, back.access(back.get("north"), ArmyActor.player(Q, false)));
    }

    @Test
    public void registerWithoutGrantsKeepsTheM111Shape() {
        assertFalse(reg.write(new NBTTagCompound()).hasKey("Grants"));
        ArmyRegistry back = new ArmyRegistry();
        back.read(reg.write(new NBTTagCompound()));
        assertEquals(0, back.skippedOnRead());
        assertEquals(0, back.grants().size());
    }

    @Test
    public void invalidGrantEntriesAreSkippedAndCountedNotThrown() {
        NBTTagCompound data = reg.write(new NBTTagCompound());
        NBTTagList list = new NBTTagList();
        list.appendTag(record("nobody", P.toString(), "View", north.key()));
        list.appendTag(record(OWNER.toString(), "nope", "View", north.key()));
        list.appendTag(record(OWNER.toString(), P.toString(), "View", "gone"));
        // West belongs to OTHER: a hand edited record of OWNER gets nothing from it.
        list.appendTag(record(OWNER.toString(), Q.toString(), "Command", west.key()));
        list.appendTag(record(ArmyGrants.SERVER, Q.toString(), "View", "NORTH"));
        data.setTag("Grants", list);
        ArmyRegistry back = new ArmyRegistry();
        back.read(data);
        assertEquals(4, back.skippedOnRead());
        assertEquals(1, back.grants().size());
        assertSame(Access.VIEW, back.access(back.get("north"), ArmyActor.player(Q, false)));
        assertSame(Access.NONE, back.access(back.get("west"), ArmyActor.player(Q, false)));
    }

    private static NBTTagCompound record(String grantor, String grantee, String key, String army) {
        NBTTagCompound c = new NBTTagCompound();
        c.setString("Grantor", grantor);
        c.setString("Grantee", grantee);
        NBTTagList armies = new NBTTagList();
        armies.appendTag(new NBTTagString(army));
        c.setTag(key, armies);
        return c;
    }

    @Test
    public void commandIsOpenToNonOperatorsAndRightsComeFromTheSender() {
        CommandRwArmy command = new CommandRwArmy();
        // A vanilla player who is not an operator fails canUseCommand at every level.
        assertTrue(command.checkPermission(null, new Sender(-1)));
        assertSame(ArmyActor.NOBODY, CommandRwArmy.actor(new Sender(-1)));
        assertSame(ArmyActor.SYSTEM, CommandRwArmy.actor(new Sender(2)));
        assertSame(ArmyActor.NOBODY, CommandRwArmy.actor(new Sender(1)));
    }

    @Test
    public void switchOffChangesNothingAndNeedsNoServer() {
        ArmyCommandService.setSwitch(0);
        assertFalse(ArmyCommandService.grant(null, ArmyActor.SYSTEM, "uuid:" + P, "view", "north").ok);
        assertFalse(ArmyCommandService.revoke(null, ArmyActor.SYSTEM, "uuid:" + P, "view", "north").ok);
        assertSame(Access.NONE, ArmyCommandService.access(null, ArmyActor.SYSTEM, "north"));
        assertTrue(ArmyCommandService.candidates(null, ArmyActor.SYSTEM, "").isEmpty());
        assertFalse(ArmyApi.canView("uuid:" + P, "north"));
        assertFalse(ArmyApi.canCommand("uuid:" + P, "north"));
        assertEquals(0, ArmyApi.visibleArmies("uuid:" + P).length);
        assertFalse(ArmyApi.grant("uuid:" + P, "view", "north"));
        assertFalse(ArmyApi.revoke("uuid:" + P, "view", "north"));
    }

    @Test
    public void switchOnValidatesBeforeTouchingTheWorld() {
        ArmyCommandService.setSwitch(1);
        ArmyActor player = ArmyActor.player(P, false);
        assertEquals("RWARMY create needs an operator", ArmyCommandService.create(null, player, "x", "faction:0").message);
        assertTrue(ArmyCommandService.create(null, "all", "faction:0").message.contains("invalid name"));
        assertTrue(ArmyCommandService.create(null, "ALL", "faction:0").message.contains("invalid name"));
        assertTrue(ArmyCommandService.grant(null, player, "uuid:" + Q, "own", "x").message.contains("invalid right"));
        assertTrue(ArmyCommandService.revoke(null, player, "uuid:" + Q, "own", "x").message.contains("invalid right"));
        assertTrue(ArmyCommandService.candidates(null, player, "").isEmpty());
    }

    @Test
    public void playerSpecsWithoutAServer() {
        assertEquals(P, ArmyCommandService.resolvePlayer(null, "uuid:" + P.toString().toUpperCase()));
        assertNull(ArmyCommandService.resolvePlayer(null, "faction:1"));
        assertNull(ArmyCommandService.resolvePlayer(null, "uuid:nope"));
        assertNull(ArmyCommandService.resolvePlayer(null, "Name"));
        assertNull(ArmyCommandService.resolvePlayer(null, null));
        assertFalse(ArmyCommandService.actorFor(null, P).operator());
        assertEquals(P, ArmyCommandService.actorFor(null, P).player());
    }

    /** Sender with a permission level; -1 refuses every level as vanilla does for a player who is not an operator. */
    private static final class Sender implements ICommandSender {
        private final int level;

        Sender(int level) {
            this.level = level;
        }

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public boolean canUseCommand(int permLevel, String commandName) {
            return permLevel <= level;
        }

        @Override
        public Entity getCommandSenderEntity() {
            return null;
        }

        @Override
        public World getEntityWorld() {
            return null;
        }

        @Override
        public MinecraftServer getServer() {
            return null;
        }
    }
}
